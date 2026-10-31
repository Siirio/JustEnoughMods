import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tomllib
import zipfile
from datetime import datetime
from pathlib import Path, PurePosixPath


ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = ROOT / "tools" / "workspace.json"
PACK_SYNC_MANIFEST = "jem-pack/manifest.json"
MODULE_INVENTORY = json.loads((ROOT / "tools/module-inventory.json").read_text(encoding="utf-8"))["modules"]
DEVELOPMENT_MOD_IDS = frozenset(mod_id for module in MODULE_INVENTORY
                                if module["status"] == "development" for mod_id in module["modIds"])
PRODUCTION_BRANCH = "develop"
PRODUCTION_REMOTE_REF = "refs/remotes/origin/develop"
PRODUCTION_APPROVAL_FILE = "production-approval.json"
PRODUCTION_ACTIONS = frozenset({
    "baseline",
    "promote-baseline",
    "sync-stable",
    "package-update",
    "repackage-release",
    "package-full",
    "package-universal",
})
PRODUCTION_SOURCE_PATHS = (
    "Mods",
    "Client/ManagedPack",
    "Client/JustEnoughMods",
    "tools/jem.py",
    "tools/workspace.json",
    "tools/module-inventory.json",
)


def load_config():
    config = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    for key in ("clientSource", "installerSource", "installerArchive", "server", "packSyncHost", "state"):
        config[key] = ROOT / config[key]
    for key in ("stableClient", "testClient", "updates"):
        config[key] = Path(config[key])
    return config


def digest(path):
    return file_digest(path, "sha256")


def file_digest(path, algorithm):
    value = hashlib.new(algorithm)
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def inventory(root, excluded=()):
    exclusions = {Path(value).as_posix().rstrip("/") for value in excluded}
    files = {}
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(root).as_posix()
        if any(relative == item or relative.startswith(item + "/") for item in exclusions):
            continue
        files[relative] = {"sha256": digest(path), "size": path.stat().st_size}
    return files


def production_inventory(source, files):
    """Development domains never enter shared releases, even if a JAR is renamed."""
    excluded = set()
    for relative in files:
        if not relative.startswith("mods/") or not relative.endswith(".jar"):
            continue
        with zipfile.ZipFile(source / relative) as archive:
            if "META-INF/mods.toml" not in archive.namelist():
                continue
            metadata = tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8"))
        ids = {entry.get("modId") for entry in metadata.get("mods", [])}
        if ids & DEVELOPMENT_MOD_IDS:
            excluded.add(relative)
    return {path: value for path, value in files.items() if path not in excluded}


def compare(left, right):
    left_paths = set(left)
    right_paths = set(right)
    return {
        "added": sorted(right_paths - left_paths),
        "changed": sorted(path for path in left_paths & right_paths if left[path] != right[path]),
        "deleted": sorted(left_paths - right_paths),
    }


def exclude_inventory(files, excluded):
    exclusions = {Path(value).as_posix().rstrip("/") for value in excluded}
    return {
        path: metadata
        for path, metadata in files.items()
        if not any(path == item or path.startswith(item + "/") for item in exclusions)
    }


def exclude_delta(delta, excluded):
    return {
        kind: sorted(exclude_inventory(dict.fromkeys(paths), excluded))
        for kind, paths in delta.items()
    }


def baseline_path(config):
    return config["state"] / "release-baseline.json"


def sync_state_path(config, target_name):
    return config["state"] / f"{target_name}-sync.json"


def load_baseline(config):
    path = baseline_path(config)
    if not path.is_file():
        raise SystemExit("Baseline is missing. Run: python tools/jem.py baseline")
    return json.loads(path.read_text(encoding="utf-8"))


def ensure_directory(path, label):
    if not path.is_dir():
        raise SystemExit(f"{label} does not exist: {path}")


def write_json_atomic(path, payload):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)


def git_output(*arguments):
    result = subprocess.run(
        ["git", *arguments],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        detail = result.stderr.strip() or result.stdout.strip() or "unknown Git error"
        raise SystemExit(f"Git command failed: git {' '.join(arguments)}\n{detail}")
    return result.stdout.strip()


def inventory_identity(files):
    canonical = json.dumps(files, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(canonical).hexdigest()


def inspect_release_develop(config):
    branch = git_output("branch", "--show-current")
    if branch != PRODUCTION_BRANCH:
        raise SystemExit(f"Production release is blocked on branch '{branch or 'detached HEAD'}'; switch to develop")
    git_output("fetch", "origin", PRODUCTION_BRANCH)
    head = git_output("rev-parse", "HEAD").lower()
    remote_head = git_output("rev-parse", PRODUCTION_REMOTE_REF).lower()
    if head != remote_head:
        raise SystemExit(f"Production release is blocked: local develop {head} differs from origin/develop {remote_head}")
    dirty = git_output("status", "--porcelain=v1", "--untracked-files=all", "--", *PRODUCTION_SOURCE_PATHS)
    if dirty:
        raise SystemExit("Production release inputs differ from the pushed develop commit:\n" + dirty)
    release_files = release_inventory(config)
    pack_files = pack_sync_inventory(config)
    return {
        "format": 2,
        "verifiedAt": datetime.now().astimezone().isoformat(),
        "branch": PRODUCTION_BRANCH,
        "developCommit": head,
        "originDevelopCommit": remote_head,
        "releaseInventorySha256": inventory_identity(release_files),
        "packSyncInventorySha256": inventory_identity(pack_files),
        "releaseFiles": len(release_files),
        "packSyncFiles": len(pack_files),
    }


def record_production_preflight(config):
    approval = inspect_release_develop(config)
    destination = config["state"] / PRODUCTION_APPROVAL_FILE
    destination.parent.mkdir(parents=True, exist_ok=True)
    write_json_atomic(destination, approval)
    return approval, destination


def production_preflight(config):
    approval, destination = record_production_preflight(config)
    print(json.dumps({**approval, "approvalReceipt": str(destination)}, ensure_ascii=False, indent=2))


def release_inventory(config):
    source = config["clientSource"]
    installer = config["installerSource"]
    ensure_directory(source, "Client source")
    ensure_directory(installer, "Installer source")
    source_files = production_inventory(source, inventory(source, config["releaseExcluded"]))
    bootstrap_path = "mods/jem-bootstrap.jar"
    source_bootstrap = source / bootstrap_path
    installer_bootstrap = installer / bootstrap_path
    if not source_bootstrap.is_file() or not installer_bootstrap.is_file() or digest(source_bootstrap) != digest(installer_bootstrap):
        raise SystemExit("Installer bootstrap differs from the managed pack bootstrap")
    return source_files


def baseline(config):
    destination = baseline_path(config)
    if destination.exists():
        raise SystemExit(f"Baseline already exists: {destination}")
    source_files = release_inventory(config)
    destination.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "createdAt": datetime.now().astimezone().isoformat(),
        "source": str(config["clientSource"]),
        "stableClient": str(config["stableClient"]),
        "files": source_files,
    }
    write_json_atomic(destination, payload)
    write_json_atomic(sync_state_path(config, "stableClient"), exclude_inventory(source_files, config["stableExcluded"]))
    print(f"Baseline fixed: {len(source_files)} files")


def point_update_folder_files(folder, excluded=()):
    if folder.is_file() and folder.suffix.lower() == ".zip":
        files = {}
        with zipfile.ZipFile(folder) as archive:
            if "Что делать.md" not in archive.namelist():
                raise SystemExit(f"Point-update archive lacks its guide: {folder}")
            for entry in archive.infolist():
                if entry.is_dir() or not entry.filename.startswith("Файлы/"):
                    continue
                relative = PurePosixPath(entry.filename).relative_to("Файлы")
                if relative.is_absolute() or ".." in relative.parts or "\\" in entry.filename or relative.as_posix() in files:
                    raise SystemExit(f"Unsafe or duplicate point-update member: {entry.filename}")
                with archive.open(entry) as source:
                    checksum = hashlib.file_digest(source, "sha256").hexdigest()
                files[relative.as_posix()] = {"sha256": checksum, "size": entry.file_size}
        if not files:
            raise SystemExit(f"Point-update archive has no payload: {folder}")
        return exclude_inventory(files, excluded)
    files = folder / "Файлы"
    guide = folder / "Что делать.md"
    if not files.is_dir() or not guide.is_file():
        raise SystemExit(f"Invalid point-update folder: {folder}")
    return inventory(files, excluded)


def cumulative_point_update_files(config, delta, base_update=None):
    included = set(point_update_files(config, delta))
    if base_update is not None:
        included.update(point_update_folder_files(base_update, config["releaseExcluded"]))
    client_files = inventory(config["clientSource"], config["releaseExcluded"])
    return sorted(path for path in included if path in client_files)


def baseline_point_update_folder(baseline_data):
    point_update = baseline_data.get("pointUpdate")
    if not point_update or not point_update.get("folder"):
        return None
    folder = Path(point_update["folder"])
    if (folder / "Файлы").is_dir() and (folder / "Что делать.md").is_file():
        return folder
    archive = Path(point_update.get("archive", ""))
    if archive.is_file() and digest(archive) == point_update.get("sha256"):
        return archive
    raise SystemExit(f"Prior point-update folder and verified archive are unavailable: {folder}")


def matching_point_update(config, delta, base_update=None):
    ensure_directory(config["updates"], "Updates directory")
    packaged_files = cumulative_point_update_files(config, delta, base_update)
    client_files = inventory(config["clientSource"], config["releaseExcluded"])
    expected_files = {
        path: client_files[path]
        for path in packaged_files
    }
    for folder in sorted(config["updates"].glob("JustEnoughMods-update-*"), reverse=True):
        if not folder.is_dir():
            continue
        files = folder / "Файлы"
        guide = folder / "Что делать.md"
        archive = folder.with_suffix(".zip")
        if not files.is_dir() or not guide.is_file() or not archive.is_file():
            continue
        if inventory(files) == expected_files and guide.read_text(encoding="utf-8") == update_notes(delta, packaged_files):
            return folder, archive
    raise SystemExit("No point-update folder and ZIP match the current release delta")


def promote_baseline(config):
    destination = baseline_path(config)
    previous = load_baseline(config)
    source_files = release_inventory(config)
    delta = compare(exclude_inventory(previous["files"], config["releaseExcluded"]), source_files)
    if not any(delta.values()):
        raise SystemExit("No client changes to promote")
    previous_point_update = baseline_point_update_folder(previous)
    folder, archive = matching_point_update(config, delta, previous_point_update)
    payload = {
        "createdAt": datetime.now().astimezone().isoformat(),
        "source": str(config["clientSource"]),
        "stableClient": str(config["stableClient"]),
        "previousCreatedAt": previous["createdAt"],
        "publishedDelta": delta,
        "pointUpdate": {
            "folder": str(folder),
            "archive": str(archive),
            "sha256": digest(archive),
        },
        "previousPointUpdate": previous.get("pointUpdate"),
        "files": source_files,
    }
    write_json_atomic(destination, payload)
    write_json_atomic(sync_state_path(config, "stableClient"), exclude_inventory(source_files, config["stableExcluded"]))
    print(json.dumps({key: payload[key] for key in ("createdAt", "previousCreatedAt", "publishedDelta", "pointUpdate")}, ensure_ascii=False, indent=2))


def current_delta(config):
    source = config["clientSource"]
    ensure_directory(source, "Client source")
    baseline_files = exclude_inventory(load_baseline(config)["files"], config["releaseExcluded"])
    return compare(baseline_files, inventory(source, config["releaseExcluded"]))


def profile_delta(source_files, profile_files):
    managed_profile = {path: profile_files[path] for path in source_files if path in profile_files}
    missing = sorted(set(source_files) - set(profile_files))
    changed = sorted(path for path in source_files if path in profile_files and source_files[path] != profile_files[path])
    return {"missing": missing, "changed": changed}


def pack_sync_publication(config):
    manifest = config["packSyncHost"] / PACK_SYNC_MANIFEST
    journal = config["server"] / "automodpack" / "server" / "journal.jsonl"
    published = None
    sequence = None
    if journal.is_file():
        entries = [json.loads(line) for line in journal.read_text(encoding="utf-8").splitlines() if line.strip()]
        if entries:
            sequence = entries[-1]["seq"]
        for entry in reversed(entries):
            for change in reversed(entry.get("changes", [])):
                if change.get("path") == PACK_SYNC_MANIFEST:
                    published = change.get("toSha1")
                    break
            if published is not None:
                break
    staged = file_digest(manifest, "sha1") if manifest.is_file() else None
    return {
        "published": staged is not None and staged == published,
        "journalSequence": sequence,
        "publishedManifestSha1": published,
        "stagedManifestSha1": staged,
    }


def status(config):
    source_files = inventory(config["clientSource"], config["releaseExcluded"])
    installer_files = inventory(config["installerSource"])
    bootstrap_path = "mods/jem-bootstrap.jar"
    stable_source_files = {bootstrap_path: installer_files[bootstrap_path]} if bootstrap_path in installer_files else {}
    stable_files = inventory(config["stableClient"], config["stableExcluded"])
    delta = compare(exclude_inventory(load_baseline(config)["files"], config["releaseExcluded"]), source_files)
    stable_delta = profile_delta(stable_source_files, stable_files)
    result = {
        "releaseDelta": delta,
        "stableSync": stable_delta,
        "packSyncPublication": pack_sync_publication(config),
        "counts": {
            "clientSource": len(source_files),
            "installerSource": len(installer_files),
            "added": len(delta["added"]),
            "changed": len(delta["changed"]),
            "deleted": len(delta["deleted"]),
            "stableMissing": len(stable_delta["missing"]),
            "stableChanged": len(stable_delta["changed"]),
        },
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))


def synchronize(config, target_name):
    source = config["installerSource"] if target_name == "stableClient" else config["clientSource"]
    target = config[target_name]
    ensure_directory(source, "Client source")
    ensure_directory(target, target_name)
    native_state_path = target / "automodpack" / "client" / "active-state.json"
    if target_name == "testClient" and native_state_path.is_file():
        native_state = json.loads(native_state_path.read_text(encoding="utf-8"))
        if native_state.get("status") == "ACTIVE":
            print(json.dumps({
                "target": str(target),
                "mode": "AutoModpack",
                "activeContentToken": native_state.get("contentToken"),
                "copied": [],
                "deleted": [],
                "message": "Selected AutoModpack owns installation. Stage and publish the clean Client on the local server, then update through Minecraft; direct gameDir synchronization is disabled.",
            }, ensure_ascii=False, indent=2))
            return
    exclusions = config["testExcluded"] if target_name == "testClient" else config["stableExcluded"]
    current_files = inventory(source, exclusions)
    if target_name == "stableClient":
        bootstrap_path = "mods/jem-bootstrap.jar"
        current_files = {bootstrap_path: current_files[bootstrap_path]}
    state_path = sync_state_path(config, target_name)
    previous_files = (
        json.loads(state_path.read_text(encoding="utf-8"))
        if state_path.is_file()
        else inventory_from_baseline(config, exclusions)
    )
    if target_name == "stableClient":
        previous_files = current_files
    previous_files = exclude_inventory(previous_files, exclusions)
    copied = []
    deleted = []
    for relative, metadata in current_files.items():
        source_file = source / Path(relative)
        target_file = target / Path(relative)
        if target_file.is_file() and target_file.stat().st_size == metadata["size"] and digest(target_file) == metadata["sha256"]:
            continue
        target_file.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_file, target_file)
        copied.append(relative)
    for relative in sorted(set(previous_files) - set(current_files)):
        target_file = target / Path(relative)
        if target_file.is_file():
            target_file.unlink()
            deleted.append(relative)
    remove_empty_directories(target)
    state_path.parent.mkdir(parents=True, exist_ok=True)
    state_path.write_text(json.dumps(current_files, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"target": str(target), "copied": copied, "deleted": deleted}, ensure_ascii=False, indent=2))


def recover_pack_sync(config, transaction_path):
    target = config["testClient"].resolve()
    storage = target / "automodpack" / "client"
    ensure_directory(storage, "Native client storage")
    if transaction_path is None:
        raise SystemExit("Specify the quarantined native transaction with --transaction")
    transaction_path = transaction_path.resolve()
    if transaction_path.parent != storage or not transaction_path.name.startswith("update-transaction.json.corrupt-"):
        raise SystemExit("Recovery accepts only a quarantined transaction in the development profile")
    powershell = shutil.which("pwsh") or shutil.which("powershell")
    jshell = shutil.which("jshell")
    if powershell is None or jshell is None:
        raise SystemExit("PowerShell and the JDK JShell are required for native recovery")
    processes = subprocess.run([
        powershell, "-NoProfile", "-Command",
        "Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | "
        "Where-Object { $_.CommandLine -and "
        "$_.CommandLine.IndexOf($env:JEM_RECOVERY_PROFILE, [StringComparison]::OrdinalIgnoreCase) -ge 0 } | "
        "Select-Object -ExpandProperty ProcessId",
    ], env={**os.environ, "JEM_RECOVERY_PROFILE": str(target)}, capture_output=True, text=True, check=True)
    if processes.stdout.strip():
        raise SystemExit("Close Minecraft and TLauncher before recovery; profile processes: " + processes.stdout.strip())
    pending = storage / "update-transaction.json"
    transaction_source = pending if pending.exists() else transaction_path
    transaction = json.loads(transaction_source.read_text(encoding="utf-8"))
    plan = transaction["plan"]
    selected = json.loads((storage / "selected.json").read_text(encoding="utf-8"))
    active = json.loads((storage / "active-state.json").read_text(encoding="utf-8"))
    if transaction["purpose"] != "MODPACK_UPDATE" or active["status"] != "ACTIVE" or selected["modpackId"] != plan["modpackId"] or active["modpackId"] != plan["modpackId"]:
        raise SystemExit("Transaction does not belong to the selected active installation")
    bootstrap = target / "mods" / "jem-bootstrap.jar"
    bootstrap_sources = [
        config["installerSource"] / "mods" / "jem-bootstrap.jar",
        config["clientSource"] / "mods" / "jem-bootstrap.jar",
        ROOT / "Mods" / "JEMBootstrap" / "build" / "libs" / "jem-bootstrap.jar",
    ]
    if not any(path.is_file() and digest(bootstrap) == digest(path) for path in bootstrap_sources):
        raise SystemExit("Installed bootstrap differs from the clean distribution and the current native source build")
    profile = json.loads((target / (target.name + ".json")).read_text(encoding="utf-8"))
    dependencies = []
    for library in profile["libraries"]:
        group, artifact, version = library["name"].split(":")[:3]
        if artifact in {"gson", "log4j-api", "log4j-core"}:
            dependency = target.parent.parent / "libraries" / group.replace(".", "/") / artifact / version / f"{artifact}-{version}.jar"
            if not dependency.is_file():
                raise SystemExit(f"Native dependency is missing: {dependency}")
            dependencies.append(dependency)
    if {path.parent.parent.name for path in dependencies} != {"gson", "log4j-api", "log4j-core"}:
        raise SystemExit("Profile must declare the native Gson and Log4j dependencies")
    journal = storage / "history" / plan["modpackId"] / "journal.jsonl"
    policies = {json.loads(line)["policySha1"] for line in journal.read_text(encoding="utf-8").splitlines()}
    imports = []
    for sha1 in sorted(policies):
        if not re.fullmatch(r"[0-9a-f]{40}", sha1):
            raise SystemExit("Invalid recorded policy identity")
        source = config["server"] / "automodpack" / "server" / "data" / "objects" / sha1[:2] / sha1[2:]
        if not source.is_file() or hashlib.sha1(source.read_bytes()).hexdigest() != sha1:
            raise SystemExit(f"Exact mirrored policy is unavailable on the local server: {sha1}")
        imports.append(f'ClientObjectStore.storeObject(nativeStorage, {json.dumps(sha1)}, Files.readAllBytes(Path.of({json.dumps(source.resolve().as_posix())})));')
    script = "\n".join([
        "import pl.skidam.automodpack_core.update.*;",
        "import pl.skidam.automodpack_core.client.*;",
        "import pl.skidam.automodpack_core.Constants;",
        "import java.nio.file.*;",
        'Constants.LOADER = "forge";',
        'var nativeStorage = ClientStorage.open(Path.of(System.getProperty("user.dir")));',
        f'var nativePending = UpdateTransaction.read(Path.of({json.dumps(transaction_source.as_posix())}));',
        'ClientStorageMutation.run(nativeStorage, () -> { ' + " ".join(imports) + ' return null; });',
        'try { var result = UpdateAttempt.resume(nativeStorage, nativePending, new pl.skidam.automodpack_loader_core_forge.mods.ModpackLoader(), Constants.LOADER); System.out.println("Native recovery: " + result.status()); if (result.success()) ClientStorageMutation.run(nativeStorage, () -> { var state = nativeStorage.readActiveState(); if (!new ClientGenerationStore(nativeStorage).headMatchesActive(state.modpackId, state.contentToken)) throw new java.io.IOException("Installed generation differs from mirrored head"); nativeStorage.setDetached(state.modpackId, false); return null; }); } catch (Throwable failure) { failure.printStackTrace(); }',
        "/exit",
    ])
    subprocess.run([
        jshell, "--class-path", os.pathsep.join(str(path) for path in [bootstrap, *dependencies]),
        "--feedback", "concise", "-",
    ], cwd=target, input=script, text=True, check=True)
    installed = json.loads((storage / "active-state.json").read_text(encoding="utf-8"))
    if installed["contentToken"] != plan["contentToken"] or installed.get("detached") or pending.exists():
        raise SystemExit("Native recovery did not finish the requested generation")
    print(json.dumps({"target": str(target), "contentToken": installed["contentToken"], "mode": "Native AutoModpack recovery"}, ensure_ascii=False, indent=2))


def stage_pack_sync(config):
    source = config["clientSource"]
    server = config["server"]
    target = config["packSyncHost"]
    ensure_directory(source, "Client source")
    ensure_directory(server, "Server")
    manifest_version = write_pack_sync_manifest(config)
    source_files = pack_sync_inventory(config)
    staged_files = source_files
    state_path = sync_state_path(config, "packSyncHost")
    target_files = inventory(target) if target.is_dir() else {}
    copied = []
    deleted = []
    for relative, metadata in staged_files.items():
        source_file = source / Path(relative)
        target_file = target / Path(relative)
        if target_file.is_file() and target_file.stat().st_size == metadata["size"] and digest(target_file) == metadata["sha256"]:
            continue
        target_file.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_file, target_file)
        copied.append(relative)
    for relative in sorted(set(target_files) - set(staged_files)):
        target_file = target / Path(relative)
        if target_file.is_file():
            target_file.unlink()
            deleted.append(relative)
    if target.is_dir():
        remove_empty_directories(target)
    state_path.parent.mkdir(parents=True, exist_ok=True)
    write_json_atomic(state_path, staged_files)
    print(json.dumps({
        "target": str(target),
        "copied": copied,
        "deleted": deleted,
        "staged": len(staged_files),
        "manifestVersion": manifest_version,
    }, ensure_ascii=False, indent=2))


def write_pack_sync_manifest(config):
    source = config["clientSource"]
    files = pack_sync_inventory(config, (PACK_SYNC_MANIFEST,))
    canonical = json.dumps(files, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    version = hashlib.sha256(canonical).hexdigest()
    destination = source / PACK_SYNC_MANIFEST
    destination.parent.mkdir(parents=True, exist_ok=True)
    write_json_atomic(destination, {
        "format": 1,
        "version": version,
        "algorithm": "SHA-256",
        "files": files,
    })
    return version


def pack_sync_inventory(config, extra_excluded=()):
    source = config["clientSource"]
    files = {}
    for included in config["packSyncIncluded"]:
        included_path = source / included
        if not included_path.is_dir():
            continue
        for relative, metadata in inventory(included_path).items():
            files[(Path(included) / relative).as_posix()] = metadata
    manifest = source / PACK_SYNC_MANIFEST
    if manifest.is_file() and PACK_SYNC_MANIFEST not in extra_excluded:
        files[PACK_SYNC_MANIFEST] = {"sha256": digest(manifest), "size": manifest.stat().st_size}
    files = exclude_inventory(files, [*config["packSyncExcluded"], *extra_excluded])
    blocked_suffixes = {".tmp", ".disabled", ".bak"}
    return {
        path: metadata
        for path, metadata in production_inventory(source, files).items()
        if not any(part.startswith(".") for part in Path(path).parts)
        and Path(path).suffix.lower() not in blocked_suffixes
    }


def inventory_from_baseline(config, excluded):
    return exclude_inventory(load_baseline(config)["files"], excluded)


def remove_empty_directories(root):
    for path in sorted((item for item in root.rglob("*") if item.is_dir()), reverse=True):
        try:
            path.rmdir()
        except OSError:
            pass


def point_update_files(config, delta):
    included = set(delta["added"] + delta["changed"])
    source = config["clientSource"]
    for pattern in config.get("pointUpdateAlwaysInclude", []):
        included.update(path.relative_to(source).as_posix() for path in source.glob(pattern) if path.is_file())
    return sorted(included)


def update_notes(delta, packaged_files=None):
    changed_files = packaged_files if packaged_files is not None else delta["added"] + delta["changed"]
    lines = [
        "# Что делать",
        "",
        "Закройте Minecraft и запустите JustEnoughMods через свой лаунчер. Подтвердите обновление AutoModpack и дождитесь его завершения; перезапустите игру, если загрузчик попросит.",
        "",
        "Если игра падает до окна обновления, одного перезапуска или замены JAR в обычной mods недостаточно: активной установкой управляет AutoModpack. Используйте приложенную инструкцию восстановления, если она есть, либо отправьте администратору свежий crash-report и latest.log.",
        "",
        "Не копируйте папку Файлы поверх активного профиля AutoModpack, не удаляйте его кэш и не редактируйте active-state.json. Папка Файлы содержит полный накопительный материал обновления для штатного установщика и восстановления.",
        "",
        "Удаление устаревших управляемых файлов также выполняет AutoModpack. Миры, скриншоты, личные точки и настройки вручную удалять не нужно.",
        "",
        f"В накопительном пакете файлов: {len(changed_files)}.",
        "",
    ]
    return "\n".join(lines)


def package_delta(config, delta, base_update=None):
    delta = exclude_delta(delta, config["releaseExcluded"])
    release_files = delta["added"] + delta["changed"]
    if not release_files and not delta["deleted"]:
        raise SystemExit("No client changes relative to the fixed release baseline")
    packaged_files = cumulative_point_update_files(config, delta, base_update)
    config["updates"].mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().astimezone().strftime("%Y-%m-%d_%H-%M-%S")
    name = f"JustEnoughMods-update-{timestamp}"
    folder = config["updates"] / name
    files_folder = folder / "Файлы"
    files_folder.mkdir(parents=True, exist_ok=True)
    for relative in packaged_files:
        destination = files_folder / Path(relative)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(config["clientSource"] / Path(relative), destination)
    (folder / "Что делать.md").write_text(update_notes(delta, packaged_files), encoding="utf-8")
    archive_path = config["updates"] / f"{name}.zip"
    with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        archive.writestr("Файлы/", "")
        for path in sorted(folder.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(folder).as_posix())
    return {
        "folder": str(folder),
        "archive": str(archive_path),
        "sha256": digest(archive_path),
        "inheritedFrom": str(base_update) if base_update is not None else None,
        "included": packaged_files,
        **delta,
    }


def package_update(config):
    baseline_data = load_baseline(config)
    base_update = baseline_point_update_folder(baseline_data)
    print(json.dumps(package_delta(config, current_delta(config), base_update), ensure_ascii=False, indent=2))


def repackage_release(config, base_update=None):
    destination = baseline_path(config)
    baseline_data = load_baseline(config)
    if any(current_delta(config).values()):
        raise SystemExit("Client differs from the published release baseline")
    delta = baseline_data.get("publishedDelta")
    if not delta:
        raise SystemExit("Published release delta is missing from the baseline")
    if base_update is None:
        base_update = baseline_point_update_folder({"pointUpdate": baseline_data.get("previousPointUpdate")})
    result = package_delta(config, delta, base_update)
    baseline_data["pointUpdate"] = {key: result[key] for key in ("folder", "archive", "sha256")}
    if base_update is not None:
        baseline_data["previousPointUpdate"] = {"folder": str(base_update)}
    write_json_atomic(destination, baseline_data)
    print(json.dumps(result, ensure_ascii=False, indent=2))


def package_full(config):
    config["updates"].mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().astimezone().strftime("%Y-%m-%d_%H-%M-%S")
    destination = config["updates"] / f"JustEnoughMods-full-{timestamp}.zip"
    source = config["installerSource"]
    trees = [(source, Path())]
    for relative in map(Path, config["installerSeedPaths"]):
        seed = config["clientSource"] / relative
        ensure_directory(seed, f"Installer seed {relative.as_posix()}")
        trees.append((seed, relative))
    written = set()
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for tree, prefix in trees:
            for path in sorted(tree.rglob("*")):
                relative = (prefix / path.relative_to(tree)).as_posix()
                member = f"JustEnoughMods/{relative}"
                if path.is_dir():
                    member += "/"
                    if member not in written:
                        archive.writestr(member, "")
                        written.add(member)
                if path.is_file():
                    if member in written:
                        raise SystemExit(f"Duplicate installer path: {relative}")
                    archive.write(path, member)
                    written.add(member)
    shutil.copy2(destination, config["installerArchive"])
    print(json.dumps({
        "archive": str(destination),
        "shareableArchive": str(config["installerArchive"]),
        "sha256": digest(destination),
    }, ensure_ascii=False, indent=2))


def package_universal(config):
    """Complete, launcher-neutral files for a fresh Forge instance, without runtime state."""
    source = config["clientSource"]
    # Native publication excludes Bootstrap because it is the loader itself. A full pack includes it.
    files = pack_sync_inventory(config)
    bootstrap = "mods/jem-bootstrap.jar"
    release = release_inventory(config)
    files[bootstrap] = release[bootstrap]
    files = dict(sorted(files.items()))
    destination = ROOT / "Client" / "JustEnoughMods-universal.zip"
    temporary = destination.with_suffix(".zip.tmp")
    guide = (ROOT / "Docs" / "universal" / "INSTALL.md").read_text(encoding="utf-8")
    manifest = {
        "format": 1, "createdAt": datetime.now().astimezone().isoformat(),
        "minecraft": "1.20.1", "forge": "47.4.10", "java": 17,
        "platforms": ["windows", "linux", "macos"],
        "files": files,
    }
    with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        archive.writestr("INSTALL.md", guide)
        archive.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
        for relative in files:
            archive.write(source / relative, "JustEnoughMods/" + relative)
    temporary.replace(destination)
    receipt = {
        "archive": str(destination), "createdAt": manifest["createdAt"],
        "sha256": digest(destination), "files": len(files), "bootstrapIncluded": True,
        "platformRuntimeAcceptance": "not run",
    }
    write_json_atomic(config["state"] / "universal-package.json", receipt)
    print(json.dumps(receipt, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=("baseline", "promote-baseline", "production-preflight", "status", "sync-stable", "sync-test", "stage-pack-sync", "recover-pack-sync", "package-update", "repackage-release", "package-full", "package-universal"))
    parser.add_argument("--base-update", type=Path)
    parser.add_argument("--transaction", type=Path)
    parser.add_argument("--approved-develop", help=argparse.SUPPRESS)
    args = parser.parse_args()
    config = load_config()
    if args.action in PRODUCTION_ACTIONS:
        record_production_preflight(config)
    actions = {
        "baseline": baseline,
        "promote-baseline": promote_baseline,
        "production-preflight": production_preflight,
        "status": status,
        "sync-stable": lambda value: synchronize(value, "stableClient"),
        "sync-test": lambda value: synchronize(value, "testClient"),
        "stage-pack-sync": stage_pack_sync,
        "recover-pack-sync": lambda value: recover_pack_sync(value, args.transaction),
        "package-update": package_update,
        "repackage-release": lambda value: repackage_release(value, args.base_update),
        "package-full": package_full,
        "package-universal": package_universal,
    }
    actions[args.action](config)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(130)


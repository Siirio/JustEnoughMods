import argparse
import hashlib
import json
import shutil
import sys
import zipfile
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = ROOT / "tools" / "workspace.json"
PACK_SYNC_MANIFEST = "jem-pack/manifest.json"


def load_config():
    config = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    for key in ("clientSource", "server", "packSyncHost", "state"):
        config[key] = ROOT / config[key]
    for key in ("stableClient", "testClient", "updates"):
        config[key] = Path(config[key])
    return config


def digest(path):
    value = hashlib.sha256()
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


def release_inventory(config):
    source = config["clientSource"]
    stable = config["stableClient"]
    ensure_directory(source, "Client source")
    ensure_directory(stable, "Stable client")
    source_files = inventory(source)
    sync_source_files = inventory(source, config["stableExcluded"])
    stable_files = inventory(stable, config["stableExcluded"])
    mismatch = compare(sync_source_files, {path: stable_files[path] for path in sync_source_files if path in stable_files})
    missing = sorted(set(sync_source_files) - set(stable_files))
    if mismatch["changed"] or missing:
        raise SystemExit(json.dumps({"changed": mismatch["changed"], "missing": missing}, ensure_ascii=False, indent=2))
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


def point_update_folder_files(folder):
    files = folder / "Файлы"
    guide = folder / "Что делать.md"
    if not files.is_dir() or not guide.is_file():
        raise SystemExit(f"Invalid point-update folder: {folder}")
    return inventory(files)


def cumulative_point_update_files(config, delta, base_update=None):
    included = set(point_update_files(config, delta))
    if base_update is not None:
        included.update(point_update_folder_files(base_update))
    client_files = inventory(config["clientSource"])
    return sorted(path for path in included if path in client_files)


def baseline_point_update_folder(baseline_data):
    point_update = baseline_data.get("pointUpdate")
    if not point_update or not point_update.get("folder"):
        return None
    return Path(point_update["folder"])


def matching_point_update(config, delta, base_update=None):
    ensure_directory(config["updates"], "Updates directory")
    packaged_files = cumulative_point_update_files(config, delta, base_update)
    client_files = inventory(config["clientSource"])
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
    delta = compare(previous["files"], source_files)
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
    baseline_files = load_baseline(config)["files"]
    return compare(baseline_files, inventory(source))


def profile_delta(source_files, profile_files):
    managed_profile = {path: profile_files[path] for path in source_files if path in profile_files}
    missing = sorted(set(source_files) - set(profile_files))
    changed = sorted(path for path in source_files if path in profile_files and source_files[path] != profile_files[path])
    return {"missing": missing, "changed": changed}


def status(config):
    source_files = inventory(config["clientSource"])
    stable_source_files = exclude_inventory(source_files, config["stableExcluded"])
    stable_files = inventory(config["stableClient"], config["stableExcluded"])
    delta = compare(load_baseline(config)["files"], source_files)
    stable_delta = profile_delta(stable_source_files, stable_files)
    result = {
        "releaseDelta": delta,
        "stableSync": stable_delta,
        "counts": {
            "clientSource": len(source_files),
            "added": len(delta["added"]),
            "changed": len(delta["changed"]),
            "deleted": len(delta["deleted"]),
            "stableMissing": len(stable_delta["missing"]),
            "stableChanged": len(stable_delta["changed"]),
        },
    }
    print(json.dumps(result, ensure_ascii=False, indent=2))


def synchronize(config, target_name):
    source = config["clientSource"]
    target = config[target_name]
    ensure_directory(source, "Client source")
    ensure_directory(target, target_name)
    exclusions = config["testExcluded"] if target_name == "testClient" else config["stableExcluded"]
    current_files = inventory(source, exclusions)
    state_path = sync_state_path(config, target_name)
    previous_files = (
        json.loads(state_path.read_text(encoding="utf-8"))
        if state_path.is_file()
        else inventory_from_baseline(config, exclusions)
    )
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
        for path, metadata in files.items()
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
    roots = sorted({Path(path).parts[0] for path in changed_files})
    lines = [
        "# Что делать",
        "",
        "Удалить:",
        *(f"- `versions/JustEnoughMods/{path}`" for path in delta["deleted"]),
    ]
    if not delta["deleted"]:
        lines.append("- Ничего.")
    lines.append("")
    for root in roots:
        matching = [Path(path) for path in changed_files if Path(path).parts[0] == root]
        if all(len(path.parts) == 1 for path in matching):
            for path in matching:
                lines.append(f"Вставить файл `Файлы/{path.as_posix()}` → `versions/JustEnoughMods/{path.as_posix()}` с заменой.")
        else:
            lines.append(f"Вставить всё из папки `Файлы/{root}` → `versions/JustEnoughMods/{root}` с заменой.")
    lines.extend(("", "Готово.", ""))
    return "\n".join(lines)


def package_delta(config, delta, base_update=None):
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
    if compare(baseline_data["files"], inventory(config["clientSource"])) != {"added": [], "changed": [], "deleted": []}:
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
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(config["clientSource"].rglob("*")):
            if path.is_file():
                archive.write(path, "JustEnoughMods/" + path.relative_to(config["clientSource"]).as_posix())
    print(json.dumps({"archive": str(destination), "sha256": digest(destination)}, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=("baseline", "promote-baseline", "status", "sync-stable", "sync-test", "stage-pack-sync", "package-update", "repackage-release", "package-full"))
    parser.add_argument("--base-update", type=Path)
    args = parser.parse_args()
    config = load_config()
    actions = {
        "baseline": baseline,
        "promote-baseline": promote_baseline,
        "status": status,
        "sync-stable": lambda value: synchronize(value, "stableClient"),
        "sync-test": lambda value: synchronize(value, "testClient"),
        "stage-pack-sync": stage_pack_sync,
        "package-update": package_update,
        "repackage-release": lambda value: repackage_release(value, args.base_update),
        "package-full": package_full,
    }
    actions[args.action](config)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(130)


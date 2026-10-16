"""Compose a fresh reobfuscated R3 extension with the pinned standalone sandbox.

The historical standalone source is absent from the current checkout. Keep all
its existing classes and assets byte-identical; this module owns only the R3
extension. The original JAR is an explicit local build input, never a source of
gameplay truth. It remains backed up outside mods for recovery/rebuilding.
"""
from pathlib import Path
import argparse
import copy
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import zipfile

BASE_SHA256 = "65bc16ee2881c354f6fe069ba49a509b1ab784168ced66689da3f14a401dd66a"
HERE = Path(__file__).resolve().parent


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def compose(base):
    if sha256(base) != BASE_SHA256:
        raise ValueError("Wrong standalone base JAR; use the preserved original from visual-backups")
    overlay = HERE / "build/libs/jem-visual-sandbox-overlay-r3.jar"
    if not overlay.is_file():
        raise FileNotFoundError("Build the production jar/reobfJar first")
    target = HERE / "build/distributions/jem-visual-sandbox-1.0.0.jar"
    target.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(base) as original, zipfile.ZipFile(overlay) as fresh:
        additions = {n: fresh.read(n) for n in fresh.namelist() if not n.endswith("/") and n != "META-INF/MANIFEST.MF"}
        if not any(n.startswith("com/siirio/jemvisualsandbox/r3/") for n in additions):
            raise ValueError("R3 classes missing from overlay")
        collisions = set(original.namelist()) & set(additions)
        languages = {"assets/jem_visual_sandbox/lang/en_us.json", "assets/jem_visual_sandbox/lang/ru_ru.json"}
        if collisions - languages:
            raise ValueError(f"Unexpected replacement of original sandbox entries: {collisions - languages}")
        for name in collisions:
            old = json.loads(original.read(name))
            new = json.loads(additions[name])
            if old.keys() & new.keys():
                raise ValueError("Extension must not replace existing translations")
            additions[name] = (json.dumps(old | new, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
        with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as output:
            for entry in original.infolist():
                if entry.filename not in additions:
                    output.writestr(copy.copy(entry), original.read(entry.filename))
            for name, data in sorted(additions.items()):
                output.writestr(name, data)
            sources = HERE.parent.parent / "Docs/races/assets/sandbox-workbench-sources"
            archive = target.parent / "blockbench-editable-sources.zip"
            if not (sources / "manifest.json").is_file():
                raise FileNotFoundError("Snapshot the authored Blockbench sources before composing")
            with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as editable:
                for source in sorted(sources.rglob("*")):
                    if source.is_file():
                        editable.write(source, source.relative_to(sources).as_posix())
            output.write(archive, "assets/jem_visual_sandbox/sources.zip")
        with zipfile.ZipFile(target) as output:
            preserved = [n for n in original.namelist() if n not in collisions]
            if any(output.read(n) != original.read(n) for n in preserved):
                raise ValueError("Original sandbox content changed during composition")
    return target


def install(target, profile):
    if profile.name != "jem sandbox" or not (profile / "jem sandbox.json").is_file():
        raise ValueError("Only the existing jem sandbox profile is an allowed target")
    # Never replace an open game's artifact. No process is stopped or launched.
    shell = shutil.which("pwsh") or str(Path(os.environ["SystemRoot"]) / "System32/WindowsPowerShell/v1.0/powershell.exe")
    processes = subprocess.run([shell, "-NoProfile", "-Command",
        "Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^javaw?\\.exe$' } | Select-Object -ExpandProperty CommandLine"],
        capture_output=True, text=True, check=True).stdout
    if str(profile).lower() in processes.lower() or "jem sandbox" in processes.lower():
        raise RuntimeError("Close jem sandbox before replacing its JAR")
    installed = profile / "mods/jem-visual-sandbox-1.0.0.jar"
    previous = sha256(installed)
    timestamp = datetime.datetime.now().astimezone().strftime("%Y%m%d-%H%M%S")
    backup = profile / "visual-backups" / ("bloom-r3-" + timestamp)
    backup.mkdir(parents=True, exist_ok=False)
    shutil.copy2(installed, backup / installed.name)
    temporary = installed.with_suffix(".jar.r3-staged")
    shutil.copy2(target, temporary)
    if sha256(temporary) != sha256(target):
        raise OSError("Staged JAR differs from production output")
    os.replace(temporary, installed)
    record = {"profile": str(profile), "installed": str(installed), "previousSha256": previous,
        "installedSha256": sha256(installed), "baseSha256": BASE_SHA256,
        "backup": str(backup / installed.name), "installedAt": datetime.datetime.now().astimezone().isoformat(),
        "sourceOwner": "Mods/JEMVisualSandboxR3", "gameLaunched": False,
        "scope": "standalone sandbox main skills only; canonical pack and production gameplay unchanged"}
    (backup / "installation.json").write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (HERE / "build/installation.json").write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return record


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True, type=Path)
    parser.add_argument("--profile", type=Path, help="Install only into the named jem sandbox profile")
    args = parser.parse_args()
    target = compose(args.base.resolve())
    print(json.dumps(install(target, args.profile.resolve()) if args.profile else {"artifact": str(target), "sha256": sha256(target)}, ensure_ascii=False, indent=2))

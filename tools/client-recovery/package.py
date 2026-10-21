"""Build and repackage the approved October 21 recovery without touching game profiles."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
from datetime import datetime
import zipfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "tools"))
import jem


def main():
    config = jem.load_config()
    baseline = jem.load_baseline(config)
    old = Path(baseline["pointUpdate"]["archive"])
    if jem.digest(old) != baseline["pointUpdate"]["sha256"]:
        raise SystemExit("Prior package hash differs")
    java_home = Path(os.environ["JEM_RECOVERY_JDK"])
    profile = config["stableClient"]
    bootstrap = profile / "mods/jem-bootstrap.jar"
    if jem.digest(bootstrap) != "a8c26d26b28ba4d51134731cf6c285cd6a92268777d82c28831e38a716a5c99b":
        raise SystemExit("Matching bootstrap required")
    if jem.digest(config["clientSource"] / "jem-pack/manifest.json") != "87f1ce9c0e863d6e285bf04424b8b582bc08750c3103be03fa8e3a86bdaa1a83":
        raise SystemExit("This recovery is pinned to the October 21 hotfix")
    dependencies = [bootstrap]
    for library in json.loads((profile / "JustEnoughMods.json").read_text())["libraries"]:
        group, name, version, *_ = library["name"].split(":")
        path = profile.parent.parent / "libraries" / group.replace(".", "/") / name / version / f"{name}-{version}.jar"
        if path.is_file():
            dependencies.append(path)
    build = SOURCE / "build"
    build.mkdir(exist_ok=True)
    subprocess.run([str(java_home / "bin/javac.exe"), "--release", "17", "-proc:none", "-encoding", "UTF-8",
                    "-cp", os.pathsep.join(map(str, dependencies)), "-d", str(build), str(SOURCE / "JemClientRecovery.java")], check=True)
    helper = build / "jem-client-recovery.jar"
    subprocess.run([str(java_home / "bin/jar.exe"), "--create", "--file", str(helper), "-C", str(build), "JemClientRecovery.class"], check=True)
    folder = config["updates"] / ("JustEnoughMods-update-" + datetime.now().astimezone().strftime("%Y-%m-%d_%H-%M-%S"))
    folder.mkdir()
    inherited = []
    with zipfile.ZipFile(old) as previous:
        for entry in previous.infolist():
            relative = Path(entry.filename)
            if not entry.filename.startswith("Файлы/") or entry.is_dir():
                continue
            if relative.is_absolute() or ".." in relative.parts:
                raise SystemExit("Unsafe prior archive member")
            destination = folder / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            with previous.open(entry) as source, destination.open("wb") as target:
                shutil.copyfileobj(source, target)
            inherited.append(relative.relative_to("Файлы").as_posix())
    wanted = set(inherited)
    wanted.update(path.relative_to(config["clientSource"]).as_posix()
                  for path in (config["clientSource"] / "kubejs").rglob("*") if path.is_file())
    for relative in sorted(wanted):
        current = config["clientSource"] / relative
        if not current.is_file():
            raise SystemExit("Inherited payload has no current owner: " + relative)
        destination = folder / "Файлы" / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(current, destination)
    for name in ("Repair.cmd", "Что делать.md"):
        shutil.copy2(SOURCE / name, folder / name)
    shutil.copy2(helper, folder / helper.name)
    script = (SOURCE / "Repair.ps1").read_text(encoding="utf-8").replace("__RECOVERY_SHA256__", jem.digest(helper))
    (folder / "Repair.ps1").write_text(script, encoding="utf-8-sig")
    archive = folder.with_suffix(".zip")
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as output:
        for path in sorted(folder.rglob("*")):
            if path.is_file():
                output.write(path, path.relative_to(folder).as_posix())
    result = {"folder": str(folder), "archive": str(archive), "sha256": jem.digest(archive),
              "inheritedFrom": str(old), "includedFiles": len(wanted), "helperSha256": jem.digest(helper)}
    baseline["pointUpdate"] = {key: result[key] for key in ("folder", "archive", "sha256")}
    jem.write_json_atomic(jem.baseline_path(config), baseline)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

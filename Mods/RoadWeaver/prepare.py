import argparse
import hashlib
import json
import os
import shutil
import subprocess
import urllib.request
from pathlib import Path


MODULE = Path(__file__).resolve().parent
ROOT = MODULE.parents[1]
CONFIG = MODULE / "upstream.json"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--build", action="store_true")
    arguments = parser.parse_args()
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    if not arguments.build:
        print("Use --build to create the patched RoadWeaver artifact")
        return

    build = MODULE / "build"
    upstream = build / "upstream" / config["upstreamFile"]
    upstream.parent.mkdir(parents=True, exist_ok=True)
    if not upstream.is_file() or sha256(upstream) != config["upstreamSha256"]:
        with urllib.request.urlopen(config["upstreamUrl"]) as response:
            upstream.write_bytes(response.read())
    if sha256(upstream) != config["upstreamSha256"]:
        raise SystemExit("Downloaded RoadWeaver artifact does not match the pinned SHA-256")

    minecraft = Path.home() / "AppData/Roaming/.minecraft/libraries/net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-srg.jar"
    asm = Path.home() / "AppData/Roaming/.minecraft/libraries/org/ow2/asm/asm/9.5/asm-9.5.jar"
    asm_tree = Path.home() / "AppData/Roaming/.minecraft/libraries/org/ow2/asm/asm-tree/9.5/asm-tree-9.5.jar"
    for dependency in (minecraft, asm, asm_tree):
        if not dependency.is_file():
            raise SystemExit(f"Missing build dependency: {dependency}")

    classes = build / "classes"
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    helper = MODULE / "src/net/shiroha233/roadweaver/features/path/pathlogic/surface/GroundAnchoredHeightProfile.java"
    patcher = MODULE / "tools/PatchRoadWeaver.java"
    compile_classpath = os.pathsep.join((str(minecraft), str(upstream)))
    subprocess.run(["javac", "--release", "17", "-cp", compile_classpath, "-d", str(classes), str(helper)], check=True)
    asm_classpath = os.pathsep.join((str(asm), str(asm_tree)))
    subprocess.run(["javac", "--release", "17", "-cp", asm_classpath, "-d", str(classes), str(patcher)], check=True)

    output = build / "libs" / config["distributionName"]
    runtime_classpath = os.pathsep.join((str(classes), str(asm), str(asm_tree)))
    subprocess.run(["java", "-cp", runtime_classpath, "PatchRoadWeaver", str(upstream), str(output), str(classes)], check=True)
    print(f"RoadWeaver patched artifact: {output}")
    print(f"SHA-256: {sha256(output)}")


if __name__ == "__main__":
    main()

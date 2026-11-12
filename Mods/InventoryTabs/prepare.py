import argparse
import hashlib
import io
import json
import os
import shutil
import subprocess
import tarfile
import urllib.request
from pathlib import Path


MODULE = Path(__file__).resolve().parent
CONFIG = MODULE / "upstream.json"
PATCH = MODULE / "patches" / "loaded-chunks-only.patch"


def prepare(config, source):
    destination = MODULE / "build" / "upstream"
    receipt = destination / "jem-source.json"
    identity = {
        "commit": config["commit"],
        "patchSha256": hashlib.sha256(PATCH.read_bytes()).hexdigest(),
    }
    if receipt.is_file():
        previous = json.loads(receipt.read_text(encoding="utf-8"))
        if all(previous.get(key) == value for key, value in identity.items()):
            return destination
    if not destination.exists():
        if source is not None:
            archive = subprocess.run(
                ["git", "-C", str(source), "archive", config["commit"]],
                capture_output=True,
                check=True,
            ).stdout
            strip_root = False
        else:
            with urllib.request.urlopen(config["archive"]) as response:
                archive = response.read()
            strip_root = True
        destination.mkdir(parents=True)
        with tarfile.open(fileobj=io.BytesIO(archive)) as files:
            for item in files:
                parts = Path(item.name).parts[1:] if strip_root else Path(item.name).parts
                if not parts or any(part in {"test", "tests", "gametest", ".github"} for part in parts):
                    continue
                if item.isdir():
                    continue
                if not item.isfile():
                    raise SystemExit(f"Unsupported upstream entry: {item.name}")
                target = destination.joinpath(*parts).resolve()
                if not target.is_relative_to(destination.resolve()):
                    raise SystemExit(f"Upstream entry escapes its workspace: {item.name}")
                target.parent.mkdir(parents=True, exist_ok=True)
                with files.extractfile(item) as content:
                    target.write_bytes(content.read())
    for relative, expected in config["sourceHashes"].items():
        actual = hashlib.sha256((destination / relative).read_bytes()).hexdigest()
        if actual != expected:
            raise SystemExit(f"Pinned upstream source differs: {relative}")
    patch_environment = dict(os.environ)
    patch_environment["GIT_CEILING_DIRECTORIES"] = str(destination.parent.resolve())
    subprocess.run(["git", "apply", str(PATCH)], cwd=destination, env=patch_environment, check=True)
    receipt.write_text(json.dumps(identity, indent=2) + "\n", encoding="utf-8")
    return destination


def run_gradle(source, arguments, environment):
    wrapper = source / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [str(wrapper), *arguments]
    if os.name != "nt":
        command.insert(0, "sh")
    subprocess.run(command, cwd=source, env=environment, check=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path)
    parser.add_argument("--build", action="store_true")
    parser.add_argument("--java-home", type=Path)
    args = parser.parse_args()
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    source = prepare(config, args.source)
    print(f"Inventory Tabs source prepared: {source}")
    if args.build:
        environment = dict(os.environ)
        if args.java_home is not None:
            environment["JAVA_HOME"] = str(args.java_home.resolve())
            environment["PATH"] = str(args.java_home.resolve() / "bin") + os.pathsep + environment.get("PATH", "")
        version_argument = f"-Pmod_version={config['version']}"
        # Upstream keeps its ForgeGradle cache below build/. A single
        # `clean reobfJar` invocation deletes the resolved mapped JAR between
        # configuration and compilation, so cleanup and build must be separate.
        run_gradle(source, ["clean", "--no-daemon", version_argument], environment)
        run_gradle(source, [*config["buildArguments"], version_argument], environment)
        destination = MODULE / "build" / "libs" / config["distributionName"]
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source / config["artifact"], destination)
        print(f"Inventory Tabs patched artifact: {destination}")


if __name__ == "__main__":
    main()

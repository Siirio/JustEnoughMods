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
PATCH = MODULE / "patches" / "java17-runtime.patch"


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
    if destination.exists():
        raise SystemExit(f"Prepared source differs from this patch. Remove only {destination} before preparing again")
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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path)
    parser.add_argument("--build", action="store_true")
    parser.add_argument("--java-home", type=Path)
    args = parser.parse_args()
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    source = prepare(config, args.source)
    print(f"Sift Backport source prepared: {source}")
    if args.build:
        environment = dict(os.environ)
        if args.java_home is not None:
            environment["JAVA_HOME"] = str(args.java_home.resolve())
        build_directory = source / config["buildDirectory"]
        wrapper = build_directory / ("gradlew.bat" if os.name == "nt" else "gradlew")
        command = [str(wrapper), *config["buildArguments"]]
        if os.name != "nt":
            command.insert(0, "sh")
        subprocess.run(command, cwd=build_directory, env=environment, check=True)
        destination = MODULE / "build" / "libs" / config["distributionName"]
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source / config["artifact"], destination)
        print(f"Sift Backport Java 17 artifact: {destination}")


if __name__ == "__main__":
    main()

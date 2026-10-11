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
PATCH = MODULE / "patches" / "reliable-updates.patch"


def prepare(config, source):
    destination = MODULE / "build" / "upstream"
    receipt = destination / "jem-source.json"
    identity = {
        "commit": config["commit"],
        "patchSha256": hashlib.sha256(PATCH.read_bytes()).hexdigest(),
    }
    if receipt.is_file() and all(json.loads(receipt.read_text(encoding="utf-8")).get(key) == value for key, value in identity.items()):
        return destination
    if destination.exists():
        raise SystemExit(f"Prepared source differs from this patch; remove only {destination} before preparing again")
    if source is not None:
        archive = subprocess.run(["git", "-C", str(source), "archive", config["commit"]], capture_output=True, check=True).stdout
        strip_root = False
    else:
        with urllib.request.urlopen(config["archive"]) as response:
            archive = response.read()
        strip_root = True
    destination.mkdir(parents=True)
    with tarfile.open(fileobj=io.BytesIO(archive)) as files:
        for item in files:
            parts = Path(item.name).parts[1:] if strip_root else Path(item.name).parts
            if not parts or any(part in {"test", "tests", "autotester", "testFixtures", ".github", "docs"} for part in parts):
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
        if hashlib.sha256((destination / relative).read_bytes()).hexdigest() != expected:
            raise SystemExit(f"Pinned upstream source differs: {relative}")
    subprocess.run(["git", "apply", "--ignore-space-change", str(PATCH)], cwd=destination, check=True)
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
    print(f"Native updater source prepared: {source}")
    if args.build:
        environment = dict(os.environ)
        if args.java_home is not None:
            environment["JAVA_HOME"] = str(args.java_home.resolve())
        subprocess.run([
            str(source / "gradlew.bat"), *config["buildArguments"],
            f"-Pmod_version={config['version']}", f"-PjemBootstrapRevision={config['revision']}",
            f"-PjemUpstreamCommit={config['commit']}", "--init-script", str(MODULE / "production.gradle"),
        ], cwd=source, env=environment, check=True)
        artifact = source / "merged" / f"automodpack-{config['version']}.jar"
        destination = MODULE / "build" / "libs" / "jem-bootstrap.jar"
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(artifact, destination)
        print(f"Native updater artifact: {destination}")


if __name__ == "__main__":
    main()

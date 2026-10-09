from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--resources", type=Path, required=True)
    parser.add_argument("--index", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    args = parser.parse_args()

    resources = args.resources.resolve()
    target = args.target.resolve()
    expected_parent = (target.parents[2] / "paxi" / "datapacks").resolve()
    target.relative_to(expected_parent)
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)
    (target / "pack.mcmeta").write_text(
        json.dumps({"pack": {"pack_format": 15, "description": "JEM native advancement placement overrides"}}, indent=2) + "\n",
        encoding="utf-8",
    )
    document = json.loads(args.index.read_text(encoding="utf-8"))
    for relative in document["files"]:
        source = (resources / relative).resolve()
        source.relative_to(resources)
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
    print(json.dumps({"copied": len(document["files"]), "target": str(target)}))


if __name__ == "__main__":
    main()

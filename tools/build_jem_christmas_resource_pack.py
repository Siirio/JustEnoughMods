#!/usr/bin/env python3
"""Build the private JEM Christmas overlay from user-supplied resource packs."""

from __future__ import annotations

import argparse
import hashlib
import json
import zipfile
from collections import Counter
from pathlib import Path, PurePosixPath


IGNORED_NAMES = {".DS_Store", "Thumbs.db"}
ROOT_METADATA = {"pack.mcmeta", "pack.png"}
SOURCE_METADATA = {
    "credits.txt",
    "license",
    "readme.md",
    "readme.txt",
    "terms_of_use.md",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalized_entry(name: str) -> str | None:
    value = name.replace("\\", "/").lstrip("/")
    if not value or value.endswith("/"):
        return None
    parts = PurePosixPath(value).parts
    if ".." in parts or parts[0] == "__MACOSX" or parts[-1] in IGNORED_NAMES:
        return None
    return "/".join(parts)


def metadata_path(source_name: str, entry_name: str) -> str:
    return f"JEM_CHRISTMAS_SOURCES/{source_name}/{PurePosixPath(entry_name).name}"


def add_archive(
    source_name: str,
    source_path: Path,
    files: dict[str, tuple[str, bytes]],
    collisions: Counter[tuple[str, str]],
) -> int:
    added = 0
    with zipfile.ZipFile(source_path) as archive:
        for info in archive.infolist():
            entry_name = normalized_entry(info.filename)
            if entry_name is None:
                continue
            lower = entry_name.lower()
            if lower in ROOT_METADATA:
                if lower == "pack.mcmeta":
                    files[metadata_path(source_name, entry_name)] = (source_name, archive.read(info))
                continue
            if "/" not in entry_name and lower in SOURCE_METADATA:
                files[metadata_path(source_name, entry_name)] = (source_name, archive.read(info))
                continue

            previous = files.get(entry_name)
            if previous is not None:
                collisions[(previous[0], source_name)] += 1
            files[entry_name] = (source_name, archive.read(info))
            added += 1
    return added


def write_entry(archive: zipfile.ZipFile, name: str, data: bytes) -> None:
    info = zipfile.ZipInfo(name, (2026, 11, 28, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o644 << 16
    archive.writestr(info, data)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-pack", type=Path, required=True)
    parser.add_argument("--background-music", type=Path, required=True)
    parser.add_argument("--winter-pack", type=Path, required=True)
    parser.add_argument("--christmas-pack", type=Path, required=True)
    parser.add_argument("--hat-pack", type=Path, required=True)
    parser.add_argument("--gui-pack", type=Path, required=True)
    parser.add_argument("--hotbar-pack", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    sources = [
        ("christmas_background_music", args.background_music),
        ("winter_pack", args.winter_pack),
        ("christmas_pack", args.christmas_pack),
        ("christmas_hat", args.hat_pack),
        ("christmas_gui", args.gui_pack),
        ("christmas_hotbar", args.hotbar_pack),
    ]
    required = [args.base_pack, *(path for _, path in sources)]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise SystemExit("Missing required source: " + ", ".join(missing))

    files: dict[str, tuple[str, bytes]] = {}
    collisions: Counter[tuple[str, str]] = Counter()
    source_manifest = []
    for source_name, source_path in sources:
        source_manifest.append(
            {
                "name": source_name,
                "filename": source_path.name,
                "sha256": sha256(source_path),
                "entries_applied": add_archive(source_name, source_path, files, collisions),
            }
        )

    with zipfile.ZipFile(args.christmas_pack) as source:
        icon = source.read("pack.png")

    manifest = {
        "name": "JEM Christmas",
        "base_pack": {
            "filename": args.base_pack.name,
            "sha256": sha256(args.base_pack),
            "relationship": "Selected below JEM Christmas by JEM Holidays and not copied into this overlay",
        },
        "overlay_order_low_to_high": [name for name, _ in sources],
        "sources": source_manifest,
        "collisions": [
            {"from": previous, "to": replacement, "count": count}
            for (previous, replacement), count in sorted(collisions.items())
        ],
        "excluded": [
            {
                "filename": "ChristmasChestAllYear v.15.1.zip",
                "reason": "Its bundled license prohibits redistribution without creator permission",
            }
        ],
        "final_entries": len(files),
    }
    pack_meta = {
        "pack": {
            "pack_format": 15,
            "description": "JEM Christmas | Nov 28 - Jan 28",
        }
    }
    credits = (
        "JEM Christmas is a private seasonal overlay assembled from user-supplied packs.\n"
        "Original pack metadata and available credits are retained under JEM_CHRISTMAS_SOURCES/.\n"
        "JEM Release v0.3 remains the separate base layer.\n"
        "ChristmasChestAllYear is not included because its license forbids redistribution without permission.\n"
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    with zipfile.ZipFile(temporary, "w") as output:
        write_entry(output, "pack.mcmeta", json.dumps(pack_meta, ensure_ascii=False, indent=2).encode("utf-8"))
        write_entry(output, "pack.png", icon)
        write_entry(output, "JEM_CHRISTMAS_CREDITS.txt", credits.encode("utf-8"))
        write_entry(
            output,
            "jem_christmas_manifest.json",
            json.dumps(manifest, ensure_ascii=False, indent=2).encode("utf-8"),
        )
        for name in sorted(files):
            write_entry(output, name, files[name][1])
    temporary.replace(args.output)

    print(json.dumps({"output": str(args.output), "sha256": sha256(args.output), **manifest}, indent=2))


if __name__ == "__main__":
    main()

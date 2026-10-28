#!/usr/bin/env python3
"""Build the private JEM Release GUI resource pack from selected source packs."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import posixpath
import zipfile
from collections import Counter
from pathlib import Path, PurePosixPath
from typing import Callable

from PIL import Image


PACKS = {
    "immersive_interfaces": "%C2%A76Immersive_Interfaces_Modded.zip",
    "gui_experience": "%C2%A7lGUI%20Experience%2B%C2%A70.zip",
    "reimagined_gui": "Reimagined GUI - Light - 1.20.1.zip",
    "icons": "Icons%20v.1.14.zip",
    "hud_reimagined": "§bHUD§8-§bReimagined§0_§8[v1]§0.zip",
    "effect_icons": "§6Effect Icons Recrafted§8.zip",
}

ROOT_EXCLUDES = {
    "pack.mcmeta",
    "pack.png",
    "credits.txt",
    "discord server.txt",
    "readme.md",
    "readme.txt",
    "terms_of_use.md",
    "credits.md",
    "whatareyoudoinghere.png",
}

PLAYER_INTERFACE_PATHS = {
    "assets/minecraft/textures/gui/container/inventory.png",
    "assets/minecraft/textures/gui/recipe_book.png",
    "assets/minecraft/textures/gui/recipe_button.png",
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalized_entry(name: str) -> str | None:
    name = name.replace("\\", "/").lstrip("/")
    if not name or name.endswith("/"):
        return None
    parts = PurePosixPath(name).parts
    if any(part in {"", ".", ".."} for part in parts):
        raise ValueError(f"Unsafe ZIP entry: {name}")
    return "/".join(parts)


def assets_only(path: str) -> bool:
    return path.startswith("assets/")


def immersive_interfaces_entry(path: str) -> bool:
    lowered = path.lower()
    if not lowered.startswith("assets/"):
        return False
    # JEI owns its responsive search/ingredient-list sprites. Immersive's fixed
    # panels are larger than JEI's runtime rectangles and visibly stack below them.
    if lowered.startswith("assets/jei/"):
        return False
    # Cosmetic Armor keeps its native inventory contract instead of inheriting
    # Immersive Interfaces merely because the composite pack has higher priority.
    return not lowered.startswith("assets/cosmeticarmorreworked/")


def icons_pack_entry(path: str) -> bool:
    lowered = path.lower()
    return (
        path.startswith("assets/")
        or path.startswith("1.20.2/")
        or path.startswith("26.2/")
        or lowered == "respackopts.json5"
    )


def reimagined_gui_entry(path: str) -> bool:
    lowered = path.lower()
    if not lowered.startswith("assets/"):
        return False
    if lowered in PLAYER_INTERFACE_PATHS:
        return True
    if lowered.startswith("assets/minecraft/textures/gui/container/creative_inventory/"):
        return True
    if lowered == "assets/curios/textures/gui/inventory_revamp.png":
        return True
    if lowered.startswith("assets/inventorio/textures/gui/player_inventory"):
        return True
    if lowered.startswith("assets/backslot/textures/gui/"):
        return True
    if "/textures/gui/" not in lowered:
        return False
    filename = posixpath.basename(lowered)
    return (
        "button" in filename
        or "widget" in filename
        or filename in {"checkbox.png", "slider.png"}
    )


def hud_entry(path: str) -> bool:
    return path.startswith("assets/") and path != "assets/minecraft/textures/gui/widgets.png"


def read_entry(archive: Path, entry_name: str) -> bytes:
    with zipfile.ZipFile(archive) as source:
        return source.read(entry_name)


def composite_widgets(reimagined: bytes, hud: bytes) -> bytes:
    with Image.open(io.BytesIO(reimagined)) as reimagined_image:
        result = reimagined_image.convert("RGBA")
    with Image.open(io.BytesIO(hud)) as hud_image:
        hud_rgba = hud_image.convert("RGBA")
    if result.size != (256, 256) or hud_rgba.size != result.size:
        raise ValueError(
            f"Unexpected widgets.png dimensions: Reimagined={result.size}, HUD={hud_rgba.size}"
        )
    # Minecraft 1.20.1 keeps HUD/hotbar sprites in rows 0..45 and button states below.
    # Copying this exact native rectangle preserves HUD Reimagined while retaining
    # Reimagined GUI's button rows and player-interface controls.
    result.paste(hud_rgba.crop((0, 0, 256, 46)), (0, 0))
    output = io.BytesIO()
    result.save(output, format="PNG", optimize=False)
    return output.getvalue()


def is_hybrid_interface_path(path: str) -> bool:
    lowered = path.lower()
    return (
        lowered.startswith("assets/minecraft/textures/gui/container/")
        and lowered.endswith(".png")
        and not lowered.endswith("/stats_icons.png")
        and "/creative_inventory/" not in lowered
    )


def is_dark_reimagined_recess(pixel: tuple[int, int, int, int]) -> bool:
    red, green, blue, alpha = pixel
    return alpha == 255 and max(red, green, blue) - min(red, green, blue) <= 6 and 45 <= red <= 80


def is_light_gui_experience_center(pixel: tuple[int, int, int, int]) -> bool:
    red, green, blue, alpha = pixel
    return alpha == 255 and max(red, green, blue) - min(red, green, blue) <= 6 and 120 <= red <= 170


def connected_components(mask: list[bool], width: int, height: int) -> list[list[int]]:
    components: list[list[int]] = []
    visited = [False] * len(mask)
    for start, enabled in enumerate(mask):
        if not enabled or visited[start]:
            continue
        stack = [start]
        component: list[int] = []
        visited[start] = True
        while stack:
            index = stack.pop()
            component.append(index)
            x = index % width
            y = index // width
            for neighbor_x, neighbor_y in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                if not (0 <= neighbor_x < width and 0 <= neighbor_y < height):
                    continue
                neighbor = neighbor_y * width + neighbor_x
                if mask[neighbor] and not visited[neighbor]:
                    visited[neighbor] = True
                    stack.append(neighbor)
        components.append(component)
    return components


def composite_interface(gui_experience: bytes, reimagined: bytes) -> tuple[bytes, int, int]:
    with Image.open(io.BytesIO(gui_experience)) as gui_image:
        result = gui_image.convert("RGBA")
    with Image.open(io.BytesIO(reimagined)) as reimagined_image:
        recess_source = reimagined_image.convert("RGBA")
    if result.size != recess_source.size:
        raise ValueError(
            f"Interface dimensions differ: GUI Experience={result.size}, Reimagined={recess_source.size}"
        )
    width, height = result.size
    gui_pixels = list(result.get_flattened_data())
    reimagined_pixels = list(recess_source.get_flattened_data())
    candidate_mask = [
        is_light_gui_experience_center(gui_pixel) and is_dark_reimagined_recess(reimagined_pixel)
        for gui_pixel, reimagined_pixel in zip(gui_pixels, reimagined_pixels)
    ]
    recess_slots: list[tuple[int, int, int, int]] = []
    for component in connected_components(candidate_mask, width, height):
        xs = [index % width for index in component]
        ys = [index // width for index in component]
        left, right = min(xs), max(xs)
        top, bottom = min(ys), max(ys)
        component_width = right - left + 1
        component_height = bottom - top + 1
        if len(component) < 200 or not (14 <= component_width <= 18 and 14 <= component_height <= 18):
            continue
        recess_slots.append(
            (
                max(0, left - 1),
                max(0, top - 1),
                min(width, right + 2),
                min(height, bottom + 2),
            )
        )

    changed_indexes: set[int] = set()
    if len(recess_slots) == 27:
        frame_top = min(top for _, top, _, _ in recess_slots)
        frame_bottom = max(bottom for _, _, _, bottom in recess_slots)
        panel_left = 0
        panel_right = min(width, 176)
        panel_top = frame_top
        panel_bottom = min(height, frame_bottom + 1)

        # Retain GUI Experience's five-pixel light upper ledge, then take the
        # Reimagined bank below it. This connects the recessed grid to the upper
        # interface without restoring the rejected light side and bottom rails.
        for y in range(panel_top, panel_bottom):
            for x in range(panel_left, panel_right):
                index = y * width + x
                gui_pixels[index] = reimagined_pixels[index]
                changed_indexes.add(index)
    else:
        for left, top, right, bottom in recess_slots:
            for y in range(top, bottom):
                for x in range(left, right):
                    index = y * width + x
                    gui_pixels[index] = reimagined_pixels[index]
                    changed_indexes.add(index)
    result.putdata(gui_pixels)
    output = io.BytesIO()
    result.save(output, format="PNG", optimize=False)
    return output.getvalue(), len(changed_indexes), len(recess_slots)


def pack_metadata(release_version: str) -> bytes:
    payload = {
        "pack": {
            "pack_format": 15,
            "supported_formats": [15, 200],
            "description": f"JEM Release v{release_version} | unified GUI, HUD and icon resources",
        },
        "overlays": {
            "entries": [
                {
                    "directory": "1.20.2",
                    "formats": [18, 200],
                    "min_format": 18,
                    "max_format": 200,
                },
                {
                    "directory": "26.2",
                    "formats": [88, 200],
                    "min_format": [88, 0],
                    "max_format": [200, 0],
                },
            ]
        },
    }
    return (json.dumps(payload, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def credits_text(source_hashes: dict[str, dict[str, str]]) -> bytes:
    lines = [
        "JEM Release private composite resource pack",
        "",
        "This local pack combines unmodified source-pack assets according to the user's",
        "selected precedence. It is for private JEM sandbox use and is not licensed for",
        "redistribution or re-upload as a standalone edited asset pack.",
        "",
        "Sources:",
        "- Reimagined GUI - Light by AmongstReality and credited contributors",
        "- GUI Experience + by Kryqu",
        "- Immersive Interfaces Modded",
        "- HUD Reimagined by Reijvi",
        "- Effect Icons Recrafted",
        "- Icons by WeNAN Studios and its credited contributors",
        "",
        "Exact local source identities:",
    ]
    for source, identity in source_hashes.items():
        lines.append(f"- {source}: {identity['filename']} | SHA-256 {identity['sha256']}")
    lines.append("")
    return "\n".join(lines).encode("utf-8")


def merge_archive(
    archive: Path,
    owner: str,
    selector: Callable[[str], bool],
    files: dict[str, bytes],
    owners: dict[str, str],
    collisions: list[dict[str, str]],
) -> int:
    added = 0
    with zipfile.ZipFile(archive) as source:
        for member in source.infolist():
            path = normalized_entry(member.filename)
            if path is None or path.lower() in ROOT_EXCLUDES or not selector(path):
                continue
            data = source.read(member)
            previous = owners.get(path)
            if previous is not None and files[path] != data:
                collisions.append({"path": path, "previous": previous, "winner": owner})
            files[path] = data
            owners[path] = owner
            added += 1
    return added


def validate_pngs(files: dict[str, bytes]) -> tuple[int, list[str]]:
    checked = 0
    failures: list[str] = []
    for path, data in files.items():
        if not path.lower().endswith(".png"):
            continue
        try:
            with Image.open(io.BytesIO(data)) as image:
                image.verify()
                if image.width <= 0 or image.height <= 0:
                    failures.append(f"{path}: invalid dimensions {image.size}")
        except Exception as error:
            failures.append(f"{path}: {error}")
        checked += 1
    return checked, failures


def write_zip(output: Path, files: dict[str, bytes]) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as target:
        for path in sorted(files):
            info = zipfile.ZipInfo(path, date_time=(2026, 10, 27, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            target.writestr(info, files[path])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--resourcepacks-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--title", type=Path)
    parser.add_argument("--audit", type=Path, required=True)
    parser.add_argument("--release-version", default="0.1")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source_paths = {name: args.resourcepacks_dir / filename for name, filename in PACKS.items()}
    missing = [str(path) for path in source_paths.values() if not path.is_file()]
    if missing:
        raise FileNotFoundError("Missing source packs:\n" + "\n".join(missing))

    source_hashes = {
        name: {"filename": path.name, "sha256": file_sha256(path)}
        for name, path in source_paths.items()
    }
    files: dict[str, bytes] = {}
    owners: dict[str, str] = {}
    collisions: list[dict[str, str]] = []
    selected_counts: dict[str, int] = {}

    selected_counts["immersive_interfaces"] = merge_archive(
        source_paths["immersive_interfaces"],
        "immersive_interfaces",
        immersive_interfaces_entry,
        files,
        owners,
        collisions,
    )
    selected_counts["gui_experience"] = merge_archive(
        source_paths["gui_experience"],
        "gui_experience",
        assets_only,
        files,
        owners,
        collisions,
    )
    selected_counts["reimagined_gui"] = merge_archive(
        source_paths["reimagined_gui"],
        "reimagined_gui",
        reimagined_gui_entry,
        files,
        owners,
        collisions,
    )
    selected_counts["icons"] = merge_archive(
        source_paths["icons"],
        "icons",
        icons_pack_entry,
        files,
        owners,
        collisions,
    )
    selected_counts["hud_reimagined"] = merge_archive(
        source_paths["hud_reimagined"],
        "hud_reimagined",
        hud_entry,
        files,
        owners,
        collisions,
    )
    selected_counts["effect_icons"] = merge_archive(
        source_paths["effect_icons"],
        "effect_icons",
        assets_only,
        files,
        owners,
        collisions,
    )

    hybrid_interfaces: dict[str, dict[str, int]] = {}
    with zipfile.ZipFile(source_paths["gui_experience"]) as gui_source, zipfile.ZipFile(
        source_paths["reimagined_gui"]
    ) as reimagined_source:
        gui_entries = {name for name in gui_source.namelist() if is_hybrid_interface_path(name)}
        reimagined_entries = {
            name for name in reimagined_source.namelist() if is_hybrid_interface_path(name)
        }
        for path in sorted(gui_entries & reimagined_entries):
            try:
                composite, changed_pixels, recess_slots = composite_interface(
                    gui_source.read(path), reimagined_source.read(path)
                )
            except ValueError:
                continue
            files[path] = composite
            owners[path] = "composite:gui_experience_shell+reimagined_player_bank"
            hybrid_interfaces[path] = {
                "reimagined_player_bank_pixels": changed_pixels,
                "detected_recessed_slots": recess_slots,
            }

    widgets_path = "assets/minecraft/textures/gui/widgets.png"
    files[widgets_path] = composite_widgets(
        read_entry(source_paths["reimagined_gui"], widgets_path),
        read_entry(source_paths["hud_reimagined"], widgets_path),
    )
    owners[widgets_path] = "composite:hud_rows_0_45+reimagined_buttons"

    if args.title:
        if not args.title.is_file():
            raise FileNotFoundError(args.title)
        title_path = "assets/jem_release/textures/gui/title/jem_release_title.png"
        title_data = args.title.read_bytes()
        with Image.open(io.BytesIO(title_data)) as title_image:
            title_image.verify()
        files[title_path] = title_data
        owners[title_path] = "jem_release_title_candidate"

    files["pack.mcmeta"] = pack_metadata(args.release_version)
    owners["pack.mcmeta"] = "jem_release"
    files["JEM_RELEASE_CREDITS.txt"] = credits_text(source_hashes)
    owners["JEM_RELEASE_CREDITS.txt"] = "jem_release"

    required_winners = {
        "assets/minecraft/textures/gui/container/inventory.png": "composite:gui_experience_shell+reimagined_player_bank",
        "assets/minecraft/textures/gui/container/anvil.png": "composite:gui_experience_shell+reimagined_player_bank",
        "assets/minecraft/textures/gui/container/creative_inventory/tab_inventory.png": "reimagined_gui",
        "assets/accessories/textures/gui/container/accessories_inventory.png": "immersive_interfaces",
        "assets/minecraft/textures/gui/icons.png": "hud_reimagined",
        "assets/minecraft/textures/mob_effect/absorption.png": "effect_icons",
        "assets/icons/textures/menu/main_menu.png": "icons",
        widgets_path: "composite:hud_rows_0_45+reimagined_buttons",
    }
    winner_failures = [
        f"{path}: expected {expected}, got {owners.get(path)}"
        for path, expected in required_winners.items()
        if owners.get(path) != expected
    ]
    png_count, png_failures = validate_pngs(files)
    if winner_failures or png_failures:
        raise RuntimeError(
            "Validation failed:\n" + "\n".join(winner_failures + png_failures)
        )

    manifest = {
        "name": f"JEM Release v{args.release_version}",
        "status": "private-local-build",
        "minecraft_target": "1.20.1",
        "redistribution_allowed": False,
        "precedence": [
            "effect_icons for status-effect icons",
            "hud_reimagined for HUD textures",
            "icons for icon-language assets",
            "reimagined_gui for buttons and player interfaces",
            "gui_experience for every supplied block GUI",
            "immersive_interfaces for remaining modded GUIs",
        ],
        "hybrid_interface_rule": {
            "base": "GUI Experience paper titles, transparent frames and functional crafting art",
            "recess": "GUI Experience five-pixel light upper ledge plus the complete Reimagined GUI player bank below it, identified from 27 connected 16 by 16 wells",
            "minimum_connected_recess_area": 200,
            "interfaces": hybrid_interfaces,
        },
        "special_composites": {
            widgets_path: {
                "rows_0_45": "hud_reimagined",
                "rows_46_255": "reimagined_gui",
            }
        },
        "sources": source_hashes,
        "selected_entry_counts": selected_counts,
        "final_owner_counts": dict(sorted(Counter(owners.values()).items())),
        "collision_count": len(collisions),
        "collisions": collisions,
        "required_winners": required_winners,
        "validated_png_count": png_count,
        "final_file_count": len(files) + 1,
        "includes_title_candidate": bool(args.title),
    }
    files["jem_release_manifest.json"] = (
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    ).encode("utf-8")
    owners["jem_release_manifest.json"] = "jem_release"

    write_zip(args.output, files)
    with zipfile.ZipFile(args.output) as built:
        bad_member = built.testzip()
        if bad_member:
            raise RuntimeError(f"ZIP CRC validation failed at {bad_member}")
        final_entries = len(built.infolist())

    audit = {
        "output": str(args.output),
        "output_sha256": file_sha256(args.output),
        "output_bytes": args.output.stat().st_size,
        "zip_entries": final_entries,
        "validated_png_count": png_count,
        "winner_failures": winner_failures,
        "png_failures": png_failures,
        "zip_crc_failure": None,
        "source_hashes": source_hashes,
        "required_winners": required_winners,
        "result": "pass",
        "runtime_verified": False,
        "user_approved": False,
    }
    args.audit.parent.mkdir(parents=True, exist_ok=True)
    args.audit.write_text(json.dumps(audit, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(audit, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

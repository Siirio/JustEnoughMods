#!/usr/bin/env python3
"""Build and validate the exact JEM JEI item-to-leaf manifest."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "Mods" / "JEMCompat" / "src" / "main" / "resources" / "assets" / "jemcompat" / "jei_catalog.json"
REPORT = ROOT / "Docs" / "catalog" / "JEI_CATALOG_REVIEW.md"
SCHEMA_VERSION = 1
LEAVES = (
    "WEAPON_TOTEMS",
    "WEAPON_TRAINING",
    "WEAPON_MELEE",
    "WEAPON_RANGED",
    "WEARABLES",
    "FOOD_EDIBLES",
    "FOOD_COOKING_EQUIPMENT",
    "FISHING",
    "TRANSPORT",
    "BUILDING_CONTAINERS",
    "POTIONS",
    "BOOKS",
    "ORES",
    "CREATE_REDSTONE",
    "BUILDING_FURNITURE",
    "BUILDING_NATURE",
    "BUILDING_GLASS",
    "BUILDING_LIGHT",
    "BUILDING_HOLIDAY",
    "BUILDING_BLOCKS",
    "SPAWN_EGGS",
    "OTHER",
)
PARENTS = {"WEAPONS", "FOOD", "BUILDING"}
ID_PATTERN = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
SKARRIER_TOTEMS = {
    "skarrier_mobs:totem_of_extinction",
    "skarrier_mobs:totem_of_freezing",
    "skarrier_mobs:totem_of_melding",
    "skarrier_mobs:totem_of_repulsion",
    "skarrier_mobs:totem_of_seething",
}
EXACT = {
    "companions:book_ice_shard": "WEARABLES",
    "legendary_monsters:totem_of_moss": "OTHER",
    "legendary_monsters:eye_of_moss": "OTHER",
    "spawn:ant_crystal": "OTHER",
    "spawn:moonshine": "OTHER",
    "spawn:moonstone": "OTHER",
    **{item_id: "WEAPON_TOTEMS" for item_id in SKARRIER_TOTEMS},
}
OLD_LEAF_MAP = {
    "WEAPON_TOTEMS": "WEAPON_TOTEMS",
    "WEAPON_RANGED": "WEAPON_RANGED",
    "WEAPON_MELEE": "WEAPON_MELEE",
    "WEARABLE": "WEARABLES",
    "FOOD": "FOOD_COOKING_EQUIPMENT",
    "ORES": "ORES",
    "CREATE_REDSTONE": "CREATE_REDSTONE",
    "FURNITURE": "BUILDING_FURNITURE",
    "NATURE": "BUILDING_NATURE",
    "CONTAINERS": "BUILDING_CONTAINERS",
    "HOLIDAY": "BUILDING_HOLIDAY",
    "BUILDING_BLOCKS": "BUILDING_BLOCKS",
    "GLASS": "BUILDING_GLASS",
    "LIGHT": "BUILDING_LIGHT",
    "FISHING": "FISHING",
    "BOOKS": "BOOKS",
    "POTIONS": "POTIONS",
    "TRANSPORT": "TRANSPORT",
    "SPAWN_EGGS": "SPAWN_EGGS",
    "OTHER": "OTHER",
}


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def words(path: str) -> set[str]:
    return {token for token in re.split(r"[^a-z0-9]+", path.casefold()) if token}


def has_phrase(path: str, *phrases: str) -> bool:
    padded = "_" + path.casefold().replace("/", "_") + "_"
    return any("_" + phrase + "_" in padded for phrase in phrases)


def has_tag(row: dict, *needles: str) -> bool:
    tags = {str(tag).casefold().removeprefix("items/") for tag in row.get("tags", [])}
    return any(any(needle in tag for needle in needles) for tag in tags)


def class_has(row: dict, *needles: str) -> bool:
    names = (str(row.get("class_name", "")) + " " + str(row.get("block_class_name", ""))).casefold()
    return any(needle.casefold() in names for needle in needles)


def is_block(row: dict) -> bool:
    return bool(row.get("block_item_id"))


def migration_hints(valid_ids: set[str]) -> dict[str, str]:
    if not MANIFEST.exists():
        return {}
    data = read_json(MANIFEST)
    result: dict[str, str] = {}
    for old_leaf, item_ids in data.get("assignments", {}).items():
        leaf = OLD_LEAF_MAP.get(old_leaf)
        if leaf is None:
            continue
        for item_id in item_ids:
            if item_id in valid_ids:
                result[item_id] = leaf
    return result


def classify(row: dict, hint: str | None) -> str:
    item_id = row["registry_id"]
    namespace, path = item_id.split(":", 1)
    token_set = words(path)

    if item_id in EXACT:
        return EXACT[item_id]
    if "hourglass" in token_set or {"sand", "clock"} <= token_set:
        return "OTHER"

    if class_has(row, "SpawnEggItem") or path.endswith("_spawn_egg"):
        return "SPAWN_EGGS"

    if item_id == "minecraft:totem_of_undying" or (
        namespace == "moretotems" and "totem" in token_set
    ):
        return "WEAPON_TOTEMS"

    if namespace == "recruits" and path in {
        "recruit_block",
        "recruit_shield_block",
        "bowman_block",
        "crossbowman_block",
        "horseman_block",
        "nomad_block",
    }:
        return "WEAPON_TRAINING"

    if "spear" in token_set and not ({"javelin", "trident"} & token_set):
        return "WEAPON_MELEE"
    if class_has(row, "SwordItem", "DiggerItem", "ShieldItem") or has_tag(
        row, "minecraft:swords", "forge:tools/swords", "forge:weapons"
    ) or has_phrase(
        path,
        "sword",
        "dagger",
        "mace",
        "battleaxe",
        "warhammer",
        "shield",
        "scythe",
        "halberd",
        "glaive",
        "katana",
        "claymore",
        "rapier",
        "shortsword",
        "greatsword",
        "saber",
        "blade",
    ):
        return "WEAPON_MELEE"

    if {"javelin", "trident"} & token_set or class_has(row, "BowItem", "CrossbowItem") or has_tag(
        row, "bows", "crossbows", "arrows", "spears", "throwable", "ranged"
    ) or has_phrase(
        path,
        "bow",
        "crossbow",
        "arrow",
        "bolt",
        "javelin",
        "trident",
        "chakram",
        "shuriken",
        "gun",
        "rifle",
        "pistol",
        "musket",
        "cannon",
        "launcher",
        "blaster",
        "slingshot",
    ):
        return "WEAPON_RANGED"

    if row.get("armor_slot") or class_has(
        row, "ArmorItem", "DollItem", "Curio", "BackpackItem", "Quiver"
    ) or has_tag(row, "curios:", "forge:armors", "minecraft:head_armor", "minecraft:chest_armor") or has_phrase(
        path,
        "player_head",
        "mob_head",
        "skull",
        "mask",
        "crown",
        "hat",
        "helmet",
        "chestplate",
        "leggings",
        "boots",
        "cape",
        "backpack",
        "quiver",
        "wearable",
        "white_doll",
    ):
        return "WEARABLES"

    if row.get("has_food_properties") or has_tag(
        row, "foods", "crops", "fruits", "vegetables", "drinks", "beverages", "meat"
    ) or has_phrase(
        path,
        "raw_meat",
        "cooked_meat",
        "steak",
        "chop",
        "fillet",
        "fruit",
        "vegetable",
        "meal",
        "sandwich",
        "burger",
        "soup",
        "stew",
        "drink",
        "juice",
        "tea",
        "coffee",
        "wine",
        "beer",
        "cocktail",
    ):
        return "FOOD_EDIBLES"

    cooking_namespace = namespace in {
        "bakery",
        "brewery",
        "farm_and_charm",
        "herbalbrews",
        "kaleidoscope_cookery",
        "vinery",
    }
    if class_has(row, "CookingPot", "Stove", "Oven", "CuttingBoard", "Millstone") or (
        cooking_namespace
        and has_phrase(
            path,
            "furnace",
            "oven",
            "stove",
            "grill",
            "cutting_board",
            "millstone",
            "cooking_pot",
            "cauldron",
            "brewery",
            "kitchen",
            "worktop",
            "basket",
            "crate",
            "cabinet",
            "barrel",
        )
    ):
        return "FOOD_COOKING_EQUIPMENT"

    if namespace == "tide" or class_has(row, "FishingRod", "FishingBobber", "Fishing") or has_tag(
        row, "minecraft:fishes"
    ) or has_phrase(
        path,
        "fishing_rod",
        "fish_display",
        "fish_frame",
        "fish_finder",
        "fishing_journal",
        "bait",
        "bobber",
        "fishing_hook",
        "fish_bucket",
    ) or path == "bucket" or path.endswith("_bucket"):
        return "FISHING"

    if class_has(row, "BoatItem", "MinecartItem", "SledItem", "Aircraft") or namespace in {
        "immersive_aircraft",
        "smallships",
        "trotting_wagons",
    } or has_phrase(
        path,
        "sled",
        "ship",
        "rowboat",
        "galley",
        "cog",
        "dhow",
        "drakkar",
        "brigg",
        "war_galley",
        "boat",
        "minecart",
        "wagon",
        "carriage",
        "aircraft",
        "biplane",
        "gyrodyne",
        "locomotive",
        "mount_control",
    ):
        return "TRANSPORT"

    if class_has(row, "ShulkerBox", "ChestBlock", "BarrelBlock", "ContainerBlock") or namespace in {
        "sophisticatedstorage",
        "toms_storage",
    } or has_phrase(
        path,
        "shulker_box",
        "bundle",
        "chest",
        "barrel",
        "crate",
        "sack",
        "box",
        "drawer",
        "cupboard",
        "cabinet",
        "storage",
        "basket",
    ):
        return "BUILDING_CONTAINERS"

    if class_has(row, "PotionItem") or has_phrase(
        path, "potion", "splash_potion", "lingering_potion", "elixir", "tonic", "status_flask"
    ):
        return "POTIONS"

    if class_has(row, "BookItem", "WrittenBookItem") or has_phrase(
        path, "book", "tome", "guide", "journal", "manual", "codex", "grimoire", "document"
    ):
        return "BOOKS"

    if path.endswith("_ore") or "_ore_" in path or path == "ancient_debris" or has_tag(
        row,
        "forge:ores",
        "forge:raw_materials",
        "forge:ingots",
        "forge:nuggets",
        "forge:gems",
        "forge:dusts",
        "forge:storage_blocks",
        "forge:plates",
        "forge:rods",
        "forge:wires",
        "c:ores",
        "c:raw_materials",
        "c:ingots",
        "c:nuggets",
        "c:gems",
        "c:dusts",
    ) or (not is_block(row) and has_phrase(
        path,
        "raw",
        "ingot",
        "nugget",
        "gem",
        "crystal",
        "dust",
        "powder",
        "crushed",
        "shard",
        "chunk",
        "plate",
        "sheet",
        "rod",
        "wire",
    )):
        return "ORES"

    create_family = namespace in {
        "create",
        "railways",
        "bellsandwhistles",
        "create_train_parts",
        "createrailwaysnavigator",
    }
    if namespace == "infinitybuttons" or item_id == "minecraft:tnt" or has_tag(
        row, "jemcompat:jei/create_redstone"
    ) or has_phrase(
        path,
        "redstone",
        "repeater",
        "comparator",
        "piston",
        "observer",
        "dispenser",
        "dropper",
        "hopper",
        "lever",
        "pressure_plate",
        "tripwire",
        "daylight_detector",
        "sculk_sensor",
        "target",
        "command_block",
        "button",
    ) or (
        create_family
        and has_phrase(
            path,
            "cogwheel",
            "shaft",
            "gearbox",
            "press",
            "deployer",
            "mixer",
            "basin",
            "motor",
            "fan",
            "pump",
            "pipe",
            "boiler",
            "flywheel",
            "drill",
            "saw",
            "belt",
            "conveyor",
            "millstone",
            "crushing_wheel",
            "steam_engine",
            "mechanical",
            "circuit",
        )
    ):
        return "CREATE_REDSTONE"

    if namespace in {"beachparty", "casualswing", "chimes", "secretdoors", "shutterup"} or has_phrase(
        path,
        "chair",
        "table",
        "table_cloth",
        "sideboard",
        "buffet",
        "counter",
        "countertop",
        "sofa",
        "couch",
        "stool",
        "bench",
        "curtain",
        "carpet",
        "painting",
        "decoration",
        "display",
        "parasol",
        "towel",
        "radio",
        "urn",
        "shelf",
        "banner",
        "sign",
        "bed",
        "cushion",
        "sheet",
        "desk",
        "pennant",
        "bunting",
    ):
        return "BUILDING_FURNITURE"

    if has_tag(
        row,
        "jemcompat:jei/nature",
        "minecraft:leaves",
        "minecraft:logs",
        "minecraft:saplings",
        "minecraft:flowers",
        "minecraft:corals",
        "minecraft:dirt",
    ) or has_phrase(
        path,
        "leaf_pile",
        "leaves",
        "log",
        "wood",
        "sapling",
        "flower",
        "grass",
        "coral",
        "natural_soil",
        "moss",
        "lichen",
        "ivy",
        "roots",
        "sprout",
        "vine",
        "seastar",
    ):
        return "BUILDING_NATURE"

    if has_tag(row, "forge:glass", "c:glass") or has_phrase(
        path, "glass", "glass_pane", "stained_glass", "framed_glass", "seaglass", "window", "window_pane"
    ):
        return "BUILDING_GLASS"

    if has_phrase(
        path, "torch", "lantern", "candle", "light", "lamp", "chandelier", "sconce", "brazier"
    ):
        return "BUILDING_LIGHT"

    if namespace in {"moa_decor_holidays", "snowyspirit", "giftypresent"} or has_phrase(
        path, "christmas", "halloween", "easter", "holiday", "snowman", "wreath", "garland", "tinsel", "pumpkin_lantern"
    ):
        return "BUILDING_HOLIDAY"

    if is_block(row):
        return "BUILDING_BLOCKS"

    if hint and hint != "OTHER":
        return hint
    return "OTHER"


def validate(data: dict, registered_ids: set[str] | None = None) -> None:
    if data.get("schema_version") != SCHEMA_VERSION:
        raise ValueError("Expected schema_version 1")
    assignments = data.get("assignments")
    if not isinstance(assignments, dict) or set(assignments) != set(LEAVES):
        raise ValueError("Manifest must contain every leaf and no parent assignment")
    seen: dict[str, str] = {}
    for leaf, item_ids in assignments.items():
        if leaf in PARENTS or not isinstance(item_ids, list):
            raise ValueError(f"Invalid assignment leaf {leaf}")
        if item_ids != sorted(item_ids) or len(item_ids) != len(set(item_ids)):
            raise ValueError(f"Leaf {leaf} must contain sorted unique IDs")
        for item_id in item_ids:
            if not isinstance(item_id, str) or not ID_PATTERN.fullmatch(item_id):
                raise ValueError(f"Invalid item ID {item_id!r}")
            previous = seen.setdefault(item_id, leaf)
            if previous != leaf:
                raise ValueError(f"Duplicate assignment {item_id}: {previous} and {leaf}")
    if data.get("item_count") != len(seen):
        raise ValueError("item_count does not match assignments")
    if registered_ids is not None:
        unknown = set(seen) - registered_ids
        missing = registered_ids - set(seen)
        if unknown:
            raise ValueError(f"Manifest has {len(unknown)} IDs absent from runtime export, first: {sorted(unknown)[0]}")
        if missing:
            raise ValueError(f"Manifest misses {len(missing)} registered IDs, first: {sorted(missing)[0]}")


def load_export(path: Path) -> tuple[list[dict], set[str], str]:
    data = read_json(path)
    rows = data.get("items")
    if data.get("schema_version") != 1 or not isinstance(rows, list):
        raise ValueError("Runtime item export has an unsupported schema")
    item_ids = [row.get("registry_id") for row in rows]
    if any(not isinstance(item_id, str) or not ID_PATTERN.fullmatch(item_id) for item_id in item_ids):
        raise ValueError("Runtime item export contains an invalid registry ID")
    if item_ids != sorted(item_ids) or len(item_ids) != len(set(item_ids)):
        raise ValueError("Runtime item export must contain sorted unique registry IDs")
    if data.get("item_count") != len(rows):
        raise ValueError("Runtime item export item_count does not match its rows")
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    return rows, set(item_ids), digest


def generated_manifest(rows: list[dict], registered_ids: set[str]) -> dict:
    hints = migration_hints(registered_ids)
    grouped: dict[str, list[str]] = defaultdict(list)
    for row in rows:
        item_id = row["registry_id"]
        grouped[classify(row, hints.get(item_id))].append(item_id)
    assignments = {leaf: sorted(grouped[leaf]) for leaf in LEAVES}
    data = {
        "schema_version": SCHEMA_VERSION,
        "source": "Exact logical-server Forge item registry export, classified offline by the 2026-11-22 JEM contract",
        "item_count": len(rows),
        "assignments": assignments,
    }
    validate(data, registered_ids)
    return data


def updated_manifest(registered_ids: set[str]) -> dict:
    data = read_json(MANIFEST)
    validate(data)
    existing = {
        item_id: leaf
        for leaf, item_ids in data["assignments"].items()
        for item_id in item_ids
    }
    removed = set(existing) - registered_ids
    if removed:
        raise ValueError(f"Existing manifest contains removed ID {sorted(removed)[0]}")
    grouped: dict[str, list[str]] = defaultdict(list)
    for item_id in sorted(registered_ids):
        grouped[existing.get(item_id, "OTHER")].append(item_id)
    result = {
        "schema_version": SCHEMA_VERSION,
        "source": "Exact logical-server Forge item registry export with preserved reviewed assignments",
        "item_count": len(registered_ids),
        "assignments": {leaf: grouped[leaf] for leaf in LEAVES},
    }
    validate(result, registered_ids)
    return result


def write_report(data: dict, rows: list[dict], export_path: Path, digest: str) -> None:
    namespace_by_id = {row["registry_id"]: row["namespace"] for row in rows}
    lines = [
        "# JEI Catalog Review",
        "",
        f"- Runtime export: {export_path.as_posix()}",
        f"- Export SHA-256: {digest}",
        f"- Registered items: {data['item_count']}",
        "- Runtime classification: direct immutable item-ID lookup only",
        "",
        "## Leaf and namespace counts",
        "",
    ]
    for leaf in LEAVES:
        item_ids = data["assignments"][leaf]
        lines.extend((f"### {leaf}", "", f"Total: {len(item_ids)}", ""))
        counts = Counter(namespace_by_id[item_id] for item_id in item_ids)
        lines.extend(f"- {namespace}: {count}" for namespace, count in sorted(counts.items()))
        lines.append("")
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("generate", "update", "validate"))
    parser.add_argument("export", type=Path)
    args = parser.parse_args()
    export_path = args.export.resolve()
    rows, registered_ids, digest = load_export(export_path)
    if args.mode == "generate":
        data = generated_manifest(rows, registered_ids)
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        write_report(data, rows, export_path, digest)
    elif args.mode == "update":
        data = updated_manifest(registered_ids)
        MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        write_report(data, rows, export_path, digest)
    else:
        data = read_json(MANIFEST)
        validate(data, registered_ids)
    counts = {leaf: len(item_ids) for leaf, item_ids in data["assignments"].items()}
    print(json.dumps({"item_count": data["item_count"], "export_sha256": digest, "categories": counts}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

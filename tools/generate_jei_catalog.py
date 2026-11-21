#!/usr/bin/env python3
"""Generate or validate JEMCompat's stable JEI item-category manifest."""

from __future__ import annotations

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ITEMS = ROOT / "Catalog" / "registries" / "items"
VIEWS = ROOT / "Catalog" / "views"
MANIFEST = ROOT / "Mods" / "JEMCompat" / "src" / "main" / "resources" / "assets" / "jemcompat" / "jei_catalog.json"
SCHEMA_VERSION = 1
LEAVES = (
    "WEAPON_TOTEMS", "WEAPON_RANGED", "WEAPON_MELEE", "WEARABLE", "FOOD", "ORES",
    "CREATE_REDSTONE", "FURNITURE", "NATURE", "CONTAINERS", "HOLIDAY", "BUILDING_BLOCKS",
    "GLASS", "LIGHT", "FISHING", "BOOKS", "POTIONS", "TRANSPORT", "SPAWN_EGGS", "OTHER",
)
PARENTS = {"WEAPONS", "BUILDING"}
ID_PATTERN = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
HANDCRAFTED_COLORS = (
    "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
    "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow",
)
HANDCRAFTED_WOODS = (
    "acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle", "mangrove",
    "oak", "spruce", "warped",
)
INITIAL_EXACT_ASSIGNMENTS = {
    "irons_spellbooks:scroll": "WEAPON_MELEE",
    **{
        f"handcrafted:{color}_{suffix}": "FURNITURE"
        for color in HANDCRAFTED_COLORS
        for suffix in ("cushion", "sheet")
    },
    **{
        f"handcrafted:{wood}_{suffix}": "FURNITURE"
        for wood in HANDCRAFTED_WOODS
        for suffix in ("counter", "desk")
    },
}


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def records(folder: Path) -> list[dict]:
    result: list[dict] = []
    for path in sorted(folder.glob("*.json")):
        result.extend(read_json(path).get("records", []))
    return result


def view_ids(name: str) -> set[str]:
    return {row["item_id"] for row in records(VIEWS / name) if ID_PATTERN.fullmatch(row.get("item_id", ""))}


def has_phrase(path: str, *phrases: str) -> bool:
    padded = f"_{path.casefold().replace('/', '_')}_"
    return any(f"_{phrase}_" in padded for phrase in phrases)


def has_tag(tags: set[str], *needles: str) -> bool:
    normalized = {tag.casefold().replace("items/", "") for tag in tags}
    return any(any(needle in tag for needle in needles) for tag in normalized)


def existing_assignments() -> dict[str, str]:
    if not MANIFEST.exists():
        return {}
    data = read_json(MANIFEST)
    validate(data)
    return {item_id: category for category, ids in data["assignments"].items() for item_id in ids}


def category(row: dict, placeables: set[str], armor: set[str], tools: set[str], locked: dict[str, str]) -> str:
    item_id = row["registry_id"]
    if item_id in locked:
        return locked[item_id]
    namespace, path = item_id.split(":", 1)
    tags = set(row.get("tags") or [])
    placeable = item_id in placeables or bool(row.get("block_item_id"))

    if item_id in {"spawn:sunstone", "spawn:moonstone", "spawn:snail_shell", "minecraft:resin_clump",
                   "minecraft:powder_snow_bucket", "minecraft:resin_brick"}:
        return "ORES"
    if path.endswith("_ore") or "_ore_" in path or path == "ancient_debris" or has_tag(
            tags, "forge:ores", "forge:raw_materials", "forge:ingots", "forge:nuggets", "forge:gems",
            "forge:dusts", "forge:storage_blocks", "forge:plates", "forge:rods", "forge:wires",
            "c:ores", "c:raw_materials", "c:ingots", "c:nuggets", "c:gems", "c:dusts"):
        return "ORES"
    if not placeable and has_phrase(path, "raw", "ingot", "nugget", "gem", "crystal", "dust", "powder",
                                    "crushed", "shard", "chunk", "plate", "sheet", "rod", "wire"):
        return "ORES"
    if namespace == "infinitybuttons" or item_id == "minecraft:tnt":
        return "CREATE_REDSTONE"
    if namespace in {"toms_storage", "sophisticatedbackpacks", "sophisticatedstorage", "mobhome"} \
            or item_id == "jem_economy:shipping_box" or path == "backpack":
        return "CONTAINERS"
    if namespace.startswith("kaleidoscope_") or namespace in {"bakery", "herbalbrews"}:
        return "FOOD"
    if namespace in {"casualswing", "shutterup", "chimes", "secretdoors"}:
        return "FURNITURE"
    if item_id in {"animal_feeding_trough:feeding_trough", "farm_and_charm:chicken_coop"}:
        return "FOOD"
    if path.endswith("_trophy") or path.endswith("_skull") or path.endswith("_skull_block") \
            or namespace == "minecraft" and path.endswith("_head") \
            or namespace == "blastfromthepast" and path in {"antler_display", "broad_antler_display", "curly_antler_display", "spikey_antler_display"} \
            or item_id in {"dustydecorations:displayed_oars", "infinitybuttons:hoglin_mount_button"}:
        return "FURNITURE"
    if namespace == "spawn" and path.startswith("clam_case_") or namespace == "beachparty" and path == "palm_cabinet" \
            or placeable and has_phrase(path, "chest", "barrel", "crate", "drawer", "cupboard", "cabinet", "storage", "basket", "bookshelf"):
        return "CONTAINERS"
    if placeable and has_phrase(path, "torch", "lantern", "candle", "light", "lamp", "chandelier", "sconce", "brazier"):
        return "LIGHT"
    if placeable and (has_phrase(path, "glass", "glass_pane", "seaglass", "window", "window_pane")
                      or has_tag(tags, "forge:glass", "c:glass")):
        return "GLASS"
    if has_tag(tags, "jemcompat:jei/create_redstone"):
        return "CREATE_REDSTONE"
    if has_tag(tags, "jemcompat:jei/fishing"):
        return "FISHING"
    if has_tag(tags, "jemcompat:transport") or namespace in {"immersive_aircraft", "trotting_wagons"} \
            or has_phrase(path, "wagon", "cart", "sled", "ship", "boat", "canoe", "paraglider", "elytra", "aircraft", "biplane", "gyrodyne") \
            or namespace in {"create", "railways", "bellsandwhistles", "create_train_parts", "createrailwaysnavigator"} \
            and has_phrase(path, "train", "station", "track", "schedule", "controls", "conductor", "locomotive", "bogey", "carriage"):
        return "TRANSPORT"
    if path.endswith("_spawn_egg"):
        return "SPAWN_EGGS"
    if row.get("categories") and "food" in row["categories"] or has_tag(tags, "foods", "crops", "fruits", "vegetables", "drinks", "beverages", "dough", "flour") \
            or has_phrase(path, "drink", "juice", "tea", "coffee", "wine", "beer", "cocktail", "crop", "flour", "dough", "cheese", "butter", "cooking_pot", "stove", "oven", "fryer", "kettle", "brewery", "kitchen", "watering_can", "sickle", "cultivator", "fertilizer", "bone_meal"):
        return "FOOD"
    if item_id == "irons_spellbooks:scroll":
        return "WEAPON_MELEE"
    if item_id == "minecraft:totem_of_undying" or namespace == "moretotems" and path.endswith("_totem_of_undying") \
            or has_tag(tags, "skarrier_mobs:totems"):
        return "WEARABLE"
    weapon = item_id in tools or namespace == "xtraarrows" or has_tag(tags, "minecraft:weapons", "forge:weapons") or has_phrase(
        path, "sword", "dagger", "spear", "javelin", "trident", "mace", "battleaxe", "warhammer", "shield",
        "bow", "crossbow", "arrow", "bolt", "chakram", "shuriken", "musket", "rifle", "pistol", "cannon",
        "launcher", "blaster", "slingshot", "scythe", "halberd", "glaive", "katana", "claymore", "rapier",
    )
    if weapon:
        if has_phrase(path, "totem") or has_tag(tags, "totem"):
            return "WEAPON_TOTEMS"
        if has_phrase(path, "bow", "crossbow", "arrow", "bolt", "spear", "javelin", "trident", "chakram",
                      "shuriken", "gun", "rifle", "pistol", "musket", "cannon", "launcher", "blaster", "slingshot") \
                or has_tag(tags, "bows", "crossbows", "arrows", "spears", "throwable", "ranged"):
            return "WEAPON_RANGED"
        return "WEAPON_MELEE"
    if item_id in armor or path.endswith("_doll") or namespace == "kaleidoscope_doll":
        return "WEARABLE"
    if has_phrase(path, "potion", "elixir", "tonic", "flask"):
        return "POTIONS"
    if has_phrase(path, "book", "tome", "guide", "journal", "manual", "codex", "grimoire"):
        return "BOOKS"
    if namespace == "tide" or has_tag(tags, "minecraft:fishes") or has_phrase(path, "fishing_rod", "fish_display", "fish_frame", "bait", "bobber", "fishing_hook"):
        return "FISHING"
    if has_phrase(path, "redstone", "repeater", "comparator", "piston", "observer", "dispenser", "dropper",
                  "hopper", "lever", "pressure_plate", "tripwire", "daylight_detector", "sculk_sensor", "target",
                  "command_block", "button") or namespace in {"create", "railways", "bellsandwhistles", "create_train_parts", "createrailwaysnavigator"} \
            and has_phrase(path, "cogwheel", "shaft", "gearbox", "press", "deployer", "mixer", "basin", "motor",
                           "fan", "pump", "pipe", "boiler", "flywheel", "drill", "saw", "belt", "conveyor",
                           "millstone", "crushing_wheel", "steam_engine", "steam_whistle", "mechanical"):
        return "CREATE_REDSTONE"
    if namespace in {"moa_decor_holidays", "snowyspirit", "giftypresent"} or has_phrase(
            path, "christmas", "halloween", "holiday", "snowman", "wreath", "garland", "tinsel", "pumpkin_lantern"):
        return "HOLIDAY"
    if namespace == "beachparty" or namespace == "fairylights" and has_phrase(path, "pennant_bunting", "spearhead_pennant", "square_pennant", "swallowtail_pennant", "triangle_pennant") \
            or has_phrase(path, "chair", "table", "table_cloth", "sideboard", "buffet", "counter", "countertop", "sofa", "couch",
                          "stool", "bench", "curtain", "carpet", "painting", "decoration", "display", "parasol", "towel",
                          "radio", "urn", "shelf", "banner", "sign", "bed", "cushion", "sheet", "desk", "pennant", "bunting"):
        return "FURNITURE"
    if has_tag(tags, "jemcompat:jei/nature", "minecraft:leaves") or has_phrase(
            path, "leaf_pile", "leaves", "sapling", "icicle", "moss", "lichen", "ivy", "roots", "sprout", "flower", "vine", "seastar", "seastars"):
        return "NATURE"
    if placeable and path not in {"crafting_table", "smithing_table", "enchanting_table", "cartography_table",
                                  "fletching_table", "grindstone", "stonecutter", "loom", "anvil", "chipped_anvil", "damaged_anvil"}:
        return "BUILDING_BLOCKS"
    return "OTHER"


def validate(data: dict) -> None:
    if data.get("schema_version") != SCHEMA_VERSION:
        raise ValueError(f"Expected schema_version {SCHEMA_VERSION}")
    assignments = data.get("assignments")
    if not isinstance(assignments, dict) or set(assignments) != set(LEAVES):
        raise ValueError("Manifest must contain every leaf category and no other assignment target")
    seen: dict[str, str] = {}
    for name, ids in assignments.items():
        if name in PARENTS or name not in LEAVES or not isinstance(ids, list):
            raise ValueError(f"Invalid assignment category {name}")
        if ids != sorted(ids) or len(ids) != len(set(ids)):
            raise ValueError(f"Category {name} must contain sorted unique IDs")
        for item_id in ids:
            if not isinstance(item_id, str) or not ID_PATTERN.fullmatch(item_id):
                raise ValueError(f"Invalid item ID {item_id!r}")
            if item_id in seen:
                raise ValueError(f"Duplicate assignment {item_id}: {seen[item_id]} and {name}")
            seen[item_id] = name
    if data.get("item_count") != len(seen):
        raise ValueError("item_count does not match assignments")


def generate() -> dict:
    locked = existing_assignments()
    item_rows = {row["registry_id"]: row for row in records(ITEMS) if ID_PATTERN.fullmatch(row.get("registry_id", ""))}
    for item_id in locked:
        item_rows.setdefault(item_id, {"registry_id": item_id, "tags": [], "categories": []})
    for item_id in INITIAL_EXACT_ASSIGNMENTS:
        item_rows.setdefault(item_id, {"registry_id": item_id, "tags": [], "categories": []})
    grouped: dict[str, list[str]] = defaultdict(list)
    placeables = view_ids("placeables")
    armor = view_ids("armor")
    tools = view_ids("tools")
    for item_id, row in sorted(item_rows.items()):
        assignment = locked.get(item_id)
        if assignment is None:
            assignment = INITIAL_EXACT_ASSIGNMENTS.get(item_id)
        if assignment is None:
            assignment = category(row, placeables, armor, tools, locked)
        grouped[assignment].append(item_id)
    assignments = {name: grouped[name] for name in LEAVES}
    data = {
        "schema_version": SCHEMA_VERSION,
        "source": "JEM Registry Atlas exact item IDs with preserved reviewed assignments",
        "item_count": sum(map(len, assignments.values())),
        "assignments": assignments,
    }
    validate(data)
    return data


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("generate", "validate"))
    args = parser.parse_args()
    if args.mode == "generate":
        data = generate()
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    else:
        data = read_json(MANIFEST)
        validate(data)
    counts = {name: len(ids) for name, ids in data["assignments"].items()}
    print(json.dumps({"item_count": data["item_count"], "categories": counts}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

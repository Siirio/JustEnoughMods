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
ATLAS_ITEMS = ROOT / "Catalog" / "registries" / "items"
SCHEMA_VERSION = 2
SOURCE_EXPORT_SHA256 = "958be5aee64f564c148764bdb9e97a8115c3e00fd44c0fe9b65fcd5f98174d39"
LEAVES = (
    "WEAPON_TOTEMS",
    "WEAPON_TRAINING",
    "WEAPON_MELEE",
    "WEAPON_RANGED",
    "WEAPON_SHIELDS",
    "WEAPON_SMITHING_TEMPLATES",
    "WEARABLE_ARMOR",
    "WEARABLE_MOB_ARMOR",
    "WEARABLE_DOLLS",
    "FOOD_EDIBLES",
    "FOOD_COOKING_EQUIPMENT",
    "FISHING",
    "TRANSPORT",
    "COMPANIONS",
    "BUILDING_CONTAINERS",
    "POTIONS",
    "BOOKS",
    "ORES",
    "COMPONENTS",
    "CREATE_REDSTONE",
    "BUILDING_FURNITURE",
    "BUILDING_NATURE",
    "BUILDING_GLASS",
    "BUILDING_LIGHT",
    "BUILDING_TOOLS",
    "BUILDING_HOLIDAY",
    "BUILDING_BLOCKS",
    "SPAWN_EGGS",
    "OTHER",
)
PARENTS = {"WEAPONS", "WEARABLES", "FOOD", "BUILDING"}
ID_PATTERN = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
SKARRIER_TOTEMS = {
    "skarrier_mobs:totem_of_extinction",
    "skarrier_mobs:totem_of_freezing",
    "skarrier_mobs:totem_of_melding",
    "skarrier_mobs:totem_of_repulsion",
    "skarrier_mobs:totem_of_seething",
}
EXACT = {
    "alexscaves:ortholance_inventory": "WEAPON_MELEE",
    "alexscaves:sea_staff": "WEAPON_RANGED",
    "alexscaves:sea_staff_inventory": "WEAPON_RANGED",
    "alexscaves:sugar_staff": "WEAPON_RANGED",
    "alexscaves:sugar_staff_inventory": "WEAPON_RANGED",
    "companions:book_ice_shard": "BOOKS",
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
    "WEAPON_SHIELDS": "WEAPON_SHIELDS",
    "WEAPON_SMITHING_TEMPLATES": "WEAPON_SMITHING_TEMPLATES",
    "WEARABLE": "WEARABLE_ARMOR",
    "WEARABLES": "WEARABLE_ARMOR",
    "WEARABLE_ARMOR": "WEARABLE_ARMOR",
    "WEARABLE_MOB_ARMOR": "WEARABLE_MOB_ARMOR",
    "WEARABLE_DOLLS": "WEARABLE_DOLLS",
    "FOOD": "FOOD_COOKING_EQUIPMENT",
    "ORES": "ORES",
    "COMPONENTS": "COMPONENTS",
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
    "COMPANIONS": "COMPANIONS",
    "BUILDING_TOOLS": "BUILDING_TOOLS",
    "SPAWN_EGGS": "SPAWN_EGGS",
    "OTHER": "OTHER",
}

COMPANION_EGGS = {
    "minecraft:allay_spawn_egg",
    "minecraft:cat_spawn_egg",
    "minecraft:parrot_spawn_egg",
    "minecraft:wolf_spawn_egg",
    *{
        f"alexsmobs:spawn_egg_{path}"
        for path in (
            "bald_eagle", "caiman", "capuchin_monkey", "cosmaw", "crow", "flutter",
            "kangaroo", "mantis_shrimp", "mimic_octopus", "mudskipper", "raccoon",
            "sugar_glider", "tarantula_hawk", "warped_toad",
        )
    },
    *{
        f"companions:{path}_spawn_egg"
        for path in (
            "broken_dinamo", "cornelius", "golden_allay", "hostile_imp",
            "hostile_puppet_glove", "illager_golem", "living_candle", "sacred_pontiff",
            "teddy", "wild_antlion",
        )
    },
}
TRANSPORT_EGGS = {
    *{
        f"minecraft:{path}_spawn_egg"
        for path in (
            "camel", "donkey", "happy_ghast", "horse", "mule", "skeleton_horse",
            "strider", "zombie_horse",
        )
    },
    *{
        f"alexsmobs:spawn_egg_{path}"
        for path in ("elephant", "grizzly_bear", "komodo_dragon", "laviathan", "straddler", "tusklin")
    },
    *{
        f"alexscaves:spawn_egg_{path}"
        for path in ("atlatitan", "subterranodon", "tremorsaurus")
    },
}

CREATE_NAMESPACES = {
    "create",
    "railways",
    "bellsandwhistles",
    "create_train_parts",
    "createrailwaysnavigator",
}

EXACT_RECLASSIFICATIONS = {
    "alexsmobs:animal_dictionary": "BOOKS",
    "barnyardbuddies:pyrenees_protective_vest": "WEARABLE_MOB_ARMOR",
    "companions:big_bread": "FOOD_EDIBLES",
    "luminous_beasts:ultimate_bestiary": "BOOKS",
    "minecraft:dragon_breath": "POTIONS",
    "minecraft:resin_clump": "ORES",
    "minecraft:crafter": "CREATE_REDSTONE",
    "trials:crafter": "CREATE_REDSTONE",
    # Semantic categories take precedence over the source mod family
    "create:cardboard_sword": "WEAPON_MELEE",
    "create:potato_cannon": "WEAPON_RANGED",
    "create:bar_of_chocolate": "FOOD_EDIBLES",
    "create:builders_tea": "FOOD_EDIBLES",
    "create:chocolate_glazed_berries": "FOOD_EDIBLES",
    "create:honeyed_apple": "FOOD_EDIBLES",
    "create:sweet_roll": "FOOD_EDIBLES",
    "create:millstone": "FOOD_COOKING_EQUIPMENT",
    "create:chocolate_bucket": "FISHING",
    "create:honey_bucket": "FISHING",
    **{
        f"create:{path}": "WEARABLE_ARMOR"
        for path in (
            "cardboard_boots",
            "cardboard_chestplate",
            "cardboard_helmet",
            "cardboard_leggings",
            "copper_backtank",
            "copper_diving_boots",
            "copper_diving_helmet",
            "goggles",
            "netherite_backtank",
            "netherite_diving_boots",
            "netherite_diving_helmet",
        )
    },
    **{
        f"railways:{color}_conductor_cap": "WEARABLE_ARMOR"
        for color in (
            "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow",
        )
    },
    **{
        f"create:{path}": "ORES"
        for path in (
            "andesite_alloy", "andesite_alloy_block", "brass_block", "brass_ingot", "brass_nugget",
            "brass_sheet", "cardboard", "cardboard_block", "copper_nugget", "copper_sheet",
            "crushed_raw_aluminum", "crushed_raw_copper", "crushed_raw_gold", "crushed_raw_iron",
            "crushed_raw_lead", "crushed_raw_nickel", "crushed_raw_osmium", "crushed_raw_platinum",
            "crushed_raw_quicksilver", "crushed_raw_silver", "crushed_raw_tin", "crushed_raw_uranium",
            "crushed_raw_zinc", "deepslate_zinc_ore", "experience_block", "experience_nugget",
            "golden_sheet", "iron_sheet", "powdered_obsidian", "raw_zinc", "raw_zinc_block",
            "sturdy_sheet", "unprocessed_obsidian_sheet", "zinc_block", "zinc_ingot", "zinc_nugget",
            "zinc_ore",
        )
    },
}

COMPONENT_ITEMS = {
    # Alex's Caves materials and creature drops
    "alexscaves:charred_remnant",
    "alexscaves:corrodent_teeth",
    "alexscaves:dark_tatters",
    "alexscaves:ferrouslime_ball",
    "alexscaves:fissile_core",
    "alexscaves:guano",
    "alexscaves:heavy_bone",
    "alexscaves:immortal_embryo",
    "alexscaves:licowitch_radiant_essence",
    "alexscaves:ominous_catalyst",
    "alexscaves:pewen_sap",
    "alexscaves:pure_darkness",
    "alexscaves:radiant_essence",
    "alexscaves:shadow_silk",
    "alexscaves:tough_hide",
    "alexscaves:toxic_paste",
    # Alex's Mobs materials and creature drops
    "alexsmobs:ambergris",
    "alexsmobs:banana_slug_slime",
    "alexsmobs:bear_fur",
    "alexsmobs:bison_fur",
    "alexsmobs:blood_sac",
    "alexsmobs:bone_serpent_tooth",
    "alexsmobs:cachalot_whale_tooth",
    "alexsmobs:centipede_leg",
    "alexsmobs:cockroach_ootheca",
    "alexsmobs:cockroach_wing",
    "alexsmobs:cockroach_wing_fragment",
    "alexsmobs:crocodile_scute",
    "alexsmobs:dropbear_claw",
    "alexsmobs:elastic_tendon",
    "alexsmobs:emu_feather",
    "alexsmobs:farseer_arm",
    "alexsmobs:fish_bones",
    "alexsmobs:froststalker_horn",
    "alexsmobs:gazelle_horn",
    "alexsmobs:guster_eye",
    "alexsmobs:hemolymph_sac",
    "alexsmobs:kangaroo_hide",
    "alexsmobs:komodo_spit",
    "alexsmobs:lost_tentacle",
    "alexsmobs:moose_antler",
    "alexsmobs:mosquito_proboscis",
    "alexsmobs:mungal_spores",
    "alexsmobs:raccoon_tail",
    "alexsmobs:rattlesnake_rattle",
    "alexsmobs:roadrunner_feather",
    "alexsmobs:rocky_shell",
    "alexsmobs:serrated_shark_tooth",
    "alexsmobs:shark_tooth",
    "alexsmobs:shed_snake_skin",
    "alexsmobs:skreecher_soul",
    "alexsmobs:spiked_scute",
    "alexsmobs:tarantula_hawk_wing",
    "alexsmobs:tarantula_hawk_wing_fragment",
    "alexsmobs:void_worm_eye",
    "alexsmobs:void_worm_mandible",
    "alexsmobs:warped_mixture",
    "alexsmobs:warped_muscle",
    # Barnyard Buddies and Primal creature drops
    "barnyardbuddies:silkie_feather",
    "primal:crocodile_scute",
    # Legendary Monsters boss materials
    "legendary_monsters:air_rune",
    "legendary_monsters:anchor_handle",
    "legendary_monsters:ancient_spike",
    "legendary_monsters:corrupted_soul",
    "legendary_monsters:dinosaur_bone",
    "legendary_monsters:eye_of_air",
    "legendary_monsters:eye_of_annihilation",
    "legendary_monsters:eye_of_bones",
    "legendary_monsters:eye_of_chorus",
    "legendary_monsters:eye_of_frost",
    "legendary_monsters:eye_of_ghost",
    "legendary_monsters:eye_of_magma",
    "legendary_monsters:eye_of_many_ribs",
    "legendary_monsters:eye_of_moss",
    "legendary_monsters:eye_of_sandstorm",
    "legendary_monsters:eye_of_shulker",
    "legendary_monsters:eye_of_soul",
    "legendary_monsters:frozen_rune",
    "legendary_monsters:large_shulker_shell",
    "legendary_monsters:lava_eaters_skin",
    "legendary_monsters:long_stick_half",
    "legendary_monsters:metal_debris",
    "legendary_monsters:warped_mushroom_cap_part",
    "legendary_monsters:withered_bone",
    "legendary_monsters:withered_horn",
    # Luminous Beasts and Luminous Nether creature drops
    "luminous_beasts:bone_rattle",
    "luminous_beasts:gator_tooth",
    "luminous_beasts:jungle_pendant",
    "luminous_beasts:legendary_scrap",
    "luminous_beasts:luminous_antennae",
    "luminous_beasts:magic_root",
    "luminous_beasts:molten_coal",
    "luminous_beasts:phoenix_feather",
    "luminous_beasts:viper_tooth",
    "luminous_beasts:wind_item",
    "luminous_beasts:yeti_horn",
    "luminous_nether:ashes",
    "luminous_nether:crimson_contaminant",
    "luminous_nether:ectoplasm_ball",
    "luminous_nether:ghostly_essence",
    "luminous_nether:molten_coal",
    "luminous_nether:soul_bone",
    "luminous_nether:torn_rag",
}

HOLIDAY_ITEMS = {
    "dustydecorations:gourd",
    "dustydecorations:gourd_3",
    "dustydecorations:ground_2",
    "dustydecorations:hanging_gourds",
    "dustydecorations:pumpkin_scarecrow",
    "dustydecorations:winter_reef",
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


def load_atlas_items() -> dict[str, dict]:
    records: dict[str, dict] = {}
    for path in sorted(ATLAS_ITEMS.glob("*.json")):
        data = read_json(path)
        for row in data.get("records", []):
            item_id = row.get("registry_id")
            if isinstance(item_id, str) and ID_PATTERN.fullmatch(item_id):
                records.setdefault(item_id, row)
    return records


def is_smithing_template(item_id: str, row: dict) -> bool:
    path = item_id.split(":", 1)[1]
    return (
        "smithing_template" in path
        or "smithing_augment" in path
        or path.endswith("_armor_trim")
        or has_tag(row, "smithing_templates", "trim_templates")
    )


def is_head_block(path: str) -> bool:
    return (
        path.endswith("_head")
        or path.endswith("_skull")
        or path.endswith("_wall_head")
        or path.endswith("_wall_skull")
    ) and not any(marker in path for marker in ("headstock", "headlight", "headsman"))


def is_food_block(namespace: str, path: str, row: dict) -> bool:
    if has_tag(row, "foods", "food_ingredients", "jams"):
        return True
    if namespace == "kaleidoscope_cookery" and path.endswith("_plate"):
        return True
    if namespace == "kaleidoscope_cookery" and (
        path.endswith("_platter")
        or path in {"biluochun", "cold_cut_ham_slices", "oil_block", "oolong", "tieguanyin"}
    ):
        return True
    if namespace == "handcrafted" and path == "berry_jam_jar":
        return True
    if namespace in {"farm_and_charm", "vinery"} and path.endswith("_bag"):
        return True
    if namespace == "vinery" and path.endswith("grapejuice"):
        return True
    return path == "cake" or path.endswith((
        "_jam",
        "_pie",
        "_cake",
        "_tart",
        "_pudding",
        "_gateau",
        "_gingerbread",
    )) or path in {
        "gingerbread",
        "strung_mushrooms",
    } or (namespace == "brewery" and path.startswith("dried_"))


def is_cooking_equipment(namespace: str, path: str) -> bool:
    if (namespace, path) in {
        ("kaleidoscope_cookery", "pot"),
        ("kaleidoscope_cookery", "oil_pot"),
        ("kaleidoscope_cookery", "trash_can"),
        ("farm_and_charm", "chicken_nest"),
    }:
        return True
    return namespace in {
        "bakery",
        "brewery",
        "farm_and_charm",
        "herbalbrews",
        "kaleidoscope_cookery",
        "vinery",
    } and has_phrase(
        path,
        "baker_station",
        "cake_stand",
        "tray",
        "jar",
        "wok",
        "oil_pot",
        "trash_can",
        "cutting_board",
        "chopping_board",
        "frying_pan",
        "cooking_pot",
        "mixing_bowl",
        "kettle",
        "cauldron",
        "brewery",
        "millstone",
        "oven",
        "stove",
        "grill",
        "press",
        "steamer",
        "enamel_basin",
        "empty_cup",
        "kitchenware_racks",
        "chili_ristra",
    )


def is_furniture_block(namespace: str, path: str, row: dict) -> bool:
    if namespace in {"dustydecorations", "shutterup"}:
        return True
    if has_tag(row, "items/trophies", "items/pots", "items/crockery", "items/plates", "beast_trophies"):
        return True
    if namespace == "block_factorys_bosses" and any(marker in path for marker in (
        "remains",
        "corpse",
        "coin_pile",
        "candles",
        "vase",
    )):
        return True
    return has_phrase(
        path,
        "trophy",
        "decorated_pot",
        "ceramic_pot",
        "flower_pot",
        "pottery",
        "tableware",
        "dish_set",
        "plate_set",
        "coin_pile",
        "candelabra",
        "corpse",
        "bone_remains",
        "camera",
        "shark_jaws",
        "nightstand",
    ) or path.endswith(("_pot", "_vase")) or path in {"pot", "vase"}


def is_nature_block(path: str, row: dict) -> bool:
    if has_tag(
        row,
        "minecraft:items/planks",
        "minecraft:items/logs",
        "minecraft:items/wooden_",
        "minecraft:items/leaves",
        "minecraft:items/saplings",
        "minecraft:items/flowers",
        "minecraft:items/corals",
    ):
        return True
    if any(marker in path for marker in ("mushroom", "mushoom", "shroom")):
        return True
    wood_species = (
        "acacia", "aspen", "bamboo", "birch", "cedar", "cherry", "coconut", "crimson",
        "cypress", "dark_cherry", "dark_oak", "fir", "ghaf", "joshua", "jungle", "mangrove",
        "maple", "oak", "olive", "palo_verde", "redwood", "spruce", "sugi", "warped",
        "willow", "wisteria", "withered",
    )
    wood_shapes = (
        "_beam", "_button", "_door", "_fence", "_fence_gate", "_floorboard", "_lattice",
        "_pressure_plate", "_slab", "_stairs", "_trap_door", "_trapdoor", "_wall",
    )
    if any(species in path for species in wood_species) and path.endswith(wood_shapes):
        return True
    return has_phrase(
        path,
        "log",
        "wood",
        "planks",
        "leaves",
        "sapling",
        "mushroom",
        "toadstool",
        "lily_pad",
        "lilypad",
        "lily",
        "reed",
        "clover",
        "fern",
        "fungus",
        "bush",
        "plant",
        "azolla",
        "kelp",
        "seagrass",
        "cactus",
        "succulent",
        "roots",
        "moss",
        "vine",
        "crop",
        "sprout",
        "lichen",
        "grass",
        "coral",
        "nylium",
        "golden_fungi",
        "cattail",
        "bee_balm",
    )


def reclassification_target(item_id: str, current_leaf: str, row: dict) -> str:
    namespace, path = item_id.split(":", 1)
    exact = EXACT_RECLASSIFICATIONS.get(item_id)
    if exact is not None:
        return exact
    if is_smithing_template(item_id, row):
        return "WEAPON_SMITHING_TEMPLATES"
    if namespace in CREATE_NAMESPACES:
        return "CREATE_REDSTONE"
    if item_id in COMPONENT_ITEMS:
        return "COMPONENTS"
    if item_id in HOLIDAY_ITEMS or (
        current_leaf == "BUILDING_BLOCKS"
        and has_phrase(path, "wreath", "garland", "gourd", "pumpkin_scarecrow")
    ):
        return "BUILDING_HOLIDAY"
    if current_leaf != "BUILDING_BLOCKS":
        return current_leaf
    if item_id == "block_factorys_bosses:plank":
        return "BUILDING_NATURE"
    if is_head_block(path):
        return "WEARABLE_ARMOR"
    if namespace == "bakery" and path == "breadbox":
        return "BUILDING_CONTAINERS"
    if is_food_block(namespace, path, row):
        return "FOOD_EDIBLES"
    if is_cooking_equipment(namespace, path):
        return "FOOD_COOKING_EQUIPMENT"
    if is_furniture_block(namespace, path, row):
        return "BUILDING_FURNITURE"
    if is_nature_block(path, row):
        return "BUILDING_NATURE"
    return current_leaf


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

    if class_has(row, "ShieldItem") or "shield" in token_set or {"buckler", "targe"} & token_set:
        return "WEAPON_SHIELDS"

    if "spear" in token_set and not ({"javelin", "trident"} & token_set):
        return "WEAPON_MELEE"
    if class_has(row, "SwordItem", "DiggerItem") or has_tag(
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

    if any(marker in path for marker in ("_horse_armor", "_nautilus_armor", "wolf_armor")):
        return "WEARABLE_MOB_ARMOR"
    if (
        namespace == "kaleidoscope_doll"
        and (
            path == "custom_doll"
            or path in {"doll_entity_item", "doll_icon"}
            or re.fullmatch(r"doll_\d+", path)
        )
    ) or (
        namespace == "kaleidoscope_nether"
        and re.fullmatch(r"(?:entity_)?doll_\d+", path)
    ):
        return "WEARABLE_DOLLS"
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
        return "WEARABLE_ARMOR"

    if item_id in {
        "deep_dark_regrowth:apatine_plant",
        "deep_dark_regrowth:apatine_stage_1",
        "deep_dark_regrowth:exxo_plant",
        "deep_dark_regrowth:infected_mushroom",
        "deep_dark_regrowth:large_infected_mushroom",
        "deep_dark_regrowth:short_infected_mushroom",
        "deep_dark_regrowth:vilethorn",
        "immersive_weathering:icicle",
        "unearthed_journey:platypodium_elegans_sapling_plant",
    }:
        return "BUILDING_NATURE"

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

    if item_id in {"jem_building:builder_hammer", "bbb:hammer", "handcrafted:hammer"}:
        return "BUILDING_TOOLS"

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
        raise ValueError("Expected schema_version 2")
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
    additional = data.get("additional_views")
    if not isinstance(additional, dict):
        raise ValueError("Manifest must contain additional_views")
    for view, item_ids in additional.items():
        if view not in LEAVES or view in PARENTS or not isinstance(item_ids, list):
            raise ValueError(f"Invalid additional view {view}")
        if item_ids != sorted(item_ids) or len(item_ids) != len(set(item_ids)):
            raise ValueError(f"Additional view {view} must contain sorted unique IDs")
        for item_id in item_ids:
            if item_id not in seen:
                raise ValueError(f"Additional view {view} contains unknown ID {item_id}")
            if seen[item_id] == view:
                raise ValueError(f"Additional view {view} redundantly contains its primary ID {item_id}")
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
        "additional_views": additional_views(registered_ids),
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
        "additional_views": additional_views(registered_ids),
    }
    validate(result, registered_ids)
    return result


def reclassified_manifest() -> dict:
    data = read_json(MANIFEST)
    for leaf in LEAVES:
        data.setdefault("assignments", {}).setdefault(leaf, [])
    validate(data)
    atlas = load_atlas_items()
    grouped: dict[str, list[str]] = defaultdict(list)
    moves: Counter[tuple[str, str]] = Counter()
    for current_leaf, item_ids in data["assignments"].items():
        for item_id in item_ids:
            target = reclassification_target(item_id, current_leaf, atlas.get(item_id, {}))
            grouped[target].append(item_id)
            if target != current_leaf:
                moves[(current_leaf, target)] += 1
    result = {
        "schema_version": SCHEMA_VERSION,
        "source": "Exact logical-server Forge item registry export with reviewed 2026-11-22 semantic corrections",
        "item_count": data["item_count"],
        "assignments": {leaf: sorted(grouped[leaf]) for leaf in LEAVES},
        "additional_views": data["additional_views"],
    }
    validate(result)
    assigned_ids = {
        item_id
        for item_ids in result["assignments"].values()
        for item_id in item_ids
    }
    for item_id, target in EXACT_RECLASSIFICATIONS.items():
        if item_id in assigned_ids:
            if item_id not in result["assignments"][target]:
                raise ValueError(f"Required correction failed for {item_id}: expected {target}")
    for item_id in COMPONENT_ITEMS:
        if item_id in assigned_ids:
            if item_id not in result["assignments"]["COMPONENTS"]:
                raise ValueError(f"Required component correction failed for {item_id}")
    remaining_templates = [
        item_id
        for leaf, item_ids in result["assignments"].items()
        if leaf != "WEAPON_SMITHING_TEMPLATES"
        for item_id in item_ids
        if is_smithing_template(item_id, atlas.get(item_id, {}))
    ]
    if remaining_templates:
        raise ValueError(f"Smithing template remained outside its leaf: {remaining_templates[0]}")
    remaining_create = [
        item_id
        for leaf, item_ids in result["assignments"].items()
        if leaf != "CREATE_REDSTONE"
        for item_id in item_ids
        if item_id.split(":", 1)[0] in CREATE_NAMESPACES
        and EXACT_RECLASSIFICATIONS.get(item_id) != leaf
    ]
    if remaining_create:
        raise ValueError(f"Create-family item remained outside Create and Redstone: {remaining_create[0]}")
    print(json.dumps({
        "moves": {
            f"{source} -> {target}": count
            for (source, target), count in sorted(moves.items())
        }
    }, indent=2))
    return result


def additional_views(registered_ids: set[str]) -> dict[str, list[str]]:
    return {
        "COMPANIONS": sorted(COMPANION_EGGS & registered_ids),
        "TRANSPORT": sorted(TRANSPORT_EGGS & registered_ids),
    }


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
    lines.extend(("## Additional exact views", ""))
    for view, item_ids in data["additional_views"].items():
        lines.extend((f"### {view}", "", f"Total: {len(item_ids)}", ""))
        counts = Counter(item_id.split(":", 1)[0] for item_id in item_ids)
        lines.extend(f"- {namespace}: {count}" for namespace, count in sorted(counts.items()))
        lines.append("")
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def write_manifest_review(data: dict) -> None:
    lines = [
        "# JEI Catalog Review",
        "",
        "- Registry source: reviewed logical-server export removed after initial validation",
        f"- Source export SHA-256: {SOURCE_EXPORT_SHA256}",
        f"- Registered items: {data['item_count']}",
        f"- Manifest schema: {data['schema_version']}",
        "- Runtime classification: direct immutable item-ID lookup only",
        "",
        "## Leaf and namespace counts",
        "",
    ]
    for leaf in LEAVES:
        item_ids = data["assignments"][leaf]
        lines.extend((f"### {leaf}", "", f"Total: {len(item_ids)}", ""))
        counts = Counter(item_id.split(":", 1)[0] for item_id in item_ids)
        lines.extend(f"- {namespace}: {count}" for namespace, count in sorted(counts.items()))
        lines.append("")
    lines.extend(("## Additional exact views", ""))
    for view, item_ids in data["additional_views"].items():
        lines.extend((f"### {view}", "", f"Total: {len(item_ids)}", ""))
        counts = Counter(item_id.split(":", 1)[0] for item_id in item_ids)
        lines.extend(f"- {namespace}: {count}" for namespace, count in sorted(counts.items()))
        lines.append("")
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("generate", "update", "reclassify", "validate", "review"))
    parser.add_argument("export", type=Path, nargs="?")
    args = parser.parse_args()
    if args.export is None:
        if args.mode not in {"reclassify", "validate", "review"}:
            parser.error("generate and update require a runtime export")
        if args.mode == "reclassify":
            data = reclassified_manifest()
            MANIFEST.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            write_manifest_review(data)
        else:
            data = read_json(MANIFEST)
            validate(data)
        if args.mode == "review":
            write_manifest_review(data)
        counts = {leaf: len(item_ids) for leaf, item_ids in data["assignments"].items()}
        print(json.dumps({"item_count": data["item_count"], "categories": counts}, indent=2))
        return 0
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

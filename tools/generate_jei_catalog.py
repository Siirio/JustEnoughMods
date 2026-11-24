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
    "MUSIC_DISCS",
    "TREES",
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
INTERNAL_ITEM_EXCLUSIONS = {
    "food:empty",
}
SKARRIER_TOTEMS = {
    "skarrier_mobs:blank_totem",
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
    "COMPONENTS": "OTHER",
    "CREATE_REDSTONE": "CREATE_REDSTONE",
    "FURNITURE": "BUILDING_FURNITURE",
    "NATURE": "BUILDING_NATURE",
    "TREES": "TREES",
    "CONTAINERS": "BUILDING_CONTAINERS",
    "HOLIDAY": "BUILDING_HOLIDAY",
    "BUILDING_BLOCKS": "BUILDING_BLOCKS",
    "GLASS": "BUILDING_GLASS",
    "LIGHT": "BUILDING_LIGHT",
    "FISHING": "FISHING",
    "BOOKS": "BOOKS",
    "POTIONS": "POTIONS",
    "MUSIC_DISCS": "MUSIC_DISCS",
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

CREATE_PALETTE_MATERIALS = (
    "andesite", "asurine", "calcite", "crimsite", "deepslate", "diorite", "dripstone",
    "granite", "limestone", "ochrum", "scorchia", "scoria", "tuff", "veridium",
)
CREATE_NATURAL_PALETTE_MATERIALS = (
    "asurine", "crimsite", "limestone", "ochrum", "scorchia", "scoria", "veridium",
)
CREATE_COPPER_DECORATION_STAGES = (
    "", "exposed_", "weathered_", "oxidized_", "waxed_", "waxed_exposed_",
    "waxed_weathered_", "waxed_oxidized_",
)
LOCOMETAL_COLORS = (
    "black", "blue", "brown", "chartreuse", "cyan", "diorite", "dripstone", "granite",
    "gray", "green", "light_blue", "light_gray", "lime", "limestone", "magenta",
    "maroon", "ochrum", "olive_green", "orange", "pine_green", "pink", "purple", "red",
    "royal_blue", "scorchia", "sea_green", "tuff", "turquoise", "vermilion", "white",
    "yellow",
)

EXACT_RECLASSIFICATIONS = {
    "alexsmobs:animal_dictionary": "BOOKS",
    "barnyardbuddies:pyrenees_protective_vest": "WEARABLE_MOB_ARMOR",
    "companions:big_bread": "FOOD_EDIBLES",
    "luminous_beasts:ultimate_bestiary": "BOOKS",
    "minecraft:dragon_breath": "POTIONS",
    "minecraft:resin_clump": "ORES",
    "minecraft:crafter": "CREATE_REDSTONE",
    "trials:crafter": "CREATE_REDSTONE",
    # User-reviewed weapon and tool corrections
    "alexscaves:shot_gum": "WEAPON_RANGED",
    "alexsmobs:dimensional_carver": "WEAPON_MELEE",
    "alexsmobs:shattered_dimensional_carver": "WEAPON_MELEE",
    "blastfromthepast:bear_glove": "WEAPON_MELEE",
    "epicfight:glove": "WEAPON_MELEE",
    "epicfight:uchigatana": "WEAPON_MELEE",
    "royalvariations:royal_bomb": "WEAPON_RANGED",
    "skarrier_mobs:blank_totem": "WEAPON_TOTEMS",
    "unearthed_journey:toxic_gas_bomb": "WEAPON_RANGED",
    "bbb:chisel": "BUILDING_TOOLS",
    # User-reviewed crafting parts that are not weapons.
    "minecraft:blade_pottery_sherd": "OTHER",
    "xtraarrows:bat_skin": "OTHER",
    "xtraarrows:vex_wing": "OTHER",
    # Non-wearable blocks and utilities
    "alexscaves:hazmat_skull_block": "BUILDING_BLOCKS",
    "fairylights:skull_light": "BUILDING_LIGHT",
    "toms_storage:ts.adv_wireless_terminal": "OTHER",
    # Components was retired. These liquid or magical mixtures belong with potions.
    "alexscaves:licowitch_radiant_essence": "POTIONS",
    "alexscaves:radiant_essence": "POTIONS",
    "alexsmobs:warped_mixture": "POTIONS",
    "luminous_nether:crimson_contaminant": "POTIONS",
    # Reviewed textile decorations and complete wooden furniture sets.
    **{
        f"handcrafted:{color}_sheet": "BUILDING_FURNITURE"
        for color in (
            "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow",
        )
    },
    **{
        f"kaleidoscope_cookery:{kind}_{wood}": "TREES"
        for kind in ("chair", "cook_stool", "table")
        for wood in (
            "acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle",
            "mangrove", "oak", "spruce", "warped",
        )
    },
    **{
        f"create_train_parts:bellow_{size}": "CREATE_REDSTONE"
        for size in ("1x2", "2x2", "2x3", "3x2", "3x3")
    },
    **{
        f"kaleidoscope_doll:doll_{number}": "WEARABLE_DOLLS"
        for number in range(694, 723)
    },
    **{
        f"kaleidoscope_cookery:{path}": "FOOD_EDIBLES"
        for path in (
            "apple_skewer", "barley_tea_bag", "biluochun_tea_bag", "butter_tea",
            "butter_tea_bag", "candied_apple_skewer", "candied_caterpillar_skewer",
            "candied_chorus_fruit_skewer", "candied_glow_berry_skewer",
            "candied_sweet_berry_skewer", "caterpillar_skewer", "chorus_fruit_skewer",
            "clay_pot_milk_tea", "dried_tea_leaves", "fresh_tea_leaves",
            "glow_berry_skewer", "mystery_tea", "oolong_tea_bag",
            "sakura_fubuki_tea_bag", "sweet_berry_skewer", "tea_egg", "tea_seed",
            "tieguanyin_tea_bag",
        )
    },
    "kaleidoscope_cookery:bamboo_tray": "FOOD_COOKING_EQUIPMENT",
    "kaleidoscope_cookery:kitchen_shelf": "FOOD_COOKING_EQUIPMENT",
    "kaleidoscope_cookery:eight_immortals_table": "TREES",
    "kaleidoscope_cookery:long_bench": "TREES",
    "kaleidoscope_cookery:red_lantern": "BUILDING_LIGHT",
    "kaleidoscope_cookery:tea_banner": "BUILDING_FURNITURE",
    **{
        f"luminous_beasts:{path}": "MUSIC_DISCS"
        for path in (
            "music_disc_a_day_in_the_nether", "music_disc_aerie", "music_disc_amethyst",
            "music_disc_blizzard", "music_disc_creaking_woods", "music_disc_firebugs",
            "music_disc_fizz", "music_disc_left_to_bloom", "music_disc_medieval_sands",
            "music_disc_os_piano", "music_disc_ritual", "music_disc_submerged",
            "music_disc_sunrise", "music_disc_sunset_at_glengorm",
        )
    },
    **{
        f"luminous_beasts:{path}": "BUILDING_FURNITURE"
        for path in (
            "arid_yeti_trophy", "basalt_executioner_trophy", "bogged_bone_stalker_trophy",
            "cherry_tree_ent_trophy", "coral_sea_viper_trophy",
            "crimson_mushlin_king_trophy", "frigid_gator_trophy",
            "golden_hermit_king_trophy", "red_mummy_trophy", "sea_viper_trophy",
            "warped_mushlin_king_trophy", "wind_phoenix_trophy",
            "woodland_witch_doctor_trophy",
        )
    },
    "luminous_beasts:sea_viper_fang": "OTHER",
    "luminous_beasts:tree_ent_root": "OTHER",
    **{
        f"luminous_nether:{path}": "BUILDING_LIGHT"
        for path in (
            "blue_fungirack_lamp", "fungirack_lamp", "green_fungirack_lamp",
            "orange_fungirack_lamp", "pink_fungirack_lamp", "purple_fungirack_lamp",
            "red_fungirack_lamp", "white_fungirack_lamp",
        )
    },
    **{
        f"luminous_nether:{path}": "BUILDING_BLOCKS"
        for path in (
            "chiseled_fungirack_bricks", "decorative_fungirack_bricks", "fungirack",
            "fungirack_brick_dart_trap", "fungirack_bricks", "fungirack_slab",
            "fungirack_stairs", "fungirack_wall", "heavy_ash_block",
            "heavy_fungirack_bricks",
        )
    },
    "luminous_nether:full_golden_mushroom_block": "BUILDING_NATURE",
    "luminous_nether:golden_mushroom_block": "BUILDING_NATURE",
    "luminous_nether:mushroom_stipe": "TREES",
    "luminous_nether:shredded_mushroom_stipe": "TREES",
    "luminous_nether:mushlin_king_trophy": "BUILDING_FURNITURE",
    "luminous_nether:warped_mushlin_king_trophy": "BUILDING_FURNITURE",
    "luminous_nether:music_disc_a_day_in_the_nether": "MUSIC_DISCS",
    "luminous_nether:music_disc_sunset_at_glengorm": "MUSIC_DISCS",
    **{
        f"sophisticatedstorage:{tier}tank": "BUILDING_CONTAINERS"
        for tier in ("", "copper_", "diamond_", "gold_", "iron_", "netherite_")
    },
    "spider_overhaul:sculk_string": "BUILDING_BLOCKS",
    # Unearthed Journey wood architecture. Leaves, saplings and branches remain Nature.
    **{
        f"unearthed_journey:{path}": "TREES"
        for path in (
            "betula_button", "betula_door", "betula_fence", "betula_fence_gate", "betula_log",
            "betula_planks", "betula_pressure_plate", "betula_slab", "betula_stairs",
            "betula_trapdoor", "betula_wood", "stripped_betula_log", "stripped_betula_wood",
            "picea_critchfieldii_door", "picea_critchfieldii_trapdoor", "pine_button",
            "pine_fence", "pine_fence_gate", "pine_log", "pine_planks", "pine_pressure_plate",
            "pine_slab", "pine_stairs", "pine_wood", "stripped_pine_log", "stripped_pine_wood",
            "ceroxylon_quindiuense", "ceroxylon_quindiuense_stripped_log",
            "mossy_ceroxylon_quindiuense_log", "palm_button", "palm_door", "palm_fence",
            "palm_fence_gate", "palm_planks", "palm_pressure_plate", "palm_slab",
            "palm_stairs", "palm_trapdoor", "palm_wood", "stripped_palm_log",
            "stripped_palm_wood",
        )
    },
    # Reviewed non-food plant blocks.
    **{
        f"unearthed_journey:{path}": "BUILDING_NATURE"
        for path in (
            "araciphyllites", "asplenium", "calopogon", "carola", "comptonia_columbiana",
            "cyclanthus_messelensis", "ganoderma", "lepidocaryum", "montrichardia", "nuphaea",
            "psilocybe_azurescens", "sarracenia", "sea_lettuce", "zingiberales",
        )
    },
    **{
        f"minecraft:{path}": "BUILDING_NATURE"
        for path in (
            "allium", "azure_bluet", "blue_orchid", "cornflower", "dandelion", "lilac",
            "mangrove_propagule", "orange_tulip", "oxeye_daisy", "peony", "pink_tulip",
            "poppy", "red_tulip", "sunflower", "torchflower", "white_tulip", "wither_rose",
        )
    },
    "alexscaves:pewen_branch": "BUILDING_NATURE",
    "alexscaves:pewen_pines": "BUILDING_NATURE",
    "netherexp:nether_wart_beard": "BUILDING_NATURE",
    "netherexp:warped_wart_beard": "BUILDING_NATURE",
    # Reviewed Create decorative palettes. Machinery and train equipment remain Create and Redstone.
    **{
        f"create:{path}": "BUILDING_BLOCKS"
        for material in CREATE_PALETTE_MATERIALS
        for path in (
            f"cut_{material}", f"cut_{material}_slab", f"cut_{material}_stairs",
            f"cut_{material}_wall", f"cut_{material}_bricks", f"cut_{material}_brick_slab",
            f"cut_{material}_brick_stairs", f"cut_{material}_brick_wall",
            f"polished_cut_{material}", f"polished_cut_{material}_slab",
            f"polished_cut_{material}_stairs", f"polished_cut_{material}_wall",
            f"small_{material}_bricks", f"small_{material}_brick_slab",
            f"small_{material}_brick_stairs", f"small_{material}_brick_wall",
            f"layered_{material}",
        )
    },
    **{
        f"create:{path}": "BUILDING_BLOCKS"
        for material in CREATE_NATURAL_PALETTE_MATERIALS
        for path in (material, f"{material}_pillar")
    },
    **{
        f"create:{stage}copper_{shape}": "BUILDING_BLOCKS"
        for stage in CREATE_COPPER_DECORATION_STAGES
        for shape in (
            "tiles", "tile_slab", "tile_stairs", "shingles", "shingle_slab", "shingle_stairs",
        )
    },
    **{
        f"railways:{color}_slashed_locometal": "BUILDING_BLOCKS"
        for color in LOCOMETAL_COLORS
    },
    "railways:slashed_locometal": "BUILDING_BLOCKS",
    "create:bound_cardboard_block": "BUILDING_BLOCKS",
    "create:industrial_iron_block": "BUILDING_BLOCKS",
    "create:weathered_iron_block": "BUILDING_BLOCKS",
    "create:rose_quartz": "BUILDING_BLOCKS",
    "create:rose_quartz_block": "BUILDING_BLOCKS",
    "create:rose_quartz_tiles": "BUILDING_BLOCKS",
    "create:small_rose_quartz_tiles": "BUILDING_BLOCKS",
    # Archie's Dinosaurs 1.0.7 exact behavior families
    **{
        f"ec_ad:{path}": "WEAPON_TOTEMS"
        for path in (
            "burning_totem", "echo_totem", "fearful_totem", "lush_totem",
            "soaring_totem", "tidal_totem", "tropic_totem",
        )
    },
    **{
        f"ec_ad:{path}": "WEAPON_MELEE"
        for path in (
            "chorus_scythe", "lush_mace", "theri_claw_copper", "theri_claw_diamond",
            "theri_claw_gold", "theri_claw_iron", "theri_claw_netherite",
        )
    },
    **{
        f"ec_ad:{path}": "WEAPON_RANGED"
        for path in ("ooze_tipped_arrow", "rib_bow", "sculk_bow")
    },
    **{
        f"ec_ad:{part}": "WEARABLE_ARMOR"
        for family in ("chorusoatl", "coralosaur", "lushsaur", "obsidiceratops", "sculksaur", "therimoss")
        for part in (f"{family}_helmet", f"{family}_chestplate", f"{family}_leggings", f"{family}_boots")
    },
    **{
        f"ec_ad:{path}": "SPAWN_EGGS"
        for path in (
            "chorusoatl_spawn_egg", "coralosaur_spawn_egg", "lushsaur_spawn_egg",
            "obsidiceratops_spawn_egg", "sculkraptor_spawn_egg", "sculksaur_spawn_egg",
            "therimoss_spawn_egg",
        )
    },
    "ec_ad:guidebook": "BOOKS",
    "ec_ad:coral_stew": "FOOD_EDIBLES",
    "ec_ad:scorching_horn": "COMPANIONS",
    # Placed hatch eggs. Four hatch rideable mounts. The Sculkraptor egg hatches a non-mount creature.
    "ec_ad:chorusoatl_egg": "TRANSPORT",
    "ec_ad:lushsaur_egg": "TRANSPORT",
    "ec_ad:obsidiceratops_egg": "TRANSPORT",
    "ec_ad:therimoss_egg": "TRANSPORT",
    "ec_ad:sculkraptor_egg": "SPAWN_EGGS",
    "ec_ad:amber": "ORES",
    "ec_ad:amber_ore": "ORES",
    "ec_ad:carved_amber": "ORES",
    "ec_ad:raw_amber_block": "ORES",
    "ec_ad:amber_lamp": "BUILDING_LIGHT",
    **{
        f"ec_ad:{path}": "BUILDING_BLOCKS"
        for path in (
            "amber_brick_slab", "amber_brick_stairs", "amber_brick_wall", "amber_bricks",
            "amber_chain", "cracked_amber_bricks", "polished_amber", "polished_amber_slab",
            "polished_amber_stairs", "polished_amber_wall",
        )
    },
    # Royal Variations travel items
    "royalvariations:empty_royal_telelocator": "TRANSPORT",
    "royalvariations:royal_ender_pearl": "TRANSPORT",
    "royalvariations:royal_telelocator": "TRANSPORT",
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

HOLIDAY_ITEMS = {
    "dustydecorations:gourd",
    "dustydecorations:gourd_3",
    "dustydecorations:ground_2",
    "dustydecorations:hanging_gourds",
    "dustydecorations:pumpkin_scarecrow",
    "dustydecorations:winter_reef",
}

WOOD_SPECIES = (
    "acacia", "ash_willow", "aspen", "bamboo", "birch", "cedar", "cherry", "coconut",
    "crimson", "cypress", "dark_cherry", "dark_oak", "fir", "ghaf", "joshua", "jungle",
    "leguminocarpon", "mangrove", "maple", "oak", "olive", "overgrown_willow", "palo_verde",
    "redwood", "spruce", "sugi", "warped", "willow", "wisteria", "withered",
)

COSMETIC_HEADS = {
    "dustydecorations:cow_skull",
    "dustydecorations:equine_skull",
    "dustydecorations:wolf_skull",
    "endermanoverhaul:tiny_skull",
    "minecraft:creeper_head",
    "minecraft:dragon_head",
    "minecraft:piglin_head",
    "minecraft:player_head",
    "minecraft:skeleton_skull",
    "minecraft:wither_skeleton_skull",
    "minecraft:zombie_head",
    "skarrier_mobs:breacher_skull",
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


def is_glass_or_window(path: str, row: dict) -> bool:
    return has_tag(row, "forge:glass", "c:glass") or has_phrase(
        path,
        "glass",
        "glass_pane",
        "stained_glass",
        "framed_glass",
        "seaglass",
        "window",
        "window_pane",
    )


def is_music_disc(row: dict) -> bool:
    return has_tag(row, "minecraft:items/music_discs", "forge:items/music_discs", "c:items/music_discs")


def is_cosmetic_head(item_id: str, path: str, row: dict) -> bool:
    namespace = item_id.split(":", 1)[0]
    return (
        item_id in COSMETIC_HEADS
        or has_tag(row, "items/mob_heads")
        or namespace == "cnc" and path.endswith("_head")
        or namespace == "rottencreatures" and path.endswith("_head")
    )


def is_tree_product(namespace: str, path: str, row: dict) -> bool:
    if namespace == "casualswing":
        return False
    if has_tag(row, "minecraft:items/leaves", "minecraft:items/saplings", "minecraft:items/flowers", "minecraft:items/corals"):
        return False
    if has_phrase(
        path,
        "leaves", "foliage", "sapling", "flower", "mushroom", "bush", "plant", "roots",
        "sprout", "vine", "grass", "coral", "cactus", "fern", "moss", "lichen",
    ):
        return False
    if has_tag(
        row,
        "minecraft:items/planks", "minecraft:items/logs", "minecraft:items/wooden_",
        "minecraft:items/signs", "minecraft:items/hanging_signs",
    ):
        return True
    if path.endswith(("_sign", "_hanging_sign")) and is_block(row):
        return True
    if has_phrase(path, "log", "wood", "planks", "stripped_log", "stripped_wood"):
        return True
    if not any(path.startswith(species + "_") for species in WOOD_SPECIES):
        return False
    return is_block(row) and path.endswith((
        "_balustrade", "_beam", "_bench", "_button", "_chair", "_corner_trim", "_couch",
        "_counter", "_cupboard", "_desk", "_dining_bench", "_door", "_drawer", "_fancy_bed",
        "_fence", "_fence_gate", "_floorboard", "_frame", "_ladder", "_lattice", "_layer",
        "_nightstand", "_pallet", "_pillar_trim", "_pressure_plate", "_shelf", "_side_table",
        "_slab", "_stairs", "_stool", "_support", "_table", "_trap_door", "_trapdoor", "_trim",
        "_wall",
    ))


def is_false_light_color(path: str) -> bool:
    return path.startswith(("light_blue_", "light_gray_"))


def is_light_source(namespace: str, path: str) -> bool:
    return has_phrase(path, "torch", "lantern", "candle", "lamp", "chandelier", "sconce", "brazier") or (
        namespace == "fairylights" and has_phrase(path, "light", "lights")
    )


def reclassification_target(item_id: str, current_leaf: str, row: dict) -> str:
    namespace, path = item_id.split(":", 1)
    exact = EXACT_RECLASSIFICATIONS.get(item_id)
    if exact is not None:
        return exact
    if namespace == "infinitybuttons" and path.endswith("_secret_button") and is_block(row):
        return "BUILDING_BLOCKS"
    if "shingle" in path and is_block(row):
        return "BUILDING_BLOCKS"
    if namespace == "farm_and_charm" and path.startswith("wild_"):
        return "FOOD_EDIBLES"
    if current_leaf == "FOOD_EDIBLES" and is_block(row) and has_tag(
        row,
        "minecraft:items/flowers",
        "minecraft:items/small_flowers",
        "minecraft:items/tall_flowers",
    ):
        return "BUILDING_NATURE"
    if is_music_disc(row):
        return "MUSIC_DISCS"
    if is_cosmetic_head(item_id, path, row):
        return "WEARABLE_DOLLS"
    if current_leaf == "COMPONENTS":
        return "OTHER"
    if is_smithing_template(item_id, row):
        return "WEAPON_SMITHING_TEMPLATES"
    if namespace in CREATE_NAMESPACES and is_glass_or_window(path, row):
        return "BUILDING_GLASS"
    if namespace in CREATE_NAMESPACES:
        return "CREATE_REDSTONE"
    if namespace == "casualswing":
        return "BUILDING_FURNITURE"
    if current_leaf == "CREATE_REDSTONE":
        return "TREES" if is_tree_product(namespace, path, row) else current_leaf
    if current_leaf in {
        "BUILDING_BLOCKS", "BUILDING_FURNITURE", "BUILDING_GLASS", "BUILDING_LIGHT",
        "BUILDING_NATURE", "WEARABLE_ARMOR",
    } and is_block(row) and is_light_source(namespace, path):
        return "BUILDING_LIGHT"
    if current_leaf == "BUILDING_LIGHT" and is_false_light_color(path):
        if path.endswith(("_awning", "_fabric")):
            return "BUILDING_FURNITURE"
        if namespace == "snowyspirit":
            return "BUILDING_HOLIDAY"
        return "BUILDING_BLOCKS" if is_block(row) else "OTHER"
    if is_tree_product(namespace, path, row):
        return "TREES"
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

    if item_id in EXACT_RECLASSIFICATIONS:
        return EXACT_RECLASSIFICATIONS[item_id]
    if item_id in EXACT:
        return EXACT[item_id]
    if is_music_disc(row):
        return "MUSIC_DISCS"
    if is_cosmetic_head(item_id, path, row):
        return "WEARABLE_DOLLS"
    if "hourglass" in token_set or {"sand", "clock"} <= token_set:
        return "OTHER"

    if class_has(row, "SpawnEggItem") or path.endswith("_spawn_egg"):
        return "SPAWN_EGGS"

    if item_id == "minecraft:totem_of_undying" or (
        namespace == "moretotems" and "totem" in token_set
    ):
        return "WEAPON_TOTEMS"

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

    if is_tree_product(namespace, path, row):
        return "TREES"

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

    if is_glass_or_window(path, row):
        return "BUILDING_GLASS"

    if is_block(row) and is_light_source(namespace, path):
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
    rows = [row for row in rows if row["registry_id"] not in INTERNAL_ITEM_EXCLUSIONS]
    item_ids = [row["registry_id"] for row in rows]
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
    retired_components = data.setdefault("assignments", {}).pop("COMPONENTS", [])
    data["assignments"].setdefault("OTHER", []).extend(retired_components)
    data["assignments"]["OTHER"] = sorted(set(data["assignments"]["OTHER"]))
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
        "source": "Exact logical-server Forge item registry export with reviewed 2026-11-24 semantic corrections",
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
        and not (
            leaf == "BUILDING_GLASS"
            and is_glass_or_window(item_id.split(":", 1)[1], atlas.get(item_id, {}))
        )
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

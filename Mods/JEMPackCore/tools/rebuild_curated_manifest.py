from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ADVANCEMENTS = ROOT / "src" / "main" / "advancements"
RESOURCES = ROOT / "src" / "main" / "resources"
MANIFEST = ADVANCEMENTS / "manifest.json"
STATUS = ADVANCEMENTS / "implementation_status.json"
NATIVE = ADVANCEMENTS / "native-integrations.json"
AUDIT = ROOT / "ADVANCEMENT_AUDIT.md"
TABS = ("qol", "mobs", "arsenal", "fishing", "bosses", "building", "engineering", "cool_stuff")

CUSTOM_IDS = {
    "jem_guide:qol/1", "jem_guide:qol/13", "jem_guide:qol/13_3_1", "jem_guide:qol/13_3_1_1_1",
    "jem_guide:qol/13_3_1_1_2", "jem_guide:qol/7", "jem_guide:qol/7_1", "jem_guide:qol/7_3",
    "jem_guide:qol/8", "jem_guide:qol/8_3", "jem_guide:qol/natures_compass", "jem_guide:mobs/8_4_1",
    "jem_guide:qol/group_homecoming", "jem_guide:qol/universal_leash",
    "jem_guide:arsenal/ballista", "jem_guide:arsenal/catapult", "jem_guide:fishing/4_3",
    "jem_guide:arsenal/efn_armor", "jem_guide:arsenal/efn_armor_collection",
    "jem_guide:arsenal/efn_weapon_collection", "jem_guide:arsenal/efn_weapons",
    "jem_guide:fishing/5_6", "jem_guide:fishing/6_2", "jem_guide:bosses/1",
    "jem_guide:bosses/1_1_1_10_1", "jem_guide:bosses/1_1_1_4", "jem_guide:bosses/1_1_1_5",
    "jem_guide:bosses/1_1_1_7", "jem_guide:bosses/1_1_1_8_1", "jem_guide:bosses/1_2_1",
    "jem_guide:bosses/2_1", "jem_guide:bosses/2_2", "jem_guide:bosses/3_2", "jem_guide:bosses/3_7",
    "jem_guide:bosses/5_1", "jem_guide:building/1", "jem_guide:building/14",
    "jem_guide:building/4", "jem_guide:building/7", "jem_guide:building/7_4",
    "jem_guide:building/8", "jem_guide:cool_stuff/4_4", "jem_guide:cool_stuff/play_a_note",
    "jem_guide:cool_stuff/zipline_attach", "jem_guide:cool_stuff/zipline_transfer",
    "jem_guide:cool_stuff/zipline_release", "jem:engineering/drill_work",
    "jem:engineering/drill_service", "jem:engineering/drill_replaced",
}

PARENTS = {
    "jem_guide:qol/13_3_1": "jem_guide:qol/13", "jem_guide:qol/13_3_1_1_1": "jem_guide:qol/13_3_1",
    "jem_guide:qol/13_3_1_1_2": "jem_guide:qol/13_3_1", "jem_guide:qol/7_1": "jem_guide:qol/7",
    "jem_guide:qol/7_3": "jem_guide:qol/7", "jem_guide:qol/8_3": "jem_guide:qol/8",
    "jem_guide:arsenal/efn_armor_collection": "jem_guide:arsenal/efn_armor",
    "jem_guide:arsenal/efn_weapon_collection": "jem_guide:arsenal/efn_weapons",
    "jem_guide:arsenal/catapult": "jem_guide:arsenal/ballista", "jem_guide:building/7_4": "jem_guide:building/7",
    "jem_guide:cool_stuff/zipline_transfer": "jem_guide:cool_stuff/zipline_attach",
    "jem_guide:cool_stuff/zipline_release": "jem_guide:cool_stuff/zipline_attach",
    "jem:engineering/drill_service": "jem:engineering/drill_work",
    "jem:engineering/drill_replaced": "jem:engineering/drill_service",
}

ROOT_TEXT = {
    "qol": ("Little conveniences make every expedition smoother.", "Маленькие удобства делают каждое путешествие приятнее."),
    "mobs": ("Learn what the creatures around you can actually do.", "Узнай, на что действительно способны окружающие тебя существа."),
    "arsenal": ("Master unusual weapons, clever shots, and heavy machinery.", "Освой необычное оружие, хитрые выстрелы и тяжёлую технику."),
    "fishing": ("Cast into stranger waters and see what answers.", "Забрось удочку в незнакомые воды и посмотри, кто ответит."),
    "bosses": ("Find the great threats of each dimension and bring them down.", "Найди главные угрозы каждого измерения и одолей их."),
    "building": ("Turn a shelter into a place worth returning to.", "Преврати укрытие в место, куда хочется возвращаться."),
    "engineering": ("Begin with Create, then learn what hard work does to a drill.", "Начни с Create, а затем узнай, что тяжёлая работа делает с буром."),
    "cool_stuff": ("Make music, take photographs, collect oddities, and enjoy the ride.", "Играй музыку, фотографируй, собирай диковинки и наслаждайся поездкой."),
}

ICONS = {
    "jem_guide:qol/root": "minecraft:compass", "jem_guide:qol/1": "minecraft:spyglass",
    "jem_guide:qol/13": "paraglider:paraglider", "jem_guide:qol/13_3_1": "paraglider:spirit_orb",
    "jem_guide:qol/13_3_1_1_1": "minecraft:golden_apple", "jem_guide:qol/13_3_1_1_2": "minecraft:rabbit_foot",
    "jem_guide:qol/7": "minecraft:bundle", "jem_guide:qol/7_1": "minecraft:chest",
    "jem_guide:qol/7_3": "minecraft:ender_chest", "jem_guide:qol/8": "minecraft:iron_axe",
    "jem_guide:qol/8_3": "minecraft:red_mushroom_block", "jem_guide:qol/natures_compass": "naturescompass:naturescompass",
    "jem_guide:qol/group_homecoming": "minecraft:red_bed", "jem_guide:qol/universal_leash": "minecraft:lead",
    "jem_guide:mobs/root": "minecraft:lead", "jem_guide:mobs/8_4_1": "minecraft:white_bed",
    "jem_guide:arsenal/root": "minecraft:crossbow", "jem_guide:arsenal/ballista": "siegeweapons:ballista_item",
    "jem_guide:arsenal/catapult": "siegeweapons:catapult_item", "jem_guide:fishing/root": "minecraft:fishing_rod",
    "jem_guide:arsenal/efn_armor": "efn:duskfire_chestplate", "jem_guide:arsenal/efn_armor_collection": "efn:ruinfighter_chestplate",
    "jem_guide:arsenal/efn_weapon_collection": "efn:hf_murasama", "jem_guide:arsenal/efn_weapons": "efn:ruinsgreatsword",
    "jem_guide:fishing/4_3": "tide:sunflower_fishing_rod", "jem_guide:fishing/5_6": "tide:midas_fish",
    "jem_guide:fishing/6_2": "minecraft:cod_bucket", "jem_guide:bosses/root": "minecraft:nether_star",
    "jem_guide:bosses/1": "minecraft:wither_skeleton_skull", "jem_guide:bosses/1_1_1_10_1": "alexsmobs:spawn_egg_void_worm",
    "jem_guide:bosses/1_1_1_4": "cataclysm:ancient_remnant_spawn_egg", "jem_guide:bosses/1_1_1_5": "cataclysm:the_leviathan_spawn_egg",
    "jem_guide:bosses/1_1_1_7": "cataclysm:maledictus_spawn_egg", "jem_guide:bosses/1_1_1_8_1": "cataclysm:ignis_spawn_egg",
    "jem_guide:bosses/1_2_1": "minecraft:quartz", "jem_guide:bosses/2_1": "block_factorys_bosses:yeti_spawn_egg",
    "jem_guide:bosses/2_2": "block_factorys_bosses:infernal_dragon_spawn_egg", "jem_guide:bosses/3_2": "rottencreatures:mummy_spawn_egg",
    "jem_guide:bosses/3_7": "deep_dark_regrowth:stalker_spawn_egg", "jem_guide:bosses/5_1": "alexscaves:spawn_egg_luxtructosaurus",
    "jem_guide:building/root": "minecraft:scaffolding", "jem_guide:building/1": "minecraft:oak_log",
    "jem_guide:building/14": "minecraft:emerald",
    "jem_guide:building/4": "minecraft:oak_stairs", "jem_guide:building/7": "minecraft:bell",
    "jem_guide:building/7_4": "minecraft:amethyst_shard", "jem_guide:building/8": "minecraft:campfire",
    "jem:engineering/root": "create:wrench", "jem:engineering/drill_work": "create:mechanical_drill",
    "jem:engineering/drill_service": "minecraft:iron_ingot", "jem:engineering/drill_replaced": "create:andesite_alloy",
    "jem_guide:cool_stuff/root": "minecraft:painting", "jem_guide:cool_stuff/4_4": "minecraft:brush",
    "jem_guide:cool_stuff/play_a_note": "minecraft:note_block", "jem_guide:cool_stuff/zipline_attach": "minecraft:chain",
    "jem_guide:cool_stuff/zipline_transfer": "minecraft:compass", "jem_guide:cool_stuff/zipline_release": "minecraft:slime_ball",
}


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def criterion_path(advancement_id: str) -> Path:
    namespace, path = advancement_id.split(":", 1)
    return RESOURCES / "data" / namespace / "advancements" / f"{path}.json"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--legacy-jar", type=Path)
    args = parser.parse_args()
    document = read_json(MANIFEST)
    old = {node["advancement_id"]: node for node in document["nodes"]}
    providers = {node["advancement_id"]: node["provider"] for node in read_json(STATUS)["nodes"]}
    root_ids = {f"{'jem' if tab == 'engineering' else 'jem_guide'}:{tab}/root" for tab in TABS}
    wanted = root_ids | CUSTOM_IDS
    missing = wanted - set(old)
    if missing:
        raise ValueError(f"Missing curated nodes: {sorted(missing)}")
    nodes = []
    for advancement_id in sorted(wanted):
        node = dict(old[advancement_id])
        tab = node["tab"]
        source = read_json(criterion_path(advancement_id))
        node["criteria"] = source["criteria"]
        node["requirements"] = source.get("requirements")
        node["provider"] = providers[advancement_id]
        node["parent"] = PARENTS.get(advancement_id, None if advancement_id in root_ids else f"{'jem' if tab == 'engineering' else 'jem_guide'}:{tab}/root")
        node["icon"] = ICONS[advancement_id]
        if advancement_id in root_ids:
            node["exact_en"], node["exact_ru"] = ROOT_TEXT[tab]
        nodes.append(node)
    replacements = {
        "jem_guide:qol/natures_compass": ("Obtain a Nature's Compass.", "Получи компас природы.", "Get a Nature's Compass and keep it in your inventory.", "Получи компас природы и положи его в инвентарь."),
        "jem_guide:arsenal/ballista": ("Get your hands on a Ballista.", "Раздобудь баллисту.", "Obtain a Ballista item.", "Получи предмет баллисты."),
        "jem_guide:arsenal/catapult": ("Get your hands on a Catapult.", "Раздобудь катапульту.", "Obtain a Catapult item.", "Получи предмет катапульты."),
    }
    for node in nodes:
        replacement = replacements.get(node["advancement_id"])
        if replacement:
            node["short_en"], node["short_ru"], node["exact_en"], node["exact_ru"] = replacement
    nodes.sort(key=lambda node: (TABS.index(node["tab"]), str(node["number"]), node["advancement_id"]))
    counts = {tab: sum(node["tab"] == tab for node in nodes) for tab in TABS}
    write_json(MANIFEST, {"schema_version": 3, "expected_counts": counts, "nodes": nodes})

    native = read_json(NATIVE)
    gateways = {
        "legendary_monsters": "jem_guide:bosses/root", "exposure": "jem_guide:cool_stuff/root",
        "kaleidoscope_doll": "jem_guide:cool_stuff/root", "critters_and_crawlers": "jem_guide:mobs/root",
        "alexs_mobs": "jem_guide:mobs/root", "alexs_caves_mobs": "jem_guide:mobs/root",
        "immersive_weathering": "jem_guide:building/root", "cataclysm": "jem_guide:bosses/root",
        "alexs_caves_bosses": "jem_guide:bosses/root", "alexs_caves_arsenal": "jem_guide:arsenal/root",
    }
    native["integrations"] = [integration for integration in native["integrations"] if integration["name"] != "create"]
    for integration in native["integrations"]:
        integration["gateway"] = gateways[integration["name"]]
    native["integrations"].insert(0, {"name": "create", "jar_pattern": "create-*.jar", "namespace": "create", "gateway": "jem:engineering/root", "include_all_visible": True})
    write_json(NATIVE, native)

    legacy_ids = set(old)
    if args.legacy_jar:
        with zipfile.ZipFile(args.legacy_jar) as archive:
            legacy_ids = {
                f"{name.split('/')[1]}:{name.split('/advancements/', 1)[1][:-5]}"
                for name in archive.namelist()
                if name.endswith(".json") and (name.startswith("data/jem_guide/advancements/") or name.startswith("data/jem/advancements/"))
            }
    removed = sorted(legacy_ids - wanted)
    lines = ["# Advancement Audit", "", "## Result", "", f"- Curated custom nodes: {len(nodes)}", f"- Removed inherited nodes: {len(removed)}", "- Native mod tabs are replaced at the datapack definition level.", "- Selected native advancements retain their original criteria and live inside JEM tabs.", "- Unselected mod-tab advancements use a hidden impossible criterion with a new criterion name, preventing both trigger registration and direct awards by the old name.", "- Engineering contains every visible Create advancement plus three drill-wear milestones.", "", "## Removed inherited IDs", ""]
    lines.extend(f"- `{advancement_id}`" for advancement_id in removed)
    AUDIT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({"custom_nodes": len(nodes), "removed": len(removed), "counts": counts}))


if __name__ == "__main__":
    main()

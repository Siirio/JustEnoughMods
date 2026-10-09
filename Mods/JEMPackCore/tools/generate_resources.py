from __future__ import annotations

import argparse
import fnmatch
import json
import re
import shutil
import zipfile
from collections import defaultdict
from pathlib import Path


MOD_ID = "jem_advancements"
GUIDE_MOD_ID = MOD_ID
LEGACY_GUIDE_MOD_ID = "jem_advancement_guide"
TAB_ICONS = {
    "qol": "minecraft:spyglass",
    "mobs": "minecraft:lead",
    "arsenal": "minecraft:diamond_sword",
    "fishing": "minecraft:fishing_rod",
    "bosses": "minecraft:nether_star",
    "building": "minecraft:bricks",
    "engineering": "create:mechanical_drill",
    "cool_stuff": "minecraft:firework_rocket",
}
EXACT_UPSTREAM_ADVANCEMENTS = {
    "jem_guide:building/9_1": "immersive_weathering:immersive_weathering/crack_on",
    "jem_guide:building/9_2": "immersive_weathering:immersive_weathering/moss_off",
    "jem_guide:cool_stuff/4": "exposure:adventure/exposure",
    "jem_guide:fishing/5_3": "tide:fish_in_void",
    "jem_guide:mobs/8_3": "minecraft:primal/give_petrified_fruit",
    "jem_guide:mobs/15_1": "alexscaves:alexscaves/atlatitan_stomp",
    "jem_guide:mobs/10_1": "alexsmobs:alexsmobs/crimson_mosquito_sick",
    "jem_guide:mobs/14_4": "alexsmobs:alexsmobs/save_cachalot_whale",
    "jem_guide:mobs/2_2_1": "alexsmobs:alexsmobs/mantis_shrimp_bucket",
    "jem_guide:mobs/5": "minecraft:adventure/recruit_guard",
    "jem_guide:mobs/9": "alexsmobs:alexsmobs/breed_anteater",
    "jem_guide:scary/9_1": "netherexp:nether/exorcism",
    "jem_guide:scary/3": "deep_dark_regrowth:bad_place",
    "jem_guide:scary/9_4_1": "incision:calciumlegos",
    "jem_guide:bosses/1_1_1_10": "alexsmobs:alexsmobs/void_worm_summon",
    "jem_guide:bosses/5": "alexscaves:alexscaves/summon_luxtructosaurus",
    "jem_guide:world/6_2": "alexscaves:alexscaves/cave_codex",
    "jem_guide:fishing/7_2": "luminousworld:crab_rave",
    "jem_guide:cool_stuff/4_6": "exposure:adventure/spotlight",
}
EXACT_JAVA_CALLERS = {
    "jem_guide:qol/5": "SearchManagerMixin#afterSuccessfulSearch",
    "jem:engineering/drill_work": "BlockBreakingMovementBehaviourMixin#onBlockBroken",
    "jem:engineering/black_tooth": "BlockBreakingMovementBehaviourMixin#onBlockBroken",
    "jem:engineering/drill_dull": "BlockBreakingMovementBehaviourMixin#onBlockBroken",
    "jem:engineering/drill_service": "AbstractContraptionEntityMixin#serviceDrills",
    "jem:engineering/batch_service": "AbstractContraptionEntityMixin#serviceDrills",
    "jem:engineering/drill_last_service": "AbstractContraptionEntityMixin#serviceDrills",
    "jem:engineering/drill_replaced": "BlockBreakingMovementBehaviourMixin#onBlockBroken",
    "jem:engineering/bedrock_crack": "BlockBreakingMovementBehaviourMixin#breakOnBedrock",
    "jem_guide:qol/1": "LivingEntityMixin#detectSpyglassTarget",
    "jem_guide:qol/13_3_1_1_1": "SimpleBargainMixin#afterBargain",
    "jem_guide:qol/13_3_1_1_2": "SimpleBargainMixin#afterBargain",
    "jem_guide:cool_stuff/2": "DollGiftBoxBlockMixin#afterGiftBoxOpened",
    "jem_guide:mobs/7": "BugNetItemMixin#afterCapture",
    "jem_guide:qol/3": "NaturesCompassItemMixin#afterSearch",
    "jem_guide:qol/3_1": "NaturesCompassItemMixin#afterSearch",
    "jem_guide:fishing/6_2": "CastingNetItemMixin#afterNetUsed",
    "jem_guide:qol/7": "BackpackedMixin#onEquipBackpack",
    "jem_guide:qol/7_1": "BackpackedServerPlayHandlerMixin#onChangeAugment",
    "jem_guide:qol/7_3": "BackpackedMixin#onEquipBackpack",
    "jem_guide:qol/8": "TreeChopIntegration#onTreeFelled",
    "jem_guide:qol/8_3": "TreeChopIntegration#onTreeFelled",
    "jem_guide:mobs/8_4": "ForgeGameplayEvents#onEntityInteract",
    "jem_guide:mobs/8_4_1": "ForgeGameplayEvents#onItemCrafted",
    "jem_guide:building/14": "ForgeGameplayEvents#onItemCrafted",
    "jem_guide:cool_stuff/4_4": "ForgeGameplayEvents#onItemCrafted",
    "jem_guide:fishing/4": "ForgeGameplayEvents#onItemFished",
}


def read_json(archive: zipfile.ZipFile, name: str) -> dict | None:
    try:
        return json.loads(archive.read(name).decode("utf-8-sig"))
    except KeyError:
        return None


def key_base(node: dict) -> str:
    path = node["advancement_id"].split(":", 1)[1]
    tab, leaf = path.split("/", 1)
    return f"{node['advancement_id'].split(':', 1)[0]}.advancement.{tab}.{leaf.replace('/', '.')}"


def guide_key(node: dict) -> str:
    path = node["advancement_id"].split(":", 1)[1]
    tab, leaf = path.split("/", 1)
    return f"{GUIDE_MOD_ID}.advancement_guide.entry.{tab}.{leaf.replace('/', '.').replace('_', '.')}"


def node_trigger(node: dict) -> str:
    namespace, path = node["advancement_id"].split(":", 1)
    return f"{MOD_ID}:node/{namespace}/{path}"


def normalize(value: str) -> str:
    return " ".join(re.sub(r"[^a-z0-9]+", " ", value.lower()).split())


def registry_names(mods_dir: Path) -> dict[str, dict[str, list[str]]]:
    result: dict[str, dict[str, set[str]]] = {
        "item": defaultdict(set),
        "block": defaultdict(set),
        "entity": defaultdict(set),
    }
    for jar in mods_dir.glob("*.jar"):
        try:
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    if not re.fullmatch(r"assets/[^/]+/lang/en_us\.json", name):
                        continue
                    try:
                        lang = json.loads(archive.read(name).decode("utf-8-sig"))
                    except (json.JSONDecodeError, UnicodeDecodeError):
                        continue
                    for key, display in lang.items():
                        match = re.match(r"^(item|block|entity)\.([a-z0-9_.-]+)\.([a-z0-9_./-]+)$", key)
                        if match and isinstance(display, str) and "%" not in display:
                            kind, namespace, path = match.groups()
                            if kind == "entity" and path.endswith(("_part", "_segment")):
                                continue
                            normalized = normalize(display)
                            if len(normalized) >= 4:
                                result[kind][normalized].add(f"{namespace}:{path}")
                                short = normalize(display.split(",", 1)[0])
                                if " the " in short:
                                    short = short.split(" the ", 1)[0]
                                if len(short) >= 5:
                                    result[kind][short].add(f"{namespace}:{path}")
        except (OSError, zipfile.BadZipFile):
            continue
    return {kind: {name: sorted(ids) for name, ids in names.items()} for kind, names in result.items()}


def code_referenced_advancements(mods_dir: Path) -> set[str]:
    available: set[str] = set()
    jars = sorted(mods_dir.glob("*.jar"))
    for jar in jars:
        try:
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    match = re.fullmatch(r"data/([^/]+)/advancements/(.+)\.json", name)
                    if match:
                        available.add(f"{match.group(1)}:{match.group(2)}")
        except (OSError, zipfile.BadZipFile):
            continue

    referenced: set[str] = set()
    pattern = re.compile(rb"[a-z0-9_.-]+:[a-z0-9_./-]+")
    for jar in jars:
        try:
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    if not name.endswith(".class"):
                        continue
                    for match in pattern.finditer(archive.read(name)):
                        advancement_id = match.group().decode("ascii")
                        if advancement_id in available:
                            referenced.add(advancement_id)
        except (OSError, zipfile.BadZipFile):
            continue
    return referenced


def longest_name(text: str, names: dict[str, list[str]], prefix: str) -> tuple[str, list[str]] | None:
    normalized = normalize(text)
    if not normalized.startswith(prefix):
        return None
    target = normalized[len(prefix):].strip()
    matches = [(name, ids) for name, ids in names.items() if name in target[:120]]
    return max(matches, key=lambda pair: len(pair[0]), default=None)


def name_after_verb(text: str, names: dict[str, list[str]], verbs: tuple[str, ...]) -> tuple[str, list[str]] | None:
    normalized = normalize(text.split(".", 1)[0])
    positions = [(match.start(), verb) for verb in verbs for match in re.finditer(rf"\b{re.escape(verb.strip())}\s+", normalized)]
    if not positions:
        return None
    position, verb = min(positions)
    target = normalized[position + len(verb):position + len(verb) + 160]
    matches = [(name, ids) for name, ids in names.items() if re.search(rf"\b{re.escape(name)}\b", target)]
    return max(matches, key=lambda pair: len(pair[0]), default=None)


def inferred_vanilla(node: dict, names: dict[str, dict[str, list[str]]]) -> tuple[dict, list[list[str]], str] | None:
    exact = node["exact_en"]
    normalized = normalize(exact)
    if any(phrase in normalized for phrase in (
        "then defeat", "after defeating", "without taking", "during one fight", "only after",
        "merely", "does not count", "do not count", "do not kill", "let it kill", "any eligible", "same item", "same entity", "same rod",
    )):
        return None
    killed = name_after_verb(exact, names["entity"], ("defeat ", "kill "))
    if killed:
        _, ids = killed
        criteria = {
            ("completion" if len(ids) == 1 else f"entity_{index + 1}"): {
                "trigger": "minecraft:player_killed_entity",
                "conditions": {"entity": {"type": value}},
            }
            for index, value in enumerate(ids)
        }
        return criteria, [list(criteria)], "minecraft:player_killed_entity"
    obtained = longest_name(exact, names["item"], "obtain ") or longest_name(exact, names["item"], "get ")
    if obtained and " and " not in normalized[:160]:
        _, ids = obtained
        return {
            "completion": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ids}]}}
        }, [["completion"]], "minecraft:inventory_changed"
    placed = longest_name(exact, names["block"], "place ")
    if placed and " and " not in normalized[:160]:
        _, ids = placed
        criteria = {
            ("completion" if len(ids) == 1 else f"block_{index + 1}"): {
                "trigger": "minecraft:placed_block",
                "conditions": {
                    "location": [{
                        "condition": "minecraft:location_check",
                        "predicate": {"block": {"blocks": [value]}},
                    }]
                },
            }
            for index, value in enumerate(ids)
        }
        return criteria, [list(criteria)], "minecraft:placed_block"
    consumed = longest_name(exact, names["item"], "eat ") or longest_name(exact, names["item"], "drink ")
    if consumed and " and " not in normalized[:160]:
        _, ids = consumed
        criteria = {
            ("completion" if len(ids) == 1 else f"item_{index + 1}"): {
                "trigger": "minecraft:consume_item", "conditions": {"item": {"items": [value]}}
            }
            for index, value in enumerate(ids)
        }
        return criteria, [list(criteria)], "minecraft:consume_item"
    tamed = longest_name(exact, names["entity"], "tame ")
    if tamed and " and " not in normalized[:160]:
        _, ids = tamed
        criteria = {
            ("completion" if len(ids) == 1 else f"entity_{index + 1}"): {
                "trigger": "minecraft:tame_animal", "conditions": {"entity": {"type": value}}
            }
            for index, value in enumerate(ids)
        }
        return criteria, [list(criteria)], "minecraft:tame_animal"
    bred = longest_name(exact, names["entity"], "breed ")
    if bred and " and " not in normalized[:160]:
        _, ids = bred
        criteria = {
            ("completion" if len(ids) == 1 else f"entity_{index + 1}"): {
                "trigger": "minecraft:bred_animals", "conditions": {"child": {"type": value}}
            }
            for index, value in enumerate(ids)
        }
        return criteria, [list(criteria)], "minecraft:bred_animals"
    return None


def exact_vanilla(node: dict) -> tuple[dict, list[list[str]], str] | None:
    node_id = node["advancement_id"]
    biome_locations = {
        "jem_guide:scary/4": ["deep_dark_regrowth:deep_light"],
        "jem_guide:scary/9_4": ["incision:eroded_yard"],
        "jem_guide:scary/11": ["eldritch_end:hasturian_wastes", "eldritch_end:primordial_abyss"],
    }
    structure_locations = {
        "jem_guide:world/5_1_1": ["ribbits:ribbit_village"],
        "jem_guide:world/6": ["alexscaves:underground_cabin"],
        "jem_guide:world/7": ["minecraft:end_city"],
        "jem_guide:world/7_1": ["end_villager_trader:end_trader_ship"],
    }
    locations = biome_locations.get(node_id)
    location_key = "biome"
    if locations is None:
        locations = structure_locations.get(node_id)
        location_key = "structure"
    if locations:
        criteria = {
            f"location_{index + 1}": {
                "trigger": "minecraft:location",
                "conditions": {
                    "player": [{
                        "condition": "minecraft:entity_properties",
                        "entity": "this",
                        "predicate": {"location": {location_key: location}},
                    }]
                },
            }
            for index, location in enumerate(locations)
        }
        return criteria, [list(criteria)], "minecraft:location"
    if node_id == "jem_guide:fishing/4_3":
        return {
            "caught_rod": {
                "trigger": "minecraft:fishing_rod_hooked",
                "conditions": {"item": {"items": ["tide:sunflower_fishing_rod"]}},
            },
            "caught_fish_with_rod": {
                "trigger": "minecraft:fishing_rod_hooked",
                "conditions": {
                    "rod": {"items": ["tide:sunflower_fishing_rod"]},
                    "item": {"tag": "forge:raw_fishes"},
                },
            },
        }, [["caught_rod"], ["caught_fish_with_rod"]], "minecraft:fishing_rod_hooked"
    if node_id == "jem_guide:fishing/5_6":
        return {
            "completion": {
                "trigger": "minecraft:fishing_rod_hooked",
                "conditions": {"item": {"items": ["tide:midas_fish"]}},
            }
        }, [["completion"]], "minecraft:fishing_rod_hooked"
    if node_id == "jem_guide:qol/13_3_1":
        return {
            "completion": {
                "trigger": "minecraft:inventory_changed",
                "conditions": {
                    "items": [{
                        "items": ["paraglider:spirit_orb"],
                        "count": {"min": 4},
                    }]
                },
            }
        }, [["completion"]], "minecraft:inventory_changed"
    if node_id == "jem_guide:qol/4":
        return {
            "completion": {
                "trigger": "minecraft:inventory_changed",
                "conditions": {"items": [{"items": ["travelerscompass:travelerscompass"]}]},
            }
        }, [["completion"]], "minecraft:inventory_changed"
    if node_id == "jem_guide:bosses/1":
        return {
            "wither": {
                "trigger": "minecraft:player_killed_entity",
                "conditions": {"entity": {"type": "minecraft:wither"}},
            },
            "star": {
                "trigger": "minecraft:inventory_changed",
                "conditions": {"items": [{"items": ["minecraft:nether_star"]}]},
            },
        }, [["wither"], ["star"]], "minecraft:player_killed_entity + minecraft:inventory_changed"
    if node_id == "jem_guide:building/1":
        return {
            "completion": {
                "trigger": "minecraft:placed_block",
                "conditions": {"location": {"block": {"tag": "gwoodworks:beam"}}},
            }
        }, [["completion"]], "minecraft:placed_block"
    if node_id == "jem_guide:building/7":
        return {
            "completion": {
                "trigger": "minecraft:placed_block",
                "conditions": {"location": {"block": {"tag": "chimes:wind_chimes"}}},
            }
        }, [["completion"]], "minecraft:placed_block"
    if node_id == "jem_guide:building/7_4":
        return {
            "completion": {
                "trigger": "minecraft:placed_block",
                "conditions": {
                    "location": [{
                        "condition": "minecraft:location_check",
                        "predicate": {"block": {"blocks": ["chimes:amethyst_chimes"]}},
                    }]
                },
            }
        }, [["completion"]], "minecraft:placed_block"
    if node_id == "jem_guide:building/8":
        return {
            "completion": {
                "trigger": "minecraft:placed_block",
                "conditions": {
                    "location": [{
                        "condition": "minecraft:location_check",
                        "predicate": {"block": {"tag": "sootychimneys:chimneys"}},
                    }]
                },
            }
        }, [["completion"]], "minecraft:placed_block"
    return None


def criterion_for(node: dict, legacy: dict | None, rule: dict | None, names: dict[str, dict[str, list[str]]]) -> tuple[dict, list[list[str]], str]:
    if node.get("criteria"):
        criteria = node["criteria"]
        requirements = node.get("requirements") or [[name] for name in criteria]
        return criteria, requirements, node.get("provider") or ", ".join(sorted({criterion["trigger"] for criterion in criteria.values()}))
    if node["parent"] is None and node["tab"] != "engineering":
        return {"completion": {"trigger": "minecraft:tick"}}, [["completion"]], "minecraft:tick"
    if legacy:
        triggers = [criterion.get("trigger", "") for criterion in legacy.get("criteria", {}).values()]
        if triggers and all(trigger.startswith("minecraft:") and trigger != "minecraft:impossible" for trigger in triggers):
            criteria = legacy["criteria"]
            if len(criteria) == 1 and "completion" not in criteria:
                criteria = {"completion": next(iter(criteria.values()))}
            requirements = legacy.get("requirements") or [[name] for name in criteria]
            requirements = [["completion" if name not in criteria and len(criteria) == 1 else name for name in group] for group in requirements]
            return criteria, requirements, ", ".join(sorted(set(triggers)))
    exact = exact_vanilla(node)
    if exact:
        return exact
    exact_upstream = EXACT_UPSTREAM_ADVANCEMENTS.get(node["advancement_id"])
    if exact_upstream:
        return {
            "completion": {
                "trigger": f"{MOD_ID}:upstream_advancement",
                "conditions": {"source": exact_upstream},
            }
        }, [["completion"]], f"{MOD_ID}:upstream_advancement"
    if rule and rule.get("kind") == "ADVANCEMENT":
        all_sources = list(dict.fromkeys(rule.get("advancementsAll") or []))
        any_sources = [source for source in dict.fromkeys(rule.get("advancementsAny") or []) if source not in all_sources]
        criteria: dict[str, dict] = {}
        requirements: list[list[str]] = []
        for index, source in enumerate(all_sources):
            name = "completion" if len(all_sources) == 1 and not any_sources else f"all_{index + 1}"
            criteria[name] = {"trigger": f"{MOD_ID}:upstream_advancement", "conditions": {"source": source}}
            requirements.append([name])
        if any_sources:
            names = []
            for index, source in enumerate(any_sources):
                name = "completion" if len(any_sources) == 1 and not all_sources else f"any_{index + 1}"
                criteria[name] = {"trigger": f"{MOD_ID}:upstream_advancement", "conditions": {"source": source}}
                names.append(name)
            requirements.append(names)
        if criteria:
            return criteria, requirements, f"{MOD_ID}:upstream_advancement"
    inferred = inferred_vanilla(node, names)
    if inferred:
        return inferred
    return {"completion": {"trigger": node_trigger(node)}}, [["completion"]], node_trigger(node)


def display_for(node: dict, legacy: dict | None) -> dict:
    display = dict((legacy or {}).get("display") or {})
    display["icon"] = {"item": node.get("icon", TAB_ICONS[node["tab"]])}
    base = key_base(node)
    display["title"] = {"translate": f"{base}.title"}
    display["description"] = {"translate": f"{base}.description"}
    display["frame"] = "challenge" if "CHALLENGE" in node["flags"] else "goal" if "GOAL" in node["flags"] else "task"
    display["show_toast"] = node.get("show_toast", True)
    display["announce_to_chat"] = node.get("announce_to_chat", True)
    display["hidden"] = "HIDDEN" in node["flags"]
    display.pop("x", None)
    display.pop("y", None)
    if node["parent"] is None:
        display["background"] = "minecraft:textures/gui/advancements/backgrounds/stone.png"
    else:
        display.pop("background", None)
    return display


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def matrix_cell(value: object) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def write_matrix(path: Path, nodes: list[dict]) -> None:
    lines = [
        "# Implementation Matrix",
        "",
        "| Advancement ID | Tab | Parent | Strategy | Authoritative source | Hook/event/criterion | Stateful? | Test | Status |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for node in nodes:
        provider = node["criterion_provider"]
        unresolved = provider.startswith(f"{MOD_ID}:node/")
        status = "BLOCKED: authoritative caller not implemented" if unresolved else "IMPLEMENTED"
        values = (
            node["advancement_id"],
            node["tab"],
            node["parent"] or "—",
            node["strategy"],
            node["authoritative_source"],
            provider,
            "yes" if node["stateful"] else "no",
            node["test"],
            status,
        )
        lines.append("| " + " | ".join(matrix_cell(value) for value in values) + " |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def safe_replace_dir(path: Path, root: Path) -> None:
    resolved = path.resolve()
    resolved.relative_to(root.resolve())
    if resolved.exists():
        shutil.rmtree(resolved)
    resolved.mkdir(parents=True)


def safe_remove_dir(path: Path, root: Path) -> None:
    resolved = path.resolve()
    resolved.relative_to(root.resolve())
    if resolved.exists():
        shutil.rmtree(resolved)


def remove_generated_native_files(index_path: Path, resources: Path) -> None:
    if not index_path.exists():
        return
    document = json.loads(index_path.read_text(encoding="utf-8"))
    for relative in document.get("files", []):
        target = (resources / relative).resolve()
        target.relative_to(resources)
        if target.exists():
            target.unlink()


def find_mod_jar(mods_dir: Path, pattern: str) -> Path:
    matches = sorted(path for path in mods_dir.glob("*.jar") if fnmatch.fnmatch(path.name.lower(), pattern.lower()))
    if len(matches) != 1:
        raise ValueError(f"Expected one jar matching {pattern}, found {[path.name for path in matches]}")
    return matches[0]


def load_language(jar: Path, language: str) -> dict[str, str]:
    values: dict[str, str] = {}
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if not re.fullmatch(r"assets/[^/]+/lang/" + language + r"\.json", name):
                continue
            source = read_json(archive, name) or {}
            values.update({key: value for key, value in source.items() if isinstance(value, str)})
    return values


def component_text(component: object, translations: dict[str, str]) -> str:
    if isinstance(component, str):
        return component
    if not isinstance(component, dict):
        return str(component)
    if "text" in component:
        return str(component["text"])
    if "translate" in component:
        text = translations.get(component["translate"], component["translate"])
        for argument in component.get("with", []):
            text = text.replace("%s", component_text(argument, translations), 1)
        return text
    return ""


NATIVE_ICON_OVERRIDES = {
    "legendary_monsters:defeat_ambusher": "minecraft:iron_sword",
}

NATIVE_BACKGROUND_OVERRIDES = {
    "create:root": "minecraft:textures/gui/advancements/backgrounds/stone.png",
}


def native_guide_entries(native_nodes: list[dict], resources: Path, guide_assets: Path, en: dict[str, str], ru: dict[str, str], index_entries: list[dict]) -> None:
    for sortnum, node in enumerate(native_nodes, start=1000):
        if node["proof"] != "NATIVE_CRITERIA_UNCHANGED":
            continue
        source_path = resources / node["relative_path"]
        source = json.loads(source_path.read_text(encoding="utf-8"))
        display = source.get("display", {})
        namespace, path = node["advancement_id"].split(":", 1)
        key_path = f"native.{namespace}.{path.replace('/', '.')}"
        title_key = f"{GUIDE_MOD_ID}.advancement.{key_path}.title"
        description_key = f"{GUIDE_MOD_ID}.advancement.{key_path}.description"
        en_translations = load_language(Path(node["source_jar_path"]), "en_us")
        ru_translations = load_language(Path(node["source_jar_path"]), "ru_ru")
        en[title_key] = component_text(display.get("title", path), en_translations)
        en[description_key] = component_text(display.get("description", "Native advancement."), en_translations)
        ru[title_key] = component_text(display.get("title", path), ru_translations) or en[title_key]
        ru[description_key] = component_text(display.get("description", "Родное достижение."), ru_translations) or en[description_key]
        icon = NATIVE_ICON_OVERRIDES.get(node["advancement_id"], (display.get("icon") or {}).get("item", "minecraft:paper"))
        entry = {
            "name": title_key,
            "category": f"{GUIDE_MOD_ID}:{node['guide_tab']}",
            "icon": icon,
            "pages": [
                {"type": "patchouli:text", "title": title_key, "text": description_key},
                {"type": "patchouli:spotlight", "item": icon, "text": ""},
            ],
            "read_by_default": True,
            "sortnum": sortnum,
        }
        entry_path = guide_assets / "patchouli_books" / "just_enough_guide" / "en_us" / "entries" / "native" / namespace / f"{path}.json"
        write_json(entry_path, entry)
        index_entries.append({"id": node["advancement_id"], "guide": description_key, "subject": title_key, "itemRefs": [icon]})


def write_native_integrations(document: dict, mods_dir: Path, resources: Path, custom_ids: set[str]) -> list[dict]:
    generated: list[dict] = []
    owned_ids = set(custom_ids)
    selected: dict[str, tuple[Path, dict, dict]] = {}
    all_entries: dict[str, tuple[Path, dict]] = {}
    for jar in sorted(mods_dir.glob("*.jar")):
        try:
            with zipfile.ZipFile(jar) as archive:
                for name in archive.namelist():
                    match = re.fullmatch(r"data/([^/]+)/advancements/(.+)\.json", name)
                    if not match:
                        continue
                    namespace, path = match.groups()
                    source = read_json(archive, name)
                    if source is not None:
                        all_entries.setdefault(f"{namespace}:{path}", (jar, source))
        except zipfile.BadZipFile:
            continue
    for integration in document.get("integrations", []):
        if integration["gateway"] not in custom_ids:
            continue
        jar = find_mod_jar(mods_dir, integration["jar_pattern"])
        namespace = integration["namespace"]
        with zipfile.ZipFile(jar) as archive:
            if integration.get("include_all_visible"):
                paths = [
                    name.removeprefix(f"data/{namespace}/advancements/")[:-5]
                    for name in archive.namelist()
                    if name.startswith(f"data/{namespace}/advancements/") and name.endswith(".json")
                ]
                paths = [path for path in paths if (source := read_json(archive, f"data/{namespace}/advancements/{path}.json")) and "display" in source]
            else:
                paths = list(dict.fromkeys(integration["paths"]))
            for path in paths:
                advancement_id = f"{namespace}:{path}"
                source = read_json(archive, f"data/{namespace}/advancements/{path}.json")
                if source is None:
                    raise ValueError(f"Missing native advancement {advancement_id} in {jar.name}")
                if "display" not in source:
                    raise ValueError(f"Native integration {advancement_id} has no visible display")
                all_entries.setdefault(advancement_id, (jar, source))
                selected[advancement_id] = (jar, source, integration)

    selected_ids = set(selected)
    for advancement_id, (jar, source, integration) in selected.items():
        if advancement_id in owned_ids:
            raise ValueError(f"Duplicate generated advancement ID {advancement_id}")
        namespace, path = advancement_id.split(":", 1)
        override = json.loads(json.dumps(source))
        original_parent = source.get("parent")
        explicit_parent = integration.get("parent_overrides", {}).get(path)
        if explicit_parent:
            override["parent"] = explicit_parent
        elif integration.get("branch_root") and advancement_id != integration.get("branch_root_id"):
            override["parent"] = integration["branch_root"]
        elif original_parent not in selected_ids:
            override["parent"] = integration["gateway"]
        if advancement_id in NATIVE_ICON_OVERRIDES:
            override.setdefault("display", {})["icon"] = {"item": NATIVE_ICON_OVERRIDES[advancement_id]}
        if advancement_id in NATIVE_BACKGROUND_OVERRIDES:
            override.setdefault("display", {})["background"] = NATIVE_BACKGROUND_OVERRIDES[advancement_id]
        source_semantics = {key: value for key, value in source.items() if key not in {"parent", "display"}}
        override_semantics = {key: value for key, value in override.items() if key not in {"parent", "display"}}
        if source_semantics != override_semantics:
            raise ValueError(f"Native criterion drift in {advancement_id}")
        relative = Path("data") / namespace / "advancements" / f"{path}.json"
        write_json(resources / relative, override)
        generated.append({
            "advancement_id": advancement_id, "source_jar": jar.name, "source_jar_path": str(jar),
            "source_parent": original_parent, "output_parent": override.get("parent"),
            "guide_tab": integration["gateway"].split(":", 1)[1].split("/", 1)[0],
            "proof": "NATIVE_CRITERIA_UNCHANGED", "relative_path": relative.as_posix(),
        })
        owned_ids.add(advancement_id)

    return generated


def campaign_guide_entries(document, guide_assets):
    for node in document["entries"]:
        namespace, path = node["id"].split(":", 1)
        for language, text in node["text"].items():
            entry = {
                "name": text["name"],
                "category": node["category"],
                "icon": node["item"],
                "pages": [
                    {"type": "patchouli:text", "title": text["name"], "text": text["hint"]},
                    {"type": "patchouli:spotlight", "item": node["item"], "text": text["reference"]},
                ],
                "read_by_default": True,
            }
            write_json(guide_assets / "patchouli_books" / "just_enough_guide" / language / "entries" / "native" / namespace / f"{path}.json", entry)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--legacy-jar", type=Path, required=True)
    parser.add_argument("--mods-dir", type=Path, required=True)
    parser.add_argument("--resources", type=Path, required=True)
    parser.add_argument("--native-integrations", type=Path, required=True)
    args = parser.parse_args()

    document = json.loads(args.manifest.read_text(encoding="utf-8"))
    native_document = json.loads(args.native_integrations.read_text(encoding="utf-8"))
    nodes = document["nodes"]
    resources = args.resources.resolve()
    names = registry_names(args.mods_dir)
    known_items = {
        item_id
        for registry in (names["item"], names["block"])
        for item_ids in registry.values()
        for item_id in item_ids
    }
    data_jem_guide = resources / "data" / "jem_guide" / "advancements"
    data_jem = resources / "data" / "jem" / "advancements"
    guide_assets = resources / "assets" / GUIDE_MOD_ID
    native_index = args.manifest.parent / "native-generated-files.json"
    remove_generated_native_files(native_index, resources)
    legacy_guide_archive = resources / "assets" / f"{GUIDE_MOD_ID}.zip"
    if legacy_guide_archive.exists():
        legacy_guide_archive.unlink()
    safe_replace_dir(data_jem_guide, resources)
    safe_replace_dir(data_jem, resources)
    safe_replace_dir(guide_assets, resources)
    safe_remove_dir(resources / "assets" / LEGACY_GUIDE_MOD_ID, resources)
    safe_remove_dir(resources / "data" / LEGACY_GUIDE_MOD_ID, resources)

    native_nodes = write_native_integrations(native_document, args.mods_dir, resources, {node["advancement_id"] for node in nodes})
    with zipfile.ZipFile(args.legacy_jar) as archive:
        rules_document = read_json(archive, "assets/jem_semantic_runtime/semantic_rules.json") or {"rules": []}
        rules = {rule["advancement"]: rule for rule in rules_document.get("rules", [])}
        en: dict[str, str] = {
            f"{GUIDE_MOD_ID}.patchouli.name": "Just Enough Mods Field Guide",
            f"{GUIDE_MOD_ID}.patchouli.subtitle": "A path to complete progression",
            f"{GUIDE_MOD_ID}.patchouli.landing_text": "Select a JEM advancement to open its exact field-guide entry.",
            f"{GUIDE_MOD_ID}.patchouli.open_failed": "This guide entry could not be opened. Advancement progression is unaffected.",
        }
        ru: dict[str, str] = {
            f"{GUIDE_MOD_ID}.patchouli.name": "Полевой справочник Just Enough Mods",
            f"{GUIDE_MOD_ID}.patchouli.subtitle": "Путь к полному прохождению",
            f"{GUIDE_MOD_ID}.patchouli.landing_text": "Выберите достижение JEM, чтобы открыть точную инструкцию.",
            f"{GUIDE_MOD_ID}.patchouli.open_failed": "Не удалось открыть запись справочника. Достижения продолжают работать.",
        }
        index_entries = []
        criteria_catalog = []
        for sort_index, node in enumerate(nodes):
            namespace, path = node["advancement_id"].split(":", 1)
            legacy = read_json(archive, f"data/{namespace}/advancements/{path}.json")
            criteria, requirements, provider = criterion_for(node, legacy, rules.get(node["advancement_id"]), names)
            advancement = {
                "display": display_for(node, legacy),
                "criteria": criteria,
                "requirements": requirements,
            }
            if node["parent"]:
                advancement["parent"] = node["parent"]
            output = resources / "data" / namespace / "advancements" / f"{path}.json"
            write_json(output, advancement)
            base = key_base(node)
            guide = guide_key(node)
            en[f"{base}.title"] = node["title_en"]
            en[f"{base}.description"] = node["short_en"]
            en[f"{guide}.guide"] = node["exact_en"]
            en[f"{guide}.subject"] = node["mechanics"]
            ru[f"{base}.title"] = node["title_ru"]
            ru[f"{base}.description"] = node["short_ru"]
            ru[f"{guide}.guide"] = node["exact_ru"]
            ru[f"{guide}.subject"] = node["mechanics"]
            item_refs = [value for value in node["resource_ids"] if not value.startswith(("jem_guide:", "jem:"))]
            icon = advancement["display"]["icon"].get("item", TAB_ICONS[node["tab"]])
            pages = [{"type": "patchouli:text", "title": f"{base}.title", "text": f"{guide}.guide"}]
            spotlight = icon
            if spotlight:
                pages.append({"type": "patchouli:spotlight", "item": spotlight, "text": ""})
            entry = {
                "name": f"{base}.title",
                "category": f"{GUIDE_MOD_ID}:{node['tab']}",
                "icon": icon,
                "pages": pages,
                "read_by_default": True,
                "sortnum": sort_index,
            }
            entry_path = guide_assets / "patchouli_books" / "just_enough_guide" / "en_us" / "entries" / node["tab"] / f"{path.split('/', 1)[-1]}.json"
            write_json(entry_path, entry)
            index_entries.append({"id": node["advancement_id"], "guide": f"{guide}.guide", "subject": f"{guide}.subject", "itemRefs": item_refs})
            for criterion in criteria.values():
                trigger = criterion["trigger"]
                if trigger.startswith(f"{MOD_ID}:node/"):
                    criteria_catalog.append(trigger)
            node["criterion_provider"] = node.get("provider") or EXACT_JAVA_CALLERS.get(node["advancement_id"], provider)
            if node["criterion_provider"].startswith(f"{MOD_ID}:node/") and node["advancement_id"] not in EXACT_JAVA_CALLERS:
                raise ValueError(f"Custom node has no authoritative caller: {node['advancement_id']}")

        native_guide_entries(native_nodes, resources, guide_assets, en, ru, index_entries)
        campaign_guide_entries(json.loads((args.manifest.parent / "campaign-guide.json").read_text(encoding="utf-8")), guide_assets)

        for tab, icon in TAB_ICONS.items():
            write_json(guide_assets / "patchouli_books" / "just_enough_guide" / "en_us" / "categories" / f"{tab}.json", {
                "name": tab.replace("_", " ").title(),
                "description": f"JEM {tab.replace('_', ' ').title()} advancements",
                "icon": icon,
                "sortnum": list(TAB_ICONS).index(tab),
            })
        write_json(guide_assets / "advancement_guides" / "index.json", {"entries": index_entries})
        text_overrides = json.loads((args.manifest.parent / "guide-text-overrides.json").read_text(encoding="utf-8"))
        en.update(text_overrides["en_us"])
        ru.update(text_overrides["ru_ru"])
        write_json(guide_assets / "lang" / "en_us.json", en)
        write_json(guide_assets / "lang" / "ru_ru.json", ru)
        write_json(resources / "data" / MOD_ID / "node_criteria.json", {"criteria": sorted(criteria_catalog)})
        allowed_namespaces = ["minecraft", "jemcompat", "jem_world_boss_tiers"]
        allowed_advancements = sorted(
                node["advancement_id"]
                for node in native_nodes
                if node["proof"] == "NATIVE_CRITERIA_UNCHANGED"
        )
        allowed_advancement_set = set(allowed_advancements)
        compatibility_placeholders = sorted(
            advancement_id
            for advancement_id in code_referenced_advancements(args.mods_dir)
            if advancement_id.split(":", 1)[0] not in allowed_namespaces
            and advancement_id not in allowed_advancement_set
        )
        write_json(resources / "data" / MOD_ID / "advancement_allowlist.json", {
            "namespaces": allowed_namespaces,
            "advancements": allowed_advancements,
            "compatibility_placeholders": compatibility_placeholders,
        })
        write_json(args.manifest.parent / "implementation_status.json", {
            "nodes": [{"advancement_id": node["advancement_id"], "provider": node["criterion_provider"]} for node in nodes]
        })
        write_matrix(args.manifest.resolve().parents[3] / "IMPLEMENTATION_MATRIX.md", nodes)
        write_json(resources / "data" / GUIDE_MOD_ID / "patchouli_books" / "just_enough_guide" / "book.json", {
            "name": f"{GUIDE_MOD_ID}.patchouli.name",
            "landing_text": f"{GUIDE_MOD_ID}.patchouli.landing_text",
            "subtitle": f"{GUIDE_MOD_ID}.patchouli.subtitle",
            "icon": "minecraft:book",
            "version": "4",
            "use_resource_pack": True,
            "dont_generate_book": True,
            "custom_book_item": "minecraft:book",
            "show_progress": False,
            "show_toasts": False,
            "i18n": True,
            "pause_game": False,
        })
    write_json(native_index, {"files": [node["relative_path"] for node in native_nodes], "nodes": native_nodes})
    print(json.dumps({"nodes": len(nodes), "native_nodes": len(native_nodes), "node_criteria": len(criteria_catalog), "upstream_or_vanilla": len(nodes) - len(criteria_catalog)}))


if __name__ == "__main__":
    main()

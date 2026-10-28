"""Compile the authored book to native FTB 2001.4.22 SNBT and indexed event rules.

book.json owns text, bindings, identities and coordinates. Compilation never assigns
new coordinates to existing cards and never imports over manual edits.
"""
from __future__ import annotations

import csv
import argparse
import hashlib
import json
import math
from pathlib import Path, PurePosixPath, PureWindowsPath
import re

ROOT = Path(__file__).resolve().parents[3]
SOURCE = Path(__file__).parent / "questbook"
RESOURCES = Path(__file__).resolve().parents[1] / "src/main/resources"
BOOK = ROOT / "Client/ManagedPack/config/ftbquests/quests"
OUTPUT_ROOT = ROOT
MANIFEST_ROOT = SOURCE
BASE_RESOURCES = RESOURCES
LEGACY = BASE_RESOURCES / "assets/jem_advancements/questbook/migration/legacy"


def candidate_output(output: Path):
    """Generate reviewable artifacts without activating unfinished gameplay contracts."""
    global OUTPUT_ROOT, RESOURCES, BOOK, MANIFEST_ROOT
    OUTPUT_ROOT = output.resolve()
    RESOURCES = OUTPUT_ROOT / "Mods/JEMAchievementGuide/src/main/resources"
    BOOK = OUTPUT_ROOT / "Client/ManagedPack/config/ftbquests/quests"
    MANIFEST_ROOT = OUTPUT_ROOT / "Mods/JEMAchievementGuide/tools/questbook"


def advancement_sources():
    return {path.relative_to(SOURCE / "advancements").as_posix(): path
            for path in sorted((SOURCE / "advancements").rglob("*.json"))}


def activation_contract():
    hashes = {name: hashlib.sha256(path.read_bytes()).hexdigest() for name, path in advancement_sources().items()}
    return {"book_sha256": hashlib.sha256((SOURCE / "book.json").read_bytes()).hexdigest(),
            "advancement_resources_sha256": hashlib.sha256(json.dumps(hashes, sort_keys=True,
                separators=(",", ":")).encode("utf-8")).hexdigest()}


def require_activation_ready():
    receipt = ROOT / "Docs/quests/visual-v4/implementation.json"
    if not receipt.is_file():
        raise ValueError("V4 activation requires the reviewed producer and migration receipt: " + str(receipt))
    readiness = json.loads(receipt.read_text(encoding="utf-8"))
    for key, expected in activation_contract().items():
        if readiness.get(key) != expected:
            raise ValueError("Stale V4 activation receipt: " + key)
    native_path = ROOT / "Docs/quests/visual-v4/native-criteria.json"
    native_rows = json.loads(native_path.read_text(encoding="utf-8")).get("resources", []) if native_path.is_file() else []
    native_reviewed = {row["advancement"]: row for row in native_rows}
    reviewed = {row["advancement"]: row for row in readiness.get("producers", [])}
    missing, unknown_native = [], []
    for name, path in advancement_sources().items():
        definition = json.loads(path.read_text(encoding="utf-8"))
        advancement = "jem_advancements:" + name.removesuffix(".json")
        actions = {criterion["conditions"]["action"] for criterion in definition["criteria"].values()
                   if criterion["trigger"] == "jem_advancements:quest_action"}
        native_triggers = {criterion["trigger"] for criterion in definition["criteria"].values()
                           if criterion["trigger"] != "jem_advancements:quest_action"}
        if native_triggers:
            native = native_reviewed.get(advancement, {})
            if (native.get("native_criterion_reviewed") is not True
                    or native.get("sha256") != hashlib.sha256(path.read_bytes()).hexdigest()
                    or not native_triggers <= set(native.get("allowed_triggers", []))):
                unknown_native.append(advancement)
        if actions:
            record = reviewed.get(advancement, {})
            if record.get("producer_code_reviewed") is not True or not actions <= set(record.get("reviewed_actions", [])):
                missing.append(advancement)
    problems = []
    if missing:
        problems.append("Unimplemented or unreviewed advancement producers (" + str(len(missing))
                        + "): " + ", ".join(missing))
    if unknown_native:
        problems.append("Unreviewed native criteria resources: " + ", ".join(unknown_native))
    if (readiness.get("history_migration_policy") != "not_required_unused_quests"
            and readiness.get("migration_code_ready") is not True):
        problems.append("V4 migration has not been reviewed as code-ready")
    if problems:
        raise ValueError("V4 activation blocked. " + " | ".join(problems))


def compile_advancements():
    """Own only authored quest blueprints and output-manifest paths, never upstream resources."""
    sources = advancement_sources()
    retired_path = SOURCE / "retired-advancements.json"
    retired = set(json.loads(retired_path.read_text(encoding="utf-8"))["resources"]) if retired_path.is_file() else set()
    if retired & sources.keys():
        raise ValueError("Retired quest advancement reintroduced without a user amendment: " + ", ".join(sorted(retired & sources.keys())))
    owner = RESOURCES / "data/jem_advancements/advancements"
    manifest = MANIFEST_ROOT / "compiled-advancement-files.json"
    if manifest.is_file():
        managed = json.loads(manifest.read_text(encoding="utf-8"))
        if not isinstance(managed, list) or any(not isinstance(name, str) for name in managed):
            raise ValueError("Invalid generated advancement manifest: " + str(manifest))
    else:
        # First migration knows exact current source names and explicitly retired source history.
        # It never discovers ownership by scanning a runtime/resource tree.
        managed = sorted(set(sources) | retired)
    paths = {}
    for name in set(managed) | set(sources) | retired:
        if not isinstance(name, str) or not re.fullmatch(r"quest_v[23]/(?:[A-Za-z0-9_][A-Za-z0-9_.-]*/)*[A-Za-z0-9_][A-Za-z0-9_.-]*\.json", name):
            raise ValueError("Unexpected owned quest advancement path: " + str(name))
        paths[name] = safe_child(owner, safe_relative(name))
    for name, source in sources.items():
        target = paths[name]
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(source.read_bytes())
    for name in set(managed) - set(sources):
        target = paths[name]
        if target.is_file():
            target.unlink()
    dump(manifest, sorted(sources))


def identity(kind: str, key: str) -> str:
    return "2" + hashlib.sha256(f"jem.questbook/{kind}/{key}".encode()).hexdigest()[:15].upper()


def dump(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    encoded = json.dumps(data, ensure_ascii=False, indent=2)
    if path.suffix == ".snbt":
        encoded = re.sub(r'"table_id": "([A-F0-9]+)"', lambda match: '"table_id": ' + str(int(match[1], 16)) + "L", encoded)
    path.write_text(encoded + "\n", encoding="utf-8")


def simple(match):
    return len(match["ids"]) == 1 and set(match) == {"ids"}


def task_definitions(card):
    tasks, rules = [], []
    advancement_count = sum(b["kind"] == "advancement" for b in card["bindings"])
    kill_count = sum(b["kind"] == "kill" for b in card["bindings"])
    for number, binding in enumerate(card["bindings"]):
        targets = binding["targets"]
        fixed = binding["distinct"] == len(targets) and binding["distinct"] > 0
        if binding["kind"] == "inventory" and (fixed or len(targets) == 1) and all(simple(t) for t in targets):
            for group, match in enumerate(targets):
                tasks.append({"id": identity("item", f"{card['id']}/{number}/{group}"), "type": "item",
                              "item": match["ids"][0], "count": 1 if fixed else binding["count"], "consume_items": False})
        elif binding["kind"] == "dimension":
            for group, match in enumerate(targets):
                tasks.append({"id": identity("dimension", f"{card['id']}/{number}/{group}"),
                              "type": "dimension", "dimension": match["ids"][0], "title": card["condition"]})
        elif binding["kind"] == "advancement":
            tasks.append({"id": identity("advancement", card["id"] if advancement_count == 1 else f"{card['id']}/{number}/{binding['argument']}"), "type": "advancement",
                          "advancement": binding["argument"]})
        elif binding["kind"] == "kill" and len(targets) == 1 and simple(targets[0]) and not binding["distinct"]:
            tasks.append({"id": identity("kill", card["id"] if kill_count == 1 else f"{card['id']}/{number}"), "type": "kill", "entity": targets[0]["ids"][0], "value": binding["count"]})
        else:
            task_id = card.get("preserved_task_id", identity("event", f"{card['id']}/{number}")) if len(card["bindings"]) == 1 else identity("event", f"{card['id']}/{number}")
            task = {"id": task_id, "type": "custom", "title": binding.get("title", card["condition"]), "icon": card["icon"],
                    "count": max(binding["distinct"], binding["count"])}
            tasks.append(task)
            rules.append(dict(binding, id=task_id, quest=card["ftb_id"]))
    return tasks, rules


def item_reward(card, index, reward, choice=False):
    return {"id": identity("choice_option" if choice else "reward", f"{card['id']}/{index}"),
            "type": "jem_advancements:quest_item", "item": reward["id"], "count": reward["count"],
            "claim_key": f"{card['id']}/choice" if choice else f"{card['id']}/item/{index}",
            "claim_reward_id": identity("choice", card["id"]) if choice else identity("reward", f"{card['id']}/{index}"),
            "team_reward": False}


class Localization:
    def __init__(self):
        self.values = {"en_us": {}, "ru_ru": {}}

    def text(self, key, english, russian):
        key = "jem.atlas." + key
        self.values["en_us"][key] = english
        self.values["ru_ru"][key] = russian
        return json.dumps({"translate": key}, ensure_ascii=False, separators=(",", ":"))

    def plain_key(self, key, english, russian):
        self.text(key, english, russian)
        return "jem.atlas." + key

    def write(self):
        shared = {
            "en_us": {"jem.questbook.unavailable.hover": "Unavailable here - reference only", "jem.questbook.unavailable.reason": "Required content or feature is unavailable in this world", "jem.questbook.unavailable.summary": "%s: %s"},
            "ru_ru": {"jem.questbook.unavailable.hover": "Недоступно здесь - только справка", "jem.questbook.unavailable.reason": "Нужное содержимое или возможность недоступны в этом мире", "jem.questbook.unavailable.summary": "%s: %s"},
        }
        for language, values in self.values.items():
            path = RESOURCES / "assets/jem_advancements/lang" / (language + ".json")
            original = BASE_RESOURCES / "assets/jem_advancements/lang" / (language + ".json")
            old = json.loads(original.read_text("utf-8")) if original.exists() else {}
            if path.exists():
                old.update(json.loads(path.read_text("utf-8")))
            # Moved chapters and existing saves may still refer to old locale keys.
            dump(path, dict(old, **shared[language], **values))


def safe_relative(value: str) -> Path:
    normalized = value.replace("\\", "/")
    parts = PurePosixPath(normalized)
    if (not normalized or parts.is_absolute() or PureWindowsPath(normalized).drive
            or ".." in parts.parts or normalized.startswith("//")):
        raise ValueError(f"Unsafe generated path: {value}")
    return Path(*parts.parts)


def safe_child(root: Path, relative: Path) -> Path:
    target = root / relative
    if not target.resolve().is_relative_to(root.resolve()):
        raise ValueError(f"Generated path escapes its owner: {relative}")
    return target


def resource_stem(card_id: str) -> str:
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", card_id):
        raise ValueError(f"Unsafe authored card identity: {card_id}")
    return card_id.lower().replace(".", "_")


def page_chunks(text: str, maximum: int):
    """Keep native text pages modest; never truncate authored mechanic prose."""
    # These are layout budgets, not a claim that native game rendering was observed.
    paragraphs = [part.strip() for part in re.split(r"\n\s*\n|\$\(br2\)", text) if part.strip()]
    output, current = [], ""
    for paragraph in paragraphs:
        sentences = re.split(r"(?<=[.!?。])\s+", paragraph)
        for sentence in sentences:
            for word in sentence.split():
                if current and len(current) + len(word) + 1 > maximum:
                    output.append(current)
                    current = word
                else:
                    current = (current + " " + word).strip()
        if current:
            output.append(current)
            current = ""
    return output or [text.strip()]


def guide_aliases(card, chapter):
    # Both languages and exact item identifiers support the existing handbook search.
    aliases = [card["id"], card["icon"]]
    for language in ("en_us", "ru_ru"):
        aliases.extend([card["localization"][language]["title"],
                        chapter["localization"][language]["title"]])
    item_kinds = {"inventory", "craft", "smelt", "eat", "drink", "fished", "equip", "use_item", "place", "use_block", "break"}
    for binding in card.get("bindings", []):
        if binding["kind"] in item_kinds:
            aliases.extend(item for match in binding["targets"] for item in match.get("ids", []))
        for field in ("held", "mainHand", "secondHand"):
            aliases.extend(item for match in binding.get(field, []) for item in match.get("ids", []))
    return list(dict.fromkeys(value for value in aliases if value))


def compile_handbook(book, *, resources=None):
    """Use the existing native book/items category; add no quest or book navigation UI."""
    guide_root = (resources or RESOURCES) / "assets/jem_advancements/patchouli_books/just_enough_guide"
    manifest = MANIFEST_ROOT / "compiled-guide-files.json"
    generated = []
    for chapter in book["chapters"]:
        for index, card in enumerate(chapter["cards"]):
            if not card.get("conditions") or card.get("status", "active") != "active":
                continue
            for language in ("en_us", "ru_ru"):
                localized = card["localization"][language]
                maximum = 300 if language == "en_us" else 260
                title = localized["title"]
                introduction = localized["condition"] + "$(br2)" + localized["purpose"]
                pages = [{"type": "patchouli:text", "title": title, "text": chunk}
                         for chunk in page_chunks(introduction, maximum)]
                hint_title = "Getting started" if language == "en_us" else "С чего начать"
                pages.extend({"type": "patchouli:text", "title": hint_title, "text": chunk}
                             for chunk in page_chunks(localized["tip"], maximum))
                text_key = "text_en" if language == "en_us" else "text_ru"
                title_key = "title_en" if language == "en_us" else "title_ru"
                # Never discard a section merely because several sections share a heading.
                seen = set()
                for section in card.get("mechanic_sections", []):
                    prose = section.get(text_key, "").strip()
                    heading = section.get(title_key) or hint_title
                    identity = (heading, prose)
                    if not prose or identity in seen:
                        continue
                    seen.add(identity)
                    pages.extend({"type": "patchouli:text", "title": heading, "text": chunk}
                                 for chunk in page_chunks(prose, maximum))
                path = Path(language) / "entries/handbook/items/quest_v3" / (resource_stem(card["id"]) + ".json")
                definition = {"name": title, "icon": card["icon"],
                              "category": "jem_advancements:handbook/items",
                              "sortnum": chapter["order"] * 1000 + index,
                              "jem_aliases": guide_aliases(card, chapter),
                              "jem_summary": localized["condition"], "pages": pages}
                dump(safe_child(guide_root, path), definition)
                generated.append(path.as_posix())
    expected = set(generated)
    if manifest.exists():
        for old in json.loads(manifest.read_text(encoding="utf-8")):
            relative = safe_relative(old)
            normalized = relative.as_posix()
            if not re.fullmatch(r"(?:en_us|ru_ru)/entries/handbook/items/quest_v3/[a-z0-9_-]+\.json", normalized):
                raise ValueError(f"Unexpected generated handbook path: {old}")
            target = safe_child(guide_root, relative)
            if normalized not in expected and target.is_file():
                target.unlink()
    dump(manifest, sorted(generated))


def effective_node_size(card):
    return float(card.get("native_options", {}).get("size",
        1.25 if card["reward"]["tier"] == "capstone" else 1.0))


def compile_layout(book):
    """One deterministic geometry owner shared by native output and camera metadata."""
    layout = {}
    for chapter in book["chapters"]:
        active = [card for card in chapter["cards"]
                  if card.get("conditions") and card.get("status", "active") == "active"]
        regular = sorted({effective_node_size(card) for card in active
                          if card["reward"]["tier"] != "capstone"})
        milestones = sorted({effective_node_size(card) for card in active
                             if card["reward"]["tier"] == "capstone"})
        nodes = {}
        for card in active:
            shape = card.get("native_options", {}).get("shape") or (
                "diamond" if card["reward"]["tier"] == "capstone"
                else "hexagon" if card["collection"] else "circle")
            nodes[card["ftb_id"]] = {"source_id": card["id"],
                "x": card["position"][0], "y": card["position"][1],
                "size": effective_node_size(card), "shape": shape}
        # Explicit visual links are decoration only; native quest requirements are unchanged.
        palette = ["#79bca6", "#deb27c", "#b39bd8", "#7faac8", "#cf929e", "#b1bb7b", "#83bfc2", "#c7a485", "#a5b6db"]
        groups = chapter.get("layout_groups", [])
        group_of = {qid: group["id"] for group in groups for qid in group["quest_ids"]}
        routes = [group["id"] for group in groups] or list(dict.fromkeys(card["design"].get("visual_route", "route") for card in active))
        colors = {route: palette[i % len(palette)] for i, route in enumerate(routes)}
        local = {card["id"]: card for card in active}
        candidates = chapter.get("visual_edges")
        if candidates is None:
            raise ValueError(f"Missing explicit authored visual_edges: {chapter['id']}")
        connections, omitted, seen = [], [], set()
        def point_segment_distance(point, start, end):
            dx, dy = end[0] - start[0], end[1] - start[1]
            if not dx and not dy:
                return math.dist(point, start)
            t = max(0.0, min(1.0, ((point[0]-start[0])*dx+(point[1]-start[1])*dy)/(dx*dx+dy*dy)))
            return math.dist(point, [start[0]+t*dx, start[1]+t*dy])
        for edge in candidates:
            if isinstance(edge, (list, tuple)):
                edge = {"from": edge[0], "to": edge[1], "kind": "visual_route"}
            previous, following = edge["from"], edge["to"]
            if previous not in local or following not in local:
                raise ValueError(f"Unknown visual endpoint: {chapter['id']} {edge}")
            key = tuple(sorted([previous, following]))
            if key in seen:
                continue
            seen.add(key)
            parent, card = local[previous], local[following]
            points = [parent["position"]] + edge.get("via", []) + [card["position"]]
            blocked = []
            for other in active:
                if other["id"] in (previous, following):
                    continue
                radius = effective_node_size(other) / 2 + 0.12
                if any(point_segment_distance(other["position"], aa, bb) < radius
                       for aa, bb in zip(points, points[1:])):
                    blocked.append(other["id"])
            if blocked:
                omitted.append({"from_source": previous, "to_source": following,
                                "reason": "Would pass through another native node", "nodes": blocked})
                continue
            route = group_of.get(following, card["design"].get("visual_route", "route"))
            connections.append({"from": parent["ftb_id"], "to": card["ftb_id"],
                "from_source": previous, "to_source": following,
                "start": points[0], "end": points[-1], "points": points,
                "kind": edge.get("kind", "visual_route"),
                "color": colors.get(route, "#8798a7"),
                "opacity": 0.45, "width_gui": 1.0, "dash_gui": [4, 5],
                "semantics": "visual_relationship_only"})
        layout[chapter["ftb_id"]] = {
            "source_id": chapter["id"], "bounds": chapter["entry_bounds"],
            "background": chapter["background_slot"], "hero": chapter["hero"],
            # Reserved artwork space is metadata only: no detached caption renderer.
            "reserved_title_zone": chapter.get("title_zone", chapter.get("title_caption")),
            "regular_node_size": regular[0] if regular else 1.0,
            "milestone_node_size": milestones[-1] if milestones else (regular[-1] if regular else 1.0),
            "ordinary_entry": "overview", "explicit_search": "native_target",
            "min_zoom": "fit_zoom", "pan_margin_gui_units": 8,
            "nodes": nodes, "groups": groups, "connections": connections, "omitted_connections": omitted,
            "entry_ids": chapter.get("design", {}).get("visual_entry_ids", []),
            "finale_ids": chapter.get("design", {}).get("visual_finale_ids", [])}
    dump(RESOURCES / "data/jem_advancements/questbook/layout.json", layout)
    dump(RESOURCES / "assets/jem_advancements/questbook/layout.json", layout)


def compile_book(*, emit_reward_selector=True):
    if OUTPUT_ROOT.resolve() == ROOT.resolve():
        require_activation_ready()
    book = json.loads((SOURCE / "book.json").read_text(encoding="utf-8"))
    compile_advancements()
    all_rules, mapping, generated = [], [], []
    language = Localization()
    rows = {card["id"]: card for chapter in book["chapters"] for card in chapter["cards"]}
    if len(rows) != sum(len(chapter["cards"]) for chapter in book["chapters"]):
        raise ValueError("Duplicate source card identity")
    if len({card["ftb_id"] for card in rows.values()}) != len(rows):
        raise ValueError("Duplicate native quest identity")
    stems = [resource_stem(card["id"]) for card in rows.values()]
    if len(set(stems)) != len(stems):
        raise ValueError("Colliding generated guide/reward resource stems")
    for card in rows.values():
        if card["conditions"] and card.get("status", "active") == "active" and not card["bindings"]:
            raise ValueError(f"Active card has no detection contract: {card['id']}")
        for binding in card["bindings"]:
            targets = binding["targets"]
            if binding["count"] < 1 or binding["distinct"] < 0:
                raise ValueError(f"Invalid objective quantity: {card['id']}")
            if binding["distinct"] and not binding.get("distinctById") and binding["distinct"] > len(targets):
                raise ValueError(f"Not enough named groups: {card['id']}")
            if binding.get("distinctById") and all(target["ids"] for target in targets) and len({item for target in targets for item in target["ids"]}) < binding["distinct"]:
                raise ValueError(f"Not enough distinct identities: {card['id']}")
    groups = [{"id": identity("group", group["id"]),
               "title": language.text("group." + group["id"], group["en_us"], group["ru_ru"]),
               "icon": group["icon"]} for group in book["navigation_groups"]]
    dump(BOOK / "chapter_groups.snbt", {"chapter_groups": groups})
    free_order = language.text("free_order", "Follow your own route. Nearby rewards are never prerequisites.", "Выбирай свой путь. Награда соседа никогда не становится условием.")
    choice_title = language.text("choice", "Choose one", "Один предмет на выбор")
    for chapter in book["chapters"]:
        quests, references = [], []
        images = list(chapter.get("images", []))
        chapter_text = chapter["localization"]
        chapter_title = language.text("chapter." + chapter["id"], chapter_text["en_us"]["title"], chapter_text["ru_ru"]["title"])
        theme = chapter["theme"]
        background = dict(chapter["background_slot"])
        required_geometry = ("x", "y", "width", "height", "image")
        if not set(required_geometry) <= background.keys():
            raise ValueError(f"Incomplete background geometry: {chapter['id']}")
        if background["width"] <= 0 or background["height"] <= 0:
            raise ValueError(f"Invalid background size: {chapter['id']}")
        images.insert(0, {key: background[key] for key in required_geometry} | {"order": -100})
        hero = {key: chapter["hero"][key] for key in ("x", "y", "width", "height")}
        # Authored dimensions already preserve each source emblem's aspect ratio.
        # Respect them; do not replace them with historical global constants.
        hero_image = chapter["hero"].get("image") or (
            "minecraft:textures/item/ender_eye.png" if theme == "bosses"
            else f"jem_advancements:textures/questbook/atlas/identity-{theme}.jpg")
        images.append({**hero, "image": hero_image, "order": -20 if theme == "bosses" else -50})
        for card in chapter["cards"]:
            localized = card["localization"]
            text = {key: language.text("card." + card["id"] + "." + key,
                                      localized["en_us"][key], localized["ru_ru"][key])
                    for key in ("title", "condition", "purpose", "tip", "reward")}
            status = card.get("status", "active") if card["conditions"] else "reference"
            if status != "active":
                mapping.append([card["id"], chapter["id"], "not displayed", card["condition"], "Нет", status, card.get("implementation_difference", card.get("adaptation", ""))])
                continue
            tasks, rules = task_definitions(card)
            for task in tasks:
                if "title" in task:
                    task["title"] = text["condition"]
            all_rules.extend(rules)
            description = [text["condition"], "", text["purpose"], "", text["tip"], "", text["reward"]]
            if card.get("mechanic_sections"):
                guide_hint = language.text("card." + card["id"] + ".guide_hint",
                    "More details: unified guide → Items → “" + localized["en_us"]["title"] + "”.",
                    "Подробнее: единый справочник → Предметы → «" + localized["ru_ru"]["title"] + "».")
                description.extend(["", guide_hint])
            if card["after"]:
                description.extend(["", free_order])
            reward = card["reward"]
            rewards = [item_reward(card, i, item) for i, item in enumerate(reward["items"])]
            if reward["xp"]:
                rewards.append({"id": identity("xp", card["id"]), "type": "jem_advancements:quest_xp", "xp": reward["xp"], "team_reward": False})
            if reward["choice"]:
                table_id = identity("choice_table", card["id"])
                table = {"id": table_id, "title": choice_title, "use_title": True, "loot_size": 1,
                         "rewards": [item_reward(card, i, item, True) for i, item in enumerate(reward["choice"])]}
                table_path = BOOK / "reward_tables" / (resource_stem(card["id"]) + ".snbt")
                dump(table_path, table); generated.append(str(table_path.relative_to(BOOK)))
                rewards.append({"id": identity("choice", card["id"]), "type": "choice", "table_id": table_id, "team_reward": False})
            quest = {"id": card["ftb_id"], "title": text["title"], "subtitle": text["condition"],
                     "description": description, "icon": card["icon"], "x": card["position"][0], "y": card["position"][1],
                     "shape": "diamond" if reward["tier"] == "capstone" else "hexagon" if card["collection"] else "circle",
                     "size": effective_node_size(card),
                     "tasks": tasks, "rewards": rewards, "jem_source_id": card["id"],
                     "jem_required_mods": card.get("required_mods", []),
                     "jem_economy": any(rule["kind"] == "operation" for rule in rules),
                     "jem_dedicated_event": bool(card.get("dedicated_event")) or any(rule.get("dedicated") for rule in rules)}
            if card.get("native_availability"):
                quest["jem_native_condition"] = card["native_availability"]
            quest.update(card.get("native_options", {}))
            quests.append(quest)
            reward_summary = "; ".join(f"{item['count']}x {item['id']}" for item in reward["items"])
            if reward["choice"]: reward_summary += "; один на выбор: " + ", ".join(f"{item['count']}x {item['id']}" for item in reward["choice"])
            mapping.append([card["id"], chapter["id"], card["ftb_id"], card["condition"], reward_summary + f"; XP {reward['xp']}", "adapted" if card.get("adaptation") else "active", card.get("implementation_difference", card.get("adaptation", ""))])
        definition = {"id": chapter["ftb_id"], "filename": chapter["filename"], "title": chapter_title,
                      "subtitle": [],
                      "icon": chapter["icon"], "order_index": chapter["order"],
                      "default_quest_shape": "circle", "default_hide_dependency_lines": True,
                      "default_quest_size": 1.0, "autofocus_id": chapter["autofocus_id"],
                      "tags": ["jem_visual_v4", "jem_authored_v3"],
                      "progression_mode": "flexible", "quests": quests, "images": images,
                      # NBT lists require a single tag type. Authored bounds may
                      # contain both whole and fractional coordinates.
                      "jem_rescue_entry_bounds": [float(value) for value in chapter["entry_bounds"]]}
        target = BOOK / "chapters" / (chapter["filename"] + ".snbt")
        dump(target, definition); generated.append(str(target.relative_to(BOOK)))
    dump(RESOURCES / "data/jem_advancements/questbook/rules.json", all_rules)
    compile_layout(book)
    # Current owner retired the old custom chapter-information layer. Do not ship
    # a copied V2 generated resource when regenerating the native handbook.
    legacy_information = RESOURCES / "data/jem_advancements/questbook/information.json"
    if legacy_information.is_file():
        legacy_information.unlink()
    if emit_reward_selector:
        dump(OUTPUT_ROOT / "Catalog/changes/sets/ftb-questbook-reward-subjects.json", {
        "source": "Mods/JEMAchievementGuide/tools/questbook/book.json",
        "purpose": "Selector membership only; quantities and entitlement semantics remain in the authored book and JEMRewards.",
        "records": [{"id": item} for item in sorted({item["id"] for card in rows.values()
                    if card["conditions"] and card.get("status", "active") == "active"
                    for item in card["reward"]["items"] + card["reward"]["choice"]})]})
    compile_handbook(book)
    language.write()
    data = {"version": 13, "jem_universal_book": True, "jem_questbook_revision": 6, "jem_visual_v4": True, "jem_history_migration": False,
            "default_autoclaim_rewards": "disabled", "default_consume_items": False,
            "default_quest_disable_jei": False, "default_reward_team": False, "detection_delay": 20,
            "disable_gui": False, "drop_book_on_death": False, "drop_loot_crates": False,
            "grid_scale": 0.5, "pause_game": False, "progression_mode": "flexible", "show_lock_icons": True}
    dump(BOOK / "data.snbt", data)
    # The user confirmed that nobody used the previous quests. Start V4 fresh,
    # without historical reward tables, archive quests or a migration checkpoint.
    previous = MANIFEST_ROOT / "compiled-files.json"
    generated = sorted(name.replace("\\", "/") for name in generated)
    current = set(generated)
    if previous.exists():
        for old in json.loads(previous.read_text(encoding="utf-8")):
            normalized = old.replace("\\", "/")
            relative = safe_relative(normalized)
            if not re.fullmatch(r"(?:chapters|reward_tables)/[A-Za-z0-9_.-]+\.snbt", normalized):
                raise ValueError(f"Unexpected generated FTB path: {old}")
            target = safe_child(BOOK, relative)
            if normalized not in current and target.is_file():
                target.unlink()
    dump(previous, generated)
    mapping_file = OUTPUT_ROOT / "Docs/quests/card-mapping.csv"
    mapping_file.parent.mkdir(parents=True, exist_ok=True)
    with mapping_file.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.writer(stream); writer.writerow(["HTML ID", "Chapter", "FTB ID / informational element", "Conditions", "Reward", "Status", "Difference"]); writer.writerows(mapping)
    print(f"Compiled {len(book['chapters'])} chapters, {len(mapping)} mapped cards, {len(all_rules)} indexed predicates.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--without-reward-selector", action="store_true", help="Leave the Atlas selector to its active owner during parallel authoring")
    parser.add_argument("--output-root", type=Path, default=ROOT / "Mods/JEMAchievementGuide/build/questbook-v4-candidate",
                        help="Isolated candidate root, the default until all producers and migration are ready")
    parser.add_argument("--activate", action="store_true",
                        help="Activate only after the exact source producer/migration receipt is complete")
    arguments = parser.parse_args()
    if arguments.activate:
        require_activation_ready()
    candidate_output(ROOT if arguments.activate else arguments.output_root)
    compile_book(emit_reward_selector=not arguments.without_reward_selector)


"""Compile the authored book to native FTB 2001.4.22 SNBT and indexed event rules.

book.json owns text, bindings, identities and coordinates. Compilation never assigns
new coordinates to existing cards and never imports over manual edits.
"""
from __future__ import annotations

import csv
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[3]
SOURCE = Path(__file__).parent / "questbook"
RESOURCES = Path(__file__).resolve().parents[1] / "src/main/resources"
BOOK = ROOT / "Client/ManagedPack/config/ftbquests/quests"


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
            tasks.append({"id": identity("advancement", card["id"]), "type": "advancement",
                          "advancement": binding["argument"]})
        elif binding["kind"] == "kill" and len(targets) == 1 and simple(targets[0]) and not binding["distinct"]:
            tasks.append({"id": identity("kill", card["id"]), "type": "kill", "entity": targets[0]["ids"][0], "value": binding["count"]})
        else:
            task_id = card.get("preserved_task_id", identity("event", f"{card['id']}/{number}"))
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
            old = json.loads(path.read_text("utf-8")) if path.exists() else {}
            old = {key: value for key, value in old.items() if not key.startswith("jem.atlas.")}
            dump(path, dict(old, **shared[language], **values))


def compile_book(*, emit_reward_selector=True):
    book = json.loads((SOURCE / "book.json").read_text(encoding="utf-8"))
    all_rules, mapping, generated, information = [], [], [], {}
    language = Localization()
    rows = {card["id"]: card for chapter in book["chapters"] for card in chapter["cards"]}
    if len(rows) != sum(len(chapter["cards"]) for chapter in book["chapters"]):
        raise ValueError("Duplicate source card identity")
    if len({card["ftb_id"] for card in rows.values()}) != len(rows):
        raise ValueError("Duplicate native quest identity")
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
    chapter_by_key = {chapter["id"]: chapter for chapter in book["chapters"]}
    hub = chapter_by_key[book["hub"]]
    reference_hint = language.plain_key("reference.hint", "Reference only - no reward or completion", "Справка без награды и процента завершения")
    free_order = language.text("free_order", "Follow your own route. Nearby rewards are never prerequisites.", "Выбирай свой путь. Награда соседа никогда не становится условием.")
    choice_title = language.text("choice", "Choose one", "Один предмет на выбор")
    for chapter in book["chapters"]:
        quests, references = [], []
        images = list(chapter.get("images", []))
        chapter_text = chapter["localization"]
        chapter_title = language.text("chapter." + chapter["id"], chapter_text["en_us"]["title"], chapter_text["ru_ru"]["title"])
        images.append({**chapter.get("title_caption", {"x": 0, "y": 0, "width": 12, "height": 5}),
                       "image": "ftblibrary:textures/icons/blank.png", "click": "jem-label:" + chapter_title, "order": 30})
        if chapter.get("background"):
            images.insert(0, dict(chapter["background"], order=-100))
        routes = chapter.get("routes", [])
        if chapter["id"] != hub["id"]:
            routes = [{"chapter": hub["id"], "position": chapter.get("back_position", [-20, -15]), "back": True}] + routes
        for route in routes:
            destination = chapter_by_key[route["chapter"]]
            x, y = route["position"]
            label = language.text("route." + destination["id"],
                                  destination["localization"]["en_us"]["title"], destination["localization"]["ru_ru"]["title"])
            hint = language.plain_key("route_hint." + destination["id"],
                                     "Open: " + destination["localization"]["en_us"]["title"],
                                     "Открыть: " + destination["localization"]["ru_ru"]["title"])
            icon = "minecraft:textures/item/compass.png" if route.get("back") else destination.get("portal_image", "minecraft:textures/item/book.png")
            images.append({"x": x, "y": y, "width": 1.25, "height": 1.25,
                           "image": icon, "click": "#" + destination["ftb_id"], "hover": [hint], "order": 20})
            images.append({"x": x, "y": y + 1.5, "width": 5.2, "height": 1.3,
                           "image": "ftblibrary:textures/icons/blank.png", "click": "jem-label:" + label, "order": 21})
        for route in chapter.get("quest_routes", []):
            destination = rows[route["quest"]]
            x, y = route["position"]
            hint = language.plain_key("quest_route." + chapter["id"] + "." + destination["id"], route["en_us"], route["ru_ru"])
            images.append({"x": x, "y": y, "width": .8, "height": .8,
                           "image": "minecraft:textures/item/filled_map.png", "click": "#" + destination["ftb_id"],
                           "hover": [hint], "order": 20})
        for card in chapter["cards"]:
            localized = card["localization"]
            text = {key: language.text("card." + card["id"] + "." + key,
                                      localized["en_us"][key], localized["ru_ru"][key])
                    for key in ("title", "condition", "purpose", "tip", "reward")}
            information[card["id"]] = {"title": text["title"], "paragraphs": [text["condition"], text["purpose"], text["tip"]]}
            status = card.get("status", "active") if card["conditions"] else "reference"
            if status != "active":
                images.append({"x": card["position"][0], "y": card["position"][1], "width": 0.9, "height": 0.9,
                               "image": "minecraft:textures/item/book.png", "click": "jem-info:" + card["id"],
                               "hover": ["jem.atlas.card." + card["id"] + ".title", reference_hint], "order": 20})
                mapping.append([card["id"], chapter["id"], "chapter image: jem-info:" + card["id"], card["condition"], "Нет", status, card.get("implementation_difference", card.get("adaptation", ""))])
                continue
            tasks, rules = task_definitions(card)
            for task in tasks:
                if "title" in task:
                    task["title"] = text["condition"]
            all_rules.extend(rules)
            description = [text["condition"], "", text["purpose"], "", text["tip"], "", text["reward"]]
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
                table_path = BOOK / "reward_tables" / (card["id"].lower().replace(".", "_") + ".snbt")
                dump(table_path, table); generated.append(str(table_path.relative_to(BOOK)))
                rewards.append({"id": identity("choice", card["id"]), "type": "choice", "table_id": table_id, "team_reward": False})
            quest = {"id": card["ftb_id"], "title": text["title"], "subtitle": text["condition"],
                     "description": description, "icon": card["icon"], "x": card["position"][0], "y": card["position"][1],
                     "shape": "diamond" if reward["tier"] == "capstone" else "hexagon" if card["collection"] else "circle",
                     "size": 1.25 if reward["tier"] == "capstone" else 1.0,
                     "tasks": tasks, "rewards": rewards, "jem_source_id": card["id"],
                     "jem_required_mods": card.get("required_mods", []),
                     "jem_economy": any(rule["kind"] == "operation" for rule in rules),
                     "jem_dedicated_event": any(rule.get("dedicated") for rule in rules)}
            quest.update(card.get("native_options", {}))
            quests.append(quest)
            reward_summary = "; ".join(f"{item['count']}x {item['id']}" for item in reward["items"])
            if reward["choice"]: reward_summary += "; один на выбор: " + ", ".join(f"{item['count']}x {item['id']}" for item in reward["choice"])
            mapping.append([card["id"], chapter["id"], card["ftb_id"], card["condition"], reward_summary + f"; XP {reward['xp']}", "adapted" if card.get("adaptation") else "active", card.get("implementation_difference", card.get("adaptation", ""))])
        definition = {"id": chapter["ftb_id"], "filename": chapter["filename"], "title": chapter_title,
                      "subtitle": [language.text("intro." + chapter["id"], chapter_text["en_us"]["intro"], chapter_text["ru_ru"]["intro"]), free_order] + references,
                      "icon": chapter["icon"], "group": groups[chapter["group"]]["id"], "order_index": chapter["order"],
                      "default_quest_shape": "circle", "default_hide_dependency_lines": True,
                      "progression_mode": "flexible", "quests": quests, "images": images,
                      "jem_atlas_hub": chapter["id"] == hub["id"]}
        target = BOOK / "chapters" / (chapter["filename"] + ".snbt")
        dump(target, definition); generated.append(str(target.relative_to(BOOK)))
    dump(RESOURCES / "data/jem_advancements/questbook/rules.json", all_rules)
    dump(RESOURCES / "data/jem_advancements/questbook/information.json", information)
    if emit_reward_selector:
        dump(ROOT / "Catalog/changes/sets/ftb-questbook-reward-subjects.json", {
        "source": "Mods/JEMAchievementGuide/tools/questbook/book.json",
        "purpose": "Selector membership only; quantities and entitlement semantics remain in the authored book and JEMRewards.",
        "records": [{"id": item} for item in sorted({item["id"] for card in rows.values()
                    if card["conditions"] and card.get("status", "active") == "active"
                    for item in card["reward"]["items"] + card["reward"]["choice"]})]})
    language.write()
    data = {"version": 13, "jem_universal_book": True, "jem_questbook_revision": 4,
            "default_autoclaim_rewards": "disabled", "default_consume_items": False,
            "default_quest_disable_jei": False, "default_reward_team": False, "detection_delay": 20,
            "disable_gui": False, "drop_book_on_death": False, "drop_loot_crates": False,
            "grid_scale": 0.5, "pause_game": False, "progression_mode": "flexible", "show_lock_icons": True}
    dump(BOOK / "data.snbt", data)
    # Preserve every retired quest/task identity in an inaccessible optional archive.
    preserved = {card["ftb_id"] for card in rows.values() if "preserved_task_id" in card}
    for legacy in (SOURCE / "legacy").glob("*.snbt"):
        lines = legacy.read_text(encoding="utf-8").splitlines()
        lines = [line for line in lines if not any('id: "' + key + '"' in line for key in preserved)]
        raw = "\n".join(lines) + "\n"
        raw = raw.replace("{", "{\n jem_archive: true\n always_invisible: true\n", 1)
        raw = re.sub(r'(?m)^(\s*)id: "[A-Fa-f0-9]+"', lambda m: m[1] + 'id: "' + identity("archive_chapter", legacy.stem) + '"', raw, count=1)
        raw = re.sub(r'filename: "[^"]+"', 'filename: "_archive_' + legacy.stem + '"', raw, count=1)
        target = BOOK / "chapters" / ("_archive_" + legacy.name)
        target.parent.mkdir(parents=True, exist_ok=True); target.write_text(raw, encoding="utf-8")
        generated.append(str(target.relative_to(BOOK)))
        retired = BOOK / "chapters" / legacy.name
        if retired.is_file():
            if retired.read_bytes() != legacy.read_bytes():
                raise ValueError(f"Retired chapter was edited after archival: {retired}")
            retired.unlink()
    previous = SOURCE / "compiled-files.json"
    if previous.exists():
        for old in json.loads(previous.read_text(encoding="utf-8")):
            if old not in generated and (BOOK / old).is_file(): (BOOK / old).unlink()
    dump(previous, sorted(generated))
    mapping_file = ROOT / "Docs/quests/card-mapping.csv"
    mapping_file.parent.mkdir(parents=True, exist_ok=True)
    with mapping_file.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.writer(stream); writer.writerow(["HTML ID", "Chapter", "FTB ID / informational element", "Conditions", "Reward", "Status", "Difference"]); writer.writerows(mapping)
    print(f"Compiled {len(book['chapters'])} chapters, {len(mapping)} mapped cards, {len(all_rules)} indexed predicates.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--without-reward-selector", action="store_true", help="Leave the Atlas selector to its active owner during parallel authoring")
    arguments = parser.parse_args()
    compile_book(emit_reward_selector=not arguments.without_reward_selector)

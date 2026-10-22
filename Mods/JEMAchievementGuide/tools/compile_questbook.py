"""Compile the authored book to native FTB 2001.4.22 SNBT and indexed event rules.

book.json owns text, bindings, identities and coordinates. Compilation never assigns
new coordinates to existing cards and never imports over manual edits.
"""
from __future__ import annotations

import csv
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


def compile_book():
    book = json.loads((SOURCE / "book.json").read_text(encoding="utf-8"))
    all_rules, mapping, generated, information = [], [], [], {}
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
    groups = [{"id": identity("group", name), "title": name, "icon": icon} for name, icon in (
        ("Освоиться", "minecraft:compass"), ("Свой дом", "minecraft:oak_door"),
        ("Мастерская и пути", "create:cogwheel"), ("Живой мир", "minecraft:oak_sapling"),
        ("Приключения", "minecraft:iron_sword"), ("Кампания и дальние цели", "minecraft:ender_eye"))]
    dump(BOOK / "chapter_groups.snbt", {"chapter_groups": groups})
    for chapter in book["chapters"]:
        quests, references = [], []
        images = list(chapter.get("images", []))
        for card in chapter["cards"]:
            information[card["id"]] = {"title": card["number"] + " · " + card["title"],
                                       "paragraphs": [card["condition"], card["purpose"], card["tip"]]
                                        + ([card["adaptation"]] if card.get("adaptation") else [])}
            status = card.get("status", "active") if card["conditions"] else "reference"
            if status != "active":
                images.append({"x": card["position"][0], "y": card["position"][1], "width": 0.9, "height": 0.9,
                               "image": "minecraft:textures/item/book.png", "click": "jem-info:" + card["id"],
                               "hover": [f"{card['number']} · {card['title']}", "Справка: без награды и процента завершения"], "order": 20})
                mapping.append([card["id"], chapter["id"], "chapter image: jem-info:" + card["id"], card["condition"], "Нет", status, card.get("implementation_difference", card.get("adaptation", ""))])
                continue
            tasks, rules = task_definitions(card)
            all_rules.extend(rules)
            description = ["Что сделать: " + card["condition"], "", "Зачем: " + card["purpose"], "", "Совет: " + card["tip"], "", "Награда: " + card["reward"]["reason"]]
            if card.get("adaptation"): description.extend(["", card["adaptation"]])
            if card["after"]:
                description.extend(["", "Рекомендуемый путь: " + "; ".join(rows[key]["number"] + " · " + rows[key]["title"] for key in card["after"] if key in rows) + ". Порядок свободный."])
            reward = card["reward"]
            rewards = [item_reward(card, i, item) for i, item in enumerate(reward["items"])]
            if reward["xp"]:
                rewards.append({"id": identity("xp", card["id"]), "type": "jem_advancements:quest_xp", "xp": reward["xp"], "team_reward": False})
            if reward["choice"]:
                table_id = identity("choice_table", card["id"])
                table = {"id": table_id, "title": "Один предмет на выбор", "use_title": True, "loot_size": 1,
                         "rewards": [item_reward(card, i, item, True) for i, item in enumerate(reward["choice"])]}
                table_path = BOOK / "reward_tables" / (card["id"].lower().replace(".", "_") + ".snbt")
                dump(table_path, table); generated.append(str(table_path.relative_to(BOOK)))
                rewards.append({"id": identity("choice", card["id"]), "type": "choice", "table_id": table_id, "team_reward": False})
            quest = {"id": card["ftb_id"], "title": card["number"] + " · " + card["title"], "subtitle": card["condition"],
                     "description": description, "icon": card["icon"], "x": card["position"][0], "y": card["position"][1],
                     "shape": "diamond" if reward["tier"] == "capstone" else "hexagon" if card["collection"] else "circle",
                     "size": 1.25 if reward["tier"] == "capstone" else 1.0,
                     "tasks": tasks, "rewards": rewards, "jem_source_id": card["id"],
                     "jem_required_mods": card.get("required_mods", []),
                     "jem_economy": any(rule["kind"] == "operation" for rule in rules),
                     "jem_dedicated_event": any(rule.get("dedicated") for rule in rules)}
            quests.append(quest)
            reward_summary = "; ".join(f"{item['count']}x {item['id']}" for item in reward["items"])
            if reward["choice"]: reward_summary += "; один на выбор: " + ", ".join(f"{item['count']}x {item['id']}" for item in reward["choice"])
            mapping.append([card["id"], chapter["id"], card["ftb_id"], card["condition"], reward_summary + f"; XP {reward['xp']}", "adapted" if card.get("adaptation") else "active", card.get("implementation_difference", card.get("adaptation", ""))])
        definition = {"id": chapter["ftb_id"], "filename": chapter["filename"], "title": chapter["title"],
                      "subtitle": [chapter["intro"][0], "Рекомендуемый порядок свободный; награда соседа не требуется."] + references,
                      "icon": chapter["icon"], "group": groups[chapter["group"]]["id"], "order_index": chapter["order"],
                      "default_quest_shape": "circle", "default_hide_dependency_lines": True,
                      "progression_mode": "flexible", "quests": quests, "images": images,
                      "autofocus_id": quests[0]["id"] if quests else ""}
        target = BOOK / "chapters" / (chapter["filename"] + ".snbt")
        dump(target, definition); generated.append(str(target.relative_to(BOOK)))
    dump(RESOURCES / "data/jem_advancements/questbook/rules.json", all_rules)
    dump(RESOURCES / "data/jem_advancements/questbook/information.json", information)
    dump(ROOT / "Catalog/changes/sets/ftb-questbook-reward-subjects.json", {
        "source": "Mods/JEMAchievementGuide/tools/questbook/book.json",
        "purpose": "Selector membership only; quantities and entitlement semantics remain in the authored book and JEMRewards.",
        "records": [{"id": item} for item in sorted({item["id"] for card in rows.values()
                    if card["conditions"] and card.get("status", "active") == "active"
                    for item in card["reward"]["items"] + card["reward"]["choice"]})]})
    data = {"version": 13, "jem_universal_book": True, "jem_questbook_revision": 3,
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
    compile_book()

"""Inventory authored/native book contracts and report preservation during authoring.

This is an offline content audit, not a gameplay scenario or a runtime test runner.
The baseline is an immutable inventory and is never refreshed by compilation.
"""
from __future__ import annotations

import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path
import re
import zipfile

ROOT = Path(__file__).resolve().parents[4]
SOURCE = Path(__file__).parent
NATIVE = ROOT / "Client/ManagedPack/config/ftbquests/quests"
DOCS = ROOT / "Docs/quests"
BASELINE = DOCS / "baseline-20261025.json"


def digest(value):
    raw = value if isinstance(value, bytes) else json.dumps(value, sort_keys=True, ensure_ascii=False).encode()
    return hashlib.sha256(raw).hexdigest()


def read_snbt(path):
    tokens = re.findall(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[{}\[\]:,]|[^\s{}\[\]:,]+', path.read_text("utf-8-sig"))
    offset = 0

    def take():
        nonlocal offset
        token = tokens[offset]
        offset += 1
        if token == "{":
            value = {}
            while tokens[offset] != "}":
                if tokens[offset] == ",":
                    offset += 1
                    continue
                key = take()
                if tokens[offset] != ":":
                    raise ValueError(f"Missing SNBT key separator in {path}")
                offset += 1
                value[str(key)] = take()
            offset += 1
            return value
        if token == "[":
            value = []
            while tokens[offset] != "]":
                if tokens[offset] == ",":
                    offset += 1
                else:
                    value.append(take())
            offset += 1
            return value
        if token.startswith('"'):
            return json.loads(token)
        if token.startswith("'"):
            return token[1:-1].replace("\\'", "'").replace("\\\\", "\\")
        if token in ("true", "false"):
            return token == "true"
        number = re.fullmatch(r"(-?(?:\d+(?:\.\d*)?|\.\d+)(?:[Ee][+-]?\d+)?)[bBsSlLfFdD]?", token)
        if number:
            return float(number[1]) if any(c in number[1] for c in ".eE") else int(number[1])
        return token

    result = take()
    if offset != len(tokens):
        raise ValueError(f"Unconsumed SNBT in {path}")
    return result


def inventory():
    book_path = SOURCE / "book.json"
    book = json.loads(book_path.read_text("utf-8"))
    cards = {}
    for chapter in book["chapters"]:
        for card in chapter["cards"]:
            cards[card["id"]] = {
                "chapter": chapter["id"], "native_id": card["ftb_id"], "title": card["title"],
                "position": card["position"], "bindings": card["bindings"], "after": card["after"],
                "conditions": card["conditions"], "required_mods": card["required_mods"],
                "status": card.get("status", "active") if card["conditions"] else "reference",
                "reward": card["reward"], "preserved_task_id": card.get("preserved_task_id"),
            }
    chapters, quests, tasks, rewards, tables = {}, {}, {}, {}, {}
    duplicates = []

    def insert(target, row, location):
        key = row["id"]
        if key in target:
            duplicates.append({"id": key, "location": location})
        target[key] = row

    for path in sorted((NATIVE / "chapters").glob("*.snbt")):
        chapter = read_snbt(path)
        chapters[chapter["id"]] = dict(chapter, source_path=path.relative_to(NATIVE).as_posix())
        for quest in chapter.get("quests", []):
            insert(quests, dict(quest, source_chapter=chapter["id"], source_path=path.relative_to(NATIVE).as_posix()), path.name)
            for task in quest.get("tasks", []):
                insert(tasks, dict(task, source_quest=quest["id"]), path.name)
            for reward in quest.get("rewards", []):
                insert(rewards, dict(reward, source_quest=quest["id"]), path.name)
    for path in sorted((NATIVE / "reward_tables").glob("*.snbt")):
        table = read_snbt(path)
        tables[table["id"]] = table
        for reward in table.get("rewards", []):
            insert(rewards, dict(reward, source_table=table["id"]), path.name)
    edges = sorted([q["id"], dependency] for q in quests.values() for dependency in q.get("dependencies", []))
    registry_targets = sorted({identity for card in cards.values() for binding in card["bindings"]
                               for target in binding["targets"] for identity in target["ids"]})
    return {"schema": 1, "source_sha256": digest(book_path.read_bytes()),
            "native_version": "2001.4.22", "cards": cards, "chapters": chapters,
            "quests": quests, "tasks": tasks, "rewards": rewards, "reward_tables": tables,
            "dependency_edges": edges, "registry_targets": registry_targets,
            "duplicates": duplicates,
            "legacy_source_sha256": {p.name: digest(p.read_bytes()) for p in sorted((SOURCE / "legacy").glob("*.snbt"))},
            "counts": {"source_cards": len(cards), "chapters": len(chapters), "quests": len(quests),
                       "tasks": len(tasks), "rewards_including_choice_options": len(rewards),
                       "reward_tables": len(tables), "dependency_edges": len(edges),
                       "active_cards": sum(c["status"] == "active" for c in cards.values()),
                       "reference_cards": sum(c["status"] != "active" for c in cards.values())}}


def contract(row, kind):
    excluded = {"source_path", "source_chapter", "title", "subtitle", "description", "x", "y", "icon", "shape", "size"}
    if kind == "cards":
        excluded = {"chapter", "title", "position"}
    result = {key: value for key, value in row.items() if key not in excluded}
    if kind == "quests":
        result["tasks"] = [contract(task, "tasks") for task in row.get("tasks", [])]
    return result


def report():
    baseline = json.loads(BASELINE.read_text("utf-8"))
    current = inventory()
    preservation = {}
    for kind in ("cards", "quests", "tasks", "rewards", "reward_tables"):
        old, new = baseline[kind], current[kind]
        changed = []
        for key in old.keys() & new.keys():
            before, after = contract(old[key], kind), contract(new[key], kind)
            if kind == "reward_tables":
                before.pop("title", None)
                after.pop("title", None)
            if before != after:
                changed.append(key)
        preservation[kind] = {"missing": sorted(old.keys() - new.keys()), "changed_contracts": sorted(changed),
                              "added": sorted(new.keys() - old.keys()), "preserved": len(old) - len(changed) - len(old.keys() - new.keys())}
    missing_edges = [edge for edge in baseline["dependency_edges"] if edge not in current["dependency_edges"]]
    native_ids = set(current["quests"]) | set(current["chapters"])
    authored = json.loads((SOURCE / "book.json").read_text("utf-8"))
    authored_chapters = {chapter["ftb_id"]: chapter for chapter in authored["chapters"]}
    navigation, broken_navigation, overlaps, title_overlaps, invalid_dependencies = [], [], [], [], []
    image_node_overlaps, interactive_image_overlaps, missing_resources = [], [], []
    for chapter in current["chapters"].values():
        if chapter.get("jem_archive"):
            continue
        for image in chapter.get("images", []):
            click = image.get("click", "")
            if click.startswith("#"):
                link = {"from": chapter["id"], "to": click[1:]}
                navigation.append(link)
                if click[1:] not in native_ids:
                    broken_navigation.append(link)
        nodes = chapter.get("quests", [])
        source_chapter = authored_chapters[chapter["id"]]
        zone = source_chapter["title_zone"]
        interactive = [image for image in chapter.get("images", []) if image.get("click", "").startswith(("#", "jem-label:", "jem-info:"))]
        for index, image in enumerate(interactive):
            for node in nodes:
                if abs(image["x"]-node["x"]) < (image["width"]+node.get("size",1))/2+.15 and abs(image["y"]-node["y"]) < (image["height"]+node.get("size",1))/2+.15:
                    image_node_overlaps.append({"chapter":chapter["id"],"quest":node["id"],"image":index})
            for other in interactive[index+1:]:
                if abs(image["x"]-other["x"]) < (image["width"]+other["width"])/2 and abs(image["y"]-other["y"]) < (image["height"]+other["height"])/2:
                    interactive_image_overlaps.append({"chapter":chapter["id"],"first":image.get("click"),"second":other.get("click")})
        for image in chapter.get("images", []):
            resource = image.get("image", "")
            if resource.startswith("jem_advancements:"):
                resource_path = ROOT / "Mods/JEMAchievementGuide/src/main/resources/assets/jem_advancements" / resource.split(":",1)[1]
                if not resource_path.is_file():
                    missing_resources.append(resource)
        for i, node in enumerate(nodes):
            half = node.get("size", 1) / 2
            if abs(node["x"]-zone["center"][0]) < zone["width"]/2+zone["padding"]+half and abs(node["y"]-zone["center"][1]) < zone["height"]/2+zone["padding"]+half:
                title_overlaps.append(node["id"])
            for other in nodes[i + 1:]:
                distance = ((node["x"] - other["x"]) ** 2 + (node["y"] - other["y"]) ** 2) ** .5
                if distance < (node.get("size", 1) + other.get("size", 1)) / 2 + .25:
                    overlaps.append([node["id"], other["id"]])
            for dependency in node.get("dependencies", []):
                if dependency not in native_ids:
                    invalid_dependencies.append([node["id"], dependency])
    hub = next((chapter["id"] for chapter in current["chapters"].values() if chapter.get("jem_atlas_hub")), None)
    reachable = {hub} if hub else set()
    changed = True
    while changed:
        before = len(reachable)
        reachable.update(link["to"] for link in navigation if link["from"] in reachable)
        changed = len(reachable) != before
    unreachable = [chapter["id"] for chapter in current["chapters"].values()
                   if not chapter.get("always_invisible") and chapter["id"] not in reachable]
    visiting, ranks, cycles = set(), {}, []
    def rank(key):
        if key in ranks:
            return ranks[key]
        if key in visiting:
            cycles.append(key)
            return 0
        visiting.add(key)
        parents = current["quests"][key].get("dependencies", [])
        value = max((rank(parent)+1 for parent in parents if parent in current["quests"]), default=0)
        visiting.remove(key)
        ranks[key] = value
        return value
    for key in current["quests"]:
        rank(key)
    language = {lang:json.loads((ROOT / "Mods/JEMAchievementGuide/src/main/resources/assets/jem_advancements/lang" / (lang+".json")).read_text("utf-8")) for lang in ("en_us","ru_ru")}
    translation_keys = set()
    for chapter in current["chapters"].values():
        if chapter.get("jem_archive"):
            continue
        for value in re.findall(r'jem\.atlas\.[A-Za-z0-9_.-]+', json.dumps(chapter)):
            translation_keys.add(value)
    missing_translations = {lang:sorted(translation_keys-set(values)) for lang,values in language.items()}
    provenance = []
    forbidden = re.compile(r"\b(?:FTB(?: Quests| Teams)?|JEI|Sophisticated Backpacks|Sophisticated Storage|Epic Fight|JEMRoles|JEMRewards|JEMCompat|Alex['’]s (?:Mobs|Caves)|Kaleidoscope(?: Cookery| Nether| End)?|Toms Simple Storage|Handcrafted|efs_iss|efiscompat)\b", re.I)
    namespace = re.compile(r"\b[a-z][a-z0-9_]*:[a-z][a-z0-9_./-]+\b")
    for lang, values in language.items():
        for key,value in values.items():
            if key.startswith("jem.atlas.") and (forbidden.search(value) or namespace.search(value)):
                provenance.append({"language":lang,"key":key,"text":value})
    registry_cache, unresolved_targets = {}, []
    def registered(kind, identity):
        if kind not in registry_cache:
            # The file is grouped by owning mod, which need not equal the target
            # namespace. Backports and retained persistence aliases rely on this.
            registry_cache[kind] = {row["registry_id"] for path in (ROOT / "Catalog/registries" / kind).glob("*.json")
                                    for row in json.loads(path.read_text("utf-8"))["records"]}
        return identity in registry_cache[kind]
    for key, card in current["cards"].items():
        for binding in card["bindings"]:
            kind = binding["kind"]
            target_kind = "entities" if kind in ("interact_entity","hit","kill","mount") else "blocks" if kind in ("place","break","use_block") else "items"
            if kind in ("native","advancement","dimension","operation","guard_hit"):
                continue
            for target in binding["targets"]:
                for identity in target["ids"]:
                    if not registered(target_kind,identity):
                        unresolved_targets.append({"card":key,"registry":target_kind,"target":identity,"new_objective":key not in baseline["cards"]})
    native_acquisitions = []
    efn = next((ROOT / "Client/ManagedPack/mods").glob("Epic Fight Nightfall*.jar"),None)
    if efn is None:
        efn = next((p for p in (ROOT / "Client/ManagedPack/mods").glob("*.jar") if "nightfall" in p.name.lower()),None)
    if efn:
        with zipfile.ZipFile(efn) as archive:
            for key,card in current["cards"].items():
                if key in baseline["cards"]:
                    continue
                for binding in card["bindings"]:
                    if binding["kind"] == "advancement":
                        resource = "data/"+binding["argument"].replace(":","/advancements/")+".json"
                        native_acquisitions.append({"card":key,"resource":resource,"exists":resource in archive.namelist()})
    result = {"schema": 1, "baseline": BASELINE.relative_to(ROOT).as_posix(), "before": baseline["counts"],
              "after": current["counts"], "preservation": preservation,
              "missing_dependency_edges": missing_edges,
              "legacy_sources_unchanged": baseline["legacy_source_sha256"] == current["legacy_source_sha256"],
              "duplicate_ids": current["duplicates"], "navigation_links": len(navigation),
              "broken_navigation": broken_navigation, "unreachable_chapters": unreachable,
              "invalid_dependencies": invalid_dependencies, "node_collisions": overlaps,
              "title_zone_intrusions": title_overlaps,
              "image_node_overlaps": image_node_overlaps, "interactive_image_overlaps": interactive_image_overlaps,
              "missing_authored_resources": sorted(set(missing_resources)),
              "dependency_cycles": sorted(set(cycles)), "maximum_dependency_rank":max(ranks.values(),default=0),
              "missing_translations": missing_translations, "player_visible_provenance":provenance,
              "atlas_unresolved_objective_targets":unresolved_targets,
              "new_native_acquisition_hooks":native_acquisitions,
              "canonical_campaign_main_leaves":authored_chapters[next(chapter["ftb_id"] for chapter in authored["chapters"] if chapter["id"]=="tab-28")]["campaign_leaves"],
              "runtime_acceptance": "UNEXERCISED: no client launch, gameplay scenario or screenshot capture authorized",
              "registry_acceptance": "Exact installed resources and canonical Atlas records inspected. Live registry/runtime detection not rerun."}
    (DOCS / "layout-audit-20261025.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", "utf-8")
    with (DOCS / "chapter-migration-20261025.csv").open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.writer(stream)
        writer.writerow(["source_card", "native_quest_id", "old_chapter", "new_chapter", "old_x", "old_y", "new_x", "new_y"])
        for key, old in baseline["cards"].items():
            new = current["cards"].get(key)
            writer.writerow([key, old["native_id"], old["chapter"], new["chapter"] if new else "MISSING", *old["position"], *(new["position"] if new else ["", ""])])
    print(json.dumps({key: value for key, value in result.items() if key not in ("preservation","atlas_unresolved_objective_targets")}, ensure_ascii=False, indent=2))
    print("Atlas unresolved target references:",len(unresolved_targets))
    print(json.dumps({key: dict(value, added=len(value["added"])) for key, value in preservation.items()}, ensure_ascii=False, indent=2))
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture-baseline", action="store_true")
    args = parser.parse_args()
    if args.capture_baseline:
        if BASELINE.exists():
            raise SystemExit("Baseline already exists. A rebuild must not replace preservation evidence.")
        snapshot = inventory()
        DOCS.mkdir(parents=True, exist_ok=True)
        BASELINE.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", "utf-8")
        print(json.dumps(snapshot["counts"], indent=2))
    else:
        report()

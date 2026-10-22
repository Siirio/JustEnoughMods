"""Import the supplied design once; never overwrite authored quest bindings/layouts."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil

from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[3]
SOURCE = Path(__file__).parent / "questbook"


def text(node):
    return node.get_text(" ", strip=True) if node else ""


def import_book(pack: Path):
    destination = SOURCE / "book.json"
    if destination.exists():
        raise SystemExit("Authored book already exists; import never replaces edited content.")
    soup = BeautifulSoup((pack / "JEM_FTB_Questbook_2026-10-22.html").read_text(encoding="utf-8"), "html.parser")
    rewards = json.loads((pack / "REWARDS.json").read_text(encoding="utf-8"))
    chapters = []
    for chapter in soup.select(".chapter"):
        cards = []
        for card in chapter.select(".quest"):
            key = card["id"].removeprefix("q-")
            columns = card.select(".quest-grid > div")
            conditions = []
            for condition in card.select(".technical > .condition"):
                fields = {}
                for paragraph in condition.select("p"):
                    label = paragraph.find("b")
                    if label:
                        fields[text(label).rstrip(":")] = text(paragraph)[len(text(label)):].strip()
                heading = condition.select_one(".condition-title")
                conditions.append({"action": text(heading).removesuffix(text(heading.find("span"))).strip(),
                                   "quantity": text(heading.find("span")), "fields": fields})
            branch = card.find_previous_sibling("h3", class_="branch")
            cards.append({"id": key, "number": text(card.select_one(".qnum")),
                          "title": text(card.select_one("h4")), "branch": text(branch),
                          "condition": text(columns[0].select_one("p")),
                          "purpose": text(columns[2].select_one("p")),
                          "tip": text(card.select_one(".quest-tip p")),
                          "after": [a["href"].removeprefix("#q-") for a in card.select(".dependency a")],
                          "kinds": card.get("data-kinds", "").split(),
                          "collection": card.get("data-collection") == "1",
                          "conditions": conditions,
                          "technical": text(card.select_one(".technical")),
                          "sources": [a["href"] for a in card.select(".technical > .sources a")],
                          "reward": rewards["quests"][key]})
        chapters.append({"id": chapter["id"], "title": text(chapter.select_one("summary strong")),
                         "intro": [text(p) for p in chapter.select(".chapter-intro p")], "cards": cards})
    book = {"schema": 1, "revision": rewards["revision"], "chapters": chapters,
            "appendices": [text(a) for a in soup.select(".appendix")],
            "implementation": [text(s) for s in soup.select("#implementation, #campaign, #rewards")],
            "provenance": {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                           for p in pack.iterdir() if p.is_file()}}
    SOURCE.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(book, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    reference = ROOT / "Docs/quests/reference/implementation-pack-2026-10-22"
    reference.mkdir(parents=True, exist_ok=True)
    for name in ("START_HERE.md", "JEM_FTB_Questbook_2026-10-22.html", "REWARDS.json"):
        shutil.copy2(pack / name, reference / name)
    for name in ("assets", "references"):
        shutil.copytree(pack / name, reference / name, dirs_exist_ok=True)
    print(f"Imported {len(chapters)} chapters / {sum(len(c['cards']) for c in chapters)} cards.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pack", type=Path)
    import_book(parser.parse_args().pack)

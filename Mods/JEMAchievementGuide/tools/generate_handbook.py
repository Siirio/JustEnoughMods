"""Materialize authored navigation; no registry inference or gameplay execution."""
from pathlib import Path
import json
import re

MODULE = Path(__file__).resolve().parents[1]
RESOURCES = MODULE / "src/main/resources"
ASSETS = RESOURCES / "assets/jem_advancements"
BOOK = ASSETS / "patchouli_books/just_enough_guide"


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def intro_parts(text):
    # Keep hyperlinks whole and leave a dedicated footer for the real item links.
    parts, current = [], ""
    for word in re.findall(r"\$\(l:[^)]*\).*?\$\(/l\)|\S+", text):
        if current and len(current) + len(word) + 1 > 160:
            parts.append(current)
            current = ""
        current += (" " if current else "") + word
    return [*parts, current] if current else parts


def compact_boss_pages(entry, localized, language):
    pages = localized["pages"]
    first = pages[0]
    excluded = ("указател", "начало боя", "добыча", "кампания") if language == "ru_ru" else (
        "locator", "starting combat", "loot", "campaign")
    combat = next((page for page in pages[1:] if not any(word in page.get("title", "").lower() for word in excluded)), None)
    if combat is None:
        combat = {"text": "Избегайте заметных атак и бейте после их завершения." if language == "ru_ru"
                  else "Avoid clearly signalled attacks and strike after they finish."}
    sentences = re.split(r"(?<=[.!?])\s+", " ".join(page.get("text", "") for page in pages))
    immunity_words = ("не действ", "невосприим", "стрел", "яд") if language == "ru_ru" else (
        "does not", "immune", "arrow", "poison")
    immunities = " ".join(sentence for sentence in sentences if any(word in sentence.lower() for word in immunity_words))
    if not immunities:
        immunities = "Особых иммунитетов не подтверждено." if language == "ru_ru" else "No special immunity is confirmed."
    loot = next((page.get("text", "") for page in pages if ("добыча" if language == "ru_ru" else "loot") in page.get("title", "").lower()), "")
    campaign = next((page.get("text", "") for page in pages if ("кампания" if language == "ru_ru" else "campaign") in page.get("title", "").lower()), "")
    if entry["subject"] == "block_factorys_bosses:sandworm":
        campaign += " Награда: 300 XP." if language == "ru_ru" else " Reward: 300 XP."
    return [
        {"type": "jem_advancements:boss_preview", "entity": entry["subject"], "icon": entry["icon"], "name": localized["name"]},
        {"type": "patchouli:text", "title": "Где найти" if language == "ru_ru" else "Where to find", "text": first.get("text", "")},
        {"type": "patchouli:text", "title": "Как сражаться" if language == "ru_ru" else "How to fight", "text": combat.get("text", "")},
        {"type": "patchouli:text", "title": "Что не действует" if language == "ru_ru" else "What does not work", "text": immunities},
        {"type": "jem_advancements:boss_description", "title": "Что выпадает" if language == "ru_ru" else "Drops",
         "text": (loot + " " + campaign).strip(), "drops": entry.get("drops", []),
         "drop_status": ("Список добычи не подтверждён" if language == "ru_ru" else "Drop list is not confirmed")
         if entry.get("drop_unresolved") else ("Нативные условия добычи сохраняются" if language == "ru_ru" else "Native loot conditions still apply")}
    ]


def generate_navigation(resources=RESOURCES, *, questbook_source=MODULE / "tools/questbook/book.json"):
    assets = resources / "assets/jem_advancements"
    book = assets / "patchouli_books/just_enough_guide"
    (assets / "advancement_guides/index.json").unlink(missing_ok=True)
    source = json.loads((MODULE / "src/main/handbook/navigation.json").read_text(encoding="utf-8"))
    translations = json.loads((MODULE / "src/main/handbook/translations.json").read_text(encoding="utf-8"))
    manuals = json.loads((MODULE / "src/main/handbook/manuals.json").read_text(encoding="utf-8"))
    sections = {section["id"] for section in source["sections"]}
    for language in ("ru_ru", "en_us"):
        # The handbook owns reference pages only. Prune retired notes and sections on regeneration.
        for kind in ("categories", "entries"):
            base = book / language / kind
            for path in base.rglob("*.json"):
                parts = path.relative_to(base).with_suffix("").parts
                if len(parts) < 2 or parts[0] != "handbook" or parts[1] not in sections:
                    path.unlink()
        for index, section in enumerate(source["sections"]):
            write(book / language / "categories/handbook" / (section["id"] + ".json"), {
                **section[language], "icon": section["icon"], "sortnum": index})
        for entry in source["entries"]:
            content = entry[language]
            write(book / language / "entries/handbook" / (entry["id"] + ".json"), {
                "name": content["name"], "category": "jem_advancements:handbook/" + entry["section"],
                "icon": entry["icon"], "jem_aliases": entry["aliases"], "pages": content["pages"]})
        for manual in manuals:
            status = "$(jemmanual:" + manual["id"] + ")"
            text = ("Нажмите кнопку внизу страницы, чтобы открыть настоящее руководство. "
                    "Синяя лента показывает все доступные книги. Кнопка возврата в руководстве вернёт сюда."
                    if language == "ru_ru" else
                    "Use the button below to open the native manual. The blue ribbon lists all available books. "
                    "The return button in a manual restores this page.")
            write(book / language / "entries/handbook/manuals" / (manual["id"] + ".json"), {
                "name": manual[language], "category": "jem_advancements:handbook/manuals",
                "icon": manual["icon"], "flag": "mod:" + manual["id"], "jem_aliases": manual["aliases"],
                "pages": [{"type": "patchouli:text", "title": manual[language], "text": text},
                          {"type": "patchouli:text", "title": "Доступ" if language == "ru_ru" else "Access", "text": status}]})
        lang_path = assets / "lang" / (language + ".json")
        lang = json.loads(lang_path.read_text(encoding="utf-8"))
        lang.pop("jem_advancements.handbook.section.mechanics", None)
        lang.update(translations[language])
        for section in source["sections"]:
            lang["jem_advancements.handbook.section." + section["id"]] = section[language]["name"]
        write(lang_path, lang)
    generate_authored_content(book)
    if questbook_source is not None:
        from compile_questbook import compile_handbook
        compile_handbook(json.loads(Path(questbook_source).read_text(encoding="utf-8")), resources=resources)


def generate_authored_content(book=BOOK):
    for section in ("bosses", "items"):
        path = MODULE / "src/main/handbook" / (section + ".json")
        if not path.exists():
            continue
        authored = json.loads(path.read_text(encoding="utf-8"))
        if section == "items":
            # Exposure is retired from the pack. Remove its old authored projections and stale output.
            authored["entries"] = [entry for entry in authored["entries"]
                                   if not entry.get("subject", "").startswith("exposure:")]
            for language in ("ru_ru", "en_us"):
                retired = book / language / "entries/handbook/items/exposure"
                if retired.exists():
                    for old in retired.glob("*.json"):
                        old.unlink()
                    retired.rmdir()
        if section == "bosses":
            # Remove retired projections in both languages, including stale prior output.
            retained = {entry["id"] for entry in authored["entries"] if entry.get("handbook_role", "boss") == "boss"}
            for language in ("ru_ru", "en_us"):
                for old in (book / language / "entries/handbook/bosses").rglob("*.json"):
                    if old.relative_to(book / language / "entries").with_suffix("").as_posix() not in retained:
                        old.unlink()
        for entry in authored["entries"]:
            if section == "bosses" and entry.get("handbook_role", "boss") != "boss":
                continue
            for language in ("ru_ru", "en_us"):
                localized = entry[language]
                value = {"name": localized["name"], "icon": entry["icon"],
                         "category": "jem_advancements:handbook/" + section, "sortnum": 0,
                         "flag": "mod:" + entry["subject"].split(":", 1)[0],
                         "jem_aliases": entry["jem_aliases"], "pages": localized["pages"]}
                if "summary" in localized:
                    value["jem_summary"] = localized["summary"]
                if section == "bosses":
                    if "dimension" in entry:
                        value["jem_dimension"] = entry["dimension"]
                    value["pages"] = compact_boss_pages(entry, localized, language)
                write(book / language / "entries" / (entry["id"] + ".json"), value)


def generate_enchantment_links():
    # Existing authored effect/level/application pages stay intact. Add one native link page.
    for language in ("ru_ru", "en_us"):
        for path in (BOOK / language / "entries/handbook/enchantments").glob("*.json"):
            value = json.loads(path.read_text(encoding="utf-8"))
            pages = [page for page in value["pages"] if page.get("type") != "jem_advancements:enchantment_links"]
            enchantment = next((page["enchantment"] for page in pages if "enchantment" in page), None)
            if enchantment:
                value["pages"] = [{"type": "jem_advancements:enchantment_links", "title": value["name"],
                                   "enchantment": enchantment}, *pages]
                write(path, value)


if __name__ == "__main__":
    generate_navigation()
    generate_enchantment_links()

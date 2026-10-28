"""Author exact acquisition budgets from a saved registry capture, and export trade restrictions.

This production authoring tool does not start Minecraft, run verification or infer
shared JEI category membership. Only JEMCompat's server-safe classifier admits
purchase listings. The recorded capture is historical input, never runtime proof.
"""
from __future__ import annotations
import argparse
import collections
import csv
import json
import math
import hashlib
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "Mods/JEMEconomy/src/main/resources/data/jem_economy/economy"
FINAL = ROOT / "Docs/economy/FINAL_PRICE_LIST"
RETIRED_ITEMS = {"handcrafted:hammer"}


def read(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def write_csv(name, fields, rows):
    with (FINAL / name).open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def author_prices(runtime):
    if any(row.get('schema',1)>=2 for row in read(DATA / "item_policies.json")):
        raise SystemExit("Rational schema2 pricing is owned by JEMEconomy/tools/author_shipping_economy.py; legacy integer authoring cannot overwrite it")
    existing = {row["item"]: row for row in read(DATA / "item_policies.json") if row.get("outcome") == "BUY_AND_SELL"}
    native = {row["item"]: row for name in ("native_item_prices.json", "profession_pools.json") for row in read(DATA / name)}
    items = {row["id"]: row for row in runtime["items"]}
    recipes = collections.defaultdict(list)
    for row in runtime["recipes"]:
        if row.get("count", 0) > 0 and row.get("ingredients") and all(row["ingredients"]):
            recipes[row["output"]].append(row)

    def budget(item):
        path = item.split(":", 1)[1]
        tags = " ".join(items[item]["tags"])
        if item in existing: return existing[item]["buy_copper"]
        if item in native: return native[item]["buy_copper"]
        if any(part in tags for part in ("minecraft:logs", "minecraft:planks", "minecraft:wooden_", "minecraft:saplings")): return 24
        if "wool" in path: return 12
        if "glass" in path: return 16
        if any(part in path for part in ("stone", "brick", "sand", "gravel", "concrete", "terracotta", "dirt", "clay", "moss", "snow", "leaves")): return 16
        if any(part in path for part in ("chair", "table", "bench", "carpet", "painting", "stool", "sign", "fence", "door", "slab", "stairs", "planks")): return 32
        return 128

    memo = {}
    def price(item, visiting=frozenset()):
        if item in existing: return existing[item]["buy_copper"]
        if item in native: return native[item]["buy_copper"]
        if item in memo: return memo[item]
        floor = budget(item)
        if item in visiting: return floor
        candidates = []
        for recipe in recipes[item]:
            total = 0
            for group in recipe["ingredients"]:
                choices = [price(ingredient, visiting | {item}) for ingredient in group if ingredient in items]
                if not choices: break
                total += min(choices)
            else:
                candidates.append(math.ceil(total / recipe["count"]))
        value = min(1_000_000_000, max(floor, min(candidates, default=floor)))
        memo[item] = value
        return value

    rows = [{"item": item, "buy_copper": price(item)} for item in sorted(items) if item not in RETIRED_ITEMS]
    definition = {"schema": 1, "capture": runtime["capturedUtc"],
        "scope": "Exact registered item acquisition budgets only. Admission is exclusively JemCatalogCategories.isMarketBuilding plus wood/wool/glass/painting selectors, after trade_restrictions. Unlisted future items fail closed. Does not authorize NPC buyback.",
        "pricing": "Existing canonical/native acquisition prices retained; new building material budgets 12/16/24/32/128 Copper with captured ingredient-cost floor. Captured recipes are historical evidence; no runtime/arbitrage verification claimed.",
        "items": rows}
    (DATA / "building_purchase_prices.json").write_text(json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(rows)


def asset_archives():
    # Canonical managed source only; no active profile or personal runtime data.
    yield ROOT / "Client/JustEnoughMods/JustEnoughMods.jar"
    yield from sorted((ROOT / "Client/ManagedPack/mods").glob("*.jar"))
    yield from sorted((ROOT / "Client/ManagedPack/resourcepacks").glob("*.zip"))


def author_search_index(runtime, assets_root):
    terms = collections.defaultdict(set)
    sources = []
    def ingest(label, raw):
        text = raw.decode("utf-8-sig")
        # Native language assets permit comments/trailing commas through Gson.
        text = re.sub(r'("(?:\\.|[^"\\])*")|(/\*.*?\*/|//[^\r\n]*)',
            lambda match: match[1] or "", text, flags=re.DOTALL)
        text = re.sub(r",\s*([}\]])", r"\1", text)
        try: payload = json.loads(text, strict=False)
        except json.JSONDecodeError as failure: raise ValueError("Invalid installed language/tag source: " + label) from failure
        for key, value in payload.items():
            if isinstance(value, str) and key.startswith(("item.", "block.", "painting.")):
                terms[key].add(value)
        sources.append({"source": label, "sha256": hashlib.sha256(raw).hexdigest()})
    for archive in asset_archives():
        with zipfile.ZipFile(archive) as jar:
            for name in sorted(jar.namelist()):
                if re.fullmatch(r"assets/[^/]+/lang/(en_us|ru_ru)\.json", name):
                    ingest(archive.relative_to(ROOT).as_posix() + "!/" + name, jar.read(name))
    metadata = read(ROOT / "Client/JustEnoughMods/JustEnoughMods.json")
    index = read(assets_root / "indexes" / (metadata["assetIndex"]["id"] + ".json"))
    for locale in ("en_us", "ru_ru"):
        key = "minecraft/lang/" + locale + ".json"
        if key in index["objects"]:
            digest = index["objects"][key]["hash"]
            ingest("vanilla-assets/" + metadata["assetIndex"]["id"] + "/" + key + "@" + digest,
                (assets_root / "objects" / digest[:2] / digest).read_bytes())
    directories = [ROOT / "Client/ManagedPack/kubejs/assets",
        *sorted((ROOT / "Client/ManagedPack/resourcepacks").glob("*/assets"))]
    for directory in directories:
        for source in sorted(directory.glob("*/lang/*.json")):
            if source.stem in {"en_us", "ru_ru"}:
                ingest(source.relative_to(ROOT).as_posix(), source.read_bytes())
    items = {}
    for row in sorted(runtime["items"], key=lambda row: row["id"]):
        namespace, path = row["id"].split(":", 1)
        suffix = namespace + "." + path.replace("/", ".")
        names = terms["item." + suffix] | terms["block." + suffix]
        # Loaded display names cover items with native custom description IDs.
        captured = row.get("name", "")
        if captured and not captured.startswith(("item.", "block.")): names.add(captured)
        if names and row["id"] not in RETIRED_ITEMS: items[row["id"]] = sorted(names)
    paintings = {key: sorted(values) for key, values in sorted(terms.items())
        if key.startswith("painting.") and key.endswith(".title")}
    definition = {"schema": 1, "locales": ["en_us", "ru_ru"],
        "scope": "Source-backed search aliases only; no purchase admission. Union of installed native and packaged overlay names supports either visible localization without trusting server locale or client-supplied registries.",
        "sources": sources, "items": items, "paintings": paintings}
    (DATA / "market_search_terms.json").write_text(json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(items), len(paintings)


def ore_block_ids():
    # Forge ore block tags can differ from the captured Item tags. Resolve their
    # actual data-backed members for documentation; never infer them from names.
    members = collections.defaultdict(list)
    def ingest(namespace, path, raw):
        payload = json.loads(raw.decode("utf-8-sig"))
        values = [value if isinstance(value, str) else value["id"] for value in payload.get("values", [])]
        key = namespace + ":" + path
        if payload.get("replace"): members[key].clear()
        members[key].extend(values)
    for archive in asset_archives():
        with zipfile.ZipFile(archive) as jar:
            for name in sorted(jar.namelist()):
                match = re.fullmatch(r"data/([^/]+)/tags/blocks/(.+)\.json", name)
                if match: ingest(match[1], match[2], jar.read(name))
    for directory in [ROOT / "Client/ManagedPack/kubejs/data"]:
        for source in sorted(directory.glob("*/tags/blocks/**/*.json")):
            relative = source.relative_to(directory)
            ingest(relative.parts[0], "/".join(relative.parts[3:])[:-5], source.read_bytes())
    result = set()
    def visit(tag, seen):
        if tag in seen: return
        for value in members.get(tag, []):
            if value.startswith("#"): visit(value[1:], seen | {tag})
            else: result.add(value)
    # Every forge:ores/<mineral> is a real ore category even if its parent root
    # definition resides in the Forge runtime rather than the installed mod JAR.
    for tag in sorted(members):
        if tag == "forge:ores" or tag.startswith("forge:ores/"): visit(tag, set())
    return result


def export_catalog_overrides(runtime):
    """Apply permanent restrictions and conditional supply semantics to existing report exports."""
    restrictions = read(DATA / "trade_restrictions.json")
    prices = read(DATA / "building_purchase_prices.json")
    items = {row["id"]: row for row in runtime["items"]}
    exact = set(restrictions["all_npc_items"])
    ore_blocks = ore_block_ids()
    def npc_prohibited(item):
        path = item.split(":", 1)[-1]
        tags = " ".join(items.get(item, {}).get("tags", []))
        return (item in exact or item in ore_blocks or any(path.endswith(x) for x in restrictions["all_npc_path_suffixes"])
            or any(path.startswith(x) for x in restrictions["all_npc_path_prefixes"])
            or any(tag in tags for tag in restrictions["all_npc_item_tags"]))
    def market_prohibited(item):
        return npc_prohibited(item) or item.split(":", 1)[0] in restrictions["market_only_namespaces"]
    def edit_csv(name, transform):
        path = FINAL / name
        if not path.exists(): return
        with path.open(encoding="utf-8-sig", newline="") as stream:
            reader = csv.DictReader(stream); fields = reader.fieldnames; rows = list(reader)
        write_csv(name, fields, [value for row in rows if (value := transform(row)) is not None])
    edit_csv("MARKET.csv", lambda row: None if market_prohibited(row["item"]) else row)
    def native_row(row):
        if npc_prohibited(row.get("item", "")):
            row["supported_directions"] = "PLAYER_SELLS_ONLY" if row.get("native_direction") == "PLAYER_SELLS" else "PROHIBITED_NPC_RESULT"
            row["notes"] = "Permanent trade_restrictions.json overrides NPC sales and reverse offers. " + row.get("notes", "")
        return row
    edit_csv("VILLAGERS.csv", native_row)
    def excluded_row(row):
        item = row["item"]
        if npc_prohibited(item):
            row["classification"] = "PERMANENT_NPC_SALE_PROHIBITION"
            row["reason"] = "Canonical trade_restrictions.json forbids every NPC result, including old offers and item barter."
        elif market_prohibited(item):
            row["classification"] = "MARKET_PROHIBITED_NATIVE_PROFESSIONS_ALLOWED"
            row["reason"] = "Main Create namespace is excluded only from the public market; appropriate native offers remain eligible."
        else:
            row["classification"] = "CONDITIONAL_BUILDING_PURCHASE_NO_NEW_BUYBACK"
            row["reason"] = "Purchase is admitted only if the current shared JEMCompat category/explicit material selector matches. This file does not classify live JEI membership. Existing native eligibility is unchanged."
        row["source"] = "MarketSupply + shared JemCatalogCategories + trade_restrictions.json"
        return row
    edit_csv("EXCLUDED.csv", excluded_row)
    write_csv("BUILDING_PURCHASE_PRICES.csv", ["item", "buy_copper", "purchase_admission", "new_buyback", "source_capture"],
        ({"item": row["item"], "buy_copper": row["buy_copper"],
          "purchase_admission": "PROHIBITED" if market_prohibited(row["item"]) else "CONDITIONAL_SHARED_CATEGORY_OR_EXPLICIT_MATERIAL_SELECTOR",
          "new_buyback": "NONE", "source_capture": prices["capture"]} for row in prices["items"]))
    restriction_rows = []
    for field in ("all_npc_items", "all_npc_item_tags", "all_npc_block_tags", "all_npc_path_suffixes", "all_npc_path_prefixes", "market_only_namespaces"):
        restriction_rows.extend({"scope": field, "selector": value, "owner": "data/jem_economy/economy/trade_restrictions.json"} for value in restrictions[field])
    write_csv("TRADE_RESTRICTIONS.csv", ["scope", "selector", "owner"], restriction_rows)
    notice = """The 2026-10-16 building-supply change supersedes the older catalog totals and two-way-only coverage below.
`MARKET.csv` contains retained two-way commodities after public-market restrictions.
`BUILDING_PURCHASE_PRICES.csv` contains acquisition budgets, **not a live availability list**: only the
shared JEMCompat classifier/material selectors admit items, then permanent restrictions win.
Newly admitted purchases have no new NPC buyback. `TRADE_RESTRICTIONS.csv` exports the single canonical
restriction resource. The capture and any older recipe audit predate this change; no new gameplay,
arbitrage, visual or automated-test acceptance is claimed. See ../market-building-supply.md.
"""
    for name in ("IMPLEMENTATION_REPORT.md", "ECONOMY_BALANCE.md"):
        path = FINAL / name
        text = path.read_text(encoding="utf-8") if path.exists() else ""
        marker = "<!-- BEGIN BUILDING SUPPLY -->"
        if marker in text: text = text[text.index("<!-- END BUILDING SUPPLY -->") + len("<!-- END BUILDING SUPPLY -->"):].lstrip()
        path.write_text(marker + "\n" + notice + "<!-- END BUILDING SUPPLY -->\n\n" + text, encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capture", required=True)
    parser.add_argument("--author-search", action="store_true", help="Compile native/packaged en_us and ru_ru name aliases")
    parser.add_argument("--assets-root", type=Path, help="Local Mojang asset cache matching the canonical bootstrap metadata")
    parser.add_argument("--author-prices", action="store_true", help="Explicitly replace the authored acquisition-budget resource")
    parser.add_argument("--export", action="store_true", help="Export data and qualify historical reports; does not infer live categories")
    args = parser.parse_args()
    capture = read(args.capture)
    if args.author_search:
        if args.assets_root is None: parser.error("--author-search requires --assets-root")
        print("Authored search aliases:", author_search_index(capture, args.assets_root))
    if args.author_prices: print("Authored acquisition budgets:", author_prices(capture))
    if args.export: export_catalog_overrides(capture); print("Exported supply/restriction source data")

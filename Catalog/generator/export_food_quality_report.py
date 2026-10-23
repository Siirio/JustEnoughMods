"""Project the change-owned assignments into a deterministic human-readable report.

This tool never assigns tiers, infers gameplay, edits original registry facts or
executes Minecraft. Installed evidence is retained in the canonical change set.
"""
from collections import Counter
import json
from pathlib import Path


def main():
    root = Path(__file__).resolve().parents[2]
    data = json.loads((root / "Catalog/changes/food-quality-assignments.json").read_text(encoding="utf-8"))
    rows = sorted(data["assignments"], key=lambda row: row["item"])
    excluded = sorted(data["excluded_or_unresolved"], key=lambda row: row["item"])
    counts = Counter(row["mod"] for row in rows)
    tiers = Counter(row["tier"] for row in rows)
    lines = [
        "# Prepared-food quality coverage", "",
        "Derived from `Catalog/changes/food-quality-assignments.json`; assignments have one authority. Source review on 2026-10-23; no gameplay verification.", "",
        f"Classified: **{len(rows)} exact IDs** across {len(data['coverage']['audited_mods'])} installed modules: {tiers[1]} Tasty, {tiers[2]} Exquisite and {tiers[3]} Masterpiece. Unassigned native recipe outputs: **{len(excluded)}**. This is measured recipe-backed coverage, not a complete edible registry inventory.", "",
        "| Source mod | Classified IDs | Installed JAR | SHA-256 |",
        "| --- | ---: | --- | --- |",
    ]
    for mod in data["coverage"]["audited_mods"]:
        lines.append(f"| {mod['mod']} | {counts[mod['mod']]} | {mod['jar']} | `{mod['sha256']}` |")
    lines.extend([
        "",
        "Durations use Minecraft days according to the global project contract; internal native durations remain unchanged. Amplifiers are zero-based. Complete native recipe JSON, flexible/bulk alternatives, source registration classes and JAR hashes remain with each canonical assignment. No third-party recipe is changed.", "",
        "| Item registry ID | Mod | Actual installed recipe evidence | Tier | Family | Exact effect IDs/amplifiers | Duration | Rationale |",
        "| --- | --- | --- | ---: | --- | --- | --- | --- |",
    ])
    for row in rows:
        evidence = []
        for recipe in row["recipe_evidence"]:
            native = recipe["data"]
            inputs = {key: value for key, value in native.items() if key not in {
                "type", "result", "cooking_bubble_color", "cooking_texture", "finished_bubble_color", "finished_texture"
            }}
            evidence.append(f"`{recipe['path']}` ({recipe['type']}; " + json.dumps(inputs, ensure_ascii=False, separators=(",", ":")) + ")")
        effects = ", ".join(f"`{effect['id']}` amp {effect['amplifier']}" for effect in row["effects"])
        duration = {1: "0.4", 2: "0.75", 3: "1.5"}[row["tier"]]
        text = "<br>".join(evidence).replace("|", "\\|")
        lines.append(f"| `{row['item']}` | {row['mod']} | {text} | {row['tier']} | {row['family']} | {effects} | {duration} Minecraft days | {row['rationale']} |")
    lines.extend([
        "", "## Exclusions and unresolved native recipe outputs", "",
        "Raw ingredients, ordinary bread, intermediate dough/flour/oil/jam, beverages, potion-like items, pet food, repeated-use artifacts, native harmful/special foods and unverified placed-dessert routes receive no quality classification. Eating them never clears a bundle. This exact output inventory covers inspected recipe families; it does not imply every listed output is edible. Kaleidoscope FoodBiteBlock portions use their audited successful native callback; generic third-party cake blocks are not covered.", "",
        "| Exact native ID | Reason |", "| --- | --- |",
    ])
    lines.extend(f"| `{row['item']}` | {row['reason']} |" for row in excluded)
    lines.extend([
        "", "## Other inspected installed modules", "",
        "Beachparty 2.0.3, Vinery 1.4.41 and HerbalBrews 1.0.12 were inspected: reviewed routes are plain mussel cooking, drinks, brewing and non-food objects. These are not automatically gourmet food. Kaleidoscope Compat 2.6.5 is an integration module; its resources add no separately assigned namespace in this report. Other installed namespaces are not exhaustively covered.", "",
        "Native recipe conditions remain authoritative. A recipe requiring an absent optional ingredient/mod does not become obtainable because of a food assignment. Runtime additionally requires an actual edible consumed stack or the audited successful native FoodBiteBlock portion route.",
    ])
    destination = root / "Docs/food/COVERAGE.md"
    destination.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {destination.relative_to(root)}: {len(rows)} classified, {len(excluded)} excluded/unresolved recipe outputs")


if __name__ == "__main__":
    main()

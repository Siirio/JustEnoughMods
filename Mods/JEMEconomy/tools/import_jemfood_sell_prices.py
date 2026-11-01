#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from fractions import Fraction
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
POLICIES = ROOT / "Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/item_policies.json"
ASSIGNMENTS = ROOT / "Catalog/changes/food-quality-assignments.json"
ITEM_REGISTRIES = ROOT / "Catalog/registries/items"
BLOCK_REGISTRIES = ROOT / "Catalog/registries/blocks"
REPORT = ROOT / "Docs/economy/jemfood-sell-only-prices-2026-11-01.csv"
CERTIFICATE = ROOT / "Docs/economy/shipping-monetary-potential-certificate.json"
BLOCK_ONLY_ID = "natures_spirit:cheese_block"
PRICE_QUANTUM = 1_000_000
PORTION_RELATIONSHIPS = {
    "bakery:apple_pie_slice": ("bakery:apple_pie", Fraction(1, 4)),
    "bakery:bundt_cake_slice": ("bakery:bundt_cake", Fraction(1, 4)),
    "bakery:chocolate_cake_slice": ("bakery:chocolate_cake", Fraction(1, 4)),
    "bakery:chocolate_tart_slice": ("bakery:chocolate_tart", Fraction(1, 4)),
    "bakery:linzer_tart_slice": ("bakery:linzer_tart", Fraction(1, 4)),
    "bakery:pudding_slice": ("bakery:pudding", Fraction(1, 4)),
    "bakery:strawberry_cake_slice": ("bakery:strawberry_cake", Fraction(1, 4)),
    "bakery:sweetberry_cake_slice": ("bakery:sweetberry_cake", Fraction(1, 4)),
    "dustydecorations:sweet_berry_pie_half": ("dustydecorations:sweet_berry_pie", Fraction(1, 2)),
    "dustydecorations:sweet_berry_pie_one_fourths": ("dustydecorations:sweet_berry_pie", Fraction(1, 4)),
    "dustydecorations:sweet_berry_pie_three_fourths": ("dustydecorations:sweet_berry_pie", Fraction(3, 4)),
}


def load_registry_ids(folder: Path) -> set[str]:
    ids: set[str] = set()
    for path in sorted(folder.glob("*.json")):
        envelope = json.loads(path.read_text(encoding="utf-8"))
        ids.update(row["registry_id"] for row in envelope.get("records", []))
    return ids


def read_input(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as source:
        rows = list(csv.DictReader(source, delimiter="\t"))
    if len(rows) != 848 or len({row["item"] for row in rows}) != 848:
        raise ValueError("JEMFood price input must contain exactly 848 unique IDs")
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("--previous-policies", type=Path, default=POLICIES)
    args = parser.parse_args()

    rows = read_input(args.input)
    assignments = json.loads(ASSIGNMENTS.read_text(encoding="utf-8"))["assignments"]
    assignment_by_id = {row["id"]: row for row in assignments}
    if len(assignment_by_id) != 848:
        raise ValueError("Live JEMFood assignment owner must contain exactly 848 unique IDs")

    item_ids = load_registry_ids(ITEM_REGISTRIES)
    block_ids = load_registry_ids(BLOCK_REGISTRIES)
    policies = json.loads(args.previous_policies.read_text(encoding="utf-8"))
    policy_by_id = {row["item"]: row for row in policies}
    if len(policy_by_id) != len(policies):
        raise ValueError("Duplicate current economy policy ID")

    previous_outcomes: dict[str, str] = {}
    if REPORT.exists():
        with REPORT.open(encoding="utf-8-sig", newline="") as prior_report:
            previous_outcomes = {row["item"]: row["previous_outcome"] for row in csv.DictReader(prior_report)}
    certificate = json.loads(CERTIFICATE.read_text(encoding="utf-8"))
    certified_potential = {item_id: Fraction(value) for item_id, value in certificate["potential"].items()}

    prepared: dict[str, dict[str, object]] = {}
    for input_row in rows:
        item_id = input_row["item"]
        assignment = assignment_by_id.get(item_id)
        if assignment is None:
            raise ValueError(f"Missing live JEMFood assignment: {item_id}")
        archived_traits = [value for value in input_row["blessing_traits_archive"].split(",") if value]
        if int(input_row["tier"]) != assignment["quality_tier"] or archived_traits != assignment["blessing_traits"]:
            raise ValueError(f"Live JEMFood tier/trait mismatch: {item_id}")

        is_item = item_id in item_ids
        is_block = item_id in block_ids
        if not is_item:
            if item_id != BLOCK_ONLY_ID or not is_block:
                raise ValueError(f"Unresolved non-item JEMFood identity: {item_id}")
            prepared[item_id] = {
                "input": input_row,
                "assignment": assignment,
                "is_item": False,
                "previous": policy_by_id.get(item_id),
            }
            continue

        previous = policy_by_id.get(item_id)
        proposed = Fraction(int(input_row["sell_copper"]), 1)
        tier_cap = Fraction(int(input_row["max_final_sell_copper"]), 1)
        potential = certified_potential.get(item_id)
        if potential is None:
            raise ValueError(f"Missing certified recipe potential: {item_id}")
        effective = proposed
        demand_multiplier = Fraction(1, 1) if previous and previous.get("fixed_price") else Fraction(3, 2)
        if potential == 0:
            effective = Fraction(1, PRICE_QUANTUM)
        elif min(proposed * demand_multiplier, tier_cap) > potential:
            effective = min(proposed, potential / demand_multiplier)
            numerator = (effective.numerator * PRICE_QUANTUM) // effective.denominator
            if numerator <= 0:
                raise ValueError(f"Certified safe price is below the supported quantum: {item_id}")
            effective = Fraction(numerator, PRICE_QUANTUM)
        prepared[item_id] = {
            "input": input_row,
            "assignment": assignment,
            "is_item": True,
            "previous": previous,
            "effective": effective,
            "final_cap": tier_cap,
            "potential": potential,
            "demand_multiplier": demand_multiplier,
        }

    for item_id, (whole_id, ratio) in PORTION_RELATIONSHIPS.items():
        portion = prepared[item_id]
        whole = prepared[whole_id]
        portion["effective"] = min(portion["effective"], whole["effective"] * ratio)
        portion["final_cap"] = min(portion["final_cap"], whole["final_cap"] * ratio)

    report_rows: list[dict[str, object]] = []
    enabled = 0
    safety_adjusted = 0
    certificate_bootstrap_minimums = 0
    for item_id in [row["item"] for row in rows]:
        row = prepared[item_id]
        input_row = row["input"]
        previous = row["previous"]
        if not row["is_item"]:
            report_rows.append({
                **input_row,
                "live_registry": "block_only",
                "sell_enabled": "false",
                "exclusion_reason": "No registered Item form in current Atlas snapshot",
                "previous_outcome": previous_outcomes.get(item_id, previous.get("outcome", "NONE") if previous else "NONE"),
                "demand_group": "",
                "effective_sell_numerator": "",
                "effective_sell_denominator": "",
                "effective_nominal_copper": "",
                "effective_final_cap_copper": "",
                "effective_max_demand_payout": "",
                "safety_adjusted": "false",
                "recipe_safety_basis": "block_only_exclusion",
                "certified_recipe_potential": "",
                "certificate_capture_utc": certificate["capturedUtc"],
            })
            continue

        demand_group = previous.get("demand_group", "cooking") if previous else "cooking"
        daily_player = max(1, int(previous.get("daily_player_units", 64))) if previous else 64
        daily_global = max(1, int(previous.get("daily_global_units", 2048))) if previous else 2048
        proposed = Fraction(int(input_row["sell_copper"]), 1)
        effective = row["effective"]
        final_cap = row["final_cap"]
        potential = row["potential"]
        demand_multiplier = row["demand_multiplier"]
        if final_cap.denominator != 1:
            raise ValueError(f"Portion final cap is not integral: {item_id}")
        effective_max = min(effective * demand_multiplier, final_cap)
        if potential > 0 and effective_max > potential:
            raise ValueError(f"Effective payout exceeds certified recipe potential: {item_id}")
        recipe_safety_basis = "exact_recipe_potential"
        if potential == 0:
            recipe_safety_basis = "certificate_bootstrap_minimum"
            certificate_bootstrap_minimums += 1
        adjusted = effective != proposed or final_cap != Fraction(int(input_row["max_final_sell_copper"]), 1)
        safety_adjusted += int(adjusted)
        safety_note = (
            "The curated nominal unit price is bounded by the existing exact recipe-potential certificate"
            if potential > 0 else
            "The older exact certificate has no positive bound for this previously inactive endpoint, so the supported minimum quantum is used pending recertification"
        )
        policy = {
            "schema": 3,
            "item": item_id,
            "outcome": "SELL_ONLY",
            "sell_copper": effective.numerator,
            "buy_copper": None,
            "sell_denominator": effective.denominator,
            "buy_denominator": 1,
            "max_final_sell_copper": final_cap.numerator,
            "daily_player_units": daily_player,
            "daily_global_units": daily_global,
            "demand_group": demand_group,
            "quota_family": item_id,
            "quota_units": 1,
            "notes": (
                "User 2026-11-01: verified live JEMFood Item is sell-only. "
                f"{safety_note}; the tier cap applies after demand and modifiers. "
                "Canonical stack/NBT validation and funded market settlement remain mandatory."
            ),
            "recipe_audit_adjusted": {
                "capturedUtc": certificate["capturedUtc"],
                "decision": safety_note,
                "certificate": (
                    "Exact expected-recipe monetary potential"
                    if potential > 0 else
                    "Minimum supported quantum pending exact recertification; current purchase-root audit has no unsafe endpoint"
                ),
            },
        }
        for key in ("fixed_price", "user_income_anchor", "native_tide_rarity"):
            if previous and key in previous:
                policy[key] = previous[key]
        policy_by_id[item_id] = policy
        enabled += 1
        report_rows.append({
            **input_row,
            "live_registry": "item",
            "sell_enabled": "true",
            "exclusion_reason": "",
            "previous_outcome": previous_outcomes.get(item_id, previous.get("outcome", "NONE") if previous else "NONE"),
            "demand_group": demand_group,
            "effective_sell_numerator": effective.numerator,
            "effective_sell_denominator": effective.denominator,
            "effective_nominal_copper": str(effective),
            "effective_final_cap_copper": str(final_cap),
            "effective_max_demand_payout": str(effective_max),
            "safety_adjusted": str(adjusted).lower(),
            "recipe_safety_basis": recipe_safety_basis,
            "certified_recipe_potential": str(potential),
            "certificate_capture_utc": certificate["capturedUtc"],
        })

    if enabled != 847:
        raise ValueError(f"Expected 847 sell-enabled Item identities, got {enabled}")

    ordered = [policy_by_id[item_id] for item_id in sorted(policy_by_id)]
    POLICIES.write_text(json.dumps(ordered, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    REPORT.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = list(rows[0]) + [
        "live_registry", "sell_enabled", "exclusion_reason", "previous_outcome", "demand_group",
        "effective_sell_numerator", "effective_sell_denominator", "effective_nominal_copper",
        "effective_final_cap_copper", "effective_max_demand_payout", "safety_adjusted", "recipe_safety_basis",
        "certified_recipe_potential", "certificate_capture_utc",
    ]
    with REPORT.open("w", encoding="utf-8-sig", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(report_rows)

    print(json.dumps({
        "input_rows": len(rows),
        "live_assignments": len(assignments),
        "sell_enabled_items": enabled,
        "block_only_exclusions": 1,
        "safety_adjusted_prices": safety_adjusted,
        "certificate_bootstrap_minimums": certificate_bootstrap_minimums,
        "policy_rows": len(ordered),
        "report": REPORT.relative_to(ROOT).as_posix(),
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

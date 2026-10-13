#!/usr/bin/env python3
from __future__ import annotations

import json
import sys

from impact_model import ROOT, canonical_records, effective_for, iter_envelopes


def records(pattern: str):
    for path, row in iter_envelopes(pattern):
        yield path.relative_to(ROOT).as_posix(), row


def find_id(value: str) -> list[dict]:
    matches = []
    for registry, rows in canonical_records().items():
        if value in rows:
            matches.append({
                "registry": registry,
                "path": f"Catalog/registries/{registry}/{value.partition(':')[0]}.json",
                "record": rows[value],
            })
    return matches


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: query_atlas.py id|behavior|impact <namespace:path> | role <role> | trade <outcome> | food-negative")
        return 2
    mode = sys.argv[1]
    value = sys.argv[2] if len(sys.argv) > 2 else ""
    matches = []
    if mode in {"id", "behavior", "impact", "why"}:
        base_matches = find_id(value)
        if mode == "id":
            matches = base_matches
        else:
            behavior = [effective_for(match["registry"], value, match["record"]) for match in base_matches]
            if mode == "impact":
                matches = [{
                    "registry": row["original_registry"]["registry"],
                    "id": value,
                    "impact_analysis": row["impact_analysis"],
                    "change_ids": [change["change_id"] for change in row["applied_jem_changes"]],
                } for row in behavior]
            else:
                matches = behavior
    elif mode == "role":
        for path, row in records("views/mobs/*.json"):
            if value in row.get("roles", []):
                matches.append({"path": path, "record": row})
    elif mode == "trade":
        for path, row in records("views/trade_policies/*.json"):
            if row.get("trade_outcome") == value:
                matches.append({"path": path, "record": row})
    elif mode == "food-negative":
        for path, row in records("views/foods/*.json"):
            if (row.get("villager_mood_base_points") or 0) < 0:
                matches.append({"path": path, "record": row})
    else:
        print(f"unknown mode: {mode}", file=sys.stderr)
        return 2
    print(json.dumps(matches, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

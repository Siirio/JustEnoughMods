#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"


def records(pattern: str):
    for path in CATALOG.glob(pattern):
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        for row in payload.get("records", []):
            yield path.relative_to(ROOT).as_posix(), row


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: query_atlas.py id <namespace:path> | role <role> | trade <outcome> | food-negative")
        return 2
    mode = sys.argv[1]
    value = sys.argv[2] if len(sys.argv) > 2 else ""
    matches = []
    if mode == "id":
        for path, row in records("registries/*/*.json"):
            if value in (row.get("registry_id"), row.get("item_id"), row.get("entity_id")):
                matches.append({"path": path, "record": row})
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

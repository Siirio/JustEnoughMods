#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"


def main() -> int:
    errors: list[str] = []
    canonical: dict[str, set[str]] = {}
    for registry_dir in sorted((CATALOG / "registries").iterdir()):
        ids: set[str] = set()
        for path in sorted(registry_dir.glob("*.json")):
            payload = json.loads(path.read_text(encoding="utf-8"))
            rows = payload.get("records", [])
            row_ids = [row.get("registry_id") for row in rows]
            if row_ids != sorted(row_ids):
                errors.append(f"unsorted records: {path.relative_to(ROOT)}")
            for registry_id in row_ids:
                if not registry_id or registry_id in ids:
                    errors.append(f"duplicate/blank ID in {registry_dir.name}: {registry_id}")
                ids.add(registry_id)
        canonical[registry_dir.name] = ids
    for view_dir in sorted((CATALOG / "views").iterdir()):
        for path in sorted(view_dir.glob("*.json")):
            payload = json.loads(path.read_text(encoding="utf-8"))
            for row in payload.get("records", []):
                item_id = row.get("item_id") or row.get("source_item_id")
                entity_id = row.get("entity_id")
                if item_id and item_id not in canonical.get("items", set()):
                    errors.append(f"view item missing canonical record: {item_id} ({path.relative_to(ROOT)})")
                if entity_id and entity_id not in canonical.get("entities", set()):
                    errors.append(f"view entity missing canonical record: {entity_id} ({path.relative_to(ROOT)})")
    if errors:
        print("\n".join(errors[:200]))
        print(f"errors={len(errors)}")
        return 1
    print(json.dumps({"valid": True, "canonical_counts": {key: len(value) for key, value in canonical.items()}}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

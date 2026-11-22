#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

from impact_model import (
    BASELINE_COVERAGE,
    EFFECTIVE_INDEX,
    baseline_coverage_digest,
    canonical_records_digest,
    change_records,
    effective_record_digest,
    read_json,
    records_digest,
    validate_change_records,
)


ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"


def main() -> int:
    errors: list[str] = validate_change_records()
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
    if not EFFECTIVE_INDEX.is_file():
        errors.append("missing generated effective index: Catalog/effective/index.json")
    else:
        effective = read_json(EFFECTIVE_INDEX)
        records = change_records()
        if effective.get("change_records_sha256") != records_digest(records):
            errors.append("stale effective index: run python Catalog/generator/maintain_atlas.py rebuild")
        if effective.get("canonical_records_sha256") != canonical_records_digest():
            errors.append("stale effective registry snapshot: run python Catalog/generator/maintain_atlas.py rebuild --full")
        effective_digests = {row.get("change_id"): row.get("record_sha256") for row in effective.get("changes", [])}
        for path, rule in records:
            if effective_digests.get(rule.get("change_id")) != effective_record_digest(path, rule):
                errors.append(f"stale effective change entry: {rule.get('change_id')}")
    if not BASELINE_COVERAGE.is_file():
        errors.append("missing baseline source coverage: run python Catalog/generator/maintain_atlas.py audit")
    elif read_json(BASELINE_COVERAGE).get("coverage_inputs_sha256") != baseline_coverage_digest():
        errors.append("stale baseline source coverage: run python Catalog/generator/maintain_atlas.py audit")
    if errors:
        print("\n".join(errors[:200]))
        print(f"errors={len(errors)}")
        return 1
    print(json.dumps({"valid": True, "canonical_counts": {key: len(value) for key, value in canonical.items()}, "change_records": len(change_records()), "effective_index": EFFECTIVE_INDEX.relative_to(ROOT).as_posix()}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

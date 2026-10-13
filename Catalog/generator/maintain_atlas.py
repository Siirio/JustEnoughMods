#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json

from impact_model import build_baseline_coverage, build_effective_index, scan_coverage, validate_change_records


def main() -> int:
    parser = argparse.ArgumentParser(description="Maintain the JEM Registry Atlas change/impact layer.")
    sub = parser.add_subparsers(dest="command", required=True)
    scan = sub.add_parser("scan", help="Find gameplay-source changes lacking a same-diff Atlas change record.")
    scan.add_argument("--base", help="Git base ref; omit to inspect the working tree.")
    scan.add_argument("--head", help="Git head ref used with --base (default HEAD).")
    sub.add_parser("rebuild", help="Validate records and refresh the compact effective index.")
    sub.add_parser("audit", help="Inventory all tracked canonical sources and mark unreviewed historical coverage explicitly.")
    args = parser.parse_args()
    if args.command == "scan":
        result = scan_coverage(args.base, args.head)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0 if result["valid"] else 1
    errors = validate_change_records()
    if errors:
        print("\n".join(errors))
        return 1
    if args.command == "audit":
        audit = build_baseline_coverage()
        print(json.dumps({"totals": audit["totals"], "groups": {name: {key: value for key, value in group.items() if key != "unresolved_paths"} for name, group in audit["groups"].items()}, "output": "Catalog/effective/baseline-coverage.json"}, ensure_ascii=False, indent=2))
        return 0
    print(json.dumps(build_effective_index(), ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

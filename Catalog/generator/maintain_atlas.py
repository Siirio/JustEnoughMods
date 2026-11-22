#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from artifact_delta import refresh as refresh_artifact
from impact_model import build_baseline_coverage, build_effective_index, scan_coverage, validate_change_records


def main() -> int:
    parser = argparse.ArgumentParser(description="Maintain the JEM Registry Atlas change/impact layer.")
    sub = parser.add_subparsers(dest="command", required=True)
    scan = sub.add_parser("scan", help="Find gameplay-source changes lacking a same-diff Atlas change record.")
    scan.add_argument("--base", help="Git base ref; omit to inspect the working tree.")
    scan.add_argument("--head", help="Git head ref used with --base (default HEAD).")
    rebuild = sub.add_parser("rebuild", help="Validate records and refresh only changed effective-index entries.")
    rebuild.add_argument("--full", action="store_true", help="Recompute every entry after registry snapshot changes.")
    artifact = sub.add_parser("artifact-delta", help="Refresh provenance for a replacement with unchanged Atlas evidence.")
    artifact.add_argument("--previous", required=True, help="Previous JAR used by the current Atlas manifest.")
    artifact.add_argument("--replacement", required=True, help="Fresh replacement JAR already installed in client and server targets.")
    sub.add_parser("audit", help="Inventory all tracked canonical sources and mark unreviewed historical coverage explicitly.")
    args = parser.parse_args()
    if args.command == "scan":
        result = scan_coverage(args.base, args.head)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0 if result["valid"] else 1
    if args.command == "artifact-delta":
        print(json.dumps(refresh_artifact(Path(args.previous), Path(args.replacement)), ensure_ascii=False, indent=2))
        return 0
    errors = validate_change_records()
    if errors:
        print("\n".join(errors))
        return 1
    if args.command == "audit":
        audit = build_baseline_coverage()
        print(json.dumps({"totals": audit["totals"], "groups": {name: {key: value for key, value in group.items() if key != "unresolved_paths"} for name, group in audit["groups"].items()}, "output": "Catalog/effective/baseline-coverage.json"}, ensure_ascii=False, indent=2))
        return 0
    _, report = build_effective_index(force_full=args.full)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

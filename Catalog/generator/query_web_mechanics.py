#!/usr/bin/env python3
"""Read source-backed native-mod mechanics without treating web prose as runtime truth.

This is an OFFLINE reader for human-curated external evidence; it does not scrape
websites, modify the pack, generate FTB Quests, or edit canonical registries.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[2]
RESEARCH = ROOT / "Catalog" / "research" / "web-mechanics"
REGISTRY_TYPES = {"items", "blocks", "entities", "effects", "enchantments", "biomes", "structures", "creative_tabs"}
QUEST_DETECTION = {"inventory", "custom_criterion", "manual", "not_applicable"}
FACT_KINDS = {"metadata", "progression", "acquisition", "interaction", "crafting", "combat", "integration", "compatibility", "automation"}


def load_research() -> list[dict]:
    paths = sorted(RESEARCH.glob("*.json"))
    if not paths:
        raise ValueError(f"No research documents found in {RESEARCH}")
    return [json.loads(path.read_text(encoding="utf-8")) for path in paths]


def installed_versions() -> dict[str, set[str]]:
    manifest_path = ROOT / "Catalog" / "manifest.json"
    if not manifest_path.is_file():
        raise ValueError(f"Missing installed-pack manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    mods: dict[str, set[str]] = {}
    def collect(artifact: dict) -> None:
        for mod in artifact.get("mods", []):
            mods.setdefault(mod["id"], set()).add(str(mod.get("version", "")))
        for nested in artifact.get("nested_dependencies", []):
            collect(nested)
    for artifact in manifest["artifacts"]:
        if artifact.get("enabled", False):
            collect(artifact)
    if not mods:
        raise ValueError("Installed-pack manifest contains no enabled mod IDs")
    return mods


def enrich(mod: dict, observation: dict) -> dict:
    sources = {s["id"]: s for s in mod["sources"]}
    return {
        "mod_id": mod["mod_id"],
        "installed_version_at_review": mod["installed_version"],
        "pack_snapshot_sha_at_review": mod["pack_snapshot_sha"],
        "researched_on": mod["researched_on"],
        **observation,
        "evidence_urls": [sources[s]["url"] for s in observation["source_ids"]],
    }


def audit(docs: list[dict], check_registry: bool = False) -> dict:
    errors, warnings = [], []
    from jsonschema import Draft202012Validator, FormatChecker

    schema = json.loads((RESEARCH.parent / "schema.json").read_text(encoding="utf-8"))
    Draft202012Validator.check_schema(schema)
    validator = Draft202012Validator(schema, format_checker=FormatChecker())
    if not docs:
        errors.append("No research documents supplied")
    for index, doc in enumerate(docs):
        for error in sorted(validator.iter_errors(doc), key=lambda e: str(e.json_path)):
            errors.append(f"document {index} {error.json_path}: {error.message}")
    if errors:
        return {"valid_structure": False, "errors": errors, "warnings": warnings}
    mods = installed_versions()
    snapshot = json.loads((ROOT / "Catalog" / "manifest.json").read_text(encoding="utf-8"))["snapshot_sha"]
    reviewed_snapshots = sorted({doc["pack_snapshot_sha"] for doc in docs})
    if reviewed_snapshots != [snapshot]:
        warnings.append(f"Research snapshots {reviewed_snapshots} differ from manifest snapshot {snapshot}; recheck applicability")
    seen_features: dict[str, str] = {}
    seen_mods: set[str] = set()
    registry_refs: list[tuple[str, str, str]] = []
    for doc in docs:
        mod = doc.get("mod_id", "")
        if doc.get("schema_version") != 1:
            errors.append(f"{mod}: schema_version must be 1")
        if not mod or mod in seen_mods:
            errors.append(f"duplicate/blank mod: {mod}")
        seen_mods.add(mod)
        if mod not in mods:
            warnings.append(f"{mod}: no matching enabled mod in Catalog/manifest.json")
        elif doc.get("installed_version") not in mods[mod]:
            warnings.append(f"{mod}: reviewed {doc.get('installed_version')} differs from manifest versions {sorted(mods[mod])}")
        src_ids = set()
        for s in doc.get("sources", []):
            sid, url = s.get("id"), s.get("url", "")
            if not sid or sid in src_ids:
                errors.append(f"{mod}: duplicate/blank source id {sid}")
            src_ids.add(sid)
            parsed = urlparse(url)
            if parsed.scheme != "https" or not parsed.netloc:
                errors.append(f"{mod}:{sid}: source must have a real https URL")
            if not s.get("publisher") or not s.get("reviewed_on") or not s.get("scope"):
                errors.append(f"{mod}:{sid}: missing source provenance")
        for f in doc.get("observations", []):
            key = f.get("key", "")
            if not key or key in seen_features:
                errors.append(f"duplicate/blank mechanic key: {key}")
            seen_features[key] = mod
            if not key.startswith(mod + ":"):
                errors.append(f"{key}: mechanic keys must be prefixed with owning mod id")
            if f.get("kind") not in FACT_KINDS:
                errors.append(f"{key}: unknown kind {f.get('kind')}")
            if not all(f.get(field) for field in ("title", "player_action", "outcome")):
                errors.append(f"{key}: missing meaningful text fields")
            if f.get("verification") != "EXTERNAL_SOURCE_ONLY" or f.get("runtime_verified") is not False:
                errors.append(f"{key}: web research cannot claim runtime verification")
            if not f.get("source_ids"):
                errors.append(f"{key}: no evidence source")
            for sid in f.get("source_ids", []):
                if sid not in src_ids:
                    errors.append(f"{key}: undeclared source {sid}")
            seed = f.get("quest_seed")
            if seed and (seed.get("detection") not in QUEST_DETECTION or not seed.get("goal_ru")):
                errors.append(f"{key}: invalid quest proposal")
            for ref in f.get("registry_refs", []):
                kind, rid = ref.get("registry"), ref.get("id", "")
                if kind not in REGISTRY_TYPES or ":" not in rid or rid.startswith(":") or rid.endswith(":"):
                    errors.append(f"{key}: bad registry reference {ref}")
                else:
                    registry_refs.append((key, kind, rid))
    edges: list[tuple[str, str]] = []
    for doc in docs:
        for f in doc.get("observations", []):
            for dep in f.get("depends_on", []):
                if dep not in seen_features:
                    errors.append(f"{f['key']}: unknown prerequisite feature {dep}")
                edges.append((f["key"], dep))
    graph = {a: [] for a in seen_features}
    for a, b in edges:
        if b in seen_features:
            graph[a].append(b)
    active, visited = set(), set()
    def visit(node: str) -> None:
        if node in visited:
            return
        if node in active:
            errors.append(f"cycle detected at {node}")
            return
        active.add(node)
        for to in graph[node]:
            visit(to)
        active.remove(node)
        visited.add(node)
    for node in graph:
        visit(node)
    if check_registry:
        ids: dict[str, set[str]] = {kind: set() for kind in REGISTRY_TYPES}
        for kind in ids:
            for path in (ROOT / "Catalog" / "registries" / kind).glob("*.json"):
                envelope = json.loads(path.read_text(encoding="utf-8"))
                ids[kind].update(x.get("registry_id", "") for x in envelope.get("records", []))
        for key, kind, rid in registry_refs:
            if rid not in ids[kind]:
                warnings.append(f"{key}: registry ref not statically present: {kind}/{rid}")
    return {
        "valid_structure": not errors,
        "reviewed_mods": len(seen_mods),
        "source_backed_observations": len(seen_features),
        "researched_dependency_edges": len(edges),
        "runtime_verified_by_this_source": 0,
        "errors": errors,
        "warnings": warnings,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Inspect web-researched native-mod mechanics, not actual runtime guarantees")
    sub = parser.add_subparsers(dest="mode", required=True)
    q = sub.add_parser("find", help="Search mods, feature keys, player actions and registry references")
    q.add_argument("text")
    p = sub.add_parser("quests", help="Show non-authoritative FTB Quests ideas for a mod")
    p.add_argument("mod_id")
    sub.add_parser("coverage", help="Show reviewed vs unreviewed installed mod IDs")
    g = sub.add_parser("graph", help="Show reviewed mechanic prerequisite edges")
    g.add_argument("--mod", default="", help="Optional owning mod id filter")
    a = sub.add_parser("audit", help="Check evidence, references and cycles without accessing network/game")
    a.add_argument("--check-registry", action="store_true", help="Warn if a source-backed registry ref is not statically indexed")
    args = parser.parse_args()
    try:
        docs = load_research()
        validation = audit(docs, getattr(args, "check_registry", False))
    except ImportError as error:
        parser.exit(2, f"Research validation requires jsonschema: install Catalog/research/requirements.txt ({error})\n")
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.exit(2, f"Cannot read research inputs: {error}\n")
    if args.mode != "audit" and not validation["valid_structure"]:
        print(json.dumps(validation, indent=2, ensure_ascii=False, sort_keys=True))
        return 1
    if args.mode != "audit":
        for warning in validation["warnings"]:
            print(f"warning: {warning}", file=sys.stderr)
    result = None
    if args.mode == "find":
        needle = args.text.casefold()
        result = [enrich(d, f) for d in docs for f in d["observations"] if needle in json.dumps({"mod":d["mod_id"], **f}, ensure_ascii=False).casefold()]
    elif args.mode == "quests":
        result = [enrich(d, f) for d in docs if d["mod_id"] == args.mod_id for f in d["observations"] if f.get("quest_seed")]
    elif args.mode == "graph":
        result = [{"from": f["key"], "requires": dep, "relation": "requires",
                   "verified_runtime": False, "conditions": f["conditions"],
                   "evidence_urls": enrich(d, f)["evidence_urls"],
                   "installed_version_at_review": d["installed_version"],
                   "pack_snapshot_sha_at_review": d["pack_snapshot_sha"]}
                  for d in docs for f in d["observations"] for dep in f["depends_on"]
                  if not args.mod or args.mod == d["mod_id"]]
    elif args.mode == "coverage":
        mods = installed_versions()
        reviewed = {d["mod_id"] for d in docs}
        result = {"installed_mod_ids": len(mods), "researched_mod_ids": len(reviewed & mods.keys()),
                  "unresearched_mod_ids": sorted(mods.keys() - reviewed),
                  "observations": sum(len(d["observations"]) for d in docs),
                  "note": "Mod IDs include libraries; researched does not mean complete knowledge or runtime verification."}
    elif args.mode == "audit":
        result = validation
    print(json.dumps(result, indent=2, ensure_ascii=False, sort_keys=True))
    return 1 if args.mode == "audit" and not result["valid_structure"] else 0


if __name__ == "__main__":
    raise SystemExit(main())

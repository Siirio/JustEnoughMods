#!/usr/bin/env python3
"""Shared model for JEM Atlas change records, effective behavior and impact lookup."""
from __future__ import annotations

import fnmatch
import hashlib
import json
import subprocess
from collections import defaultdict
from functools import lru_cache
from pathlib import Path, PurePosixPath
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"
CHANGE_DIR = CATALOG / "changes" / "records"
EFFECTIVE_INDEX = CATALOG / "effective" / "index.json"
BASELINE_COVERAGE = CATALOG / "effective" / "baseline-coverage.json"
REGISTRY_NAMES = {"items", "blocks", "entities", "effects", "enchantments", "biomes", "structures", "creative_tabs"}
SELECTOR_TYPES = {"id", "namespace", "registry", "tag", "category", "source_set"}
SPECIFICITY = {"registry": 10, "namespace": 20, "category": 30, "tag": 40, "source_set": 50, "id": 60}


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=False) + "\n", encoding="utf-8")


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def iter_envelopes(pattern: str) -> Iterable[tuple[Path, dict[str, Any]]]:
    for path in sorted(CATALOG.glob(pattern)):
        try:
            payload = read_json(path)
        except (OSError, json.JSONDecodeError):
            continue
        for row in payload.get("records", []):
            yield path, row


def canonical_records() -> dict[str, dict[str, dict[str, Any]]]:
    result: dict[str, dict[str, dict[str, Any]]] = defaultdict(dict)
    for path, row in iter_envelopes("registries/*/*.json"):
        registry = path.parent.name
        registry_id = row.get("registry_id")
        if registry_id:
            result[registry][registry_id] = row
    return dict(result)


def change_records() -> list[tuple[Path, dict[str, Any]]]:
    records: list[tuple[Path, dict[str, Any]]] = []
    if not CHANGE_DIR.exists():
        return records
    for path in sorted(CHANGE_DIR.glob("*.json")):
        records.append((path, read_json(path)))
    return records


def _source_set_members(selector: dict[str, Any]) -> dict[str, dict[str, Any]]:
    path = ROOT / selector.get("members_path", "")
    if not path.is_file():
        return {}
    stamp = path.stat()
    return _source_set_snapshot(path, stamp.st_mtime_ns, stamp.st_size,
                                selector.get("records_field", "records"), selector.get("id_field", "id"))


@lru_cache(maxsize=32)
def _source_set_snapshot(path: Path, modified_ns: int, size: int,
                         records_field: str, id_field: str) -> dict[str, dict[str, Any]]:
    # A source-set lookup runs once for every registry object. Reuse the parsed
    # snapshot while its file identity is unchanged; normal writes invalidate it.
    payload = read_json(path)
    rows = payload if isinstance(payload, list) else payload.get(records_field, [])
    return {row[id_field]: row for row in rows if isinstance(row, dict) and row.get(id_field)}


def selector_match(selector: dict[str, Any], registry: str, registry_id: str, row: dict[str, Any]) -> tuple[bool, dict[str, Any] | None]:
    if selector.get("registry") not in (registry, "*"):
        return False, None
    kind = selector.get("type")
    value = selector.get("value", "*")
    matched = False
    details = None
    if kind == "id":
        matched = registry_id == value
    elif kind == "namespace":
        matched = registry_id.partition(":")[0] == value
    elif kind == "registry":
        matched = value == "*"
    elif kind == "tag":
        matched = value in row.get("tags", [])
    elif kind == "category":
        matched = value in row.get("categories", []) or value in row.get("roles", [])
    elif kind == "source_set":
        details = _source_set_members(selector).get(registry_id)
        matched = details is not None
    return matched, details


def rule_match(rule: dict[str, Any], registry: str, registry_id: str, row: dict[str, Any]) -> tuple[bool, int, list[dict[str, Any]]]:
    matches: list[dict[str, Any]] = []
    specificity = 0
    for selector in rule.get("selectors", []):
        matched, details = selector_match(selector, registry, registry_id, row)
        if matched:
            specificity = max(specificity, SPECIFICITY.get(selector.get("type"), 0))
            if details is not None:
                matches.append({"selector": selector, "member": details})
    if not specificity:
        return False, 0, []
    for selector in rule.get("exceptions", []):
        if selector_match(selector, registry, registry_id, row)[0]:
            return False, 0, []
    return True, specificity, matches


def resolved_commit(path: Path, value: str | None) -> str | None:
    if value != "SELF":
        return value
    result = subprocess.run(
        ["git", "log", "-1", "--format=%H", "--", relative(path)],
        cwd=ROOT, capture_output=True, text=True, check=False,
    )
    return result.stdout.strip() or "uncommitted"


def validate_change_records() -> list[str]:
    errors: list[str] = []
    seen: set[str] = set()
    for path, rule in change_records():
        label = relative(path)
        change_id = rule.get("change_id")
        if not change_id or change_id in seen:
            errors.append(f"duplicate/blank change_id: {change_id} ({label})")
        seen.add(change_id)
        for field in ("schema_version", "status", "selectors", "precedence", "change", "cause", "rationale", "introduced", "history", "impacts"):
            if field not in rule:
                errors.append(f"missing {field}: {label}")
        if rule.get("schema_version") != 2:
            errors.append(f"unsupported change schema: {label}")
        selectors = rule.get("selectors", []) + rule.get("exceptions", [])
        for selector in selectors:
            kind = selector.get("type")
            registry = selector.get("registry")
            if kind not in SELECTOR_TYPES:
                errors.append(f"unknown selector type {kind}: {label}")
            if registry not in REGISTRY_NAMES | {"*"}:
                errors.append(f"unknown selector registry {registry}: {label}")
            if kind == "source_set":
                member_path = ROOT / selector.get("members_path", "")
                if not member_path.is_file():
                    errors.append(f"missing selector source_set {selector.get('members_path')}: {label}")
        for source in rule.get("cause", {}).get("source_files", []):
            if not any(ch in source for ch in "*?[") and not (ROOT / source).exists():
                errors.append(f"missing cause source {source}: {label}")
        modifiers = rule.get("change", {}).get("modifiers", [])
        if not modifiers:
            errors.append(f"change has no modifiers: {label}")
        for modifier in modifiers:
            for field in ("property", "operation", "before", "after"):
                if field not in modifier:
                    errors.append(f"modifier missing {field}: {label}")
        if not rule.get("history"):
            errors.append(f"empty history: {label}")
    indexed = {rule.get("change_id"): rule for _, rule in change_records() if rule.get("change_id")}
    for change_id, rule in indexed.items():
        for parent in rule.get("inherits", []):
            if parent not in indexed:
                errors.append(f"missing inherited change {parent}: {change_id}")
    visiting: set[str] = set()
    visited: set[str] = set()
    def visit(change_id: str) -> None:
        if change_id in visiting:
            errors.append(f"change inheritance cycle: {change_id}")
            return
        if change_id in visited:
            return
        visiting.add(change_id)
        for parent in indexed.get(change_id, {}).get("inherits", []):
            visit(parent)
        visiting.remove(change_id)
        visited.add(change_id)
    for change_id in indexed:
        visit(change_id)
    return errors


def records_digest(records: list[tuple[Path, dict[str, Any]]]) -> str:
    digest = hashlib.sha256()
    for path, payload in records:
        digest.update(relative(path).encode())
        digest.update(json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode())
    return digest.hexdigest()


def source_digest(source: str) -> str | None:
    path = ROOT / source
    if not path.is_file():
        return None
    return hashlib.sha256(path.read_bytes()).hexdigest()


def installed_artifact_drift() -> dict[str, list[str]]:
    manifest_path = CATALOG / "manifest.json"
    if not manifest_path.is_file():
        return {"added": [], "removed": [], "changed": [], "unresolved": ["Catalog/manifest.json is missing"]}
    expected = {}
    for artifact in read_json(manifest_path).get("artifacts", []):
        source = artifact.get("source_path", "").replace("\\", "/")
        if source.startswith("Client/ManagedPack/mods/"):
            expected[source] = artifact.get("sha256")
    actual = {relative(path): path for path in (ROOT / "Client" / "ManagedPack" / "mods").glob("*.jar")}
    added = sorted(set(actual) - set(expected))
    removed = sorted(set(expected) - set(actual))
    changed = sorted(path for path in set(actual) & set(expected) if source_digest(path) != expected[path])
    return {"added": added, "removed": removed, "changed": changed, "unresolved": []}


def recipe_impacts(item_id: str, item_tags: Iterable[str] = ()) -> dict[str, Any]:
    consumed_by: list[str] = []
    produced_by: list[str] = []
    downstream: set[str] = set()
    recipes = [recipe for _, recipe in iter_envelopes("graphs/recipes/*.json")]
    item_tags = set(item_tags)
    for recipe in recipes:
        inputs = set(recipe.get("ingredient_groups", [])) | set(recipe.get("expanded_ingredient_ids", []))
        outputs = set(recipe.get("outputs", [])) | set(recipe.get("byproducts", []))
        if item_id in inputs or item_tags.intersection(recipe.get("tag_inputs", [])):
            consumed_by.append(recipe.get("recipe_id"))
            downstream.update(outputs)
        if item_id in outputs:
            produced_by.append(recipe.get("recipe_id"))
    transitive_items = set(downstream)
    transitive_recipes = set(value for value in consumed_by if value)
    frontier = set(downstream)
    while frontier:
        next_frontier: set[str] = set()
        for recipe in recipes:
            inputs = set(recipe.get("ingredient_groups", [])) | set(recipe.get("expanded_ingredient_ids", []))
            if not frontier.intersection(inputs):
                continue
            recipe_id = recipe.get("recipe_id")
            if recipe_id:
                transitive_recipes.add(recipe_id)
            for output in set(recipe.get("outputs", [])) | set(recipe.get("byproducts", [])):
                if output not in transitive_items and output != item_id:
                    transitive_items.add(output)
                    next_frontier.add(output)
        frontier = next_frontier
    item_limit = 500
    recipe_limit = 500
    return {
        "consumed_by_recipe_count": len(consumed_by),
        "consumed_by_recipes": sorted(value for value in consumed_by if value),
        "produced_by_recipe_count": len(produced_by),
        "produced_by_recipes": sorted(value for value in produced_by if value),
        "direct_downstream_items": sorted(downstream),
        "transitive_downstream_item_count": len(transitive_items),
        "transitive_downstream_items": sorted(transitive_items)[:item_limit],
        "transitive_affected_recipe_count": len(transitive_recipes),
        "transitive_affected_recipes": sorted(transitive_recipes)[:recipe_limit],
        "transitive_output_truncated": len(transitive_items) > item_limit or len(transitive_recipes) > recipe_limit,
    }


def effective_for(registry: str, registry_id: str, base: dict[str, Any]) -> dict[str, Any]:
    applied = []
    all_rules = change_records()
    indexed = {rule["change_id"]: (path, rule) for path, rule in all_rules}

    def append_rule(path: Path, rule: dict[str, Any], specificity: int, match_details: list[dict[str, Any]], inherited_by: str | None = None) -> None:
        if any(entry["change_id"] == rule["change_id"] for entry in applied):
            return
        applied.append({
            "change_id": rule["change_id"],
            "summary": rule["change"]["summary"],
            "status": rule["status"],
            "priority": rule["precedence"]["priority"],
            "specificity": specificity,
            "layer": rule["precedence"]["layer"],
            "overrides": rule["precedence"].get("overrides", []),
            "inherits": rule.get("inherits", []),
            "inherited_by": inherited_by,
            "conditions": rule.get("conditions", []),
            "modifiers": rule["change"]["modifiers"],
            "selector_details": match_details,
            "cause": rule["cause"],
            "rationale": rule["rationale"],
            "introduced": {**rule["introduced"], "commit": resolved_commit(path, rule["introduced"].get("commit"))},
            "history": [{**entry, "commit": resolved_commit(path, entry.get("commit"))} for entry in rule["history"]],
            "impacts": rule["impacts"],
            "record": relative(path),
        })

    direct_matches = []
    for path, rule in all_rules:
        if rule.get("status") not in {"active", "conditional"}:
            continue
        matched, specificity, match_details = rule_match(rule, registry, registry_id, base)
        if not matched:
            continue
        append_rule(path, rule, specificity, match_details)
        direct_matches.append(rule["change_id"])
    frontier = list(direct_matches)
    while frontier:
        child_id = frontier.pop()
        child = indexed[child_id][1]
        for parent_id in child.get("inherits", []):
            parent_path, parent = indexed[parent_id]
            if not any(entry["change_id"] == parent_id for entry in applied):
                append_rule(parent_path, parent, 0, [], child_id)
                frontier.append(parent_id)
    applied.sort(key=lambda value: (value["priority"], value["specificity"], value["change_id"]))
    by_property: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for change in applied:
        for modifier in change["modifiers"]:
            by_property[modifier["property"]].append({
                "change_id": change["change_id"], "operation": modifier["operation"],
                "before": modifier["before"], "after": modifier["after"],
                "priority": change["priority"], "specificity": change["specificity"],
            })
    conflicts = []
    overrides = {change["change_id"]: set(change.get("overrides", [])) for change in applied}
    for prop, modifiers in by_property.items():
        buckets: dict[tuple[int, int], list[str]] = defaultdict(list)
        for modifier in modifiers:
            buckets[(modifier["priority"], modifier["specificity"])].append(modifier["change_id"])
        for order, ids in buckets.items():
            unresolved_pairs = [
                (left, right) for index, left in enumerate(ids) for right in ids[index + 1:]
                if right not in overrides.get(left, set()) and left not in overrides.get(right, set())
            ]
            if unresolved_pairs:
                conflicts.append({"property": prop, "priority": order[0], "specificity": order[1], "changes": sorted(ids)})
    result = {
        "original_registry": {"registry": registry, "id": registry_id, "record": base},
        "applied_jem_changes": applied,
        "effective_gameplay_behavior": {
            "resolution_order": "priority ascending, then selector specificity ascending; later modifiers apply last",
            "properties": dict(sorted(by_property.items())),
            "conflicts": conflicts,
            "unresolved": [condition for change in applied for condition in change.get("conditions", []) if condition.get("confidence") == "unresolved"],
        },
    }
    if registry == "items":
        result["impact_analysis"] = recipe_impacts(registry_id, base.get("tags", []))
        result["impact_analysis"]["declared_systems"] = sorted({system for change in applied for system in change["impacts"].get("systems", [])})
    else:
        result["impact_analysis"] = {"declared_systems": sorted({system for change in applied for system in change["impacts"].get("systems", [])})}
    return result


def build_effective_index() -> dict[str, Any]:
    records = change_records()
    canonical = canonical_records()
    summaries = []
    source_fingerprints: dict[str, str | None] = {}
    for path, rule in records:
        potential = 0
        for registry, rows in canonical.items():
            potential += sum(rule_match(rule, registry, registry_id, row)[0] for registry_id, row in rows.items())
        for source in rule.get("cause", {}).get("source_files", []):
            if not any(ch in source for ch in "*?["):
                source_fingerprints[source] = source_digest(source)
        summaries.append({
            "change_id": rule["change_id"], "status": rule["status"], "summary": rule["change"]["summary"],
            "record": relative(path), "selectors": rule["selectors"], "exceptions": rule.get("exceptions", []),
            "potential_static_target_count": potential,
        })
    payload = {
        "schema_version": 2,
        "model": "Original Registry -> Applied JEM Changes -> Effective Gameplay Behavior",
        "change_records_sha256": records_digest(records),
        "precedence": {
            "order": "priority ascending, selector specificity ascending, change_id ascending",
            "specificity": SPECIFICITY,
            "conflict_policy": "Equal-order modifiers of one property are reported; history and base facts are never overwritten.",
        },
        "changes": summaries,
        "source_fingerprints": dict(sorted(source_fingerprints.items())),
    }
    write_json(EFFECTIVE_INDEX, payload)
    return payload


def path_matches(path: str, pattern: str) -> bool:
    return fnmatch.fnmatchcase(path, pattern) or PurePosixPath(path).match(pattern)


def source_coverage_index(sources: Iterable[tuple[str, str]]) -> tuple[dict[tuple[str, ...], set[str]], list[tuple[str, str]]]:
    literal: dict[tuple[str, ...], set[str]] = defaultdict(set)
    wildcard: list[tuple[str, str]] = []
    for change_id, pattern in sources:
        parts = PurePosixPath(pattern).parts
        if parts and not any(character in pattern for character in "*?["):
            literal[parts].add(change_id)
        else:
            wildcard.append((change_id, pattern))
    return dict(literal), wildcard


def covering_source_rules(path: str, index: tuple[dict[tuple[str, ...], set[str]], list[tuple[str, str]]]) -> list[str]:
    literal, wildcard = index
    parts = PurePosixPath(path).parts
    covered: set[str] = set()
    # PurePosixPath.match permits relative literal patterns to match a suffix;
    # absolute patterns include their root part and therefore only match in full.
    for offset in range(len(parts)):
        covered.update(literal.get(parts[offset:], ()))
    covered.update(change_id for change_id, pattern in wildcard if path_matches(path, pattern))
    return sorted(covered)


def changed_paths(base: str | None = None, head: str | None = None) -> list[str]:
    if base:
        target = head or "HEAD"
        command = ["git", "diff", "--name-only", f"{base}...{target}"]
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, check=False)
        return sorted({line.strip().replace("\\", "/") for line in result.stdout.splitlines() if line.strip()})
    result = subprocess.run(["git", "status", "--porcelain=v1", "-uall"], cwd=ROOT, capture_output=True, text=True, check=False)
    paths = set()
    for line in result.stdout.splitlines():
        value = line[3:]
        if " -> " in value:
            value = value.split(" -> ", 1)[1]
        paths.add(value.strip('"').replace("\\", "/"))
    return sorted(paths)


def scan_coverage(base: str | None = None, head: str | None = None) -> dict[str, Any]:
    config = read_json(CATALOG / "changes" / "source-watch.json")
    changed = changed_paths(base, head)
    watched = []
    for path in changed:
        matches = [entry["domain"] for entry in config["watch"] if path_matches(path, entry["pattern"])]
        if matches and not any(path_matches(path, pattern) for pattern in config.get("ignore", [])):
            watched.append({"path": path, "domains": sorted(set(matches))})
    changed_record_paths = {path for path in changed if path.startswith("Catalog/changes/records/") and path.endswith(".json")}
    changed_rules = [(path, rule) for path, rule in change_records() if relative(path) in changed_record_paths]
    source_patterns = []
    for _, rule in changed_rules:
        sources = list(rule.get("cause", {}).get("source_files", []))
        # Deleted sources are historical evidence, never a missing current cause.
        # Existing/new code still needs a current cause in the same changed rule.
        sources.extend(source for event in rule.get("history", [])
                       for source in event.get("source_files", []) if not (ROOT / source).exists())
        source_patterns.extend((rule["change_id"], source) for source in sources)
    source_index = source_coverage_index(source_patterns)
    uncovered = []
    for entry in watched:
        covering = covering_source_rules(entry["path"], source_index)
        if not covering:
            uncovered.append(entry)
        else:
            entry["covered_by"] = sorted(covering)
    artifacts = installed_artifact_drift()
    artifact_changed = any(artifacts[key] for key in ("added", "removed", "changed", "unresolved"))
    return {"changed_paths": changed, "watched_changes": watched, "changed_records": sorted(changed_record_paths), "uncovered": uncovered, "installed_artifact_drift": artifacts, "full_regeneration_required": artifact_changed, "valid": not uncovered and not artifact_changed}


def build_baseline_coverage() -> dict[str, Any]:
    config = read_json(CATALOG / "changes" / "source-watch.json")
    tracked = subprocess.run(
        ["git", "ls-files"], cwd=ROOT, capture_output=True, text=True, check=False,
    ).stdout.splitlines()
    sources = [(rule["change_id"], source) for _, rule in change_records() for source in rule.get("cause", {}).get("source_files", [])]
    source_index = source_coverage_index(sources)
    groups: dict[str, dict[str, Any]] = {}
    totals = {"watched": 0, "covered": 0, "unresolved": 0}
    for raw_path in sorted(set(tracked)):
        path = raw_path.replace("\\", "/")
        domains = sorted({entry["domain"] for entry in config["watch"] if path_matches(path, entry["pattern"])})
        if not domains or any(path_matches(path, pattern) for pattern in config.get("ignore", [])):
            continue
        parts = path.split("/")
        group_name = "/".join(parts[:2]) if parts[0] == "Mods" else "/".join(parts[:3])
        group = groups.setdefault(group_name, {"watched": 0, "covered": 0, "unresolved": 0, "unresolved_paths": []})
        covering = covering_source_rules(path, source_index)
        group["watched"] += 1
        totals["watched"] += 1
        if covering:
            group["covered"] += 1
            totals["covered"] += 1
        else:
            group["unresolved"] += 1
            totals["unresolved"] += 1
            group["unresolved_paths"].append(path)
    payload = {
        "schema_version": 2,
        "scope": "Tracked canonical gameplay-source paths matched by Catalog/changes/source-watch.json",
        "meaning": "Unresolved means no reviewed Atlas change record owns this historical source yet; it does not assert that the file changes gameplay.",
        "change_records_sha256": records_digest(change_records()),
        "totals": totals,
        "groups": dict(sorted(groups.items())),
    }
    write_json(BASELINE_COVERAGE, payload)
    return payload

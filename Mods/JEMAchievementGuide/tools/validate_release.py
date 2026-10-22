from __future__ import annotations

import argparse
import json
import struct
import zipfile
from collections import Counter
from pathlib import Path


FORBIDDEN_JAR_PARTS = (
    "SemanticRuntime",
    "RegistryMatcher",
    "jem_semantic_runtime",
    "semantic_rules.json",
    "jem_advancement_guide.zip",
    "AdvancementBridgeEvents",
    "JemTriggerRuntime",
    "JemExpansionRuntime",
)


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--mods-dir", type=Path, required=True)
    args = parser.parse_args()
    root = args.root.resolve()
    resources = root / "src" / "main" / "resources"
    manifest = load(root / "src" / "main" / "advancements" / "manifest.json")
    status = load(root / "src" / "main" / "advancements" / "implementation_status.json")
    nodes = manifest["nodes"]
    errors: list[str] = []
    warnings: list[str] = []
    checks: list[str] = []

    counts = Counter(node["tab"] for node in nodes)
    expected = manifest["expected_counts"]
    if dict(counts) != expected or len(nodes) != sum(expected.values()):
        errors.append(f"Manifest counts are {dict(counts)}, expected {expected}")
    else:
        checks.append(f"Manifest contains {len(nodes)} curated nodes across all {len(expected)} tabs.")
    ids = [node["advancement_id"] for node in nodes]
    if len(ids) != len(set(ids)):
        errors.append("Manifest contains duplicate advancement IDs.")
    else:
        checks.append("Advancement IDs are unique.")
    id_set = set(ids)
    for node in nodes:
        if node["parent"] and node["parent"].startswith(("jem:", "jem_guide:")) and node["parent"] not in id_set:
            errors.append(f"{node['advancement_id']} references missing parent {node['parent']}")
        for field in ("title_ru", "title_en", "short_ru", "short_en", "exact_ru", "exact_en"):
            if not node[field]:
                errors.append(f"{node['advancement_id']} is missing {field}")

    resource_ids: set[str] = set()
    custom_triggers: set[str] = set()
    impossible: list[str] = []
    for namespace in ("jem_guide", "jem"):
        base = resources / "data" / namespace / "advancements"
        for path in base.rglob("*.json"):
            value = load(path)
            relative = path.relative_to(base).with_suffix("").as_posix()
            resource_ids.add(f"{namespace}:{relative}")
            if not value.get("criteria"):
                errors.append(f"{namespace}:{relative} has no criteria")
                continue
            for criterion in value["criteria"].values():
                trigger = criterion.get("trigger", "")
                if trigger == "minecraft:impossible":
                    impossible.append(f"{namespace}:{relative}")
                if trigger.startswith("jem_advancements:"):
                    custom_triggers.add(trigger)
    if resource_ids != id_set:
        errors.append(f"Generated advancement ID drift: missing={sorted(id_set - resource_ids)}, extra={sorted(resource_ids - id_set)}")
    else:
        checks.append(f"Generated custom advancement resources match all {len(nodes)} manifest IDs exactly.")
    if impossible:
        errors.append(f"Player-facing minecraft:impossible criteria remain: {impossible}")
    else:
        checks.append("No player-facing minecraft:impossible criterion exists.")

    catalog = set(load(resources / "data" / "jem_advancements" / "node_criteria.json")["criteria"])
    allowed = catalog | {"jem_advancements:upstream_advancement"}
    if not custom_triggers <= allowed:
        errors.append(f"Unregistered custom triggers: {sorted(custom_triggers - allowed)}")
    else:
        checks.append("Every referenced custom criterion is registered by the single bootstrap.")

    status_by_id = {entry["advancement_id"]: entry["provider"] for entry in status["nodes"]}
    unresolved = sorted(node_id for node_id, provider in status_by_id.items() if provider.startswith("jem_advancements:node/"))
    if unresolved:
        warnings.append(f"RELEASE BLOCKER: {len(unresolved)} node-specific criteria have no installed-mod caller yet.")
    provider_counts = Counter(status_by_id.values())
    matrix_rows = sum(1 for line in (root / "IMPLEMENTATION_MATRIX.md").read_text(encoding="utf-8").splitlines() if line.startswith("| jem"))
    if matrix_rows != len(nodes):
        errors.append(f"Implementation matrix contains {matrix_rows} advancement rows, expected {len(nodes)}")
    else:
        checks.append("Implementation matrix contains exactly one row for every advancement.")

    native_index = load(root / "src" / "main" / "advancements" / "native-generated-files.json")
    native_nodes = native_index["nodes"]
    native_ids = {node["advancement_id"] for node in native_nodes}
    legendary = {node_id for node_id in native_ids if node_id.startswith("legendary_monsters:")}
    if len(legendary) != 43:
        errors.append(f"Legendary Monsters integration contains {len(legendary)} nodes, expected 43")
    else:
        checks.append("All 43 Legendary Monsters native advancements are integrated.")
    for node in native_nodes:
        generated = load(resources / node["relative_path"])
        if node["proof"] == "DISABLED_IMPOSSIBLE_CRITERION":
            if generated.get("criteria") != {"jem_disabled": {"trigger": "minecraft:impossible"}}:
                errors.append(f"Disabled native advancement is not blocked: {node['advancement_id']}")
            continue
        source_jar = args.mods_dir / node["source_jar"]
        namespace, path = node["advancement_id"].split(":", 1)
        with zipfile.ZipFile(source_jar) as source_archive:
            source = json.loads(source_archive.read(f"data/{namespace}/advancements/{path}.json").decode("utf-8-sig"))
        generated_semantics = {key: value for key, value in generated.items() if key not in {"parent", "display"}}
        source_semantics = {key: value for key, value in source.items() if key not in {"parent", "display"}}
        if generated_semantics != source_semantics:
            errors.append(f"Native criterion drift: {node['advancement_id']}")
    if not any(error.startswith("Native criterion drift") for error in errors):
        checks.append(f"All selected native integrations preserve criteria, requirements, rewards, and conditions; disabled native trees use impossible criteria.")

    forbidden_namespaces = ("luminous", "aquamirae", "aquaculture", "naturalist", "meteor_shower")
    serialized = json.dumps([load(path) for path in resources.rglob("*.json")], ensure_ascii=False).lower()
    stale = sorted(namespace for namespace in forbidden_namespaces if f"{namespace}:" in serialized)
    if stale:
        errors.append(f"Absent-mod namespace references remain: {stale}")
    else:
        checks.append("No removed Luminous, Aquamirae, Aquaculture, Naturalist, or Meteor Shower IDs remain.")

    create_native = [node for node in native_nodes if node["advancement_id"].startswith("create:") and node["proof"] == "NATIVE_CRITERIA_UNCHANGED"]
    if len(create_native) < 1:
        errors.append("Create visible advancement tree was not integrated.")
    else:
        checks.append(f"Complete visible Create advancement tree is integrated under Engineering ({len(create_native)} nodes).")

    with zipfile.ZipFile(args.jar) as archive:
        names = archive.namelist()
        forbidden = [name for name in names if any(part in name for part in FORBIDDEN_JAR_PARTS)]
        if forbidden:
            errors.append(f"Forbidden legacy content in JAR: {forbidden}")
        else:
            checks.append("Release JAR contains no old semantic runtime, bridge, or nested guide ZIP.")
        mods_toml = archive.read("META-INF/mods.toml").decode("utf-8")
        if mods_toml.count("[[mods]]") != 1 or 'modId="jem_advancements"' not in mods_toml:
            errors.append("JAR does not contain exactly one jem_advancements mod definition")
        else:
            checks.append("Release JAR exposes one jem_advancements initialization identity.")
        class_versions = set()
        for name in names:
            if name.endswith(".class"):
                data = archive.read(name)[:8]
                if data[:4] == b"\xca\xfe\xba\xbe":
                    class_versions.add(struct.unpack(">H", data[6:8])[0])
        if class_versions != {61}:
            errors.append(f"Unexpected class-file versions: {sorted(class_versions)}; Java 17 requires 61")
        else:
            checks.append("All compiled classes target Java 17 class-file version 61.")
        test_classes = [name for name in names if name.endswith(("Test.class", "Tests.class"))]
        if test_classes:
            errors.append(f"Test classes were packaged: {test_classes}")
        else:
            checks.append("No test classes are packaged.")

    lines = ["# Validation Report", "", "## Result", ""]
    result = "STRUCTURE PASS / RUNTIME INCOMPLETE" if not errors and unresolved else "FAIL" if errors else "PASS"
    lines.append(result)
    lines.extend(("", "## Passed checks", ""))
    lines.extend(f"- {check}" for check in checks)
    lines.extend(("", "## Errors", ""))
    lines.extend(f"- {error}" for error in errors) if errors else lines.append("- None.")
    lines.extend(("", "## Runtime blockers", ""))
    lines.extend(f"- {warning}" for warning in warnings) if warnings else lines.append("- None.")
    if unresolved:
        lines.extend(("", "### Criteria without authoritative callers", ""))
        lines.extend(f"- `{node_id}`" for node_id in unresolved)
    lines.extend(("", "## Provider summary", ""))
    lines.extend(f"- `{provider}`: {count}" for provider, count in provider_counts.most_common())
    lines.extend(("", "## Manual checks deferred by user", "", "- Minecraft client startup and advancement-screen interaction.", "- Dedicated-server startup.", "- Multiplayer, reconnect, restart, dimension-change, and `/reload` behavior.", "- Positive/negative gameplay checks and memory profiling."))
    args.report.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(result)
    if errors or unresolved:
        raise SystemExit(1)


if __name__ == "__main__":
    main()

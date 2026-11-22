#!/usr/bin/env python3
"""Refresh Atlas artifact provenance when its observed registry evidence is unchanged."""
from __future__ import annotations

import hashlib
import json
import subprocess
import tomllib
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from impact_model import CATALOG, ROOT, read_json, relative, write_json


LANG_PREFIXES = ("item.", "block.", "entity.", "effect.", "enchantment.", "biome.", "itemGroup.")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalized_json(raw: bytes) -> bytes:
    value = json.loads(raw.decode("utf-8-sig"))
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()


def metadata(path: Path) -> tuple[list[dict[str, str]], list[str]]:
    with zipfile.ZipFile(path) as jar:
        parsed = tomllib.loads(jar.read("META-INF/mods.toml").decode("utf-8-sig"))
        mods = [{
            "id": str(row.get("modId", "")).lower(),
            "name": str(row.get("displayName", row.get("modId", ""))),
            "version": str(row.get("version", "unknown")),
            "display_test": str(row.get("displayTest", "unknown")),
        } for row in parsed.get("mods", [])]
        namespaces = sorted({
            name.split("/", 2)[1]
            for name in jar.namelist()
            if name.startswith(("assets/", "data/")) and name.count("/") >= 2
        })
    return mods, namespaces


def evidence_digest(path: Path) -> str:
    digest = hashlib.sha256()
    with zipfile.ZipFile(path) as jar:
        for name in sorted(jar.namelist()):
            if name.endswith("/"):
                continue
            include = False
            filtered: bytes | None = None
            if name.startswith("META-INF/jarjar/"):
                include = True
            elif name.startswith("assets/") and name.endswith(".json"):
                include = "/models/item/" in name or "/blockstates/" in name
                if name.endswith(("/en_us.json", "/ru_ru.json")):
                    raw = json.loads(jar.read(name).decode("utf-8-sig"))
                    filtered = json.dumps(
                        {key: value for key, value in raw.items() if key.startswith(LANG_PREFIXES)},
                        ensure_ascii=False, sort_keys=True, separators=(",", ":"),
                    ).encode()
                    include = True
            elif name.startswith("data/") and name.endswith(".json"):
                include = any(token in name for token in (
                    "/recipes/", "/loot_tables/", "/tags/", "/worldgen/biome/", "/worldgen/structure/",
                ))
            if not include:
                continue
            raw = filtered if filtered is not None else jar.read(name)
            if name.endswith(".json") and filtered is None:
                raw = normalized_json(raw)
            digest.update(name.encode())
            digest.update(hashlib.sha256(raw).digest())
    return digest.hexdigest()


def replace_provenance(value: Any, old_name: str, new_name: str, old_hash: str,
                       new_hash: str, mod_versions: dict[str, str]) -> tuple[Any, bool]:
    changed = False
    if isinstance(value, dict):
        updated = {}
        owns_changed_artifact = str(value.get("source_sha256", "")).lower() == old_hash
        for key, child in value.items():
            if key == "source_sha256" and str(child).lower() == old_hash:
                updated[key], child_changed = new_hash, True
            elif key == "source_jar" and child == old_name:
                updated[key], child_changed = new_name, True
            elif key == "mod_version" and owns_changed_artifact and value.get("mod_id") in mod_versions:
                updated[key], child_changed = mod_versions[value["mod_id"]], child != mod_versions[value["mod_id"]]
            else:
                updated[key], child_changed = replace_provenance(
                    child, old_name, new_name, old_hash, new_hash, mod_versions,
                )
            changed = changed or child_changed
        if value.get("mod", {}).get("id") in mod_versions:
            version = mod_versions[value["mod"]["id"]]
            if updated["mod"].get("version") != version:
                updated["mod"]["version"] = version
                changed = True
        return updated, changed
    if isinstance(value, list):
        updated_list = []
        for child in value:
            updated_child, child_changed = replace_provenance(
                child, old_name, new_name, old_hash, new_hash, mod_versions,
            )
            updated_list.append(updated_child)
            changed = changed or child_changed
        return updated_list, changed
    if isinstance(value, str) and value.startswith(old_name + ":"):
        return new_name + value[len(old_name):], True
    return value, False


def refresh(previous: Path, replacement: Path) -> dict[str, Any]:
    previous = previous.resolve()
    replacement = replacement.resolve()
    old_hash = sha256(previous)
    new_hash = sha256(replacement)
    old_mods, old_namespaces = metadata(previous)
    new_mods, new_namespaces = metadata(replacement)
    if [row["id"] for row in old_mods] != [row["id"] for row in new_mods]:
        raise RuntimeError("artifact delta rejected: mod IDs changed")
    if old_namespaces != new_namespaces:
        raise RuntimeError("artifact delta rejected: resource namespaces changed")
    old_evidence = evidence_digest(previous)
    new_evidence = evidence_digest(replacement)
    if old_evidence != new_evidence:
        raise RuntimeError("artifact delta rejected: Atlas-observed registry evidence changed; run the full generator")

    manifest_path = CATALOG / "manifest.json"
    manifest = read_json(manifest_path)
    matched = [row for row in manifest.get("artifacts", []) if str(row.get("sha256", "")).lower() == old_hash]
    if not matched:
        raise RuntimeError("artifact delta rejected: previous artifact hash is absent from Catalog/manifest.json")
    mod_versions = {row["id"]: row["version"] for row in new_mods}
    runtime_paths = {
        "client": ROOT / "Client" / "ManagedPack" / "mods" / replacement.name,
        "server": ROOT / "Server" / "mods" / replacement.name,
    }
    for row in matched:
        sides = set(row.get("sides", []))
        side = next((candidate for candidate in ("client", "server") if candidate in sides), None)
        if side is None or not runtime_paths[side].is_file() or sha256(runtime_paths[side]) != new_hash:
            raise RuntimeError(f"artifact delta rejected: matching {side or 'unknown'} runtime artifact is missing")
        row.update({
            "filename": replacement.name,
            "sha256": new_hash,
            "mods": new_mods,
            "namespaces": new_namespaces,
            "source_path": relative(runtime_paths[side]).replace("/", "\\"),
        })
    manifest["snapshot_sha"] = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    manifest["generated_utc"] = datetime.now(timezone.utc).isoformat()
    write_json(manifest_path, manifest)

    changed_files = [relative(manifest_path)]
    patterns = ("registries/*/*.json", "views/*/*.json", "graphs/recipes/*.json", "unresolved.json")
    for pattern in patterns:
        for path in sorted(CATALOG.glob(pattern)):
            payload = read_json(path)
            updated, changed = replace_provenance(
                payload, previous.name, replacement.name, old_hash, new_hash, mod_versions,
            )
            if changed:
                write_json(path, updated)
                changed_files.append(relative(path))
    return {
        "mode": "artifact_delta",
        "previous_sha256": old_hash,
        "replacement_sha256": new_hash,
        "evidence_sha256": new_evidence,
        "updated_files": changed_files,
    }

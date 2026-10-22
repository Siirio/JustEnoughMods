#!/usr/bin/env python3
"""Build the deterministic, static-evidence JEM Registry Atlas.

This intentionally does not launch Minecraft.  It inventories the canonical managed
pack, the local dedicated-server artifacts, vanilla's unpacked 1.20.1 archive, pack
data, and JEM source.  Dynamic registrations and stack-dependent semantics which
cannot be proven from those inputs remain explicit unresolved records.
"""

from __future__ import annotations

import csv
import hashlib
import io
import json
import os
import re
import subprocess
import tempfile
import time
import tomllib
import zipfile
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"
CLIENT_MODS = ROOT / "Client" / "ManagedPack" / "mods"
SERVER_MODS = ROOT / "Server" / "mods"
VANILLA = ROOT / "Server" / "libraries" / "net" / "minecraft" / "server" / "1.20.1-20230612.114412" / "server-1.20.1-20230612.114412-unpacked.jar"
SCHEMA = 1
REGISTRY_DIRS = {
    "items": "item",
    "blocks": "block",
    "entities": "entity_type",
    "fluids": "fluid",
    "effects": "mob_effect",
    "enchantments": "enchantment",
    "biomes": "biome",
    "structures": "structure",
    "block_entities": "block_entity_type",
    "creative_tabs": "creative_mode_tab",
}
LANG_PREFIXES = {
    "item": "items", "block": "blocks", "entity": "entities",
    "effect": "effects", "enchantment": "enchantments", "biome": "biomes",
    "itemGroup": "creative_tabs",
}
# Tag resource directories carry registry identity independently of the member ID.
# Both singular and plural forms below are present in the installed artifact evidence.
TAG_REGISTRY_DIRS = {
    "items": ("items/", "item/"),
    "blocks": ("blocks/", "block/"),
    "entities": ("entity_types/", "entity_type/"),
    "fluids": ("fluids/",),
    "effects": ("mob_effect/", "mob_effects/"),
    "enchantments": ("enchantment/",),
    "biomes": ("worldgen/biome/", "biomes/"),
    "structures": ("worldgen/structure/",),
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_json_bytes(raw: bytes) -> Any | None:
    try:
        return json.loads(raw.decode("utf-8-sig"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return None


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(value, ensure_ascii=False, indent=2, sort_keys=False) + "\n"
    descriptor, pending = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(payload)
        deadline = time.monotonic() + 2.0
        while True:
            try:
                os.replace(pending, path)
                break
            except PermissionError as failure:
                # Windows readers can briefly deny replacement without delete
                # sharing. The identical operation succeeds after their handle
                # closes; permanent denials still fail this generation.
                if getattr(failure, "winerror", None) not in {5, 32, 33} or time.monotonic() >= deadline:
                    raise
                time.sleep(0.05)
    finally:
        if os.path.exists(pending):
            os.unlink(pending)


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def resource_id(value: str) -> str | None:
    value = value.lower()
    return value if re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", value) else None


def flatten_ids(value: Any) -> set[str]:
    found: set[str] = set()
    if isinstance(value, str):
        candidate = resource_id(value)
        if candidate:
            found.add(candidate)
    elif isinstance(value, list):
        for child in value:
            found.update(flatten_ids(child))
    elif isinstance(value, dict):
        for child in value.values():
            found.update(flatten_ids(child))
    return found


@dataclass
class Artifact:
    path: Path
    sides: set[str] = field(default_factory=set)
    digest: str = ""
    mods: list[dict[str, Any]] = field(default_factory=list)
    namespaces: set[str] = field(default_factory=set)
    payload: bytes | None = None
    nested_dependencies: list[dict[str, Any]] = field(default_factory=list)

    def open(self) -> zipfile.ZipFile:
        return zipfile.ZipFile(io.BytesIO(self.payload) if self.payload is not None else self.path)

    @property
    def source_name(self) -> str:
        return self.path.name


class Atlas:
    def __init__(self) -> None:
        self.head = git("rev-parse", "HEAD")
        self.artifacts: dict[str, Artifact] = {}
        self.owners: dict[str, dict[str, Any]] = {
            "minecraft": {"id": "minecraft", "name": "Minecraft", "version": "1.20.1", "source_jar": VANILLA.name, "sha256": sha256(VANILLA)}
        }
        self.registries: dict[str, dict[str, dict[str, Any]]] = {name: {} for name in REGISTRY_DIRS}
        self.translations: dict[str, dict[str, dict[str, str]]] = defaultdict(lambda: defaultdict(dict))
        self.recipes: dict[str, dict[str, Any]] = {}
        self.loot: dict[str, dict[str, Any]] = {}
        self.tags: dict[str, set[str]] = defaultdict(set)
        self.evidence: dict[tuple[str, str], set[str]] = defaultdict(set)
        self.unresolved: list[dict[str, Any]] = []
        self.food_policies: dict[str, dict[str, Any]] = {}
        self.trade_policies: dict[str, dict[str, Any]] = {}
        self._nested_by_digest: dict[str, Artifact] = {}

    def inventory(self) -> None:
        paths: dict[Path, set[str]] = defaultdict(set)
        for side, directory in (("client", CLIENT_MODS), ("server", SERVER_MODS)):
            for path in directory.glob("*.jar"):
                paths[path.resolve()].add(side)
            for path in directory.glob("*.jar.disabled"):
                paths[path.resolve()].add(f"{side}_disabled")
        paths[VANILLA.resolve()].update(("client", "server"))
        for path, sides in sorted(paths.items(), key=lambda row: row[0].name.lower()):
            artifact = Artifact(path=path, sides=sides, digest=sha256(path))
            self._read_metadata(artifact)
            self.artifacts[str(path)] = artifact
            self._nested_by_digest.setdefault(artifact.digest, artifact)
            self._register_owners(artifact)
        # Read declared Forge Jar-in-Jar dependencies, including dependencies of dependencies.
        # Identical embedded bytes are one inspected artifact even with multiple consumers/sides.
        for artifact in list(self.artifacts.values()):
            self._inventory_nested(artifact, 0, set())

    def _register_owners(self, artifact: Artifact) -> None:
        for mod in artifact.mods:
            current = self.owners.get(mod["id"])
            owner = {**mod, "source_jar": artifact.source_name, "sha256": artifact.digest}
            if current and current.get("sha256") != artifact.digest:
                self.unresolved.append({"type": "duplicate_mod_id", "mod_id": mod["id"], "artifacts": [current.get("source_jar"), artifact.source_name]})
            else:
                self.owners[mod["id"]] = owner

    def _inventory_nested(self, artifact: Artifact, depth: int, ancestors: set[str]) -> None:
        if depth >= 16 or artifact.digest in ancestors:
            self.unresolved.append({"type": "nested_dependency_limit", "artifact": artifact.source_name})
            return
        try:
            with artifact.open() as jar:
                if "META-INF/jarjar/metadata.json" not in jar.namelist():
                    return
                metadata = json.loads(jar.read("META-INF/jarjar/metadata.json"))
                if not isinstance(metadata, dict) or not isinstance(metadata.get("jars", []), list):
                    raise ValueError("Invalid Jar-in-Jar metadata root")
                for dependency in metadata.get("jars", []):
                    if not isinstance(dependency, dict):
                        raise ValueError("Invalid Jar-in-Jar dependency")
                    name = dependency.get("path", "")
                    if not isinstance(name, str) or not name.endswith(".jar") or name not in jar.namelist():
                        self.unresolved.append({"type": "nested_dependency_missing", "artifact": artifact.source_name, "path": name})
                        continue
                    raw = jar.read(name)
                    digest = hashlib.sha256(raw).hexdigest()
                    nested = self._nested_by_digest.get(digest)
                    if nested is None:
                        nested = Artifact(Path(Path(name).name), set(artifact.sides), digest, payload=raw)
                        self._nested_by_digest[digest] = nested
                        self._read_metadata(nested)
                        self.artifacts[f"nested:{digest}"] = nested
                        self._register_owners(nested)
                    else:
                        nested.sides.update(artifact.sides)
                    relation = {"path": name, "sha256": digest, "identifier": dependency.get("identifier", {}), "version": dependency.get("version", {}), "artifact": nested}
                    if not any(row["path"] == name and row["sha256"] == digest for row in artifact.nested_dependencies):
                        artifact.nested_dependencies.append(relation)
                    self._inventory_nested(nested, depth + 1, ancestors | {artifact.digest})
        except (zipfile.BadZipFile, KeyError, ValueError, TypeError) as exc:
            self.unresolved.append({"type": "nested_dependency_metadata", "artifact": artifact.source_name, "reason": type(exc).__name__})

    def _read_metadata(self, artifact: Artifact) -> None:
        if artifact.path == VANILLA.resolve():
            artifact.mods = [{"id": "minecraft", "name": "Minecraft", "version": "1.20.1", "display_test": "MATCH_VERSION"}]
            artifact.namespaces.add("minecraft")
            return
        try:
            with artifact.open() as jar:
                artifact.namespaces.update({n.split("/", 2)[1] for n in jar.namelist() if n.startswith(("assets/", "data/")) and n.count("/") >= 2})
                if artifact.payload is not None and "META-INF/mods.toml" not in jar.namelist():
                    return  # Declared non-mod libraries have no invented mod identity.
                raw = jar.read("META-INF/mods.toml")
                data = tomllib.loads(raw.decode("utf-8-sig"))
                for row in data.get("mods", []):
                    mod_id = str(row.get("modId", "")).lower()
                    if not re.fullmatch(r"[a-z0-9_.-]+", mod_id):
                        continue
                    artifact.mods.append({
                        "id": mod_id,
                        "name": str(row.get("displayName", mod_id)),
                        "version": str(row.get("version", "unknown")),
                        "display_test": str(row.get("displayTest", "unknown")),
                    })
                artifact.namespaces.update({n.split("/", 2)[1] for n in jar.namelist() if n.startswith(("assets/", "data/")) and n.count("/") >= 2})
        except (KeyError, zipfile.BadZipFile, tomllib.TOMLDecodeError, UnicodeDecodeError) as exc:
            self.unresolved.append({"type": "artifact_metadata", "artifact": artifact.path.name, "reason": type(exc).__name__})
        if not artifact.mods:
            artifact.mods.append({"id": artifact.path.stem.lower().replace(" ", "_")[:64], "name": artifact.path.stem, "version": "unresolved", "display_test": "unknown"})

    def scan(self) -> None:
        for artifact in self.artifacts.values():
            if any(side.endswith("_disabled") for side in artifact.sides) and not ({"client", "server"} & artifact.sides):
                continue
            try:
                with artifact.open() as jar:
                    names = sorted(jar.namelist())
                    for name in names:
                        if name.endswith("/"):
                            continue
                        self._scan_entry(artifact, jar, name)
            except zipfile.BadZipFile:
                continue
        self._scan_sources()
        self._load_overlays()

    def _owner_for(self, artifact: Artifact, namespace: str) -> dict[str, Any]:
        if namespace in self.owners:
            return self.owners[namespace]
        if len(artifact.mods) == 1:
            return {**artifact.mods[0], "source_jar": artifact.source_name, "sha256": artifact.digest}
        return {"id": namespace, "name": namespace, "version": "unresolved", "source_jar": artifact.source_name, "sha256": artifact.digest}

    def _observe(self, registry: str, registry_id: str, artifact: Artifact, evidence: str) -> None:
        if registry_id.count(":") != 1:
            return
        namespace = registry_id.split(":", 1)[0]
        owner = self._owner_for(artifact, namespace)
        concrete_owner = "/models/" in evidence or "/blockstates/" in evidence or "src/main" in evidence
        if concrete_owner and len(artifact.mods) == 1:
            # A relocated registration may retain another module's legacy namespace.
            # The artifact/source containing the registration owns it today.
            owner = {**artifact.mods[0]}
            owner.setdefault("source_jar", artifact.source_name)
            owner.setdefault("sha256", artifact.digest)
        row = self.registries[registry].setdefault(registry_id, {
            "registry_id": registry_id,
            "registry_type": REGISTRY_DIRS[registry],
            "schema_version": SCHEMA,
            "mod_id": owner["id"],
            "mod_name": owner["name"],
            "mod_version": owner["version"],
            "source_jar": owner.get("source_jar"),
            "source_sha256": owner.get("sha256"),
            "side_availability": sorted(artifact.sides),
            "observed_in": [],
            "display_name_en_us": None,
            "display_name_ru_ru": None,
            "java_registry_class": None,
            "implementation_class": None,
            "tags": [],
            "is_registered": None,
            "survival_obtainable": None,
            "survival_source_evidence": [],
            "acquisition_class": "unresolved",
            "source_provenance": [],
            "confidence": "inferred",
            "confidence_reason": "Observed in installed static artifact; final Forge registry bootstrap not run",
            "variant_model": {"kind": "registry_id", "stack_dependent": None, "reason": "Static artifact cannot prove all ItemStack variants"},
            "unresolved_reason": "Runtime registration and dynamic properties not observed",
        })
        row["observed_in"] = sorted(set(row["observed_in"]) | {evidence})
        # A shared translation file may mention another mod's items. Concrete model,
        # blockstate or source registration evidence supplies that object's owner.
        if concrete_owner:
            row.update({"mod_id": owner["id"], "mod_name": owner["name"], "mod_version": owner["version"],
                        "source_jar": owner.get("source_jar"), "source_sha256": owner.get("sha256")})
        row["source_provenance"] = sorted(set(row["source_provenance"]) | {f"{artifact.source_name}:{evidence}"})
        self.evidence[(registry, registry_id)].add(evidence)

    def _scan_entry(self, artifact: Artifact, jar: zipfile.ZipFile, name: str) -> None:
        parts = name.split("/")
        if len(parts) >= 4 and parts[0] == "assets" and parts[2] == "models" and parts[3] == "item" and name.endswith(".json"):
            item_id = f"{parts[1]}:{'/'.join(parts[4:])[:-5]}"
            model = read_json_bytes(jar.read(name))
            # A root model with unresolved texture parameters is an authored parent
            # template, not evidence that an item with the model's filename exists.
            # An actual source registration or recipe can independently prove its ID.
            unbound = set()
            if isinstance(model, dict) and not model.get("parent"):
                references = set(re.findall(r'"#([a-z0-9_]+)"', json.dumps(model)))
                unbound = references - set(model.get("textures", {}))
            if unbound:
                self.unresolved.append({"type": "parameterized_item_model", "artifact": artifact.source_name,
                                        "model": name, "unbound_texture_parameters": sorted(unbound),
                                        "reason": "Parent template alone cannot prove an item registration"})
            else:
                self._observe("items", item_id, artifact, name)
        if len(parts) >= 4 and parts[0] == "assets" and parts[2] == "blockstates" and name.endswith(".json"):
            block_id = f"{parts[1]}:{'/'.join(parts[3:])[:-5]}"
            self._observe("blocks", block_id, artifact, name)
        if len(parts) >= 5 and parts[0] == "data" and parts[2:4] == ["worldgen", "biome"] and name.endswith(".json"):
            self._observe("biomes", f"{parts[1]}:{'/'.join(parts[4:])[:-5]}", artifact, name)
        if len(parts) >= 5 and parts[0] == "data" and parts[2:4] == ["worldgen", "structure"] and name.endswith(".json"):
            self._observe("structures", f"{parts[1]}:{'/'.join(parts[4:])[:-5]}", artifact, name)
        if name.endswith(("/en_us.json", "/ru_ru.json")) and name.startswith("assets/"):
            raw = read_json_bytes(jar.read(name))
            if isinstance(raw, dict):
                locale = Path(name).stem
                self._scan_lang(artifact, raw, locale, name)
        if name.startswith("data/") and name.endswith(".json"):
            raw = read_json_bytes(jar.read(name))
            if raw is None:
                return
            if "/recipes/" in name:
                recipe_id = f"{parts[1]}:{name.split('/recipes/', 1)[1][:-5]}"
                self.recipes[recipe_id] = self._recipe_record(recipe_id, artifact, name, raw)
            elif "/loot_tables/" in name:
                loot_id = f"{parts[1]}:{name.split('/loot_tables/', 1)[1][:-5]}"
                self.loot[loot_id] = {"loot_table_id": loot_id, "source": f"{artifact.source_name}:{name}", "referenced_ids": sorted(flatten_ids(raw))}
            elif "/tags/" in name and isinstance(raw, dict):
                tag_id = f"{parts[1]}:{name.split('/tags/', 1)[1][:-5]}"
                for value in raw.get("values", []):
                    if isinstance(value, str):
                        self.tags[tag_id].add(value)

    def _scan_lang(self, artifact: Artifact, data: dict[str, Any], locale: str, evidence: str) -> None:
        for key, value in data.items():
            split = key.split(".", 2)
            if len(split) != 3 or split[0] not in LANG_PREFIXES:
                continue
            namespace, path = split[1], split[2]
            registry = LANG_PREFIXES[split[0]]
            registry_id = f"{namespace}:{path}"
            self._observe(registry, registry_id, artifact, evidence)
            self.translations[registry_id][locale][key] = str(value)

    def _recipe_record(self, recipe_id: str, artifact: Artifact, path: str, raw: Any) -> dict[str, Any]:
        ids = flatten_ids(raw)
        output_ids: set[str] = set()
        if isinstance(raw, dict):
            result = raw.get("result")
            if isinstance(result, str):
                output_ids.add(result)
            elif isinstance(result, dict):
                candidate = result.get("item") or result.get("id")
                if isinstance(candidate, str):
                    output_ids.add(candidate)
        for item_id in output_ids:
            if resource_id(item_id):
                self._observe("items", item_id, artifact, path)
                row = self.registries["items"][item_id]
                row["survival_obtainable"] = True
                row["acquisition_class"] = "normal_survival"
                row["survival_source_evidence"] = sorted(set(row["survival_source_evidence"]) | {recipe_id})
        return {
            "schema_version": SCHEMA,
            "recipe_id": recipe_id,
            "mod_id": recipe_id.split(":", 1)[0],
            "mod_name": self._owner_for(artifact, recipe_id.split(":", 1)[0])["name"],
            "mod_version": self._owner_for(artifact, recipe_id.split(":", 1)[0])["version"],
            "recipe_type": raw.get("type") if isinstance(raw, dict) else None,
            "machine_id": raw.get("type") if isinstance(raw, dict) else None,
            "ingredient_groups": sorted(ids - output_ids),
            "tag_inputs": sorted(value for value in ids if value.startswith("#")),
            "expanded_ingredient_ids": [],
            "outputs": sorted(output_ids),
            "byproducts": [],
            "conditional_requirements": None,
            "source_provenance": [f"{artifact.source_name}:{path}"],
        }

    def _scan_sources(self) -> None:
        pattern = re.compile(r'\bregister\s*\(\s*"([a-z0-9_./-]+)"\s*,')
        for module in sorted((ROOT / "Mods").iterdir()):
            source = module / "src" / "main"
            if not source.exists():
                continue
            toml = source / "resources" / "META-INF" / "mods.toml"
            mod_id = module.name.lower()
            properties = module / "gradle.properties"
            if properties.is_file():
                for line in properties.read_text(encoding="utf-8-sig").splitlines():
                    key, separator, value = line.partition("=")
                    if separator and key.strip() == "mod_id" and re.fullmatch(r"[a-z0-9_]+", value.strip()):
                        mod_id = value.strip()
            if toml.exists():
                try:
                    # Source templates contain dependency table keys with this
                    # exact Gradle property; parse the same native identity.
                    template = toml.read_text(encoding="utf-8-sig").replace("${mod_id}", mod_id)
                    parsed = tomllib.loads(template)
                    if parsed.get("mods"):
                        mod_id = parsed["mods"][0].get("modId", mod_id)
                except tomllib.TOMLDecodeError:
                    pass
            owner = self.owners.get(mod_id, {"id": mod_id, "name": module.name, "version": "source", "source_jar": None, "sha256": None})
            self.owners.setdefault(mod_id, owner)
            java_sources = {java: java.read_text(encoding="utf-8", errors="ignore")
                            for java in (source / "java").rglob("*.java")} if (source / "java").exists() else {}
            constants = {f"{java.stem}.{match.group(1)}": match.group(2)
                         for java, text in java_sources.items()
                         for match in re.finditer(r'\bstatic\s+final\s+String\s+(\w+)\s*=\s*"([a-z0-9_.-]+)"', text)}
            for java, text in java_sources.items():
                registry = None
                upper = text.upper()
                if not re.search(r'\bDeferredRegister\s*\.\s*create\s*\(', text):
                    continue  # Registry lookups and Set.of(...) strings never register objects.
                if "DEFERREDREGISTER<ITEM" in upper or "FORGEREGISTRIES.ITEMS" in upper:
                    registry = "items"
                elif "DEFERREDREGISTER<BLOCK" in upper or "FORGEREGISTRIES.BLOCKS" in upper:
                    registry = "blocks"
                elif "ENTITYTYPE" in upper and "DEFERREDREGISTER" in upper:
                    registry = "entities"
                elif "FORGEREGISTRIES.ENCHANTMENTS" in upper:
                    registry = "enchantments"
                if not registry:
                    continue
                namespace = mod_id
                declared = re.search(r'DeferredRegister\.create\([^,]+,\s*("[a-z0-9_.-]+"|[\w.]+)\s*\)', text)
                if declared:
                    token = declared.group(1)
                    namespace = token.strip('"') if token.startswith('"') else constants.get(token, mod_id)
                pseudo = Artifact(java, {"source"}, "", [owner], {namespace})
                for match in pattern.finditer(text):
                    self._observe(registry, f"{namespace}:{match.group(1)}", pseudo, str(java.relative_to(ROOT)).replace("\\", "/"))
                dynamic = re.findall(r'\b\w+\.register\s*\(\s*([^"\s][^,;\n]*|"[a-z0-9_./-]+"\s*\+[^,;\n]*),', text)
                if dynamic:
                    self.unresolved.append({"type": "dynamic_source_registration", "registry": registry,
                                            "artifact": owner.get("source_jar"), "source": str(java.relative_to(ROOT)).replace("\\", "/"),
                                            "expressions": sorted(set(value.strip() for value in dynamic)),
                                            "reason": "Nonliteral registration IDs require their enum/helper/runtime evidence; source strings are not promoted as IDs"})

    def _load_food_policies(self) -> None:
        for path in sorted((ROOT / "Mods" / "JEMSettlements" / "src" / "main" / "resources" / "data" / "jem_settlements" / "happiness" / "food_values").glob("*.json")):
            raw = json.loads(path.read_text(encoding="utf-8"))
            for row in raw:
                self.food_policies[row["item"]] = row

    def _load_overlays(self) -> None:
        self._load_food_policies()
        policy_path = ROOT / "Mods" / "JEMEconomy" / "src" / "main" / "resources" / "data" / "jem_economy" / "economy" / "item_policies.json"
        if policy_path.exists():
            for row in json.loads(policy_path.read_text(encoding="utf-8")):
                self.trade_policies[row["item"]] = row
        for registry, records in self.registries.items():
            for registry_id, row in records.items():
                translations = self.translations.get(registry_id, {})
                row["display_name_en_us"] = next(iter(translations.get("en_us", {}).values()), None)
                row["display_name_ru_ru"] = next(iter(translations.get("ru_ru", {}).values()), None)
                row["tags"] = sorted(tag for tag, values in self.tags.items()
                                     if registry_id in values and any(tag.partition(":")[2].startswith(prefix)
                                                                     for prefix in TAG_REGISTRY_DIRS.get(registry, ())))
                if row["observed_in"]:
                    row["is_registered"] = True if any("src/main" in x for x in row["observed_in"]) else None
                if registry == "items":
                    row.update({
                        "categories": [], "max_stack_size": None, "durability": None,
                        "is_damageable": None, "rarity": None, "fire_resistant": None,
                        "equipment_slots": [], "enchantability": None, "crafting_remainder": None,
                        "repair_ingredients": [], "block_item_id": registry_id if registry_id in self.registries["blocks"] else None,
                        "acquisition_routes": row.pop("survival_source_evidence"), "uses": [],
                        "safe_canonical_stack": None, "trade_policy_ref": f"trade:{registry_id}",
                    })

    def common_view(self, item_id: str, registry: str = "items") -> dict[str, Any]:
        row = self.registries[registry][item_id]
        keys = ("schema_version", "mod_id", "mod_name", "mod_version", "source_jar", "source_sha256", "side_availability", "observed_in", "display_name_en_us", "display_name_ru_ru", "source_provenance", "confidence", "confidence_reason", "unresolved_reason")
        return {key: row.get(key) for key in keys}

    def generate_views(self) -> dict[str, dict[str, list[dict[str, Any]]]]:
        views: dict[str, dict[str, list[dict[str, Any]]]] = defaultdict(lambda: defaultdict(list))
        for item_id, row in self.registries["items"].items():
            path = item_id.split(":", 1)[1]
            lower_name = (row.get("display_name_en_us") or path).lower()
            base = {"item_id": item_id, "registry_type": "item_view", **self.common_view(item_id)}
            if item_id in self.food_policies:
                policy = self.food_policies[item_id]
                mood = int(policy["mood"])
                views["foods"][row["mod_id"]].append({**base, "edible_by_player": True, "edible_by_registered_villager": True, "food_properties_source": "installed FoodProperties + JEM reviewed override", "nutrition_hunger_points": None, "saturation_modifier": None, "estimated_saturation_restored": None, "always_edible": None, "use_duration_ticks": None, "consumed_count": 1, "remaining_item_id": None, "is_meat": None, "safety_class": "unsafe" if mood < 0 else "reviewed", "effects_on_consume": [], "direct_health_restored": None, "mana_or_other_stats": None, "villager_mood_base_points": mood, "villager_satiety_base_points": None, "food_group": policy.get("group"), "mood_repeat_penalty": "ResidentMood policy", "mood_daily_caps": "ResidentMood policy", "cooldown_ticks": 12000, "context_dependent": False, "variant_requirements": None})
            categories: list[tuple[str, dict[str, Any]]] = []
            if any(token in path for token in ("sword", "dagger", "spear", "hammer", "bow", "crossbow", "staff", "spellbook")):
                categories.append(("weapons", {"weapon_archetypes": [next((token for token in ("sword", "dagger", "spear", "hammer", "crossbow", "bow", "staff", "spellbook") if token in path), "other")], "category_evidence": "registry path; requires runtime class confirmation"}))
            if any(path.endswith(token) for token in ("_helmet", "_chestplate", "_leggings", "_boots")):
                categories.append(("armor", {"armor_slot": next(token[1:] for token in ("_helmet", "_chestplate", "_leggings", "_boots") if path.endswith(token)), "category_evidence": "registry path; requires runtime class confirmation"}))
            if any(token in path for token in ("pickaxe", "shovel", "_hoe", "_axe", "drill")):
                categories.append(("tools", {"tool_class": next(token.strip("_") for token in ("pickaxe", "shovel", "_hoe", "_axe", "drill") if token in path), "category_evidence": "registry path; requires runtime class confirmation"}))
            for view, extra in categories:
                views[view][row["mod_id"]].append({**base, **extra})
            if item_id in self.registries["blocks"]:
                views["placeables"][row["mod_id"]].append({**base, "source_item_id": item_id, "placed_registry_ids": [item_id], "place_action": "BlockItem inferred by matching installed item/block IDs", "placement_conditions": None, "dimensions": None, "blockstate_or_entity_variants": None, "consumed_on_place": True, "break_drops": None, "permissions": "native Forge placement checks", "acquisition_routes": row.get("acquisition_routes", [])})
            policy = self.trade_policies.get(item_id)
            outcome = policy.get("outcome") if policy else "NONE"
            views["trade_policies"][row["mod_id"]].append({**base, "trade_outcome": outcome, "reason": policy.get("notes") if policy else "unreviewed_static_registry_entry", "progression_protected": outcome == "NONE" and bool(policy), "protection_source_ids": [], "independent_ordinary_routes": row.get("acquisition_routes", []), "recipe_dependency_evidence": [], "buy_copper": policy.get("buy_copper") if policy else None, "sell_copper": policy.get("sell_copper") if policy else None, "buy_denominator": policy.get("buy_denominator", 1) if policy else None, "sell_denominator": policy.get("sell_denominator", 1) if policy else None, "quota_family": policy.get("quota_family", item_id) if policy else None, "quota_units": policy.get("quota_units", 1) if policy else None, "bulk_1024": policy.get("bulk_1024") if policy else None, "price_rounding": "buy=ceil(full order); sell=floor(full sale plus ledger-backed account residue)", "daily_player_units": policy.get("daily_player_units", 0) if policy else 0, "daily_global_units": policy.get("daily_global_units", 0) if policy else 0, "demand_group": policy.get("demand_group", "") if policy else "", "price_basis": policy.get("notes") if policy else None, "arbitrage_risk": "reviewed" if policy else "unresolved", "review_status": "reviewed" if policy else "unresolved"})
        for entity_id, row in self.registries["entities"].items():
            base = {"entity_id": entity_id, "registry_type": "entity_view", **self.common_view(entity_id, "entities")}
            views["mobs"][row["mod_id"]].append({**base, "roles": [], "living": None, "attitude": None, "default_health": None, "default_attributes": None, "natural_spawn": None, "biomes": [], "dimensions": [], "structure_spawns": [], "summoned_by_boss_ids": [], "related_boss_ids": [], "relationship_evidence": [], "boss_campaign_role": None, "taming_method": None, "owner_mechanism": None, "rideable": None, "ride_control": None, "breeding": None, "despawn_behavior": None, "drops": [], "mount_eligibility": "unresolved"})
        return views

    def emit(self) -> None:
        generated = datetime.now(timezone.utc).isoformat()
        partitions: set[Path] = set()

        def write_partition(path: Path, payload: dict[str, Any]) -> None:
            write_json(path, payload)
            partitions.add(path)

        for registry, records in self.registries.items():
            grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
            for row in records.values():
                grouped[row["mod_id"]].append(row)
            for mod_id, rows in grouped.items():
                write_partition(CATALOG / "registries" / registry / f"{mod_id}.json", self._envelope(registry, mod_id, rows, generated))
        views = self.generate_views()
        for view in ("mobs", "mountables", "bosses", "boss_allies", "placeables", "foods", "weapons", "armor", "tools", "accessories", "containers", "villager_variants", "worldgen", "trade_policies"):
            groups = views.get(view, {})
            for mod_id, rows in groups.items():
                write_partition(CATALOG / "views" / view / f"{mod_id}.json", self._envelope(view, mod_id, rows, generated))
        recipes_by_mod: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for row in self.recipes.values():
            recipes_by_mod[row["mod_id"]].append(row)
        for mod_id, rows in recipes_by_mod.items():
            write_partition(CATALOG / "graphs" / "recipes" / f"{mod_id}.json", self._envelope("recipes", mod_id, rows, generated))
        # These partitions are a snapshot of the current artifact/source inventory.
        # Retaining an absent namespace would make retired mods appear installed.
        for pattern in ("registries/*/*.json", "views/*/*.json", "graphs/recipes/*.json"):
            for path in CATALOG.glob(pattern):
                if path not in partitions:
                    previous = json.loads(path.read_text(encoding="utf-8"))
                    if previous.get("generation", {}).get("generator") == "Catalog/generator/build_atlas.py":
                        path.unlink()
        write_json(CATALOG / "graphs" / "tag_membership.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "tags": {key: sorted(values) for key, values in sorted(self.tags.items())}})
        write_json(CATALOG / "graphs" / "progression.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "status": "source overlays required", "protected_endpoints": sorted(item for item, p in self.trade_policies.items() if p.get("outcome") == "NONE")})
        write_json(CATALOG / "graphs" / "item_lineage.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "status": "recipe edges generated; exclusive progression lineage requires reviewed overlay", "recipe_count": len(self.recipes)})
        write_json(CATALOG / "manifest.json", self._manifest(generated))
        write_json(CATALOG / "coverage.json", self._coverage(views, generated))
        self.unresolved.extend([
            {"type": "runtime_registry_boundary", "priority": "high", "reason": "This generator consumes static artifacts; separate runtime captures are not imported into this snapshot and cannot prove dynamic registrations or absence here."},
            {"type": "dynamic_item_semantics", "priority": "high", "reason": "ItemStack-dependent max stack, food, attributes, capabilities, NBT variants and Java-only acquisition need a registry bootstrap or bytecode-specific overlay."},
            {"type": "entity_semantics", "priority": "high", "reason": "Living status, AI roles, attributes, tame/ride contracts and boss relationships are not inferred from display names."},
            {"type": "villager_resourcepacks", "priority": "medium", "reason": "Canonical Client/ManagedPack has no resourcepacks directory entries to audit; private active-profile packs are diagnostic-only and were not copied."},
        ])
        # Source-only registrations intentionally retain a null artifact owner.
        # Order unknown evidence explicitly without inventing an installed JAR.
        unresolved_order = lambda row: (row.get("priority", ""), row.get("type", ""),
                                        row.get("artifact") is None, row.get("artifact") or "", row.get("source", ""))
        write_json(CATALOG / "unresolved.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "entries": sorted(self.unresolved, key=unresolved_order)})
        trade_rows = [row for groups in views["trade_policies"].values() for row in groups]
        self._emit_economy_docs(trade_rows, generated)
        write_json(CATALOG / "reports" / "trade-policy-audit.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "registered_static_items": len(self.registries["items"]), "reviewed": sum(row["review_status"] == "reviewed" for row in trade_rows), "buy_and_sell": sum(row["trade_outcome"] == "BUY_AND_SELL" for row in trade_rows), "buy_only": sum(row["trade_outcome"] == "BUY_ONLY" for row in trade_rows), "sell_only": sum(row["trade_outcome"] == "SELL_ONLY" for row in trade_rows), "none": sum(row["trade_outcome"] == "NONE" for row in trade_rows), "unresolved": sum(row["review_status"] == "unresolved" for row in trade_rows), "denominator_note": "Observed canonical registry IDs from static artifacts; not JEI variants and not a runtime registry dump."})
        write_json(CATALOG / "reports" / "registry-discrepancies.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "entries": [row for row in self.unresolved if row.get("type") in {"duplicate_mod_id", "artifact_metadata"}]})
        write_json(CATALOG / "reports" / "category-overlap.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "note": "Views intentionally overlap; canonical definitions remain registries/items and registries/entities."})
        write_json(CATALOG / "reports" / "compatibility.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "client_artifacts": sum("client" in a.sides for a in self.artifacts.values()), "server_artifacts": sum("server" in a.sides for a in self.artifacts.values())})

    def _emit_economy_docs(self, rows: list[dict[str, Any]], generated: str) -> None:
        docs = ROOT / "Docs" / "economy"
        docs.mkdir(parents=True, exist_ok=True)
        fields = ["item_id", "mod_id", "survival_obtainable", "trade_status", "reason", "acquisition_evidence",
                  "recipe_dependency_evidence", "protected_gate", "independent_uses", "sell_copper", "buy_copper",
                  "daily_player_units", "daily_global_units", "demand_group", "review_status"]
        with (docs / "registry-coverage.csv").open("w", newline="", encoding="utf-8") as handle:
            writer = csv.DictWriter(handle, fieldnames=fields)
            writer.writeheader()
            for row in sorted(rows, key=lambda value: value["item_id"]):
                routes = row.get("independent_ordinary_routes", [])
                writer.writerow({
                    "item_id": row["item_id"], "mod_id": row["mod_id"],
                    "survival_obtainable": "verified" if row["review_status"] == "reviewed" else "unresolved",
                    "trade_status": row["trade_outcome"], "reason": row["reason"],
                    "acquisition_evidence": "|".join(routes),
                    "recipe_dependency_evidence": "|".join(row.get("recipe_dependency_evidence", [])),
                    "protected_gate": "true" if row.get("progression_protected") else "false",
                    "independent_uses": "|".join(routes), "sell_copper": row.get("sell_copper"),
                    "buy_copper": row.get("buy_copper"), "daily_player_units": row.get("daily_player_units", 0),
                    "daily_global_units": row.get("daily_global_units", 0), "demand_group": row.get("demand_group", ""),
                    "review_status": row["review_status"],
                })
        by_mod: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
        for row in rows:
            bucket = by_mod[row["mod_id"]]
            bucket["registered"] += 1
            bucket["reviewed"] += row["review_status"] == "reviewed"
            bucket["unknown"] += row["review_status"] != "reviewed"
            bucket["buy_and_sell"] += row["trade_outcome"] == "BUY_AND_SELL"
            bucket["none"] += row["trade_outcome"] == "NONE"
        lines = ["# Economy registry coverage", "", f"Generated from Atlas schema {SCHEMA} at `{generated}` for `{self.head}`.", "",
                 f"Static evidence observed {len(rows):,} canonical item IDs. This is not a Forge runtime registry dump or a JEI subtype count; the user's ~19.2k/~19.5k observation therefore has a different denominator. Unreviewed entries remain `NONE` and are not falsely called survival-untradeable.", "",
                 "| Mod | Registered | Reviewed | Unknown | BUY_AND_SELL | NONE |", "| --- | ---: | ---: | ---: | ---: | ---: |"]
        for mod_id, counts in sorted(by_mod.items()):
            lines.append(f"| {mod_id} | {counts['registered']} | {counts['reviewed']} | {counts['unknown']} | {counts['buy_and_sell']} | {counts['none']} |")
        (docs / "coverage-summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
        protected = sorted(row["item_id"] for row in rows if row.get("progression_protected"))
        review = ["# Progression-chain review", "", "The runtime owner is the single `ItemPolicyRegistry`: only `BUY_AND_SELL` supplies both quotes; `NONE` supplies neither. Recipe distance alone never changes policy. Generic diamonds, netherite and ordinary materials remain eligible for explicit review even when they occur in a protected recipe; exclusive boss/rare-mob gates and their genuinely exclusive descendants fail closed.", "", "## Current reviewed protected endpoints", ""]
        review.extend(f"- `{item}`" for item in protected)
        review.extend(["", "## Evidence boundary", "", "`Catalog/graphs/recipes/` preserves static recipe evidence. Exclusive transitive descendants are not inferred automatically because alternative acquisition and ordinary independent uses require human review. Ambiguous entries stay `NONE`/unresolved rather than contaminating generic ancestors or receiving fabricated prices."])
        (docs / "progression-chain-review.md").write_text("\n".join(review) + "\n", encoding="utf-8")

    def _envelope(self, registry: str, mod_id: str, rows: Iterable[dict[str, Any]], generated: str) -> dict[str, Any]:
        owner = self.owners.get(mod_id, {"id": mod_id, "name": mod_id, "version": "unresolved"})
        key = "records"
        return {"schema_version": SCHEMA, "registry_type": registry, "snapshot_sha": self.head, "generation": {"generated_utc": generated, "generator": "Catalog/generator/build_atlas.py", "method": "offline_static_evidence"}, "mod": {"id": mod_id, "name": owner.get("name", mod_id), "version": owner.get("version", "unresolved")}, key: sorted(rows, key=lambda row: row.get("registry_id") or row.get("item_id") or row.get("entity_id") or row.get("recipe_id") or "")}

    def _manifest(self, generated: str) -> dict[str, Any]:
        def nested_rows(artifact: Artifact, ancestors: set[str]) -> list[dict[str, Any]]:
            if artifact.digest in ancestors:
                return []
            return [{"path": row["path"], "sha256": row["sha256"], "identifier": row["identifier"], "version": row["version"],
                     "mods": row["artifact"].mods, "namespaces": sorted(row["artifact"].namespaces),
                     "sides": sorted(row["artifact"].sides), "nested_dependencies": nested_rows(row["artifact"], ancestors | {artifact.digest})}
                    for row in sorted(artifact.nested_dependencies, key=lambda value: value["path"])]
        rows = []
        for artifact in sorted(self.artifacts.values(), key=lambda a: a.path.name.lower()):
            if artifact.payload is not None:
                continue  # Nested evidence lives below the real enclosing file's manifest row.
            rows.append({"filename": artifact.path.name, "sha256": artifact.digest, "sides": sorted(artifact.sides), "enabled": not all(side.endswith("_disabled") for side in artifact.sides), "mods": artifact.mods, "namespaces": sorted(artifact.namespaces), "source_path": str(artifact.path.relative_to(ROOT)) if artifact.path.is_relative_to(ROOT) else str(artifact.path), "nested_dependencies": nested_rows(artifact, set())})
        launcher = (ROOT / "Server/run.bat").read_text(encoding="utf-8").replace("\\", "/")
        forge_launch = re.search(r"forge/1\.20\.1-([^/\s]+)/win_args\.txt", launcher)
        return {"schema_version": SCHEMA, "snapshot_sha": self.head, "generated_utc": generated, "minecraft": "1.20.1", "forge": forge_launch.group(1) if forge_launch else None, "java": "17", "pack_sources": ["Client/ManagedPack", "Server (read-only comparison)", "Mods source", "vanilla unpacked server archive"], "artifacts": rows}

    def _coverage(self, views: dict[str, Any], generated: str) -> dict[str, Any]:
        registries = {}
        for name, records in self.registries.items():
            by_mod: dict[str, int] = defaultdict(int)
            for row in records.values():
                by_mod[row["mod_id"]] += 1
            registries[name] = {"expected_id_count": None, "enumerated_id_count": len(records), "missing_ids": None, "duplicate_ids": 0, "unresolved_mod_owners": sum(row["mod_version"] == "unresolved" for row in records.values()), "method": "installed static artifact evidence", "by_mod": dict(sorted(by_mod.items()))}
        return {"schema_version": SCHEMA, "snapshot_sha": self.head, "generated_utc": generated, "claim": "100% of statically observed canonical IDs; runtime boundary unresolved", "id_coverage": registries, "classification_coverage": {name: sum(len(rows) for rows in groups.values()) for name, groups in sorted(views.items())}, "semantic_field_coverage": {"runtime_verified": 0, "static_inferred": sum(len(rows) for rows in self.registries.values()), "note": "Null is intentional where static sources do not prove semantics."}, "unresolved_counts": {"entries": len(self.unresolved)}}


def main() -> None:
    atlas = Atlas()
    atlas.inventory()
    atlas.scan()
    atlas.emit()
    from impact_model import build_effective_index, validate_change_records
    change_errors = validate_change_records()
    if change_errors:
        raise RuntimeError("Atlas change records are invalid:\n" + "\n".join(change_errors))
    build_effective_index()
    print(json.dumps({"head": atlas.head, "artifacts": len(atlas.artifacts), "registries": {name: len(rows) for name, rows in atlas.registries.items()}, "recipes": len(atlas.recipes), "unresolved": len(atlas.unresolved)}, indent=2))


if __name__ == "__main__":
    main()

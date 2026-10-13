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
import json
import re
import subprocess
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
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, sort_keys=False) + "\n", encoding="utf-8")


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
            for mod in artifact.mods:
                current = self.owners.get(mod["id"])
                owner = {**mod, "source_jar": path.name, "sha256": artifact.digest}
                if current and current.get("sha256") != artifact.digest:
                    self.unresolved.append({"type": "duplicate_mod_id", "mod_id": mod["id"], "artifacts": [current.get("source_jar"), path.name]})
                else:
                    self.owners[mod["id"]] = owner

    def _read_metadata(self, artifact: Artifact) -> None:
        if artifact.path == VANILLA.resolve():
            artifact.mods = [{"id": "minecraft", "name": "Minecraft", "version": "1.20.1", "display_test": "MATCH_VERSION"}]
            artifact.namespaces.add("minecraft")
            return
        try:
            with zipfile.ZipFile(artifact.path) as jar:
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
                with zipfile.ZipFile(artifact.path) as jar:
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
        row["source_provenance"] = sorted(set(row["source_provenance"]) | {f"{artifact.source_name}:{evidence}"})
        self.evidence[(registry, registry_id)].add(evidence)

    def _scan_entry(self, artifact: Artifact, jar: zipfile.ZipFile, name: str) -> None:
        parts = name.split("/")
        if len(parts) >= 4 and parts[0] == "assets" and parts[2] == "models" and parts[3] == "item" and name.endswith(".json"):
            item_id = f"{parts[1]}:{'/'.join(parts[4:])[:-5]}"
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
        pattern = re.compile(r"(?:register|create|of)\(\s*\"([a-z0-9_./-]+)\"")
        for module in sorted((ROOT / "Mods").iterdir()):
            source = module / "src" / "main"
            if not source.exists():
                continue
            toml = source / "resources" / "META-INF" / "mods.toml"
            mod_id = module.name.lower()
            if toml.exists():
                try:
                    parsed = tomllib.loads(toml.read_text(encoding="utf-8"))
                    if parsed.get("mods"):
                        mod_id = parsed["mods"][0].get("modId", mod_id)
                except tomllib.TOMLDecodeError:
                    pass
            owner = self.owners.get(mod_id, {"id": mod_id, "name": module.name, "version": "source", "source_jar": None, "sha256": None})
            self.owners.setdefault(mod_id, owner)
            for java in (source / "java").rglob("*.java") if (source / "java").exists() else []:
                text = java.read_text(encoding="utf-8", errors="ignore")
                registry = None
                upper = text.upper()
                if "DEFERREDREGISTER<ITEM" in upper or "FORGEREGISTRIES.ITEMS" in upper:
                    registry = "items"
                elif "DEFERREDREGISTER<BLOCK" in upper or "FORGEREGISTRIES.BLOCKS" in upper:
                    registry = "blocks"
                elif "ENTITYTYPE" in upper and "DEFERREDREGISTER" in upper:
                    registry = "entities"
                if not registry:
                    continue
                pseudo = Artifact(java, {"source"}, "", [owner], {mod_id})
                for match in pattern.finditer(text):
                    self._observe(registry, f"{mod_id}:{match.group(1)}", pseudo, str(java.relative_to(ROOT)).replace("\\", "/"))

    def _load_overlays(self) -> None:
        for path in (ROOT / "Mods" / "JEMSettlements" / "src" / "main" / "resources" / "data" / "jem_settlements" / "happiness").glob("food_values.json"):
            raw = json.loads(path.read_text(encoding="utf-8"))
            for row in raw:
                self.food_policies[row["item"]] = row
        policy_path = ROOT / "Mods" / "JEMEconomy" / "src" / "main" / "resources" / "data" / "jem_economy" / "economy" / "item_policies.json"
        if policy_path.exists():
            for row in json.loads(policy_path.read_text(encoding="utf-8")):
                self.trade_policies[row["item"]] = row
        for registry, records in self.registries.items():
            for registry_id, row in records.items():
                translations = self.translations.get(registry_id, {})
                row["display_name_en_us"] = next(iter(translations.get("en_us", {}).values()), None)
                row["display_name_ru_ru"] = next(iter(translations.get("ru_ru", {}).values()), None)
                row["tags"] = sorted(tag for tag, values in self.tags.items() if registry_id in values)
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
            views["trade_policies"][row["mod_id"]].append({**base, "trade_outcome": outcome, "reason": policy.get("notes") if policy else "unreviewed_static_registry_entry", "progression_protected": outcome == "NONE" and bool(policy), "protection_source_ids": [], "independent_ordinary_routes": row.get("acquisition_routes", []), "recipe_dependency_evidence": [], "buy_copper": policy.get("buy_copper") if policy else None, "sell_copper": policy.get("sell_copper") if policy else None, "daily_player_units": policy.get("daily_player_units", 0) if policy else 0, "daily_global_units": policy.get("daily_global_units", 0) if policy else 0, "demand_group": policy.get("demand_group", "") if policy else "", "price_basis": policy.get("notes") if policy else None, "arbitrage_risk": "reviewed" if policy else "unresolved", "review_status": "reviewed" if policy else "unresolved"})
        for entity_id, row in self.registries["entities"].items():
            base = {"entity_id": entity_id, "registry_type": "entity_view", **self.common_view(entity_id, "entities")}
            views["mobs"][row["mod_id"]].append({**base, "roles": [], "living": None, "attitude": None, "default_health": None, "default_attributes": None, "natural_spawn": None, "biomes": [], "dimensions": [], "structure_spawns": [], "summoned_by_boss_ids": [], "related_boss_ids": [], "relationship_evidence": [], "boss_campaign_role": None, "taming_method": None, "owner_mechanism": None, "rideable": None, "ride_control": None, "breeding": None, "despawn_behavior": None, "drops": [], "mount_eligibility": "unresolved"})
        return views

    def emit(self) -> None:
        generated = datetime.now(timezone.utc).isoformat()
        for registry, records in self.registries.items():
            grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
            for row in records.values():
                grouped[row["mod_id"]].append(row)
            for mod_id, rows in grouped.items():
                write_json(CATALOG / "registries" / registry / f"{mod_id}.json", self._envelope(registry, mod_id, rows, generated))
        views = self.generate_views()
        for view in ("mobs", "mountables", "bosses", "boss_allies", "placeables", "foods", "weapons", "armor", "tools", "accessories", "containers", "villager_variants", "worldgen", "trade_policies"):
            groups = views.get(view, {})
            for mod_id, rows in groups.items():
                write_json(CATALOG / "views" / view / f"{mod_id}.json", self._envelope(view, mod_id, rows, generated))
        recipes_by_mod: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for row in self.recipes.values():
            recipes_by_mod[row["mod_id"]].append(row)
        for mod_id, rows in recipes_by_mod.items():
            write_json(CATALOG / "graphs" / "recipes" / f"{mod_id}.json", self._envelope("recipes", mod_id, rows, generated))
        write_json(CATALOG / "graphs" / "tag_membership.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "tags": {key: sorted(values) for key, values in sorted(self.tags.items())}})
        write_json(CATALOG / "graphs" / "progression.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "status": "source overlays required", "protected_endpoints": sorted(item for item, p in self.trade_policies.items() if p.get("outcome") == "NONE")})
        write_json(CATALOG / "graphs" / "item_lineage.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "status": "recipe edges generated; exclusive progression lineage requires reviewed overlay", "recipe_count": len(self.recipes)})
        write_json(CATALOG / "manifest.json", self._manifest(generated))
        write_json(CATALOG / "coverage.json", self._coverage(views, generated))
        self.unresolved.extend([
            {"type": "runtime_registry_boundary", "priority": "high", "reason": "No Minecraft/Forge runtime bootstrap was authorized; static evidence cannot prove dynamic registrations or absence."},
            {"type": "dynamic_item_semantics", "priority": "high", "reason": "ItemStack-dependent max stack, food, attributes, capabilities, NBT variants and Java-only acquisition need a registry bootstrap or bytecode-specific overlay."},
            {"type": "entity_semantics", "priority": "high", "reason": "Living status, AI roles, attributes, tame/ride contracts and boss relationships are not inferred from display names."},
            {"type": "villager_resourcepacks", "priority": "medium", "reason": "Canonical Client/ManagedPack has no resourcepacks directory entries to audit; private active-profile packs are diagnostic-only and were not copied."},
        ])
        write_json(CATALOG / "unresolved.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "entries": sorted(self.unresolved, key=lambda row: (row.get("priority", ""), row.get("type", ""), row.get("artifact", "")))})
        trade_rows = [row for groups in views["trade_policies"].values() for row in groups]
        self._emit_economy_docs(trade_rows, generated)
        write_json(CATALOG / "reports" / "trade-policy-audit.json", {"schema_version": SCHEMA, "snapshot_sha": self.head, "registered_static_items": len(self.registries["items"]), "reviewed": sum(row["review_status"] == "reviewed" for row in trade_rows), "buy_and_sell": sum(row["trade_outcome"] == "BUY_AND_SELL" for row in trade_rows), "none": sum(row["trade_outcome"] == "NONE" for row in trade_rows), "unresolved": sum(row["review_status"] == "unresolved" for row in trade_rows), "denominator_note": "Observed canonical registry IDs from static artifacts; not JEI variants and not a runtime registry dump."})
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
        rows = []
        for artifact in sorted(self.artifacts.values(), key=lambda a: a.path.name.lower()):
            rows.append({"filename": artifact.path.name, "sha256": artifact.digest, "sides": sorted(artifact.sides), "enabled": not all(side.endswith("_disabled") for side in artifact.sides), "mods": artifact.mods, "namespaces": sorted(artifact.namespaces), "source_path": str(artifact.path.relative_to(ROOT)) if artifact.path.is_relative_to(ROOT) else str(artifact.path), "nested_dependencies": []})
        return {"schema_version": SCHEMA, "snapshot_sha": self.head, "generated_utc": generated, "minecraft": "1.20.1", "forge": "47.4.16", "java": "17", "pack_sources": ["Client/ManagedPack", "Server (read-only comparison)", "Mods source", "vanilla unpacked server archive"], "artifacts": rows}

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

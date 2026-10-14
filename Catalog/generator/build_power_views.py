#!/usr/bin/env python3
from __future__ import annotations

import ast
import json
import hashlib
import re
import subprocess
import zipfile
from collections import Counter
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"
OUTPUT = CATALOG / "views" / "power"
SCHEMA = 4


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write(name: str, value: Any) -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def git_head() -> str:
    return subprocess.run(["git", "rev-parse", "HEAD"], cwd=ROOT, check=True, capture_output=True, text=True).stdout.strip()


def envelope(kind: str, records: list[dict[str, Any]], snapshot: str) -> dict[str, Any]:
    return {
        "schema_version": SCHEMA,
        "view": kind,
        "snapshot_sha": snapshot,
        "generation": {
            "generator": "Catalog/generator/build_power_views.py",
            "method": "offline derived evidence",
            "deterministic": True
        },
        "records": records
    }


def rows(pattern: str) -> Iterable[tuple[str, dict[str, Any]]]:
    for path in sorted(CATALOG.glob(pattern)):
        data = load(path)
        for row in data.get("records", []):
            yield path.relative_to(ROOT).as_posix(), row


def basic(record_id: str, kind: str, row: dict[str, Any], source_path: str) -> dict[str, Any]:
    acquisition = row.get("acquisition_routes") or []
    original_status = "partial" if row.get("confidence") in {"source_verified", "verified"} else "unresolved"
    return {
        "id": record_id,
        "kind": kind,
        "display_name": row.get("display_name_en_us") or row.get("display_name_ru_ru"),
        "original": {
            "status": original_status,
            "expression": None,
            "known_dimensions": {},
            "unknown_is_zero": False
        },
        "modifiers": [],
        "jem_tweaks": [],
        "effective": {
            "status": "unresolved",
            "expression": "original (partially unresolved) composed with only applicable documented modifiers"
        },
        "acquisition": {
            "status": "partial" if acquisition else "unresolved",
            "routes": acquisition
        },
        "assessment": {
            "status": "unverified",
            "reason": "A registry candidate is accounted for, but static registry evidence does not prove its runtime combat expression."
        },
        "evidence": [{"type": "atlas_static", "source": source_path, "confidence": row.get("confidence", "inferred")}]
    }


def spell_ids() -> list[tuple[str, str]]:
    text = (ROOT / "Docs" / "integrations" / "irons-spell-registry-3.15.6.md").read_text(encoding="utf-8")
    found: list[tuple[str, str]] = []
    for school, values in re.findall(r"^- ([a-z_]+): (.+)$", text, flags=re.MULTILINE):
        for spell in re.findall(r"`([^`]+)`", values):
            found.append((f"irons_spellbooks:{spell}", school))
    return sorted(found)


def irons_spell_names(evidence: dict[str, Any]) -> dict[str, str]:
    jar = ROOT / evidence["artifact"]["canonical_path"]
    with zipfile.ZipFile(jar) as archive:
        language = json.loads(archive.read("assets/irons_spellbooks/lang/en_us.json").decode("utf-8-sig"))
    prefix = "spell.irons_spellbooks."
    return {f"irons_spellbooks:{key[len(prefix):]}": value for key, value in language.items()
            if key.startswith(prefix) and "." not in key[len(prefix):]}


def load_tag_values(path: Path) -> set[str]:
    if not path.exists():
        return set()
    return {
        value if isinstance(value, str) else value.get("id", "")
        for value in load(path).get("values", [])
    }


def registered_item_map(native: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {row["item_id"]: row for row in native["item_registrations"]}


def reward_profiles() -> dict[str, list[dict[str, Any]]]:
    directory = (
        ROOT / "Mods" / "JEMPackCore" / "src" / "main" / "resources" / "data" /
        "jem_world_boss_tiers" / "jem_world_boss_tiers" / "rewards"
    )
    result: dict[str, list[dict[str, Any]]] = {}
    for filename in ("major_rewards.json", "expanded_rewards.json"):
        for reward in load(directory / filename).get("rewards", []):
            if reward.get("item_id"):
                result.setdefault(reward["item_id"], []).append({"source_file": filename, **reward})
    return result


def boss_reward_weapon_evidence() -> dict[str, Any]:
    evidence = load(CATALOG / "power-sources" / "boss-reward-weapons-stage35.json")
    records = evidence.get("records", [])
    ids = [row.get("id") for row in records]
    if len(ids) != len(set(ids)):
        duplicates = sorted(item_id for item_id, count in Counter(ids).items() if count > 1)
        raise SystemExit(f"duplicate Stage 3.5A boss-reward weapon IDs: {duplicates}")
    canonical_ids = {
        item_id
        for item_id, profiles in reward_profiles().items()
        if any("weapon" in str(profile.get("category", "")).lower() for profile in profiles)
    }
    evidence_ids = set(ids)
    if evidence_ids != canonical_ids:
        raise SystemExit(
            "Stage 3.5A roster differs from canonical reward JSON: "
            f"missing={sorted(canonical_ids - evidence_ids)}, extra={sorted(evidence_ids - canonical_ids)}"
        )
    if len(evidence_ids) != 36:
        raise SystemExit(f"Stage 3.5A requires exactly 36 unique weapon records, found {len(evidence_ids)}")
    return evidence


def attach_boss_reward_weapon_evidence(weapons: list[dict[str, Any]], evidence: dict[str, Any]) -> None:
    by_id = {row["id"]: row for row in evidence["records"]}
    weapon_by_id = {row["id"]: row for row in weapons}
    missing = sorted(set(by_id) - set(weapon_by_id))
    if missing:
        raise SystemExit(f"Stage 3.5A records absent from generated weapons view: {missing}")
    for item_id, profile in by_id.items():
        weapon = weapon_by_id[item_id]
        ordinary = profile.get("ordinary", {})
        combo = profile.get("combo", {})
        specials = profile.get("specials", [])
        durability = profile.get("durability", {})
        unresolved = profile.get("unresolved", [])
        weapon["stage_3_5a"] = {
            "status": "static_evidence_exhausted_runtime_gaps_explicit" if unresolved else "static_profile_resolved",
            "owner_class_inspected": profile.get("owner"),
            "ordinary_attack": ordinary,
            "combo": combo,
            "special_abilities": specials,
            "durability": durability,
            "world_tier": {
                "without_progression_enchantment": {str(tier): ordinary.get("damage") for tier in range(1, 6)},
                "with_progression_enchantment": [
                    {
                        "tier": tier,
                        "ordinary_damage": round(float(ordinary["damage"]) * (1.0 if tier == 1 else 1.05 if tier == 2 else 1.10), 6)
                        if isinstance(ordinary.get("damage"), (int, float)) else None,
                        "critical_damage": round(float(ordinary["damage"]) * 1.10 * 1.5, 6)
                        if tier >= 3 and isinstance(ordinary.get("damage"), (int, float)) else None,
                        "critical_probability": 0.15 if tier >= 3 else 0.0,
                    }
                    for tier in range(1, 6)
                ],
                "boundary": evidence["shared_boundaries"]["progression"],
            },
            "effective_combat": {
                "basic_pve_before_mitigation": ordinary.get("damage"),
                "basic_pvp_before_mitigation": ordinary.get("damage"),
                "combo_damage": combo.get("total_damage"),
                "special_burst": next((
                    ability.get("max_burst_damage", ability.get("ideal_overlap_upper_bound", ability.get("direct_center_upper_bound", ability.get("ideal_direct_target_burst"))))
                    for ability in specials
                    if any(key in ability for key in ("max_burst_damage", "ideal_overlap_upper_bound", "direct_center_upper_bound", "ideal_direct_target_burst"))
                ), None),
                "whole_weapon_burst_dps": None,
                "whole_weapon_sustained_dps": {"30s": None, "60s": None, "120s": None},
                "boundary": evidence["shared_boundaries"]["dps"],
            },
            "power_assessment": {
                "boss_reward_suitability": "review_required",
                "verified_balance_issue": durability.get("status") == "verified_unbreakable_balance_issue",
                "confidence": "exact_static_components_with_named_runtime_boundaries",
            },
            "evidence": {
                "installed_artifacts": evidence["artifacts"],
                "effective_configs": ["Client/ManagedPack/config/cataclysm-common.toml", "Client/ManagedPack/config/legendary_monsters-common.toml"],
                "reward_sources": [profile["source_file"] for profile in weapon.get("boss_reward_profiles", [])],
                "owner_class": profile.get("owner"),
                "unresolved_fields": unresolved,
            },
        }
        weapon["assessment"] = weapon["stage_3_5a"]["power_assessment"]


def stage35a_calculations(weapons: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for weapon in weapons:
        profile = weapon.get("stage_3_5a")
        if not profile:
            continue
        damage = profile["ordinary_attack"].get("damage")
        basic_targets = None
        if isinstance(damage, (int, float)):
            basic_targets = {
                target["id"]: round(vanilla_post_mitigation(float(damage), target["armor"], target["toughness"], target["protection_epf"]), 6)
                for target in REFERENCE_TARGETS
            }
        special_windows = []
        for ability in profile["special_abilities"]:
            burst = ability.get("max_burst_damage", ability.get("ideal_overlap_upper_bound", ability.get("direct_center_upper_bound", ability.get("ideal_direct_target_burst"))))
            cooldown = ability.get("cooldown_ticks")
            if not isinstance(burst, (int, float)) or not isinstance(cooldown, int) or cooldown <= 0:
                continue
            totals = {}
            for seconds in (30, 60, 120):
                activations = 1 + max(0, (seconds * 20 - 1) // cooldown)
                total = round(float(burst) * activations, 6)
                totals[f"{seconds}s"] = {"activations": activations, "damage_upper_bound": total, "dps_upper_bound": round(total / seconds, 6)}
            special_windows.append({
                "ability": ability["name"],
                "burst_upper_bound": burst,
                "cooldown_ticks": cooldown,
                "windows": totals,
                "boundary": "Cooldown-limited special-only upper bound; excludes ordinary attacks and any hit-count field still marked runtime-dependent.",
            })
        records.append({
            "id": weapon["id"],
            "kind": "boss_reward_weapon_stage_3_5a_card",
            "ordinary_attack": profile["ordinary_attack"],
            "ordinary_reference_targets": basic_targets,
            "combo": profile["combo"],
            "special_abilities": profile["special_abilities"],
            "special_cooldown_windows": special_windows,
            "world_tier": profile["world_tier"],
            "durability": profile["durability"],
            "resolution": {
                "status": "partial" if profile["evidence"]["unresolved_fields"] else "resolved_static_scope",
                "basic_numeric": isinstance(damage, (int, float)),
                "numeric_special_count": sum("resolved" in str(row.get("status", "")) for row in profile["special_abilities"]),
                "fully_numeric_combo": profile["combo"].get("status") == "fully_numeric",
                "burst_calculation_count": len(special_windows),
                "sustained_window_count": len(special_windows) * 3,
            },
        })
    return sorted(records, key=lambda row: row["id"])


def registry_item_map() -> dict[str, tuple[str, dict[str, Any]]]:
    return {row["registry_id"]: (source, row) for source, row in rows("registries/items/*.json")}


def combat_item_signal(item_id: str, kind: str) -> bool:
    path = item_id.partition(":")[2].lower()
    if item_id.startswith("deep_dark_extended:"):
        return False
    if any(marker in path for marker in (
        "spawn_egg", "_visual_", "_pulling_", "_inventory", "_hand", "blade_half",
        "spearhead_pennant", "bowman_block", "crossbowman_block", "upgrade/", "_charge_", "_invisible_",
    )):
        return False
    tokens = set(filter(None, re.split(r"[/_.-]+", path)))
    if any(token in {"bowl", "bowls", "pennant", "string", "frame", "dust"} for token in tokens):
        return False
    if kind == "weapon_candidate":
        weapon_tokens = {
            "sword", "blade", "spear", "javelin", "dagger", "katana", "scythe", "sickle",
            "claw", "mace", "halberd", "halbert", "greatsword", "claymore", "glaive", "rapier",
            "sabre", "saber", "cutlass", "trident", "shield", "bow", "crossbow", "cannon", "staff",
            "hammer", "axe", "pickaxe", "shovel", "hoe",
        }
        if tokens & weapon_tokens:
            return True
        return bool(re.search(r"(?:dread|long|short|war|great|cross)?bow$", path)) and not path.endswith(("rainbow", "bowlder"))
    armor_tokens = {"helmet", "chestplate", "leggings", "boots", "armor", "armour", "robe", "robes"}
    return bool(tokens & armor_tokens)


def native_item_mechanics(kind: str, item_id: str, native: dict[str, Any]) -> dict[str, Any]:
    fields = (
        ("base_damage", "attack_speed", "epic_fight_moveset", "special_attacks", "effects", "cooldown", "resource_cost", "penetration", "durability")
        if kind == "weapon_candidate"
        else ("armor", "toughness", "knockback_resistance", "damage_type_reductions", "passives", "actives", "cooldown", "durability")
    )
    extracted = next((row for row in native.get("items", []) if row["item_id"] == item_id), None)
    combat_data = native.get("data_driven_combat", {})
    better_combat = next((row for row in combat_data.get("better_combat_weapon_attributes", []) if row["resource_id"] == item_id), None)
    epic_fight = [row for row in combat_data.get("epic_fight_capabilities", []) if row["item_id"] == item_id]
    numeric = extracted.get("exact_numeric_mechanics", {}) if extracted else {}
    dimensions = {
        field: {
            "status": "unresolved",
            "unresolved_reason": f"{field} still requires a safely traced method expression or runtime ItemStack attributes for {item_id}; raw class constants are evidence, not assigned values",
        }
        for field in fields
    }
    if numeric.get("attributes"):
        for attribute in numeric["attributes"]:
            mapping = {
                "attack_damage_addition": "base_damage",
                "attack_speed_addition": "attack_speed",
                "armor_addition": "armor",
                "armor_toughness_addition": "toughness",
                "knockback_resistance_addition": "knockback_resistance",
            }
            field = mapping.get(attribute["attribute"])
            if field in dimensions:
                dimensions[field] = {"status": "exact_owner_bytecode", "value": attribute["value"], "semantic": attribute["attribute"]}
    if numeric.get("durability"):
        dimensions["durability"] = {"status": "exact_owner_bytecode", "values": numeric["durability"]}
    if numeric.get("cooldowns") and "cooldown" in dimensions:
        dimensions["cooldown"] = {"status": "exact_owner_bytecode", "values": numeric["cooldowns"]}
    special_methods = extracted.get("special_attack_methods", []) if extracted else []
    if special_methods and "special_attacks" in dimensions:
        dimensions["special_attacks"] = {
            "status": "exact_method_sinks_formula_partial",
            "values": special_methods,
            "unresolved_reason": "Each native sink is named; only methods with a safely reconstructed expression may be converted into final damage.",
        }
    if better_combat and "epic_fight_moveset" in dimensions:
        dimensions["epic_fight_moveset"] = {
            "status": "inactive_optional_compatibility_data" if not better_combat.get("provider_installed") else "not_applicable_better_combat_present",
            "unresolved_reason": "The owning mod embeds Better Combat weapon_attributes, but Better Combat is not installed and Punchy compatibility is disabled." if not better_combat.get("provider_installed") else "Exact installed data provides a Better Combat weapon attribute profile, not an Epic Fight capability.",
        }
    if epic_fight and "epic_fight_moveset" in dimensions:
        dimensions["epic_fight_moveset"] = {
            "status": "exact_installed_data" if len(epic_fight) == 1 else "installed_resource_collision_precedence_unresolved",
            "values": epic_fight,
            "unresolved_reason": None if len(epic_fight) == 1 else "Multiple installed mod JARs provide an Epic Fight capability for the same item ID; static extraction preserves every resource but does not invent runtime resource-pack precedence.",
        }
    return {
        "owner_artifact": extracted.get("artifact") if extracted else None,
        "owner_classes": extracted.get("owner_classes", []) if extracted else [],
        "delegated_classes": extracted.get("delegated_classes", []) if extracted else [],
        "owner_status": extracted.get("owner_status", "unresolved") if extracted else "unresolved",
        "observed_channels": extracted.get("mechanics", []) if extracted else [],
        "numeric_constants": extracted.get("numeric_constants", []) if extracted else [],
        "exact_numeric_mechanics": numeric,
        "special_attack_methods": special_methods,
        "special_attack_coverage": extracted.get("special_attack_coverage", {}) if extracted else {},
        "better_combat": better_combat,
        "epic_fight": epic_fight,
        "epic_fight_resolution": "single_exact_resource" if len(epic_fight) == 1 else ("resource_collision_precedence_unresolved" if epic_fight else "not_observed"),
        "dimensions": dimensions,
    }


def item_records(kind: str, pattern: str, dimensions: list[str], native: dict[str, Any], tweak: str | None = None) -> list[dict[str, Any]]:
    registrations = registered_item_map(native)
    excluded = load_tag_values(ROOT / "Mods" / "JEMCompat" / "src" / "main" / "resources" / "data" / "jemcompat" / "tags" / "items" / "not_weapons.json")
    included = load_tag_values(ROOT / "Mods" / "JEMCompat" / "src" / "main" / "resources" / "data" / "jemcompat" / "tags" / "items" / "weapons.json")
    rewards = reward_profiles()
    reward_ids = {item_id for item_id, profiles in rewards.items() if any("weapon" in str(profile.get("category", "")).lower() for profile in profiles)}
    capability_kind = "weapon" if kind == "weapon_candidate" else "armor"
    capability_ids = {
        row["item_id"]
        for row in native.get("data_driven_combat", {}).get("epic_fight_capabilities", [])
        if row.get("kind") == capability_kind
    }
    grouped: dict[str, list[tuple[str, dict[str, Any]]]] = {}
    for source, row in rows(pattern):
        grouped.setdefault(row["item_id"], []).append((source, row))
    registry = registry_item_map()
    source_backed_ids = reward_ids | capability_ids if kind == "weapon_candidate" else capability_ids
    for item_id in sorted(source_backed_ids):
        if item_id in registry:
            source, row = registry[item_id]
            grouped.setdefault(item_id, []).append((source, {**row, "item_id": item_id}))
    result = []
    for item_id, candidates in sorted(grouped.items()):
        source, row = max(candidates, key=lambda pair: (bool(pair[1].get("display_name_en_us")), pair[1].get("confidence") == "source_verified"))
        registration = registrations.get(item_id)
        is_builtin = item_id.startswith("minecraft:")
        source_named = bool(row.get("display_name_en_us")) and "." not in item_id.partition(":")[2]
        if item_id in excluded:
            continue
        if item_id not in included and item_id not in reward_ids and item_id not in capability_ids and not combat_item_signal(item_id, kind):
            continue
        if registration is None and not is_builtin and not source_named:
            continue
        entry = basic(item_id, kind, row, source)
        entry["power_dimensions"] = dimensions
        entry["registration"] = registration or {
            "item_id": item_id,
            "status": "minecraft_builtin_static" if is_builtin else "language_named_registration_unresolved",
            "unresolved_reason": None if is_builtin else "no literal item register call was extracted; the exact translation-backed candidate remains visible for dynamic/helper registration review",
        }
        entry["native_mechanics"] = native_item_mechanics(kind, item_id, native)
        registry_row = registry.get(item_id, (None, {}))[1]
        entry["identity"] = {
            "source_mod": registry_row.get("mod_name") or registry_row.get("mod_id"),
            "installed_version": registry_row.get("mod_version"),
            "source_jar": registry_row.get("source_jar"),
        }
        entry["acquisition"] = {
            "status": "source_verified_partial" if registry_row.get("acquisition_routes") else "unresolved",
            "routes": registry_row.get("acquisition_routes", []),
            "class": registry_row.get("acquisition_class"),
            "unresolved": None if registry_row.get("acquisition_routes") else "No static recipe/loot/acquisition route is linked to this registry ID.",
        }
        if item_id in rewards:
            entry["boss_reward_profiles"] = rewards[item_id]
        if tweak:
            entry["jem_tweaks"].append({"condition": tweak, "source": "JEMWorldBossTiers progression equipment rules"})
            entry["effective"]["expression"] = f"native ItemStack expression; then {tweak} when applicable"
        result.append(entry)
    return sorted(result, key=lambda value: value["id"])


def accessory_records(native: dict[str, Any]) -> list[dict[str, Any]]:
    tokens = ("ring", "amulet", "necklace", "talisman", "charm", "bracelet", "belt", "curio", "relic")
    registrations = registered_item_map(native)
    result = []
    for source, row in rows("registries/items/*.json"):
        item_id = row["registry_id"]
        path = item_id.partition(":")[2].lower()
        name = (row.get("display_name_en_us") or "").lower()
        if not any(token in path or token in name for token in tokens):
            continue
        if item_id not in registrations:
            continue
        entry = basic(item_id, "accessory_candidate", row, source)
        entry["power_dimensions"] = ["attribute modifiers", "spell power", "resistance", "resource economy", "conditional effects"]
        entry["registration"] = registrations.get(item_id, {
            "status": "language_named_registration_unresolved",
            "unresolved_reason": "no literal register call was extracted from the owning exact-version artifact",
        })
        entry["native_mechanics"] = {
            "owner_artifact": next((item.get("artifact") for item in native.get("items", []) if item["item_id"] == item_id), None),
            "owner_classes": next((item.get("owner_classes", []) for item in native.get("items", []) if item["item_id"] == item_id), []),
            "observed_channels": next((item.get("mechanics", []) for item in native.get("items", []) if item["item_id"] == item_id), []),
            "exact_numeric_mechanics": next((item.get("exact_numeric_mechanics", {}) for item in native.get("items", []) if item["item_id"] == item_id), {}),
            "special_attack_methods": next((item.get("special_attack_methods", []) for item in native.get("items", []) if item["item_id"] == item_id), []),
            "dimensions": {
                field: {"status": "unresolved", "unresolved_reason": f"{field} requires Curios slot/capability and registered item implementation evidence for {item_id}"}
                for field in ("slot", "attributes", "passive", "active", "trigger", "cooldown", "resource_cost")
            },
        }
        entry["assessment"]["reason"] = "Registration/name evidence identifies an accessory candidate; each native capability stays explicitly unresolved until its item implementation is traced."
        result.append(entry)
    return sorted(result, key=lambda value: value["id"])


def enchantment_records() -> list[dict[str, Any]]:
    result = []
    for source, row in rows("registries/enchantments/*.json"):
        enchantment_id = row["registry_id"]
        entry = basic(enchantment_id, "enchantment", row, source)
        entry["power_dimensions"] = ["damage", "mitigation", "utility", "resource cost", "compatibility"]
        if enchantment_id == "jem_world_boss_tiers:progression":
            entry["jem_tweaks"] = [{
                "expression": "weapon damage x1.00/x1.05/x1.10 by World Tier; Tier III+ 15% chance x1.5; eligible bows gain pierce 0/2/3; armor has slot-specific bonuses",
                "source": "JemWorldBossTiers.java, EquipmentScaler.java and ProgressionArmor.java"
            }]
            entry["effective"]["status"] = "partial"
        result.append(entry)
    return sorted(result, key=lambda value: value["id"])


def spell_records(evidence: dict[str, Any], native: dict[str, Any]) -> list[dict[str, Any]]:
    verified = {row["spell_id"]: row for row in evidence["beam_candidates"]}
    mechanics = {row["spell_id"]: row for row in native["spells"]}
    names = irons_spell_names(evidence)
    result = []
    for spell_id, school in spell_ids():
        mechanic = mechanics.get(spell_id, {})
        formulas = mechanic.get("formulas", {})
        primary = formulas.get("getDamage") or formulas.get("getHealing")
        metadata = dict(mechanic.get("metadata", {}))
        metadata.update({key: value for key, value in verified.get(spell_id, {}).items() if key in {"rarity", "cast_type", "max_level", "base_spell_power", "spell_power_per_level", "cast_time_ticks"}})
        entry = {
            "id": spell_id,
            "kind": "spell",
            "display_name": names.get(spell_id) or verified.get(spell_id, {}).get("display_name"),
            "school": school,
            "power_dimensions": ["per-hit damage", "hit count", "cast time", "cooldown", "mana", "range/area", "control", "sustain"],
            "original": {
                "status": "source_verified_partial" if mechanic.get("owner_class") else "unresolved",
                "expression": verified.get(spell_id, {}).get("native_damage_expression") or (primary or {}).get("expression"),
                "known_dimensions": metadata,
                "unknown_is_zero": False
            },
            "native_mechanics": {
                "owner_class": mechanic.get("owner_class"),
                "owner_status": mechanic.get("owner_status", "unresolved"),
                "delegated_classes": mechanic.get("delegated_classes", []),
                "channels": mechanic.get("channels", []),
                "formulas": formulas,
                "effect_methods": mechanic.get("effect_methods", []),
                "unresolved_reason": mechanic.get("unresolved_reason"),
            },
            "modifiers": [{"owner": "Iron's Spells", "expression": "school resistance soft-cap multiplier after SpellDamageEvent"}],
            "jem_tweaks": [
                {"owner": "JEMWorldBossTiers", "expression": "PvE player cast x0.65/x0.80/x0.95/x1.10/x1.20 by World Tier; PvP x1.00"},
                {"owner": "JEMRoles", "expression": "Human direct-hit spell +1 when its native direct-hit conditions apply"}
            ],
            "effective": {
                "status": "partial" if mechanic.get("owner_class") else "unresolved",
                "expression": "(native expression * PvE tier factor or PvP 1.00 + conditional Human direct-hit 1) * Iron school-resistance multiplier; then ordinary target mitigation"
            },
            "acquisition": {
                "status": "partial",
                "routes": ["irons_spellbooks:scroll ItemStack variant", "native randomize_spell loot pools"],
                "unresolved": "Exact per-spell loot probability and observed stack level are not proven by the registry item ID."
            },
            "assessment": {
                "status": "source_reviewed_runtime_unverified" if mechanic.get("owner_class") else "unverified",
                "reason": verified.get(spell_id, {}).get("unresolved", "Exact owner-class mechanics are extracted where safely recognizable; delegated entities/effects, the observed ItemStack, caster attributes, target mitigation and runtime result remain unverified.")
            },
            "evidence": [
                {"type": "exact_jar", "source": evidence["artifact"]["file"], "sha256": evidence["artifact"]["sha256"]},
                {"type": "owner_bytecode", "source": mechanic.get("owner_class"), "confidence": "exact_installed_class" if mechanic.get("owner_class") else "unresolved"},
            ]
        }
        result.append(entry)
    return result


def native_boss_tags() -> dict[str, list[dict[str, str]]]:
    result: dict[str, list[dict[str, str]]] = {}
    for jar in sorted((ROOT / "Client" / "ManagedPack" / "mods").glob("*.jar")):
        try:
            with zipfile.ZipFile(jar) as archive:
                tag_files: dict[str, dict[str, Any]] = {}
                for name in archive.namelist():
                    lower = name.lower()
                    if lower.endswith(".json") and "/tags/entity_types/" in lower:
                        try:
                            tag_files[name] = json.loads(archive.read(name).decode("utf-8-sig"))
                        except (UnicodeDecodeError, json.JSONDecodeError):
                            pass
                roots = [name for name in tag_files if name.lower().endswith(("/forge/tags/entity_types/bosses.json", "/c/tags/entity_types/bosses.json"))]
                pending = [(name, value) for name in roots for value in tag_files[name].get("values", [])]
                seen_tags: set[str] = set()
                while pending:
                    source_name, value = pending.pop()
                    value = value if isinstance(value, str) else value.get("id", "")
                    if value.startswith("#"):
                        tag = value[1:]
                        if tag in seen_tags or ":" not in tag:
                            continue
                        seen_tags.add(tag)
                        namespace, path = tag.split(":", 1)
                        nested = f"data/{namespace}/tags/entity_types/{path}.json"
                        pending.extend((nested, child) for child in tag_files.get(nested, {}).get("values", []))
                    elif ":" in value:
                        result.setdefault(value, []).append({"jar": jar.name, "tag": source_name})
        except zipfile.BadZipFile:
            continue
    for vanilla in ("minecraft:ender_dragon", "minecraft:wither"):
        result.setdefault(vanilla, []).append({"jar": "Minecraft 1.20.1 server", "tag": "native boss contract"})
    return result


def boss_records(native_mechanics: dict[str, Any]) -> list[dict[str, Any]]:
    directory = ROOT / "Mods" / "JEMPackCore" / "src" / "main" / "resources" / "data" / "jem_world_boss_tiers" / "jem_world_boss_tiers" / "bosses"
    profiles: dict[str, tuple[dict[str, Any], Path]] = {}
    for source in sorted(directory.glob("*.json")):
        for profile in load(source).get("bosses", []):
            for entity_id in profile["entity_ids"]:
                profiles[entity_id] = (profile, source)
    native = native_boss_tags()
    mechanics = {row["entity_id"]: row for row in native_mechanics["bosses"]}
    entity_names = {row["entity_id"]: row.get("display_name_en_us") for _, row in rows("views/mobs/*.json") if row.get("display_name_en_us")}
    result = []
    for entity_id in sorted(set(profiles) | set(native)):
        profile_source = profiles.get(entity_id)
        profile = profile_source[0] if profile_source else None
        source = profile_source[1] if profile_source else None
        registered = profile is not None
        mechanic = mechanics.get(entity_id, {})
        profile_values = mechanic.get("profile", profile or {})
        special_values = profile_values.get("special_damage_multiplier")
        tier_expression = "native attack"
        if registered:
            tier_expression += " * (1 + 0.15 * world_tier)"
            if special_values:
                tier_expression += f" * special_damage_multiplier{special_values} when the encounter is not Raid"
            tier_expression += " * hosted participant damage factor when hosted; hosted owned attacks then x0.88"
        result.append({
                "id": entity_id,
                "kind": "registered_boss" if registered else "native_boss_candidate",
                "profile_key": profile["key"] if profile else None,
                "display_name": profile.get("display_name") if profile else entity_names.get(entity_id),
                "jem_registered_profile": registered,
                "native_boss_tag": entity_id in native,
                "native_mechanics": {
                    "owner_artifact": mechanic.get("owner_artifact"),
                    "owner_classes": mechanic.get("owner_classes", []),
                    "delegated_classes": mechanic.get("delegated_classes", []),
                    "owner_status": mechanic.get("owner_status", "unresolved"),
                    "unresolved_reason": mechanic.get("unresolved_reason"),
                    "channels": mechanic.get("channels", []),
                    "native_attributes": mechanic.get("native_attributes", []),
                    "attack_methods": mechanic.get("attack_methods", []),
                    "attack_coverage": mechanic.get("attack_coverage", {}),
                },
                "registered_profile": {
                    key: profile_values.get(key)
                    for key in ("native_threat", "hp_scaling_safety", "attributes", "special_damage_multiplier", "damage_adapter", "healing_adapter", "stagger_adapter", "arena_strategy", "anti_cheese_profile")
                    if key in profile_values
                },
                "attacks": mechanic.get("attack_methods", []),
                "original": {"status": "source_verified_partial" if mechanic.get("owner_classes") else "unresolved", "expression": "exact owner-class attack channels; attack-specific arithmetic stays attached to its native class/projectile", "unknown_is_zero": False},
                "modifiers": [{"owner": "native boss mod", "expression": "projectile/explosion/beam/summon/custom-source behavior remains attack-specific"}],
                "jem_tweaks": ([
                    {"expression": tier_expression},
                    {"expression": "max health receives profile/generic tier attributes, then x(1 + 0.15*tier), then hosted participant/raid health factor"},
                    {"expression": "below 30% health: +15% movement speed and attack speed (not direct damage)"}
                ] if registered else [{"expression": "No JEM registered profile proven; native boss behavior remains authoritative."}]),
                "effective": {"status": "partial", "expression": tier_expression + "; each delegated projectile/goal without a resolved arithmetic formula remains symbolic"},
                "acquisition": {"status": "not_applicable", "routes": []},
                "assessment": {"status": "source_reviewed_runtime_unverified", "reason": ("JEM registration, exact owner class and observed attack-channel signals are recorded; non-observed delegated goals/projectiles remain explicitly unresolved and no runtime result is claimed." if registered else "Native boss tag/owner evidence is recorded, but the entity is not in the canonical JEM Pack Core boss profiles.")},
                "evidence": (([{"type": "jem_owner", "source": source.relative_to(ROOT).as_posix(), "confidence": "source_verified"}] if source else [])
                             + [{"type": "native_boss_tag", **item, "confidence": "source_verified"} for item in native.get(entity_id, [])]
                             + ([{"type": "owner_bytecode", "artifact": mechanic.get("owner_artifact"), "classes": mechanic.get("owner_classes", []), "confidence": mechanic.get("owner_status")} ] if mechanic else []))
            })
    return result


def entity_records(boss_ids: set[str], native: dict[str, Any]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    allies, others = [], []
    ally_mechanics = {row["entity_id"]: row for row in native.get("allies", [])}
    ally_tokens = ("recruit", "companion", "summon", "tamed", "golem", "polar_bear", "vex", "horse")
    ally_namespaces = {"recruits", "workers", "companions"}
    for source, row in rows("views/mobs/*.json"):
        entity_id = row["entity_id"]
        if entity_id in boss_ids:
            continue
        path = entity_id.partition(":")[2].lower()
        namespace = entity_id.partition(":")[0]
        is_ally_candidate = namespace in ally_namespaces or any(token in path for token in ally_tokens)
        entry = basic(entity_id, "ally_candidate" if is_ally_candidate else "damage_capable_entity_candidate", row, source)
        entry["power_dimensions"] = ["health", "armor", "attack channels", "speed/range", "AI uptime", "owner scaling", "cost/upkeep"]
        if is_ally_candidate:
            mechanic = ally_mechanics.get(entity_id, {})
            entry["native_mechanics"] = {
                "owner_artifact": mechanic.get("owner_artifact"),
                "owner_classes": mechanic.get("owner_classes", []),
                "delegated_classes": mechanic.get("delegated_classes", []),
                "owner_status": mechanic.get("owner_status", "unresolved"),
                "combat_channels": mechanic.get("combat_channels", []),
                "native_attributes": mechanic.get("native_attributes", []),
                "attack_methods": mechanic.get("attack_methods", []),
                "unresolved_reason": mechanic.get("unresolved_reason"),
            }
            entry["assessment"]["reason"] = "Exact owner-class combat signals are recorded when matched; ownership, tameability, AI uptime and delegated goals remain explicit unresolved dimensions."
        else:
            entry["assessment"]["reason"] = "Entity is inventory-accounted; hostility, summon ownership and native attack expressions require class/config evidence."
        (allies if is_ally_candidate else others).append(entry)
    return sorted(others, key=lambda value: value["id"]), sorted(allies, key=lambda value: value["id"])


def modifier_records() -> list[dict[str, Any]]:
    changes = []
    for path in sorted((CATALOG / "changes" / "records").glob("*.json")):
        row = load(path)
        if (any(system in {"combat", "progression", "event_balance", "boss_scaling", "pvp"} for system in row.get("impacts", {}).get("systems", []))
                and any(modifier.get("operation") != "document" for modifier in row["change"]["modifiers"])):
            changes.append({
                "id": row["change_id"],
                "status": row["status"],
                "selectors": row["selectors"],
                "modifiers": row["change"]["modifiers"],
                "owner": row["cause"]["owner"],
                "source_files": row["cause"]["source_files"]
            })
    changes.extend([
        {"id": "jem.progression-weapon", "owner": "JEMWorldBossTiers", "expression": "eligible progression weapon x1.00/x1.05/x1.10; Tier III+ 15% x1.5 critical", "source_files": ["Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/JemWorldBossTiers.java"]},
        {"id": "jem.spell-tier", "owner": "JEMWorldBossTiers", "expression": "non-PvP player spell x0.65/0.80/0.95/1.10/1.20; PvP x1.00", "source_files": ["Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/compat/irons/SpellTierFactors.java"]},
        {"id": "jem.hosted-boss", "owner": "JEMWorldBossTiers", "expression": "participant health 1/1.45/1.8/2.1/+0.2; damage 1/1.10/1.18/1.25/+0.05 capped1.45; raid defaults x2.5; owned attacks x0.88", "source_files": ["Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/encounter/HostedScaling.java", "Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/encounter/HostedConfig.java"]},
        {"id": "jem.race-profession-defense", "owner": "JEMRoles/JEMProfessions", "expression": "Elf fall x0.90; Miner level10 stone fall x0.90; Human direct spell +1; Elf eligible arrow +1", "source_files": ["Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java", "Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/progress/ProfessionGameplay.java"]}
    ])
    return changes


def scenarios() -> list[dict[str, Any]]:
    definitions = [
        (1, "Weak early player", ["minecraft:stone_sword", "minecraft:iron_chestplate"], "native melee against ordinary hostile at World Tier I"),
        (2, "Mid-game player", ["minecraft:diamond_sword", "minecraft:diamond_chestplate"], "native weapon + actual ItemStack enchantments; no assumed enchant levels"),
        (3, "Advanced player", ["minecraft:diamond_sword"], "scenario requires exact accessory IDs/buffs before numeric comparison"),
        (4, "Legitimate endgame player", ["minecraft:netherite_sword", "minecraft:netherite_chestplate"], "native gear plus exact verified modifiers"),
        (5, "Endgame without consumables", ["minecraft:netherite_sword"], "same as endgame but consumable effects omitted, not treated as zero-cost preparation"),
        (6, "Two-player mixed party", [], "boss health x1.45 and damage x1.10 before optional raid factors"),
        (7, "Full cooperative party", [], "four-player boss health x2.10 and damage x1.25 before optional raid factors"),
        (8, "Comparable PvP", [], "Iron spell tier factor is exactly x1.00; use actual armor, resistance and ItemStack state"),
        (9, "Undergeared versus top PvP", [], "report time-to-defeat and control uptime; no PvE tier spell multiplier"),
        (10, "One recruit or tameable", ["recruits:recruit"], "native companion attributes/AI; JEM does not invent combat scaling"),
        (11, "Several mixed troops", [], "sum source-resolved per-unit output with target access and uptime; unresolved units stay unresolved"),
        (12, "Large funded army", [], "include Recruits payroll/acquisition, congestion, survival and native AI; never multiply one unit by count blindly"),
        (13, "Iron's Spells mage", ["irons_spellbooks:ray_of_siphoning"], "native per-hit expression -> PvE tier factor -> conditional Human +1 -> school resistance"),
        (14, "Epic Fight melee", [], "requires exact weapon moveset, animation timings, stamina and target context from installed Nightfall/Epic Fight owners"),
        (15, "Heavy armor versus beam", ["irons_spellbooks:sunbeam", "irons_spellbooks:ray_of_siphoning"], "magic damage is not tagged bypasses_armor by Iron, but school resistance and actual armor integration must be measured separately"),
        (16, "World Tier hosted raid boss", ["minecraft:wither"], "native attack * boss tier * optional profile factor * party damage * raid x2.5 * hosted owned-attack x0.88; health uses corresponding health factors")
    ]
    return [{"scenario": number, "name": name, "exact_ids": ids, "expression": expression, "result_status": "partial", "unresolved_policy": "unknown values remain symbolic and are never substituted with zero"} for number, name, ids, expression in definitions]


def warnings(evidence: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        {
            "id": "beam.reported-item-unidentified",
            "exact_ids": [row["spell_id"] for row in evidence["beam_candidates"]],
            "context": "Reported easy-to-obtain unenchanted book/scroll removes about half of Nightfall-armored player health.",
            "original_expression": "Candidate-specific fixed or per-channel-hit expressions; none of the inspected candidates uses percentage-health damage.",
            "jem_expression": "PvP x1.00; PvE x0.65/0.80/0.95/1.10/1.20, plus conditional Human direct-hit +1, then Iron resistance.",
            "effective_expression": "Cannot be made unique without spell ID, level and caster/target attributes from the observed ItemStack/context.",
            "suspicion": "Common continuous spells can accumulate several native hit applications and Ray of Siphoning also heals for post-event damage.",
            "evidence": ["exact installed Iron's Spells 3.15.6 bytecode", "native loot randomize_spell tables", "JEM spell adapters"],
            "confidence": "candidate_set_source_verified_identity_unresolved",
            "suggested_owner": "Iron's Spells native spell config/code first; narrow JEM adapter only if the exact native seam cannot express the approved correction",
            "action": "No speculative nerf applied. Capture exact localized spell name or scroll ItemStack spell ID/level."
        },
        {
            "id": "boss.attack-channel-coverage",
            "exact_ids": ["registered boss profiles"],
            "context": "Bosses may damage through projectiles, explosions, beams, summons or custom sources.",
            "original_expression": "attack-specific native code",
            "jem_expression": "registered boss tier and hosted multipliers apply around native damage events",
            "effective_expression": "partial until each native attack channel is traced",
            "suspicion": "Using ATTACK_DAMAGE as the complete boss model would silently miss burst channels.",
            "evidence": ["canonical JEM Pack Core boss profiles", "native boss tags", "JemWorldBossTiers.java"],
            "confidence": "high",
            "suggested_owner": "owning boss mod per attack; JEMWorldBossTiers only for shared wrapper rules"
        }
    ]


def audit_findings() -> list[dict[str, Any]]:
    return [
        {
            "id": "boss-reward-json-unused-fields",
            "status": "source_verified",
            "severity": "high",
            "finding": "attribute_contributions and ability_multipliers exist in reward JSON but are not parsed into RewardProfile and therefore do not affect EquipmentScaler.",
            "mechanism": "BalanceDataParser.parseRewards constructs RewardProfile(item_id, source_boss, category); RewardProfile has exactly those three components; EquipmentScaler adds only Progression enchantment contributions.",
            "sources": [
                "Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/balance/BalanceDataParser.java",
                "Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/balance/RewardProfile.java",
                "Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/balance/EquipmentScaler.java",
            ],
            "runtime_change": False,
        },
        {
            "id": "frostbitten-golem-special-stack",
            "status": "source_verified",
            "severity": "critical_review",
            "finding": "Frostbitten Golem special damage multipliers [2.7, 2.8, 3.2, 5.0, 5.0] stack with the ordinary registered-boss tier boost and hosted participant damage factor outside Raid; hosted owned attacks then receive x0.88. Raid skips the profile special multiplier.",
            "mechanism": "JemWorldBossTiers multiplies factor by BossTierScaling.boost(tier), then non-Raid specialDamageMultiplier, then HostedEncounters.damageFactor; a later hosted-owner branch multiplies by 0.88.",
            "sources": [
                "Mods/JEMPackCore/src/main/resources/data/jem_world_boss_tiers/jem_world_boss_tiers/bosses/expanded_bosses.json",
                "Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/JemWorldBossTiers.java",
            ],
            "runtime_change": False,
        },
        {
            "id": "blood-moon-penetration-post-mitigation",
            "status": "source_verified",
            "severity": "semantic_mismatch",
            "finding": "Blood Moon Armor Penetration is post-mitigation damage amplification, not removal of a fraction of armor/toughness.",
            "mechanism": "LivingDamageEvent amount is divided by (1 - min(0.30, penetration)); the event is after armor mitigation. The player debuff is another damage multiplier.",
            "sources": [
                "Mods/JEMServer/src/main/java/com/siirio/jemserver/smp/events/EventMobModifiers.java",
                "Mods/JEMServer/src/main/java/com/siirio/jemserver/smp/events/BossDebuffController.java",
            ],
            "runtime_change": False,
        },
        {
            "id": "progression-guard-any-hurt-event",
            "status": "source_verified",
            "severity": "review",
            "finding": "Tier III Progression chest guard is consumed by the first qualifying LivingHurtEvent while ready, without a heavy-hit, projectile, minimum-damage or damage-type gate of its own.",
            "mechanism": "The guardReady branch adds 35% reduction and immediately writes a 240-tick cooldown; it is independent of the separate heavy-hit branch.",
            "sources": ["Mods/JEMWorldBossTiers/src/main/java/com/siirio/jemworldbosstiers/balance/ProgressionArmor.java"],
            "runtime_change": False,
        },
        {
            "id": "equipment-durability-current-state",
            "status": "source_verified",
            "severity": "resolved_before_phase_2",
            "finding": "The current main branch does not grant artificial infinite durability or default max-damage values to progression/boss equipment. Eligible equipment keeps native wear, Mending and Unbreaking; dropped-item protection is a separate loss-prevention rule.",
            "mechanism": "Current module logic lock and localization explicitly preserve normal durability; no InfiniteEquipmentDurability production class exists in the current source tree.",
            "sources": [
                "Mods/JEMWorldBossTiers/AGENTS.logic-locks.md",
                "Mods/JEMWorldBossTiers/src/main/resources/assets/jem_world_boss_tiers/lang/en_us.json",
            ],
            "runtime_change": False,
        },
        {
            "id": "skarrier-ban-sword-1499-damage",
            "status": "exact_jar_bytecode",
            "severity": "critical_if_player_obtainable",
            "finding": "skarrier_mobs:ban_sword registers BanSwordItem with an exact +1499 attack-damage modifier and -2.4 attack-speed modifier, producing 1500 basic melee damage with the vanilla player base attribute.",
            "mechanism": "BanSwordItem constructs its main-hand AttributeModifier map with literal attack damage 1499. The current Atlas has no linked recipe, loot or other acquisition route, so player accessibility is unresolved rather than assumed.",
            "sources": [
                "Client/ManagedPack/mods/skarrier_mobs-1.0.6-forge-1.20.1.jar",
                "net.mcreator.skarriermobs.item.BanSwordItem",
            ],
            "runtime_change": False,
        },
    ]


REFERENCE_TARGETS = [
    {"id": "unarmored", "armor": 0.0, "toughness": 0.0, "protection_epf": 0},
    {"id": "full_iron", "armor": 15.0, "toughness": 0.0, "protection_epf": 0},
    {"id": "full_diamond", "armor": 20.0, "toughness": 8.0, "protection_epf": 0},
    {"id": "full_netherite", "armor": 20.0, "toughness": 12.0, "protection_epf": 0},
    {"id": "full_netherite_protection_iv", "armor": 20.0, "toughness": 12.0, "protection_epf": 16},
]


def evaluate_expression(expression: str, values: dict[str, float]) -> float:
    operators = {
        ast.Add: lambda left, right: left + right,
        ast.Sub: lambda left, right: left - right,
        ast.Mult: lambda left, right: left * right,
        ast.Div: lambda left, right: left / right,
    }

    def visit(node: ast.AST) -> float:
        if isinstance(node, ast.Expression):
            return visit(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
            return float(node.value)
        if isinstance(node, ast.Name) and node.id in values:
            return float(values[node.id])
        if isinstance(node, ast.BinOp) and type(node.op) in operators:
            return operators[type(node.op)](visit(node.left), visit(node.right))
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.UAdd, ast.USub)):
            value = visit(node.operand)
            return value if isinstance(node.op, ast.UAdd) else -value
        raise ValueError(f"unsupported expression node {type(node).__name__}")

    return visit(ast.parse(expression, mode="eval"))


def vanilla_post_mitigation(damage: float, armor: float, toughness: float, protection_epf: int) -> float:
    armor_term = max(armor * 0.2, min(20.0, armor - damage / (2.0 + toughness / 4.0)))
    after_armor = damage * (1.0 - armor_term / 25.0)
    return after_armor * (1.0 - min(20, protection_epf) / 25.0)


def spell_numeric_calculations(spells: list[dict[str, Any]]) -> list[dict[str, Any]]:
    tiers = {1: 0.65, 2: 0.80, 3: 0.95, 4: 1.10, 5: 1.20}
    records = []
    for spell in spells:
        metadata = spell["original"]["known_dimensions"]
        formula = spell.get("native_mechanics", {}).get("formulas", {}).get("getDamage", {})
        expression = formula.get("expression") if formula.get("status") == "resolved_simple_bytecode" else None
        base_power = metadata.get("base_spell_power")
        per_level = metadata.get("spell_power_per_level")
        max_level = metadata.get("max_level")
        if expression is None or not all(isinstance(value, (int, float)) for value in (base_power, per_level, max_level)):
            continue
        levels = []
        for level in range(1, int(max_level) + 1):
            spell_power = float(base_power) + float(per_level) * (level - 1)
            native = evaluate_expression(expression, {"spell_power": spell_power, "level": float(level)})
            mana = None
            if isinstance(metadata.get("base_mana_cost"), (int, float)) and isinstance(metadata.get("mana_cost_per_level"), (int, float)):
                mana = float(metadata["base_mana_cost"]) + float(metadata["mana_cost_per_level"]) * (level - 1)
            contexts = []
            for context, tier, factor in [("pve", tier, factor) for tier, factor in tiers.items()] + [("pvp", None, 1.0)]:
                outgoing = native * factor
                contexts.append({
                    "context": context,
                    "world_tier": tier,
                    "jem_outgoing_factor": factor,
                    "pre_mitigation_hp": round(outgoing, 6),
                    "reference_target_lost_hp": {
                        target["id"]: round(vanilla_post_mitigation(outgoing, target["armor"], target["toughness"], target["protection_epf"]), 6)
                        for target in REFERENCE_TARGETS
                    },
                })
            levels.append({
                "level": level,
                "base_spell_power_without_caster_bonus": round(spell_power, 6),
                "mana": mana,
                "native_damage_per_application": round(native, 6),
                "contexts": contexts,
            })
        records.append({
            "id": spell["id"],
            "kind": "spell_damage_table",
            "native_expression": expression,
            "levels": levels,
            "calculation_boundary": "One successful native damage application with no caster spell-power bonus, Human +1, school resistance, Resistance effect, absorption, shield, iframe, hit-count or duration assumption.",
            "mitigation_boundary": "Reference armor math is included because the exact installed Iron damage types are not tagged bypasses_armor; other mod integrations and school resistance remain unresolved.",
            "evidence": spell["evidence"],
        })
    return records


def hosted_factor(players: int, kind: str) -> float:
    if kind == "health":
        return {1: 1.0, 2: 1.45, 3: 1.8, 4: 2.1}.get(players, 2.1 + 0.2 * (players - 4))
    return {1: 1.0, 2: 1.10, 3: 1.18, 4: 1.25}.get(players, min(1.45, 1.25 + 0.05 * (players - 4)))


def boss_numeric_calculations(bosses: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for boss in bosses:
        if not boss.get("jem_registered_profile"):
            continue
        specials = boss.get("registered_profile", {}).get("special_damage_multiplier")
        rows = []
        for tier in range(1, 6):
            tier_factor = 1.0 + 0.15 * tier
            special = float(specials[tier - 1]) if isinstance(specials, list) and len(specials) >= tier else 1.0
            for players in range(1, 7):
                for raid in (False, True):
                    raid_factor = 2.5 if raid else 1.0
                    applied_special = 1.0 if raid else special
                    rows.append({
                        "world_tier": tier,
                        "players": players,
                        "raid": raid,
                        "tier_factor": round(tier_factor, 6),
                        "profile_special_damage_factor": round(applied_special, 6),
                        "hosted_health_factor": round(hosted_factor(players, "health"), 6),
                        "hosted_damage_factor": round(hosted_factor(players, "damage"), 6),
                        "raid_factor": raid_factor,
                        "effective_health_multiplier_over_native": round(tier_factor * hosted_factor(players, "health") * raid_factor, 6),
                        "effective_owned_attack_multiplier_over_native": round(tier_factor * applied_special * hosted_factor(players, "damage") * raid_factor * 0.88, 6),
                    })
        records.append({
            "id": boss["id"],
            "kind": "registered_boss_scaling_table",
            "rows": rows,
            "calculation_boundary": "Multipliers over each native attack/health value. Native per-channel arithmetic, profile attribute overrides and non-owned environmental damage remain attached to their own owners.",
            "evidence": boss["evidence"],
        })
    return records


OFFENSIVE_SINKS = {"hurt", "projectile_damage", "explosion", "effect"}


def offensive_method(method: dict[str, Any]) -> bool:
    return bool(OFFENSIVE_SINKS & set(method.get("damage_or_defense_sinks", [])))


def weapon_numeric_calculations(weapons: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for weapon in weapons:
        native = weapon.get("native_mechanics", {})
        exact = native.get("exact_numeric_mechanics", {})
        special_methods = [row for row in native.get("special_attack_methods", []) if offensive_method(row)]
        damage_values = [row for row in exact.get("attributes", []) if row.get("attribute") == "attack_damage_addition"]
        factory_damage = [row for row in exact.get("attributes", []) if row.get("attribute") == "factory_attack_damage_parameter"]
        speed_values = [row for row in exact.get("attributes", []) if row.get("attribute") == "attack_speed_addition"]
        better = native.get("better_combat")
        damage_addition = damage_values[0]["value"] if damage_values else None
        player_total = 1.0 + float(damage_addition) if isinstance(damage_addition, (int, float)) else None
        progression = []
        if player_total is not None:
            for tier, factor in ((1, 1.0), (2, 1.05), (3, 1.10), (4, 1.10), (5, 1.10)):
                progression.append({
                    "world_tier": tier,
                    "ordinary_hit_before_target_mitigation": round(player_total * factor, 6),
                    "tier_iii_plus_critical_before_target_mitigation": round(player_total * factor * 1.5, 6) if tier >= 3 else None,
                    "critical_probability": 0.15 if tier >= 3 else 0.0,
                })
        combo = []
        if better and better.get("provider_installed") and player_total is not None:
            for position, attack in enumerate(better.get("effective_attributes", {}).get("attacks", []), start=1):
                multiplier = attack.get("damage_multiplier")
                combo.append({
                    "position": position,
                    "damage_multiplier": multiplier,
                    "native_damage_before_target_mitigation": round(player_total * float(multiplier), 6) if isinstance(multiplier, (int, float)) else None,
                    "attack": attack,
                })
        records.append({
            "id": weapon["id"],
            "kind": "weapon_numeric_card",
            "attack_damage_addition": damage_addition,
            "factory_attack_damage_parameters": factory_damage,
            "player_base_attack_damage": 1.0 if player_total is not None else None,
            "native_basic_melee_damage_before_target_mitigation": round(player_total, 6) if player_total is not None else None,
            "attack_speed_additions": speed_values,
            "progression_enchantment_table": progression,
            "progression_enchantment_applicability": "Conditional only: use these rows when the exact ItemStack is eligible and actually carries the JEM Progression enchantment; this table does not assert universal application.",
            "better_combat_combo": combo,
            "better_combat_profile": better,
            "better_combat_runtime_active": bool(better and better.get("provider_installed")),
            "epic_fight_capabilities": native.get("epic_fight", []),
            "epic_fight_resolution": native.get("epic_fight_resolution", "not_observed"),
            "durability": exact.get("durability", []),
            "durability_costs": exact.get("durability_costs", []),
            "cooldowns": exact.get("cooldowns", []),
            "config_dependencies": exact.get("config_dependencies", []),
            "special_attack_methods": special_methods,
            "support_and_wear_methods": [row for row in native.get("special_attack_methods", []) if not offensive_method(row)],
            "special_attack_coverage": {
                "method_count": len(special_methods),
                "fully_numeric_count": sum(row.get("formula_status", "").startswith("resolved") for row in special_methods),
                "boundary": "Pure cooldown, healing and durability-consumption methods are preserved separately and are not counted as weapon special attacks.",
            },
            "resolution": {
                "status": "partial" if any((damage_values, factory_damage, speed_values, special_methods, native.get("epic_fight"))) else "unresolved",
                "missing": [
                    field for field, resolved in {
                        "basic_attack_total": bool(damage_values),
                        "special_attack_formula": any(row.get("formula_status", "").startswith("resolved") for row in special_methods),
                        "epic_fight_combo_damage": bool(combo),
                        "durability": bool(exact.get("durability")),
                    }.items() if not resolved
                ],
            },
            "calculation_boundary": "Basic melee totals use the vanilla player base attack-damage attribute 1 plus an exact ADDITION modifier. Projectile, beam, explosion, active ability and config-dependent values are not replaced by this number.",
            "evidence": weapon["evidence"],
        })
    return records


def defense_numeric_calculations(defenses: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for defense in defenses:
        native = defense.get("native_mechanics", {})
        exact = native.get("exact_numeric_mechanics", {})
        attributes = exact.get("attributes", [])
        records.append({
            "id": defense["id"],
            "kind": "defense_numeric_card",
            "attributes": attributes,
            "epic_fight_capabilities": native.get("epic_fight", []),
            "durability": exact.get("durability", []),
            "durability_costs": exact.get("durability_costs", []),
            "passive_or_active_methods": native.get("special_attack_methods", []),
            "config_dependencies": exact.get("config_dependencies", []),
            "resolution": {
                "status": "partial" if any((attributes, native.get("epic_fight"), exact.get("durability"), native.get("special_attack_methods"))) else "unresolved",
                "missing": [
                    field for field, resolved in {
                        "armor_and_toughness": any(row.get("attribute") in {"armor_addition", "armor_toughness_addition"} for row in attributes),
                        "durability": bool(exact.get("durability")),
                        "passive_active_formula": any(row.get("formula_status", "").startswith("resolved") for row in native.get("special_attack_methods", [])),
                    }.items() if not resolved
                ],
            },
            "evidence": defense["evidence"],
        })
    return records


def accessory_numeric_calculations(accessories: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for accessory in accessories:
        native = accessory.get("native_mechanics", {})
        exact = native.get("exact_numeric_mechanics", {})
        records.append({
            "id": accessory["id"],
            "kind": "accessory_numeric_card",
            "attributes": exact.get("attributes", []),
            "active_or_passive_methods": native.get("special_attack_methods", []),
            "cooldowns": exact.get("cooldowns", []),
            "config_dependencies": exact.get("config_dependencies", []),
            "resolution": {
                "status": "partial" if any((exact.get("attributes"), native.get("special_attack_methods"), exact.get("cooldowns"))) else "unresolved",
                "missing": ["curios_slot_and_stack_limit", "complete_passive_or_active_formula"],
            },
            "evidence": accessory["evidence"],
        })
    return records


def spell_method_calculations(spells: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [{
        "id": spell["id"],
        "kind": "spell_method_card",
        "native_formulas": spell.get("native_mechanics", {}).get("formulas", {}),
        "delegated_classes": spell.get("native_mechanics", {}).get("delegated_classes", []),
        "effect_methods": spell.get("native_mechanics", {}).get("effect_methods", []),
        "resolution": {
            "status": "partial" if spell.get("native_mechanics", {}).get("effect_methods") or spell.get("native_mechanics", {}).get("formulas") else "unresolved",
            "missing": ["full_application_count"] if any(channel.get("channel") == "multi_hit_or_channel" and channel.get("status", "").startswith("observed") for channel in spell.get("native_mechanics", {}).get("channels", [])) else [],
        },
        "evidence": spell["evidence"],
    } for spell in spells]


def boss_attack_calculations(bosses: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for boss in bosses:
        all_methods = boss.get("native_mechanics", {}).get("attack_methods", [])
        attacks = [row for row in all_methods if offensive_method(row)]
        records.append({
            "id": boss["id"],
            "kind": "boss_attack_table",
            "native_attributes": boss.get("native_mechanics", {}).get("native_attributes", []),
            "attack_methods": attacks,
            "support_and_recovery_methods": [row for row in all_methods if not offensive_method(row)],
            "delegated_classes": boss.get("native_mechanics", {}).get("delegated_classes", []),
            "resolution": {
                "status": "partial" if attacks else "unresolved",
                "attack_method_count": len(attacks),
                "fully_numeric_attack_count": sum(row.get("formula_status", "").startswith("resolved") for row in attacks),
            },
            "evidence": boss["evidence"],
        })
    return records


def durability_audit(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for item in items:
        exact = item.get("native_mechanics", {}).get("exact_numeric_mechanics", {})
        durability = exact.get("durability", [])
        costs = exact.get("durability_costs", [])
        records.append({
            "id": item["id"],
            "kind": "durability_audit",
            "native_max_damage": durability,
            "observed_consumption": costs,
            "status": "max_and_consumption_traced" if durability and costs else ("partial" if durability or costs else "unresolved"),
            "unresolved_reason": None if durability and costs else "Registration factory, inherited Item behavior, tags/NBT or active ability path still lacks one side of max-damage/consumption evidence.",
            "runtime_observation_required": False,
        })
    return records


ARMOR_SUFFIXES = ("helmet", "chestplate", "leggings", "boots")


def armor_set_calculations(defenses: list[dict[str, Any]]) -> list[dict[str, Any]]:
    groups: dict[str, dict[str, dict[str, Any]]] = {}
    for defense in defenses:
        namespace, _, path = defense["id"].partition(":")
        slot = next((suffix for suffix in ARMOR_SUFFIXES if path.endswith(f"_{suffix}")), None)
        if slot is None:
            continue
        stem = path[:-(len(slot) + 1)]
        groups.setdefault(f"{namespace}:{stem}", {})[slot] = defense
    records = []
    for set_id, pieces in sorted(groups.items()):
        if len(pieces) < 4:
            continue
        attributes = [
            attribute
            for piece in pieces.values()
            for attribute in piece.get("native_mechanics", {}).get("exact_numeric_mechanics", {}).get("attributes", [])
        ]
        capabilities = [
            capability
            for piece in pieces.values()
            for capability in piece.get("native_mechanics", {}).get("epic_fight", [])
        ]
        totals = {
            semantic: round(sum(float(row["value"]) for row in attributes if row.get("attribute") == semantic), 6)
            for semantic in ("armor_addition", "armor_toughness_addition", "knockback_resistance_addition")
        }
        exact_totals = any(totals.values())
        records.append({
            "id": set_id,
            "kind": "armor_set_card",
            "pieces": {slot: piece["id"] for slot, piece in sorted(pieces.items())},
            "summed_exact_attributes": totals,
            "epic_fight_capabilities": capabilities,
            "resolution": {
                "status": "partial" if exact_totals or capabilities else "unresolved",
                "missing": [
                    field for field, resolved in {
                        "all_native_armor_values": exact_totals,
                        "all_set_passive_active_formulas": all(piece.get("native_mechanics", {}).get("special_attack_methods") for piece in pieces.values()),
                    }.items() if not resolved
                ],
            },
            "boundary": "Only four-piece registry stems are grouped. Set bonuses are not inferred from naming; exact capability and owner-method evidence stays attached to each piece.",
        })
    return records


def weapon_power_ladder(weapons: list[dict[str, Any]]) -> list[dict[str, Any]]:
    records = []
    for weapon in weapons:
        native = weapon.get("native_mechanics", {})
        exact = native.get("exact_numeric_mechanics", {})
        damage = next((row.get("value") for row in exact.get("attributes", []) if row.get("attribute") == "attack_damage_addition"), None)
        capabilities = native.get("epic_fight", [])
        armor_negation = [
            style.get("armor_negation")
            for capability in capabilities
            for style in capability.get("data", {}).get("attributes", {}).values()
            if isinstance(style, dict) and isinstance(style.get("armor_negation"), (int, float))
        ]
        is_reward = bool(weapon.get("boss_reward_profiles"))
        records.append({
            "id": weapon["id"],
            "kind": "weapon_power_ladder_entry",
            "exact_basic_melee_damage": round(1.0 + float(damage), 6) if isinstance(damage, (int, float)) else None,
            "exact_epic_fight_armor_negation": armor_negation,
            "special_attack_methods_traced": sum(offensive_method(row) for row in native.get("special_attack_methods", [])),
            "boss_reward": is_reward,
            "actual_current_power_ranking": {
                "status": "unresolved_full_ranking",
                "reason": "A truthful overall rank requires complete special/combo DPS, acquisition and target-context evidence; the exact basic-melee value is exposed only as a sub-ranking input.",
            },
            "recommended_progression_ranking": {
                "status": "source_intent_only" if is_reward else "unresolved",
                "tier": "boss_reward" if is_reward else None,
                "reason": "Boss-reward association is source-backed intent; no recommended buff/nerf is invented during analysis-only Stage 3." if is_reward else "No approved intended tier is encoded for this item.",
            },
        })
    return sorted(records, key=lambda row: (row["exact_basic_melee_damage"] is None, -(row["exact_basic_melee_damage"] or 0), row["id"]))


def research_blockers(
    weapons: list[dict[str, Any]],
    defenses: list[dict[str, Any]],
    accessories: list[dict[str, Any]],
    spells: list[dict[str, Any]],
    bosses: list[dict[str, Any]],
    durability: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    blockers = []
    for record in weapons + defenses + accessories:
        deep_profile = record.get("stage_3_5a")
        if deep_profile:
            for unresolved in deep_profile["evidence"]["unresolved_fields"]:
                blockers.append({
                    "id": record["id"],
                    "kind": "boss_reward_weapon_stage_3_5a_blocker",
                    "missing_field": unresolved["field"],
                    "owner_classes": [deep_profile["owner_class_inspected"]],
                    "candidate_methods": [],
                    "why_unresolved": unresolved["reason"],
                    "runtime_observation_required": unresolved.get("runtime_required", False),
                    "diagnostic": "Log one server-authoritative hurt event with weapon, target armor/effects, raw amount, accepted amount, projectile/entity UUID and game tick; no production combat mutation."
                    if unresolved.get("runtime_required") else "Continue exact-version static reconstruction of the named animation/helper chain.",
                })
            continue
        native = record.get("native_mechanics", {})
        owners = native.get("owner_classes", [])
        dimensions = native.get("dimensions", {})
        for field, value in dimensions.items():
            if value.get("status", "").startswith(("exact", "inactive_optional")):
                continue
            blockers.append({
                "id": record["id"],
                "kind": "item_missing_field",
                "missing_field": field,
                "owner_classes": owners,
                "candidate_methods": [row.get("method") for row in native.get("special_attack_methods", [])],
                "why_unresolved": value.get("unresolved_reason") or "No safe static expression was reconstructed for this field.",
                "runtime_observation_required": field in {"acquisition_probability", "runtime_itemstack_nbt"},
            })
    for spell in spells:
        native = spell.get("native_mechanics", {})
        for method, formula in native.get("formulas", {}).items():
            if formula.get("status") == "resolved_simple_bytecode":
                continue
            blockers.append({
                "id": spell["id"],
                "kind": "spell_missing_formula",
                "missing_field": method,
                "owner_classes": [native.get("owner_class")] + native.get("delegated_classes", []),
                "candidate_methods": [method],
                "why_unresolved": formula.get("unresolved_reason"),
                "runtime_observation_required": False,
            })
        if any(channel.get("channel") == "multi_hit_or_channel" and channel.get("status", "").startswith("observed") for channel in native.get("channels", [])):
            blockers.append({
                "id": spell["id"],
                "kind": "spell_missing_application_count",
                "missing_field": "full_cast_application_count",
                "owner_classes": [native.get("owner_class")] + native.get("delegated_classes", []),
                "candidate_methods": [row.get("method") for row in native.get("effect_methods", [])],
                "why_unresolved": "Continuous scheduling, collision gating and invulnerability frames span owner and delegated entity tick methods.",
                "runtime_observation_required": False,
            })
    for boss in bosses:
        for attack in boss.get("native_mechanics", {}).get("attack_methods", []):
            if attack.get("formula_status", "").startswith("resolved"):
                continue
            blockers.append({
                "id": boss["id"],
                "kind": "boss_attack_missing_formula",
                "missing_field": "attack_damage_formula",
                "owner_classes": [attack.get("owner_class")],
                "candidate_methods": [attack.get("method")],
                "why_unresolved": attack.get("unresolved_reason"),
                "runtime_observation_required": attack.get("runtime_observation_required", False),
            })
    deep_ids = {record["id"] for record in weapons if record.get("stage_3_5a")}
    for row in durability:
        if row["id"] in deep_ids:
            continue
        if row["status"] == "max_and_consumption_traced":
            continue
        blockers.append({
            "id": row["id"],
            "kind": "durability_missing_field",
            "missing_field": "native_max_damage" if not row["native_max_damage"] else "ability_or_standard_consumption",
            "owner_classes": [],
            "candidate_methods": ["registration factory", "hurtAndBreak", "getMaxDamage"],
            "why_unresolved": row["unresolved_reason"],
            "runtime_observation_required": False,
        })
    return sorted(blockers, key=lambda row: (row["id"], row["kind"], row["missing_field"], str(row.get("candidate_methods"))))


def reference_mitigation_table() -> dict[str, Any]:
    inputs = (1.0, 2.0, 4.0, 8.0, 10.0, 20.0, 40.0, 80.0, 160.0)
    return {
        "id": "minecraft:vanilla_reference_mitigation",
        "kind": "vanilla_reference_mitigation",
        "formula": "damage * (1 - clamp(armor - damage/(2 + toughness/4), armor*0.2, 20)/25) * (1 - min(20, protection_epf)/25)",
        "targets": REFERENCE_TARGETS,
        "rows": [
            {"incoming_hp": damage, "lost_hp": {target["id"]: round(vanilla_post_mitigation(damage, target["armor"], target["toughness"], target["protection_epf"]), 6) for target in REFERENCE_TARGETS}}
            for damage in inputs
        ],
        "boundary": "Ordinary armor/enchantment reference only; bypass tags, shields, Resistance, mod passives, school resistance, absorption and JEM incoming reducers are separate layers.",
    }


def main() -> int:
    snapshot = git_head()
    evidence = load(CATALOG / "power-sources" / "irons-spellbooks-3.15.6.json")
    native = load(CATALOG / "power-sources" / "native-mechanics-phase3.json")
    stage35a_evidence = boss_reward_weapon_evidence()
    evidence_jar = ROOT / evidence["artifact"]["canonical_path"]
    actual_sha256 = hashlib.sha256(evidence_jar.read_bytes()).hexdigest()
    if actual_sha256 != evidence["artifact"]["sha256"]:
        raise SystemExit(f"stale Iron's Spells power evidence: expected {evidence['artifact']['sha256']}, found {actual_sha256}")
    weapons = item_records("weapon_candidate", "views/weapons/*.json", ["damage", "attack speed", "reach/range", "area", "cooldown", "resource cost"], native, "progression enchantment conditional tier multiplier and critical")
    attach_boss_reward_weapon_evidence(weapons, stage35a_evidence)
    armor = item_records("armor_candidate", "views/armor/*.json", ["armor", "toughness", "knockback resistance", "damage-type mitigation", "durability"], native, "progression enchantment conditional slot bonuses")
    accessories = accessory_records(native)
    enchantments = enchantment_records()
    spells = spell_records(evidence, native)
    bosses = boss_records(native)
    hostiles, allies = entity_records({row["id"] for row in bosses}, native)
    calculations = (
        [reference_mitigation_table()]
        + weapon_numeric_calculations(weapons)
        + stage35a_calculations(weapons)
        + defense_numeric_calculations(armor)
        + armor_set_calculations(armor)
        + accessory_numeric_calculations(accessories)
        + spell_method_calculations(spells)
        + spell_numeric_calculations(spells)
        + boss_numeric_calculations(bosses)
        + boss_attack_calculations(bosses)
    )
    durability = durability_audit(weapons + armor + accessories)
    stage35a_by_id = {row["id"]: row["stage_3_5a"] for row in weapons if row.get("stage_3_5a")}
    for durability_record in durability:
        if durability_record["id"] in stage35a_by_id:
            durability_record["stage_3_5a"] = stage35a_by_id[durability_record["id"]]["durability"]
            durability_record["status"] = stage35a_by_id[durability_record["id"]]["durability"]["status"]
            durability_record["unresolved_reason"] = None if "traced" in durability_record["status"] or "unbreakable" in durability_record["status"] else durability_record.get("unresolved_reason")
    power_ladder = weapon_power_ladder(weapons)
    for ladder_record in power_ladder:
        if ladder_record["id"] in stage35a_by_id:
            deep = stage35a_by_id[ladder_record["id"]]
            ladder_record["exact_basic_melee_damage"] = deep["ordinary_attack"].get("damage")
            ladder_record["stage_3_5a"] = {
                "combo": deep["combo"],
                "special_ability_count": len(deep["special_abilities"]),
                "verified_balance_issue": deep["power_assessment"]["verified_balance_issue"],
            }
    blockers = research_blockers(weapons, armor, accessories, spells, bosses, durability)
    reward_weapon_ids = {
        item_id
        for item_id, profiles in reward_profiles().items()
        if any("weapon" in str(profile.get("category", "")).lower() for profile in profiles)
    }
    outputs = {
        "weapons.json": ("weapons", weapons),
        "defenses.json": ("armor_and_defense", armor),
        "accessories.json": ("accessories", accessories),
        "enchantments.json": ("enchantments", enchantments),
        "spells.json": ("spells", spells),
        "bosses.json": ("bosses", bosses),
        "entities.json": ("hostile_and_damage_capable_candidates", hostiles),
        "allies.json": ("allies_troops_summons_mounts", allies),
        "modifiers.json": ("shared_modifiers", modifier_records()),
        "scenarios.json": ("combat_scenarios", scenarios()),
        "warnings.json": ("regression_warnings", warnings(evidence)),
        "findings.json": ("phase_3_source_findings", audit_findings()),
        "calculations.json": ("phase_3_numeric_calculations", calculations),
        "durability.json": ("phase_3_durability_audit", durability),
        "power-ladder.json": ("phase_3_weapon_power_ladder", power_ladder),
        "blockers.json": ("phase_3_named_research_blockers", blockers),
    }
    for name, (kind, values) in outputs.items():
        write(name, envelope(kind, values, snapshot))
    counts = {kind: len(values) for kind, values in (pair for pair in outputs.values())}
    all_inventory = weapons + armor + accessories + enchantments + spells + bosses + hostiles + allies
    status_counts = Counter(row.get("effective", {}).get("status", "not_applicable") for row in all_inventory)
    coverage = {
        "schema_version": SCHEMA,
        "snapshot_sha": snapshot,
        "denominator": "All candidates from current Atlas weapon/armor/entity/enchantment views plus spell IDs from the exact-version integration registry; heuristic categories remain explicitly candidates.",
        "identified_by_view": counts,
        "boss_breakdown": {
            "jem_registered_profiles": sum(row["jem_registered_profile"] for row in bosses),
            "additional_native_boss_candidates": sum(not row["jem_registered_profile"] for row in bosses),
            "total_boss_inventory": len(bosses)
        },
        "combat_inventory_total": len(all_inventory),
        "effective_expression_status": dict(sorted(status_counts.items())),
        "fully_resolved": sum(row.get("resolution", {}).get("status") == "resolved" for row in calculations),
        "partially_resolved": sum(row.get("resolution", {}).get("status") == "partial" for row in calculations),
        "unresolved": sum(row.get("resolution", {}).get("status") == "unresolved" for row in calculations),
        "reason_no_full_resolution": "No static record proves every runtime ItemStack attribute, target mitigation, attack channel and acquisition context.",
        "evidence_levels": ["atlas_static", "source_verified", "exact_jar_bytecode", "runtime_observed"],
        "unresolved_is_zero": False,
        "phase": 3,
        "native_mechanics": {
            "spell_owner_classes_resolved": sum(bool(row.get("native_mechanics", {}).get("owner_class")) for row in spells),
            "boss_owner_classes_resolved": sum(bool(row.get("native_mechanics", {}).get("owner_classes")) for row in bosses),
            "boss_combat_method_records": sum(len(row.get("attacks", [])) for row in bosses),
            "spell_channel_slots": sum(len(row.get("native_mechanics", {}).get("channels", [])) for row in spells),
            "ally_owner_classes_resolved": sum(bool(row.get("native_mechanics", {}).get("owner_classes")) for row in allies),
            "literal_item_registrations_observed": len(native["item_registrations"]),
            "boss_reward_weapon_ids": len(reward_weapon_ids),
            "boss_reward_weapon_ids_in_weapon_view": len(reward_weapon_ids & {row["id"] for row in weapons}),
            "spell_numeric_tables": sum(row.get("kind") == "spell_damage_table" for row in calculations),
            "boss_scaling_numeric_tables": sum(row.get("kind") == "registered_boss_scaling_table" for row in calculations),
            "weapon_numeric_cards": sum(row.get("kind") == "weapon_numeric_card" for row in calculations),
            "defense_numeric_cards": sum(row.get("kind") == "defense_numeric_card" for row in calculations),
            "armor_set_cards": sum(row.get("kind") == "armor_set_card" for row in calculations),
            "accessory_numeric_cards": sum(row.get("kind") == "accessory_numeric_card" for row in calculations),
            "spell_method_cards": sum(row.get("kind") == "spell_method_card" for row in calculations),
            "boss_attack_tables": sum(row.get("kind") == "boss_attack_table" for row in calculations),
            "boss_attack_methods_traced": sum(len(row.get("attack_methods", [])) for row in calculations if row.get("kind") == "boss_attack_table"),
            "boss_attacks_fully_numeric": sum(row.get("resolution", {}).get("fully_numeric_attack_count", 0) for row in calculations if row.get("kind") == "boss_attack_table"),
            "weapon_special_attack_methods_traced": sum(len(row.get("special_attack_methods", [])) for row in calculations if row.get("kind") == "weapon_numeric_card"),
            "weapon_special_attacks_fully_numeric": sum(sum(method.get("formula_status", "").startswith("resolved") for method in row.get("special_attack_methods", [])) for row in calculations if row.get("kind") == "weapon_numeric_card"),
            "epic_fight_capability_records": sum(len(row.get("epic_fight_capabilities", [])) for row in calculations if row.get("kind") == "weapon_numeric_card"),
            "epic_fight_fully_numeric_combos": sum(bool(row.get("better_combat_combo")) for row in calculations if row.get("kind") == "weapon_numeric_card" and row.get("epic_fight_capabilities")),
            "durability_max_damage_resolved": sum(bool(row.get("native_max_damage")) for row in durability),
            "durability_consumption_resolved": sum(bool(row.get("observed_consumption")) for row in durability),
            "durability_fully_traced": sum(row.get("status") == "max_and_consumption_traced" for row in durability),
            "stage_3_5a_boss_reward_profiles": sum(row.get("kind") == "boss_reward_weapon_stage_3_5a_card" for row in calculations),
            "stage_3_5a_exact_basic_melee_values": sum(row.get("kind") == "boss_reward_weapon_stage_3_5a_card" and row.get("resolution", {}).get("basic_numeric") for row in calculations),
            "stage_3_5a_numeric_special_abilities": sum(row.get("resolution", {}).get("numeric_special_count", 0) for row in calculations if row.get("kind") == "boss_reward_weapon_stage_3_5a_card"),
            "stage_3_5a_fully_numeric_combos": sum(row.get("resolution", {}).get("fully_numeric_combo", False) for row in calculations if row.get("kind") == "boss_reward_weapon_stage_3_5a_card"),
            "stage_3_5a_burst_calculations": sum(row.get("resolution", {}).get("burst_calculation_count", 0) for row in calculations if row.get("kind") == "boss_reward_weapon_stage_3_5a_card"),
            "stage_3_5a_sustained_dps_windows": sum(row.get("resolution", {}).get("sustained_window_count", 0) for row in calculations if row.get("kind") == "boss_reward_weapon_stage_3_5a_card"),
            "stage_3_5a_durability_profiles_closed": sum(
                row.get("kind") == "boss_reward_weapon_stage_3_5a_card"
                and row.get("durability", {}).get("status") in {"max_and_wear_traced", "max_and_normal_wear_traced", "max_and_normal_wear_traced_special_no_wear", "max_and_channel_wear_traced", "max_and_special_wear_traced", "max_and_consumption_traced", "verified_unbreakable_balance_issue"}
                for row in calculations
            ),
            "stage_3_5a_runtime_blockers": sum(row.get("kind") == "boss_reward_weapon_stage_3_5a_blocker" and row.get("runtime_observation_required") for row in blockers),
        },
        "known_gaps": [
            "Dynamic/helper-built item registrations and runtime ItemStack/NBT attribute variants",
            "Arbitrary branched bytecode cannot be collapsed into a verified formula merely because its sink, constants and installed config inputs are now named",
            "Epic Fight/Nightfall combo damage multipliers and attack phases remain unresolved where Java animation builders do not expose a safe data expression",
            "Continuous spell total applications remain unresolved when scheduling/hit gating spans multiple entity ticks",
            "Boss attack frequency and recovery remain unresolved unless a goal/animation scheduler exposes exact timing",
            "Entity hostility/taming/AI and native attributes for heuristic candidates",
            "Actual observed beam ItemStack spell ID, level, caster attributes and target resistances"
        ]
    }
    write("coverage.json", coverage)
    index = []
    for filename in ("weapons.json", "defenses.json", "accessories.json", "enchantments.json", "spells.json", "bosses.json", "entities.json", "allies.json", "calculations.json", "durability.json", "power-ladder.json", "blockers.json"):
        data = load(OUTPUT / filename)
        for position, row in enumerate(data["records"]):
            index.append({"id": row["id"], "display_name": row.get("display_name"), "kind": row["kind"], "file": f"Catalog/views/power/{filename}", "record_index": position})
    write("index.json", {"schema_version": SCHEMA, "snapshot_sha": snapshot, "records": sorted(index, key=lambda value: (value["id"], value["kind"]))})
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

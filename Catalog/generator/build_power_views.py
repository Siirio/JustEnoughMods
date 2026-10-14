#!/usr/bin/env python3
from __future__ import annotations

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
SCHEMA = 1


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


def item_records(kind: str, pattern: str, dimensions: list[str], tweak: str | None = None) -> list[dict[str, Any]]:
    result = []
    for source, row in rows(pattern):
        item_id = row["item_id"]
        entry = basic(item_id, kind, row, source)
        entry["power_dimensions"] = dimensions
        if tweak:
            entry["jem_tweaks"].append({"condition": tweak, "source": "JEMWorldBossTiers progression equipment rules"})
            entry["effective"]["expression"] = f"native ItemStack expression; then {tweak} when applicable"
        result.append(entry)
    return sorted(result, key=lambda value: value["id"])


def accessory_records() -> list[dict[str, Any]]:
    tokens = ("ring", "amulet", "necklace", "talisman", "charm", "bracelet", "belt", "curio", "relic")
    result = []
    for source, row in rows("registries/items/*.json"):
        item_id = row["registry_id"]
        path = item_id.partition(":")[2].lower()
        name = (row.get("display_name_en_us") or "").lower()
        if not any(token in path or token in name for token in tokens):
            continue
        entry = basic(item_id, "accessory_candidate", row, source)
        entry["power_dimensions"] = ["attribute modifiers", "spell power", "resistance", "resource economy", "conditional effects"]
        entry["assessment"]["reason"] = "Name/path identifies an accessory candidate; slot, Curios capability and runtime attributes remain unresolved until source-verified."
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


def spell_records(evidence: dict[str, Any]) -> list[dict[str, Any]]:
    verified = {row["spell_id"]: row for row in evidence["beam_candidates"]}
    names = irons_spell_names(evidence)
    result = []
    for spell_id, school in spell_ids():
        entry = {
            "id": spell_id,
            "kind": "spell",
            "display_name": names.get(spell_id) or verified.get(spell_id, {}).get("display_name"),
            "school": school,
            "power_dimensions": ["per-hit damage", "hit count", "cast time", "cooldown", "mana", "range/area", "control", "sustain"],
            "original": {
                "status": "source_verified_partial" if spell_id in verified else "unresolved",
                "expression": verified.get(spell_id, {}).get("native_damage_expression"),
                "known_dimensions": {key: value for key, value in verified.get(spell_id, {}).items() if key in {"rarity", "cast_type", "max_level", "base_spell_power", "spell_power_per_level", "cast_time_ticks"}},
                "unknown_is_zero": False
            },
            "modifiers": [{"owner": "Iron's Spells", "expression": "school resistance soft-cap multiplier after SpellDamageEvent"}],
            "jem_tweaks": [
                {"owner": "JEMWorldBossTiers", "expression": "PvE player cast x0.65/x0.80/x0.95/x1.10/x1.20 by World Tier; PvP x1.00"},
                {"owner": "JEMRoles", "expression": "Human direct-hit spell +1 when its native direct-hit conditions apply"}
            ],
            "effective": {
                "status": "partial" if spell_id in verified else "unresolved",
                "expression": "(native expression * PvE tier factor or PvP 1.00 + conditional Human direct-hit 1) * Iron school-resistance multiplier; then ordinary target mitigation"
            },
            "acquisition": {
                "status": "partial",
                "routes": ["irons_spellbooks:scroll ItemStack variant", "native randomize_spell loot pools"],
                "unresolved": "Exact per-spell loot probability and observed stack level are not proven by the registry item ID."
            },
            "assessment": {
                "status": "unverified" if spell_id not in verified else "source_reviewed_runtime_unverified",
                "reason": verified.get(spell_id, {}).get("unresolved", "Native formula is source-extracted; the observed ItemStack, caster attributes, target mitigation and runtime result remain unverified.")
            },
            "evidence": [{"type": "exact_jar", "source": evidence["artifact"]["file"], "sha256": evidence["artifact"]["sha256"]}]
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


def boss_records() -> list[dict[str, Any]]:
    directory = ROOT / "Mods" / "JEMPackCore" / "src" / "main" / "resources" / "data" / "jem_world_boss_tiers" / "jem_world_boss_tiers" / "bosses"
    profiles: dict[str, tuple[dict[str, Any], Path]] = {}
    for source in sorted(directory.glob("*.json")):
        for profile in load(source).get("bosses", []):
            for entity_id in profile["entity_ids"]:
                profiles[entity_id] = (profile, source)
    native = native_boss_tags()
    entity_names = {row["entity_id"]: row.get("display_name_en_us") for _, row in rows("views/mobs/*.json") if row.get("display_name_en_us")}
    result = []
    for entity_id in sorted(set(profiles) | set(native)):
        profile_source = profiles.get(entity_id)
        profile = profile_source[0] if profile_source else None
        source = profile_source[1] if profile_source else None
        registered = profile is not None
        result.append({
                "id": entity_id,
                "kind": "registered_boss" if registered else "native_boss_candidate",
                "profile_key": profile["key"] if profile else None,
                "display_name": profile.get("display_name") if profile else entity_names.get(entity_id),
                "jem_registered_profile": registered,
                "native_boss_tag": entity_id in native,
                "attacks": [{
                    "id": f"{entity_id}#native_attack_channels",
                    "status": "unresolved",
                    "expression": "native melee/projectile/explosion/beam/summon/custom damage channels must be enumerated from the owning exact-version mod",
                    "unknown_is_zero": False
                }],
                "original": {"status": "unresolved", "expression": "native boss attack-specific logic; ATTACK_DAMAGE alone is insufficient", "unknown_is_zero": False},
                "modifiers": [{"owner": "native boss mod", "expression": "projectile/explosion/beam/summon/custom-source behavior remains attack-specific"}],
                "jem_tweaks": ([
                    {"expression": "if scales_with_world_tier: outgoing damage x(1 + 0.15*tier); optional profile special multiplier; hosted participant/raid damage factor; hosted owned attacks x0.88"},
                    {"expression": "max health receives profile/generic tier attributes, then x(1 + 0.15*tier), then hosted participant/raid health factor"},
                    {"expression": "below 30% health: +15% movement speed and attack speed (not direct damage)"}
                ] if registered else [{"expression": "No JEM registered profile proven; native boss behavior remains authoritative."}]),
                "effective": {"status": "partial", "expression": "native attack expression * applicable registered boss tier/special/hosted factors; each non-attribute attack still needs source tracing"},
                "acquisition": {"status": "not_applicable", "routes": []},
                "assessment": {"status": "in_testing", "reason": ("JEM registration and shared wrapper are resolved; individual native attacks are not uniformly represented by entity ATTACK_DAMAGE." if registered else "Native boss tag is source-verified, but the entity is not in the canonical JEM Pack Core boss profiles.")},
                "evidence": (([{"type": "jem_owner", "source": source.relative_to(ROOT).as_posix(), "confidence": "source_verified"}] if source else [])
                             + [{"type": "native_boss_tag", **item, "confidence": "source_verified"} for item in native.get(entity_id, [])])
            })
    return result


def entity_records(boss_ids: set[str]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    allies, others = [], []
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


def main() -> int:
    snapshot = git_head()
    evidence = load(CATALOG / "power-sources" / "irons-spellbooks-3.15.6.json")
    evidence_jar = ROOT / evidence["artifact"]["canonical_path"]
    actual_sha256 = hashlib.sha256(evidence_jar.read_bytes()).hexdigest()
    if actual_sha256 != evidence["artifact"]["sha256"]:
        raise SystemExit(f"stale Iron's Spells power evidence: expected {evidence['artifact']['sha256']}, found {actual_sha256}")
    weapons = item_records("weapon_candidate", "views/weapons/*.json", ["damage", "attack speed", "reach/range", "area", "cooldown", "resource cost"], "progression enchantment conditional tier multiplier and critical")
    armor = item_records("armor_candidate", "views/armor/*.json", ["armor", "toughness", "knockback resistance", "damage-type mitigation", "durability"], "progression enchantment conditional slot bonuses")
    accessories = accessory_records()
    enchantments = enchantment_records()
    spells = spell_records(evidence)
    bosses = boss_records()
    hostiles, allies = entity_records({row["id"] for row in bosses})
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
        "warnings.json": ("regression_warnings", warnings(evidence))
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
        "fully_resolved": 0,
        "reason_no_full_resolution": "No static record proves every runtime ItemStack attribute, target mitigation, attack channel and acquisition context.",
        "evidence_levels": ["atlas_static", "source_verified", "exact_jar_bytecode", "runtime_observed"],
        "unresolved_is_zero": False,
        "known_gaps": [
            "Dynamic Java-only registrations and ItemStack/NBT variants",
            "Native weapon attributes and Epic Fight moveset timing for every weapon",
            "Native formula for every Iron's Spells spell beyond the priority beam candidates",
            "Every individual boss attack channel",
            "Entity hostility/taming/AI and native attributes for heuristic candidates",
            "Actual observed beam ItemStack spell ID, level, caster attributes and target resistances"
        ]
    }
    write("coverage.json", coverage)
    index = []
    for filename in ("weapons.json", "defenses.json", "accessories.json", "enchantments.json", "spells.json", "bosses.json", "entities.json", "allies.json"):
        data = load(OUTPUT / filename)
        for position, row in enumerate(data["records"]):
            index.append({"id": row["id"], "display_name": row.get("display_name"), "kind": row["kind"], "file": f"Catalog/views/power/{filename}", "record_index": position})
    write("index.json", {"schema_version": SCHEMA, "snapshot_sha": snapshot, "records": sorted(index, key=lambda value: (value["id"], value["kind"]))})
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

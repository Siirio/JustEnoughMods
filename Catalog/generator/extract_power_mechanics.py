#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
import tomllib
import zipfile
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"
MODS = ROOT / "Client" / "ManagedPack" / "mods"
OUTPUT = CATALOG / "power-sources" / "native-mechanics-phase3.json"
MAPPED_FORGE = (
    Path.home() / ".gradle" / "caches" / "forge_gradle" / "minecraft_user_repo" /
    "net" / "minecraftforge" / "forge" / "1.20.1-47.4.10_mapped_official_1.20.1" /
    "forge-1.20.1-47.4.10_mapped_official_1.20.1.jar"
)
JAVAP_CACHE: dict[tuple[str, str], str] = {}
JAVAP_VERBOSE_CACHE: dict[tuple[str, str], str] = {}
CLASS_CACHE: dict[str, list[str]] = {}
CLASS_BYTES_CACHE: dict[str, list[tuple[str, bytes]]] = {}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def atlas_rows(pattern: str) -> Iterable[dict[str, Any]]:
    for path in sorted(CATALOG.glob(pattern)):
        for row in load(path).get("records", []):
            yield row


def boss_reward_ids() -> set[str]:
    directory = (
        ROOT / "Mods" / "JEMPackCore" / "src" / "main" / "resources" / "data" /
        "jem_world_boss_tiers" / "jem_world_boss_tiers" / "rewards"
    )
    return {
        reward["item_id"]
        for filename in ("major_rewards.json", "expanded_rewards.json")
        for reward in load(directory / filename).get("rewards", [])
        if "weapon" in str(reward.get("category", "")).lower() and reward.get("item_id")
    }


def registry_items_by_id() -> dict[str, dict[str, Any]]:
    return {row["registry_id"]: row for row in atlas_rows("registries/items/*.json")}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def javap(jar: Path, class_name: str) -> str:
    key = (str(jar), class_name)
    if key in JAVAP_CACHE:
        return JAVAP_CACHE[key]
    result = subprocess.run(
        ["javap", "-classpath", str(jar), "-c", "-p", class_name],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    value = result.stdout if result.returncode == 0 else ""
    JAVAP_CACHE[key] = value
    return value


def javap_verbose(jar: Path, class_name: str) -> str:
    key = (str(jar), class_name)
    if key in JAVAP_VERBOSE_CACHE:
        return JAVAP_VERBOSE_CACHE[key]
    result = subprocess.run(
        ["javap", "-classpath", str(jar), "-v", "-p", class_name],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    value = result.stdout if result.returncode == 0 else ""
    JAVAP_VERBOSE_CACHE[key] = value
    return value


def class_names(jar: Path) -> list[str]:
    key = str(jar)
    if key in CLASS_CACHE:
        return CLASS_CACHE[key]
    with zipfile.ZipFile(jar) as archive:
        value = sorted(
            name[:-6].replace("/", ".")
            for name in archive.namelist()
            if name.endswith(".class") and not name.startswith("META-INF/versions/")
        )
    CLASS_CACHE[key] = value
    return value


def artifact_key(name: str) -> str:
    # Atlas paths encode spaces as `_20`, while the canonical pack keeps `%20` in
    # several CurseForge filenames. They identify the same exact artifact name.
    return re.sub(r"[^a-z0-9.]", "", re.sub(r"(?i)(?:%20|_20)", "", name).lower())


def camel_to_snake(value: str) -> str:
    value = re.sub(r"Spell$", "", value)
    value = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1_\2", value)
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", value)
    return value.replace("$", "_").lower()


def method_body(disassembly: str, method_name: str) -> str:
    pattern = re.compile(
        rf"(?m)^  .*\b{re.escape(method_name)}\([^\n]*\);\r?\n    Code:\r?\n(?P<body>(?:[ \t]+\d+:.*\r?\n)+)"
    )
    match = pattern.search(disassembly)
    return match.group("body") if match else ""


def method_sections(disassembly: str) -> list[dict[str, str]]:
    """Return bytecode methods without assigning meaning to arbitrary constants."""
    pattern = re.compile(
        r"(?m)^  (?P<declaration>(?:public|protected|private|static|final|synchronized|native|abstract|strictfp|synthetic|bridge|[A-Za-z0-9_.$<>\[\]/])+[^\n]*\([^\n]*\)(?: throws [^;]+)?);\r?\n"
        r"    Code:\r?\n(?P<body>(?:[ \t]+\d+:.*\r?\n)+)"
    )
    result = []
    for match in pattern.finditer(disassembly):
        declaration = match.group("declaration").strip()
        name_match = re.search(r"([A-Za-z0-9_$<>]+)\([^()]*\)(?: throws .*)?$", declaration)
        result.append({
            "declaration": declaration,
            "method": name_match.group(1) if name_match else declaration,
            "body": match.group("body"),
        })
    return result


def referenced_classes(disassembly: str) -> list[str]:
    # A class literal, instanceof/checkcast or registry field is not evidence that
    # the referenced class implements this owner's attack. Follow only an actual
    # construction or method invocation from executable bytecode.
    values: set[str] = set()
    for line in disassembly.splitlines():
        if re.search(r"\bnew\s+#", line):
            match = re.search(r"// class ([A-Za-z0-9_$/]+)", line)
            if match:
                values.add(match.group(1))
        if re.search(r"\binvoke(?:virtual|special|static|interface)\b", line):
            match = re.search(r"// (?:Interface)?Method ([A-Za-z0-9_$/]+)\.", line)
            if match:
                values.add(match.group(1))
    return sorted(value.replace("/", ".") for value in values)


def delegated_owner_texts(
    jar: Path,
    roots: list[str],
    *,
    depth: int = 2,
    limit: int = 16,
) -> dict[str, str]:
    """Follow same-artifact combat collaborators, bounded and deterministic."""
    available = set(class_names(jar))
    root_prefixes = {root.rsplit(".", 2)[0] for root in roots if "." in root}
    result: dict[str, str] = {}
    frontier = [(root, 0) for root in roots]
    seen = set(roots)
    combat_tokens = (
        "projectile", "effect", "goal", "attack", "ability", "skill", "weapon",
        "item", "entity", "spell", "beam", "laser", "strike", "missile", "bullet",
        "arrow", "spear", "explosion", "damage", "combo", "animation",
    )
    while frontier and len(result) < limit:
        owner, level = frontier.pop(0)
        text = javap(jar, owner)
        result[owner] = text
        if level >= depth:
            continue
        for referenced in referenced_classes(text):
            lowered = referenced.lower()
            if referenced in seen or referenced not in available:
                continue
            if any(part in lowered for part in (".client.", ".render.", ".model.", ".particle.")):
                continue
            same_package = any(referenced.startswith(prefix) for prefix in root_prefixes)
            if not same_package or not any(token in lowered for token in combat_tokens):
                continue
            seen.add(referenced)
            frontier.append((referenced, level + 1))
    return result


def flatten_toml(value: Any, prefix: tuple[str, ...] = ()) -> Iterable[tuple[tuple[str, ...], Any]]:
    if isinstance(value, dict):
        for key, child in value.items():
            yield from flatten_toml(child, prefix + (str(key),))
    elif isinstance(value, (str, int, float, bool)):
        yield prefix, value


def installed_config_values() -> list[dict[str, Any]]:
    records = []
    config_root = ROOT / "Client" / "ManagedPack" / "config"
    for path in sorted(config_root.rglob("*.toml")):
        try:
            data = tomllib.loads(path.read_text(encoding="utf-8-sig"))
        except (OSError, UnicodeDecodeError, tomllib.TOMLDecodeError):
            continue
        for key_path, value in flatten_toml(data):
            records.append({
                "key": ".".join(key_path),
                "normalized_key": re.sub(r"[^a-z0-9]", "", ".".join(key_path).lower()),
                "value": value,
                "source": path.relative_to(ROOT).as_posix(),
            })
    return records


def config_references(text: str, config_values: list[dict[str, Any]]) -> list[dict[str, Any]]:
    references = []
    seen: set[tuple[str, str]] = set()
    for owner, field in re.findall(
        r"// Field ([A-Za-z0-9_$/]*[Cc]onfig[A-Za-z0-9_$/]*)\.([A-Za-z0-9_$]+):",
        text,
    ):
        owner_name = owner.rsplit("/", 1)[-1].replace("$", ".")
        owner_tokens = [
            token.lower() for token in re.split(r"[^A-Za-z0-9]+", owner_name)
            if len(token) >= 4 and token.lower() not in {"config", "common", "server", "client"}
        ]
        field_normalized = re.sub(r"[^a-z0-9]", "", field.lower())
        matches = [
            row for row in config_values
            if field_normalized in row["normalized_key"]
            and (not owner_tokens or any(token in row["normalized_key"] for token in owner_tokens))
        ]
        key = (owner, field)
        if key in seen:
            continue
        seen.add(key)
        references.append({
            "owner": owner.replace("/", "."),
            "field": field,
            "status": "exact_installed_config" if len(matches) == 1 else "config_key_ambiguous_or_missing",
            "matches": matches,
            "unresolved_reason": None if len(matches) == 1 else (
                f"expected one installed TOML key for {owner_name}.{field}, found {len(matches)}"
            ),
        })
    return references


DAMAGE_SINKS = {
    "hurt": (".hurt:", ".m_6469_:"),
    "projectile_damage": ("setDamage:", "setBaseDamage:"),
    "explosion": (".explode:", "Explosion.<init>"),
    "effect": ("MobEffectInstance.\"<init>\"", "addEffect:"),
    "cooldown": ("addCooldown:", "ItemCooldowns.m_41524_:"),
    "durability_cost": ("hurtAndBreak:", "ItemStack.m_41622_:"),
    "healing": (".heal:", ".m_5634_:"),
}


def combat_method_records(
    owner_texts: dict[str, str],
    config_values: list[dict[str, Any]],
    *,
    limit: int = 80,
) -> list[dict[str, Any]]:
    records = []
    for owner, text in owner_texts.items():
        owner_config = config_references(text, config_values)
        for section in method_sections(text):
            body = section["body"]
            sinks = [
                semantic for semantic, needles in DAMAGE_SINKS.items()
                if any(needle in body for needle in needles)
            ]
            if not sinks:
                continue
            constants = sorted(
                {value for line in body.splitlines() if (value := bytecode_number(line)) is not None},
                key=float,
            )
            calls = sorted(set(re.findall(r"// (?:Interface)?Method ([^:]+):", body)))
            method_config = [row for row in owner_config if row["field"] in body]
            body_lines = body.splitlines()
            literal_arguments = []
            sink_occurrences: Counter[str] = Counter()
            for body_index, body_line in enumerate(body_lines):
                for semantic, needles in DAMAGE_SINKS.items():
                    if not any(needle in body_line for needle in needles):
                        continue
                    sink_occurrences[semantic] += 1
                    value = bytecode_number(body_lines[body_index - 1]) if body_index else None
                    if value is not None:
                        literal_arguments.append({
                            "sink": semantic,
                            "value": value,
                            "status": "exact_literal_immediately_before_sink",
                        })
            damaging_sinks = [sink for sink in sinks if sink in {"hurt", "projectile_damage", "healing"}]
            formula_status = (
                "resolved_literal_sink_argument"
                if damaging_sinks and all(
                    sum(argument["sink"] == sink for argument in literal_arguments) == sink_occurrences[sink]
                    for sink in damaging_sinks
                )
                else "sink_and_inputs_traced_formula_unresolved"
            )
            records.append({
                "owner_class": owner,
                "method": section["method"],
                "declaration": section["declaration"],
                "damage_or_defense_sinks": sinks,
                "numeric_constants": constants,
                "literal_sink_arguments": literal_arguments,
                "config_references": method_config,
                "calls": calls,
                "formula_status": formula_status,
                "unresolved_reason": None if formula_status.startswith("resolved") else (
                    "The exact damage/defense sink and inputs are located, but arbitrary control flow is not "
                    "collapsed into one value unless the safe symbolic evaluator proves the expression."
                ),
                "runtime_observation_required": False,
            })
            if len(records) >= limit:
                return records
    return records


def bytecode_number(line: str) -> int | float | None:
    instruction = line.split("//", 1)[0]
    match = re.search(r"\biconst_m1\b", instruction)
    if match:
        return -1
    match = re.search(r"\biconst_([0-5])\b", instruction)
    if match:
        return int(match.group(1))
    match = re.search(r"\bfconst_([0-2])\b", instruction)
    if match:
        return float(match.group(1))
    match = re.search(r"\bdconst_([01])\b", instruction)
    if match:
        return float(match.group(1))
    match = re.search(r"\b(?:bipush|sipush)\s+(-?\d+)\b", instruction)
    if match:
        return int(match.group(1))
    match = re.search(r"//\s+(?:float|double|int)\s+(-?\d+(?:\.\d+)?)(?:[df])?\b", line)
    if match:
        number = float(match.group(1))
        return int(number) if number.is_integer() else number
    return None


def preceding_number(lines: list[str], index: int) -> int | float | None:
    for line in reversed(lines[max(0, index - 5):index]):
        value = bytecode_number(line)
        if value is not None:
            return value
    return None


def spell_metadata(disassembly: str) -> dict[str, Any]:
    lines = disassembly.splitlines()
    result: dict[str, Any] = {}
    setters = {
        "setMaxLevel:": "max_level",
        "setCooldownSeconds:": "cooldown_seconds",
        "Field manaCostPerLevel:": "mana_cost_per_level",
        "Field baseSpellPower:": "base_spell_power",
        "Field spellPowerPerLevel:": "spell_power_per_level",
        "Field castTime:": "cast_time_ticks",
        "Field baseManaCost:": "base_mana_cost",
    }
    for index, line in enumerate(lines):
        for marker, key in setters.items():
            if marker in line:
                value = preceding_number(lines, index)
                if value is not None:
                    result[key] = value
        rarity = re.search(r"SpellRarity\.([A-Z_]+):", line)
        if rarity:
            result["rarity"] = rarity.group(1).lower()
        school = re.search(r"SchoolRegistry\.([A-Z_]+)_RESOURCE:", line)
        if school:
            result["school"] = school.group(1).lower()
    cast = re.search(r"CastType\.([A-Z_]+):", method_body(disassembly, "getCastType"))
    if cast:
        result["cast_type"] = cast.group(1).lower()
    if "base_mana_cost" in result and "mana_cost_per_level" in result:
        result["mana_expression"] = (
            f"{result['base_mana_cost']} + {result['mana_cost_per_level']} * (level - 1)"
        )
    if "base_spell_power" in result and "spell_power_per_level" in result:
        result["spell_power_expression"] = (
            f"{result['base_spell_power']} + {result['spell_power_per_level']} * (level - 1), then caster spell-power modifiers"
        )
    return result


def symbolic_return(body: str) -> str | None:
    stack: list[str] = []
    for line in body.splitlines():
        instruction = line.split("//", 1)[0]
        number = bytecode_number(line)
        if number is not None:
            stack.append(str(number))
            continue
        if re.search(r"\biload_1\b", instruction):
            stack.append("level")
        elif re.search(r"\baload_0\b", instruction):
            stack.append("this")
        elif re.search(r"\baload_2\b", instruction):
            stack.append("caster")
        elif "Method getSpellPower:" in line:
            if len(stack) >= 3:
                del stack[-3:]
            stack.append("spell_power")
        elif re.search(r"\b[ifd]2[ifd]\b", instruction):
            continue
        elif re.search(r"\b(?:i|f|d)(add|sub|mul|div)\b", instruction):
            operation = re.search(r"\b(?:i|f|d)(add|sub|mul|div)\b", instruction).group(1)
            if len(stack) < 2:
                return None
            right, left = stack.pop(), stack.pop()
            symbol = {"add": "+", "sub": "-", "mul": "*", "div": "/"}[operation]
            stack.append(f"({left} {symbol} {right})")
        elif re.search(r"\b(?:i|f|d)return\b", instruction):
            return stack[-1] if stack else None
        elif "invoke" in instruction and "getSpellPower:" not in line:
            # A helper call changes stack semantics. Do not fabricate a formula.
            return None
    return None


SPELL_CHANNELS: dict[str, tuple[str, ...]] = {
    "direct_damage": (" getDamage:", "DamageSources", ".hurt:", "setDamage:"),
    "multi_hit_or_channel": ("CastType.CONTINUOUS", "onServerCastTick", "channel", "tickCount"),
    "beam": ("Sunbeam", "RayOf", "ray_of", "Electrocute", "Beam"),
    "area": ("getRadius:", "Explosion", ".explode:", "getEntitiesOfClass", ".inflate:"),
    "damage_over_time": ("setSecondsOnFire", "MobEffects.POISON", "MobEffects.WITHER", "DamageOverTime", "FIRE_TICK"),
    "healing": ("SpellHealEvent", ".heal:", "m_5634_"),
    "shield_or_absorption": ("ABSORPTION", "Shield", "shield"),
    "summon": ("Summon", "RaiseDead", "Summoned", "Necromancer"),
    "control": ("SLOWNESS", "LEVITATION", "Stun", "Root", "Telekinesis", "Knockback", "setDeltaMovement"),
    "projectile": ("Projectile", "AbstractMagicProjectile", "setDamage:", ".shoot:"),
}


BOSS_CHANNELS: dict[str, tuple[str, ...]] = {
    "melee": ("doHurtTarget", "ATTACK_DAMAGE", "MeleeAttack", "HurtByTarget"),
    "area": ("AreaAttack", "getEntitiesOfClass", ".inflate:", "AABB", "aoe"),
    "projectile": ("Projectile", ".shoot:", "Arrow", "Fireball", "Missile", "Bullet", "Spear"),
    "beam": ("Beam", "Laser", "Ray"),
    "explosion": ("Explosion", ".explode:", "explosion"),
    "magic_or_custom_damage": ("DamageSources", "DamageSource", "magic", "indirectMagic"),
    "damage_over_time": ("setSecondsOnFire", "POISON", "WITHER", "FIRE_TICK", "burn"),
    "summons": ("Summon", "Minion", "Reinforcement"),
    "grab_or_stun": ("Grab", "grab", "Stun", "stun", "immobil", "LEVITATION"),
    "healing_or_shield": (".heal:", "m_5634_", "Shield", "shield", "ABSORPTION"),
    "phase_or_enrage": ("Phase", "phase", "Enrage", "enrage", "berserk"),
}


ITEM_CHANNELS: dict[str, tuple[str, ...]] = {
    "attributes": ("AttributeModifier", "ATTACK_DAMAGE", "ATTACK_SPEED", "ARMOR_TOUGHNESS", "KNOCKBACK_RESISTANCE"),
    "special_attack_or_active": ("use:", "releaseUsing", "onUseTick", "hurtEnemy", "inventoryTick", "SpecialAttack", "Ability"),
    "projectile_or_range": ("Projectile", ".shoot:", "Arrow", "Fireball", "rayTrace", "clip"),
    "area": ("getEntitiesOfClass", ".inflate:", "AABB", "Explosion", ".explode:"),
    "effects_or_passive": ("MobEffectInstance", "addEffect", "Potion", "Effect", "inventoryTick"),
    "cooldown": ("Cooldown", "cooldown", "addCooldown"),
    "resource_cost": ("Mana", "mana", "Stamina", "stamina", "consume", "shrink"),
    "durability": ("durability", "setDamageValue", "hurtAndBreak", "maxDamage", "getMaxDamage"),
    "curios_capability": ("curios", "ICurio", "Curio", "SlotContext"),
}


def channel_records(text: str, patterns: dict[str, tuple[str, ...]], owner_reason: str) -> list[dict[str, Any]]:
    lowered = text.lower()
    records = []
    for channel, needles in patterns.items():
        matched = []
        for needle in needles:
            if re.fullmatch(r"[A-Za-z_]+", needle):
                present = re.search(rf"(?<![A-Za-z]){re.escape(needle)}(?![A-Za-z])", text, flags=re.IGNORECASE) is not None
            else:
                present = needle.lower() in lowered
            if present:
                matched.append(needle)
        matched = sorted(set(matched))
        if matched:
            records.append({
                "channel": channel,
                "status": "observed_in_owner_bytecode",
                "signals": matched,
            })
        else:
            records.append({
                "channel": channel,
                "status": "not_observed",
                "unresolved_reason": owner_reason,
            })
    return records


def spell_evidence(config_values: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    jar = MODS / "irons_spellbooks-1.20.1-3.15.6.jar"
    wanted = {
        row["id"].partition(":")[2]: row
        for row in load(CATALOG / "views" / "power" / "spells.json")["records"]
    }
    classes = [
        name for name in class_names(jar)
        if ".spells." in name and name.endswith("Spell")
        and ".api." not in name and not name.endswith(("AbstractSpell", "NoneSpell", "AbstractEldritchSpell"))
    ]
    by_id = {camel_to_snake(name.rsplit(".", 1)[-1]): name for name in classes}
    by_id["angel_wing"] = by_id.get("angel_wings")
    records = []
    for spell_id in sorted(wanted):
        owner = by_id.get(spell_id)
        if owner is None:
            records.append({
                "spell_id": f"irons_spellbooks:{spell_id}",
                "owner_class": None,
                "owner_status": "unresolved",
                "unresolved_reason": "no exact Spell class name matched the registered spell ID in the installed artifact",
                "channels": [],
                "metadata": {},
                "formulas": {},
                "delegated_classes": [],
                "effect_methods": [],
            })
            continue
        text = javap(jar, owner)
        owner_texts = delegated_owner_texts(jar, [owner], depth=1, limit=8)
        combined = "\n".join(owner_texts.values())
        metadata = spell_metadata(text)
        formulas: dict[str, Any] = {}
        for method in ("getDamage", "getHealing", "getRadius", "getDuration", "getRange"):
            body = method_body(text, method)
            if not body:
                continue
            expression = symbolic_return(body)
            formulas[method] = {
                "status": "resolved_simple_bytecode" if expression else "source_present_expression_unresolved",
                "expression": expression,
                "unresolved_reason": None if expression else f"{method} delegates to helpers or uses bytecode outside the safe symbolic subset",
                "constants": sorted({value for line in body.splitlines() if (value := bytecode_number(line)) is not None}, key=float),
                "calls": sorted(set(re.findall(r"// (?:Interface)?Method ([^:]+):", body))),
            }
        channels = channel_records(
            combined,
            SPELL_CHANNELS,
            "no matching signal was found in the exact installed spell owner class; delegated projectile/effect behavior may require its referenced class",
        )
        records.append({
            "spell_id": f"irons_spellbooks:{spell_id}",
            "owner_class": owner,
            "owner_status": "exact_installed_class",
            "metadata": metadata,
            "channels": channels,
            "formulas": formulas,
            "delegated_classes": sorted(set(owner_texts) - {owner}),
            "effect_methods": combat_method_records(owner_texts, config_values, limit=100),
        })
    artifact = {"file": jar.name, "sha256": sha256(jar), "class_count": len(classes)}
    return records, artifact


def boss_profiles() -> dict[str, dict[str, Any]]:
    directory = ROOT / "Mods" / "JEMPackCore" / "src" / "main" / "resources" / "data" / "jem_world_boss_tiers" / "jem_world_boss_tiers" / "bosses"
    result: dict[str, dict[str, Any]] = {}
    for source in sorted(directory.glob("*.json")):
        for profile in load(source).get("bosses", []):
            for entity_id in profile["entity_ids"]:
                result[entity_id] = {**profile, "profile_source": source.relative_to(ROOT).as_posix()}
    return result


def entity_sources() -> dict[str, str]:
    return {
        row["entity_id"]: row.get("source_jar")
        for row in atlas_rows("views/mobs/*.json")
        if row.get("source_jar")
    }


def boss_owner_candidates(entity_id: str, classes: list[str]) -> list[str]:
    path = entity_id.partition(":")[2]
    wanted = re.sub(r"[^a-z0-9]", "", path.lower())
    tokens = [token for token in path.lower().split("_") if len(token) > 2]
    scored: list[tuple[int, str]] = []
    for name in classes:
        lowered = name.lower()
        if any(part in lowered for part in (".client.", ".model.", ".render.")):
            continue
        simple = name.rsplit(".", 1)[-1].split("$", 1)[0]
        normalized = re.sub(r"[^a-z0-9]", "", simple.lower())
        score = 0
        if normalized in {wanted, wanted + "entity", "entity" + wanted}:
            score += 100
        score += sum(8 for token in tokens if token in lowered)
        if ".entity." in lowered or ".entities." in lowered:
            score += 12
        if simple.lower().endswith("entity"):
            score += 8
        if "$" in name:
            score -= 5
        if score >= max(16, len(tokens) * 8):
            scored.append((score, name))
    if not scored:
        return []
    scored.sort(key=lambda value: (-value[0], len(value[1]), value[1]))
    top = scored[0][0]
    return [name for score, name in scored if score >= top - 8][:4]


ENTITY_ATTRIBUTE_FIELDS = {
    "Attributes.f_22276_": "max_health",
    "Attributes.f_22279_": "movement_speed",
    "Attributes.f_22281_": "attack_damage",
    "Attributes.f_22284_": "armor",
    "Attributes.f_22285_": "armor_toughness",
    "Attributes.f_22278_": "knockback_resistance",
    "Attributes.MAX_HEALTH": "max_health",
    "Attributes.MOVEMENT_SPEED": "movement_speed",
    "Attributes.ATTACK_DAMAGE": "attack_damage",
    "Attributes.ARMOR": "armor",
    "Attributes.ARMOR_TOUGHNESS": "armor_toughness",
    "Attributes.KNOCKBACK_RESISTANCE": "knockback_resistance",
}


def entity_attribute_defaults(owner_texts: dict[str, str]) -> list[dict[str, Any]]:
    records = []
    for owner, text in owner_texts.items():
        lines = text.splitlines()
        for index, line in enumerate(lines):
            semantic = next((value for field, value in ENTITY_ATTRIBUTE_FIELDS.items() if field in line), None)
            if semantic is None:
                continue
            add_index = next(
                (
                    position
                    for position in range(index + 1, min(len(lines), index + 8))
                    if "AttributeSupplier$Builder" in lines[position]
                    and any(marker in lines[position] for marker in (".add:", ".m_22268_:"))
                ),
                None,
            )
            if add_index is None:
                continue
            value = numeric_before(lines, add_index, 6)
            if value is not None:
                records.append({
                    "attribute": semantic,
                    "value": value,
                    "owner_class": owner,
                    "status": "exact_attribute_builder_bytecode",
                })
    seen = set()
    return [row for row in records if not ((row["attribute"], row["value"], row["owner_class"]) in seen or seen.add((row["attribute"], row["value"], row["owner_class"])))]


def boss_evidence(config_values: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    current = load(CATALOG / "views" / "power" / "bosses.json")["records"]
    profiles = boss_profiles()
    sources = entity_sources()
    jars = {artifact_key(path.name): path for path in MODS.glob("*.jar")}
    cache: dict[str, list[str]] = {}
    artifacts: dict[str, dict[str, Any]] = {}
    records = []
    for current_row in current:
        entity_id = current_row["id"]
        profile = profiles.get(entity_id, {})
        jar_name = sources.get(entity_id)
        jar = jars.get(artifact_key(jar_name or ""))
        vanilla_owner = {
            "minecraft:ender_dragon": "net.minecraft.world.entity.boss.enderdragon.EnderDragon",
            "minecraft:wither": "net.minecraft.world.entity.boss.wither.WitherBoss",
        }.get(entity_id)
        if vanilla_owner and MAPPED_FORGE.exists():
            owner_texts = delegated_owner_texts(MAPPED_FORGE, [vanilla_owner], depth=1, limit=10)
            text = "\n".join(owner_texts.values())
            artifacts.setdefault(MAPPED_FORGE.name, {
                "file": MAPPED_FORGE.name,
                "sha256": sha256(MAPPED_FORGE),
                "evidence_role": "Forge 47.4.10 official-mapped Minecraft 1.20.1 dependency",
            })
            records.append({
                "entity_id": entity_id,
                "owner_artifact": MAPPED_FORGE.name,
                "owner_classes": [vanilla_owner],
                "delegated_classes": sorted(set(owner_texts) - {vanilla_owner}),
                "owner_status": "exact_mapped_dependency_class",
                "unresolved_reason": None,
                "profile": profile,
                "channels": channel_records(
                    text,
                    BOSS_CHANNELS,
                    "no matching signal was found in the exact mapped vanilla owner class; referenced projectiles/phases remain a named follow-up",
                ),
                "native_attributes": entity_attribute_defaults(owner_texts),
                "attack_methods": combat_method_records(owner_texts, config_values, limit=120),
            })
            continue
        if jar is None:
            records.append({
                "entity_id": entity_id,
                "owner_artifact": jar_name or "Minecraft 1.20.1 built-in",
                "owner_classes": [],
                "owner_status": "unresolved",
                "unresolved_reason": "owning installed mod class artifact was not linked by the static entity registry record",
                "profile": profile,
                "channels": channel_records("", BOSS_CHANNELS, "owner class unresolved, so this channel was not inspected"),
                "native_attributes": [],
                "attack_methods": [],
            })
            continue
        if jar.name not in cache:
            cache[jar.name] = class_names(jar)
            artifacts[jar.name] = {"file": jar.name, "sha256": sha256(jar)}
        owners = boss_owner_candidates(entity_id, cache[jar.name])
        owner_texts = delegated_owner_texts(jar, owners, depth=1, limit=10)
        combined = "\n".join(owner_texts.values())
        attacks = combat_method_records(owner_texts, config_values, limit=160)
        records.append({
            "entity_id": entity_id,
            "owner_artifact": jar.name,
            "owner_classes": owners,
            "delegated_classes": sorted(set(owner_texts) - set(owners)),
            "owner_status": "class_name_and_package_matched" if owners else "unresolved",
            "unresolved_reason": None if owners else "no server-side entity class matched the registered entity path strongly enough",
            "profile": profile,
            "channels": channel_records(
                combined,
                BOSS_CHANNELS,
                "no matching signal was found in the matched exact-version owner class; delegated goals/projectiles remain a named follow-up",
            ),
            "native_attributes": entity_attribute_defaults(owner_texts),
            "attack_methods": attacks,
            "attack_coverage": {
                "method_count": len(attacks),
                "numeric_formula_count": sum(row["formula_status"].startswith("resolved") for row in attacks),
                "status": "method_sinks_traced" if attacks else "no_attack_sink_observed_in_bounded_chain",
                "unresolved_reason": None if attacks else "No damage/effect/healing sink was found in the exact owner and bounded same-artifact call chain.",
            },
        })
    return records, sorted(artifacts.values(), key=lambda value: value["file"].lower())


def ally_evidence(config_values: list[dict[str, Any]]) -> list[dict[str, Any]]:
    current = load(CATALOG / "views" / "power" / "allies.json")["records"]
    sources = entity_sources()
    jars = {artifact_key(path.name): path for path in MODS.glob("*.jar")}
    cache: dict[str, list[str]] = {}
    records = []
    for row in current:
        entity_id = row["id"]
        jar_name = sources.get(entity_id)
        jar = jars.get(artifact_key(jar_name or ""))
        if jar is None:
            records.append({
                "entity_id": entity_id,
                "owner_artifact": jar_name,
                "owner_classes": [],
                "owner_status": "unresolved",
                "unresolved_reason": "owning class artifact was not linked; vanilla or dynamic entity remains explicit",
                "combat_channels": channel_records("", BOSS_CHANNELS, "owner class unresolved, so ally combat channel was not inspected"),
                "native_attributes": [],
                "attack_methods": [],
            })
            continue
        if jar.name not in cache:
            cache[jar.name] = class_names(jar)
        owners = boss_owner_candidates(entity_id, cache[jar.name])
        owner_texts = delegated_owner_texts(jar, owners, depth=1, limit=6)
        text = "\n".join(owner_texts.values())
        records.append({
            "entity_id": entity_id,
            "owner_artifact": jar.name,
            "owner_classes": owners,
            "delegated_classes": sorted(set(owner_texts) - set(owners)),
            "owner_status": "class_name_and_package_matched" if owners else "unresolved",
            "unresolved_reason": None if owners else "no server-side entity class matched the ally candidate ID strongly enough",
            "combat_channels": channel_records(
                text,
                BOSS_CHANNELS,
                "no matching signal was found in the matched ally class; inherited AI/goals remain unresolved",
            ),
            "native_attributes": entity_attribute_defaults(owner_texts),
            "attack_methods": combat_method_records(owner_texts, config_values, limit=100),
        })
    return records


def registration_classes(classes: list[str]) -> list[str]:
    result = []
    for name in classes:
        lowered = name.lower()
        simple = lowered.rsplit(".", 1)[-1]
        if any(part in lowered for part in (".client.", ".mixin.", ".model.", ".render.")):
            continue
        if "item" in simple and (
            any(token in simple for token in ("registry", "registries", "register", "items"))
            or (".registries." in lowered and simple.endswith("item"))
        ):
            result.append(name)
    return result[:40]


def bootstrap_factory_targets(verbose_disassembly: str) -> dict[str, dict[str, str]]:
    """Map an invokedynamic bootstrap index to its real lambda/constructor target."""
    marker = verbose_disassembly.find("BootstrapMethods:")
    if marker < 0:
        return {}
    table = verbose_disassembly[marker:]
    entries = list(re.finditer(r"(?m)^  (\d+): ", table))
    targets: dict[str, dict[str, str]] = {}
    for position, entry in enumerate(entries):
        block_end = entries[position + 1].start() if position + 1 < len(entries) else len(table)
        block = table[entry.start():block_end]
        lambda_target = re.search(
            r"REF_invokeStatic ([A-Za-z0-9_$/]+)\.(lambda\$[^:]+):([^\r\n]+)",
            block,
        )
        if lambda_target:
            targets[entry.group(1)] = {
                "kind": "lambda",
                "owner_class": lambda_target.group(1).replace("/", "."),
                "method": lambda_target.group(2),
                "descriptor": lambda_target.group(3),
            }
            continue
        constructor_target = re.search(
            r"REF_newInvokeSpecial ([A-Za-z0-9_$/]+)\.\"?<init>\"?:([^\r\n]+)",
            block,
        )
        if constructor_target:
            targets[entry.group(1)] = {
                "kind": "constructor",
                "owner_class": constructor_target.group(1).replace("/", "."),
                "method": "<init>",
                "descriptor": constructor_target.group(2),
            }
    return targets


def registration_factories(disassembly: str, verbose_disassembly: str) -> dict[str, dict[str, Any]]:
    """Resolve DeferredRegister literals to their invokedynamic factory bodies."""
    result: dict[str, dict[str, Any]] = {}
    lines = disassembly.splitlines()
    bootstrap_targets = bootstrap_factory_targets(verbose_disassembly)
    for index, line in enumerate(lines):
        match = re.search(r"// String ([a-z0-9_./-]+)$", line.strip())
        if not match:
            continue
        path = match.group(1)
        invoke = next(
            (
                re.search(r"InvokeDynamic #(\d+):", lines[position])
                for position in range(index + 1, min(len(lines), index + 8))
                if "InvokeDynamic #" in lines[position]
            ),
            None,
        )
        if invoke is None:
            continue
        bootstrap_index = invoke.group(1)
        target = bootstrap_targets.get(bootstrap_index)
        if target is None:
            continue
        factory_method = target["method"]
        body = method_body(disassembly, factory_method) if target["kind"] == "lambda" else ""
        implementation = re.search(r"// class ([A-Za-z0-9_$/]+)", body)
        body_lines = body.splitlines()
        constructors = []
        for body_index, body_line in enumerate(body_lines):
            constructor = re.search(r"// Method ([A-Za-z0-9_$/]+)\.\"<init>\":(\([^)]*\).)$", body_line)
            if not constructor:
                continue
            constructors.append({
                "owner_class": constructor.group(1).replace("/", "."),
                "descriptor": constructor.group(2),
                "preceding_numeric_sequence": [
                    value for candidate in body_lines[max(0, body_index - 18):body_index]
                    if (value := bytecode_number(candidate)) is not None
                ],
            })
        if target["kind"] == "constructor":
            constructors.append({
                "owner_class": target["owner_class"],
                "descriptor": target["descriptor"],
                "preceding_numeric_sequence": [],
            })
        implementation_class = (
            implementation.group(1).replace("/", ".")
            if implementation else target["owner_class"]
        )
        result[path] = {
            "factory_method": factory_method,
            "bootstrap_index": int(bootstrap_index),
            "implementation_class": implementation_class,
            "constructor_calls": constructors,
            "field_references": [
                {"owner_class": field_owner.replace("/", "."), "field": field, "descriptor": descriptor}
                for field_owner, field, descriptor in re.findall(
                    r"// Field ([A-Za-z0-9_$/]+)\.([A-Za-z0-9_$]+):(L[A-Za-z0-9_$/]+;)",
                    body,
                )
            ],
            "numeric_constants": sorted(
                {value for candidate in body.splitlines() if (value := bytecode_number(candidate)) is not None},
                key=float,
            ),
            "status": "exact_registration_factory" if body or target["kind"] == "constructor" else "factory_body_unresolved",
        }
    return result


def item_registrations() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    candidates = list(atlas_rows("views/weapons/*.json")) + list(atlas_rows("views/armor/*.json"))
    registry_items = registry_items_by_id()
    candidates.extend(registry_items[item_id] for item_id in sorted(boss_reward_ids()) if item_id in registry_items)
    accessory_tokens = ("ring", "amulet", "necklace", "talisman", "charm", "bracelet", "belt", "curio", "relic")
    candidates.extend(
        row for row in atlas_rows("registries/items/*.json")
        if any(token in row["registry_id"].partition(":")[2].lower() or token in (row.get("display_name_en_us") or "").lower()
               for token in accessory_tokens)
    )
    jar_mods: dict[str, set[str]] = defaultdict(set)
    candidate_ids_by_mod: dict[str, set[str]] = defaultdict(set)
    for row in candidates:
        if row.get("source_jar") and row.get("mod_id"):
            jar_mods[row["source_jar"]].add(row["mod_id"])
            candidate_ids_by_mod[row["mod_id"]].add(row.get("item_id") or row.get("registry_id"))
    jars = {artifact_key(path.name): path for path in MODS.glob("*.jar")}
    registrations: dict[str, dict[str, Any]] = {}
    artifacts = []
    for jar_name in sorted(jar_mods, key=str.lower):
        jar = jars.get(artifact_key(jar_name))
        if jar is None:
            continue
        classes = class_names(jar)
        owners = registration_classes(classes)
        if not owners:
            continue
        artifacts.append({"file": jar.name, "sha256": sha256(jar), "registration_classes": owners})
        for owner in owners:
            text = javap(jar, owner)
            factories = registration_factories(text, javap_verbose(jar, owner))
            lines = text.splitlines()
            literals = set(re.findall(r"// String ([a-z0-9_./-]+)$", text, flags=re.MULTILINE))
            for namespace in sorted(jar_mods[jar_name]):
                for item_id in candidate_ids_by_mod[namespace]:
                    if item_id.partition(":")[2] in literals:
                        factory = factories.get(item_id.partition(":")[2], {})
                        registrations.setdefault(item_id, {
                            "item_id": item_id,
                            "artifact": jar.name,
                            "owner_class": owner,
                            "status": "observed_literal_in_item_registry_class",
                            **factory,
                        })
            for index, line in enumerate(lines):
                if ".register:" not in line and ".register(" not in line:
                    continue
                previous = lines[max(0, index - 8):index]
                strings = [
                    match.group(1)
                    for candidate in previous
                    if (match := re.search(r"// String ([a-z0-9_./-]+)$", candidate.strip()))
                ]
                if not strings:
                    continue
                path = strings[-1]
                if path.startswith(("item.", "block.")) or path.count(":") > 1:
                    continue
                for namespace in sorted(jar_mods[jar_name]):
                    item_id = path if ":" in path else f"{namespace}:{path}"
                    factory = factories.get(path, {})
                    registrations.setdefault(item_id, {
                        "item_id": item_id,
                        "artifact": jar.name,
                        "owner_class": owner,
                        "status": "observed_string_argument_to_register",
                        **factory,
                    })
    return sorted(registrations.values(), key=lambda value: value["item_id"]), artifacts


def forge_tier_values(jar: Path, owner_class: str, field: str) -> dict[str, Any] | None:
    vanilla_tiers = {
        "WOOD": (0, 59, 2.0, 0.0, 15),
        "STONE": (1, 131, 4.0, 1.0, 5),
        "IRON": (2, 250, 6.0, 2.0, 14),
        "DIAMOND": (3, 1561, 8.0, 3.0, 10),
        "GOLD": (0, 32, 12.0, 0.0, 22),
        "NETHERITE": (4, 2031, 9.0, 4.0, 15),
    }
    if owner_class == "net.minecraft.world.item.Tiers" and field in vanilla_tiers:
        level, uses, speed, attack_bonus, enchantability = vanilla_tiers[field]
        return {
            "owner_class": owner_class,
            "field": field,
            "level": level,
            "uses": uses,
            "speed": speed,
            "attack_damage_bonus": attack_bonus,
            "enchantability": enchantability,
            "status": "exact_minecraft_1_20_1_tier_enum",
        }
    text = javap(jar, owner_class)
    body = method_body(text, "static")
    if not body:
        static_match = re.search(r"(?m)^  static \{\};\r?\n    Code:\r?\n(?P<body>(?:[ \t]+\d+:.*\r?\n)+)", text)
        body = static_match.group("body") if static_match else ""
    lines = body.splitlines()
    target = next((index for index, line in enumerate(lines) if f"Field {field}:Lnet/minecraft/world/item/Tier;" in line), None)
    if target is None:
        return None
    constructor = next(
        (index for index in range(target - 1, max(-1, target - 80), -1) if "ForgeTier.\"<init>\":(IIFFI" in lines[index]),
        None,
    )
    if constructor is None:
        return None
    start = next((index for index in range(constructor - 1, max(-1, constructor - 30), -1) if "class net/minecraftforge/common/ForgeTier" in lines[index]), max(0, constructor - 20))
    values = [
        value for line in lines[start:constructor]
        if (value := bytecode_number(line)) is not None
    ]
    if len(values) < 5:
        return None
    level, uses, speed, attack_bonus, enchantability = values[-5:]
    return {
        "owner_class": owner_class,
        "field": field,
        "level": int(level),
        "uses": int(uses),
        "speed": speed,
        "attack_damage_bonus": attack_bonus,
        "enchantability": int(enchantability),
        "status": "exact_forge_tier_static_initializer",
    }


def item_owner_candidates(item_id: str, classes: list[str]) -> list[str]:
    path = item_id.partition(":")[2]
    tokens = [
        token for token in re.split(r"[/_.-]+", path.lower())
        if len(token) > 2 and token not in {"helmet", "chestplate", "leggings", "boots", "inventory", "item"}
    ]
    compact = "".join(tokens)
    scored: list[tuple[int, str]] = []
    for name in classes:
        lowered = name.lower()
        if any(part in lowered for part in (".client.", ".model.", ".render.", ".mixin.")):
            continue
        simple = name.rsplit(".", 1)[-1].split("$", 1)[0]
        normalized = re.sub(r"[^a-z0-9]", "", simple.lower())
        score = sum(7 for token in tokens if token in normalized)
        if compact and compact in normalized:
            score += 40
        if simple.lower().endswith(("item", "armor", "weapon", "sword", "bow", "staff")):
            score += 8
        if "$" in name:
            score -= 4
        if score >= max(14, len(tokens) * 5):
            scored.append((score, name))
    if not scored:
        return []
    scored.sort(key=lambda value: (-value[0], len(value[1]), value[1]))
    top = scored[0][0]
    return [name for score, name in scored if score >= top - 5][:3]


def item_mechanics(
    registrations: list[dict[str, Any]],
    config_values: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    registration_map = {row["item_id"]: row for row in registrations}
    targets: dict[str, str] = {}
    for row in atlas_rows("views/weapons/*.json"):
        if row["item_id"] in registration_map:
            targets[row["item_id"]] = "weapon"
    for row in atlas_rows("views/armor/*.json"):
        if row["item_id"] in registration_map:
            targets[row["item_id"]] = "armor"
    for item_id in boss_reward_ids():
        if item_id in registration_map:
            targets[item_id] = "weapon"
    accessory_tokens = ("ring", "amulet", "necklace", "talisman", "charm", "bracelet", "belt", "curio", "relic")
    for row in atlas_rows("registries/items/*.json"):
        item_id = row["registry_id"]
        if item_id in registration_map and any(token in item_id.partition(":")[2].lower() or token in (row.get("display_name_en_us") or "").lower() for token in accessory_tokens):
            targets.setdefault(item_id, "accessory")
    jars = {path.name: path for path in MODS.glob("*.jar")}
    class_cache: dict[str, list[str]] = {}
    disassembly_cache: dict[tuple[str, str], str] = {}
    records = []
    for item_id, kind in sorted(targets.items()):
        registration = registration_map[item_id]
        jar = jars.get(registration["artifact"])
        if jar is None:
            continue
        if jar.name not in class_cache:
            class_cache[jar.name] = class_names(jar)
        implementation = registration.get("implementation_class")
        implementation_jar = jar
        owners = [implementation] if implementation in set(class_cache[jar.name]) else []
        if not owners and implementation and implementation.startswith("net.minecraft.") and MAPPED_FORGE.exists():
            owners = [implementation]
            implementation_jar = MAPPED_FORGE
        ownership_status = "exact_registration_factory" if owners else "class_name_and_package_matched"
        if not owners:
            owners = item_owner_candidates(item_id, class_cache[jar.name])
        owner_texts = (
            {owner: javap(implementation_jar, owner) for owner in owners}
            if implementation_jar == MAPPED_FORGE
            else delegated_owner_texts(jar, owners, depth=1, limit=6)
        )
        direct_owner_texts = {owner: owner_texts.get(owner, "") for owner in owners}
        text = "\n".join(owner_texts.values())
        constants = sorted({value for line in text.splitlines() if (value := bytecode_number(line)) is not None}, key=float)
        exact_mechanics = extract_item_numeric_mechanics(direct_owner_texts, registration, config_values, jar)
        methods = combat_method_records(owner_texts, config_values)
        records.append({
            "item_id": item_id,
            "kind": kind,
            "artifact": jar.name,
            "implementation_artifact": implementation_jar.name,
            "registration": registration,
            "owner_classes": owners,
            "delegated_classes": sorted(set(owner_texts) - set(owners)),
            "owner_status": ownership_status if owners else "registered_item_owner_class_unresolved",
            "unresolved_reason": None if owners else "the ItemStack is registered, but no specialized implementation class matched its ID; it may use a shared/base Item class or helper factory",
            "mechanics": channel_records(
                text,
                ITEM_CHANNELS,
                "no matching signal was found in the matched item implementation; inherited/shared behavior or data-driven Epic Fight capability remains unresolved",
            ),
            "numeric_constants": constants[:80],
            "exact_numeric_mechanics": exact_mechanics,
            "special_attack_methods": methods,
            "special_attack_coverage": {
                "method_count": len(methods),
                "numeric_formula_count": sum(row["formula_status"].startswith("resolved") for row in methods),
                "status": "method_sinks_traced" if methods else "no_special_sink_observed_in_bounded_chain",
                "unresolved_reason": None if methods else (
                    "No damage/effect/cooldown/durability sink was observed in the exact factory owner and bounded same-artifact call chain."
                ),
            },
        })
    return records


ATTRIBUTE_FIELDS = {
    "Attributes.f_22278_": "knockback_resistance_addition",
    "Attributes.f_22281_": "attack_damage_addition",
    "Attributes.f_22283_": "attack_speed_addition",
    "Attributes.f_22284_": "armor_addition",
    "Attributes.f_22285_": "armor_toughness_addition",
    "ForgeMod.ENTITY_REACH": "entity_reach_addition",
    "ForgeMod.BLOCK_REACH": "block_reach_addition",
}


def numeric_before(lines: list[str], index: int, window: int = 8) -> int | float | None:
    for position in range(index - 1, max(-1, index - window - 1), -1):
        value = bytecode_number(lines[position])
        if value is not None:
            return value
    return None


def extract_item_numeric_mechanics(
    owner_texts: dict[str, str],
    registration: dict[str, Any],
    config_values: list[dict[str, Any]],
    registration_jar: Path,
) -> dict[str, Any]:
    attributes: list[dict[str, Any]] = []
    durability_values: list[dict[str, Any]] = []
    durability_costs: list[dict[str, Any]] = []
    cooldowns: list[dict[str, Any]] = []
    config_dependencies: list[dict[str, Any]] = []
    tier_values: list[dict[str, Any]] = []
    for owner, text in owner_texts.items():
        lines = text.splitlines()
        for index, line in enumerate(lines):
            for field, semantic in ATTRIBUTE_FIELDS.items():
                if field not in line:
                    continue
                end = min(len(lines), index + 18)
                modifier_index = next((p for p in range(index + 1, end) if 'AttributeModifier."<init>"' in lines[p]), None)
                if modifier_index is not None:
                    value = numeric_before(lines, modifier_index, 10)
                    if value is not None:
                        attributes.append({"attribute": semantic, "value": value, "owner_class": owner, "status": "exact_owner_bytecode"})
            if "Item$Properties.m_41503_:(I)" in line or "Item$Properties.durability:(I)" in line:
                value = numeric_before(lines, index, 3)
                if value is not None:
                    durability_values.append({"max_damage": int(value), "owner_class": owner, "status": "exact_owner_bytecode"})
            if "ItemStack.m_41622_:(I" in line or "ItemStack.hurtAndBreak:(I" in line:
                value = numeric_before(lines, index, 5)
                if value is not None:
                    durability_costs.append({"cost": int(value), "owner_class": owner, "status": "exact_owner_bytecode"})
            if "ItemCooldowns.m_41524_" in line or "ItemCooldowns.addCooldown" in line:
                value = numeric_before(lines, index, 5)
                if value is not None:
                    cooldowns.append({"ticks": int(value), "seconds": round(float(value) / 20.0, 3), "owner_class": owner, "status": "exact_owner_bytecode"})
        config_dependencies.extend(config_references(text, config_values))

        constructor = re.search(
            r"public [^(]+\(net\.minecraft\.world\.item\.Item\$Properties\);\r?\n    Code:\r?\n(?P<body>(?:[ \t]+\d+:.*\r?\n)+)",
            text,
        )
        if constructor:
            body_lines = constructor.group("body").splitlines()
            for index, line in enumerate(body_lines):
                if "Cataclysm_Weapon_Item.\"<init>\":(Lnet/minecraft/world/item/Item$Properties;DD)V" in line:
                    values = [bytecode_number(candidate) for candidate in body_lines[max(0, index - 6):index]]
                    values = [value for value in values if value is not None]
                    if len(values) >= 2:
                        attributes.extend([
                            {"attribute": "attack_damage_addition", "value": values[-2], "owner_class": owner, "status": "exact_constructor_argument"},
                            {"attribute": "attack_speed_addition", "value": values[-1], "owner_class": owner, "status": "exact_constructor_argument"},
                        ])

    for constructor in registration.get("constructor_calls", []):
        descriptor = constructor.get("descriptor", "")
        values = constructor.get("preceding_numeric_sequence", [])
        if re.search(r"Tier;[IF]FLnet/minecraft/world/item/Item\$Properties;\)V$", descriptor) and len(values) >= 2:
            tier_reference = next(
                (
                    reference for reference in registration.get("field_references", [])
                    if reference.get("descriptor") in {
                        "Lnet/minecraft/world/item/Tier;",
                        "Lnet/minecraft/world/item/Tiers;",
                    }
                ),
                None,
            )
            tier = forge_tier_values(registration_jar, tier_reference["owner_class"], tier_reference["field"]) if tier_reference else None
            if tier:
                tier_values.append(tier)
                durability_values.append({
                    "max_damage": int(tier["uses"]),
                    "owner_class": tier["owner_class"],
                    "status": tier["status"],
                    "source_field": tier["field"],
                })
            attributes.extend([
                {
                    "attribute": "factory_attack_damage_parameter",
                    "value": values[-2],
                    "owner_class": constructor["owner_class"],
                    "status": "exact_registration_constructor_argument",
                    "semantic_boundary": "SwordItem-style damage parameter; total modifier also includes the exact Tier attack bonus.",
                },
                {
                    "attribute": "attack_speed_addition",
                    "value": values[-1],
                    "owner_class": constructor["owner_class"],
                    "status": "exact_registration_constructor_argument",
                },
            ])
            if tier:
                attributes.append({
                    "attribute": "attack_damage_addition",
                    "value": float(values[-2]) + float(tier["attack_damage_bonus"]),
                    "owner_class": constructor["owner_class"],
                    "status": "exact_registration_constructor_plus_tier",
                    "components": {
                        "factory_parameter": values[-2],
                        "tier_attack_damage_bonus": tier["attack_damage_bonus"],
                    },
                })

    def unique(records: list[dict[str, Any]], fields: tuple[str, ...]) -> list[dict[str, Any]]:
        seen: set[tuple[Any, ...]] = set()
        result = []
        for record in records:
            key = tuple(record.get(field) for field in fields)
            if key not in seen:
                seen.add(key)
                result.append(record)
        return result

    return {
        "attributes": unique(attributes, ("attribute", "value", "owner_class")),
        "durability": unique(durability_values, ("max_damage", "owner_class")),
        "durability_costs": unique(durability_costs, ("cost", "owner_class")),
        "cooldowns": unique(cooldowns, ("ticks", "owner_class")),
        "config_dependencies": unique(config_dependencies, ("owner", "field")),
        "tier_values": unique(tier_values, ("owner_class", "field")),
        "absence_policy": "An empty list means not observed in the matched owner class, never numeric zero or proof that the mechanic is absent.",
    }


def animation_timing_records(jar: Path) -> list[dict[str, Any]]:
    records: dict[str, dict[str, Any]] = {}
    try:
        with zipfile.ZipFile(jar) as archive:
            names = set(archive.namelist())
            for name in sorted(names):
                match = re.fullmatch(r"assets/([^/]+)/animmodels/animations/(.+)\.json", name)
                if not match or "/data/" in name:
                    continue
                try:
                    value = json.loads(archive.read(name).decode("utf-8-sig"))
                except (UnicodeDecodeError, json.JSONDecodeError):
                    continue
                times = [
                    float(moment)
                    for joint in value.get("animation", []) if isinstance(joint, dict)
                    for moment in joint.get("time", []) if isinstance(moment, (int, float))
                ]
                data_path = name.replace("/animations/", "/animations/")
                parent, filename = name.rsplit("/", 1)
                sidecar = f"{parent}/data/{filename}"
                trails = []
                if sidecar in names:
                    try:
                        sidecar_value = json.loads(archive.read(sidecar).decode("utf-8-sig"))
                        trails = sidecar_value.get("trail_effects", []) if isinstance(sidecar_value, dict) else []
                    except (UnicodeDecodeError, json.JSONDecodeError):
                        trails = []
                resource_id = f"{match.group(1)}:{match.group(2)}"
                records[resource_id] = {
                    "animation_id": resource_id,
                    "artifact": jar.name,
                    "path": name,
                    "duration_seconds": round(max(times), 4) if times else None,
                    "trail_windows": trails,
                    "timing_status": "exact_animation_keyframes" if times else "animation_time_unresolved",
                    "semantic_boundary": "Keyframe/trail timing is presentation evidence; it is not an attack hit window unless the owning Java animation class binds an attack phase.",
                }
    except zipfile.BadZipFile:
        return []
    return list(records.values())


def capability_type_code_evidence(jar: Path, type_id: str) -> dict[str, Any]:
    path = type_id.partition(":")[2]
    token = path.encode("utf-8").lower()
    owners = []
    try:
        cache_key = str(jar)
        if cache_key not in CLASS_BYTES_CACHE:
            with zipfile.ZipFile(jar) as archive:
                CLASS_BYTES_CACHE[cache_key] = [
                    (name[:-6].replace("/", "."), archive.read(name).lower())
                    for name in archive.namelist()
                    if name.endswith(".class") and not name.startswith("META-INF/versions/")
                ]
        for class_name, bytecode in CLASS_BYTES_CACHE[cache_key]:
            if token in bytecode:
                owners.append(class_name)
                if len(owners) >= 8:
                    break
    except zipfile.BadZipFile:
        return {"type_id": type_id, "owner_classes": [], "animation_fields": []}
    animation_fields = set()
    combo_nodes = set()
    for owner in owners:
        text = javap(jar, owner)
        animation_fields.update(re.findall(r"Field ([A-Za-z0-9_$/]*[Aa]nimation[A-Za-z0-9_$/]*)\.([A-Za-z0-9_$]+):", text))
        combo_nodes.update(re.findall(r"Field ([A-Za-z0-9_$/]*[Cc]ombo[A-Za-z0-9_$/]*)\.([A-Za-z0-9_$]+):", text))
    return {
        "type_id": type_id,
        "owner_classes": owners,
        "animation_fields": sorted(f"{owner.replace('/', '.')}.{field}" for owner, field in animation_fields),
        "combo_nodes": sorted(f"{owner.replace('/', '.')}.{field}" for owner, field in combo_nodes),
        "status": "exact_type_code_references" if owners else "type_code_owner_unresolved",
        "damage_formula_status": "unresolved_until_animation_phase_properties_and_skill_control_flow_are_composed",
    }


def data_driven_combat() -> dict[str, Any]:
    raw_weapon_attributes: dict[str, dict[str, Any]] = {}
    capabilities: list[dict[str, Any]] = []
    skill_parameters: dict[str, dict[str, Any]] = {}
    artifacts: dict[str, dict[str, str]] = {}
    animations: list[dict[str, Any]] = []
    type_evidence_cache: dict[tuple[str, str], dict[str, Any]] = {}
    better_combat_installed = any(re.search(r"better.?combat", path.name, flags=re.IGNORECASE) for path in MODS.glob("*.jar"))
    for jar in sorted(MODS.glob("*.jar"), key=lambda path: path.name.lower()):
        found = False
        try:
            with zipfile.ZipFile(jar) as archive:
                for name in sorted(archive.namelist()):
                    match = re.fullmatch(r"data/([^/]+)/weapon_attributes/(.+)\.json", name)
                    capability = re.fullmatch(r"data/([^/]+)/capabilities/(weapons|armors)/(.+)\.json", name)
                    skill = re.fullmatch(r"data/([^/]+)/skill_parameters/(.+)\.json", name)
                    if not (match or capability or skill):
                        continue
                    try:
                        value = json.loads(archive.read(name).decode("utf-8-sig"))
                    except (UnicodeDecodeError, json.JSONDecodeError):
                        continue
                    found = True
                    if match:
                        resource_id = f"{match.group(1)}:{match.group(2)}"
                        raw_weapon_attributes[resource_id] = {
                            "resource_id": resource_id,
                            "artifact": jar.name,
                            "path": name,
                            "provider": "bettercombat",
                            "provider_installed": better_combat_installed,
                            "runtime_status": "active" if better_combat_installed else "inactive_optional_compatibility_data",
                            "data": value,
                        }
                    elif capability:
                        item_id = f"{capability.group(1)}:{capability.group(3)}"
                        capabilities.append({"item_id": item_id, "kind": capability.group(2)[:-1], "artifact": jar.name, "path": name, "data": value})
                    else:
                        skill_id = f"{skill.group(1)}:{skill.group(2)}"
                        skill_parameters[skill_id] = {"skill_id": skill_id, "artifact": jar.name, "path": name, "data": value}
        except zipfile.BadZipFile:
            continue
        if found:
            artifacts[jar.name] = {"file": jar.name, "sha256": sha256(jar)}
            if any(capability.get("artifact") == jar.name for capability in capabilities):
                animations.extend(animation_timing_records(jar))

    def resolve(resource_id: str, trail: tuple[str, ...] = ()) -> tuple[dict[str, Any], list[str], str | None]:
        if resource_id in trail:
            return {}, list(trail), f"weapon attribute inheritance cycle at {resource_id}"
        row = raw_weapon_attributes.get(resource_id)
        if row is None:
            return {}, list(trail), f"parent {resource_id} is outside the extracted installed resources"
        data = row["data"]
        parent = data.get("parent")
        merged: dict[str, Any] = {}
        chain: list[str] = []
        unresolved = None
        if parent:
            parent_values, chain, unresolved = resolve(parent, trail + (resource_id,))
            merged.update(parent_values)
        merged.update(data.get("attributes", {}))
        return merged, chain + [resource_id], unresolved

    weapons = []
    for resource_id, row in sorted(raw_weapon_attributes.items()):
        effective, chain, unresolved = resolve(resource_id)
        weapons.append({**row, "inheritance_chain": chain, "effective_attributes": effective, "unresolved_reason": unresolved})
    for row in capabilities:
        type_id = row["data"].get("type")
        if type_id in skill_parameters:
            row["linked_skill_parameters"] = skill_parameters[type_id]
        artifact = MODS / row["artifact"]
        if type_id and artifact.exists():
            key = (row["artifact"], type_id)
            if key not in type_evidence_cache:
                type_evidence_cache[key] = capability_type_code_evidence(artifact, type_id)
            row["type_code_evidence"] = type_evidence_cache[key]
    return {
        "artifacts": [artifacts[name] for name in sorted(artifacts, key=str.lower)],
        "better_combat_weapon_attributes": weapons,
        "epic_fight_capabilities": sorted(capabilities, key=lambda row: (row["item_id"], row["kind"])),
        "epic_fight_skill_parameters": [skill_parameters[key] for key in sorted(skill_parameters)],
        "epic_fight_animation_timings": sorted(animations, key=lambda row: (row["artifact"].lower(), row["animation_id"])),
        "provider_status": {
            "bettercombat": {
                "installed": better_combat_installed,
                "reason": "No Better Combat JAR is present in Client/ManagedPack/mods; embedded weapon_attributes JSON is dormant compatibility data." if not better_combat_installed else "Better Combat artifact is installed.",
            },
            "epicfight": {"installed": any(re.search(r"epic.?fight", path.name, flags=re.IGNORECASE) for path in MODS.glob("*.jar"))},
        },
    }


def main() -> int:
    config_values = installed_config_values()
    if "--bosses-only" in sys.argv[1:]:
        existing = load(OUTPUT)
        bosses, boss_artifacts = boss_evidence(config_values)
        existing["bosses"] = bosses
        existing.setdefault("artifacts", {})["boss_mods"] = boss_artifacts
        existing["installed_config_value_count"] = len(config_values)
        OUTPUT.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(json.dumps({
            "output": OUTPUT.relative_to(ROOT).as_posix(),
            "mode": "bosses-only",
            "bosses": len(bosses),
        }, ensure_ascii=False, indent=2))
        return 0
    spells, spell_artifact = spell_evidence(config_values)
    bosses, boss_artifacts = boss_evidence(config_values)
    allies = ally_evidence(config_values)
    registrations, registration_artifacts = item_registrations()
    items = item_mechanics(registrations, config_values)
    combat_data = data_driven_combat()
    payload = {
        "schema_version": 3,
        "phase": 3,
        "scope": "native mechanics, active data-driven combat capabilities and safely traced numeric evidence; no gameplay values are changed",
        "generator": "Catalog/generator/extract_power_mechanics.py",
        "artifacts": {
            "irons_spellbooks": spell_artifact,
            "boss_mods": boss_artifacts,
            "item_registration_sources": registration_artifacts,
        },
        "spells": spells,
        "bosses": bosses,
        "allies": allies,
        "item_registrations": registrations,
        "items": items,
        "data_driven_combat": combat_data,
        "installed_config_value_count": len(config_values),
        "limits": [
            "Owner class matching for bosses is exact-artifact class/package evidence but is not a Forge registry bootstrap.",
            "A channel not observed in the selected class is unresolved, not proven absent; delegated goals, projectiles and effects are named explicitly.",
            "Item registration extraction recognizes literal string arguments to register calls; dynamic helper-built IDs remain unresolved.",
            "Simple spell formulas are emitted only for a deliberately small symbolic bytecode subset; other methods keep their constants and callees without invented arithmetic.",
        ],
    }
    OUTPUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "output": OUTPUT.relative_to(ROOT).as_posix(),
        "spells": len(spells),
        "bosses": len(bosses),
        "allies": len(allies),
        "item_registrations": len(registrations),
        "combat_items_inspected": len(items),
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
import subprocess
import zipfile
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"
MODS = ROOT / "Client" / "ManagedPack" / "mods"
OUTPUT = CATALOG / "power-sources" / "native-mechanics-phase2.json"
MAPPED_FORGE = (
    Path.home() / ".gradle" / "caches" / "forge_gradle" / "minecraft_user_repo" /
    "net" / "minecraftforge" / "forge" / "1.20.1-47.4.10_mapped_official_1.20.1" /
    "forge-1.20.1-47.4.10_mapped_official_1.20.1.jar"
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def atlas_rows(pattern: str) -> Iterable[dict[str, Any]]:
    for path in sorted(CATALOG.glob(pattern)):
        for row in load(path).get("records", []):
            yield row


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def javap(jar: Path, class_name: str) -> str:
    result = subprocess.run(
        ["javap", "-classpath", str(jar), "-c", "-p", class_name],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return result.stdout if result.returncode == 0 else ""


def class_names(jar: Path) -> list[str]:
    with zipfile.ZipFile(jar) as archive:
        return sorted(
            name[:-6].replace("/", ".")
            for name in archive.namelist()
            if name.endswith(".class") and not name.startswith("META-INF/versions/")
        )


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


def spell_evidence() -> tuple[list[dict[str, Any]], dict[str, Any]]:
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
            })
            continue
        text = javap(jar, owner)
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
            text,
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


def boss_evidence() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
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
            text = javap(MAPPED_FORGE, vanilla_owner)
            artifacts.setdefault(MAPPED_FORGE.name, {
                "file": MAPPED_FORGE.name,
                "sha256": sha256(MAPPED_FORGE),
                "evidence_role": "Forge 47.4.10 official-mapped Minecraft 1.20.1 dependency",
            })
            records.append({
                "entity_id": entity_id,
                "owner_artifact": MAPPED_FORGE.name,
                "owner_classes": [vanilla_owner],
                "owner_status": "exact_mapped_dependency_class",
                "unresolved_reason": None,
                "profile": profile,
                "channels": channel_records(
                    text,
                    BOSS_CHANNELS,
                    "no matching signal was found in the exact mapped vanilla owner class; referenced projectiles/phases remain a named follow-up",
                ),
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
            })
            continue
        if jar.name not in cache:
            cache[jar.name] = class_names(jar)
            artifacts[jar.name] = {"file": jar.name, "sha256": sha256(jar)}
        owners = boss_owner_candidates(entity_id, cache[jar.name])
        texts = [javap(jar, owner) for owner in owners]
        combined = "\n".join(texts)
        records.append({
            "entity_id": entity_id,
            "owner_artifact": jar.name,
            "owner_classes": owners,
            "owner_status": "class_name_and_package_matched" if owners else "unresolved",
            "unresolved_reason": None if owners else "no server-side entity class matched the registered entity path strongly enough",
            "profile": profile,
            "channels": channel_records(
                combined,
                BOSS_CHANNELS,
                "no matching signal was found in the matched exact-version owner class; delegated goals/projectiles remain a named follow-up",
            ),
        })
    return records, sorted(artifacts.values(), key=lambda value: value["file"].lower())


def ally_evidence() -> list[dict[str, Any]]:
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
            })
            continue
        if jar.name not in cache:
            cache[jar.name] = class_names(jar)
        owners = boss_owner_candidates(entity_id, cache[jar.name])
        text = "\n".join(javap(jar, owner) for owner in owners)
        records.append({
            "entity_id": entity_id,
            "owner_artifact": jar.name,
            "owner_classes": owners,
            "owner_status": "class_name_and_package_matched" if owners else "unresolved",
            "unresolved_reason": None if owners else "no server-side entity class matched the ally candidate ID strongly enough",
            "combat_channels": channel_records(
                text,
                BOSS_CHANNELS,
                "no matching signal was found in the matched ally class; inherited AI/goals remain unresolved",
            ),
        })
    return records


def registration_classes(classes: list[str]) -> list[str]:
    result = []
    for name in classes:
        lowered = name.lower()
        simple = lowered.rsplit(".", 1)[-1]
        if any(part in lowered for part in (".client.", ".mixin.", ".model.", ".render.")):
            continue
        if "item" in simple and any(token in simple for token in ("registry", "registries", "register", "items")):
            result.append(name)
    return result[:40]


def item_registrations() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    candidates = list(atlas_rows("views/weapons/*.json")) + list(atlas_rows("views/armor/*.json"))
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
            lines = text.splitlines()
            literals = set(re.findall(r"// String ([a-z0-9_./-]+)$", text, flags=re.MULTILINE))
            for namespace in sorted(jar_mods[jar_name]):
                for item_id in candidate_ids_by_mod[namespace]:
                    if item_id.partition(":")[2] in literals:
                        registrations.setdefault(item_id, {
                            "item_id": item_id,
                            "artifact": jar.name,
                            "owner_class": owner,
                            "status": "observed_literal_in_item_registry_class",
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
                    registrations.setdefault(item_id, {
                        "item_id": item_id,
                        "artifact": jar.name,
                        "owner_class": owner,
                        "status": "observed_string_argument_to_register",
                    })
    return sorted(registrations.values(), key=lambda value: value["item_id"]), artifacts


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


def item_mechanics(registrations: list[dict[str, Any]]) -> list[dict[str, Any]]:
    registration_map = {row["item_id"]: row for row in registrations}
    targets: dict[str, str] = {}
    for row in atlas_rows("views/weapons/*.json"):
        if row["item_id"] in registration_map:
            targets[row["item_id"]] = "weapon"
    for row in atlas_rows("views/armor/*.json"):
        if row["item_id"] in registration_map:
            targets[row["item_id"]] = "armor"
    accessory_tokens = ("ring", "amulet", "necklace", "talisman", "charm", "bracelet", "belt", "curio", "relic")
    for row in atlas_rows("registries/items/*.json"):
        item_id = row["registry_id"]
        if item_id in registration_map and any(token in item_id.partition(":")[2].lower() or token in (row.get("display_name_en_us") or "").lower() for token in accessory_tokens):
            targets.setdefault(item_id, "accessory")
    jars = {path.name: path for path in MODS.glob("*.jar")}
    class_cache: dict[str, list[str]] = {}
    records = []
    for item_id, kind in sorted(targets.items()):
        registration = registration_map[item_id]
        jar = jars.get(registration["artifact"])
        if jar is None:
            continue
        if jar.name not in class_cache:
            class_cache[jar.name] = class_names(jar)
        owners = item_owner_candidates(item_id, class_cache[jar.name])
        text = "\n".join(javap(jar, owner) for owner in owners)
        constants = sorted({value for line in text.splitlines() if (value := bytecode_number(line)) is not None}, key=float)
        records.append({
            "item_id": item_id,
            "kind": kind,
            "artifact": jar.name,
            "registration": registration,
            "owner_classes": owners,
            "owner_status": "class_name_and_package_matched" if owners else "registered_item_owner_class_unresolved",
            "unresolved_reason": None if owners else "the ItemStack is registered, but no specialized implementation class matched its ID; it may use a shared/base Item class or helper factory",
            "mechanics": channel_records(
                text,
                ITEM_CHANNELS,
                "no matching signal was found in the matched item implementation; inherited/shared behavior or data-driven Epic Fight capability remains unresolved",
            ),
            "numeric_constants": constants[:80],
        })
    return records


def main() -> int:
    spells, spell_artifact = spell_evidence()
    bosses, boss_artifacts = boss_evidence()
    allies = ally_evidence()
    registrations, registration_artifacts = item_registrations()
    items = item_mechanics(registrations)
    payload = {
        "schema_version": 1,
        "phase": 2,
        "scope": "native mechanics evidence only; no gameplay values are changed",
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

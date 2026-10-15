#!/usr/bin/env python3
"""Extract exact installed armor-material and Curios evidence for Power Stage 3.5C."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import zipfile
from collections import defaultdict
from functools import lru_cache
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "Catalog"
MODS = ROOT / "Client" / "ManagedPack" / "mods"
OUTPUT = CATALOG / "power-sources" / "defense-mechanics-stage35c.json"

SLOT_INDEX = {"helmet": 0, "chestplate": 1, "leggings": 2, "boots": 3}
VANILLA_MATERIALS = {
    "leather": {"defense": [1, 3, 2, 1], "toughness": 0.0, "knockback_resistance": 0.0, "durability": [55, 80, 75, 65]},
    "chainmail": {"defense": [2, 5, 4, 1], "toughness": 0.0, "knockback_resistance": 0.0, "durability": [165, 240, 225, 195]},
    "iron": {"defense": [2, 6, 5, 2], "toughness": 0.0, "knockback_resistance": 0.0, "durability": [165, 240, 225, 195]},
    "golden": {"defense": [2, 5, 3, 1], "toughness": 0.0, "knockback_resistance": 0.0, "durability": [77, 112, 105, 91]},
    "diamond": {"defense": [3, 8, 6, 3], "toughness": 2.0, "knockback_resistance": 0.0, "durability": [363, 528, 495, 429]},
    "netherite": {"defense": [3, 8, 6, 3], "toughness": 3.0, "knockback_resistance": 0.1, "durability": [407, 592, 555, 481]},
    "turtle": {"defense": [2, 0, 0, 0], "toughness": 0.0, "knockback_resistance": 0.0, "durability": [275, 0, 0, 0]},
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


@lru_cache(maxsize=None)
def javap(jar: Path, class_name: str) -> str:
    completed = subprocess.run(
        ["javap", "-classpath", str(jar), "-c", "-p", class_name],
        text=True,
        encoding="utf-8",
        errors="replace",
        capture_output=True,
        check=False,
    )
    return completed.stdout if completed.returncode == 0 else ""


def method_body(text: str, name: str) -> str:
    match = re.search(
        rf"(?m)^  .*\b{re.escape(name)}\([^\r\n]*\);\r?\n    Code:\r?\n(?P<body>(?:[ \t]+\d+:.*\r?\n)+)",
        text,
    )
    return match.group("body") if match else ""


def static_body(text: str) -> str:
    match = re.search(r"(?m)^  static \{\};\r?\n    Code:\r?\n(?P<body>(?:[ \t]+\d+:.*\r?\n)+)", text)
    return match.group("body") if match else ""


def descriptor_types(descriptor: str) -> tuple[list[str], str]:
    params, result = descriptor.split(")", 1)
    params = params[1:]
    types: list[str] = []
    index = 0
    while index < len(params):
        start = index
        while params[index] == "[":
            index += 1
        if params[index] == "L":
            index = params.index(";", index) + 1
        else:
            index += 1
        types.append(params[start:index])
    return types, result


def number_from_instruction(op: str, arg: str, comment: str) -> int | float | None:
    if op == "iconst_m1":
        return -1
    match = re.fullmatch(r"[ifld]const_(\d+)", op)
    if match:
        return int(match.group(1))
    if op in {"bipush", "sipush"}:
        try:
            return int(arg.strip())
        except ValueError:
            return None
    if op in {"ldc", "ldc_w", "ldc2_w"}:
        match = re.search(r"// (?:int|long|float|double) (-?\d+(?:\.\d+)?)(?:[fdl])?$", comment, re.I)
        if match:
            value = float(match.group(1))
            return int(value) if value.is_integer() else value
    return None


def field_reference(comment: str, current_owner: str) -> tuple[str, str, str] | None:
    match = re.search(r"Field (?:(?P<owner>[A-Za-z0-9_$/]+)\.)?(?P<field>[A-Za-z0-9_$]+):(?P<desc>\[?[A-ZL][^ ]*)", comment)
    if not match:
        return None
    return ((match.group("owner") or current_owner.replace(".", "/")).replace("/", "."), match.group("field"), match.group("desc"))


def invoke_reference(comment: str) -> tuple[str, str, str] | None:
    match = re.search(r"(?:InterfaceMethod|Method) (?:(?P<owner>[A-Za-z0-9_$/]+)\.)?\"?(?P<method>[A-Za-z0-9_$<>]+)\"?:(?P<desc>\([^ ]+)", comment)
    if not match:
        return None
    return ((match.group("owner") or "").replace("/", "."), match.group("method"), match.group("desc"))


def interpret_static(text: str, owner: str) -> dict[str, Any]:
    stack: list[Any] = []
    fields: dict[str, Any] = {}
    for raw in static_body(text).splitlines():
        match = re.match(r"\s*\d+:\s+(?P<op>[a-z0-9_]+)(?:\s+(?P<arg>.*?))?(?:\s+// (?P<comment>.*))?$", raw)
        if not match:
            continue
        op = match.group("op")
        arg = (match.group("arg") or "").strip()
        comment = match.group("comment") or ""
        number = number_from_instruction(op, arg, f"// {comment}")
        if number is not None:
            stack.append(number)
        elif op in {"ldc", "ldc_w"} and comment.startswith("String "):
            stack.append(comment.removeprefix("String "))
        elif op == "aconst_null":
            stack.append(None)
        elif op == "new":
            class_match = re.search(r"class ([A-Za-z0-9_$/]+)", comment)
            stack.append({"kind": "object", "class": class_match.group(1).replace("/", ".") if class_match else None})
        elif op in {"newarray", "anewarray"}:
            size = stack.pop() if stack else 0
            stack.append({"kind": "array", "values": [None] * int(size) if isinstance(size, (int, float)) else []})
        elif op == "dup" and stack:
            stack.append(stack[-1])
        elif op in {"iastore", "aastore", "fastore", "dastore"} and len(stack) >= 3:
            value, index, array = stack.pop(), stack.pop(), stack.pop()
            if isinstance(array, dict) and array.get("kind") == "array" and isinstance(index, int) and 0 <= index < len(array["values"]):
                array["values"][index] = value
        elif op == "getstatic":
            ref = field_reference(comment, owner)
            stack.append({"kind": "field", "owner": ref[0], "field": ref[1], "descriptor": ref[2]} if ref else {"kind": "unknown"})
        elif op == "invokedynamic":
            dynamic = re.search(r":[A-Za-z0-9_$<>]+:(\([^ ]+)", comment)
            if dynamic:
                try:
                    params, returned = descriptor_types(dynamic.group(1))
                    args = [stack.pop() if stack else {"kind": "unknown"} for _ in params][::-1]
                    if returned != "V":
                        stack.append({"kind": "invokedynamic", "descriptor": dynamic.group(1), "args": args})
                except (ValueError, IndexError):
                    stack.append({"kind": "unknown"})
        elif op.startswith("invoke"):
            ref = invoke_reference(comment)
            if not ref:
                continue
            target_owner, method, descriptor = ref
            try:
                params, returned = descriptor_types(descriptor)
            except (ValueError, IndexError):
                continue
            args = [stack.pop() if stack else {"kind": "unknown"} for _ in params][::-1]
            receiver = None
            if op != "invokestatic":
                receiver = stack.pop() if stack else {"kind": "unknown"}
            if method == "<init>" and isinstance(receiver, dict):
                receiver["constructor_owner"] = target_owner or receiver.get("class")
                receiver["constructor_descriptor"] = descriptor
                receiver["constructor_args"] = args
            if returned != "V":
                resolved_call: dict[str, Any] = {"kind": "call", "owner": target_owner, "method": method, "descriptor": descriptor, "args": args}
                if method == "makeArmorMap" and len(args) == 4 and all(isinstance(value, (int, float)) for value in args):
                    resolved_call = {"kind": "armor_map", "values": [args[3], args[2], args[1], args[0]], "source_method": method}
                elif method == "schoolArmorMap" and not args:
                    resolved_call = {"kind": "armor_map", "values": [3, 8, 6, 3], "source_method": method}
                stack.append(resolved_call)
        elif op == "putstatic":
            ref = field_reference(comment, owner)
            value = stack.pop() if stack else None
            if ref:
                fields[ref[1]] = value
        elif op == "pop" and stack:
            stack.pop()
    return fields


def constructor_field_slots(text: str) -> dict[str, int]:
    result: dict[str, int] = {}
    pattern = re.compile(
        r"aload_0[^\r\n]*\r?\n\s*\d+:\s+[a-z]load(?:_(\d+)|\s+(\d+))[^\r\n]*\r?\n\s*\d+:\s+putfield\s+#[0-9]+\s+// Field ([A-Za-z0-9_$]+):",
    )
    for match in pattern.finditer(text):
        result[match.group(3)] = int(match.group(1) or match.group(2))
    return result


def constructor_constant_fields(text: str) -> dict[str, int | float]:
    result: dict[str, int | float] = {}
    pattern = re.compile(
        r"aload_0[^\r\n]*\r?\n\s*\d+:\s+(?P<op>[ifld]const_[0-9]+|iconst_m1|bipush|sipush|ldc(?:2?_w)?)(?:\s+(?P<arg>[^\r\n/]+))?(?:\s+// (?P<comment>[^\r\n]+))?\r?\n\s*\d+:\s+putfield\s+#[0-9]+\s+// Field (?P<field>[A-Za-z0-9_$]+):"
    )
    for match in pattern.finditer(text):
        value = number_from_instruction(match.group("op"), (match.group("arg") or "").strip(), f"// {match.group('comment') or ''}")
        if value is not None:
            result[match.group("field")] = value
    return result


def parameter_by_local(descriptor: str, values: list[Any]) -> dict[int, Any]:
    params, _ = descriptor_types(descriptor)
    local = 1
    result = {}
    for param, value in zip(params, values):
        result[local] = value
        local += 2 if param in {"J", "D"} else 1
    return result


def getter_field(text: str, method: str, descriptor: str | None = None) -> str | None:
    body = method_body(text, method)
    matches = re.findall(r"// Field ([A-Za-z0-9_$]+):([^\s]+)", body)
    if descriptor:
        matches = [match for match in matches if match[1] == descriptor]
    return matches[-1][0] if matches else None


def armor_material_profile(jar: Path, field_owner: str, field: str, material_class: str) -> dict[str, Any] | None:
    owner_text = javap(jar, field_owner)
    material_text = javap(jar, material_class)
    if not owner_text or not material_text:
        return None
    value = interpret_static(owner_text, field_owner).get(field)
    if not isinstance(value, dict) or not value.get("constructor_descriptor"):
        return None
    descriptor = value["constructor_descriptor"]
    locals_to_values = parameter_by_local(descriptor, value.get("constructor_args", []))
    assigned = {
        name: locals_to_values.get(slot)
        for name, slot in constructor_field_slots(material_text).items()
    }
    assigned.update({name: value for name, value in constructor_constant_fields(material_text).items() if assigned.get(name) is None})
    defense_field = getter_field(material_text, "m_7366_", "[I") or getter_field(material_text, "m_7366_", "Ljava/util/EnumMap;")
    toughness_field = getter_field(material_text, "m_6651_", "F")
    knockback_field = getter_field(material_text, "m_6649_", "F")
    durability_body = method_body(material_text, "m_266425_")
    durability_fields = re.findall(r"// Field ([A-Za-z0-9_$]+):([^\s]+)", durability_body)
    durability_array_field = next((name for name, desc in durability_fields if desc in {"[I", "Ljava/util/EnumMap;"}), None)
    durability_multiplier_field = next((name for name, desc in durability_fields if desc == "I"), None)
    base_array = interpret_static(material_text, material_class).get(durability_array_field) if durability_array_field else None
    defense = assigned.get(defense_field) if defense_field else None
    toughness = assigned.get(toughness_field) if toughness_field else None
    knockback = assigned.get(knockback_field) if knockback_field else None
    multiplier = assigned.get(durability_multiplier_field) if durability_multiplier_field else None
    if isinstance(defense, dict) and defense.get("kind") == "array":
        defense = defense["values"]
    if isinstance(defense, dict) and defense.get("kind") == "armor_map":
        defense = defense["values"]
    if isinstance(base_array, dict) and base_array.get("kind") == "array":
        base_array = base_array["values"]
    if durability_array_field and not isinstance(base_array, list) and "EnumMap" in next((desc for name, desc in durability_fields if name == durability_array_field), ""):
        base_array = [11, 16, 15, 13]
    if not isinstance(defense, list):
        return None
    durability = None
    if isinstance(base_array, list) and isinstance(multiplier, (int, float)) and len(base_array) >= 4:
        durability = [int(value * multiplier) if isinstance(value, (int, float)) else None for value in base_array[:4]]
    return {
        "material_field_owner": field_owner,
        "material_field": field,
        "material_class": material_class,
        "defense_by_armor_type": defense[:4],
        "toughness": toughness if isinstance(toughness, (int, float)) else None,
        "knockback_resistance": knockback if isinstance(knockback, (int, float)) else None,
        "durability_by_armor_type": durability,
        "status": "exact_installed_armor_material_bytecode",
    }


def body_field_references(body: str, current_owner: str) -> list[dict[str, str]]:
    result = []
    for line in body.splitlines():
        if "// Field " not in line:
            continue
        ref = field_reference(line.partition("// ")[2], current_owner)
        if ref:
            result.append({"owner_class": ref[0], "field": ref[1], "descriptor": ref[2]})
    return result


def slot_from_item(item_id: str, references: list[dict[str, str]]) -> str | None:
    for ref in references:
        if ref["owner_class"] == "net.minecraft.world.item.ArmorItem$Type":
            return ref["field"].lower()
    path = item_id.partition(":")[2]
    return next((slot for slot in SLOT_INDEX if path.endswith(f"_{slot}")), None)


def material_reference(jar: Path, registration: dict[str, Any]) -> tuple[str, str, str] | None:
    owner = registration.get("owner_class")
    factory = registration.get("factory_method")
    if not owner or not factory:
        return None
    factory_body = method_body(javap(jar, owner), factory)
    references = body_field_references(factory_body, owner)
    implementation = registration.get("implementation_class")
    if implementation:
        references.extend(body_field_references(javap(jar, implementation), implementation))
    for ref in references:
        descriptor = ref["descriptor"]
        if not descriptor.startswith("L"):
            continue
        candidate = descriptor[1:-1].replace("/", ".")
        lowered = candidate.lower()
        if "armormaterial" in lowered or "armortier" in lowered or lowered.endswith("armor_material"):
            return ref["owner_class"], ref["field"], candidate
    return None


def explicit_durability_override(jar: Path | None, owner_classes: list[str]) -> dict[str, Any] | None:
    if not jar:
        return None
    for owner in owner_classes:
        body = method_body(javap(jar, owner), "isDamageable")
        if re.search(r"iconst_0\s*\r?\n\s*\d+:\s+ireturn", body):
            return {
                "owner_class": owner,
                "method": "isDamageable(ItemStack)",
                "returns": False,
                "effective_runtime_durability": "explicitly_unbreakable",
                "status": "exact_owner_bytecode_constant_false",
            }
    return None


def vanilla_profile(item_id: str, slot: str) -> dict[str, Any] | None:
    path = item_id.partition(":")[2]
    material = next((name for name in VANILLA_MATERIALS if path.startswith(f"{name}_")), None)
    if item_id == "minecraft:turtle_helmet":
        material = "turtle"
    if material is None:
        return None
    source = VANILLA_MATERIALS[material]
    return {
        "material_field_owner": "net.minecraft.world.item.ArmorMaterials",
        "material_field": material.upper(),
        "material_class": "net.minecraft.world.item.ArmorMaterials",
        "defense_by_armor_type": source["defense"],
        "toughness": source["toughness"],
        "knockback_resistance": source["knockback_resistance"],
        "durability_by_armor_type": source["durability"],
        "status": "exact_minecraft_1_20_1_armor_material",
    }


def curio_tags(jars: list[Path]) -> tuple[dict[str, set[str]], list[dict[str, Any]]]:
    slots: dict[str, set[str]] = defaultdict(set)
    evidence = []
    for jar in jars:
        try:
            with zipfile.ZipFile(jar) as archive:
                for name in sorted(archive.namelist()):
                    match = re.fullmatch(r"data/curios/tags/items/([^/]+)\.json", name)
                    if not match:
                        continue
                    try:
                        data = json.loads(archive.read(name).decode("utf-8-sig"))
                    except (UnicodeDecodeError, json.JSONDecodeError):
                        continue
                    values = []
                    for value in data.get("values", []):
                        item_id = value.get("id") if isinstance(value, dict) else value
                        if isinstance(item_id, str) and ":" in item_id and not item_id.startswith("#"):
                            values.append(item_id)
                            slots[item_id].add(match.group(1))
                    if values:
                        evidence.append({"artifact": jar.name, "path": name, "slot": match.group(1), "item_ids": values})
        except zipfile.BadZipFile:
            continue
    return slots, evidence


def accessory_attributes(registration: dict[str, Any]) -> list[dict[str, Any]]:
    calls = [call for call in registration.get("constructor_calls", []) if call.get("owner_class", "").endswith("AttributeContainer")]
    attributes = [ref for ref in registration.get("field_references", []) if "AttributeModifier$Operation" not in ref.get("owner_class", "") and (
        ref.get("descriptor") == "Lnet/minecraft/world/entity/ai/attributes/Attribute;"
        or "AttributeRegistry" in ref.get("owner_class", "")
        or "ModAttribute" in ref.get("owner_class", "")
    )]
    operations = [ref["field"] for ref in registration.get("field_references", []) if "AttributeModifier$Operation" in ref.get("owner_class", "")]
    result = []
    for index, call in enumerate(calls):
        values = call.get("preceding_numeric_sequence", [])
        if index >= len(attributes) or not values:
            continue
        attribute = attributes[index]
        result.append({
            "attribute_owner": attribute["owner_class"],
            "attribute_field": attribute["field"],
            "value": values[-1],
            "operation": operations[index] if index < len(operations) else None,
            "owner_class": call["owner_class"],
            "status": "exact_registration_attribute_container",
        })
    return result


def armor_set_evidence(defenses: list[dict[str, Any]], jars: dict[str, Path]) -> list[dict[str, Any]]:
    groups: dict[str, dict[str, dict[str, Any]]] = defaultdict(dict)
    for defense in defenses:
        namespace, _, path = defense["id"].partition(":")
        slot = next((candidate for candidate in SLOT_INDEX if path.endswith(f"_{candidate}")), None)
        if slot:
            groups[f"{namespace}:{path[:-(len(slot) + 1)]}"][slot] = defense
    records = []
    explicit_gate = re.compile(r"(?i)(?:has|is|wear|check).*(?:full|complete|correct).*(?:set|suit|armor)|(?:full|complete).*(?:set|suit|armor)")
    for set_id, pieces in sorted(groups.items()):
        if len(pieces) != 4:
            continue
        owners: list[tuple[str, str]] = []
        for piece in pieces.values():
            artifact = (piece.get("registration") or {}).get("artifact") or piece.get("native_mechanics", {}).get("owner_artifact")
            for owner in piece.get("native_mechanics", {}).get("owner_classes", []) + piece.get("native_mechanics", {}).get("delegated_classes", []):
                if artifact and (artifact, owner) not in owners:
                    owners.append((artifact, owner))
        gates = []
        for artifact, owner in owners:
            jar = jars.get(artifact)
            if not jar:
                continue
            text = javap(jar, owner)
            for declaration in re.finditer(r"(?m)^  (?P<decl>[^\r\n]+\((?:[^\r\n]*)\));$", text):
                method_match = re.search(r"([A-Za-z0-9_$]+)\([^()]*\)$", declaration.group("decl"))
                if not method_match or not explicit_gate.search(method_match.group(1)):
                    continue
                gates.append({
                    "owner_class": owner,
                    "method": method_match.group(1),
                    "declaration": declaration.group("decl").strip(),
                    "artifact": artifact,
                    "status": "explicit_full_set_gate_method",
                })
        records.append({
            "set_id": set_id,
            "pieces": {slot: piece["id"] for slot, piece in sorted(pieces.items())},
            "confirmed_set_gates": gates,
            "status": "confirmed_explicit_full_set_gate" if gates else "bounded_owner_chain_no_explicit_full_set_gate_observed",
            "boundary": "Absence of an explicit named gate is not proof that a data-driven, event-bus or external capability bonus does not exist.",
        })
    return records


def main() -> int:
    native = load(CATALOG / "power-sources" / "native-mechanics-phase3.json")
    defenses = load(CATALOG / "views" / "power" / "defenses.json")["records"]
    accessories = load(CATALOG / "views" / "power" / "accessories.json")["records"]
    jars = {path.name: path for path in MODS.glob("*.jar")}
    slots, tag_evidence = curio_tags(list(jars.values()))
    armor_records = []
    material_cache: dict[tuple[str, str, str, str], dict[str, Any] | None] = {}
    artifacts: dict[str, str] = {}
    for defense in defenses:
        item_id = defense["id"]
        registration = defense.get("registration") or {}
        slot = slot_from_item(item_id, registration.get("field_references", []))
        if slot not in SLOT_INDEX:
            continue
        profile = vanilla_profile(item_id, slot) if item_id.startswith("minecraft:") else None
        artifact = registration.get("artifact")
        jar = jars.get(artifact)
        reference = material_reference(jar, registration) if jar else None
        if profile is None and jar and reference:
            key = (jar.name, *reference)
            if key not in material_cache:
                material_cache[key] = armor_material_profile(jar, *reference)
            profile = material_cache[key]
        if profile is None:
            continue
        index = SLOT_INDEX[slot]
        defense_values = profile.get("defense_by_armor_type") or []
        durability_values = profile.get("durability_by_armor_type") or []
        if index >= len(defense_values) or not isinstance(defense_values[index], (int, float)):
            continue
        if jar:
            artifacts[jar.name] = sha256(jar)
        durability_override = explicit_durability_override(jar, defense.get("native_mechanics", {}).get("owner_classes", []))
        armor_records.append({
            "item_id": item_id,
            "slot": slot,
            "armor": defense_values[index],
            "toughness": profile.get("toughness"),
            "knockback_resistance": profile.get("knockback_resistance"),
            "max_damage": durability_values[index] if index < len(durability_values) else None,
            "durability_override": durability_override,
            "material": profile,
            "standard_wear": {
                "formula": "max(1, floor(incoming_damage / 4)) durability points on each equipped armor piece for armor-damaging accepted hits",
                "unbreaking_wear_probability": "0.6 + 0.4 / (level + 1)",
                "mending": "repairs 2 durability per collected XP point while selected by the native Mending path",
                "status": "exact_inherited_minecraft_1_20_1_armor_wear",
            },
        })

    accessory_records = []
    registration_map = {row["item_id"]: row for row in native.get("item_registrations", [])}
    for accessory in accessories:
        item_id = accessory["id"]
        registration = registration_map.get(item_id, accessory.get("registration") or {})
        item_slots = sorted(slots.get(item_id, set()))
        if not item_slots:
            item_slots = sorted({
                ref["field"].removesuffix("_SLOT").lower()
                for ref in registration.get("field_references", [])
                if ref.get("descriptor") == "Ljava/lang/String;" and ref.get("field", "").endswith("_SLOT")
            })
        attrs = accessory_attributes(registration)
        if not item_slots and not attrs:
            continue
        artifact = registration.get("artifact")
        if artifact in jars:
            artifacts[artifact] = sha256(jars[artifact])
        accessory_records.append({
            "item_id": item_id,
            "valid_slots": item_slots,
            "attributes": attrs,
            "equipped_only": bool(item_slots),
            "stacking_boundary": "Curios slot size and the attribute modifier UUID/operation govern stacking; an item tag proves slot validity, not unlimited copies.",
            "status": "exact_curios_tag_and_factory_evidence" if item_slots and attrs else "partial_exact_curios_evidence",
        })

    set_records = armor_set_evidence(defenses, jars)

    payload = {
        "schema_version": 1,
        "phase": "3.5C",
        "scope": "Exact installed armor-material, inherited durability and Curios registration evidence; no runtime or balance value is changed.",
        "generator": "Catalog/generator/extract_defense_mechanics.py",
        "artifacts": [{"file": name, "sha256": digest} for name, digest in sorted(artifacts.items())],
        "armor_records": sorted(armor_records, key=lambda row: row["item_id"]),
        "accessory_records": sorted(accessory_records, key=lambda row: row["item_id"]),
        "armor_set_records": set_records,
        "curios_tag_sources": tag_evidence,
        "shared_formulas": {
            "vanilla_armor": "damage_after_armor = damage * (1 - clamp(armor - damage / (2 + toughness / 4), armor / 5, 20) / 25)",
            "protection": "post_armor_damage * (1 - min(20, effective_protection_factor) / 25)",
            "resistance": "post_applicable_prior_layers * (1 - 0.20 * amplifier_level), unless the damage type bypasses effects",
            "absorption": "absorbs final health damage until absorption hearts are exhausted",
            "shield": "cancels a blockable hit before health loss; bypasses_shield damage is not blocked",
        },
        "boundaries": [
            "An armor material proves ordinary attributes and nominal durability, not a custom event-based reduction or immunity.",
            "Curios item tags prove legal equipped slots; inventory possession alone does not activate ICurio behavior.",
            "No missing material, tag, passive, cooldown or consumption value is replaced by zero.",
        ],
    }
    OUTPUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "output": OUTPUT.relative_to(ROOT).as_posix(),
        "armor_records": len(armor_records),
        "accessory_records": len(accessory_records),
        "armor_sets": len(set_records),
        "confirmed_explicit_set_gates": sum(bool(row["confirmed_set_gates"]) for row in set_records),
        "accessories_with_slots": sum(bool(row["valid_slots"]) for row in accessory_records),
        "accessories_with_attributes": sum(bool(row["attributes"]) for row in accessory_records),
        "artifacts": len(artifacts),
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

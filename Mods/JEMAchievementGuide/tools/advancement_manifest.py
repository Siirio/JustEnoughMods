from __future__ import annotations

import argparse
import json
import re
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path


EXPECTED_COUNTS = {
    "qol": 58,
    "mobs": 63,
    "scary": 60,
    "world": 44,
    "arsenal": 75,
    "fishing": 59,
    "bosses": 49,
    "building": 55,
    "engineering": 37,
    "cool_stuff": 77,
}

FILE_TABS = {
    "JEM_QoL": "qol",
    "JEM_Mobs": "mobs",
    "JEM_Scary": "scary",
    "JEM_World": "world",
    "JEM_Arsenal": "arsenal",
    "JEM_Fishing": "fishing",
    "JEM_Bosses": "bosses",
    "JEM_Building": "building",
    "JEM_Engineering": "engineering",
    "JEM_Cool_Stuff": "cool_stuff",
}

FIELD_PATTERN = re.compile(r"^\*\*([^*]+):\*\*\s*(.*)$")
NUMBERED_HEADING = re.compile(
    r"^##\s+(\d+(?:\.\d+)*)\.\s+(.+?)\s+/\s+(.+?)(\s+(?:\[[^]]+\]\s*)*)$"
)
NUMBERED_HEADING_WITHOUT_FLAGS = re.compile(r"^##\s+(\d+(?:\.\d+)*)\.\s+(.+?)\s+/\s+(.+?)\s*$")
NUMBERED_SINGLE_TITLE = re.compile(r"^##\s+(\d+(?:\.\d+)*)\.\s+([^/]+?)\s*$")
ENGINEERING_HEADING = re.compile(
    r"^##\s+J(\d{2})\.\s+(.+?)\s+/\s+(.+?)(\s+(?:\[[^]]+\]\s*)*)$"
)
ENGINEERING_HEADING_WITHOUT_FLAGS = re.compile(r"^##\s+J(\d{2})\.\s+(.+?)\s+/\s+(.+?)\s*$")
RESOURCE_LOCATION = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


@dataclass(frozen=True)
class Node:
    advancement_id: str
    number: str
    tab: str
    parent: str | None
    title_ru: str
    title_en: str
    short_ru: str
    short_en: str
    exact_ru: str
    exact_en: str
    flags: list[str]
    mechanics: str
    implementation: str
    technical_metadata: str
    resource_ids: list[str]
    strategy: str
    authoritative_source: str
    hook: str
    stateful: bool
    test: str
    source_file: str


def clean_inline(value: str) -> str:
    value = value.strip().rstrip("  ").strip()
    if value.startswith("`") and value.endswith("`") and value.count("`") == 2:
        value = value[1:-1]
    return value


def clean_multiline(lines: list[str]) -> str:
    cleaned = [clean_inline(line) for line in lines]
    while cleaned and not cleaned[0]:
        cleaned.pop(0)
    while cleaned and not cleaned[-1]:
        cleaned.pop()
    return " ".join(part for part in cleaned if part)


def parse_heading(line: str, engineering: bool) -> tuple[str, str, str, list[str]] | None:
    patterns = (
        (ENGINEERING_HEADING, ENGINEERING_HEADING_WITHOUT_FLAGS)
        if engineering
        else (NUMBERED_HEADING, NUMBERED_HEADING_WITHOUT_FLAGS)
    )
    match = patterns[0].match(line) or patterns[1].match(line)
    if match is None and not engineering:
        single = NUMBERED_SINGLE_TITLE.match(line)
        if single is not None:
            number, title = single.group(1), single.group(2).strip()
            return number, title, title, re.findall(r"\[([^]]+)\]", title)
    if match is None:
        return None
    number, title_ru, title_en = match.group(1), match.group(2), match.group(3)
    suffix = match.group(4) if match.lastindex and match.lastindex >= 4 else ""
    return number, title_ru.strip(), title_en.strip(), re.findall(r"\[([^]]+)\]", suffix or "")


def parse_fields(lines: list[str]) -> dict[str, str]:
    fields: dict[str, str] = {}
    current_name: str | None = None
    current_lines: list[str] = []

    def flush() -> None:
        nonlocal current_name, current_lines
        if current_name is not None:
            fields[current_name] = clean_multiline(current_lines)
        current_name = None
        current_lines = []

    for line in lines:
        match = FIELD_PATTERN.match(line)
        if match:
            flush()
            current_name = match.group(1).strip()
            current_lines = [match.group(2)]
        elif current_name is not None and line.strip() != "---":
            current_lines.append(line)
    flush()
    return fields


def flags_from(fields: dict[str, str], heading_flags: list[str]) -> list[str]:
    combined = heading_flags + re.findall(r"\[([^]]+)\]", fields.get("Flags", ""))
    return list(dict.fromkeys(flag.strip() for flag in combined if flag.strip()))


def map_id(tab: str, number: str, fields: dict[str, str]) -> str:
    explicit = clean_inline(fields.get("Advancement ID", ""))
    if explicit:
        if not RESOURCE_LOCATION.match(explicit):
            raise ValueError(f"Invalid explicit advancement ID: {explicit}")
        return explicit
    path = "root" if number == "0" else number.replace(".", "_")
    return f"jem_guide:{tab}/{path}"


def map_parent(tab: str, raw_parent: str) -> str | None:
    parent = clean_inline(raw_parent)
    if not parent or parent in {"—", "-", "none", "None"}:
        return None
    if RESOURCE_LOCATION.match(parent):
        return parent
    if not re.fullmatch(r"\d+(?:\.\d+)*", parent):
        raise ValueError(f"Invalid parent {parent!r} in {tab}")
    path = "root" if parent == "0" else parent.replace(".", "_")
    return f"jem_guide:{tab}/{path}"


def classify(node_id: str, flags: list[str], mechanics: str, implementation: str, exact_en: str) -> tuple[str, bool]:
    text = " ".join((mechanics, implementation, exact_en)).lower()
    flag_set = {flag.upper() for flag in flags}
    if node_id.endswith("/root"):
        return "VANILLA_JSON", False
    if "CUSTOM" in flag_set or "jem wear" in text or "jem mechanical drill" in text:
        return "JEM_GAMEPLAY_EVENT", True
    if any(term in text for term in ("existing advancement", "upstream advancement", "own advancement", "criterion trigger")):
        return "MOD_CRITERION", False
    if any(term in text for term in ("targeted mixin", "inject", "authoritative method", "success method")):
        return "MIXIN", False
    state_terms = (
        "track ", "same player", "same entity", "same machine", "same network", "sequence",
        "before ", "then ", "until ", "without ", "within ", "consecutive", "simultaneously",
        "remember", "counter", "duration", "transition", "three different", "several different",
    )
    if any(term in text for term in state_terms):
        return "SMALL_STATE_MACHINE", True
    if any(term in text for term in ("forge event", "livingdeath", "rightclick", "itemfished", "blockevent", "player event")):
        return "FORGE_EVENT", False
    vanilla_terms = (
        "vanilla", "minecraft:", "obtain", "craft", "inventory", "possess", "consume", "eat ",
        "drink ", "kill ", "defeat ", "tame ", "breed ", "place ", "enter ", "visit ", "wear ",
        "equip ", "recipe", "effect", "durability", "location criterion", "inventory_changed",
    )
    if any(term in text for term in vanilla_terms) and "actual" not in text:
        return "VANILLA_JSON", False
    if any(term in text for term in ("public api", "callback", "event emitted", "mod event")):
        return "MOD_API_EVENT", False
    if "QA" in flag_set or "[qa]" in text:
        return "MIXIN", False
    return "MOD_API_EVENT", False


def source_and_hook(strategy: str, mechanics: str, implementation: str) -> tuple[str, str]:
    source = mechanics.split("—", 1)[0].strip() if mechanics else "Minecraft/Forge"
    hook = implementation or mechanics
    if strategy == "VANILLA_JSON":
        source = "Minecraft advancement criteria"
        hook = implementation or "Data-driven criterion derived from the exact mechanic and verified IDs"
    elif strategy == "FORGE_EVENT":
        source = "MinecraftForge event bus"
    elif strategy == "JEM_GAMEPLAY_EVENT":
        source = "JEM gameplay mechanic"
    elif strategy == "MOD_CRITERION":
        source = f"{source} advancement criterion"
    return source, hook


def parse_entry(name: str, text: str, tab: str) -> list[Node]:
    lines = text.splitlines()
    engineering = tab == "engineering"
    starts: list[tuple[int, tuple[str, str, str, list[str]]]] = []
    for index, line in enumerate(lines):
        parsed = parse_heading(line, engineering)
        if parsed is not None:
            starts.append((index, parsed))
    nodes: list[Node] = []
    for position, (start, heading) in enumerate(starts):
        end = starts[position + 1][0] if position + 1 < len(starts) else len(lines)
        number, title_ru, title_en, heading_flags = heading
        fields = parse_fields(lines[start + 1:end])
        if "Parent" not in fields and "Parent / placement" not in fields:
            continue
        flags = flags_from(fields, heading_flags)
        advancement_id = map_id(tab, number, fields)
        parent_field = fields.get("Parent / placement", fields.get("Parent", ""))
        parent = map_parent(tab, parent_field)
        mechanics = fields.get("Mechanics / Mods", "")
        implementation = fields.get("Implementation / QA", "")
        core_fields = {
            "Advancement ID", "Parent", "Parent / placement", "Mechanics / Mods", "Flags",
            "Short — RU", "Short — EN", "Exact — RU", "Exact — EN", "Path / Number",
        }
        technical_metadata = " ".join(
            f"{key}: {value}" for key, value in fields.items() if key not in core_fields and value
        )
        resource_ids = list(dict.fromkeys(re.findall(r"(?<![A-Za-z0-9_.-])([a-z0-9_.-]+:[a-z0-9_./-]+)", " ".join((mechanics, implementation, technical_metadata)))))
        strategy, stateful = classify(advancement_id, flags, mechanics, implementation, fields.get("Exact — EN", ""))
        source, hook = source_and_hook(strategy, mechanics, implementation)
        short_ru = fields.get("Short — RU", "")
        short_en = fields.get("Short — EN", "")
        exact_ru = fields.get("Exact — RU", "") or short_ru
        exact_en = fields.get("Exact — EN", "") or short_en
        nodes.append(Node(
            advancement_id=advancement_id,
            number=f"J{number}" if engineering else number,
            tab=tab,
            parent=parent,
            title_ru=title_ru,
            title_en=title_en,
            short_ru=short_ru,
            short_en=short_en,
            exact_ru=exact_ru,
            exact_en=exact_en,
            flags=flags,
            mechanics=mechanics,
            implementation=implementation,
            technical_metadata=technical_metadata,
            resource_ids=resource_ids,
            strategy=strategy,
            authoritative_source=source,
            hook=hook,
            stateful=stateful,
            test=f"Validate {advancement_id} positive action and nearest negative proxy",
            source_file=name,
        ))
    return nodes


def validate(nodes: list[Node]) -> None:
    errors: list[str] = []
    counts = {tab: 0 for tab in EXPECTED_COUNTS}
    by_id = {node.advancement_id: node for node in nodes}
    if len(by_id) != len(nodes):
        errors.append("Duplicate advancement ID detected")
    for node in nodes:
        counts[node.tab] += 1
        for field in ("title_ru", "title_en", "short_ru", "short_en", "exact_ru", "exact_en", "hook"):
            if not getattr(node, field):
                errors.append(f"{node.advancement_id}: missing {field}")
        if node.parent and node.parent.startswith(("jem_guide:", "jem:")) and node.parent not in by_id:
            errors.append(f"{node.advancement_id}: missing parent {node.parent}")
    if counts != EXPECTED_COUNTS:
        errors.append(f"Counts differ: actual={counts}, expected={EXPECTED_COUNTS}")
    if len(nodes) != sum(EXPECTED_COUNTS.values()):
        errors.append(f"Total {len(nodes)} != {sum(EXPECTED_COUNTS.values())}")
    for node in nodes:
        seen: set[str] = set()
        current = node
        while current.parent in by_id:
            if current.advancement_id in seen:
                errors.append(f"Cycle containing {current.advancement_id}")
                break
            seen.add(current.advancement_id)
            current = by_id[current.parent]
    if errors:
        raise ValueError("\n".join(errors))


def write_matrix(nodes: list[Node], output: Path) -> None:
    rows = [
        "# Implementation Matrix",
        "",
        "| Advancement ID | Tab | Parent | Strategy | Authoritative source | Hook/event/criterion | Stateful? | Test |",
        "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for node in nodes:
        values = (
            node.advancement_id,
            node.tab,
            node.parent or "—",
            node.strategy,
            node.authoritative_source,
            node.hook,
            "yes" if node.stateful else "no",
            node.test,
        )
        escaped = [value.replace("|", "\\|").replace("\n", " ") for value in values]
        rows.append("| " + " | ".join(escaped) + " |")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(rows) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("zip", type=Path)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--matrix", type=Path, required=True)
    args = parser.parse_args()

    nodes: list[Node] = []
    with zipfile.ZipFile(args.zip) as archive:
        for name in archive.namelist():
            prefix = next((candidate for candidate in FILE_TABS if name.startswith(candidate)), None)
            if prefix is None:
                continue
            nodes.extend(parse_entry(name, archive.read(name).decode("utf-8-sig"), FILE_TABS[prefix]))
    nodes.sort(key=lambda node: (list(EXPECTED_COUNTS).index(node.tab), node.number))
    validate(nodes)
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(
        json.dumps({"schema_version": 1, "expected_counts": EXPECTED_COUNTS, "nodes": [asdict(node) for node in nodes]}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    write_matrix(nodes, args.matrix)
    print(json.dumps({"total": len(nodes), "counts": EXPECTED_COUNTS}, ensure_ascii=False))


if __name__ == "__main__":
    main()

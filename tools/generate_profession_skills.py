#!/usr/bin/env python3
"""Generate the Pufferfish Skills data pack owned by JEM Professions."""

from __future__ import annotations

import json
import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "Client/ManagedPack/config/paxi/datapacks/jem-profession-skills"
CATEGORIES = PACK / "data/jem_professions/puffish_skills/categories"

PROFESSIONS = {
    "architect": ("Архитектор", "minecraft:bricks", {
        "architect_hotbar": ("Расширенная панель", "13 настоящих ячеек быстрого доступа.", "minecraft:chest", "tinyinv", "", True),
    }),
    "farmer": ("Фермер", "minecraft:wheat", {}),
    "fisher": ("Рыбак", "minecraft:fishing_rod", {
        "double_catch": ("Двойной улов", "10% шанс получить ещё одну обычную рыбу.", "minecraft:cod", "", "", True),
    }),
    "miner": ("Шахтёр", "minecraft:iron_pickaxe", {
        "ore_sense": ("Чувство руды", "Shift + ПКМ киркой проверяет руду поблизости.", "minecraft:raw_iron", "", "", True),
        "vein_miner": ("Добыча жил", "Shift-добыча разрушает до 8 связанных блоков той же руды.", "minecraft:diamond_ore", "", "", True),
        "vein_master": ("Мастер жил", "Увеличивает предел добычи жил до 32 блоков.", "minecraft:deepslate_diamond_ore", "", "vein_miner", False),
        "stone_step": ("Каменный шаг", "Снижает урон от падения на каменную поверхность на 10%.", "minecraft:stone", "", "", True),
        "lava_shield": ("Лавовый щит", "При огненном уроне даёт краткую защиту от огня с перезарядкой.", "minecraft:lava_bucket", "", "stone_step", False),
        "cave_breath": ("Дыхание пещер", "Иногда сохраняет воздух глубоко под водой.", "minecraft:heart_of_the_sea", "", "lava_shield", False),
    }),
    "explorer": ("Исследователь", "minecraft:compass", {}),
    "engineer": ("Инженер", "create:wrench", {}),
    "warrior": ("Воин", "minecraft:iron_sword", {}),
    "merchant": ("Торговец", "minecraft:emerald", {}),
    "mayor": ("Мэр", "minecraft:bell", {}),
    "mage": ("Маг", "irons_spellbooks:iron_spell_book", {}),
}


def dump(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def experience_costs() -> list[int]:
    return [math.floor(100.0 * (1.0 + 0.08 * level) ** 1.4 + 0.5) for level in range(1, 51)]


def main() -> None:
    dump(PACK / "pack.mcmeta", {"pack": {"pack_format": 15, "supported_formats": [15, 15], "description": "JEM profession skills powered by Pufferfish's Skills"}})
    dump(PACK / "data/jem_professions/puffish_skills/config.json", {"version": 3, "categories": list(PROFESSIONS)})

    for profession, (title, category_icon, nodes) in PROFESSIONS.items():
        folder = CATEGORIES / profession
        dump(folder / "category.json", {
            "unlocked_by_default": True,
            "title": title,
            "description": "Опыт этой профессии превращается в очки навыков.",
            "icon": {"type": "item", "data": {"item": category_icon}},
            "background": "minecraft:textures/gui/advancements/backgrounds/stone.png",
        })
        definitions = {}
        skills = {}
        links = []
        children: dict[str, list[str]] = {}
        for node_id, (node_title, description, icon, required_mod, parent, root) in nodes.items():
            definition = {
                "title": node_title,
                "description": description,
                "icon": {"type": "item", "data": {"item": icon}},
                "cost": 1,
            }
            if required_mod:
                definition["required_mods"] = [required_mod]
            definitions[node_id] = definition
            siblings = children.setdefault(parent, [])
            x = (len(siblings) - 1) * 96
            y = 0 if not parent else 96
            skills[node_id] = {"x": x, "y": y, "definition": node_id, **({"root": True} if root else {})}
            siblings.append(node_id)
            if parent:
                links.append([parent, node_id])
        if profession == "miner":
            positions = {
                "ore_sense": (-160, 0),
                "vein_miner": (-64, -64),
                "vein_master": (64, -64),
                "stone_step": (-64, 64),
                "lava_shield": (64, 64),
                "cave_breath": (192, 64),
            }
            for node_id, (x, y) in positions.items():
                skills[node_id]["x"] = x
                skills[node_id]["y"] = y
        dump(folder / "definitions.json", definitions)
        dump(folder / "skills.json", skills)
        dump(folder / "connections.json", {"normal": {"bidirectional": links}})
        dump(folder / "experience.json", {
            "experience_per_level": {"type": "values", "data": {"values": experience_costs()}},
            "sources": [],
        })


if __name__ == "__main__":
    main()

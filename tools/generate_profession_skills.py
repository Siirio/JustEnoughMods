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
        "long_arms_1": ("Длинные руки I", "+3 блока к дальности установки.", "minecraft:scaffolding", "", "", True),
        "long_arms_2": ("Длинные руки II", "+5 блоков к дальности установки.", "minecraft:spyglass", "", "long_arms_1", False),
        "long_arms_3": ("Длинные руки III", "Финальный уровень ветви дальности строительства.", "minecraft:ender_eye", "", "long_arms_2", False),
        "phantom_3": ("Площадь 3×3", "Установка плоскости 3×3.", "minecraft:bricks", "", "", True),
        "phantom_5": ("Площадь 5×5", "Установка плоскости 5×5.", "minecraft:stone_bricks", "", "phantom_3", False),
        "phantom_7": ("Площадь 7×7", "Установка плоскости 7×7.", "minecraft:chiseled_stone_bricks", "", "phantom_5", False),
        "architect_hotbar": ("Расширенная панель", "13 настоящих ячеек быстрого доступа.", "minecraft:chest", "tinyinv", "", True),
        "architect_supply": ("Строительный резерв", "Резерв до 126 строительных блоков.", "minecraft:shulker_box", "", "architect_hotbar", False),
        "architect_command": ("Командование стройкой", "Финальный уровень ветви быстрого доступа.", "minecraft:command_block", "", "architect_supply", False),
    }),
    "farmer": ("Фермер", "minecraft:wheat", {
        "crop_sense": ("Чувство урожая", "Показывает зрелые культуры в радиусе 12 блоков.", "minecraft:wheat", "", "", True),
        "sweep": ("Сбор площади", "Последовательно собирает соседние зрелые культуры.", "minecraft:iron_hoe", "", "crop_sense", False),
        "harvest_master": ("Мастер жатвы", "Финальный уровень ухода за культурами.", "minecraft:golden_hoe", "", "sweep", False),
        "herd_call": ("Зов стада", "Привлекает до 8 животных на 10 секунд.", "minecraft:wheat_seeds", "", "", True),
        "feed_herd": ("Накормить стадо", "Кормит до 8 взрослых животных.", "minecraft:golden_carrot", "", "herd_call", False),
        "quick_shear": ("Быстрая стрижка", "Стрижёт до 8 готовых овец.", "minecraft:shears", "", "feed_herd", False),
        "greenhouse_master": ("Мастер теплицы", "+10% шанса роста культур в теплице 9×9.", "minecraft:glass", "", "", True),
        "greenhouse_keeper": ("Хранитель теплицы", "Второй уровень тепличной ветви.", "minecraft:honeycomb", "", "greenhouse_master", False),
        "greenhouse_abundance": ("Тепличное изобилие", "Финальный уровень тепличной ветви.", "minecraft:melon", "", "greenhouse_keeper", False),
    }),
    "fisher": ("Рыбак", "minecraft:fishing_rod", {
        "target_fishing": ("Целевой улов", "+20% относительного веса выбранной рыбы.", "minecraft:tropical_fish", "", "", True),
        "rare_bait": ("Редкая наживка", "Второй уровень ветви целевого улова.", "minecraft:nautilus_shell", "", "target_fishing", False),
        "trophy_catch": ("Трофейный улов", "Финальный уровень ветви целевого улова.", "minecraft:heart_of_the_sea", "", "rare_bait", False),
        "double_catch": ("Двойной улов", "10% шанс получить ещё одну обычную рыбу.", "minecraft:cod", "", "", True),
        "steady_line": ("Надёжная леска", "Второй уровень ветви количества улова.", "minecraft:string", "", "double_catch", False),
        "net_master": ("Мастер сетей", "Финальный уровень ветви количества улова.", "minecraft:pufferfish", "", "steady_line", False),
    }),
    "miner": ("Шахтёр", "minecraft:iron_pickaxe", {
        "ore_sense": ("Чувство руды", "Shift + ПКМ киркой проверяет руду поблизости.", "minecraft:raw_iron", "", "", True),
        "ore_memory": ("Память руды", "Второй уровень ветви поиска руды.", "minecraft:raw_gold", "", "ore_sense", False),
        "deep_resonance": ("Глубинный резонанс", "Финальный уровень ветви поиска руды.", "minecraft:echo_shard", "", "ore_memory", False),
        "vein_miner": ("Добыча жил", "Shift-добыча разрушает до 8 связанных блоков той же руды.", "minecraft:diamond_ore", "", "", True),
        "vein_master": ("Мастер жил", "Увеличивает предел добычи жил до 32 блоков.", "minecraft:deepslate_diamond_ore", "", "vein_miner", False),
        "bedrock_precision": ("Точность глубин", "Финальный уровень добычи жил.", "minecraft:netherite_pickaxe", "", "vein_master", False),
        "stone_step": ("Каменный шаг", "Снижает урон от падения на каменную поверхность на 10%.", "minecraft:stone", "", "", True),
        "lava_shield": ("Лавовый щит", "При огненном уроне даёт краткую защиту от огня с перезарядкой.", "minecraft:lava_bucket", "", "stone_step", False),
        "cave_breath": ("Дыхание пещер", "Иногда сохраняет воздух глубоко под водой.", "minecraft:heart_of_the_sea", "", "lava_shield", False),
    }),
    "explorer": ("Исследователь", "minecraft:compass", {
        "wall_climb": ("Подъём", "Поднимает на безопасный уступ до 2 блоков.", "minecraft:ladder", "", "", True),
        "fast_traveler": ("Быстрый путник", "+8% скорости пешком в новых чанках.", "minecraft:leather_boots", "", "wall_climb", False),
        "ledge_catch": ("Захват уступа", "Удерживает на краю до 2 секунд.", "minecraft:lead", "", "fast_traveler", False),
        "ration_saver": ("Экономия провизии", "Бег тратит на 10% меньше насыщения.", "minecraft:bread", "", "", True),
        "river_runner": ("Речной ходок", "+8% скорости обычной лодки.", "minecraft:oak_boat", "", "ration_saver", False),
        "portable_camp": ("Походный лагерь", "Разворачивает временный лагерь из припасов.", "minecraft:campfire", "", "river_runner", False),
    }),
    "engineer": ("Инженер", "create:wrench", {
        "gear_swap": ("Замена передач", "Безопасная смена детали Create.", "create:wrench", "", "", True),
        "shaft_builder": ("Монтаж валов", "Укладывает проверенную линию до 16 валов.", "create:shaft", "", "gear_swap", False),
        "factory_copy": ("Копия фабрики", "Хранит до 3 шаблонов 7×7×7.", "create:schematic", "", "shaft_builder", False),
        "track_run": ("Укладка пути", "Укладывает линию до 12 рельсов.", "create:track", "", "", True),
        "schedule_templates": ("Шаблоны расписаний", "Хранит до 3 расписаний поездов.", "create:schedule", "", "track_run", False),
        "cargo_contract": ("Грузовой контракт", "Доставка с резервированием награды.", "minecraft:minecart", "", "schedule_templates", False),
    }),
    "warrior": ("Воин", "minecraft:iron_sword", {
        "endurance": ("Выносливость", "Уклонение тратит на 5% меньше стамины.", "minecraft:leather_boots", "", "", True),
        "battle_recovery": ("Восстановление", "+2 стамины после уклонения, откат 30 с.", "minecraft:glistering_melon_slice", "", "endurance", False),
        "second_wind": ("Второе дыхание", "Скорость I на 4 с при здоровье ниже 30%.", "minecraft:rabbit_foot", "", "battle_recovery", False),
        "parry_master": ("Мастер парирования", "Идеальный блок дешевле на 15%.", "minecraft:shield", "", "", True),
        "counter_step": ("Ответный шаг", "Следующий удар наносит +1 урона.", "minecraft:iron_sword", "", "parry_master", False),
        "duelist_focus": ("Фокус дуэлянта", "Финальный уровень боевой техники.", "minecraft:diamond_sword", "", "counter_step", False),
        "field_medic": ("Полевой медик", "Поддерживаемое воскрешение быстрее на 20%.", "minecraft:golden_apple", "", "", True),
        "battle_aid": ("Боевая помощь", "Второй уровень ветви поддержки.", "minecraft:potion", "", "field_medic", False),
        "revival_master": ("Мастер спасения", "Финальный уровень ветви поддержки.", "minecraft:totem_of_undying", "", "battle_aid", False),
    }),
    "merchant": ("Торговец", "minecraft:emerald", {
        "buy_orders": ("Заказы на покупку", "До 3 профинансированных запросов.", "minecraft:paper", "", "", True),
        "auction": ("Аукцион", "Торги длительностью 1–24 часа с эскроу.", "minecraft:clock", "", "buy_orders", False),
        "commission_escrow": ("Эскроу заказа", "Защищённая приёмка заказа до 72 часов.", "minecraft:written_book", "", "auction", False),
        "stall_sign": ("Вывеска лавки", "Публикует до 3 Coin-магазинов.", "minecraft:oak_sign", "", "", True),
        "trading_post": ("Торговый пост", "Заказы из реального резерва.", "minecraft:emerald", "", "stall_sign", False),
        "market_network": ("Рыночная сеть", "До 8 собственных объявлений.", "minecraft:ender_chest", "", "trading_post", False),
    }),
    "mayor": ("Мэр", "minecraft:bell", {
        "invite_residents": ("Пригласить жителей", "До 2 настоящих NPC за 3 игровых дня.", "minecraft:player_head", "", "", True),
        "supply_notice": ("Запасы", "Следит за 3 категориями запасов.", "minecraft:barrel", "", "invite_residents", False),
        "resident_move": ("Переезд", "До 2 согласных жителей в игровой день.", "minecraft:minecart", "", "supply_notice", False),
        "taxes": ("Налоги", "Ставка 0/5/10/15% профинансированного дохода NPC.", "minecraft:gold_ingot", "", "", True),
        "treasury_audit": ("Аудит казны", "Второй уровень ветви казны.", "minecraft:book", "", "taxes", False),
        "civic_fund": ("Городской фонд", "Финальный уровень ветви казны.", "minecraft:beacon", "", "treasury_audit", False),
        "rally": ("Сбор", "Созывает до 12 собственных бойцов.", "minecraft:goat_horn", "", "", True),
        "patrol_route": ("Маршрут патруля", "6 точек для 12 собственных стражей.", "minecraft:filled_map", "", "rally", False),
        "captain_order": ("Приказ капитана", "Финальный уровень ветви стражи.", "minecraft:shield", "", "patrol_route", False),
    }),
    "mage": ("Маг", "irons_spellbooks:iron_spell_book", {
        "mana_saver": ("Экономия маны", "Обычные заклинания дешевле на 5%.", "irons_spellbooks:arcane_essence", "", "", True),
        "meditation": ("Медитация", "+5% регенерации маны после 10 с покоя.", "minecraft:amethyst_shard", "", "mana_saver", False),
        "combo_magic": ("Комбо", "После 3 разных заклинаний следующее дешевле на 20%.", "irons_spellbooks:blank_rune", "", "meditation", False),
        "magic_school": ("Школа магии", "+8% силы выбранной школы.", "irons_spellbooks:upgrade_orb", "", "", True),
        "school_master": ("Мастер школы", "Второй уровень магической специализации.", "minecraft:enchanting_table", "", "magic_school", False),
        "arcane_specialist": ("Тайный специалист", "Финальный уровень магической специализации.", "irons_spellbooks:legendary_ink", "", "school_master", False),
        "arcane_guard": ("Магический щит", "25 маны поглощают 2 HP урона.", "minecraft:enchanted_golden_apple", "", "", True),
        "magic_circle": ("Магический круг", "Радиус 6, 8 с, +10% регенерации.", "minecraft:lodestone", "", "arcane_guard", False),
        "sanctuary": ("Святилище", "Финальный уровень ветви магической поддержки.", "minecraft:beacon", "", "magic_circle", False),
    }),
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
        branch_roots = [node_id for node_id, node in nodes.items() if not node[4]]
        root_order = {node_id: index for index, node_id in enumerate(branch_roots)}
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
            branch_root = node_id
            depth = 0
            while nodes[branch_root][4]:
                branch_root = nodes[branch_root][4]
                depth += 1
            x = (root_order[branch_root] - (len(branch_roots) - 1) / 2) * 144
            y = depth * 96
            skills[node_id] = {"x": x, "y": y, "definition": node_id, **({"root": True} if root else {})}
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

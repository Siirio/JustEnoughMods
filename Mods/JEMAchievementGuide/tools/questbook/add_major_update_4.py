#!/usr/bin/env python3
"""Idempotently add the Major Update 4 teaching quests to the canonical book."""

from __future__ import annotations

import copy
import hashlib
import json
from pathlib import Path

SOURCE = Path(__file__).with_name("book.json")

QUESTS = (
    ("tab-07", "mu4_item_magnet", "item_magnet:item_magnet", "A Helpful Pull", "Полезное притяжение",
     "Craft an Item Magnet and use it for ordinary dropped items.", "Создай магнит предметов для обычных выброшенных вещей.",
     "The magnet affects real world item entities. Pending JEM Rewards remain in server storage until claimed.",
     "Магнит действует на реальные предметы в мире. Невыданные награды JEM хранятся на сервере до получения."),
    ("tab-07", "mu4_advancement_frame", "advancementframes:advancement_frame", "A Story on the Wall", "История на стене",
     "Craft an Advancement Frame.", "Создай рамку достижений.",
     "Use its FTB Quests button to select one completed quest. Selection never grants or repeats a reward.",
     "Кнопка заданий FTB позволяет выбрать одно выполненное задание. Выбор не выдаёт и не повторяет награды."),
    ("tab-17", "mu4_better_horses_saddle", "icys-better-horses:upgraded_saddle", "A Better Saddle", "Улучшенное седло",
     "Craft the Better Horses upgraded saddle.", "Создай улучшенное седло Better Horses.",
     "Read the native Stable Handbook for breeds, care, gear and carts. Barnyard Buddies' Clydesdale is separate.",
     "Породы, уход, снаряжение и повозки описаны в родном справочнике конюшни. Клейдесдаль Barnyard Buddies остаётся отдельным."),
    ("tab-17", "mu4_mineable_spawner", "minecraft:spawner", "Move the Cage", "Перенеси клетку",
     "Obtain one spawner with Silk Touch.", "Добудь один спавнер Шёлковым касанием.",
     "Breaking without Silk Touch drops nothing. The placed spawner preserves supported spawn data and never drops an egg.",
     "Без Шёлкового касания ничего не выпадает. Поставленный спавнер сохраняет поддерживаемые данные и никогда не роняет яйцо."),
)

ADVANCEMENT_QUESTS = (
    ("tab-17", "mu4_horse_tame", "icys-better-horses:hold_your_horses", "minecraft:saddle", "Meet a Better Horse", "Знакомство с улучшенной лошадью"),
    ("tab-17", "mu4_horse_tack", "icys-better-horses:all_tacked_up", "icys-better-horses:upgraded_saddle", "All Tacked Up", "Полное снаряжение"),
    ("tab-17", "mu4_horse_foal", "icys-better-horses:foal_play", "minecraft:golden_carrot", "Raise a Foal", "Вырасти жеребёнка"),
    ("tab-17", "mu4_horse_breeds", "icys-better-horses:gotta_tame_em_all", "icys-better-horses:stable_handbook", "Learn the Breeds", "Изучи породы"),
    ("tab-17", "mu4_horse_home", "icys-better-horses:home_is_where_the_hay_is", "minecraft:hay_block", "Build a Stable", "Построй конюшню"),
    ("tab-17", "mu4_horse_ride", "icys-better-horses:ride_or_die", "minecraft:leather_horse_armor", "Ride Together", "В путь вместе"),
    ("tab-07", "mu4_ancient_aquaduct", "jem_advancements:major_update_4/ancient_aquaduct", "minecraft:chiseled_stone_bricks", "Find an Ancient Aqueduct", "Найди древний акведук"),
    ("tab-07", "mu4_ancient_bridge", "jem_advancements:major_update_4/ancient_bridge", "minecraft:stone_bricks", "Cross an Ancient Bridge", "Перейди древний мост"),
    ("tab-07", "mu4_sitting_plus", "jem_advancements:major_update_4/sitting_plus", "minecraft:oak_stairs", "Take a Seat", "Присядь отдохнуть"),
)

EGG_COLLECTIONS = (
    ("mu4_eggs_farm", "Farmyard Incubation", "Инкубация на ферме", ["minecraft:cow_spawn_egg", "minecraft:pig_spawn_egg", "minecraft:sheep_spawn_egg", "minecraft:chicken_spawn_egg"]),
    ("mu4_eggs_wild", "Wildlife Incubation", "Инкубация дикой природы", ["alexsmobs:grizzly_bear_spawn_egg", "alexsmobs:gorilla_spawn_egg", "alexsmobs:raccoon_spawn_egg", "alexsmobs:kangaroo_spawn_egg"]),
    ("mu4_eggs_hostile", "Hostile Incubation", "Инкубация враждебных существ", ["minecraft:zombie_spawn_egg", "minecraft:skeleton_spawn_egg", "minecraft:spider_spawn_egg", "minecraft:creeper_spawn_egg"]),
)


def free_position(chapter: dict) -> list[float]:
    occupied = [tuple(card.get("position", (999, 999))) for card in chapter["cards"]]
    for y in range(-18, 19, 3):
        for x in range(-24, 25, 3):
            if all((x - ox) ** 2 + (y - oy) ** 2 >= 6.25 for ox, oy in occupied):
                return [float(x), float(y)]
    raise RuntimeError(f"No free quest position in {chapter['id']}")


def main() -> None:
    book = json.loads(SOURCE.read_text(encoding="utf-8"))
    for chapter in book["chapters"]:
        chapter["cards"] = [card for card in chapter["cards"]
                            if "horseman" not in json.dumps(card, ensure_ascii=False).lower()]
        for card in chapter["cards"]:
            if card.get("id", "").startswith("mu4_"):
                card["required_mods"] = ["icys_better_horses" if value == "icys-better-horses" else value
                                         for value in card.get("required_mods", [])]
    template = next(card for chapter in book["chapters"] for card in chapter["cards"]
                    if card.get("bindings") and card["bindings"][0].get("kind") == "inventory")
    chapters = {chapter["id"]: chapter for chapter in book["chapters"]}
    existing = {card["id"] for chapter in book["chapters"] for card in chapter["cards"]}
    added = 0
    for chapter_id, card_id, item, en_title, ru_title, en_condition, ru_condition, en_tip, ru_tip in QUESTS:
        if card_id in existing:
            continue
        chapter = chapters[chapter_id]
        card = copy.deepcopy(template)
        card.update({
            "id": card_id,
            "number": f"MU4.{added + 1}",
            "title": ru_title,
            "branch": "major_update_4",
            "condition": ru_condition,
            "purpose": "Освой новый предмет и его родную механику без обхода прогрессии.",
            "tip": ru_tip,
            "after": [],
            "technical": "Completion uses one native FTB inventory task. The item is not consumed.",
            "sources": ["JEM Major Update 4 supplied specification", "Installed mod registry and recipe resources"],
            "ftb_id": hashlib.sha256(card_id.encode()).hexdigest()[:16].upper(),
            "position": free_position(chapter),
            "required_mods": (["icys_better_horses"] if item.startswith("icys-better-horses:")
                              else [item.split(":", 1)[0]] if item != "minecraft:spawner" else ["mineable_spawner"]),
            "icon": item,
            "origin_chapter": chapter_id,
            "layout_group": "major_update_4",
            "layout_source_chapter": chapter_id,
            "status": "active",
            "native_options": {"size": 1.2},
            "mechanic_sections": [],
        })
        card["conditions"] = [{
            "action": en_condition,
            "quantity": "1 шт. / раз",
            "fields": {"Цель": item, "Фильтр": "consume: False", "Проверка": "FTB Quests · Item task",
                       "Храним": "Штатный прогресс FTB."},
            "kind": "inventory",
            "resolved": [{"source": item, "ids": [item], "match": {"ids": [item]}}],
        }]
        card["bindings"] = [{"kind": "inventory", "targets": [{"ids": [item]}], "count": 1, "distinct": 0}]
        card["reward"] = {
            "number": card["number"], "title": ru_title, "chapter": chapter_id, "tier": "starter",
            "items": [{"id": "minecraft:golden_carrot", "count": 4, "name": "Golden Carrot"}],
            "choice": [], "xp": 30, "claim": "once", "choice_mode": None,
            "reason": "Небольшой запас поддерживает обучение и не заменяет родную награду мода.", "tier_label": "Знакомство",
        }
        card["localization"] = {
            "en_us": {"title": en_title, "condition": en_condition,
                      "purpose": "Learn the new item through its native progression.", "tip": en_tip,
                      "reward": "4 × Golden Carrot; 30 XP."},
            "ru_ru": {"title": ru_title, "condition": ru_condition,
                      "purpose": "Освой новый предмет через его родную прогрессию.", "tip": ru_tip,
                      "reward": "4 × Золотая морковь; 30 опыта."},
        }
        card["design_metadata"] = {
            "stage": "early", "lesson": en_condition,
            "reward_purpose": "Small teaching support without replacing native progression.",
            "old_sources": [], "migration_action": "new_major_update_4_card",
            "detection_evidence": f"Installed registry item {item}", "visual_route": "major_update_4",
            "hard_dependencies": card["required_mods"], "recommended_after": [],
        }
        card["design"] = {"exact_predecessor": None, "migration_equivalence": "NEW",
                          "coverage_keys": [], "visual_group": "major_update_4"}
        chapter["cards"].append(card)
        existing.add(card_id)
        added += 1

    # Keep the legacy FTB identity and reward so existing progress/claims remain
    # valid after Horseman is removed. Its proof now uses Better Horses' native
    # stable milestone instead of a dead Horseman hook.
    if "v3_pet_trailcraft" not in existing:
        chapter = chapters["tab-17"]
        card = copy.deepcopy(template)
        card.update({
            "id": "v3_pet_trailcraft", "number": "v3_pet_trailcraft", "title": "Привал верхом",
            "branch": "tab-17", "condition": "Обустрой место для лошади и выполни родное достижение конюшни.",
            "purpose": "Сохрани прежний этап путешествий после удаления Horseman.",
            "tip": "Используй родной Stable Handbook Better Horses.", "after": [],
            "technical": "Preserved FTB identity 31D39460913B197C with a native Better Horses advancement.",
            "ftb_id": "31D39460913B197C", "position": [-12.5, 8.6], "required_mods": ["icys_better_horses"],
            "icon": "barnyardbuddies:horseshoe", "status": "active", "layout_group": "major_update_4",
            "layout_source_chapter": "tab-17", "origin_chapter": "tab-17", "native_options": {"size": 1.2},
        })
        argument = "icys-better-horses:home_is_where_the_hay_is"
        card["conditions"] = [{"kind": "advancement", "action": "Complete Better Horses' native stable milestone."}]
        card["bindings"] = [{"kind": "advancement", "targets": [], "count": 1, "distinct": 0, "argument": argument}]
        card["reward"] = {"tier": "field", "items": [
            {"id": "barnyardbuddies:horseshoe", "count": 4, "name": "Horseshoe"},
            {"id": "barnyardbuddies:spotted_quilt_rug", "count": 2, "name": "Spotted Quilt Carpet"}],
            "choice": [], "xp": 75, "claim": "once", "reason": "Preserved legacy reward.", "choice_mode": None}
        card["localization"] = {
            "en_us": {"title": "A Mounted Stop", "condition": "Complete Better Horses' native stable milestone.", "purpose": "Continue the travel lesson without Horseman.", "tip": "Use the native Stable Handbook.", "reward": "4 × Horseshoe; 2 × Spotted Quilt Carpet; 75 XP."},
            "ru_ru": {"title": "Привал верхом", "condition": "Выполни родное достижение конюшни Better Horses.", "purpose": "Продолжи урок путешествий без Horseman.", "tip": "Используй родной справочник конюшни.", "reward": "4 × Подкова; 2 × Пятнистая попона; 75 опыта."},
        }
        chapter["cards"].append(card)
        existing.add(card["id"])

    for chapter_id, card_id, argument, icon, en_title, ru_title in ADVANCEMENT_QUESTS:
        if card_id in existing:
            continue
        chapter = chapters[chapter_id]
        card = copy.deepcopy(template)
        required = (["icys_better_horses"] if argument.startswith("icys-better-horses:")
                    else ["ancient_remnants"] if "ancient_" in card_id else ["sittingplus"])
        card.update({
            "id": card_id, "number": f"MU4.{added + 1}", "title": ru_title, "branch": "major_update_4",
            "condition": ru_title, "purpose": "Освой новую механику через её настоящее игровое событие.",
            "tip": "Засчитывается родное достижение или подтверждённое сервером действие.", "after": [],
            "technical": f"Native advancement binding: {argument}", "sources": ["Installed mod advancement resources"],
            "ftb_id": hashlib.sha256(card_id.encode()).hexdigest()[:16].upper(), "position": free_position(chapter),
            "required_mods": required, "icon": icon, "origin_chapter": chapter_id,
            "layout_group": "major_update_4", "layout_source_chapter": chapter_id, "status": "active",
            "native_options": {"size": 1.2}, "mechanic_sections": [],
        })
        card["conditions"] = [{"kind": "advancement", "action": en_title}]
        card["bindings"] = [{"kind": "advancement", "targets": [], "count": 1, "distinct": 0, "argument": argument}]
        card["reward"] = {"number": card["number"], "title": ru_title, "chapter": chapter_id, "tier": "starter",
                          "items": [], "choice": [], "xp": 35, "claim": "once", "choice_mode": None,
                          "reason": "Native-mechanic teaching milestone.", "tier_label": "Знакомство"}
        card["localization"] = {
            "en_us": {"title": en_title, "condition": en_title, "purpose": "Learn the installed mechanic through a real gameplay signal.", "tip": "Use the mod's native controls and handbook.", "reward": "35 XP."},
            "ru_ru": {"title": ru_title, "condition": ru_title, "purpose": "Освой механику через настоящее игровое событие.", "tip": "Используй родные управление и справочник мода.", "reward": "35 опыта."},
        }
        chapter["cards"].append(card)
        existing.add(card_id)
        added += 1

    for card_id, en_title, ru_title, items in EGG_COLLECTIONS:
        if card_id in existing:
            continue
        chapter = chapters["tab-07"]
        card = copy.deepcopy(template)
        card.update({
            "id": card_id, "number": f"MU4.{added + 1}", "title": ru_title, "branch": "major_update_4",
            "condition": "Создай четыре разных разрешённых яйца призыва.",
            "purpose": "Освой полные рецепты инкубации без доступа к боссам.",
            "tip": "Каждое яйцо имеет полный рецепт 3×3. Яйца боссов и технических существ исключены.", "after": [],
            "technical": "FTB inventory collection requires four distinct reviewed spawn eggs without consuming them.",
            "sources": ["Major Update 4 spawn egg review"], "ftb_id": hashlib.sha256(card_id.encode()).hexdigest()[:16].upper(),
            "position": free_position(chapter), "required_mods": [], "icon": items[0], "origin_chapter": "tab-07",
            "layout_group": "major_update_4", "layout_source_chapter": "tab-07", "status": "active",
            "native_options": {"size": 1.2}, "mechanic_sections": [],
        })
        card["conditions"] = [{"kind": "inventory", "action": en_title, "quantity": "4 distinct"}]
        card["bindings"] = [{"kind": "inventory", "targets": [{"ids": [item]} for item in items], "count": 4, "distinct": 4}]
        card["reward"] = {"number": card["number"], "title": ru_title, "chapter": "tab-07", "tier": "starter",
                          "items": [], "choice": [], "xp": 40, "claim": "once", "choice_mode": None,
                          "reason": "Collection milestone without rare egg rewards.", "tier_label": "Инкубация"}
        card["localization"] = {
            "en_us": {"title": en_title, "condition": "Craft four distinct allowed spawn eggs.", "purpose": "Learn full-grid incubation recipes without boss access.", "tip": "Boss, summon-only and technical creatures are excluded.", "reward": "40 XP."},
            "ru_ru": {"title": ru_title, "condition": "Создай четыре разных разрешённых яйца призыва.", "purpose": "Освой инкубацию без доступа к боссам.", "tip": "Боссы, призываемые и технические существа исключены.", "reward": "40 опыта."},
        }
        chapter["cards"].append(card)
        existing.add(card_id)
        added += 1
    SOURCE.write_text(json.dumps(book, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Added {added} Major Update 4 quests")


if __name__ == "__main__":
    main()

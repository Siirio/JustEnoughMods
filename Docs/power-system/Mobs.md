# Обычные боевые сущности — Stage 3

`Catalog/views/power/entities.json` содержит 1 429 damage-capable candidates. Это широкий инвентарь для дальнейшей проверки, а не утверждение, что каждая строка враждебна, имеет AI-атаку или подпадает под JEM scaling.

## Что уже можно применять

| Слой | Текущее доказательство | Граница |
|---|---|---|
| Ordinary hostile World Tier | JEM source/config selectors и формулы из `modifiers.json` | Только сущности, реально прошедшие selector; имя или namespace недостаточны |
| Blood Moon archetype | Wave/archetype профиль и текущий event-код | Не переносится на обычный spawn того же entity ID |
| Armor Penetration | На `LivingDamageEvent` итог делится на `1 - min(0.30, penetration)` | Это post-mitigation amplification, а не уменьшение armor/toughness |
| Player debuff penetration | Отдельный множитель после подходящего входящего damage | Нельзя второй раз включать в native DamageSource |
| Boss/hosted/Raid | Отдельные владельцы encounter lifecycle | Не применяются к обычному мобу только из-за большого HP |

## Почему итоговый DPS пока не присваивается всем кандидатам

Для сущности должны независимо подтвердиться native attributes, goal/projectile/effect-владелец, частота успешных атак, доступ к цели и применившийся event selector. Stage 3 извлекает точные методы и атрибуты для 61 босса и 103 ally-кандидатов, где есть ограниченный owner path; он не превращает эвристический общий список в 1 429 вымышленных формул.

Пример порядка Blood Moon: native hit проходит обычные Minecraft/modded защитные слои, затем подтверждённый JEM penetration усиливает уже оставшийся health damage. Поэтому число из penetration-профиля нельзя подставлять как vanilla armor penetration и одновременно применять ещё раз после брони.

Оставшиеся поля для каждого кандидата перечислены в `Catalog/views/power/blockers.json`: конкретный ID, отсутствующее поле, owner/candidate method и необходимость runtime-наблюдения. `not_observed` не означает ноль.

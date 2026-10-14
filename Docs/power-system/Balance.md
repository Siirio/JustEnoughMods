# Граница баланса после Phase 3

Phase 3 — это доказанные итоговые числа там, где цепочка уже полна, и конкретная символическая граница во всех остальных местах. Никакой auto-nerf, datapack override, config change или Java balance patch не применён.

Подтверждены пять source findings в `Catalog/views/power/findings.json`:

1. `attribute_contributions` и `ability_multipliers` boss-reward JSON сейчас не читаются `RewardProfile` и не применяются `EquipmentScaler`.
2. Frostbitten Golem вне Raid складывает очень большой special multiplier с boss tier и hosted participant factor.
3. Blood Moon penetration — post-mitigation damage amplification, а не настоящее armor penetration.
4. Tier III chest guard может потратиться на любой подходящий hurt event.
5. Замечание старого аудита про искусственную бесконечную durability уже не соответствует текущему main: обычный износ восстановлен ранее.

Текущий числовой слой находится в `Catalog/views/power/calculations.json`: 45 spell damage tables, 56 boss scaling tables, 136 weapon numeric/data-driven cards и одна vanilla mitigation reference table. Оставшаяся работа Stage 3 — глубокая трассировка делегированных projectile/goal/effect и animation methods; они не блокируют уже доказанные таблицы, но не объявляются нулём или готовым DPS.

Phase 4 сравнивает мощность и доступность; Phase 5 меняет runtime после подтверждения.

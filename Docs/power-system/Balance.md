# Граница баланса после Phase 2

Phase 2 — это нативная механика и доказательства, не изменение цифр. Никакой auto-nerf, datapack override, config change или Java balance patch не применён.

Подтверждены пять source findings в `Catalog/views/power/findings.json`:

1. `attribute_contributions` и `ability_multipliers` boss-reward JSON сейчас не читаются `RewardProfile` и не применяются `EquipmentScaler`.
2. Frostbitten Golem вне Raid складывает очень большой special multiplier с boss tier и hosted participant factor.
3. Blood Moon penetration — post-mitigation damage amplification, а не настоящее armor penetration.
4. Tier III chest guard может потратиться на любой подходящий hurt event.
5. Замечание старого аудита про искусственную бесконечную durability уже не соответствует текущему main: обычный износ восстановлен ранее.

Phase 3 должен привязать все ещё символические/delegated формулы к конкретным projectile/goal/effect методам, собрать target mitigation и acquisition context и только затем считать результаты против эталонных целей. Phase 4 сравнивает мощность и доступность; Phase 5 меняет runtime после подтверждения.

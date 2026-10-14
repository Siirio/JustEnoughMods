# Обычные боевые сущности — Phase 2

`Catalog/views/power/entities.json` остаётся широким инвентарём damage-capable candidates, а не утверждением, что все перечисленные сущности враждебны.

Phase 2 не превращает имя сущности в выдуманную боевую формулу. Для обычных мобов по-прежнему должны быть раздельно доказаны health, armor, melee/projectile/custom channels, AI uptime, target access и World Tier/event modifiers. JEM ordinary-hostile scaling и Blood Moon modifiers принадлежат своим source records и не переносятся на сущность без выполнения их selectors.

Blood Moon `Armor Penetration` подтверждён как post-mitigation amplification: итоговый `LivingDamageEvent` делится на `(1 - min(0.30, penetration))`. Это не уменьшение armor/toughness и не vanilla bypass-armor channel. Player debuff penetration добавляет ещё один multiplier. Название и фактическая математика поэтому отмечены как semantic mismatch для будущей Phase 4, без изменения runtime в Phase 2.

# Граница баланса после Stage 3.5A

Phase 3 — это доказанные итоговые числа там, где цепочка уже полна, и конкретная символическая граница во всех остальных местах. Никакой auto-nerf, datapack override, config change или Java balance patch не применён.

Stage 3.5A выявил новые review-only outliers среди 36 наград:

1. Большая часть Cataclysm reward gear фактически не имеет уменьшаемой прочности; `Infernal Forge` дополнительно наследует nominal 2800 uses, но `Cataclysm_Tool_Item.isDamageable` возвращает false.
2. `Axe of Lightning`, `Soul Great Sword` и `The Tesseract` имеют nominal tier durability, но их `setDamage(...,-1)` блокирует накопление wear.
3. `Laser Gatling` планирует 49 лучей по 7 за 4.9 секунды (`343` raw upper bound), затем требует один redstone и одну секунду reload; луч вручную уменьшает iframe до 5.
4. `Withered Scythe` добавляет отдельные 41 damage через 2 ticks против любого undead — помимо обычного удара, dash и Wither.
5. `Mossy Hammer` имеет два разных right-click обработчика по 10 AoE с разными radius/effect/cooldown; совместное срабатывание одного клика требует runtime trace.
6. `Chorus Blade` перебирает всех living entities в радиусе 8, наносит каждому 8 и телепортирует игрока внутри того же цикла, поэтому массовая цель меняет не только total damage, но и позицию следующих итераций.

Это кандидаты Stage 4, но в текущей задаче не применено ни одного нерфа, durability patch или capability override.

Подтверждены пять source findings в `Catalog/views/power/findings.json`:

1. `attribute_contributions` и `ability_multipliers` boss-reward JSON сейчас не читаются `RewardProfile` и не применяются `EquipmentScaler`.
2. Frostbitten Golem вне Raid складывает очень большой special multiplier с boss tier и hosted participant factor.
3. Blood Moon penetration — post-mitigation damage amplification, а не настоящее armor penetration.
4. Tier III chest guard может потратиться на любой подходящий hurt event.
5. Замечание старого аудита про искусственную бесконечную durability уже не соответствует текущему main: обычный износ восстановлен ранее.

Stage 3 теперь хранит четыре разные вещи, которые нельзя смешивать:

| Выход | Значение |
|---|---|
| `calculations.json` | Воспроизводимые карточки и таблицы; source facts отдельно от derived results |
| `power-ladder.json` | Точный basic-melee sub-ranking и отдельно source-backed intended tier |
| `durability.json` | Max damage и каждый найденный consumption path без вывода «неразрушимо» из отсутствия вызова |
| `blockers.json` | Именованный незакрытый field с ID, owner/class/method и причиной |

Полный actual power ranking не выдуман: пока для оружия неизвестны combo/special DPS или acquisition context, оно не ранжируется только по отображаемому `ATTACK_DAMAGE`. Все boss-reward ID получают лишь доказанный intended tier `boss_reward`; это не утверждение, что предмет уже силён в текущем runtime.

### Проверяемые сигналы потенциального дисбаланса

| Сигнал | Доказанный результат | Что пока нельзя заключить |
|---|---|---|
| Frostbitten Golem, T5, 4 игрока, hosted non-Raid | Owned attack `×9.625` от native | TTK/one-shot без exact native attack и защиты цели |
| Тот же профиль в Raid | `×4.8125`, потому что profile special не применяется | Что Raid всегда легче: reinforcement/frequency остаются отдельными |
| Enderitium Axe | Exact factory `5 + tier bonus 3.5`, то есть addition `8.5`; player basic total `9.5`, speed addition `-3.0` | Общую силу без Epic Fight/special/acquisition |
| Enderitium Sword | Exact factory `3 + tier bonus 3.5`, addition `6.5`; player basic total `7.5`, speed addition `-2.4` | Что топор лучше меча во всех сценариях |
| Incinerator | Addition `13`, speed `-2.6`, reach `+2`, установленный cooldown `400 ticks` | Полный flame-strike burst, пока его разветвлённая damage/HP-damage цепочка не сведена безопасно |
| `skarrier_mobs:ban_sword` | Exact owner modifier `+1499`, player basic total `1500`, speed addition `-2.4` | Реальный progression impact: рецепт/loot/acquisition route в Atlas не найден, поэтому доступность игроку unresolved |

Phase 4 может сравнивать только закрытые одинаковые измерения. Полное автоматическое балансирование начинать рано: unresolved combo, continuous-hit, boss scheduling, runtime ItemStack и target-specific mitigation способны изменить итоговый порядок. Phase 5 меняет runtime только после отдельного одобрения.

# Power system — applied corrections, 2026-10-20

The Power System Revamp request authorizes source-verified implementation. The three October15 Drive documents remain proposals; their numerical pilots were not adopted as approved values. Baseline inspected: fetched GitHub `main` at `ff69ac30`. This is an implemented corrective batch, not a claim that every catalog candidate has been tuned or player-tested.

| Mechanic / fixed context | Before | Implemented after | Evidence |
|---|---|---|---|
| Frost attackTargets, EASY / NORMAL / HARD, config1 |27 /14 /10 |10 /14 /18 |Exact bytecode and injection-handler contracts; native config order retained |
| Frost hosted ordinary melee, T1 solo |43.7184 raw |16.192 raw |Native16 × tier1.15 × hosted0.88; extra Frost-only2.7 removed |
| Frost hosted ordinary melee, T5 four players |154 raw |30.8 raw |Native16 × tier1.75 × party1.25 × hosted0.88; extra Frost-only5 removed |
| Same T5 hit, full netherite Protection IV |46.5696 HP |4.9496832 HP |Armor20, toughness12, EPF16; static reference, not observed DPS |
| Progression III–V chest guard |First eligible hurt spends cooldown |Incoming event at least8 before its reductions; spend only on own accepted health/absorption loss |35% and240ticks retained; canceled/reflected and nested-only damage cannot spend it |
| Five Cataclysm capabilities |Generic/specialized resources compete |Explicit specialized native presets |Incinerator, Annihilator, Soul Render, Cursed Bow, Wrath of the Desert; native attributes/skills retained |
| Race selection immunity |Reopen/confirmation packets renew immunity |Selected players cannot create scene protection; completion gets one100tick exit window |Initial and prolonged selection, saves and V/B(И) controls preserved |
| Bloom passive vs registered boss |Green-ground target canceled |Canonical registered bosses retain targeting |Ordinary mob passive retained; no boss allowlist |
| Actual uncanceled hosted death |Fallback restores1HP and hidden DOWNED life |Immediate canonical elimination, normal death continues |Existing canceled pre-death racial abilities retain ownership |
| Ray of Siphoning healing |Pre-mitigation LivingHurtEvent amount |Native share × positive accepted net HP+absorption loss after hurt |Damage, resistance, mana and native healing modifiers retained |

Removing Frost's blanket extra factor is a Codex balance choice under this overhaul request: native differentiated attacks keep their authored damage alongside existing World Tier and co-op scaling. It is neither Drive pilotA nor pilotB and adds no damage cap. Raid coefficients and Frost Raid exclusion remain. Chest8HP reuses the existing heavy-hit definition before armor, rather than8HP finally lost.

## Corrected research

- Laser Gatling's mapped calls manipulate fire ticks and set five seconds on fire, not invulnerableTime=5. Current installed `cataclysm:laser` lacks `bypasses_cooldown`; ordinary target hurt acceptance applies unless the target overrides it.49scheduled beams ×7 remains a potential upper bound, never observed343damage or DPS.
- Withered Scythe's delayed41 uses unattributed `minecraft:wither`. Withered Abomination rejects that type: accepted bonus0 on that boss, rather than an assumed41. Armor/shield bypass is not enchantment, Resistance or ordinary cooldown bypass.
- Incinerator's outer%5 and inner%2 gates permit one application per10entityticks per flame entity before geometry/acceptance. Accepted application remains6+2%maxHP; multiplying all spawned zones does not prove overlap.
- Iron school/global resistance applies once in the inspected chain. Siphoning's early heal is a different issue. Same-call nested secondary damage/healing can affect its net-pool attribution and remains a runtime observation limit.

## Verification and remaining work

`python tools/check_power_system.py` runs offline owner arithmetic/lifecycle and resource contracts: guard transactions, Siphoning cases, actual Frost injection handlers, capability declarations and extracted actual race methods. Hosted death source safety contracts are not simulated combat.

The opt-in `/jemcombat` observer is documented in `Docs/SERVER_COMMANDS.md`. It separates native hurt calls, event-stage values and actual net HP/absorption loss; deduplicates nested hits; and suppresses rates from invalid callback captures. It cannot invent native cooldown, Curios or spell-stack evidence.

Still required: actual Laser/Scythe/Incinerator accepted-hit recordings, exact Iron stack/level/mana/school resistance and legal Curios builds, same-tier solo/co-op TTK/telegraphs, troop AI contribution, acquisition bypass reproduction and client acceptance. Vanilla equipment references are calculation anchors; realistic modded loadouts must be captured before final all-tier tuning. Ordinary-hostile, party and PvE/PvP spell vectors remain unchanged pending that evidence.

## Historical Stage3.5C research boundary (before implementation)

Phase 3 — это доказанные итоговые числа там, где цепочка уже полна, и конкретная символическая граница во всех остальных местах. Никакой auto-nerf, datapack override, config change или Java balance patch не применён.

Stage 3.5A выявил новые review-only outliers среди 36 наград:

1. Большая часть Cataclysm reward gear фактически не имеет уменьшаемой прочности; `Infernal Forge` дополнительно наследует nominal 2800 uses, но `Cataclysm_Tool_Item.isDamageable` возвращает false.
2. `Axe of Lightning`, `Soul Great Sword` и `The Tesseract` имеют nominal tier durability, но их `setDamage(...,-1)` блокирует накопление wear.
3. `Laser Gatling` планирует 49 лучей по 7 за 4.9 секунды (`343` raw upper bound), затем требует один redstone и одну секунду reload; луч вручную уменьшает iframe до 5.
4. `Withered Scythe` добавляет отдельные 41 damage через 2 ticks против любого undead — помимо обычного удара, dash и Wither.
5. `Mossy Hammer` имеет два разных right-click обработчика по 10 AoE с разными radius/effect/cooldown; совместное срабатывание одного клика требует runtime trace.
6. `Chorus Blade` перебирает всех living entities в радиусе 8, наносит каждому 8 и телепортирует игрока внутри того же цикла, поэтому массовая цель меняет не только total damage, но и позицию следующих итераций.

Это кандидаты Stage 4, но в текущей задаче не применено ни одного нерфа, durability patch или capability override.

Stage 3.5B добавляет boss-side review signals без изменения runtime:

1. Frostbitten Golem имеет native melee `16`; существующая T5/4-player hosted non-Raid композиция `×9.625` превращает его в `154` до защиты, `129.36` против full netherite и `46.5696` потерянных HP против full netherite Protection IV.
2. Его отдельный `attackTargets` использует counter-intuitive difficulty branch: при effective multiplier `1.0` HARD наносит `10`, NORMAL `14`, EASY `27` до JEM scaling. Это точный код installed JAR, а не предложение баланса.
3. Ignis имеет special attacks до `2.0 × attack_damage + 25% target max health`; Poke и Leviathan bite отдельно используют `attack_damage + 10% target max health`.
4. Maledictus ограничивает percent-health добавку через `min(base term, target max health × hp term)`, поэтому это не неограниченный чистый процент.
5. Прежний показатель `231 boss attacks` был завышен 76 inbound damage handlers. Исправленный denominator — 155 special methods плюс 56 отдельных base-melee channels.

Stage 3.5C добавляет defense-side review signals без изменения runtime:

1. Восемь Cataclysm armor items явно возвращают `false` из `isDamageable(ItemStack)`: три части Cursium и пять частей Ignitium, включая elytra chestplate. Nominal material durability при этом существует, но фактический vanilla wear запрещён owner-классом.
2. Cursium chestplate не использует тот же unbreakable-класс, поэтому весь визуально единый набор нельзя называть одинаково неразрушимым.
3. Только один из 39 сгруппированных наборов имеет доказанный именованный full-set gate — Annihilator. Имя/четыре части не используются как доказательство бонуса.
4. 24 комплекта допускают честное сравнение armor/toughness/Protection на 1 080 одинаковых сценариях; остальные 15 не получают придуманных нулей.
5. Curios slots доказаны для 22/52, точные attribute containers для 10/52. Остальные аксессуары нельзя ранжировать только по tooltip или имени.

Stage 4 должен сравнивать эти выражения на одинаковых целях и отдельно учитывать damage-type bypasses. Частота применения и accepted-hit uptime остаются runtime inputs, поэтому even exact per-hit formula ещё не является DPS.

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
| Frostbitten Golem, T5, 4 игрока, hosted non-Raid | Native melee `16` → `154` raw → `46.5696` HP через netherite Protection IV reference | TTK без AI frequency, Resistance, absorption и accepted-hit trace |
| Тот же профиль в Raid | Native melee `16` → `77` raw → `22.61952` HP через тот же reference target | Что Raid всегда легче: reinforcements/frequency остаются отдельными |
| Enderitium Axe | Exact factory `5 + tier bonus 3.5`, то есть addition `8.5`; player basic total `9.5`, speed addition `-3.0` | Общую силу без Epic Fight/special/acquisition |
| Enderitium Sword | Exact factory `3 + tier bonus 3.5`, addition `6.5`; player basic total `7.5`, speed addition `-2.4` | Что топор лучше меча во всех сценариях |
| Incinerator | Addition `13`, speed `-2.6`, reach `+2`, установленный cooldown `400 ticks` | Полный flame-strike burst, пока его разветвлённая damage/HP-damage цепочка не сведена безопасно |
| `skarrier_mobs:ban_sword` | Exact owner modifier `+1499`, player basic total `1500`, speed addition `-2.4` | Реальный progression impact: рецепт/loot/acquisition route в Atlas не найден, поэтому доступность игроку unresolved |

Phase 4 может сравнивать только закрытые одинаковые измерения. Полное автоматическое балансирование начинать рано: unresolved combo, continuous-hit, boss scheduling, runtime ItemStack и target-specific mitigation способны изменить итоговый порядок. Phase 5 меняет runtime только после отдельного одобрения.

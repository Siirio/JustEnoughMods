# Combat balance

Current source policy, 2026-10-22. Status: in testing; no gameplay acceptance is claimed.

The user requested correction of damage provenance, repeated scaling, combat admission and effect lifetime before numerical balance. The implementation extends WorldBossTiers, Roles and Server at their existing owners. World Tier thresholds 0/3/10/18/32, the Progression Tier III cap, native rewards, durability, boss phases and save schemas remain unchanged.

## Boss damage

Supported native formulas compose `flat * profile * party * raid * 0.88 + nativePercent` in a hosted encounter. The tier-specific ATK already supplies Tier growth; it receives no second BossTierScaling boost. The profile multiplier applies in Raid too. Native percent operands and their native caps remain unchanged before target armor, Protection, resistance and absorption. Effective boss health retains EncounterScaler.healthAbsorption; incoming damage caps are a separate native owner.

The generic hook consumes an invocation-specific marker once and skips its outgoing multipliers only for that adapted invocation. Native LivingEntity and Player hurt entry scopes distinguish nested hits, including earlier Forge attack callbacks. An entity/tick flag is not used. Fixed, configuration, projectile and periodic channels are not classified as ATK merely because their owner has that attribute; adapters and their exact pinned call sites define coverage. Unsupported channels retain their existing policy and remain explicit observation/research gaps.

Cataclysm adapters are pinned to installed 3.31; Iron adapters to 1.20.1-3.15.6. Exact method lists are in the registered mixins and the Atlas change record, rather than a second maintained boss allowlist. Vanilla Mob.doHurtTarget is a separately proven flat ATK/enchantment channel.

For the unchanged Ignis profile (Tier V ATK16.2, profile1.15), four players (party1.25), 20 max HP /20 armor /12 toughness /EPF16, the calculated physical accepted-hit losses are:

| Attack | Ordinary hosted | Raid |
| --- | ---: | ---: |
| ATK +5% max HP |2.877909 HP|3.956633 HP|
| 2 ATK +25% max HP |9.401363 HP|13.155571 HP|

These are calculations, not measured combat. Guard, Resistance, absorption, racial modifiers, fire followups and accepted-hit frequency are excluded. The existing generator emits this reference directly from the current profile. It also calculates Tier I–V, solo/four-player, ordinary/Raid cases at16/20/30 max HP to expose low-HP racial risk. That matrix keeps the explicitly prepared20 armor/12 toughness/EPF16 anchor; it is not an early-game acquisition claim or a measured modded build.

## Parameters and lifecycle

| Owner / parameter | Before | Current source |
| --- | --- | --- |
| Raid damage default |2.5|1.25|
| Raid health default |2.5|2.5|
| Bloom/Sculk/Nether adverse climate |-8 max HP|-25% received healing under the same conditions|
| Void passive | -6 HP | -4 HP |
| Bloom fire vulnerability |2.0|1.25|
| Boss Bloom root |whole native tick could pause for 0.0025 Minecraft days|0.000833 Minecraft days of control; native tick continues|
| Shared boss root protection |none|0.006667 Minecraft days after release, shared by every caster|
| Ordinary mob root |0.0025 Minecraft days|unchanged|
| JEM lightning warning |0.000417 Minecraft days|0.001 Minecraft days|
| Warning/hit radius |0.7 /1.75 blocks|one radius1.75 blocks|
| Warning particles |10|24|
| Life Chorus player heal |6 HP|6 HP before ordinary heal modifiers|
| Life Chorus combat ally |full-health assignment|normal heal: min(missingHP,max(6,20% maxHP))|
| Race guardian |invulnerable throughout lifetime|damageable, same encounter admission as owner|
| Progression speed Tier II /III+ |+0.25 /+0.40|+5% /+10% native attribute base, before Epic Fight weighting/events|
| Continuous Shredder Echo |one native entity per proc|merge proportional native contributions per attacker/target over0.000208 Minecraft days|
| Incinerator percent on boss |per-zone2% accepted application|shared8% native max-HP-input budget per cast; flat6 and Brand retained|

Climate penalties use one server LivingHealEvent path and remove only the obsolete race-owned HP UUIDs during normal refresh/login/death/dimension/race/environment reconciliation. Independent gear and positive Sculk modifiers remain owned by their original sources. Roots check encounter admission before damage and also require native damage acceptance before applying control; native invulnerable phases remain authoritative. Delayed racial hits retain their owner. Registered bosses keep effective-health normalization for poison rather than a later direct HP assignment.

Weapon Progression uses the actual physical Player.attack scope, Epic Fight's recorded used item, or a native launch snapshot. Unknown/custom arrows are frozen without a bonus; loading or later hand changes cannot acquire it. Cursed Bow is an explicit pinned hybrid arrow contract. The primary continuous Meat Shredder action retains its lawful Progression multiplier/critical eligibility through its actual supplied ItemStack and native invocation token; its damage-type ID alone cannot borrow a held item. Spell/proc/periodic channels cannot borrow the current held weapon, including in PvP. Eligibility during ItemAttributeModifierEvent reads its original modifiers, avoiding recursive event dispatch.

All four supported blood spells suppress early native healing and settle once from their own positive actual HP plus absorption loss, excluding nested child losses. Native shares remain Siphoning100%, Blood Needles25%, Blood Slash15%, Devour15%; normal healing events/modifiers still apply. Rejected, canceled, absorbed-only, mitigated and overkill cases use actual consumed resources; absorption is counted once.

Echo merges the already formed native pre-mitigation contribution, so the native coefficient is not applied twice and no four-of-five contributions are discarded. It retains the first native echo's tracking, radius and detonation; later contributions can resolve up to0.000167 Minecraft days earlier than their separate echo. This bounded delay tradeoff reduces entity pressure without changing the base calculation or creating spell recursion. Moving-target/AoE acceptance still needs gameplay observation. Windows expire and clear on stop.

## Admission and warning overlap

HostedEncounterApi resolves native pets, Iron IMagicSummon, Recruits and the narrowly validated race guardian. Damage, support and control require the same encounter, living active owner and actual entity/owner full bounding boxes. Native AI can retain admitted allies. Server's existing EventSession supplies the corresponding event rule; player summons are never adopted into the hostile Blood Moon wave. Dead/eliminated owners and spectators remain ineligible. PvP/friendly-fire denial remains in damage handlers, allowing legitimate teammate healing.

JEM lightning waits through confirmed Cataclysm Ignis forced riding or the native stun effect. A capture starting during a warning postpones impact and leaves a complete new warning interval after release.

Blood Moon's individual post-defense caps remain30% max HP for wave3 and49% for wave5 (35%*1.40). They are not a series budget. Its authored three-step combo has a0.0025-Minecraft-day recovery gap and a0.001333-Minecraft-day warning before each next impact; their sum0.003833 exceeds Sculk Burial's0.003333 movement debuff. Thus the three scheduled attacks are separately telegraphed opportunities, not three proven unavoidable hits. Native followups, simultaneous hazards and actual avoidance remain a gameplay observation gap; no unsupported blanket series nerf was introduced.

## Applying to existing worlds

The Java Raid default does not overwrite a saved world SERVER config. On an intentionally stopped host, review `<world>/serverconfig/jem_world_boss_tiers-server.toml` and set raidDamageMultiplier1.25 if adopting the new default. Keep custom administrator values and profileOverrides intact; per-profile overrides still take precedence. Existing prepared/active encounters retain their frozen multipliers: finish/cancel them before starting a fresh encounter under the reviewed config. No automatic world rewrite is performed. The existing local world currently saves Raid health2.0 and damage2.0 with empty profileOverrides; both administrator overrides are preserved. The reference above deliberately uses the requested new damage1.25.

Roles skills protocol is9 because the status payload gained the authoritative healing-penalty field. Install the exact matching new Roles, WorldBossTiers and Server artifacts together where those modules are installed. No new scheduled event activation is implied. The exact task-owned production artifacts are synchronized offline to Client/ManagedPack and the stopped ordinary local Server. Pack source is staged, but native preview/publication/application is pending because this request forbids client/server launches. Existing release baseline/stable profiles and MineRENT remain unchanged.

## Evidence and deliberately unchanged proposals

Installed source/bytecode establishes composition and reachable handlers; it does not measure Epic Fight DPS, accepted projectile counts or full-build power. No numerical nerf was made from Laser Gatling49*7, Withered Scythe's hypothetical41, Mossy Hammer's unproven dual invocation, Soul Rage registration alone, old Ban Sword values or max_strikes. Native late-reward unbreakability remains unchanged. Armor, Curios, consumables and acquisition evidence keep their native owners; unknown stacking/availability is not promoted to a balance fact.

Production packaging, offline Atlas generation and manual source review are separate from runtime acceptance. Automated tests/fixtures, linters, gameplay, client/server launches and live deployment are excluded by the user's request. Injection application, reload/death/dimension transitions, native AI against summons, full accepted damage sequences and realistic solo/party builds still require authorized gameplay observation.

---

## Historical balance evidence (superseded where the current policy above differs)

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

The historical automated checker was retired under the no-tests project contract. It is not an available verification or acceptance path.

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

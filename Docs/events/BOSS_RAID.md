# Boss Raid

## Source update —2026-10-21

In testing; runtime acceptance, artifact delivery and MineRENT deployment pending. The user's barrier removal applies to ordinary bosses, Campaign and Raid only. Blood Moon and Resource Rush retain their contracts. Participant departure is allowed physically; an empty arena closes through the canonical hosted grace lifecycle. Damage/reward membership and immediate elimination on actual death remain enforced.

`HostedRaidScaling` owns the preparation-time Raid boost and its persisted health/damage modifiers. It applies once before Start. Canonical Start freezes participant scaling without multiplying the prepared melee boost a second time; all damage routes use the hosted correction. Cancel/retry/reload/replacement restore the same preparation/lobby owner rather than stacking modifiers. These are source claims, not a runtime scaling measurement.

An attempted hit on a dormant registered boss opens the existing encounter GUI and does not damage it or start combat. Reload restores noAI/invulnerability/presentation hold until Start. Official server boss/event markers can invoke validated native Xaero travel; personal waypoints are unchanged. See [Boss Fight](BOSS_FIGHT.md) for the common source contract.

Authored Raid reinforcements are associated with the canonical hosted boss for damage authority, preserving outsider denial without a second participant owner. On load, a closed orphaned Raid boss restores normal dormant hold. Canonical Start independently rechecks Campaign prerequisites; free structure entry does not grant awakening or damage authorization. Legacy block cleanup is limited to exact retired JEM barrier IDs in known owned shell cells; no wider old-world migration is introduced.

## Primary state

Boss Raid reuses the canonical registered arena, party, arrival/return flow and verified native spawn anchor without a visual, collision or teleport barrier. A legitimate living boss remains in place; an absent Raid boss may be restored only at its verified native anchor. Missing anchor evidence causes explicit refusal.

Фактическая смерть остаётся elimination текущей Raid attempt, но inventory, armor, offhand и Curios участника сохраняются на respawn без grave, пока смерть произошла внутри активной структурной арены. Вне активного Raid обычная death/grave policy не меняется.

Общие периодические эффекты hosted boss не дублируются raid-кодом: Magnetron, Scylla и Cloud Golem сохраняют «Любимчика Зевса» с семисекундным интервалом, полусекундным телеграфом зафиксированной позиции и возможностью увернуться до удара молнии.

Команда старта принимает любого зарегистрированного и уже побеждённого босса, который находится в разблокированной собственной структурной арене в том же измерении. Отдельного allowlist типов нет: raid eligibility и реальные границы рейда выводятся из canonical BossProfile и зарегистрированной структуры.

Individual reward содержит ровно два различных weighted resources и одну enchanted book. Skill books не выдаются. Победа, поражение, cleanup и retry проходят через один event instance; system-created party не расходует public posting cooldown.

Фактическое участие записывается persistent по player UUID и canonical arena ID при старте боя. Повторный Raid этой арены запрещён через любую другую Party; outsider не может наносить Raid boss damage или получить награду. После victory, defeat, forfeit или termination текущий Raid boss удаляется, online-участники немедленно перемещаются на ближайшую проверенную позицию за внешней плоскостью арены, offline-участники сохраняют обязательный выход до следующего входа, и на verified native anchor идемпотентно восстанавливается ровно один свежий normal boss без Raid scale, aggro и ownership. Если локальный safe exit временно недоступен, используется сохранённая безопасная pre-Raid return position. Pending replacement сохраняется через restart и завершается при следующей загрузке структуры.

Accepted participants use shared safe arrival and encounter authorization. There is no closed Raid wall: ordinary entry/exit and teleports remain traversable, while damage, participation and reward eligibility remain authoritative. The hosted correction applies once to direct/projectile/effect/hardcoded boss damage after participant scaling.

Отмена host использует token текущей raid attempt и тот же terminal lifecycle, что defeat: stale chat action side-effect-free, а успешный запрос атомарно закрывает encounter, снимает encounter/participant state, выполняет safe exit и оставляет canonical replacement flow единственным владельцем восстановления normal boss.

Disconnect использует общий event-instance participant lifecycle: момент выхода сохраняется сервером, reconnect раньше пяти минут отменяет ожидание поражения, а solo возвращается на проверенную arena respawn-точку. Пять минут непрерывного отсутствия помечают только этого участника `eliminated`; при следующем входе он получает поражение и возвращается к сохранённой исходной позиции, но raid instance, boss и остальные участники продолжают бой.

При активации боя общий hosted lifecycle включает AI босса и назначает ближайшего живого участника начальной целью, поэтому временный lobby hold не может оставить модового босса без target. После завершения безопасный выход ищется возле вертикального диапазона структуры и никогда не выводится из dimension heightmap, чтобы Nether roof не мог стать точкой выхода.

Xaero не создаёт отдельную Raid-территорию: active Raid использует тот же необязательный canonical structure layer, что и обычный Boss Fight, без наложения второго прямоугольника.

Пока encounter активен, существующий bounded encounter tick восстанавливает ближайшую допустимую живую цель только после потери прежней; он не переопределяет native goals, attack patterns или `NoAI` каждый tick. Reload активного hosted boss один раз восстанавливает combat AI state.

## Rejected

- Отдельный raid participant registry отвергнут: он расходился с Party и recovery.
- Ручной allowlist raid boss entity types отвергнут: он дублировал BossProfile и структуру, поэтому новые структурные боссы молча не проходили запуск.
- Безлимитные reinforcement batches отвергнуты: они ломали pacing и нагрузку.
- Skill books в наградах отвергнуты решением от 2026-09-28.
- Activities/feed как журнал боя отвергнут: остаются encounter messages и native reward delivery.


## Hosted lifecycle repair — 2026-10-19

Historical note: before2026-10-21, the shared boundary presence predicate controlled a boss virtual wall. That boss-wall policy is now superseded entirely; arena/revival identity remains canonical. Blood Moon/Resource Rush geometry is unchanged.

Waiting-area immunity never applies to an ALIVE participant of another active hosted encounter. HostedEncounters owns that classification, preventing overlapping waiting bosses from canceling active boss damage. Held bosses skip their whole native entity tick at the physically dedicated ServerLevel dispatcher: NoAI alone cannot pause Bosses Rise custom combat/cinematics/bar/music. Start clears the existing hold marker and resumes native behavior; tracking still suppresses dormant bars. Integrated singleplayer is excluded by JemServerMixinPlugin. Native phases, damage values, rewards and progression are unchanged.

The supplied crab crash is an obsolete BOSS prompt passed to EventEntryKind.valueOf; source producer was already corrected in4dc02558. The prompt API now accepts only EventEntryKind, preserving protocol9 and rejecting invalid producers at compilation. Helvar second-phase no-damage is consistent with overlapping lobby immunity; the exact live incident is not observed locally. All fixes remain in testing until player acceptance.

## Structural arena volume — 2026-10-19

Historical geometry note: the2026-10-19 correction restricted structural boss walls to the native3D structure bounds. On2026-10-21 the user removed those boss walls entirely. Native structure bounds still classify arenas and safe positions; ordinary Blood Moon/Resource Rush walls remain full height.

Historical arenaEntered containment is superseded for structural bosses. Legacy entry flags must not restore a boss collision or teleport lock. Membership, progression, reward eligibility, native full-column boss lookup and revival retain their canonical owners.
# Boss Raid

## Primary state

Boss Raid использует без переопределения обычный `SOLID` boss boundary, canonical party, зарегистрированную структуру, arrival/return flow и native spawn anchor. Если живой legitimate boss присутствует, Raid использует именно эту entity без перемещения; если отсутствует, создаёт ровно одну entity только на ранее проверенном native anchor. Непроверенный anchor приводит к безопасному отказу, а не к spawn в center или возле host.

Фактическая смерть остаётся elimination текущей Raid attempt, но inventory, armor, offhand и Curios участника сохраняются на respawn без grave, пока смерть произошла внутри активной структурной арены. Вне активного Raid обычная death/grave policy не меняется.

Общие периодические эффекты hosted boss не дублируются raid-кодом: Magnetron, Scylla и Cloud Golem сохраняют «Любимчика Зевса» с семисекундным интервалом, полусекундным телеграфом зафиксированной позиции и возможностью увернуться до удара молнии.

Команда старта принимает любого зарегистрированного и уже побеждённого босса, который находится в разблокированной собственной структурной арене в том же измерении. Отдельного allowlist типов нет: raid eligibility и реальные границы рейда выводятся из canonical BossProfile и зарегистрированной структуры.

Individual reward содержит ровно два различных weighted resources и одну enchanted book. Skill books не выдаются. Победа, поражение, cleanup и retry проходят через один event instance; system-created party не расходует public posting cooldown.

Фактическое участие записывается persistent по player UUID и canonical arena ID при старте боя. Повторный Raid этой арены запрещён через любую другую Party; outsider не может наносить Raid boss damage или получить награду. После victory, defeat, forfeit или termination текущий Raid boss удаляется, online-участники немедленно перемещаются на ближайшую проверенную позицию за внешней плоскостью арены, offline-участники сохраняют обязательный выход до следующего входа, и на verified native anchor идемпотентно восстанавливается ровно один свежий normal boss без Raid scale, aggro и ownership. Если локальный safe exit временно недоступен, используется сохранённая безопасная pre-Raid return position. Pending replacement сохраняется через restart и завершается при следующей загрузке структуры.

Accepted participants используют общий safe-arrival flow. Любой teleport внутрь закрытой raid arena проходит общий server-side endpoint validation Boss Fight и до легального входа перенаправляется наружу. Во время боя участник пересекает общую `SOLID` границу согласно canonical Party authorization; после завершения доступ снимается только вместе с одноразовым безопасным выходом, поэтому игрок не остаётся заперт внутри. Ambient mobs не входят в arena; разрешены raid bosses и authored allies. К итоговому direct/projectile/effect/hardcoded boss damage применяется один hosted correction 12% после participant scaling.

После полного входа source arena остаётся authoritative даже для destination далеко от wall: command/mod/item teleport, portal, dimension transfer, ridden mount и direct entity reposition не могут вывести active participant или event-owned entity наружу. Общий policy также не разрешает outsider teleport внутрь. Native movement collision, teleport endpoints и mob navigation используют одну boundary geometry без corrective tick loop.

Отмена host использует token текущей raid attempt и тот же terminal lifecycle, что defeat: stale chat action side-effect-free, а успешный запрос атомарно закрывает encounter, снимает boundary/participant locks, выполняет safe exit и оставляет canonical replacement flow единственным владельцем восстановления normal boss.

Disconnect использует общий event-instance participant lifecycle: момент выхода сохраняется сервером, reconnect раньше пяти минут отменяет ожидание поражения, а solo возвращается на проверенную arena respawn-точку. Пять минут непрерывного отсутствия помечают только этого участника `eliminated`; при следующем входе он получает поражение и возвращается к сохранённой исходной позиции, но raid instance, boss и остальные участники продолжают бой.

При активации боя общий hosted lifecycle включает AI босса и назначает ближайшего живого участника начальной целью, поэтому временный lobby hold не может оставить модового босса без target. После завершения безопасный выход ищется возле вертикального диапазона структуры и никогда не выводится из dimension heightmap, чтобы Nether roof не мог стать точкой выхода.

Xaero не создаёт отдельную Raid-территорию: active Raid использует тот же canonical structure layer, что и обычный Boss Fight, без наложения второго прямоугольника.

Пока encounter активен, существующий bounded encounter tick восстанавливает ближайшую допустимую живую цель только после потери прежней; он не переопределяет native goals, attack patterns или `NoAI` каждый tick. Reload активного hosted boss один раз восстанавливает combat AI state.

## Rejected

- Отдельный raid participant registry отвергнут: он расходился с Party и recovery.
- Ручной allowlist raid boss entity types отвергнут: он дублировал BossProfile и структуру, поэтому новые структурные боссы молча не проходили запуск.
- Безлимитные reinforcement batches отвергнуты: они ломали pacing и нагрузку.
- Skill books в наградах отвергнуты решением от 2026-09-28.
- Activities/feed как журнал боя отвергнут: остаются encounter messages и native reward delivery.


## Hosted lifecycle repair — 2026-10-19

A discovered structure retains its canonical arena/revival identity, but its virtual wall exists only while a living registered boss or an open hosted party owns it. Rendering, collision and teleport checks use the same presence predicate, bounded to one entity lookup per area per level tick. Empty arenas therefore stay traversable; native ritual activation and canonical revival eligibility remain separate.

Waiting-area immunity never applies to an ALIVE participant of another active hosted encounter. HostedEncounters owns that classification, preventing overlapping waiting bosses from canceling active boss damage. Held bosses skip their whole native entity tick at the physically dedicated ServerLevel dispatcher: NoAI alone cannot pause Bosses Rise custom combat/cinematics/bar/music. Start clears the existing hold marker and resumes native behavior; tracking still suppresses dormant bars. Integrated singleplayer is excluded by JemServerMixinPlugin. Native phases, damage values, rewards and progression are unchanged.

The supplied crab crash is an obsolete BOSS prompt passed to EventEntryKind.valueOf; source producer was already corrected in4dc02558. The prompt API now accepts only EventEntryKind, preserving protocol9 and rejecting invalid producers at compilation. Helvar second-phase no-damage is consistent with overlapping lobby immunity; the exact live incident is not observed locally. All fixes remain in testing until player acceptance.

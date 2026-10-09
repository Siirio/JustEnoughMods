# Boss Raid

## Primary state

Boss Raid использует без переопределения обычный `SOLID` boss boundary, canonical party, зарегистрированную структуру, arrival/return flow и native spawn anchor. Если живой legitimate boss присутствует, Raid использует именно эту entity без перемещения; если отсутствует, создаёт ровно одну entity только на ранее проверенном native anchor. Непроверенный anchor приводит к безопасному отказу, а не к spawn в center или возле host.

Общие периодические эффекты hosted boss не дублируются raid-кодом: Magnetron, Scylla и Cloud Golem сохраняют «Любимчика Зевса» с семисекундным интервалом, полусекундным телеграфом зафиксированной позиции и возможностью увернуться до удара молнии.

Команда старта принимает любого зарегистрированного и уже побеждённого босса, который находится в разблокированной собственной структурной арене в том же измерении. Отдельного allowlist типов нет: raid eligibility и реальные границы рейда выводятся из canonical BossProfile и зарегистрированной структуры.

Individual reward содержит ровно два различных weighted resources и одну enchanted book. Skill books не выдаются. Победа, поражение, cleanup и retry проходят через один event instance; system-created party не расходует public posting cooldown.

Фактическое участие записывается persistent по player UUID и canonical arena ID при старте боя. Повторный Raid этой арены запрещён через любую другую Party; outsider не может наносить Raid boss damage или получить награду. После victory, defeat, forfeit или termination текущий Raid boss удаляется, online-участники немедленно перемещаются на ближайшую проверенную позицию за внешней плоскостью арены, offline-участники сохраняют обязательный выход до следующего входа, и на verified native anchor идемпотентно восстанавливается ровно один свежий normal boss без Raid scale, aggro и ownership. Если локальный safe exit временно недоступен, используется сохранённая безопасная pre-Raid return position. Pending replacement сохраняется через restart и завершается при следующей загрузке структуры.

Accepted participants используют общий safe-arrival flow. Любой teleport внутрь закрытой raid arena проходит общий server-side endpoint validation Boss Fight и до легального входа перенаправляется наружу. Во время боя участник пересекает общую `SOLID` границу согласно canonical Party authorization; после завершения доступ снимается только вместе с одноразовым безопасным выходом, поэтому игрок не остаётся заперт внутри. Ambient mobs не входят в arena; разрешены raid bosses и authored allies. К итоговому direct/projectile/effect/hardcoded boss damage применяется один hosted correction 12% после participant scaling.

Disconnect использует общий event-instance participant lifecycle: момент выхода сохраняется сервером, reconnect раньше пяти минут отменяет ожидание поражения, а solo возвращается на проверенную arena respawn-точку. Пять минут непрерывного отсутствия помечают только этого участника `eliminated`; при следующем входе он получает поражение и возвращается к сохранённой исходной позиции, но raid instance, boss и остальные участники продолжают бой.

При активации боя общий hosted lifecycle включает AI босса и назначает ближайшего живого участника начальной целью, поэтому временный lobby hold не может оставить модового босса без target. После завершения безопасный выход ищется возле вертикального диапазона структуры и никогда не выводится из dimension heightmap, чтобы Nether roof не мог стать точкой выхода.

## Rejected

- Отдельный raid participant registry отвергнут: он расходился с Party и recovery.
- Ручной allowlist raid boss entity types отвергнут: он дублировал BossProfile и структуру, поэтому новые структурные боссы молча не проходили запуск.
- Безлимитные reinforcement batches отвергнуты: они ломали pacing и нагрузку.
- Skill books в наградах отвергнуты решением от 2026-09-28.
- Activities/feed как журнал боя отвергнут: остаются encounter messages и native reward delivery.


# Boss Raid

## Primary state

Boss Raid использует без переопределения обычный hosted boss encounter lifecycle, participant set, структуру, территорию, arrival/return flow и boundary contract. Raid добавляет только собственное усиление: boss model и hitbox масштабируются вместе на 35%, themed allies ограничены двадцатью на весь бой и распределяются между стартом, 50% и 25% combined boss health, а награды распределяются raid-механикой.

Общие периодические эффекты hosted boss не дублируются raid-кодом: Magnetron, Scylla и Cloud Golem сохраняют «Любимчика Зевса» с семисекундным интервалом, полусекундным телеграфом зафиксированной позиции и возможностью увернуться до удара молнии.

Команда старта принимает любого зарегистрированного и уже побеждённого босса, который находится в разблокированной собственной структурной арене в том же измерении. Отдельного allowlist типов нет: raid eligibility и реальные границы рейда выводятся из canonical BossProfile и зарегистрированной структуры.

Individual reward содержит ровно два различных weighted resources и одну enchanted book. Skill books не выдаются. Победа, поражение, cleanup и retry проходят через один event instance; system-created party не расходует public posting cooldown.

Accepted participants используют общий safe-arrival flow. Любой teleport внутрь закрытой raid arena проходит общий server-side endpoint validation Boss Fight и до легального входа перенаправляется наружу; tick fallback выводит наружу обходные перемещения, не затрагивая уже вошедших active participants. Travel не блокируется: выход за bounds возвращает active participant внутрь и предлагает host завершить raid для всех, а member — покинуть только себя. Ambient mobs не входят в arena; разрешены raid bosses и authored allies. К итоговому direct/projectile/effect/hardcoded boss damage применяется один hosted correction 12% после participant scaling.

Disconnect использует общий event-instance participant lifecycle: момент выхода сохраняется сервером, reconnect раньше пяти минут отменяет ожидание поражения, а solo возвращается на проверенную arena respawn-точку. Пять минут непрерывного отсутствия помечают только этого участника `eliminated`; при следующем входе он получает поражение и возвращается к сохранённой исходной позиции, но raid instance, boss и остальные участники продолжают бой.

При активации боя общий hosted lifecycle включает AI босса и назначает ближайшего живого участника начальной целью, поэтому временный lobby hold не может оставить модового босса без target. После завершения безопасный выход ищется возле вертикального диапазона структуры и никогда не выводится из dimension heightmap, чтобы Nether roof не мог стать точкой выхода.

## Rejected

- Отдельный raid participant registry отвергнут: он расходился с Party и recovery.
- Ручной allowlist raid boss entity types отвергнут: он дублировал BossProfile и структуру, поэтому новые структурные боссы молча не проходили запуск.
- Безлимитные reinforcement batches отвергнуты: они ломали pacing и нагрузку.
- Skill books в наградах отвергнуты решением от 2026-09-28.
- Activities/feed как журнал боя отвергнут: остаются encounter messages и native reward delivery.


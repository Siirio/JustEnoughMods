# Server terminal commands

Команды без `/` вводятся в server console; в игре используется `/`.

## SMP and events

Управляющая ветка `smp event ...` и восстановление босса через `smp boss`/`smp boss revive` доступны только игроку `Sirio_o`; OP-права и server console не обходят это ограничение. Пользовательские SMP-команды открытия GUI, участия, голосования, возврата и выхода остаются доступны своим участникам.

- `smp` — открыть SMP.
- `smp event stop combat` — завершить активные combat events через общий cleanup.
- `smp event stop all` — завершить все events через общий cleanup.
- `smp event simulate` — симуляция расписания оператором.
- `smp event schedule list` — показать расписание.
- `smp event schedule reset` — сбросить расписание.
- `smp event schedule add <type> <DAY_OF_WEEK> <HH:mm>` — добавить слот расписания.
- `smp event schedule remove <type> <DAY_OF_WEEK> <HH:mm>` — удалить слот расписания.
- `smp event start fishing` — запустить Fishing.
- `smp event start cooking_show` — запустить Cooking Show.
- `smp event start resource_rush [x y z]` — запустить Resource Rush автоматически или в указанной точке.
- `smp event start blood_moon [x y z] [wave3] [wave5]` — запустить Blood Moon с optional arena и set-piece selections.
- `smp event start boss_raid [x y z]` — выбрать ближайшего допустимого босса/арену автоматически или от указанной точки.
- `smp event stop <uuid>` — остановить конкретный event instance.
- `smp arena <uuid>` — открыть staging prompt арены.
- `smp boss` — восстановить босса ближайшей структурной арены в сохранённой нативной точке; живой босс с крыши возвращается на место, отсутствующий создаётся заново.
- `smp boss <uuid>` — открыть boss hosting prompt; для структурного босса открыть его arena prompt.
- `smp boss revive` — операторский alias для `smp boss`; если в зарегистрированной структурной арене нет живого босса, восстанавливает его без требования прежней победы и не разблокирует Raid до первой легитимной победы.
- `smp event-open <uuid>` — открыть карточку события.
- `smp event-return <uuid> <choice>` — применить Stay/Return choice участника.
- `smp vote <uuid> continue|cashout` — голос после Blood Moon wave.
- `smp forfeit <uuid>` — покинуть попытку.

## World tier

Вся управляющая и диагностическая ветка `jemtier ...` доступна только игроку `Sirio_o`; другие OP и server console не могут вручную изменить World Tier или выдать boss progression.

- `jemtier status` — текущий tier и progression.
- `jemtier set <1..5>` — установить tier оператором.
- `jemtier reset` — сбросить tier оператором.
- `jemtier grant <profile>` — выдать progression profile.
- `jemtier boss <entity>` — применить/проверить boss profile у entity.
- `jemtier arena` — arena diagnostics команды tier engine.

## Player systems

- `claims` — открыть territories UI.
- `landmarks` — список landmarks.
- `landmarks create` — создать landmark.
- `landmarks admin` — admin landmarks.
- `sleep agree` / `sleep disagree` — голос сна.
- `tpa accept` / `tpaccept` — принять запрос телепортации.
- `deaths` / `death` — история смертей JEM.

## JEM Pack Sync

- `jempack preview` — показать add/change/delete delta без публикации.
- `jempack publish` — опубликовать новое поколение JustEnoughMods после изменения server/host-pack файлов.
- `jempack history` — показать журнал опубликованных поколений.
- `jempack rollback <generation>` — вызвать подтверждённый native AutoModpack revert выбранного поколения; при рестарте оно остаётся текущим, потому что автогенерация выключена.

Команды доступны server console и operator level 4. Bootstrap использует Minecraft-порт в режиме `HOLEPUNCH`; удаление ограничено файлами, которыми владело предыдущее опубликованное поколение.

Для recovery структурного босса используется сохранённая точка его исходного structure-generation spawn. Живой экземпляр в вертикальной колонне арены возвращается туда без замены, отсутствующий создаётся из canonical BossProfile. Нельзя выполнять массовый `kill` по entity type, потому что это затрагивает native encounters и чужие попытки.


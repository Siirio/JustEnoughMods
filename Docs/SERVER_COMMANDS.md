# Server terminal commands

Команды без `/` вводятся в server console; в игре используется `/`.

## SMP and events

`smp` — единственная публичная команда SMP: она открывает главную навигацию. Рынок, Кошелёк, профили, группы, события, магазины, поселения, рекруты, награды, голосование и выход из попытки доступны через серверно авторизованные действия GUI.

- `jem event start <event> [x y z]` — запустить реальный тип из `WeeklySchedule.TYPES`; позиция разрешена только для Resource Rush и Blood Moon, а Boss Raid сам выбирает ближайшую допустимую зарегистрированную арену.
- `jem event stop <uuid>` — остановить ровно один незавершённый event через общий cleanup; автодополнение показывает UUID активных событий.
- `jem boss revive` — восстановить босса ближайшей зарегистрированной арены через `BossRevivalApi`, не выдавая победу и не обходя progression.

Все три управляющие операции требуют permission 2. Старые `smp event ...`, `smp boss ...`, `smp arena ...`, `smp event-open`, `smp event-return`, `smp vote`, `smp forfeit` и `smp encounter-leave` больше не регистрируются.

Boss «Открыть бой» chat actions use typed GUI prompt requests, without a public command. Updated clients consume reserved `/jem-ui arena|boss <uuid>` click markers and old `/smp arena|boss <uuid>` scrollback links. Markers are not commands to type or register. Server authentication, throttle, proximity/progression and availability remain canonical; opening the screen does not start combat.

## World tier

Вся управляющая и диагностическая ветка `jemtier ...` требует permission 2.

- `jemtier status` — текущий tier и progression.
- `jemtier set <1..5>` — установить tier оператором.
- `jemtier reset` — сбросить tier оператором.
- `jemtier grant <profile>` — выдать progression profile.
- `jemtier boss <entity>` — применить/проверить boss profile у entity.
- `jemtier arena` — arena diagnostics команды tier engine.

## Player systems

- `jem role` / `jemrole choose <human|elf|dwarf>` — inspect or choose RPG role; the early first change is free.
- `jem professions` — inspect all ten profession levels and XP.
- Кошелёк внутри `/smp` — баланс, перевод онлайн-игроку, внесение/снятие и точный обмен пяти физических номиналов.
- `jem town` / `jem town create <name>` — inspect or found a non-claim town.
- `settlements` — open standalone settlement creation, unique settlement, or multi-settlement selector on integrated or compatible dedicated server. The configurable Settlements key opens the same interface.
- `settlements help` — open the localized interactive settlement guide.
- `jem town linkstorage` — link the physically targeted item-handler container to the owned town.
- `jem town workforce <entity>` — register a genuine owned Recruits/Workers NPC in the single Workforce ledger.
- `jem town feedstorage on|off` — while looking at an already linked physical container, explicitly allow or stop one-item resident feeding from it.
- Поселения → Карта — предварительный просмотр точного прямоугольного расширения;1Gold за каждый новый блок, оплата через единый Wallet/backed Coin ledger. Это мягкая граница поселения, не защита земель.
- `jem town mounts` — show the authenticated player's private lifetime/living mount counts and audited eligible-type progress; no town is required.
- `home` — return to the last valid vanilla bed or charged respawn anchor after server identity, combat, cooldown, active-event and safe-arrival validation.
- Рынок внутри `/smp` — системная покупка и продажа проверенных ингредиентов/ресурсов с реальными котировками и дневными лимитами; готовые блюда исключены.
- `jem project create <name>` — create a visual building project in the current dimension.
- `jem project add <project-uuid> <x1> <z1> <x2> <z2>` — append a source rectangle.
- `jem project remove <project-uuid> <zero-based-rectangle-index>` — remove a source rectangle and recompute its visual union.
- `jem holiday` — show persisted Halloween/New Year lifecycle and remaining active time.
- `jem holiday mute <true|false>` — suppress or restore actual personal holiday notices.
- `jem holiday gift <player>` / `accept <gift-uuid>` / `cancel <gift-uuid>` — durable consent-based New Year item escrow.
- `jem holiday preview|force|stop ...` — OP calendar-event inspection and control.
- `landmarks` — свои сохранённые места и глобально открытые боссы.
- `landmarks create` — сохранить личное место; другим игрокам оно не передаётся.
- `landmarks admin` — тот же личный список и общие открытия боссов для operator; чужие личные записи не раскрываются.
- `sleep agree` / `sleep disagree` — голос сна.
- `tpa accept` / `tpaccept` — принять запрос телепортации.
- История смертей открывается владельцем из собственного профиля `/smp`.

## JEM Pack Sync

- `jempack preview` — показать add/change/delete delta без публикации.
- `jempack publish` — опубликовать новое поколение JustEnoughMods после изменения server/host-pack файлов.
- `jempack history` — показать журнал опубликованных поколений.
- `jempack rollback <generation>` — вызвать подтверждённый native AutoModpack revert выбранного поколения; при рестарте оно остаётся текущим, потому что автогенерация выключена.

Команды доступны server console и operator level 4. Bootstrap использует Minecraft-порт в режиме `HOLEPUNCH`; удаление ограничено файлами, которыми владело предыдущее опубликованное поколение.

Для recovery структурного босса используется сохранённая точка его исходного structure-generation spawn. Живой экземпляр в вертикальной колонне арены возвращается туда без замены, отсутствующий создаётся из canonical BossProfile. Нельзя выполнять массовый `kill` по entity type, потому что это затрагивает native encounters и чужие попытки.

## Chunk pre-generation

Chunky `1.3.146` доступен server console и OP. Pre-generation создаёт настоящие чанки заранее и уменьшает server worldgen stalls; VSS/Voxy больше не входят в сборку.

- `chunky world <world>` — выбрать dimension.
- `chunky center <x> <z>` — установить центр.
- `chunky radius <radius>` — установить радиус в блоках.
- `chunky start` — начать generation выбранной области.
- `chunky progress` — показать прогресс.
- `chunky pause` — безопасно приостановить и сохранить задачу.
- `chunky continue` — продолжить сохранённую задачу.
- `chunky cancel` — удалить текущую задачу после подтверждения.


Respawn recovery must invoke the complete vanilla ServerGamePacketListenerImpl client-command handler with PERFORM_RESPAWN on the server thread. Calling PlayerList.respawn alone omits the caller’s replacement-player connection binding. The authorized recovery must preserve AuthGate authentication state; a respawned locked account still needs ordinary login. Never use account unlock or health editing as the persistent fix. Future pre-auth death-screen requests are handled by the dedicated-only Compat adapter described in DEVELOPMENT.md.

## Local world metadata recovery — 2026-10-19

The ordinary Server restart encountered native NBT depth518 in `Data.WorldGenSettings.dimensions.minecraft:the_nether.generator.settings.surface_rule`, with repeated `terrablender:merged` and nested `minecraft:sequence`. Both current and previous metadata exceeded512; archived2026-10-11 metadata contains the same pattern. The failed startup reached DestroyJavaVM with no Server thread before its remaining JVM was stopped.

Local recovery backed up both level.dat and level.dat_old under ignored `Server/local/boss-20261019/world-metadata-before-normalization`. It flattened353 strictly nested minecraft:sequence wrappers, retaining every child and evaluation order. Exact installed Minecraft SequenceRule bytecode returns the first non-null child result, so associative flattening preserves terrain-rule evaluation. Total depth became229. Only the Nether surface_rule payload was replaced atomically; decompressed prefix/suffix matched exactly. No chunk, player, dimension, seed, mod config, safe-mode flag or depth limit changed. Receipt and exact original hashes are under the same ignored recovery folder. The integration that repeatedly wraps saved rules remains unverified and must be investigated before claiming recurrence prevention.


## Combat observation (2026-10-20)

`/jemcombat start <target> <seconds>` observes one living target for1–120seconds on the dedicated server; permission2. `/jemcombat status` shows progress; `/jemcombat stop` writes the capture. One active session, at most1024native hurt calls, auto-save at duration/sample limit and server stop. JSON output: `<world>/jem-combat-captures/<uuid>.json`; do not commit personal captures.

Samples separate requested native hurt amount, LOWEST event-stage amounts/cancellation, returned boolean and real net health/absorption changes. They include ticks, tier/difficulty, ordinary equipment SNBT, armor/toughness and effects. Outer aggregates avoid nested double counting. Incomplete callback pairing invalidates the capture and suppresses rates. Game-second rates include idle capture time and are not wall-clock DPS, cooldown or attack-phase measurements. Direct setHealth, pre-super rejection, Curios/capability state and inferred spell IDs are outside its scope. Same-call healing/death prevention can mask net damage. The command does not change equipment, health, tier or the world; run actual combat scenarios only in the authorized sandbox.

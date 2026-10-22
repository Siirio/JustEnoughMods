# Server terminal commands

## Current player routes —2026-10-21, source in testing

`/smp` remains the GUI entry. `/death` opens the player's own latest three saved death locations; the selected bounded UUID is revalidated before safe teleport. `/home` uses the caller's existing personal respawn point. `/home all` additionally considers eligible living entities within1block and the ridden root vehicle/passenger tree, with bounded whole-group clearance around the same destination. No `/spawn` alias or second home store is introduced. Identity, combat, cooldown, ownership and active-event gates remain server-owned. A native transfer failure attempts each entity restoration independently; incomplete rollback reports recovery required rather than success.

Wallet actions move to Shipping's money tab; the older “Wallet” navigation description below is historical. Hitting a dormant registered boss opens the existing Start GUI, without a command or automatic combat start. Native Xaero actions for official boss/event markers send IDs only; player-created personal/home/death points receive no JEM teleport route. Artifact installation, MineRENT deployment and runtime acceptance remain pending for this update.

Команды без `/` вводятся в server console; в игре используется `/`.

## SMP and events

`smp` is the dedicated-only SMP route to parties/events and independent domain screens. Profiles and Rewards tabs are retired. `/market` opens the Economy-owned market directly on either host type when Economy is enabled and identity is authorized. Personal item rewards are claimed through the normal inventory E panel, without a command or SMP dependency.

- `jem event start <event> [x y z]` — запустить реальный тип из `WeeklySchedule.TYPES`; позиция разрешена только для Resource Rush и Blood Moon, а Boss Raid сам выбирает ближайшую допустимую зарегистрированную арену.
- `jem event stop <uuid>` — остановить ровно один незавершённый event через общий cleanup; автодополнение показывает UUID активных событий.
- `jem boss revive` — восстановить босса ближайшей зарегистрированной арены через `BossRevivalApi`, не выдавая победу и не обходя progression.

Все три управляющие операции требуют permission 2. Старые `smp event ...`, `smp boss ...`, `smp arena ...`, `smp event-open`, `smp event-return`, `smp vote`, `smp forfeit` и `smp encounter-leave` больше не регистрируются.

Boss «Открыть бой» chat actions use typed GUI prompt requests, without a public command. Updated clients consume reserved `/jem-ui arena|boss <uuid>` click markers and old `/smp arena|boss <uuid>` scrollback links. Markers are not commands to type or register. Server authentication, throttle, proximity/progression and availability remain canonical; opening the screen does not start combat.

## Boss identity audit —2026-10-21, source in testing

All routes below require OP2 on the physically dedicated server; they are not executed by preparing these sources.

- `jem boss-audit list [page]` — bounded loaded-only identities and unresolved registry claims; no chunk loading or absence inference.
- `jem boss-audit plan <keeperUUID> <duplicateUUID>` — read-only explicit repair plan for the same validated canonical arena.
- `jem boss-audit confirm <token> <keeperUUID> <duplicateUUID>` — actor-bound, one-use confirmation within0.05Minecraft-day. Preserve full duplicate NBT with synchronous exact readback; durably retain/transfer the keeper claim before marked discard. Refuse independent structures and active/ambiguous identities.
- `jem boss-audit archive <duplicateUUID>` — inspect preserved quarantine metadata; no automatic restore/spawn.

Exact evidence, exclusions and lifecycle ownership live only in [Boss Fight](events/BOSS_FIGHT.md#duplicate-boss-investigation-and-source-repair-2026-10-21). Isolated-copy checks and production repairs/deployment remain separate execution steps.

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
- Рынок внутри `/smp` — серверные котировки покупки/продажи по ItemPolicy, включая разрешённую готовую еду и отдельную категорию строительных материалов. Опт64/256/1024/4096 доставляется в существующий собственный связанный склад; отсутствие места отклоняет заказ до оплаты.
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

## MineRENT: тестовая копия и возврат на основной мир — 2026-10-20

### История подготовки до разрешения на запуск

- Сервер JustEnough / 0618360f продолжает работать на исходном `world`. Остановка, переключение, изменение игровых режимов и публикация нового клиентского пакета на MineRENT не выполнялись.
- По прямому выбору пользователя тестовый мир взят из существующего `archive-2026-10-20T092221+0300.tar.zst`; панель показывала время файла 20 октября 11:23:42 Asia/Yekaterinburg. Это снимок архива, а не обещание включить последующий прогресс.
- Штатные Copy → Move → Extract панели создали независимый `jem-test-2026-10-20/world`. Исходный архив и активный `world` не заменены. Рабочая копия архива лежит в `jem-test-2026-10-20/source-world.tar.zst`; новый снимок/бэкап не создавался.
- В распакованной папке видны `level.dat`, `level.dat_old`, `region`, `entities`, `poi`, `DIM-1`, `DIM1`, `dimensions`, `playerdata`, `advancements`, `data`, `serverconfig` и `unified_xaero_worldmap`. Запуск и игровая проверка не выполнялись; содержимое каждого файла побайтно не сверялось.
- Копия мира подготовлена; обновлённые runtime-каталоги и тестовые настройки ещё не активированы. Старый runtime остаётся рабочим до отдельной команды пользователя.

### Выполнено после разрешения пользователя — 20 октября 16:24 +05:00

- Сервер штатно остановлен командой `stop` с сохранением измерений. После установки пользователь подтвердил запуск; живая консоль показывает `Preparing level "jem-test-2026-10-20/world"` и `Done` в 16:19:32. Исходный `world` не перемещался и не запускался обновлённым runtime.
- Активны тестовые `level-name=jem-test-2026-10-20/world`, `gamemode=creative`, `force-gamemode=true` и тестовый MOTD. Новые бэкапы не создавались.
- Установлено 27 runtime JAR, 15 прежних/исключённых JAR сохранены в `jem-test-2026-10-20/previous-runtime-mods`; итоговый каталог содержит 245 JAR. Flan и JEM Claims исключены из активного runtime и клиентского host source. Источник — подготовленные артефакты текущего ManagedPack/Server на commit `4972eaa3`; никаких новых сборок или игровых тестов не выполнялось.
- Старые host dirs сохранены в `previous-host`, старые host JAR — в `previous-host-mods`. Старые root `kubejs`, `defaultconfigs`, `config/TPA`, `config/jem`, `config/ftbquests` сохранены в `previous-runtime`. Относительно этих путей сохранения находятся внутри `jem-test-2026-10-20`. Production properties и AutoModpack policy сохранены там отдельно; credentials, auth и локальные worlds не загружались.
- Клиентская публикация выполнена отдельно после запуска: `jempack preview` → `jempack publish` → history ENTRY6 `2026-10-20T11:24:05.015691140Z`, token prefix `8d1823f`, diff `+31 ~13 -10`. Повторный preview в16:24:24 дал тот же token и `+0 ~0 -0`. Предыдущая ENTRY5/token `a945410` сохранена в native journal для возврата.
- До публикации сервер уже работал с новыми JAR, но выдавал прежнюю опубликованную клиентскую generation: это незавершённая операция выпуска. Она исправлена native publish; успешный вход каждого игрока ещё не подтверждён. Присланный Broken pipe фиксирует обрыв сокета, но сам по себе не доказывает причину на клиенте. Startup WARN про существующие scoreboard teams и skipped Mixins не помешали `Done`; их источники отдельно не исправлялись.
- Это тестовый выпуск: playable stable профиль, friend ZIP и immutable release baseline не продвигались. Локальный Server не менялся этой операцией; локальная обычная publication49 уже опубликована. Все дальнейшие правки обновы по-прежнему принадлежат каноническим исходникам и последующему production выпуску, а не тестовому save.

### Последующее расследование входа и запрет изменений сервера

- После публикации выявлена блокировка Connectivity7.6: серия Broken pipe была посчитана malformed traffic и записала IP в process-lifetime блок. Его early handler молча держит новые каналы30с и закрывает. Диагностический restart снимает эту память, но не является постоянным исправлением первичного обрыва.
- Подготовлен dedicated-only terminal-IO adapter в каноническом JEMCompat; он собран, но не установлен ни в local Server/ManagedPack, ни на MineRENT. Пользователь затем явно запретил трогать и перезапускать сервер: никакой дальнейшей загрузки/перезапуска, публикации или замены миров до новой команды.
- Отдельная клиентская ошибка missing61920a3 journal entry исправляется standalone bootstrap jem.8; это не серверный выпуск. Local/remote journaling, instance routing и штатная миграция описаны в Docs/DEVELOPMENT.md. Финальный вход игрока требует его ручного запуска и подтверждения native update; сборка JAR не доказывает успешный вход.
- Второго свежего архива основного мира пока нет: пользователь первоначально запретил новые бэкапы. Сохранённый корневой world содержит прогресс до тестового переключения. Перед будущим production обновлением по новой команде создать свежий бэкап именно оригинального world; тестовые creative saves не использовать для его восстановления.

### Подготовить до окна переключения

1. Завершить текущие параллельные изменения канонических исходников и `Client/ManagedPack`; записать commit и SHA-256 артефактов. Не замораживать промежуточный набор: при подготовке status показал staged manifest b6d1aa794b42a3df8022c2638830b0011fea3403 и опубликованный локальный 563de6390b9a07f8d24ea9de23dc0c7697d87388 (generation46), то есть `published=false`. Эти значения — наблюдение, не целевая финальная generation.
2. Заранее загрузить и распаковать обновлённые server-owned `mods`, `config`, `defaultconfigs`, `kubejs` и необходимые native host-pack источники в неактивный staging. Client host pack формировать исключительно из канонического `Client/ManagedPack`; не загружать целиком локальный Server, его `local`, мир, auth, журналы, кеши или unlimited-wallet state. Не переносить sandbox JAR, test-only JVM flags и локальный AutoModpack test generation.
3. По живому списку MineRENT зафиксировать точный add/replace/delete список. Сохранить старые runtime-каталоги переименованием при остановке, чтобы возврат не требовал повторной загрузки. Учесть server-only моды отдельно; не заменять server `mods` полным клиентским набором.
4. Проверить все exercised Forge channels и matching JAR, включая зависимости aircraft. Подготовить совпадающий bootstrap отдельно; не менять stable профиль и release baseline во время подготовки теста. Существующий MineRENT AutoModpack journal/identity сохранять, не подменять локальным journal.
5. До переключения прочитать и сохранить текущие production значения `server.properties` в закрытом staging на хостинге. Тестовый вариант отличается только целевым `level-name=jem-test-2026-10-20/world`, `gamemode=creative`, `force-gamemode=true` и явно отмеченным тестовым MOTD. Порт, auth, online-mode, whitelist, RCON и остальные настройки не ослаблять; OP всем не выдавать. При таком force-gamemode новый и вернувшийся игрок входит в creative; администратор может временно выдать survival для боя до следующего входа.
6. Проинвентаризировать mutable state вне world (например, данные модов внутри config). Всё игровое состояние теста должно быть изолировано. Credentials/auth не публиковать и не заменять локальными аккаунтами. Не начинать тест, если владелец внешнего save-path не установлен.
7. Пользователь сказал, что новый бэкап понадобится позже. Сейчас не создавать; согласовать момент и охват отдельно перед реальным окном переключения. Старый сохранённый runtime при переименовании — средство быстрого возврата, не новая архивная копия.

### Быстро перейти на тест — только по новой команде пользователя

1. До остановки все загрузки/распаковки и проверка manifest закончены. Штатно остановить сервер; дождаться сохранения и полного состояния Stopped. Не применять новые JAR поверх работающего процесса.
2. По явному уточнению пользователя 20 октября оставить исходный `world` и тестовый `jem-test-2026-10-20/world` на своих местах. После полной остановки изменить только `level-name` на `jem-test-2026-10-20/world`. Не переименовывать и не перемещать оригинальный мир.
3. Установить проверенный update delta; заменяемые/исключённые runtime JAR переместить в `jem-test-2026-10-20/previous-runtime-mods`, прежние host sources — в `previous-host`, заменяемые скрипты/defaultconfigs — в `previous-runtime`. Установить test properties, сохранив прежний `production-server.properties`. Не переносить local auth/unlimited-wallet state и не менять основной мир или его playerdata.
4. Запустить dedicated server, убедиться в `Done` и отсутствии loader/channel/world ошибок. Изменение `level-name`/папки само по себе не считается успешным переключением.
5. Выполнить native `jempack preview`, проверить точный delta и `jempack publish`; дождаться завершения публикации и записать generation/content token. Автогенерацию на старте не включать. Перед допуском игроков иметь matching bootstrap и понятный native путь установки обновы.
6. Начальный режим — creative, но `force-gamemode=false`: подтверждённый игрок получает временный OP2 через bound test profile и может менять режим. Тестовое состояние остаётся исключительно в тестовом мире. Не переносить креативные предметы, деньги и прогресс в основной мир.

### Быстро вернуть старую рабочую версию без ожидания исправлений

1. По команде пользователя штатно остановить тест и дождаться Stopped.
2. Миры не перемещать. Вернуть production `level-name=world`; тестовая копия остаётся в `jem-test-2026-10-20/world`. Исходный мир не запускать до восстановления выбранного matching runtime.
3. Вернуть сохранённые старые runtime-каталоги, production properties и относящиеся к production внешние saves. Полный runtime возврат нужен вместе с миром: старый мир не открывать случайно обновлёнными модами.
4. Вернуть записанную исходную клиентскую generation через native rollback, сохранив journal/identity. При несовместимом bootstrap требуется отдельно согласованный matching артефакт; не обещать автоматический downgrade. Запустить сервер и подтвердить обычный мир и исходный игровой режим.
5. Исправленная обнова остаётся в канонических исходниках и staging; этот временный возврат её не отменяет и не продвигает release baseline.

### Правки после теста и финальная обнова основного мира

- Любая запрошенная после теста правка обновы по умолчанию предназначена также основному серверу. Исправлять canonical owner в `Mods`, `Client/ManagedPack`, общей конфигурации/скриптах и Atlas по действующим правилам, а не только установленный тестовый JAR. Фиксировать commit/hash и точный состав следующей версии.
- Единственные исключения — явно тестовые creative/force-gamemode/MOTD и диагностические настройки. Они остаются отдельным overlay и не попадают в production.
- Изменения блоков, инвентарей, казны, убийств боссов, рас/прогресса и прочих SavedData в тестовом мире не являются исправлениями кода. Их не сливать с оригиналом. Если исправление требует миграции production save, подготовить отдельную идемпотентную миграцию владельца; не переносить тестовый `.dat` или chunk.
- После одобрения тестов и явной команды финального выпуска взять сохранённый основной мир с его реальным прогрессом, применить последнюю исправленную production сборку, соответствующие world/serverconfig изменения и разрешённые миграции. Использовать исходные production настройки Survival; тестовый мир оставить отдельно.
- Выполнить обычную client-and-server release границу: native publication, matching bootstrap/stable путь, полный кумулятивный friend update со всем kubejs, дата/время и новая baseline только после успешного выпуска. Новый baseline не создаётся на стадии подготовки или тестовой копии.
- Быстрый путь убирает загрузку мира и JAR из простоя; время штатного сохранения, запуска Forge и применения клиентского пакета всё равно необходимо. Фиксированные секунды не обещаются.


## Combat observation (2026-10-20)

`/jemcombat start <target> <seconds>` observes one living target for1–120seconds on the dedicated server; permission2. `/jemcombat status` shows progress; `/jemcombat stop` writes the capture. One active session, at most1024native hurt calls, auto-save at duration/sample limit and server stop. JSON output: `<world>/jem-combat-captures/<uuid>.json`; do not commit personal captures.

Samples separate requested native hurt amount, LOWEST event-stage amounts/cancellation, returned boolean and real net health/absorption changes. They include ticks, tier/difficulty, ordinary equipment SNBT, armor/toughness and effects. Outer aggregates avoid nested double counting. Incomplete callback pairing invalidates the capture and suppresses rates. Game-second rates include idle capture time and are not wall-clock DPS, cooldown or attack-phase measurements. Direct setHealth, pre-super rejection, Curios/capability state and inferred spell IDs are outside its scope. Same-call healing/death prevention can mask net damage. The command does not change equipment, health, tier or the world; run actual combat scenarios only in the authorized sandbox.


## Bound test profile and current delivery

`Docs/DEVELOPMENT.md` owns the exact profile/save/epoch binding and original cutover contract. The two matching marker files are release-profile configuration, not a player command or an authentication bypass. Preserve the native world-local ServerLogin store of original `world`; only the stopped named test save receives a once-only private auth-store swap. Never copy test SavedData/playerdata back to original. No manual NBT balance or race patch is needed: authenticated Roles/CurrencyCore owners apply their epoch receipts.

`/jemkeys preview` shows the current registered-key differences; `/jemkeys apply` applies the curated preset once. Existing clients are not silently remapped during an update. These are client-side commands; do not enter them in the dedicated console.

Shipping Box sales use the owner-authorized native container button, without a scheduler or a new public sale command. Its second tab exchanges physical Coins and withdraws from the same CurrencyCore wallet. Test mode bypasses ordinary selling quotas only when its exact binding explicitly enables `bypassQuotas`; reserve backing and the shared music-disc quota remain active.

### Ordinary public chat —2026-10-21

ServerLogin 1.0.3 previously intercepted ordinary OP2 chat and sent `[local]` system messages only to players in the same dimension within its configured radius (100 blocks in the inspected config). This also removed the normal player-chat sender context used by Chat Heads. The next-update dedicated compatibility adapter keeps LoginEvents authentication checks and lets default chat continue through the native public player-chat path. Explicit admin-chat commands remain native. This is a local candidate; it has not been installed on MineRENT.

# Разработка JustEnoughMods

## Рабочие поверхности

- `Client` — единственный чистый клиентский набор и источник полного ZIP.
- `Mods` — исходники JEM-модулей; runtime-JAR не является исходником.
- `Server` — локальный dedicated server и активный мир, а не источник клиентского ZIP.
- `Docs/events` — по одному актуальному документу на каждый event.
- `Docs/smp-gui` — SMP behavior, click flow, visual language и reusable GUI assets.
- `Docs/SERVER_COMMANDS.md` — единственный справочник server-console операций.
- `tools/jem.py` — release baseline, stable synchronization и point/full ZIP.

## Начало задачи

1. Прочитать ближайший `AGENTS.md`, `AGENTS.logic-locks.md` и owning-документ.
2. Из корня выполнить `python tools/jem.py status`; из `Server` — `python ../tools/jem.py status`.
3. Найти canonical owner, похожую production-реализацию и все её потребители.
4. Для интеграции определить точный установленный JAR/version и исследовать его public API, registrations, config и native lifecycle.
5. Для визуала проинвентаризировать установленный asset/API stack и выбрать representation через `minecraft-visual-authoring`.
6. Изменить owning source и все production relations. Ничего не запускать для проверки без прямого запроса пользователя.

## Карта ответственности

| Область | Единственный источник истины |
| --- | --- |
| Обязательная маршрутизация и запреты | `AGENTS.md` |
| Одобренное поведение, которое нельзя регрессировать | `AGENTS.logic-locks.md` |
| Module ownership, Forge engineering, integration, networking, performance, resources, release и distribution | этот документ |
| Конкретный event, arena и encounter lifecycle | соответствующий `Docs/events/*.md` |
| SMP, claims, shops, profiles, GUI navigation и visual language | `Docs/smp-gui/README.md` |
| Server console и recovery operations | `Docs/SERVER_COMMANDS.md` |
| Forge API/source/bytecode workflow | `minecraft-forge-lab` |
| Новая gameplay system, campaign, boss AI и attack choreography | `minecraft-forge-feature-designer` |
| GUI rendering, model, texture, animation, VFX и sound authoring | `minecraft-visual-authoring` |
| Runtime crash/conflict/cleanup/modpack diagnosis | `minecraft-modpack-auditor` |

Changelog описывает только запрошенные player-visible изменения и не является specification или knowledge base. Исторические причины сохраняются здесь только тогда, когда они меняют будущий инженерный выбор; конкретно отвергнутое поведение хранится в owning event/SMP документе.

## Владение модулями

| Модуль | Единственная ответственность |
| --- | --- |
| `JEMAdaptiveCulling` | Client rendering performance и только culling |
| `JEMAchievementGuide` | Переиспользуемые advancement criteria и guide interaction contracts |
| `JEMWorldBossTiers` | Global World Tier, boss profiles, progression scaling, hosted encounters, arena/revival и pending rewards |
| `JEMTwelveEyes` | Campaign graph, prerequisites, Eyes и locator/site state; внешний контракт — `CampaignApi` и immutable `CampaignTerritory` |
| `JEMCompat` | Изолированные compatibility adapters; drill lifecycle принадлежит самостоятельному addon |
| `JEMDrillLifecycle` | Moving/stationary Create drill wear, replacement, servicing и optional Jade presentation; только Create является обязательным модом |
| `JEMMenuSupport` | Небольшая native menu library без mod ID, registry, gameplay state или network; Claims и Server встраивают один и тот же JAR через Forge Jar-in-Jar |
| `JEMPackCore` | Pack-specific data, resources и integration policy без чужой gameplay state |
| `JEMVillagerTalking` | Изолированные реакции, реплики, сеть и HUD-текст жителей; мод остаётся development-only до отдельного решения о публикации |
| `JEMClaims` | Territories, permissions и protection integration |
| `JEMServer` | SMP, parties, shops и server UX; внутренний Events domain делегирует party operations в `EventParties`, принимает `EventParticipants` и регистрируется через `EventRuntime` |
| `AutoModpack` | Bootstrap, manifest generations, verified delta transfer, managed-file deletion и client rollback для JEM Pack Sync |
| `JEMBootstrap` | Узкий source patch native AutoModpack для повторяемой установки и восстановления до загрузки модов; не вводит второй updater или ownership ledger |

Новый код идёт владельцу домена. Общий контракт появляется только у двух реальных потребителей. Потребитель получает immutable snapshot, query или event и не восстанавливает состояние из tooltip, display name, текста advancement или чужого NBT.

## Forge и интеграции

Порядок выбора решения: существующий JEM contract → data/config/tag → native public API/signal → Forge event → isolated compat adapter → accessor/access transformer → narrow Mixin. Fork, overwrite чужой системы или собственная копия её state machine являются последним вариантом, а не быстрым путём.

- Forge 1.20.1 использует Java 17 и существующий ForgeGradle wrapper каждого модуля.
- Exact installed mod version важнее README другой версии. При отсутствии source исследуется установленный JAR, metadata, registrations и bytecode.
- Optional integration изолируется так, чтобы отсутствие мода не загружало его classes.
- Публичный pack ссылается на точные официальные файлы и hashes; сторонний JAR не патчится и не распространяется как скрыто изменённая копия.
- Native dungeon, ritual, awakening, puzzle, structure interaction и meaningful miniboss sequence сохраняются; JEM адаптирует результат, а не переписывает encounter.
- Mixin targeting сверяется с реальной 1.20.1 сигнатурой; callback/accessor предпочтительнее overwrite и broad redirect.

## State, networking и client presentation

- Server владеет progression, damage, hit geometry, inventory, AI decisions, loot, cooldowns, permissions, event lifecycle и persistent state.
- Client владеет screens, input, camera-independent preview, animation, particles, sounds и visual prediction, но получает authoritative identifiers/state.
- Network payload передаёт минимальный стабильный snapshot или transition, имеет предел размера и не дублирует весь server registry.
- Client-facing entity names передаются translation keys и локализуются клиентом.
- Existing world SavedData для boss defeats, campaign flags и player progression сохраняет schema без ненужной migration или reset.
- Collections, caches и pending requests имеют owner, expiry/size bound и cleanup lifecycle.

## Performance by construction

- Event-driven update предпочтительнее global tick.
- World, structure, chunk, block и entity search всегда ограничены dimension, radius, cadence и result count.
- Recipe/registry analysis кэшируется по lifecycle, а не повторяется для каждого event instance.
- Один scheduler cycle активирует не более одного подготовленного event.
- Render loop не создаёт постоянные temporary collections, textures или network requests.
- Chunk loading является явным дорогостоящим контрактом, а не побочным эффектом поиска.
- Производительность диагностируется по конкретной цепочке logs/profile/source; mod count и догадки не являются причиной.

## Config, data и player learning

- Recipes, loot, tags, advancements, lang, models, sounds и authored variants остаются data-driven, если Java не добавляет поведения.
- Config имеет одного владельца, typed default и migration только при реальной смене schema; runtime config не становится source-файлом проекта.
- Player-facing текст не называет внутренний mod/loader/integration; он описывает предмет, entity, structure или действие.
- Advancement summary, Patchouli completion hint и contextual reference выполняют разные роли и не повторяют один текст.
- Patchouli left page содержит точный completion hint, right page — biome/item/context reference; biome link открывает нужный biome в Nature's Compass и не возвращает игрока в screen loop.
- Authored advancement icons уникальны внутри ветки и семантически узнаваемы.
- JEMCompat использует JEI как единственный индекс рецептов, но по умолчанию показывает собственный правый каталог с видимой циклической постраничной навигацией `текущая/всего`: после последней страницы открывается первая, перед первой — последняя. Разделы: `Оружие`, `Броня и носимое`, `Еда и готовка`, `Create и редстоун`, `Строительство`, `Рыбалка`, `Книги`, `Зелья`, `Транспорт`, Creative-only `Spawn eggs`, `Остальное`; отдельного `Фермерства` нет, его содержимое принадлежит `Еде и готовке`. `Строительство` делится на `Мебель и декорации`, `Природа`, `Контейнеры`, `Праздничные украшения`, `Строительные блоки`; листья, саженцы и сосульки принадлежат `Природе`, redstone block — `Create и редстоун`. Оружие, Progression eligibility и netherite-level fallback durability используют один canonical combat contract: реальные vanilla/modded classes, stack-level main-hand damage, combat use animation, audited include/exclude tags и только затем ID hint; edible и block items не становятся оружием без explicit include. `Транспорт` имеет приоритет для audited mount spawn eggs, Happy Ghast harnesses, paragliders и elytras и добавляет localized tooltip с реальным способом передвижения. Классификация назначает ItemStack ровно одному leaf-разделу; обычный JEI включается отдельной кнопкой.

## Campaign и World Tier

- `jemcompat:campaign/root` содержит ровно двенадцать direct always-visible MAIN branches, каждая читается как один путь sub-boss → MAIN.
- MAIN boss имеет не более одного JEM progression layer; sub-boss не получает новый искусственный gate.
- Экипировка получает world-tier усиление только через зачарование `Прогрессия` из книги; boss reward profile не добавляет предмету скрытые атрибуты, ability multiplier или tooltip масштабирования.
- Все Eye-awarding MAIN encounters достижимы до первого входа в End; End-only encounter не переносится ради Eye progression.
- Native victory signal переводится в canonical boss key. Death event, advancement или scripted completion являются adapters, а не независимыми владельцами победы.
- Campaign achievement не зависит от Creative/Survival branch и остаётся доступным через summon/spawn egg/command kill workflow пользователя.
- Vanilla Ender Dragon, Wither, Guardian и Elder Guardian не получают JEM boss-frenzy speed modifier.
- Записанный campaign audit переиспользуется, пока roster или native encounter действительно не изменились.
- Registered structure сохраняет native boss anchor только при первом наблюдении legitimate living boss или native structure signal; после verification координата immutable, поэтому death position, revive command, non-structure encounter, host position и generic center не могут заменить anchor.
- Native structural boss admission атомарно отклоняет второго living boss того же entity type в canonical arena через cancelable `EntityJoinLevelEvent`; duplicate никогда не добавляется в world/chunk/network tracking и не удаляется через `discard()` внутри join callback.
- Ordinary JEM-owned boss death сохраняет due time в `WorldTierData` на `Overworld gameTime + 72_000`; queue восстанавливается из SavedData после restart, а living boss, manual revival или Raid availability атомарно снимает pending countdown.
- Raid participation принадлежит canonical arena history, а не Party: player UUID записывается при фактическом старте и больше не допускается к Raid той же арены. Raid cleanup хранит pending replacement до появления одного fresh normal boss на verified native anchor.

## Pack и runtime contracts

- ServerCore 1.5.2 сохраняет `activation-range.enabled=false`, а Chunky 1.3.146 остаётся operator-controlled server pre-generation владельцем. Neo Voxy, VSS и их managed configs удалены из server/client distribution: закрытая лицензия Neo Voxy не позволяет доставить matching renderer всем игрокам, поэтому server-side VSS создавал обязательный channel и client workload без полного публичного feature contract. GGTU также отсутствует и не является dependency.
- Fiw AntiCheat (`fiw_mods_api`) отсутствует в server runtime и client distribution: версия 2.1.0 объявляет `side = BOTH`, принимает только protocol `1` для `fiw_mods_api:main` на обеих сторонах и требует client challenge/response для модов и resource packs, поэтому её нельзя считать server-only защитой или включать без отдельного одобренного feature contract.

- GraveStone 1.0.35 владеет новыми смертями; Jack's Gravestones сохраняет legacy registry/saved-data recovery, но не захватывает новые inventories при наличии GraveStone.
- Все неполученные graves сохраняются между смертями; любой player может recover/break grave, environment/mobs не уничтожают grave, equipped Backpacked backpack остаётся на игроке.
- TPA new-world cooldown равен пяти секундам; `/deaths` и `/death` принадлежат JEM, `/back` остаётся TPA.
- `options.txt` distribution template сохраняет data version `3465`, language, полный resource-pack order и все Legacy keybinds.
- TLauncher export содержит matching version JSON/JAR и sanitized `TLauncherAdditional.json` без локальных пользовательских путей.
- Safe-mode/crash recovery не перезаписывает distributable options из случайно изменённого runtime-файла.

## Build, deployment и distribution

Build или runtime action выполняется только по прямому запросу пользователя. Когда такой запрос дан:

1. Production JAR завершает `jar` через `reobfJar`; unobfuscated dev JAR не устанавливается.
2. Client-loaded module заменяется в названном active client; shared module также обновляет dedicated server.
3. Перед заменой server JAR сервер корректно останавливается, JVM должна завершиться, затем JAR заменяется и сервер запускается по запрошенному workflow.
4. `test jem` является development client; фактический `--gameDir` берётся из его свежего `latest.log` перед установкой.
5. Stable `JustEnoughMods` и `CLIENT_READY` не используются как development target без явного указания пользователя.
6. Client-distributed files обновляют `Client/ManagedPack` и публикуются только через native AutoModpack. `Client/JustEnoughMods` остаётся bootstrap-only installer template; `sync-stable` при явном вызове может заменить только matching `mods/jem-bootstrap.jar` и не копирует gameplay/config files в active gameDir.
7. `package-update` сравнивает `Client/ManagedPack` с immutable baseline и создаёт точечный пакет только с папкой `Файлы`, сохраняющей managed paths, и файлом `Что делать.md`: удаления перечисляются внутри инструкции, а копирование описывается отдельно для каждого затронутого корневого каталога (`mods`, `config` и других); каждый friend/shared ZIP дополнительно включает всё актуальное дерево `Client/ManagedPack/kubejs`, даже если эти файлы не изменились относительно baseline. `package-full` архивирует bootstrap installer из `Client/JustEnoughMods`, добавляет canonical `Client/ManagedPack/resourcepacks` и `Client/ManagedPack/config/fancymenu`, сохраняет distributable `options.txt` с готовыми keybinds и порядком resource packs и атомарно обновляет `Client/JustEnoughMods.zip`; остальные gameplay mods/configs приходят после подключения через AutoModpack.
8. Runtime-only personal files и launcher-local metadata не распространяются и не удаляются синхронизацией.

## JEM Pack Sync

- Один и тот же `mods/jem-bootstrap.jar` устанавливается на dedicated server и вручную на новый клиент. Текущий установленный файл — upstream AutoModpack `5.0.0-rc.1`; исправленная source revision `jem.4` поверх `5.0.0-rc.1` принадлежит `Mods/JEMBootstrap`, основана на exact upstream commit `4348bf2532bb8261af3297cf2480a3a0ee91befb` под LGPL-3.0-or-later и сохраняет native downloader, planner, transaction, object store и ownership ledger. Исходный commit, hashes затронутых файлов, native source patch и лицензия сохраняются в модуле; готовый сторонний JAR побайтно не патчится.
- `python Mods/JEMBootstrap/prepare.py` получает pinned upstream source и применяет `patches/reliable-updates.patch`, исключая tests и fixtures. `--source <локальный upstream checkout>` берёт тот же commit через git archive. Только явно запрошенный `--build` запускает upstream Gradle wrapper и `oneJar` для Forge 1.20.1; результат находится в `Mods/JEMBootstrap/build/libs/jem-bootstrap.jar`. Test, verification и formatting tasks отключены. Подготовка исходников не меняет Client, Server, launcher profiles или release baseline.
- Upstream Gradle plugins требуют build JVM Java 25; путь можно передать через `--java-home <JDK>` без изменения JVM TLauncher. Runtime class target остаётся Java 17. Native AutoModpack version остаётся `5.0.0-rc.1`, чтобы существующий сервер не скачал upstream JAR обратно через version synchronization; изменённый файл явно отмечен manifest attributes `JEM-Bootstrap-Revision: jem.4` и `JEM-Upstream-Commit`. Эти source patches сохраняют текущий wire contract.
- Исправленная revision ждёт helper lease в BootRecovery до чтения состояния и загрузки active JAR; при таймауте останавливает запуск вместо загрузки старых модов. Recovery признаёт выполненную замену только при совпадении exact planned result и сохранённых исходных bytes в CAS; настоящая сторонняя правка вызывает native replan. Сам helper получает loader type и native loader service от запустившего его клиента и выполняет bounded native replan без внешних скриптов.
- Первый Windows-запуск публикует распакованный loader impl через atomic directory move, но при `AccessDeniedException` от Windows provider безопасно повторяет ещё не выполненный перенос обычным move; если atomic операция фактически завершилась, состояние распознаётся по исчезнувшему source и существующему target. Поэтому чистому клиенту не требуется заранее созданный `automodpack/client/impl-cache`.
- Все native state/config writes сначала публикуются atomic file replace; если Windows provider сообщает общий `AccessDeniedException` вместо специального unsupported-atomic результата, владелец проверяет, не завершился ли перенос фактически, и только при сохранённом temporary повторяет замену обычным same-filesystem move. Это распространяется на `connection.json`, journals, selection, ownership и остальные записи через единый `DurableFiles`.
- Для текущей mirrored generation authoritative ownership checkpoint берётся из matching head document при совпадении journal sequence, contentToken и policySha1; policy bytes по-прежнему проверяет native CAS. Replay старых generations остаётся отдельным native механизмом, поэтому пропущенные пользователем промежуточные версии не блокируют установку текущей. После успешного update commit native клиент снова следует серверу; detached не снимается при сохранении более нового selection intent.
- Для активации этой revision потребуется один раз передать друзьям новый bootstrap и установить тот же файл на сервер: старый updater не может получить исправление собственных ранних классов через JEM gameplay JAR. Bootstrap исключён из served pack; обычные последующие pack updates продолжат доставляться через native AutoModpack. Сборка и публикация revision выполняются только по отдельному запросу; наличие исходников не означает обновление установленного bootstrap.
- `Client/ManagedPack` остаётся единственным полным источником клиентской generation. `python tools/jem.py stage-pack-sync` атомарно пересобирает `Client/ManagedPack/jem-pack/manifest.json` с размером и SHA-256 каждого управляемого файла и полностью обновляет строгий allowlist в `Server/automodpack/host-modpack/main`; `from-server` не используется, поэтому dedicated-server-only, world и приватные runtime-файлы не могут попасть в клиентскую generation. `Client/JustEnoughMods` содержит matching version JSON/JAR, sanitized launcher metadata, distributable options, `mods/jem-bootstrap.jar` и пустые обязательные каталоги; `package-full` берёт FancyMenu и resource packs непосредственно из canonical managed source, не создавая их вторую редактируемую копию в installer template.
- Если development profile уже содержит active AutoModpack installation, `sync-test` не копирует и не удаляет его gameDir files: managed source публикуется через `stage-pack-sync` и native `/jempack publish`, а установку/переключение generation выполняет только AutoModpack. Для compile dependencies активного профиля используется его `automodpack/client/active/mods`, но этот projection не является deployment target.
- После native update полностью закрыть Minecraft и дождаться завершения helper перед новым запуском: новый процесс удерживает старый active projection и препятствует его замене на Windows. Для незавершённой или quarantined transaction существует `python tools/jem.py recover-pack-sync --transaction <полный путь к update-transaction.json.corrupt-...>`: команда работает только с development profile при отсутствии его Java-процессов, проверяет selected installation и неизменённый bootstrap, затем через JDK JShell вызывает native `UpdateAttempt.resume` с настоящим Forge `ModpackLoader`. Недостающие policy documents из mirrored journal доставляются из local server object store только при совпадении exact SHA1 через `ClientObjectStore.storeObject` под native mutation lock. Native planner пересчитывает операцию с учётом уже выполненных замен и текущих editable files; executor фиксирует projection, generation, selection и ownership ledger. После успешного commit и native headMatchesActive клиент снова присоединяется к синхронизации через native setDetached(false). JEM не заменяет JAR вручную и не подделывает active-state.
- JEM не запускает отдельный client downloader или блокирующий pre-sync verifier. Целостность доставленных объектов, transaction, ownership-safe deletion и восстановление выполняет AutoModpack; JEM manifest доставляется тем же host layer в той же generation и остаётся серверным audit-снимком её исходного содержимого.
- `Server/automodpack/server.conf` публикует `mods`, `config`, `defaultconfigs`, `kubejs`, `resourcepacks`, `shaderpacks` и `bivrik` через обязательную группу `main` в режиме `HOLEPUNCH` на Minecraft-порту.
- JEM никогда не передаёт и не заявляет владение `options.txt`, `servers.dat`, `saves`, `screenshots`, `logs`, `crash-reports` и launcher metadata. Удаляется только путь, присутствовавший в предыдущем опубликованном AutoModpack generation.
- `/jempack preview`, `/jempack publish`, `/jempack history` и `/jempack rollback <generation>` напрямую передают выполнение нативным `automodpack generate preview`, `generate`, `generate history` и `generate revert <generation> confirm`, сохраняя их асинхронные операции и сообщения об ошибках. Bootstrap JAR не входит в публикуемый pack и обновляется отдельно одним и тем же файлом на server/client.
- `generate-modpack-on-start` постоянно выключен: после первой явной публикации сервер загружает последнюю journal generation и не создаёт поверх rollback новое поколение из текущего source tree при рестарте.
- После любого добавления, замены или удаления управляемого файла сначала выполняется `python tools/jem.py stage-pack-sync`, затем `/jempack preview`, и только после проверки списка изменений `/jempack publish` создаёт новую доступную клиентам generation. `python tools/jem.py status` сравнивает SHA-1 staged `jem-pack/manifest.json` с последней journal generation и показывает `packSyncPublication.published=false`, пока клиентская generation не опубликована.

## Память причин, которые меняют будущие решения

| Симптом прошлого проекта | Первопричина | Постоянное решение |
| --- | --- | --- |
| Boss kill не двигал World Tier | Animated/scripted death обходил общий death event | Native completion signals переводятся в canonical victory owner |
| Торговец терял injected offers | Integration пересобирала native trade pool | Адаптировать native pool/result, не создавать параллельный |
| Locator не находил известную structure | `skipKnownStructures` исключал уже открытые structures | Значение параметра выводится из product contract, не из имени API |
| GUI прыгал и терял выбор | Container переоткрывался и client предсказывал удаление server-owned slot | In-place refresh и стабильная client view state |
| Profile preview дёргал live player/camera | Vanilla preview вращал world entity | Preview-only `RemotePlayer` с реальным `GameProfile` |
| Clipping оставался после GUI | GPU scissor выключался без pop GUI scissor stack | Каждый `GuiGraphics.enableScissor` имеет один `disableScissor` |
| Model/texture выглядели сломанно | Несовместимые geometry, atlas size, UV и renderer contract соединялись по имени | Visual authoring начинается с format/topology/UV/pivot contract |
| Server stalls и disconnects | Одновременные arena/recipe scans и unbounded tick work | Queue, cached analysis и bounded event-driven work |
| Обновление исчезало или попадало не в тот client | Source, built JAR и несколько TLauncher profiles считались одной поверхностью | Отдельные source/build/runtime/distribution surfaces и точный `--gameDir` |
| Исправление ломалось после следующего ZIP | Runtime JAR/config правился раньше source owner | Сначала owning source, затем controlled deployment и Client sync |

## Внешние репозитории и подключения

### Рабочие инструменты

- `MCDxAI/minecraft-dev-mcp` — целевой Minecraft MCP: Minecraft source/mappings, exact mod-JAR metadata/decompile/search, Mixin и Access Transformer analysis. Он заменяет ручное угадывание API, но не source ownership и не Forge skill.
- `adhi-jp/minecraft-blockbench-mcp` — целевой Blockbench MCP: scoped Java/GeckoLib geometry, textures, animation authoring, validation, undo и viewport frames с shared-secret bridge.
- `minecraft-dev/MinecraftDev` — IntelliJ support для человека; улучшает IDE, но не даёт Codex отдельного runtime capability.
- `MopicMP/gltf-to-minecraft` — optional Blockbench import для лицензированного GLTF/GLB, который действительно переводится в Minecraft cubes/bones; smooth mesh не является готовой Minecraft geometry.

Подключение первых двух требует отдельного решения пользователя и локальной установки. До подключения skills обязаны работать через source/JAR inspection и обычные файлы, не притворяясь, что MCP доступен.

### Архитектурные эталоны

- `MinecraftForge/MinecraftForge`, `MinecraftForge/MDKExamples`, `SpongePowered/Mixin`, `ParchmentMC/Parchment` — authoritative API, minimal setup, injection и mappings references внутри Forge workflow.
- `Creators-of-Create/Create` и `Creators-of-Create/Ponder` — composable visible systems и in-world teaching.
- `AlexModGuy/AlexsCaves` — thematic ecology и связь biome/structure/entity/resource/boss.
- `Tslat/Advent-Of-Ascension` — пример composition behaviors и animations; паттерн изучается, реализация не копируется.
- `rehan-remade/universal-modder` — источник принципов source-first, vertical slice и asset provenance; его automation/testing/publishing stack и общий Minecraft workflow в JEM не устанавливаются.

### Runtime-библиотеки только по feature contract

- GeckoLib — authored animated model, controllers и animation keyframe events, когда vanilla renderer недостаточен.
- SmartBrainLib — несколько новых JEM entities действительно разделяют sensors, memories и reusable behaviors; один boss не оправдывает dependency автоматически.
- Lodestone/Particle Core — сначала переиспользуются уже установленные APIs/assets; собственный particle engine не создаётся параллельно.
- Photon — не принимается по умолчанию: добавляет editor/runtime surface и имеет открытые Forge 1.20.1 rendering/shader и dedicated-server compatibility risks.
- Ponder — только для самостоятельной composable system, которой необходимо animated in-world teaching.

Runtime dependency принимается только если она удаляет JEM-код, закрепляет единый контракт или даёт недостижимое текущим stack качество при совместимости с Forge 1.20.1, Epic Fight, Embeddium/Oculus и dedicated server.


## Границы после архитектурного разделения

Events остаётся внутри JEMServer: его event records находятся в общем `jem_smp_v2`, а ordinary hosted arenas и party workflow используют существующих владельцев. Отдельный JAR потребовал бы переноса общего persistence и новых обязательных связей. `EventNetwork` сериализует feature packets и передаёт entry intent в SMP adapter; `EventAttempts` подготавливает detached state до изменения сохранённого event. Общие boss arena, revival и reward primitives остаются в World Boss Tiers.

Drill addon публикует фактический `DrillLifecycleEvent` для work/replacement/service. Pack Core слушает этот event только при наличии addon и владеет pack advancement IDs. Moving-only batch/final-service условия сохранены. Compat больше не регистрирует drill Mixins и не содержит копию drill lifecycle. JEI выбирает свой overlay при discovery плагина, а optional Curios policy находится за изолированным adapter.

Claims и Server используют `com.siirio.jemmenus` из одной версии MenuSupport. `GAMELIBRARY` помещает библиотеку в игровой module layer, где доступен Minecraft; `LIBRARY` для этих menu classes недопустим. Native Jar-in-Jar выбирает одну копию по одинаковым coordinates/version. Owner-scoped menu reuse сохраняет прежнее разделение Claims/Server. Вручную устанавливать library JAR не требуется. `-slim.jar` является промежуточным файлом и не распространяется.

### Persistence и установка

Сохраняются `jem_smp_v2`, `jemcompat_main_campaign`, `jem_claim_zones`, `jem_world_boss_tiers`, `jem_hosted_boss_rewards`, прежние NBT keys и registry IDs. Drill NBT `JemDrillWear`, `JemDrillRepairs`, `JemDrillReplacementPos` и legacy `jemcompat` resource namespace не меняются. Перенос Java не создаёт новый world schema и не повторяет исторические victories/rewards.

SMP channel `jem_server:smp_v2` теперь использует protocol `3`: typed action kinds/inputs заменяют action JSON, Update включает session/base/current revision, Query поддерживает bounded resync. Требуется одновременная установка matching Server JAR на клиент и сервер. Старый Compat необходимо заменить новым вместе с добавлением `jem_drill_lifecycle-1.0.0.jar`; две версии Compat/addon одновременно недопустимы. Claims остаётся dedicated-server artifact, Adaptive Culling — client-only, Villager Talking — development-only.

Build order: World Boss Tiers и Advancements → Twelve Eyes и Drill Lifecycle → Pack Core; MenuSupport → Claims → Server. Output каждого модуля находится в `build/libs`; подготовленный набор и installation manifest — `build/architecture-revamp/staged`. Live runtime, clean Client, stable profile и release baseline не обновляются архитектурной задачей.

### Resource ownership

World Boss Tiers загружает standalone `defaults/bosses` первым, затем canonical `bosses` через native highest-resource selection; одинаковый logical ResourceLocation из Pack Core/user datapack заменяет default независимо от JAR order. Профили и thresholds сохранены.

35 overlapping Blood Moon defaults перемещены в native builtin `jem_server:event_defaults`, который регистрируется только без Pack Core. При наличии Pack Core canonical tags принадлежат ему; external datapacks сохраняют native precedence. Standalone default pack не добавляет mobs к pack-specific `replace:true` pools.

| Совпадающий путь | Разрешённая семантика |
| --- | --- |
| `assets/jemcompat/lang/en_us.json` | Native language stack объединяет раздельные ключи Compat, Drill Lifecycle и Pack Core |
| `assets/jemcompat/lang/ru_ru.json` | То же объединение с сохранением localization keys |
| `data/forge/loot_modifiers/global_loot_modifiers.json` | Native additive modifier registrations Server и World Boss Tiers |
| `data/jem_world_boss_tiers/tags/items/progression_excluded.json` | Standalone empty base и additive pack values, без replacement |

### Packet catalog

Все C2S identity decisions получают player из authenticated sender. Enum/UTF/count bounds проверяются в decoder, permissions и mutations — у use-case owner на server thread. `consumerMainThread` не получает второй `enqueueWork`; старый TierSync handler использует собственный main-thread dispatch. UTF limits ниже являются character limits; native UTF decoder отдельно проверяет encoded byte length до выделения строки.

| Owner/channel/version | ID и direction | Данные, пределы и use case | Recipients и cleanup |
| --- | --- | --- | --- |
| SMP `jem_server:smp_v2` / 3 | 0 C2S Query; 1 C2S Action; 2 S2C Update; 3 C2S Revive | Query filter64/search160/page0..10000; typed no-input/party/player/message inputs, total C2S4096 bytes; Update NBT1MiB с allocation accounting и session revisions; revive target UUID проверяет encounter owner | Selected viewer; subscriptions/replay state очищаются при logout/stop; close снимает view |
| Events `jem_server:events` / 9 | 0 S2C Prompt; 1 C2S Choice; 2 S2C Zones; 3 S2C Mob; 4 S2C Beam; 5 S2C Telegraph; 6 S2C AttackVfx | Prompt occupants≤256, zones≤32, typed entry kind; server-owned IDs/dimension/geometry/timing; finite attack span≤512; presentation не определяет damage | Event viewers/participants; client logout/world unload, server respawn/dimension invalidation и event lifecycle cleanup |
| Combat `jem_server:combat_v2` / 4 | 0 S2C Update; 1 C2S Claim; 2 C2S Vote | NBT accounting1MiB; result/event UUID; persisted reward owner и active participant vote validation | Combat participants; result/snapshot maps logout/stop, HUD logout/world unload |
| Navigation `jem_server:navigation` / 2 | 0 S2C Points | ≤64 points, subsystem16/label80, ID/dimension/position/expiry; authorization у party/shop owner | Requesting player; expiry, logout/stop, optional Xaero teardown |
| Landmarks `jem_server:landmarks` / 4 | 0 S2C bounded Snapshot/delta/reset; 1 C2S Remove | ≤4096 records/removals per frame, configurable syncBatchSize default128; name80/key160/category32/creator64; boss-only global, legacy ordinary creator-only, remove requires creator and excludes global boss | Bootstrap queue one frame/20 ticks/player; logout/offline/stop cleanup; persisted discoveries and personal legacy remain |
| Map `jem_server:map` / 5 | 0 S2C Snapshot; 1 C2S Request; 2 C2S Publish; 4 C2S Unclaim; 5 S2C Events; 6 C2S EventTeleport; 7 C2S BossTeleport | own claims≤16384/active event areas≤64; exact min/max rectangle; legacy publish name80 + dimension/position сохраняет только creator-private Landmarks; event/boss packets carry only server-owned UUID; boss ID resolves through discovered system Landmark + canonical registered arena + Campaign eligibility; request cadence20 ticks, mutations используют общий action throttle | Requesting map viewer; Blood Moon/Resource Rush area exists only while its event is active; hosted Boss Fight and Boss Raid share one canonical structure layer; one bounded pending official travel/player with chunk-ticket expiry; per-player event cache offline/stop, client disconnect |
| Claims view `jem_server:claim_boundaries` / 1 | 0 S2C Snapshot | ≤256 bounds, tool/name256, dimension/selection; Flan остаётся permission owner | Player holding territory tool; server logout/stop и client lifecycle |
| Fishing `jem_server:fishing_odds` / 1 | 0 S2C ProfileSync | bounded JSON131072 chars, ≤1024 validated rod profiles; сервер владеет odds | Login recipient; existing FishingTooltips logout очищает client profile state |
| World Tier `jem_world_boss_tiers:main` / 5 | 0 S2C TierSync | tier/count/next threshold + ≤1024 weapon IDs | Connected players; current tier/registry owner lifecycle |
| Campaign `jem_twelve_eyes:main` / 4 | 0 S2C OpenMap; 1 S2C GateWarning; 2 S2C Ending | position/dimension/marker key≤256/shared acknowledgement; two component JSON strings≤32767; empty ending intent | Target player; shared locator opens map without personal duplication; native singleplayer, warning cleanup and approved credits preserved |
| Villagers `jem_villager_talking:villager_speech` / 5 | 0 S2C Speech | line64/role32/name64, ordinal1..4 и bounded scene delay | Local scene viewers; feature HUD lifecycle; development-only distribution |

SMP сохраняет 64 replay receipts на игрока и отвергает тот же request UUID с другим typed payload. Action/revive/query work ограничивается cadence; query bursts coalesce до последнего intent. Повторные cooldown/replay replies не пересобирают view. Prizes projection постраничная; reward totals и выдача остаются у `PendingRewardContainer`, source filtering также выполняется там. Unexpected action exceptions логируются с player/request/kind/record context; ожидаемые failures имеют `SmpActionFailure.code()`.

Автоматические тесты и fixtures удалены по прямому запросу пользователя; Gradle test tasks отключены, JUnit отсутствует. Runtime test scenarios запрещены. Build/launch/analysis выполняются только при отдельном прямом запросе согласно AGENTS.md.

## Xaero ownership and matching artifacts

Terrain owner — native Unified Xaero Worldmap 2.0.1 + WorldMap 1.46.0, with existing Minimap 26.4.2 and embedded XaeroLib 1.7.3. Optional `xaero.shared` adapters extend only native all-dimension reconciliation and full-cave acceptance; no map store/renderer is duplicated. Native addon's CC0-1.0 declaration is present in META-INF/mods.toml and author description. Exact official Forge artifacts are staged in `Client/ManagedPack` and local Server; active AutoModpack profile and bootstrap-only installer are untouched.

Boss discovery owner remains ServerData.Landmarks. Physical discovery and successful locator events are producers, Campaign saved caches are historic discovery evidence, and read-only RaidArenaApi known-position lookup supplies existing arena identity. Structure/arena registration and defeat state never become an implicit reveal. Details and privacy/recovery contract live in Docs/smp-gui/README.md.

Activating these source changes requires matching production JEM World Boss Tiers, Twelve Eyes, Pack Core and Server artifacts. Campaign and Landmark channel protocol4 require the updated client counterparts; installing only external map JARs cannot activate the new JEM adapters and discovery contract. Builds, gameplay launches, active-client installation, publishing and push are separate explicitly authorized actions.

Для сборки этого обновления compile dependencies берутся из актуального `Client/ManagedPack` через JEM_INSTANCE/jem_instance: bootstrap-only installer и активный AutoModpack profile не являются источниками новых версий. Это не разрешает прямую установку или синхронизацию active gameDir. TwelveEyes извлекает XaeroLib из выбранного WorldMap, Server использует именно этот matching extracted artifact.

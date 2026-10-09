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
| `JEMWorldBossTiers` | Global World Tier, boss profiles и числовая нормализация |
| `JEMTwelveEyes` | Campaign graph, prerequisites и Eyes |
| `JEMCompat` | Изолированные adapters к native behavior сторонних модов |
| `JEMPackCore` | Pack-specific data, resources и integration policy без чужой gameplay state |
| `JEMVillagerTalking` | Изолированные реакции, реплики, сеть и HUD-текст жителей; мод остаётся development-only до отдельного решения о публикации |
| `JEMClaims` | Territories, permissions и protection integration |
| `JEMServer` | SMP, parties, shops, events и server UX commands |
| `AutoModpack` | Bootstrap, manifest generations, verified delta transfer, managed-file deletion и client rollback для JEM Pack Sync |

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

## Pack и runtime contracts

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
4. `JustEnoughMods Server Test` является development client; фактический `--gameDir` берётся из его свежего `latest.log` перед установкой.
5. Stable `JustEnoughMods` и `CLIENT_READY` не используются как development target без явного указания пользователя.
6. Client-distributed files сначала обновляют `Client`, затем `python tools/jem.py sync-stable` обязательно синхронизирует managed set в игровой TLauncher-профиль `stableClient` (`versions/JustEnoughMods`); MineRENT deployment не завершён, пока `status` показывает `stableMissing` или `stableChanged`.
7. `package-update` сравнивает `Client` с immutable baseline и создаёт точечный пакет только с папкой `Файлы`, сохраняющей пути от `versions/JustEnoughMods`, и файлом `Что делать.md`: удаления перечисляются внутри инструкции, а копирование описывается отдельно для каждого затронутого корневого каталога (`mods`, `config` и других); каждый friend/shared ZIP дополнительно включает всё актуальное дерево `Client/JustEnoughMods/kubejs`, даже если эти файлы не изменились относительно baseline, чтобы пакет не зависел от наличия у получателя скриптов, data-файлов, конфигурации, примеров или локализации; отдельные `README.md`, `DELETE.txt` и `manifest.json` не создаются. `package-full` создаётся только по запросу.
8. Runtime-only personal files и launcher-local metadata не распространяются и не удаляются синхронизацией.

## JEM Pack Sync

- Один и тот же `mods/jem-bootstrap.jar` устанавливается на dedicated server и вручную на новый клиент; это неизменённый AutoModpack `5.0.0-rc.1` из официального release `https://github.com/Skidamek/AutoModpack/releases/tag/v5.0.0-rc.1` под LGPL-3.0-or-later, а JEM задаёт его pack policy, серверный SHA-256 audit manifest и server UX.
- `Client/JustEnoughMods` остаётся единственным чистым источником клиентской сборки. `python tools/jem.py stage-pack-sync` атомарно пересобирает `Client/JustEnoughMods/jem-pack/manifest.json` с размером и SHA-256 каждого управляемого файла и полностью обновляет строгий allowlist в `Server/automodpack/host-modpack/main`; `from-server` не используется, поэтому dedicated-server-only, world и приватные runtime-файлы не могут попасть в клиентскую generation.
- JEM не запускает отдельный client downloader или блокирующий pre-sync verifier. Целостность доставленных объектов, transaction, ownership-safe deletion и восстановление выполняет AutoModpack; JEM manifest доставляется тем же host layer в той же generation и остаётся серверным audit-снимком её исходного содержимого.
- `Server/automodpack/server.conf` публикует `mods`, `config`, `defaultconfigs`, `kubejs`, `resourcepacks`, `shaderpacks` и `bivrik` через обязательную группу `main` в режиме `HOLEPUNCH` на Minecraft-порту.
- JEM никогда не передаёт и не заявляет владение `options.txt`, `servers.dat`, `saves`, `screenshots`, `logs`, `crash-reports` и launcher metadata. Удаляется только путь, присутствовавший в предыдущем опубликованном AutoModpack generation.
- `/jempack preview`, `/jempack publish`, `/jempack history` и `/jempack rollback <generation>` напрямую передают выполнение нативным `automodpack generate preview`, `generate`, `generate history` и `generate revert <generation> confirm`, сохраняя их асинхронные операции и сообщения об ошибках. Bootstrap JAR не входит в публикуемый pack и обновляется отдельно одним и тем же файлом на server/client.
- `generate-modpack-on-start` постоянно выключен: после первой явной публикации сервер загружает последнюю journal generation и не создаёт поверх rollback новое поколение из текущего source tree при рестарте.
- После любого добавления, замены или удаления управляемого файла сначала выполняется `python tools/jem.py stage-pack-sync`, затем `/jempack preview`, и только после проверки списка изменений `/jempack publish` создаёт новую доступную клиентам generation.

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

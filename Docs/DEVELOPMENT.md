# Разработка JustEnoughMods

> Amendment 2026-10-22 is implemented in source under the user request to restore `abd0a4f9` and implement the updated plan. See the implementation section below for current ownership and the delivery receipt for build/publication evidence. Historical conflicting screen/module descriptions do not override it.

Current contract: SMP routes to independent Settlements/Market; Rewards are in normal inventory E on both host types. Integrated/LAN has no JEM Events or parties. Common ordinary Minecraft XP has one x2 baseline; other modifiers apply above it.

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
| SMP, shops, Market, profiles, GUI navigation и visual language | `Docs/smp-gui/README.md` |
| Server console и recovery operations | `Docs/SERVER_COMMANDS.md` |
| Forge API/source/bytecode workflow | `minecraft-forge-lab` |
| Новая gameplay system, campaign, boss AI и attack choreography | `minecraft-forge-feature-designer` |
| GUI rendering, model, texture, animation, VFX и sound authoring | `minecraft-visual-authoring` |
| Runtime crash/conflict/cleanup/modpack diagnosis | `minecraft-modpack-auditor` |

Changelog описывает только запрошенные player-visible изменения и не является specification или knowledge base. Исторические причины сохраняются здесь только тогда, когда они меняют будущий инженерный выбор; конкретно отвергнутое поведение хранится в owning event/SMP документе.

## Владение модулями

Текущий universal contract принят пользователем 2026-10-21 и уточнён при реализации 2026-10-22. Одна production-реализация обслуживает integrated singleplayer и dedicated host; client отвечает за presentation, logical server — за результат. Полный список JEM-модов, точные обязательные зависимости из metadata и границы установки находятся в [universal inventory](universal/README.md).

| Модуль | Единственная ответственность |
| --- | --- |
| `JEMAdaptiveCulling` | Client rendering performance и только culling; dedicated server не нужен |
| `JEMAchievementGuide` | Advancement criteria/providers, curated native placements, RU/EN guide resources и optional Better Advancements/Patchouli; content-aware FTB book projection наблюдает domain APIs, не владеет progression |
| `JEMWorldBossTiers` | Global World Tier и сохранённый roster/threshold profile, boss profiles, hosted encounters, ordinary solo Raid, canonical native structure identity/anchor, revival и pending rewards |
| `JEMTwelveEyes` | Строгие двенадцать original MAIN, prerequisites, Eyes, locators/sites, post-End и ending; сохраняет `jemcompat` namespace и `jemcompat_main_campaign` SavedData |
| `JEMCompat` | Изолированные native compatibility adapters, single-level Excavation и biome codec с сохранёнными `jem_pack_core:*` IDs; ordinary prepared-food quality/diversity (`feature.food`), companion home/recovery (`feature.shelter`) и native time/season presentation (`client.time`); drill lifecycle остаётся отдельным addon |
| `JEMDrillLifecycle` | Moving/stationary Create drill wear, replacement, servicing и optional Jade; Create остаётся обязательной native dependency |
| `JEMIdentity` | Verified principal для integrated sessions, online dedicated session или unlocked Server Login; offline username не заменяет authentication |
| `JEMRoles` / JEM Races | Единственный production Races JAR: пять разработанных рас, canonical `jem_roles` saves, powers, progression, presentation и сохранённые legacy resource/registry/network IDs |
| `JEMEconomy` | Единственный runtime owner `jem_economy` Coins, Wallet/physical ledger, provenance/escrow/receipts, funded native merchants, market и Shipping Box; world SERVER config `[economy] enabled=true` |
| `JEMSettlements` | Одна production-реализация settlements для integrated/dedicated: unlimited towns, residents/housing/happiness, real storage, payroll и native GUI; Coin adapter optional, при Economy absent/off используется существующий EmeraldTreasury |
| `JEMServer` | Общие non-event SMP services, parties, host-authorized shops/profile/map UX и ordinary boss/Raid UI; scheduled/seasonal Events запускаются только при explicit dedicated host opt-in |
| `JEMBootstrap` / AutoModpack | Native generation/download/trust/transaction/ownership owner; source patch `jem.9` исправляет acquisition/projection paths и показывает один file list, risk consent и Download |
| `JEMProfessions` | Development-only consumer Pufferfish-owned category XP/levels/points; не включается в original/shareable pack |
| `JEMVillagerTalking` | Development-only text-only villager scenes/subtitles; не включается в original/shareable pack |
| `JEMClaims` | Development-only; не включается в original/shareable pack |
| `JEMGuiSandbox` | Development-only GUI gallery для `jem sandbox`: relocated native screens, in-memory demo transports и существующий detached race preview; не входит в Client/ManagedPack, Server или AutoModpack. Installation/coverage: `Docs/gui-sandbox/README.md` |

`JEMCurrencyCore` объединён в Economy, а `JEMVisualSandboxR3` больше не производит отдельный gameplay JAR: его developed race scope находится в JEMRoles. Pack Core удалён как source/build/runtime owner; его source/resources распределены по Guide, Compat, WorldTier, TwelveEyes и Server с сохранением content IDs. Menu Support runtime library удалена: требуемые native helper classes стали private source потребителей. Эти старые названия допустимы в неизменяемой истории и compatibility namespace, но не являются production dependencies.

JEMAchievementGuide also owns the single physical [JEM Handbook](handbook/README.md), its native Patchouli recipe/navigation, optional native-manual adapters and shared explanatory data. Compat projects that help into existing JEI/Jade surfaces; domain owners retain gameplay and synchronized state. Handbook opening never awards quest progress, rewards or research. Its fixed native-manual menu channel requires the same fresh Guide artifact on both sides; the FTB book and progress bridge remain intact.

Исторические sandbox receipts описывают прежние ограничения, а не текущий install path. Обычные production Economy/Races/Settlements работают на logical server без отдельной standalone ветки. Professions, Talking и Claims могут оставаться в разрешённой development-среде; это не разрешает их выпуск. Pufferfish's Skills остаётся владельцем profession state, если этот development domain установлен.

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
- Dedicated account writes resolve through `JEMIdentity`. `online-mode=false` is not a feature switch: an unlocked Server Login connection is authenticated, while a locked or unverifiable connection fails closed. Roles and Professions also accept the integrated-server owner.
- Xaero owns its complete screen, ordinary controls and waypoint actions. Approved interaction extensions are the explicit owner Settlement expansion mode and native teleport actions for official server boss/event markers only. Settlement expansion previews an exact rectangular union and confirms one wide two-line native action without an internal separator; official marker travel resolves the server identity rather than client coordinates. The server remains authoritative for ownership, overlap, price and payment; JEM never creates a replacement map screen or personal waypoint.
- Physical Coins use denominations1/10/100/1,000/10,000. Digital balances + escrow/system accounts + `PHYSICAL_OUTSTANDING` equals initial supply plus explicitly accounted authorized issuance (daily settlement income; separately gated local test/development issuance only in its own profile). Ordinary starter/market/withdrawal/exchange cannot mint. World-local denomination bearer pools allow native stacking; the journal binds each operation quantity, and deposits accept no more than backed unredeemed units. Proven legacy normalization preserves value/liability. The five item IDs are `jem_economy:{copper,silver,gold,aether,arcane}_coin`; each item model inherits `item/coin_base` and references separately authored 32×32 front/back/rim textures.
- Non-selectable Spud catalog purchases keep native physical shop stock but settle the numeric native price through JEMEconomy. A buyer payment can redeem backed physical cash into Wallet, reserves through `MARKET_ESCROW`, rechecks loaded stock and capacity, then credits the shop owner's verified UUID. Selectable offers remain physical-shop-only until the request carries a bounded selected-item fingerprint.
- Town treasury is a deterministic Economy account derived from town UUID, never a duplicated `TownRecord` number or chest sum. Only the authenticated owner can move Wallet/cash value into the treasury or return it to Wallet; physical withdrawal then uses the ordinary Wallet issuance flow.
- Existing commodity policy retains `BUY_AND_SELL` or `NONE`. The user's 2026-10-16 market expansion additionally authorizes separately reviewed purchase-only building/decorative stock; admitting a purchase does not authorize Shipping Box, Buyer or NPC buyback. Unknown, damaged, arbitrary-NBT, enchanted and packaged-storage goods remain ineligible for commodity sales. Exact painting variants are server-authored deliveries. Canonical commodity policy remains `Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/item_policies.json`; the purchase/restriction resources and current exports are documented under `Docs/economy`.
- `jem_economy:shipping_box` retains placement ownership,54 manual inputs,9 legacy payout slots and persisted recovery. Public use has one authenticated visitor; fresh explicit sale settles to that seller's ledger, while physical withdrawal is separate. Prior visitor goods/payouts remain account-bound through close/restart/full inventory. Automation is denied while a visitor or incomplete transaction owns the contents; no generic pipe capability is introduced. Money-tab deposit/withdraw/send/exchange reuse Economy-owned receipts and all-five-denomination icon dropdowns. Existing3D item/block model and goods anchors remain native.
- Ordinary daily commodity payout quotas are retired under the 2026-10-21 request; fixed authored prices, funded reserve checks and separate disc/progression/native-restock limits remain. Economy owns the existing market policy and MarketSavedData; Wallet and Town Hall consume its same demand bulletin. Disabling Economy suspends monetary functions and retains all balances, Coins and receipts.
- Vanilla and compatible `AbstractVillager` offers retain native opening, `MerchantMenu`/`MerchantScreen`, full original list, real input slots, selected resource-pack styling and stock/restock callbacks. Logical-server Economy Mixins adapt monetary result takes; the old18-slot custom menu/screen remains removed. The 2026-10-16 complete economy brief authorizes narrow monetary cells, selected quote and funds using actual Coin assets; the native geometry and controls remain unchanged. The client Mixin also synchronizes native selection for the secured Coin click path. Sales credit Wallet, purchases fund NPC_MARKET through existing Wallet/lot redemption. Original item barter remains native. Unknown/protected offers are server-denied. Exact native merchandise admits original NBT with bounded base buyback and enchantment acquisition premiums; no emerald exchange rate or generic equipment fallback exists.
- Coin result actions acknowledge synchronized quote revision/selection and a bounded operation nonce; unquoted monetary takes are denied. The server validates original item terms, both input orientations, account, distance, stock, reserve, quota and output destination. Races owns its existing Coin discount and reads scoped paid-currency callback context to avoid emerald refunds after Coin settlement.
- The Buyer is retired by the2026-10-21 user request. Shipping and `/smp` Market consume the canonical demand-derived quotes, policies and funded settlement; ordinary JEM daily quotas are removed while special disc/progression/native-restock limits remain. The Market screen supports real server-authorized purchase/sale; finished meals remain outside the universal catalog. Outside the explicit Settlement expansion and official server boss/event marker travel adapters above, JEM does not add Xaero dropdown entries, map overlays, waypoint actions, teleport cancellation, custom screens or camera focus. It may ask Xaero's native controls handler to open the ordinary map; shared-terrain/boss adapters remain data-only.
- Native-interface source introduces Economy wallet protocol3 and requires matching Economy/Roles/Server client-and-server artifacts upon an explicit build/release request. Build Identity/Economy before Roles (the optional Coin API is compile-only), then dependent Server. The complete economy overhaul builds matching Economy/Settlements/Compat/Server artifacts and publishes the local managed generation; its final report distinguishes automated and loaded-server evidence from player acceptance.

The authored FTB adventure book belongs to Guide, with canonical authoring, compiled config, native progress/claim boundaries, exact-version adapters and delivery evidence in [Docs/quests/README.md](quests/README.md). Economy publishes additive notifications only after committed operations; Settlements exposes a read-only native mount collection count. Guide consumes these owners without creating new ledgers, profiles or campaign state. Native FTB item/XP reward presentation is retained; JEMRewards owns durable personal item delivery.
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
- `JemCatalogCategories` in JEMCompat is the common, server-safe category owner used by the recipe browser and Economy purchase admission. Client-only JEI catalyst discovery and wearer context are explicit classifier inputs; common cooking-catalyst tags cover native stations on the server. Public market admission is further restricted by Economy's exact prices and permanent prohibitions, never by a second name-based category implementation.
- The narrator keyboard shortcut is disabled only inside `KeyboardHandler.keyPress`; B remains available to racial utility and narration can still be selected through accessibility settings. This input adaptation remains client-only; the separate universal non-event gameplay runs on the logical server.

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
- Ordinary JEM-owned boss death сохраняет due time в `WorldTierData` через три Minecraft-дня; queue восстанавливается из SavedData после restart, а living boss, manual revival или Raid availability атомарно снимает pending countdown. Internal save/calculation units remain native.
- Raid participation принадлежит canonical arena history, а не Party: player UUID записывается при фактическом старте и больше не допускается к Raid той же арены. Raid cleanup хранит pending replacement до появления одного fresh normal boss на verified native anchor.

WorldTier и TwelveEyes имеют разные content policies. WorldTier фиксирует доступные зарегистрированные qualifying bosses и достижимые thresholds в world profile; полный pack сохраняет 0/3/10/18/32. Удаление content не уменьшает сохранённые thresholds и не создаёт defeats. TwelveEyes всегда требует те же двенадцать originals и prerequisites: новая incomplete world не активирует невозможную campaign и сохраняет vanilla End path; существующая active campaign при missing content сохраняет progress и закрывает Start/portal до восстановления content с явным сообщением. По явному запросу 2026-10-24 восстановлены прежние dedicated Boss Fight/Campaign/Raid visual walls, physical collision и teleport restrictions по трёхмерным границам зарегистрированной структуры. Prerequisites проверяют доступ к MAIN и canonical Start.

`StructureEncounterApi` в WorldBossTiers владеет loaded-only native structure discovery, canonical arena UUID и registry association resources. Server делегирует этому API. WorldBossTiers сам предоставляет solo admission/Start/Raid без Server; при наличии Server используется его existing UI/party flow без второго prompt. Matching WorldTier client/server artifacts должны поддерживать protocol 6. Точные контракты: [WorldTier](../Mods/JEMWorldBossTiers/README.md), [TwelveEyes](../Mods/JEMTwelveEyes/README.md), [Guide](../Mods/JEMAchievementGuide/README.md), [Compat](../Mods/JEMCompat/README.md).

## Pack и runtime contracts

- ServerCore 1.5.2 сохраняет `activation-range.enabled=false`, а Chunky 1.3.146 остаётся operator-controlled server pre-generation владельцем. Neo Voxy, VSS и их managed configs удалены из server/client distribution: закрытая лицензия Neo Voxy не позволяет доставить matching renderer всем игрокам, поэтому server-side VSS создавал обязательный channel и client workload без полного публичного feature contract. GGTU также отсутствует и не является dependency.
- Fiw AntiCheat (`fiw_mods_api`) отсутствует в server runtime и client distribution: версия 2.1.0 объявляет `side = BOTH`, принимает только protocol `1` для `fiw_mods_api:main` на обеих сторонах и требует client challenge/response для модов и resource packs, поэтому её нельзя считать server-only защитой или включать без отдельного одобренного feature contract.

- GraveStone 1.0.35 владеет новыми смертями; Jack's Gravestones сохраняет legacy registry/saved-data recovery, но не захватывает новые inventories при наличии GraveStone.
- Все неполученные graves сохраняются между смертями; любой player может recover/break grave, environment/mobs не уничтожают grave, Backpacked-specific equipped-backpack retention is retired with removal of that mod.
- TPA new-world cooldown равен пяти секундам; Death History принадлежит профилю `/smp`, `/back` остаётся TPA.
- `options.txt` distribution template сохраняет data version `3465`, language, полный resource-pack order и все Legacy keybinds.
- TLauncher export содержит matching version JSON/JAR и sanitized `TLauncherAdditional.json` без локальных пользовательских путей.
- Safe-mode/crash recovery не перезаписывает distributable options из случайно изменённого runtime-файла.

## Build, deployment и distribution

Production build/sync, необходимые для явно запрошенного project change, следуют standing authorization в `AGENTS.md`. Tests, automated analysis, gameplay launches, MineRENT/stable changes и release-baseline promotion требуют своего разрешённого scope; текущая реализация не превращает build в player acceptance.

Явно запрошенный overnight player QA использует `.agents/skills/jem-overnight-player-qa/SKILL.md` и durable отчёт в `Docs/playtests/`. Разрешение относится только к указанной локальной сессии: отдельный disposable world/server clone, `test jem`, native AutoModpack и проверенный backup; MineRENT, stable profile и release baseline не входят в этот scope. У JEMCompat нет compile-зависимости от удалённого BackToBed; `/home` принадлежит JEM Server.

1. Production JAR каждого gameplay-модуля завершает `jar` через `reobfJar`; unobfuscated dev JAR не устанавливается. MenuSupport/CurrencyCore больше не являются embedded outputs. JEMRoles производит единственный `jem-races-2.0.0.jar`; существующие native third-party compile libraries не становятся новыми runtime JEM dependencies.
2. Client-loaded module заменяется в названном active client; shared module также обновляет dedicated server.
   Fresh output должен содержать domain resources, private native helpers и required Mixin refmaps. Source owners и matching native API inventory определяются module build files; Guide/Compat/TwelveEyes по умолчанию используют project-relative `Client/ManagedPack`, с optional property/environment override. Успешная компиляция не подтверждает загрузку Mixins, optional-mod matrix или runtime поведение.
3. Перед заменой server JAR сервер корректно останавливается, JVM должна завершиться, затем JAR заменяется и сервер запускается по запрошенному workflow.
4. `test jem` является development client; фактический `--gameDir` берётся из его свежего `latest.log` перед установкой.
5. Stable `JustEnoughMods` и `CLIENT_READY` не используются как development target без явного указания пользователя.
6. Client-distributed files обновляют `Client/ManagedPack` и публикуются только через native AutoModpack. `Client/JustEnoughMods` остаётся bootstrap-only installer template; `sync-stable` при явном вызове может заменить только matching `mods/jem-bootstrap.jar` и не копирует gameplay/config files в active gameDir.
7. `package-update` сравнивает `Client/ManagedPack` с immutable baseline и создаёт точечный пакет только с папкой `Файлы`, сохраняющей managed paths, и файлом `Что делать.md`: удаления перечисляются внутри инструкции, а копирование описывается отдельно для каждого затронутого корневого каталога (`mods`, `config` и других); каждый friend/shared ZIP дополнительно включает всё актуальное дерево `Client/ManagedPack/kubejs`, даже если эти файлы не изменились относительно baseline. `package-full` архивирует bootstrap installer из `Client/JustEnoughMods`, добавляет canonical `Client/ManagedPack/resourcepacks`, `Client/ManagedPack/config/fancymenu` и полное дерево `Client/ManagedPack/shaderpacks`, сохраняет distributable `options.txt` с готовыми keybinds и порядком resource packs и атомарно обновляет `Client/JustEnoughMods.zip`; остальные gameplay mods/configs приходят после подключения через AutoModpack.
8. Runtime-only personal files и launcher-local metadata не распространяются и не удаляются синхронизацией. `tools/workspace.json:releaseExcluded` задаёт отдельные исключения release inventory; `config/jei/world` содержит личную JEI history и не входит в friend ZIP, cumulative copy-forward или новый baseline. Перед сравнением фильтруются и прежний baseline, и текущий источник, поэтому старое наличие личного файла не создаёт инструкцию удалить его у друзей. При repackage фильтруется также исторический published delta. Уже созданный ZIP с личными файлами не считается matching release и пересоздаётся; исключение не удаляет runtime файлы и само по себе не продвигает baseline. Native AutoModpack сохраняет отдельный существующий `packSyncExcluded` owner.

`python tools/jem.py package-universal` создаёт `Client/JustEnoughMods-universal.zip` для fresh Forge 1.20.1 / 47.4.10 / Java 17 instance. В отличие от bootstrap-only `package-full`, archive содержит полные pack-sync included directories, `jem-pack/manifest.json`, matching bootstrap и SHA-256 inventory; launcher version files/options, saves, runtime/auth/personal map state не входят. Bootstrap включён в shareable archive, но не становится собственным managed download. [Установка universal archive](universal/INSTALL.md). Эта упаковка не запускает игру, не обновляет stable профиль, MineRENT или baseline.

Цель production-кода и archive — Windows, Linux и macOS с Java paths и относительными resource names. TLauncher является существующим локальным development workflow, а не runtime dependency. Cross-platform design/source inspection не является пройденной runtime-проверкой на каждой OS.

## JEM Pack Sync

- Один и тот же `mods/jem-bootstrap.jar` устанавливается на dedicated server и вручную на новый клиент. Текущая source revision `jem.9` поверх AutoModpack `5.0.0-rc.1` принадлежит `Mods/JEMBootstrap`, основана на exact upstream commit `4348bf2532bb8261af3297cf2480a3a0ee91befb` под LGPL-3.0-or-later и сохраняет native downloader, planner, transaction, object store и ownership ledger. Исходный commit, hashes затронутых файлов, native source patch и лицензия сохраняются в модуле; готовый сторонний JAR побайтно не патчится.
- `python Mods/JEMBootstrap/prepare.py` получает pinned upstream source и применяет `patches/reliable-updates.patch`, исключая tests и fixtures. `--source <локальный upstream checkout>` берёт тот же commit через git archive. Только явно запрошенный `--build` запускает upstream Gradle wrapper и `oneJar` для Forge 1.20.1; обычный результат находится в `Mods/JEMBootstrap/build/libs/jem-bootstrap.jar`. Source revision `jem.9` применяет переносимый native file-tree transfer для acquisition/projection длинных путей, сохраняя containment/hash/trust/transaction checks; confirmation показывает один file list, одно risk consent и Download вместо повторных warning/countdown screens. Runtime delivery устанавливается отдельным receipt, а не наличием patch. Test, verification и formatting tasks отключены. Подготовка исходников не меняет Client, Server, launcher profiles или release baseline.
- Upstream Gradle plugins требуют build JVM Java 25; путь можно передать через `--java-home <JDK>` без изменения JVM TLauncher. Runtime class target остаётся Java 17. Native AutoModpack version остаётся `5.0.0-rc.1`, чтобы существующий сервер не скачал upstream JAR обратно через version synchronization; изменённый файл явно отмечен manifest attributes `JEM-Bootstrap-Revision`, `JEM-Upstream-Commit` и `JEM-Skip-Shaders`. Source revision `jem.7` сохраняет wire version и опциональный shader distribution profile, добавляя сохранение персональных editable preferences. Клиентское исправление `jem.8` описано ниже; остановленное пользователем MineRENT/local Server обновление не подразумевается заменой клиентского bootstrap.
- Исправленная revision ждёт helper lease в BootRecovery до чтения состояния и загрузки active JAR; при таймауте останавливает запуск вместо загрузки старых модов. Recovery признаёт выполненную замену только при совпадении exact planned result и сохранённых исходных bytes в CAS; настоящая сторонняя правка вызывает native replan. Сам helper получает loader type и native loader service от запустившего его клиента и выполняет bounded native replan без внешних скриптов.
- Первый Windows-запуск публикует распакованный loader impl через atomic directory move, но при `AccessDeniedException` от Windows provider безопасно повторяет ещё не выполненный перенос обычным move; если atomic операция фактически завершилась, состояние распознаётся по исчезнувшему source и существующему target. Поэтому чистому клиенту не требуется заранее созданный `automodpack/client/impl-cache`.
- Все native state/config writes сначала публикуются atomic file replace; если Windows provider сообщает общий `AccessDeniedException` вместо специального unsupported-atomic результата, владелец проверяет, не завершился ли перенос фактически, и только при сохранённом temporary повторяет замену обычным same-filesystem move. Это распространяется на `connection.json`, journals, selection, ownership и остальные записи через единый `DurableFiles`.
- Для текущей mirrored generation authoritative ownership checkpoint берётся из matching head document при совпадении journal sequence, contentToken и policySha1; policy bytes по-прежнему проверяет native CAS. Replay старых generations остаётся отдельным native механизмом, поэтому пропущенные пользователем промежуточные версии не блокируют установку текущей. После успешного update commit native клиент снова следует серверу; detached не снимается при сохранении более нового selection intent.
- Для активации этой revision потребуется один раз передать друзьям новый bootstrap и установить тот же файл на сервер: старый updater не может получить исправление собственных ранних классов через JEM gameplay JAR. Bootstrap исключён из served pack; обычные последующие pack updates продолжат доставляться через native AutoModpack. Сборка и публикация revision выполняются только по отдельному запросу; наличие исходников не означает обновление установленного bootstrap.
- Только для адресного восстановления клиента, который не может опубликовать длинные shaderpack paths на Windows, `python Mods/JEMBootstrap/prepare.py --build --skip-shaders --java-home <JDK25>` создаёт `build/libs/jem-bootstrap-no-shaders.jar` с revision suffix `-no-shaders.1` и `JEM-Skip-Shaders: true`. Такой client-only JAR исключает только `shaderpacks/**` из complete и selected targets до поиска, скачивания, планирования и projection; моды, конфиги, resource packs, generation identity, ownership ledger и native transaction lifecycle не меняются. Его не устанавливают на server, в canonical installer или stable profile и не используют вместо исправления общего pack source.
- `Client/ManagedPack` остаётся единственным полным источником клиентской generation. `python tools/jem.py stage-pack-sync` атомарно пересобирает `Client/ManagedPack/jem-pack/manifest.json` с размером и SHA-256 каждого управляемого файла и полностью обновляет строгий allowlist в `Server/automodpack/host-modpack/main`; `from-server` не используется, поэтому dedicated-server-only, world и приватные runtime-файлы не могут попасть в клиентскую generation. `Client/JustEnoughMods` содержит matching version JSON/JAR, sanitized launcher metadata, distributable options, `mods/jem-bootstrap.jar` и пустые обязательные каталоги; `package-full` берёт FancyMenu, resource packs и заполненный shaderpacks непосредственно из canonical managed source, не создавая их вторую редактируемую копию в installer template.
- Если development profile уже содержит active AutoModpack installation, `sync-test` не копирует и не удаляет его gameDir files: managed source публикуется через `stage-pack-sync` и native `/jempack publish`, а установку/переключение generation выполняет только AutoModpack. Для compile dependencies активного профиля используется его `automodpack/client/active/mods`, но этот projection не является deployment target.
- После native update полностью закрыть Minecraft и дождаться завершения helper перед новым запуском: новый процесс удерживает старый active projection и препятствует его замене на Windows. Для незавершённой или quarantined transaction существует `python tools/jem.py recover-pack-sync --transaction <полный путь к update-transaction.json.corrupt-...>`: команда работает только с development profile при отсутствии его Java-процессов, проверяет selected installation и неизменённый bootstrap, затем через JDK JShell вызывает native `UpdateAttempt.resume` с настоящим Forge `ModpackLoader`. Недостающие policy documents из mirrored journal доставляются из local server object store только при совпадении exact SHA1 через `ClientObjectStore.storeObject` под native mutation lock. Native planner пересчитывает операцию с учётом уже выполненных замен и текущих editable files; executor фиксирует projection, generation, selection и ownership ledger. После успешного commit и native headMatchesActive клиент снова присоединяется к синхронизации через native setDetached(false). JEM не заменяет JAR вручную и не подделывает active-state.
- JEM не запускает отдельный client downloader или блокирующий pre-sync verifier. Целостность доставленных объектов, transaction, ownership-safe deletion и восстановление выполняет AutoModpack; JEM manifest доставляется тем же host layer в той же generation и остаётся серверным audit-снимком её исходного содержимого.
- `Server/automodpack/server.conf` публикует `mods`, `config`, `defaultconfigs`, `kubejs`, `resourcepacks`, `shaderpacks` и `bivrik` через обязательную группу `main` в режиме `HOLEPUNCH` на Minecraft-порту.
- JEM никогда не передаёт и не заявляет владение `options.txt`, `servers.dat`, `saves`, `screenshots`, `logs`, `crash-reports` и launcher metadata. Удаляется только путь, присутствовавший в предыдущем опубликованном AutoModpack generation.
- `/jempack preview`, `/jempack publish`, `/jempack history` и `/jempack rollback <generation>` напрямую передают выполнение нативным `automodpack generate preview`, `generate`, `generate history` и `generate revert <generation> confirm`, сохраняя их асинхронные операции и сообщения об ошибках. Bootstrap JAR не входит в публикуемый pack и обновляется отдельно одним и тем же файлом на server/client.
- `generate-modpack-on-start` постоянно выключен: после первой явной публикации сервер загружает последнюю journal generation и не создаёт поверх rollback новое поколение из текущего source tree при рестарте.
- После любого добавления, замены или удаления управляемого файла сначала выполняется `python tools/jem.py stage-pack-sync`, затем `/jempack preview`, и только после проверки списка изменений `/jempack publish` создаёт новую доступную клиентам generation. `python tools/jem.py status` сравнивает SHA-1 staged `jem-pack/manifest.json` с последней journal generation и показывает `packSyncPublication.published=false`, пока клиентская generation не опубликована.
- Revision `jem.8` separates installed-generation authority from mutable server mirrors. A native commit persists the exact generation witness with the active token/ledger, and journal replacement checkpoints an older active state first. Resolving the active target verifies the policy CAS hash, pack ID and complete content token. Legacy missing witnesses may be read from an existing native client journal identified by a validated shared-CAS ownership receipt; this supplies metadata only, never executable bytes, credentials, consent or another instance's ledger. No synthetic journal entries or missing policies are invented. The active witness's policy/content stay pinned during CAS collection.
- Remembered connection routes now belong to each instance (`history/<pack-id>/route.json`), while native shared secrets and certificate trust retain their existing owners. An explicit native login saves the instance route; a legacy instance without one reads the existing shared record. Thus a later `test jem` login cannot retarget an already remembered `JustEnoughMods` route. Forked servers with the same pack ID may still advertise different content, so one reviewed reconciliation can be legitimate; it must not strand the installed generation by overwriting its only metadata.
- Standalone bootstrap recovery: close the affected game, replace only `mods/jem-bootstrap.jar` with the matching prepared JAR, then launch manually and finish the native confirmation/application. Never raw-copy the managed pack, delete caches, edit active pointers or accept unverified JARs on the player's behalf. Bootstrap-only changes are excluded from the managed publication; no world, server restart, remote journal, friend full pack or release-baseline promotion follows automatically.
- Publication is server availability, not proof of client installation. Native preload intentionally refuses a plan that writes JARs without a Modrinth/CurseForge match and keeps the previous active projection (`Launch apply aborted: unverified jars will not be written during preload`). Locally authored JEM artifacts therefore require explicit native review/application. Before reporting a recovered development profile ready, confirm its native active content token equals the intended mirrored head, the installed JAR hashes match the exact authorized source artifacts, and no transaction remains pending. A crash in the old projection cannot be repaired merely by publishing or repeatedly relaunching. Complete the approved native planner/UpdateAttempt transaction; do not raw-copy files, edit active-state or weaken the preload trust gate.
- Friend recovery for the same shipped Xaero failure is packaged by `tools/client-recovery/package.py` as a cumulative replacement ZIP with player-run `Repair.cmd`. It compiles a standalone Java17 adapter to installed native AutoModpack, pins the exact hotfix/bootstrap, uses already fetched authoritative metadata and requires the player to confirm the bounded plan. This is external distribution tooling, not a mod, trust exception or raw managed installation. The exact delivery and unrun player-acceptance limit are recorded in [the friend recovery receipt](releases/2026-10-21-friend-recovery.json).
- The 2026-10-21 stable-profile recovery demonstrates that a corrected JAR in gameDir does not repair an older native active projection. At 14:11:01 preload refused unverified JAR writes and retained eb4b76b, which still crashed on the removed jem$drag handler. The user explicitly approved applying the exact published hotfix to JustEnoughMods; its native transaction and final installed hashes are recorded in [the recovery receipt](releases/2026-10-21-stable-native-recovery.json). Do not describe publication, a manual gameDir replacement, or another launch as a completed native installation.
- Обязательная передача локального клиента: после каждого managed client change не только опубликовать generation, но и полностью применить её к `test jem` через native AutoModpack до сообщения «готово». Пользователь должен сразу входить на сервер без повторного обновления при подключении. Standing request разрешает native review/application точных опубликованных артефактов текущей задачи; для локальных JEM JAR завершать штатную planner/UpdateAttempt transaction заранее. Сверять active content token с опубликованным head, SHA-256 установленных JAR с разрешёнными артефактами и отсутствие pending transaction. Если игра открыта, попросить закрыть её и после закрытия закончить применение; до этого явно сообщать, что установка клиента ещё не завершена. Не копировать managed files вручную, не редактировать active-state и не ослаблять trust gate. Это правило не разрешает запуск клиента или gameplay QA.



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

### Campaign structure placement

12 Campaign boss structures and their native prerequisite structures keep the exact placement owned by their installed mods: structure-set type, spacing, separation, salt, biome/terrain rules and native exclusion zones. A global spacing or salt rewriter must not make the rest of the world sparse or move these structures away from their native candidates. Before changing this inventory, inspect the exact installed structure-set JSONs; campaign/prerequisite sets must not reuse a placement salt with each other, while independent native candidates may remain close. Generated JEM sites for bosses without a native structure remain outside this structure-set contract.


## Границы после архитектурного разделения

Events остаётся внутри JEMServer: его event records находятся в общем `jem_smp_v2`, а ordinary hosted arenas и party workflow используют существующих владельцев. Отдельный JAR потребовал бы переноса общего persistence и новых обязательных связей. `EventNetwork` сериализует feature packets и передаёт entry intent в SMP adapter; `EventAttempts` подготавливает detached state до изменения сохранённого event. Общие boss arena, revival и reward primitives остаются в World Boss Tiers.

Drill addon публикует фактический `DrillLifecycleEvent` для work/replacement/service. Achievement Guide слушает этот event только при наличии addon и владеет сохранёнными pack advancement IDs. Moving-only batch/final-service условия сохранены. Compat больше не регистрирует drill Mixins и не содержит копию drill lifecycle. JEI выбирает свой overlay при discovery плагина, а optional Curios policy находится за изолированным adapter.

Native menu primitives принадлежат private source своих потребителей; MenuSupport больше не является runtime/build dependency. Claims остаётся development-only и вместе с Flan отсутствует в intended shareable pack; старые world SavedData сохраняются. Settlement territories не являются приватами и никогда не дают land protection.

### Persistence и установка

Сохраняются `jem_smp_v2`, `jemcompat_main_campaign`, `jem_claim_zones`, `jem_world_boss_tiers`, `jem_hosted_boss_rewards`, прежние NBT keys и registry IDs. Drill NBT `JemDrillWear`, `JemDrillRepairs`, `JemDrillReplacementPos` и legacy `jemcompat` resource namespace не меняются. Перенос Java не создаёт новый world schema и не повторяет исторические victories/rewards.

SMP channel `jem_server:smp_v2` использует protocol `5`: typed action kinds/inputs заменяют action JSON, Update включает session/base/current revision, Query поддерживает bounded resync и owner-only home preview. Требуется одновременная установка matching Server JAR на клиент и сервер. Старый Compat необходимо заменить новым вместе с добавлением `jem_drill_lifecycle-1.0.0.jar`; две версии Compat/addon одновременно недопустимы. Claims/Flan не распространяются, Adaptive Culling — client-only, Villager Talking — development-only.

Production compile order follows actual source API edges: Identity → Economy; WorldBossTiers and DrillLifecycle precede TwelveEyes/Guide; Economy/Identity precede Races/Settlements; Guide/Identity/WorldBossTiers precede Compat; Server consumes the completed domain outputs. Module `build/libs` contains production outputs. No CurrencyCore, PackCore, MenuSupport or separate VisualSandbox artifact is built for this graph. Runtime/Client synchronization and native publication have their own explicit delivery evidence; stable profiles, MineRENT and baseline retain the authorized release boundary.

### Resource ownership

World Boss Tiers загружает standalone `defaults/bosses` первым, затем canonical `bosses` через native highest-resource selection; одинаковый logical ResourceLocation из собственных canonical profiles/user datapack заменяет default независимо от JAR order. World profile сохраняет выбранный qualifying roster и thresholds.

Blood Moon fallback defaults принадлежат native builtin `jem_server:event_defaults` с bottom priority. Canonical event tags/rewards из бывшего Pack Core теперь принадлежат Server; внешние datapacks сохраняют native precedence. Native structure associations относятся к WorldBossTiers, campaign data/assets — к TwelveEyes, guide content — к AchievementGuide, остальные migrated compatibility policies — к Compat. Никакое наличие PackCore mod ID больше не выбирает владельца.

| Совпадающий путь | Разрешённая семантика |
| --- | --- |
| `assets/jemcompat/lang/en_us.json` | Native language stack объединяет сохранённые ключи Compat, Drill Lifecycle и TwelveEyes |
| `assets/jemcompat/lang/ru_ru.json` | То же объединение с сохранением localization keys |
| `data/forge/loot_modifiers/global_loot_modifiers.json` | Native additive modifier registrations Server и World Boss Tiers |
| `data/jem_world_boss_tiers/tags/items/progression_excluded.json` | Standalone empty base и additive pack values, без replacement |

### Packet catalog

Все C2S identity decisions получают player из authenticated sender. Enum/UTF/count bounds проверяются в decoder, permissions и mutations — у use-case owner на server thread. `consumerMainThread` не получает второй `enqueueWork`; старый TierSync handler использует собственный main-thread dispatch. UTF limits ниже являются character limits; native UTF decoder отдельно проверяет encoded byte length до выделения строки.

| Owner/channel/version | ID и direction | Данные, пределы и use case | Recipients и cleanup |
| --- | --- | --- | --- |
| SMP `jem_server:smp_v2` / 5 | 0 C2S Query; 1 C2S Action; 2 S2C Update; 3 C2S Revive | Query filter64/search160/page0..10000; typed no-input/party/player/message inputs, total C2S4096 bytes; Update NBT1MiB с allocation accounting и session revisions; revive target UUID проверяет encounter owner | Selected viewer; subscriptions/replay state очищаются при logout/stop; close снимает view |
| Events `jem_server:events` / 10 | 0 S2C Prompt; 1 C2S Choice; 2 S2C Zones; 3 S2C Mob; 4 S2C Beam; 5 S2C Telegraph; 6 S2C AttackVfx; 7 C2S PromptRequest | Prompt occupants≤256, zones≤32, typed entry kind; PromptRequest contains only ARENA/BOSS enum and UUID, uses shared throttle and canonical proximity/progression checks; server-owned IDs/dimension/geometry/timing; finite attack span≤512; presentation не определяет damage | Event viewers/participants; client logout/world unload, server respawn/dimension invalidation и event lifecycle cleanup |
| Combat `jem_server:combat_v2` / 4 | 0 S2C Update; 1 C2S Claim; 2 C2S Vote | NBT accounting1MiB; result/event UUID; persisted reward owner и active participant vote validation | Combat participants; result/snapshot maps logout/stop, HUD logout/world unload |
| Navigation `jem_server:navigation` / 2 | 0 S2C Points | ≤64 points, subsystem16/label80, ID/dimension/position/expiry; authorization у party/shop owner | Requesting player; expiry, logout/stop, optional Xaero teardown |
| Landmarks `jem_server:landmarks` / 4 | 0 S2C bounded Snapshot/delta/reset; 1 C2S Remove | ≤4096 records/removals per frame, configurable syncBatchSize default128; name80/key160/category32/creator64; boss-only global, legacy ordinary creator-only, remove requires creator and excludes global boss | Bootstrap queue one frame/20 ticks/player; logout/offline/stop cleanup; persisted discoveries and personal legacy remain |
| Map `jem_server:map` / 8 | 0 S2C Snapshot; 1 C2S Request; 2 C2S Publish; 5 S2C Events; 6 C2S EventTeleport; 7 C2S BossTeleport; 8 C2S ProjectCreate; 9 C2S ProjectExtend; 10 C2S SettlementExpand | active event areas≤64/projects≤128; exact min/max rectangles; legacy publish name80 + dimension/position сохраняет только creator-private Landmarks; event/boss packets carry only server-owned UUID; boss ID resolves through discovered system Landmark + canonical registered arena + Campaign eligibility; request cadence20 ticks, mutations используют общий action throttle | Requesting map viewer; Blood Moon/Resource Rush area exists only while its event is active; hosted Boss Fight and Boss Raid share one canonical structure layer; project overlays contain only authorized owner/collaborator regions; per-player event cache offline/stop, client disconnect |
| Fishing `jem_server:fishing_odds` / 1 | 0 S2C ProfileSync | bounded JSON131072 chars, ≤1024 validated rod profiles; сервер владеет odds | Login recipient; existing FishingTooltips logout очищает client profile state |
| World Tier `jem_world_boss_tiers:main` / 6 | 0 S2C TierSync; 1 S2C AdmissionPrompt; 2 C2S AdmissionChoice | tier/count/next threshold + ≤1024 weapon IDs; expiring exact-boss/player UUID token and bounded 0/1/2 choice; Start revalidates authority/proximity/prerequisites | Connected players; standalone prompt only without Server; pending admission expires and clears at logout/stop |
| Campaign `jem_twelve_eyes:main` / 4 | 0 S2C OpenMap; 1 S2C GateWarning; 2 S2C Ending | position/dimension/marker key≤256/shared acknowledgement; two component JSON strings≤32767; empty ending intent | Target player; locator reports its coordinates and may invoke Xaero's native open-map control without a JEM screen, camera reflection or personal marker duplication; native singleplayer, warning cleanup and approved credits preserved |
| Villagers `jem_villager_talking:villager_speech` / 5 | 0 S2C Speech | line64/role32/name64, ordinal1..4 и bounded scene delay | Local scene viewers; feature HUD lifecycle; development-only distribution |

SMP сохраняет 64 replay receipts на игрока и отвергает тот же request UUID с другим typed payload. Action/revive/query work ограничивается cadence; query bursts coalesce до последнего intent. Повторные cooldown/replay replies не пересобирают view. Prizes projection постраничная; reward totals и выдача остаются у `PendingRewardContainer`, source filtering также выполняется там. Unexpected action exceptions логируются с player/request/kind/record context; ожидаемые failures имеют `SmpActionFailure.code()`.

Автоматические тесты и fixtures удалены по прямому запросу пользователя; Gradle test tasks отключены, JUnit отсутствует. Runtime test scenarios запрещены. Build/launch/analysis выполняются только при отдельном прямом запросе согласно AGENTS.md.

## Atlas and two-stage gameplay ownership

`Catalog/` is the reproducible static registry atlas for the canonical managed pack. Its manifest owns artifact identity and hashes; partitioned registry records and derived views are evidence, not a runtime registry substitute. Unknown dynamic IDs, stack/NBT-dependent behavior and unverified native APIs remain explicitly unresolved and fail closed where progression or economy is involved.

Combat-balance navigation lives in `Docs/power-system/` and the deterministic derived `Catalog/views/power/` partitions. The power layer explains native expression -> modifiers -> JEM changes -> effective scenario -> acquisition -> assessment, but never becomes a universal damage owner. Rebuild it with `python Catalog/generator/build_power_views.py` after relevant Atlas, installed-artifact evidence or combat-owner changes. A warning is review evidence only and must not auto-edit production balance.

Villager Workers is removed from current JEM distribution inputs and Settlements compatibility. Old workforce records remain inactive history; undelivered production obligations are quarantined and never materialized or sold. Ordinary villagers retain native professions; Recruits remains the optional combat NPC owner. Town territory is a persisted dimension-aware soft guidance boundary and grants no land permissions. Settlements owns its native Xaero extension independently of JEM Server. When Economy is absent or world-disabled, physical emeralds and the existing recoverable treasury back new Settlement obligations; enabled Economy selects the existing Coin owner. Pending obligations retain their captured currency and never silently convert. See Docs/settlements/standalone/README.md for currency, debt separation and production evidence.

TinyInv remains the native owner of expanded inventory/hotbar indexing, scrolling and rendering. JEM Professions only grants its synchronized hotbar attribute to an eligible level-20 Architect and provides validated direct-slot packets. BackToBed is no longer a distribution dependency; JEM Server owns `/home` validation against server identity, recent damage, active event boundaries, vanilla respawn state and shared safe-arrival rules.

## Xaero ownership and matching artifacts

Terrain owner — native Unified Xaero Worldmap 2.0.1 + WorldMap 1.46.0, with existing Minimap 26.4.2 and embedded XaeroLib 1.7.3. Optional `xaero.shared` adapters extend only native all-dimension reconciliation and full-cave acceptance; no map store/renderer is duplicated. Native addon's CC0-1.0 declaration is present in META-INF/mods.toml and author description. Exact official Forge artifacts are staged in `Client/ManagedPack` and local Server; active AutoModpack profile and bootstrap-only installer are untouched.

Boss discovery owner remains ServerData.Landmarks. Physical discovery and successful locator events are producers, Campaign saved caches are historic discovery evidence, and read-only RaidArenaApi known-position lookup supplies existing arena identity. Structure/arena registration and defeat state never become an implicit reveal. Details and privacy/recovery contract live in Docs/smp-gui/README.md.

Activating these source changes requires matching current production WorldBossTiers, TwelveEyes, Guide, Compat and Server artifacts where installed; no Pack Core artifact remains. Campaign and Landmark channel protocol4 require the updated client counterparts; installing only external map JARs cannot activate the new JEM adapters and discovery contract. Builds, gameplay launches, active-client installation, publishing and push are separate explicitly authorized actions.

Для сборки этого обновления compile dependencies берутся из актуального `Client/ManagedPack` через JEM_INSTANCE/jem_instance: bootstrap-only installer и активный AutoModpack profile не являются источниками новых версий. Это не разрешает прямую установку или синхронизацию active gameDir. TwelveEyes извлекает XaeroLib из выбранного WorldMap, Server использует именно этот matching extracted artifact.

Тестовые послабления принадлежат `JEMIdentity DedicatedTestProfile`, а не `-Djem.test.allowRaceReselect` или клиенту. Старый JVM-флаг больше не включает повторный выбор расы. Точный dedicated save/identity/epoch binding и восстановление администраторов описаны в разделе «Изолированный test profile и production migration» ниже; несоответствующий marker отклоняется, а отсутствие marker сохраняет обычную production-политику.

### Complete economy overhaul (2026-10-16)

The editable native-only merchandise and weighted profession pools live beside item_policies.json under JEMEconomy data. Prepared meals stay outside the universal Market, but exact specialist native offers have both Coin directions. Native goods/counts/NBT/restock remain owned by Minecraft. Economy owns funded settlement and special quotas; ordinary reviewed goods no longer have artificial daily quotas/stock. Same account ledger terms bind idempotent receipts and backed cash. Settlements owns seven-day per-contract payroll, personal employer or town treasury charges, persistent three-miss rebellion, and validated paid founding/expansion. Configuration is SettlementEconomyConfig; there is no parallel wallet.

KubeJS economy_recipe_guards.js applies the same approved mincer guard in integrated and dedicated worlds only when the owning world Economy recipe policy is enabled. Recipe callbacks query EconomyApi.recipePolicyEnabled(), which is bound to the actual RecipeManager and reconciles after the world SERVER config loads; an early missing overworld is not interpreted as a final off setting. Native recipes remain when Economy is absent/off. Full catalog reconciliation and exact evidence limits are in Docs/economy/FINAL_PRICE_LIST/.

The former MenuSupport skin-mesh revision is preserved as private native helper source in its consumers. Its prior embedded-library version-selection problem explains the old receipts; the current production graph contains no MenuSupport library or Jar-in-Jar requirement. Local development money requires -Djem.localDevelopmentMoney=true, an ignored Server/local/unlimited-wallet-account.txt containing the authorized account UUID, and explicit server-thread EconomySavedData.enableLocalDevelopmentWallet activation per runtime. No production command enables it. Persisted development issuance belongs only to this local world; never deploy that ledger. Before first activation preserve world/data/jem_economy.dat while cleanly stopped. Without both local opt-ins an issued ledger freezes instead of silently weakening fixed-supply conservation. Aircraft native refuel() feeds the onboard engine buffer during flight; do not restrict that lifecycle method to grounded aircraft.

Server Login1.0.3 rejects ServerboundClientCommandPacket before authentication, which traps a saved Health0 player on the death screen before they can type login. JEM Compat’s optional ServerLoginRespawnPacketMixin is physically dedicated-only and admits only PERFORM_RESPAWN through PacketPolicy. Vanilla still owns death/credits validation, replacement entity/connection binding and death penalties; the same AuthGate connection remains locked. REQUEST_STATS, movement, inventory, gameplay actions and all account writes retain their existing gate. This adapter adds no registry/channel/schema/input/resource contract: the original fix needed only the local Server Compat artifact and kept ManagedPack/test jem on publication32. That historical server-only delivery is recorded in Docs/races/login-respawn-recovery.json; the later shared GUI checkpoint also includes the same protected adapter.


## Personal settings and native preview correction — 2026-10-18

User requests settings persistence, Carry On removal and the supplied inventory/SMP
preview corrections. Bootstrap source revision jem.7 extends the pinned native
AutoModpack reconciliation for the explicit personal presentation preference set
in server.conf: Oculus, optional Iris, QuickSkin client preferences, Fzzy/Punchy
keybind files and root shaderpack option .txt files. Before the first transition
from server-owned to editable, live user bytes enter the native overlay. Later
server-default replacements retain those personal overlays. Other server files
retain native ownership/reset policy; options.txt remains excluded. This does not
claim to restore settings already lost before migration or authenticate skin
providers. Friends require the matching standalone bootstrap revision before
using this policy; gameplay updates cannot replace early bootstrap classes.

The generation36 application exposed a concrete build-provenance defect: the
installed artifact labelled jem.6 had no `personalPreference` method. Preparation
ran Git inside an ignored generated directory without its own repository; Git
discovered the outer JEM checkout and skipped the final git-format planner patch
section, while the earlier plain-unified repair sections did apply. A matching
patch receipt/manifest alone therefore did not prove patched class contents.

`prepare.py` now bounds Git repository discovery with
`GIT_CEILING_DIRECTORIES` and records/verifies actual prepared SHA256 for all11
patch-owned original/added Java files before cached reuse. Pristine upstream pins
remain separate. Revision jem.7 also preserves a personal overlay when live bytes
equal the previous shipped default; equality is no longer interpreted as consent
to replace a personal setting. Ordinary server-owned configurations keep native
reset and replacement policy. Final production bytecode inspection verified both
`personalPreference` and the personal guard before equality/replacement clearing;
Windows durable move and projection-head repairs remain in the same source patch.

Verified full jem.7 bootstrap SHA256:
`714becc9e5cff26629a271b6bda82de13e3e95d8da7b26e6bcd9f33fe0fd07bc`.
Wire version remains5.0.0-rc.1. Matching standalone bytes are synchronized to the
ordinary Server, canonical ManagedPack/installer and stopped test jem profile;
stable remains untouched. Bootstrap is excluded from native served-pack entries.

For the two observed generation36 resets, recovery used native history122
GAME_DIR entries only after their CAS SHA256 matched the saved pre-update hashes
and current live bytes matched the exact new canonical empty defaults. Native
`fileRestoreGate` had to return AVAILABLE. Under `ClientStorageMutation.run`,
recovery recorded LIVE124, used verified CAS `VerifiedFileTransfer.copyAtomic` to
the native overlay and game paths, cleared only those two native tombstones, and
recorded FILE_RESTORE125. `StateHistory.restoreFile` itself refuses overwriting a
different live file, so its verified primitives were used with that explicit
recovery authorization; no active pointer, trust check or gameplay JAR was changed.
All24 saved preference hashes match again; active token
`e4c80d6c3d0ffe2bbab93318e6e8035c3d2783ea` stayed unchanged and no transaction
remained. This is installation/recovery evidence, not a gameplay or skin-provider
acceptance claim. Future native applications must retain these exact preferences.

QuickSkin2.6.2.4 persistence has three native owners: `quickskin-client.json`
contains the active/own skin, cape and CPM hashes; `quickskin_preferences.json`
contains per-player last selections; optional `skin-preferences.json` contains
per-hash arm-model choices. `LocalAssetManager` discovers actual local PNG/GIF
sources under `quickskin/uploads/skins` and `quickskin/uploads/capes`, while
`quickskin_cache` stores derived data. Those personal asset/cache directories are
outside `packSyncIncluded` and must never be filled from maintainer assets.
Native join/respawn restores the active hash through discovered metadata; quit
clears GPU/network handles and the in-memory appearance repository, not uploaded
source files, and saves the native preferences.

The2026-10-18 source audit found dangling maintainer hashes and one per-player
record in the canonical shipped defaults with no corresponding shipped uploads.
Fresh installs therefore received nonempty hashes that native restore could not
resolve. Only `Client/ManagedPack/config/quickskin-client.json` selected hashes
and its `quickskin_preferences.json` player map are now empty defaults; other
provider/settings fields remain native and unchanged. Existing local selections
must survive through the jem.7 native editable-overlay migration and later
editable updates, not a raw copy. No test/stable skin files or personal uploads
were changed by that canonical-default sanitization. The observed test profile
has both saved skin hashes backed by real local PNGs. The later generation36
installation did reset its two preference files; the native recovery below
restored the exact original bytes without touching uploads or the stable profile.

`config/skin-preferences.json` is additionally eligible for native editable
ownership if ever distributed. It is currently absent from both canonical and
test configs, so it is not added to the pack, seeded into profiles or claimed as
the active-skin cause; no bootstrap change is needed for that absent unmanaged
file. Native provider/authentication/network behavior and missing-asset error
semantics remain unchanged. Other skin-provider complaints and actual
reconnect/restart/update acceptance remain unproven until observed.


Carry On2.1.2.7 and current client/server Carry On configs are removed. All17 saved
player CarryOnData types were INVALID before removal; the ordinary server was
stopped cleanly and the complete world was copied outside Git. Historical release
baselines and prior research remain history. MineRENT and stable profile unchanged.

The preserved native helper reads InventoryScreen's yBodyRot for GUI geometry rather than the
unrelated dispatcher yaw. Direct race/QuickSkin preview rotation remains explicit.
Compat, Server and production Races embed the matching helper and require1.1.1.
SMP loaded-player previews delegate skin/model/cape to the actual player, preserving
the installed provider; offline profiles retain SkinManager lookup. SMP race labels
use the existing .name localization key. Compat's client-only variedVillagerOutfits
option deterministically chooses seven native outfit types by villager UUID while
leaving saved biome type, professions, trades and breeding unchanged. Installed
Fresh Animations details already randomizes adult face textures without biome
conditions; JEM varies the native biome clothing layer, reusing pack resources.

Build/application evidence is separate from actual in-game acceptance. The full
master overhaul remains incomplete in Docs/plans/2026-10-18-gameplay-settlements-overhaul.md.


## Recruits debug-screen matrix ownership — 2026-10-18

The observed test jem crash at21:46:09 was Rendering screen / DebugInvScreen, with WATUT1.2.3 innerBlit calling PoseStack.last on an empty deque. Exact installed Recruits1.15.2 bytecode shows DebugInvScreen native labels method `m_280003_(GuiGraphics,II)V` calls `popPose` at offset2044 and never pushes a frame; its ScreenBase labels method is noop. AbstractContainerScreen owns the surrounding label transform. WATUT's real screen recapture exposes the earlier ownership violation; its capture and FancyMenu are retained.

JEMCompat's client-only `RecruitDebugScreenPoseMixin`, gated to exact Recruits1.15.2, adds the missing local push immediately before the unique native0.7 scale, retaining its existing trailing pop and using full runtime descriptors. It does not guard/catch empty stacks, add balancing frames around unrelated screens, change UI content, or change recruit inventory/gameplay. The live crash is evidence; production compilation/publication is distinct from fresh player acceptance, which remains in testing.

## Settlements living-condition ownership — 2026-10-19

`HousingConditionConfig` owns the proposed balance defaults in
`jem-settlements-living-conditions.toml`. `ResidentialRooms` owns bounded cached
reachable interior floor observations; `ResidentConditions` owns current occupancy,
BLOCK-light and visible-hostile observations. `ResidentMood` remains the single
happiness owner; native Recruits retains base morale, with derived condition penalties
applied only through Settlements snapshots. Native damage-to-fear does not also
permanently subtract mood. Bed privacy is a native sleep eligibility condition.
Matching production client/server Settlements protocol10 carries localized reasons
and entity-linked refusal. Both source profiles include the same complete GUI and
localization resources; only the explicit sandbox profile enables integrated gameplay.


## Local module distribution — 2026-10-20

The user explicitly requested removal from `test jem` and confirmed removal from local ManagedPack/server. `jem-professions-1.0.0.jar`, its otherwise unused mandatory `puffish_skills` dependency, the JEM professions datapack/config and its FTB guide chapter are removed from this distribution. Native AutoModpack owns client deletions. JEMServer hides the sidebar/profile module action and isolates optional API linkage. JEMProfessions source and historical world/player progression are retained as inactive history; ordinary villager professions are unaffected. Stable client, MineRENT and release baseline are unchanged. This later explicit request supersedes the earlier approved concept's Professions navigation label for this distribution.


## Windows tax save replacement — 2026-10-20

TaxDurability retains temp-write/force → one same-volume replace → full-NBT readback → clear-dirty ordering. Windows explicitly uses Minecraft-bundled JNAplatform5.12.1 `Kernel32.MoveFileEx(REPLACE_EXISTING | WRITE_THROUGH)` without COPY_ALLOWED; the ordinary Java atomic move ignores other options and failed to replace existing local data. Other systems retain ATOMIC_MOVE. Native failure or readback mismatch stays dirty and aborts before dependent money/ack mutation. No delete-then-move substitute or new library is used. Source references: https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-movefileexw and https://github.com/java-native-access/jna/blob/5.12.1/contrib/platform/src/com/sun/jna/platform/win32/Kernel32.java .

## Digger and native required aircraft resource pack

The local2026-10-20 delivery contract and evidence are in `Docs/prepared-updates/2026-10-20-digger-skarrier-aircraft/README.md` and `delivery.json`. Compat now owns the same native completion/removal/wear batch for Excavation, retains its existing enchantment/NBT toggle and native inventory synchronization. Paxi4.0 owns required aircraft resource-pack activation from `config/paxi/resourcepacks`; AutoModpack owns its installation. Do not add a second pack loader or edit personal options. Skarrier1.1.1 hotfix requires matching client/server binaries despite unchanged advertised channel version.

## Rejected crafted GUI rollback,2026-10-20

SMP/Settlements retain their previous screen family and native content; generated crafted textures are removed after actual runtime rejection. Market keeps its separate accepted renderer/resources. Settlements has no embedded housing isometry or Выбрать/x/? header controls, and each amount has one native currency renderer. The approved native helper player body-yaw correction remains in the private consumer source. Shipping Box is exposed through the owning Economy native functional creative-tab event; its existing recipe remains, and the current explicit owner-menu sale replaces the earlier daily processor. The market lifecycle only refreshes previews and recovers existing transactions. In the full pack, enabled Economy supplies native villagers, Settlements and Recruits with the same canonical Coins; archived standalone emerald screenshots do not justify a second ledger.


## Изолированный test profile и production migration —2026-10-20

Пользовательский handoff требует одну общую сборку с изолированными test/original bindings. Source owner профиля — `Mods/JEMIdentity/src/main/java/com/siirio/jemidentity/api/DedicatedTestProfile.java`. Только physical dedicated server может получить Binding. `config/jem/test-profile.properties` должен содержать profile=test, точный save=jem-test-2026-10-20/world, UUID saveIdentity, непустой ограниченный epoch и необязательный bypassQuotas. В самом save файл `jem-test-profile.properties` повторяет saveIdentity/epoch. Canonical server root, фактический world path и real path должны совпасть с этим точным каталогом; symlink escape/иной save/несогласованный epoch отклоняются. Binding кешируется на жизнь server instance; изменение marker не является live toggle. Отсутствие marker не даёт test privileges. Это не новая инструкция создать или переключить live профиль.

`JEMServer SmpRuntime` регистрирует TestProfileRuntime только на physical dedicated server. Перед допуском игроков runtime сохраняет исходные OP entries/levels в приватный `local/test-profile/operators-before.json` и восстанавливает их при startup recovery и остановке. Journal привязан к saveIdentity/epoch; его несовпадение требует явной миграции. При отключённом профиле найденный прежний journal сначала восстанавливает исходный admin set и архивируется. После canonical IdentityPolicy authentication runtime подготавливает test race choice и запрашивает finite test grant; только затем ранее не бывший администратором игрок получает временный OP2. Auth loss/logout отзывает временное повышение. Исходные администраторы сохраняют свои уровни. Ни OP, ни creative, ни клиентский пакет не заменяют auth.

Начальный creative задаётся один раз после auth/grant на saveIdentity/epoch/account, включая существующих игроков тестовой копии. Receipt `jem_test_initial_mode` хранится в штатном `PlayerPersisted` того же player NBT, что и режим; Forge сохраняет этот раздел при respawn. Последующие входы и смерти не сбрасывают выбранный игроком режим. В test overlay остаётся `force-gamemode=false`. Frozen ledger останавливает активацию тестового профиля, а не выдаёт OP без canonical grant.

Тестовая эмиссия принадлежит существующему `JEMEconomy EconomySavedData.ensureTestGrant`: один100000000Copper wallet (10000Arcane) на account и bound epoch; прежний личный wallet возвращается CENTRAL_RESERVE, новый target и testIssued изменяются вместе в том же owner. Повторный вход не восстанавливает потраченное. Сохранённый test ledger без matching binding не проходит conservation и блокируется. Новый epoch не применяется молча к существующей эмиссии. Optional bypassQuotas действует только при совпавшем binding и не отменяет auth, владение, fingerprints, coin provenance, idempotency или фиксированную дисковую квоту. Сам профиль не изолирует глобальную ServerLogin UserDatabase: точный auth-store swap/reset и server.properties выбираются и выполняются отдельным безопасным offline deployment по owning handoff, без публикации credentials.

RaceService/RaceSavedData остаются единственным owner выбранной расы; production policy — повторная смена через1Minecraft-день по persisted overworld gameTime. Test binding даёт повторную смену без этого ожидания. Для существующего test record один раз на saveIdentity:epoch выставляется ChoiceRequired, без удаления records или всех mechanics. Выбор и подтверждение проходят server owner; presentation не создаёт второй race save. Legacy records без ReselectReadyAt получают готовую первую смену, сохраняя прежнюю выбранную расу и progress. Подробности first-login UI — `Docs/races/README.md`.

Общий formatter `JEMIdentity MinecraftDays.format` предназначен только для presentation: положительный остаток округляется вверх до трёх десятичных знаков, меньший0.001Minecraft-дня получает отдельный порог,0 означает фактическую готовность. Engine/protocol/save поля сохраняют native единицы. Roles protocol4 и production Races skills protocol7 несут новый readiness/test/nonce snapshot; matching owning JARs нужны на обеих сторонах. Изменения экрана, renderer и packet shape не являются server-only релизом.

Production wallet migration существует только как owner API `preflightWalletRebase`/`rebaseWallets`, а не выполненная операция original. Caller подаёт проверенный набор established personal starter accounts, release ID и явный допустимый funding account. Preflight показывает количество/прежний и целевой wallet/funding/shortfall/physical liability; target2000Copper=20Gold на account. Успешная однократная SavedData transition записывает release receipts и конечные personal balances вместе с funding; повтор release пропускает уже обработанные accounts. Test/development emission, неверный thread/profile или недостаток средств запрещают перенос. Town/escrow/physical Coins и race records этим API не мигрируют. Original save,auth/admin bindings, актуальный backup и фактический вызов API остаются отдельным явно разрешённым этапом; перенос test playerdata/ledger запрещён.

Source implementation не доказывает готовность поставки: окончательные matching artifacts, обычные Server/ManagedPack sync, native AutoModpack publication/application, hosted profile startup и пользовательская приёмка отмечаются отдельной итоговой квитанцией. Здесь они остаются pending; original migration не выполнена. Ни tests, gameplay launch, MineRENT deployment, stable mutation, ни baseline promotion эта запись не утверждает.

## Original release — 2026-10-21

The explicit original-world command superseded the remaining test-world phase. [The release receipt](releases/2026-10-21-original-handoff/README.md) records the source-produced artifact set, original `world` startup, production test-privilege gate, removal of only the disposable test save, matching hosted/local native content, cumulative friend package and promoted baseline. The user explicitly waived backup integrity verification; fresh archive entries are retained without claiming verified readability. Original had no existing economy ledger, so its ordinary finite initialization replaces the earlier hypothetical existing-wallet rebase path. Original auth/player data were not copied from test. Runtime player acceptance remains separate.


## Economy/encounter source update —2026-10-21

Latest user instruction revokes the temporary MineRENT pause and authorizes fixing the boss-identity defect, the separately scoped isolated-copy checks and subsequent deployment. This records scope, not execution: the earlier uploaded archive was unapplied at the pause. Production repair/restart/JAR installation and release-baseline promotion require their actual completed receipt; no copy or prepared package is a deployment claim.

The current source contract and explicit final overrides are recorded in the owning economy, race, Settlements, SMP GUI, command and Boss Fight/Raid docs and the dated plan. Source remains in testing; this paragraph is not a build/install/deployment receipt. Only the parent release workflow may record fresh artifacts, native published/installed hashes and MineRENT delivery after those steps occur.

Economy owns pooled denomination backing, operation quantities, lazy proven-legacy normalization, partial-stack capacity, approved rational price anchors and a native Mending minimum8Gold. Starter20Gold and finite NPC funding remain unchanged. The only new production issuance authorization is happiness income to a town treasury, persisted once per town/day with an explicit supply counter. Shipping owns public exclusive visitor sessions, account-bound goods recovery, funded physical sale payout in payment slots and the money tab. Pending payment blocks ordinary close, new acquisition and all player breaking; forced-close seller recovery remains account-bound. Shipping blocks are nonflammable and explosion-proof. Buyer-only trading and the separate wallet UI are retired without removing ledger or shared native merchant owners.

WorldBossTiers/HostedLobby own dormant AI/invulnerability/presentation restoration on reload and canonical Start. An attempted boss hit opens the existing GUI; it does not start combat. HostedRaidScaling owns one preparation boost and its Start/retry restoration. The explicit 2026-10-24 restoration reuses the proven shared virtual Boss Fight/Campaign/Raid structure-volume wall, collision and teleport admission, while registered structure geometry continues to own identity, eligibility and safe placement. Blood Moon/Resource Rush barriers remain unchanged. Official Xaero marker travel remains an ID adapter to existing server arrival owners; no personal waypoint data/actions are shared or replaced.


## Canonical boss identity and operator quarantine —2026-10-21

The investigation and exact local/native evidence belong to `Docs/events/BOSS_FIGHT.md`; commands belong to `Docs/SERVER_COMMANDS.md`. WorldTierData persists canonical live UUIDs and only source-proven legacy aliases; registered structure identity, timers, participation and progression retain one owner. Spawn paths must wait for native saved-entity storage, not merely loaded terrain, and acknowledge the durable claim. An unknown/unloaded UUID is never absence proof. Explicit duplicate repair preserves complete NBT through existing SmpData and exact disk readback before an optional canonical CAS transfer and marked discard; it cannot award defeats or unlocks. No density or worldgen policy change is part of this fix. Source remains in testing until separate execution evidence and user acceptance.


## Starter supplies and pre-choice shield —2026-10-21

`JEMServer/smp/StarterWelcome` owns shared logical-server welcome admission and `StarterKitData` owns a once-per-verified-account item entitlement. The announced once-only policy and cooked-beef selection are Codex implementation assumptions; user request supplies the starter/protection goal. Kit delivery after auth is independent of race completion:64cooked beef,10iron ingots, four leather armor pieces, native `sophisticatedbackpacks:backpack` and `naturescompass:naturescompass`. Economy's existing20Gold funding/receipts remain unchanged; items do not grant another money receipt.

Delivery writes the pending account operation before touching inventory; complete native player inventory and its persisted account/operation receipt share one player save and exact readback. Remaining entitlement commits only after that receipt is durable, and pending operations reconcile against native player data after interruption. Full inventories retain remainder without drops. Death clone carries the receipt through Player.PERSISTED_NBT_TAG. Persistence failure disconnects rather than exposing a transient unacknowledged inventory; no silent missing-item substitution. This is source behavior, not a claim that delivery was exercised.

The incoming/outgoing guard reads IdentityPolicy and the optional canonical RaceApi: unresolved identity, or no effective race while Races is installed, means protected incoming attack/hurt/damage/death and denied owner-resolved outgoing damage/melee. Verification plus an effective race when that domain is installed ends protection immediately; absent Races does not create an indefinite guard; screen reopening is irrelevant. No second auth/race record or indefinite invulnerability flag is introduced. Source in testing; runtime/player acceptance and delivery receipts remain separate.


### 2026-10-21 current-world cumulative delivery

The latest user correction cancels the isolated-copy runtime phase and requires installing the cumulative release directly into existing MineRENT `world`, retaining a backup only for recovery. The completed [economy/boss release receipt](releases/2026-10-21-economy-bosses/README.md) records original-world startup, ENTRY8 publication, local generation53/native application, matching friend package and baseline promotion. Earlier pending/source-only statements describe their own historical point in the work; they are superseded for delivery by this receipt. Gameplay/player acceptance remains unrun/in testing.

Point-update inheritance uses the prior extracted package when available, or the exact SHA-256-pinned prior ZIP when its extracted folder has been removed. tools/jem.py reads ZIP payload metadata without recreating deleted folders; unsafe or duplicate payload paths fail closed. The cumulative payload and canonical guide still have to match the current clean source before baseline promotion.

## Next local update —2026-10-21, MineRENT held

The user explicitly keeps MineRENT, stable profiles and the published release baseline unchanged while accumulating this update in current source, ManagedPack and ordinary local Server. Current source refinements belong to the owning Boss Fight, SMP/race GUI, server-command and economy documents. World Tier membership now remains independent from hosted admission; native-tag support delegates to the canonical arena/live-boss owner. Custom player geometry applies only inside SmpScreen; native external/race previews are restored. That historical receipt used CurrencyCore1.0.2 for the rotten-flesh1/2Copper policy and unchanged leather3Copper. The universal source now owns those same policies inside Economy, with no CurrencyCore runtime dependency; this ownership change is distinct from that earlier delivery. Local artifact/publication/application evidence belongs to `Docs/releases/2026-10-21-next-update/receipt.json`; it is not a MineRENT release or gameplay acceptance claim.


## Building But Better hammer resource override — 2026-10-21

The editable Blockbench/Aseprite source and exporter are in
`Docs/art/bbb-hammer-2026-10-21`. The canonical managed output is
`Client/ManagedPack/config/paxi/resourcepacks/JEM Building Hammer.zip` and reuses
Paxi4.0 required-pack activation. Only `bbb:hammer` model and its new texture are
overridden; no BBB gameplay code, native merchant layout or personal options are
changed. Regenerate the ZIP with that folder's `build_resource_pack.py --output`
then stage/publish/apply through the ordinary native AutoModpack lifecycle.


## SMP/rewards/XP implementation — 2026-10-22

The user restored the amended architecture and authorized implementation. JEMRewards is the independent mandatory item-entitlement owner consumed by WorldBossTiers and Server; build Rewards before WorldBossTiers, Economy before Races, then Server. Ship matching new `jem_rewards:main` protocol1 on both sides; existing clients cannot satisfy the channel with data/config alone. Rewards persist exact item snapshots, issue receipts and checked claim journals in `jem_rewards.dat`; offline old boss/SMP deliveries migrate without SMP classes. Existing non-item advancement/recovery state remains WorldBossTiers-owned.

WorldBossTiers owns the shared logical-server x2 ordinary Minecraft XP baseline, including direct point awards and orb intake before Mending. A scoped transfer prevents a second baseline pass on the remainder; downstream XP modifiers remain active. Already-earned recovery and race/profession progress are separate. Economy owns `/market`, its enabled/auth guards and native market UI. SMP is dedicated-only; Raid preparation is provenance/lobby-only and combined strength freezes at Start. The independent native domain GUIs do not require SMP.

No automated tests, linters, gameplay client or disposable server were run for this amendment. Ordinary local artifact installation/publication and native closed-profile application follow the standing delivery rule; runtime player acceptance and first-race-screen visual confirmation remain separate. The user clarified that old singleplayer-world migration is not a focus.


## Engineering v2 implementation (2026-10-22)

The user requested implementation of the supplied Java/Forge ENGINEERING v2 archive, then explicitly reaffirmed that tests stay disabled. The archive is design input, not permission to enable tests, run analyzers, deploy MineRENT, or alter gameplay. Current implementation evidence and exceptions live in [engineering/ENGINEERING_V2.md](engineering/ENGINEERING_V2.md). Existing canonical General skills retain workflow ownership.

SMP persistence has explicit typed owners for parties, profiles, events, shops, legacy contracts, event schedules and boss quarantine. Read snapshots are immutable through nested state; command scopes bind typed working aggregates on the server thread, commit revisions/dirty state automatically and preserve explicit recovery flush checkpoints. Save codecs preserve existing identities, field presence/types and unknown data; native item/entity payloads stay isolated in adapters. Domain code uses named operations instead of mutable NBT records or a generic table mutation API. Operation-shaped requests dispatch named party/event/shop commands while the existing wire envelope remains a boundary concern. View construction is read-only: profile capture belongs to lifecycle commands and shop refresh is explicit before projection. One server session starts/stops with its server and owns one transient player session for requests, subscriptions and combat presentation; logout cleanup is independent of gameplay authorization.

`tools/module-inventory.json` owns production/development/retired/upstream-patch classification. Root Gradle settings, production artifact staging and shared-pack development exclusions read that inventory. Bootstrap remains its separate pinned upstream build. Retired modules are not Gradle domain projects. GuiSandbox, Professions, Talking and Claims remain excluded from production distributions.

Use the root Gradle 8.8 wrapper and fully qualified tasks, for example `gradlew.bat :JEMServer:jar`. Module settings redirect to the same root graph; do not use an unqualified module `jar` as a selective build. `productionJars` builds all production domain JARs. Java 17 and the successfully resolved ForgeGradle 6.0.54 are pinned. Internal compile-only dependencies consume the producer's `jemCompile` outgoing configuration, whose output is supplied by its `compileJava` task. Consumers never compile against reobfuscated release JARs or undeclared neighboring output directories. Xaero's nested compile library is extracted by an input/output-declared task, not by reading a producer build directory during configuration. External installed API dependencies still require the exact canonical pack or the explicit supported instance override; this is not a network-only fresh-checkout dependency distribution.

Build tasks produce artifacts only. Native local distribution remains a separate stage/preview/publish/application workflow under the standing project rules. E20 supplies opt-in non-test configuration in `gradle/java-quality.gradle`: `-PjemQuality=true` enables the pinned Spotless, Checkstyle, PMD, SpotBugs and CPD tasks and their `qualityCheck` aggregate. Ordinary builds neither resolve nor run these tools. `tools/architecture_metrics.py` derives its active source scope from `tools/module-inventory.json` and selected source profiles; historical scope requires explicit selection. Configuration does not authorize execution: no formatter, analyzer or metrics tool was run for this implementation. No test framework, fixtures, ArchUnit or quality CI is introduced. A successful JAR build does not imply runtime or player acceptance.

`productionJars` writes `build/production-artifacts.json` from Gradle's exact archive paths and hashes. `tools/architecture_artifacts.py` consumes this descriptor, so stale older JARs in a module's `build/libs` cannot become staging candidates. Selective domain builds remain supported without running the all-module staging workflow.

The optional typed Professions adapter requires the development owner's source at compile time, but does not ship Professions. Its native Pufferfish Skills 0.19.2 API input belongs in `.gradle/jem-development-libraries/puffish_skills-0.19.2-1.20-forge.jar` or the directory selected by `-PjemDevelopmentLibraries=...`. The local retained native artifact used here has SHA-256 `b5df0b2d3561486076bdfa9318cf143d05b3eab0ccb93a009b396eee1312ca8d`. This compile-only input must not be installed in production to satisfy a build dependency.

Settlement financial commands now terminate in Economy's `SettlementEconomyApi`: receipt replay/mutation and required durable acknowledgement are owned by Economy. Native temporary-write/force/replace/readback preserves pending state on failure. Settlements owns its own journal and translates results only. Resident membership is a derived, non-persisted town index. Corrupt/unknown rows retain their raw NBT in persistent quarantine; unsupported root format remains inaccessible and round-trips its original root. Quarantine is diagnostic retention, not automatic repair.

Races initializes migrations explicitly before mechanics and keeps typed results, scalar snapshots and the existing wire IDs. Selection scenes have a logical-server lifetime. Preview renderers share capture/restoration while keeping their own camera, clipping and entity pose. Prepared screen text invalidates with content, dimensions and language resources. No artwork or gameplay balance was redesigned.

## Food, companion homes and native time HUD —2026-10-23

User contract: `JEM_Food_PetShelters_SeasonalHUD_FINAL_v2.md`. Domain owners remain ordinary logical-server/client JEMCompat packages, available on integrated and dedicated hosts without JEM Events activation. [Food](food/README.md) owns bundle/diversity and the canonical Atlas assignment projection; [companions](companions/README.md) owns discovery, binding GUI, native restoration and schema; [season HUD](season-hud/README.md) owns only native synchronized presentation. No new runtime library is added. GlitchCore0.0.1.1 is an already-installed Serene Seasons dependency and is now also an exact compile-only dependency because the native seasons config inherits `glitchcore.config.Config`.

New wire contracts are `jemcompat:food`/1 (S2C dietary count) and `jemcompat:shelters`/1 (C2S nonce/revision-bound action and S2C owned-pet rows). They require matching fresh Compat on both sides; an older client cannot acquire a channel through a datapack. Food assignments have one owner under `Catalog/changes`; the build copies that file into the artifact, never a second manually maintained registry. Ordinary native effects, hunger, existing food recipes and all boss domains retain their owners.

The user subsequently took model creation into Gemini and rejected the first HUD candidates. [Visual handoff](art/food-pets-season-hud-2026-10-23/README.md) records the actual prompts and missing production resources. Source completion/packaging does not authorize installing missing block models or rejected HUD textures. Runtime synchronization/publication of this feature remains pending those assets; do not promote the immutable release baseline, mutate stable profiles or deploy MineRENT.

The subsequent user amendment explicitly expands compatibility to installed mountables, tameables and companions while excluding clock/season icons and models from this phase. Exact installed Alex and pack adapters replace the five-species/capability restriction, with verified native resurrection, ownership, inventory, summon lifetime and multipart seams. Existing Alex's Mobs, Alex's Caves, Citadel, Knightlib and Iron's Spellbooks are compile-only API inputs, with no new runtime libraries. Schema 2 migrates schema 1 and adds explicit rider-confirmed custody for native ownerless mounts. Synchronous native entity writes and flush finish before releasing a restored body to simulation, covering all separately saved multipart chunks. [Companion evidence](companions/README.md) and the actual [local Gemini review](companions/GEMINI_REVIEW.md) document inspected contracts and remaining acceptance limits

The 2026-10-23 user amendment explicitly excludes every `recruits:*` NPC from animal homes. JEMCompat rejects shelter discovery, binding and recovery for the namespace and removes its shelter-only native hooks. Old assignments no longer occupy homes, while any captured native payload remains archived. Native Recruits hiring, commands and death behavior are preserved


## Backpacked saved-content cutover - 2026-10-24

The user requires the next MineRENT update to preserve old bag contents. JEMServer owns a raw-NBT dedicated-world migration before Forge loads chunks or players. The exact preservation, native inventory format, barrel conversion, Rewards fallback and recovery contract live in [backpack/README.md](backpack/README.md#saved-content-migration-for-the-next-release). Keep a full pre-removal production backup before the first startup with Backpacked absent. Install matching release artifacts together, retain the world migration archive and permanent Rewards receipts, and require successful migration/import/startup before admission. A migration error blocks startup and must be resolved from its exact source evidence. Local completion never establishes MineRENT delivery or gameplay acceptance.

## Standalone Mob Home ownership - 2026-10-24

The user requested a standalone Mob Home mod with vanilla singleplayer support and optional installed-mod integrations. `Mods/MobHome` is the only shelter gameplay owner, with mod ID/resource/channel namespace `mobhome`. It owns the complete home GUI, EN/RU localization, recipes, native companion journal, recovery transaction and isolated optional Mixins. JEMCompat retains food/HUD and other compatibility domains, and no longer registers shelters. The legacy `jem_companions` journal filename/fields are retained as persistence format, not as a JEM dependency

Only Forge and Minecraft are mandatory runtime dependencies. Multiplayer requires the matching artifact on clients and server. The module has an independent Gradle settings/wrapper and explicit compile-only native API inputs. The user rejected the external models and requested direct replacement production and optimization. Editable models, layered textures and review evidence live in `Docs/art/mobhome-20261024`. `Mods/MobHome/tools/package_native_models.py` partitions native codec exports into static baked cell models and derives collision, without a block-entity renderer. Optional mods need not all be installed. Four previously audited false pet candidates were removed, all Recruits remain excluded

The earlier Argon Work handoff remains historical. The replacement `Mob_Home_1.0.0.zip` contains the standalone production JAR, editable native models/textures and source. The user explicitly selected source, JAR and ZIP completion without native publication on2026-10-24. Mob Home and the newly rebuilt JEMCompat are withheld from ordinary Server/ManagedPack activation in this completion step. Guide's earlier matching local JAR synchronization remains unpublished. Stable profiles, MineRENT and the immutable release baseline remain unchanged

# SMP GUI

## Starter entitlement and authentication/race protection —2026-10-21

Source implemented / **in testing**. No build, runtime delivery, protection check or deployment is claimed by this section.

The user requests starter supplies and protection while authentication or initial race selection is incomplete. Codex's announced interpretation is one kit per canonical verified account, including existing players; cooked beef is the selected food. These once-only/food choices are implementation assumptions, not separate user approval of every detail. The kit contains64cooked beef,10iron ingots, one leather helmet/chestplate/leggings/boots, one native Backpacked backpack and one Nature's Compass. Existing20Gold starter funding/receipts remain unchanged.

The kit becomes eligible after authentication and does not wait for race choice. Server-owned entitlement/journal state and native player inventory+operation receipt prevent reconnect/death/replay from granting another kit. Partial delivery keeps undelivered items in the durable journal when inventory is full; nothing drops on the ground or replaces worn equipment. A missing required native item or failed persistence is an explicit delivery failure, not a replacement item or falsely completed kit.

Protection is authoritative while IdentityPolicy cannot resolve the verified account or that account has no effective RaceApi selection. Incoming attack/hurt/damage/death is blocked; melee and damage whose resolved owner is the protected player are also blocked to prevent protected attacks. It ends immediately when both authentication and effective race selection exist. Merely reopening a race GUI cannot restore it. StarterWelcome consumes these owners without adding authentication or race-choice state.

## Functional source update — 2026-10-21

Source implementation / in testing. Runtime acceptance is unrun; production artifact delivery, native AutoModpack application and MineRENT deployment remain pending. This section does not claim a completed release.

The user explicitly retains the present SMP materials, icons, dimensions and navigation structure. No art-study redesign is integrated. The separate wallet/profile entry and Buyer-only routes are retired; CurrencyCore money remains available through Shipping's money tab. Death history opens its own UUID-bound choice of up to three personal records and safe return. Money summaries share nonzero Coin denominations. Settlements finances show happiness income instead of a Coin tax-rate selector.

Attacking a dormant registered boss opens the existing encounter choice/host GUI; the attempted hit does not start combat or damage the lobby boss. The server checks current identity, proximity, prerequisites, party and encounter state. Ordinary boss/Raid structures have no visual/collision/teleport barrier; Blood Moon and Resource Rush keep their own boundary contracts. Official server boss/event markers may expose native Xaero teleport actions with server ID resolution; personal waypoints keep their native behavior and privacy. Details belong to the boss event and command owners, not a second GUI gameplay model.

## Navigation

SMP различает server-hosted Events и player-hosted Party. Activities отсутствует полностью. На каждом screen ровно один Back; Back возвращает на предыдущий экран, а не в фиксированный root. Cursor, scroll, selected row и form draft сохраняются при server-owned действиях.

Player-hosted Party является социальной группой, а не отдельным gameplay event: она не переходит в `ACTIVE`, не имеет ложной кнопки Start и остаётся доступной до выхода или роспуска. Start/Ready существуют только у encounter Party. Accepted member может нажать `К группе`: social Party переносит к текущей безопасной позиции лидера, encounter lobby — к безопасной staging position. Владелец выбирает участника в roster и там же принимает заявку, удаляет игрока или передаёт leadership; эти действия не требуют ручного UUID.

Статичная левая панель открывает собственный профиль, игроков, группы, события, единственный системный **Рынок**, Поселения, награды, Роли и Профессии. Главная, Магазины и Рекруты в нее не входят. `settlements` является изменяемой server-authoritative проекцией выбранного поселения; обычная система claims/«Территорий» не возвращается. Пункт Профессий выполняет server-validated открытие единого радиального атласа JEM, который показывает каноническое состояние Pufferfish. Profile показывает full-body skin, но не recent achievements.

Пункты `Расы` и `Профессии` отправляют bounded SMP module packet, а не исполняют скрытую chat-команду. Расы открывают JEM-owned трёхколоночный выбор; Профессии открывают JEM radial presentation adapter поверх Pufferfish progression state. Удалённые `/jem role`, `/jemrole`, `/jem professions` и `/jemprof` не являются запасным маршрутом.

`/smp` по умолчанию открывает события; валидатор запросов принимает `settlements` как отдельный маршрут. Рынок остается постоянным пунктом левого sidebar и открывает server-authoritative JEM Economy storefront.

## Interaction ownership

- Client владеет layout, hover, scroll, cursor, skin/entity preview и visual accents.
- Server владеет доступностью actions, shop state, party membership, event state и mutation result.
- Client не предсказывает удаление slot item для menu buttons.
- Refresh обновляет содержимое in place и не открывает container заново.
- Player preview использует preview-only `RemotePlayer`; live entity не передаётся в vanilla inventory renderer.
- Xaero не встраивается в screen. UI показывает координаты и отдельную кнопку штатного открытия карты; сама native GuiMap получает полупрозрачную всегда видимую заливку с чётким контуром поселения и owner-only ЛКМ drag-preview с подтверждением точной цены через нативное контекстное меню.

## SMP domain projection

GUI не создаёт вторую модель SMP state: shops отражают physical Spud shop block, Market отражает JEM Economy policy/quotes, Settlements отображает и изменяет состояние только через его authoritative API, parties/events отражают authoritative server instance.

- Server-hosted Events и player-hosted Party визуально и текстово различаются; Activities, inbox storage и activity-feed queries отсутствуют полностью.
- Profile показывает full-body skin без recent achievements, reward actions, work contracts и UUID.
- Shops toolbar содержит только seller и purchasable-item search; seller даёт live suggestions. Trade card явно показывает input и output.
- Remote catalog открывает native Spud customer menu, если server chunk магазина загружен; seller может быть offline, а buyer client не обязан загружать chunk.
- Разрушенный или заменённый Spud BlockEntity немедленно удаляет catalog record; `CLOSED` и `STALE` записи не существуют.
- Spud trade сохраняет native management/stock protections и не зависит от удалённой системы обычных claims.
- Building Projects remain server metadata only and never grant or deny world interaction; JEM does not draw or edit them through Xaero's native screen.
- FTB Quests is a guide projection: it may explain Roles, Professions, Coins, towns, bosses and events, but it never becomes their state or reward owner.
- Shipping Box and Coin merchant screens are Economy projections: item policy is only `BUY_AND_SELL` or `NONE`, the server re-quotes every transaction, and the client never owns demand, quotas, offer use, inventory consumption or money mutation.
- Settlements route first selects a town only when the authenticated account owns more than one. Its current internal sections are Settlement, Residents, Happiness and Chests; treasury/development remain content within the settlement section. Residents, workers and recruits share one list with role/profession; Overview, Workforce and Defense tabs do not exist. Rename, treasury and unregister actions always carry the stable town UUID and are re-authorized by the server.
- The SMP sidebar adds Roles, Professions, Wallet and Town only when their modules are loaded. Professions opens one JEM-owned radial screen where all ten roots and all 78 balanced parent-child nodes remain visible together; wheel zoom and drag pan preserve readability while Pufferfish still owns category XP, levels, points and unlocked nodes. JEMProfessions validates gain signals, migrates legacy progress once and applies effects for actually unlocked skills. These buttons execute the real server-validated screen action and never print a dead command hint.
- Shop rows display a numeric Coin price. A remote non-selectable purchase reserves one Wallet/cash-funded amount, rechecks the loaded physical Spud stock, delivers the real stack and credits the authenticated owner. Selectable Spud shops require choosing the exact item at their physical block until a bounded selection DTO is available.
- Sleep vote длится девять секунд, показывает каждого eligible player и его выбор, sleepers считаются автоматически, большинство решает, tie сохраняет ночь.
- Глобальная карта Xaero содержит общее исследование terrain, подтверждённые открытия boss locations и полупрозрачные информационные области поселений с контуром. Обычные native waypoints, home/base, names/colors/groups/settings и death points остаются личными; редактор Xaero не публикует их на сервер.
- Старые обычные Landmarks/Pending записи сохраняются в прежнем SavedData, но доступны только creator, включая /landmarks и map snapshot. Creator удаляет только свою сохранённую запись. Global boss discoveries не удаляются через чужой UI; локальная visibility/delete настройка third-party marker принадлежит native Xaero и не меняет серверное открытие. Обычные claims и их контуры удалены. Event/project geometry остаётся в JEM-owned event and planning flows and is not injected into Xaero's screen.
- Event card показывает и карту, и server-authoritative Teleport. Resource Rush переносит внутрь области; обычный переход к combat event или discovered boss всегда высаживает снаружи outer collision surface с clearance полного player box. Телепорт асинхронно подготавливает только bounded target chunks, выбирает прочный сухой безопасный пол и отклоняется с понятным сообщением, если подтверждённой точки нет.
- Xaero's native right-click menu and waypoint actions stay intact. Starting inside an owned settlement, held LMB drafts a block-exact rectangular extension; release opens the native context menu with the five-denomination physical-Coin price. Confirmation sends the target block and an idempotency key, while the server recomputes area, overlap and payment. Event/boss travel remains available only through its JEM-owned cards and server-authoritative travel path.
- Во время активного combat canonical source/destination policy до перемещения блокирует portals, commands, TPA, pearls/fruit, Back to Bed, Xaero и modded travel items, если active participant или его mount должен оказаться снаружи. Лидер получает tokenized выбор завершить текущую attempt для всех, остальные — покинуть только свою попытку; Resource Rush не ограничивает вход и выход игроков.
- Blood Moon, Resource Rush и hosted boss boundaries показывают весь server-owned perimeter как непрерывные заполненные полупрозрачные стены от packet `minY` до `maxY`. Каждая сторона является rasterized quad с opacity 20–35%, без wireframe, стоек, рёбер и промежутков; `NO_CULL` делает плоскость видимой изнутри и снаружи, а world depth скрывает только перекрытые terrain части. После того как server решил, что event area находится в радиусе показа, client не отсекает отдельные части уже принятого perimeter повторно.
- Boundary wall использует отдельный buffered untextured `POSITION_COLOR` material: цвет и alpha принадлежат vertex data и не зависят от entity-emissive texture/light/normal sampling конкретного shader pack. Геометрия, full-height bounds, opacity, `NO_CULL`, depth test и исключение Oculus shadow pass остаются единым защищённым контрактом для vanilla и shader rendering.
- Боевой телеграф всегда включён и разделяет три визуальные роли: разреженная заливка показывает всю опасную область, двойной контур однозначно отделяет опасное от безопасного, а светлая движущаяся полоса показывает время и направление срабатывания. Arena 3 и Arena 5 используют один cyan visual contract; impact использует отдельный яркий фронт с коротким затухающим следом и не оставляет warning-заливку после начала атаки.
- Текстовые реакции жителей показываются в HUD три секунды и полностью переносятся выше открытого чата. Одиночная реакция выводит только реплику. Сцена `MULTI_SPEAKER_OR_SEQUENCE` сервером закрепляет каждую последовательную реплику за другим жителем, а HUD добавляет его custom name или локализованную профессию и номер автора внутри сцены, чтобы разговор двух–четырёх жителей нельзя было принять за бессвязный монолог одного.

## Shared exploration and boss discoveries

Native Unified Xaero Worldmap 2.0.1 владеет одним постоянным tile store на dimension/layer в `<world>/unified_xaero_worldmap`; JEM не создаёт вторую карту, renderer или map database. Требуются WorldMap 1.46.0 и Unified на обеих сторонах; Minimap 26.4.2 сохраняется, XaeroLib 1.7.3 встроен в WorldMap. Это dedicated-server feature; integrated singleplayer сохраняет native личные карты.

Surface, обычные height slices и full cave объединяются из native `region.xaero`. Изменения tile/hash передаются штатным bounded uploader; старые карты текущего Xaero 1.45 имеют совместимый format 6.8. При входе native reconciliation постепенно проходит все зарегистрированные измерения, объединяет существующие локальные карты без удаления уже принятого terrain и восстанавливает серверные регионы даже на пустом клиенте. Server reconciliation сам добавляет сохранённые cave layers; отсутствие локальной пещерной папки не означает отсутствия общего исследования. Дополнительное согласование ограничено одним заданием, использует штатный worker/interval и прекращается вместе с session.

Full-cave identity `Integer.MIN_VALUE` пропускается через native MapLayer/LayerPolicy; вид пещерной карты, выбранное измерение и личные настройки не переключаются. Карты до format 6, которые native Xaero ещё не переписал, не объявляются успешно импортированными. Native store не генерирует и не подгружает Minecraft chunks; spatial data принимаются только от реального клиентского картографирования.

Локальный server config устанавливает `persistIntervalSeconds=0` для записи принятого terrain через native I/O worker с file force/atomic rename. Остальные native пределы: 4 GiB, 50 000 region/layer, очередь 512, cache 128, upload 8/sec/player, payload 1 MiB, in-flight 8 MiB/player и catch-up 2 region/20 ticks. При достижении store quota новые регионы отклоняются с предупреждением; прежние остаются и обновляются, RAM eviction не удаляет историю. Это настраиваемое конечное хранилище, а не обещание неограниченного роста.

Boss location становится общей только после физического обнаружения в StructureStaging или успешного Campaign/Post-End locator. Само наличие WorldTierData arena, structure registry, prerequisite, defeat или scheduled rematch не публикует coordinates. На startup импортируются только существующие успешные locator caches, без повторного registry обхода при каждом login; новая находка публикуется даже если искатель отключился до окончания поиска. Canonical arena lookup выполняется только для уже найденной позиции и не регистрирует/разблокирует арену. При доказанном попадании в bounds marker использует существующий arena ID/anchor; ранее созданные locator projections объединяются с ним, а соседние самостоятельные арены не удаляются.

Boss discoveries остаются в прежнем `jem_server` SavedData с system creator identity. Bootstrap делится на ограниченные frame batches, дальнейшие additions/removals передаются как metadata deltas. Discovery replay не пересылает personal records другим игрокам и не вытесняет их квоту. Boss markers используют native ThirdPartyWaypointManager, origin `jem_server:boss_discoveries`, стабильный UUID, локализованное имя, красный цвет и ◆; собственные legacy records имеют отдельный `personal_landmarks`. Native override storage сохраняет локальные color/initials/visibility/delete preferences. Очистка удаляет только JEM origins и никогда не сопоставляет native точки по совпавшим именам или координатам.

Локатор на shared dedicated server сообщает координаты и может вызвать только штатный open-map control Xaero без второй личной метки, JEM-экрана, camera reflection или принудительного focus; singleplayer сохраняет прежнее native поведение. JEM не удаляет и не добавляет Xaero waypoint-menu actions and does not cancel native personal/home/base/death waypoint behavior. После полного удаления клиента гарантируется восстановление общего terrain и global bosses; удалённые обычные personal/death files не обещаются восстановленными. Старые явно сохранённые creator-scoped Landmarks остаются существующим частным server storage.

## Visual assets

`visual-library` хранит исходные GUI atlases, covers, menu assets и прежние reference packs. `ui-design` хранит утверждённый reference 2026-09-27. Frame corners имеют фиксированный размер; event accent не меняет базовую brown/copper/cream palette.

`visual-library/JEMLightDarkGUI` содержит одну производственную систему vanilla GUI для Java 1.20.1 и две согласованные темы: светлую `Sunlit Birch` с parchment/birch, walnut и copper конструкцией и тёмную `Moonlit Slate` со slate, aged brass и cyan акцентом. Обе темы сохраняют одинаковую geometry, atlas coordinates и semantic colors; тёмные containers оставляют светлые рабочие поверхности для читаемости vanilla dark labels без core-shader override. Готовые `JEM_GUI_Sunlit_Birch_1.20.1.zip` и `JEM_GUI_Moonlit_Slate_1.20.1.zip` принадлежат `Client/ManagedPack/resourcepacks`, а `build_light_dark_gui_packs.py` остаётся единственным owner их generated textures.

До создания нового изображения агент обязан проверить:

1. существующий atlas и icon mapping;
2. подходящий Minecraft/modded `ItemStack` и его model;
3. уже установленные textures, particles, sounds и GeckoLib assets;
4. лицензию внешнего model/texture;
5. нужен ли current tool escalation: Blockbench, concept PNG, licensed GLB или живой item/entity renderer.

Paper, dye и случайный 2D placeholder не заменяют семантическую иконку. General 3D asset допускается только вместе с source model, texture, pivots/bones, export path и renderer contract.

`GuiGraphics.enableScissor` всегда закрывается ровно одним `GuiGraphics.disableScissor`; raw RenderSystem call не заменяет pop GUI stack. Final Minecraft capture используется только по прямому запросу пользователя.

# Native Xaero preservation — approved direction, 2026-10-14

Xaero's native GuiMap, navigation, zoom, controls and personal waypoint appearance
remain the interface owner. JEM territory/project/event layers must be optional,
hideable and unobtrusive; they must not globally reskin or replace the map. Only
explored terrain and discovered boss locations are shared. Personal waypoint,
home/base, labels/colors/groups/settings and death points remain private. Existing
filled world-space event boundary geometry is unaffected. The separate JEM SMP GUI
remains in scope as its own feature, not an automatic rollback target.

JEM does not inject render layers, visibility controls, project editing, or teleport
rewrites into GuiMap or the waypoint menu. Shared terrain reconciliation and native
ThirdPartyWaypoint boss markers remain narrow data adapters; profile Home may open
the native map at an authorized private location. Server access checks, personal
points and world-space event walls retain their owners.
[Concrete integration design](../economy/native-trading-design.md).


## Market reference storefront —2026-10-17

The user-selected storefront uses the generated physical wood/iron/red-cloth artwork from `assets/market-2026-10-17`. Its live librarian preview follows the mouse and is not added to the world. One dropdown contains All goods, Blocks, Food, Plants, Resources and Drops; Plants queries the existing shared JEMCompat NATURE classifier over already admitted goods. The server returns12rows per page for the4×3grid. Buy/sell, exact Coin prices, stock, revision validation, search, paging and physical exchange retain the same authority. Hover Sell shows its actual Coin quote; unavailable actions are dimmed. The integrated title sign switches storefront/exchange; Back and Escape return to the parent.

Background anchors and source/export provenance: [asset contract](assets/market-2026-10-17/README.md). The whole canvas keeps its proportions; text/hit regions and preview scissor follow the same scale. No client gameplay launch or visual acceptance is claimed; real game capture remains pending.

GUI player previews always use shared vanilla classic/slim skin geometry and a neutral pose, bypassing live PlayerRenderer layers and animation/resource-pack model setup. JEM Compat scopes the native PlayerRenderer adapter to Screen rendering and handles Quick Skin’s exact preview entry point; Races uses the shared helper directly. World rendering retains native installed-mod behavior. Race selection alone owns GUI scale 3, including mouse coordinates, text clipping and miniature GL viewports; its camera rests at distance 5.0.


## Hosted fight chat links —2026-10-19

Both structural-arena and direct-boss «Открыть бой» notifications request the existing encounter entry screen via EventPromptRequest (ARENA/BOSS, UUID), not removed SMP subcommands. Screen component clicks consume the reserved action marker, including old boss/arena scrollback links, only on remote multiplayer. Dedicated authentication, shared request throttle and canonical prompt proximity/progression/availability are checked before presentation. Combat still requires the ordinary validated selection/start. Events protocol10 requires the same JEMServer JAR on client/server.


## Rejected crafted Settlements and SMP family — 2026-10-20

The user rejected the implemented `a0f1a26c` visual overhaul and explicitly requested a cosmetic rollback of Settlements and SMP. Earlier approval of generated concepts did not establish acceptance of the installed result. The preceding presentation is restored; generated materials, icons and event illustrations from this attempt must not remain active production assets.

The scope is presentation only: live settlement data, resident/house inspection, storage, finances, confirmations, server ACKs, SMP navigation and real event actions/state retain their existing owners. Professions stays removed from the local distribution. Later Digger behavior, Skarrier version and automatic aircraft resource-pack delivery are preserved. **The existing Market storefront and all of its assets, controls, prices and behavior are unchanged.** Native merchant, recruitment and Xaero interfaces retain their separate boundaries.

[Rejected art archive](../art/crafted-gui-2026-10-20/README.md) retains generated originals, editable sources, manifests and historical export instructions for provenance. Archive retention does not authorize shipping or re-exporting that artwork into the active source tree. This rollback is not approval of a replacement art direction; no new artist production is part of it.

The restored Settlements shell uses the exact earlier hand-authored `SettlementDashboardArt` and its 20 dashboard assets, not a new flat skin. The later requested corrections remove housing isometric rendering, controls, client state and requests; Home opens the existing `HomePos` coordinates dialog. The header Select control is removed; x/? were already absent. The heading reuses the freed width, and treasury summary/main balance use one existing `SettlementPriceRenderer.draw` call per amount instead of overlapping item and rich translated currency content. Currency selection, balances and payment authority are unchanged.

The parent delivery record owns exact built artifacts, native AutoModpack publication/application and any runtime limitations. No new gameplay launch or visual acceptance is claimed by these documentation changes.


## Professions local distribution removal — 2026-10-20

The user explicitly requested removal from `test jem` and confirmed removal from local ManagedPack/server. `jem-professions-1.0.0.jar`, its otherwise unused mandatory `puffish_skills` dependency, the JEM professions datapack/config and its FTB guide chapter are removed from this distribution. Native AutoModpack owns client deletions. JEMServer hides the sidebar/profile module action and isolates optional API linkage. JEMProfessions source and historical world/player progression are retained as inactive history; ordinary villager professions are unaffected. Stable client, MineRENT and release baseline are unchanged. This later explicit request supersedes the earlier approved concept's Professions navigation label for this distribution.

## Active party and GUI player repair - 2026-10-21

An ACTIVE boss party no longer carries closeLobby. Leaders can reopen its existing SMP detail view and confirm "End encounter" through the existing CANCEL action, revision validation, HostedBoundary action token and HostedParties.forfeit. Closed rows still close; a successful canonical START still closes once. Existing boss/raid cancellation, rewards and membership ownership are reused.

JEMMenuSupport1.1.2 adds vanilla HumanoidArmorLayer to the existing neutral GUI skin model with the same transform. Native armor hooks, materials, trim and glint remain authoritative; world/EpicFight rendering isolation is retained. Matching full Compat and Server embed the updated library. Actual in-game GUI and equipment acceptance remain unobserved.

## Player preview ownership —2026-10-21

User correction: custom player geometry is for the SMP GUI only. GuiPlayerRenderScope now activates only for SmpScreen; ordinary screens and inventories keep native renderers. The Quick Skin preview replacement is removed, and direct race-preview calls return to the native entity renderer. SMP controls, assets and server actions remain unchanged. Visual runtime acceptance is UNOBSERVED: no game was launched for this task and no new art was authored.

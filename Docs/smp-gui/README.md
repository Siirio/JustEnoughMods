# SMP GUI

## Navigation

SMP различает server-hosted Events и player-hosted Party. Activities отсутствует полностью. На каждом screen ровно один Back; Back возвращает на предыдущий экран, а не в фиксированный root. Cursor, scroll, selected row и form draft сохраняются при server-owned действиях.

Player-hosted Party является социальной группой, а не отдельным gameplay event: она не переходит в `ACTIVE`, не имеет ложной кнопки Start и остаётся доступной до выхода или роспуска. Start/Ready существуют только у encounter Party. Accepted member может нажать `К группе`: social Party переносит к текущей безопасной позиции лидера, encounter lobby — к безопасной staging position. Владелец выбирает участника в roster и там же принимает заявку, удаляет игрока или передаёт leadership; эти действия не требуют ручного UUID.

Sidebar использует emerald для Shops и Ender Chest для Prizes. Shops toolbar содержит ровно два подписанных поля: seller и purchasable item; seller имеет live suggestions. Profile показывает full-body skin, но не recent achievements. Territories используют compact rows, semantic icons и fixed navigation positions.

## Interaction ownership

- Client владеет layout, hover, scroll, cursor, skin/entity preview и visual accents.
- Server владеет доступностью actions, shop state, party membership, event state, claims и mutation result.
- Client не предсказывает удаление slot item для menu buttons.
- Refresh обновляет содержимое in place и не открывает container заново.
- Player preview использует preview-only `RemotePlayer`; live entity не передаётся в vanilla inventory renderer.
- Xaero не встраивается в screen. UI показывает location summary и отдельную кнопку открытия карты.

## SMP domain projection

GUI не создаёт вторую модель SMP state: shops отражают physical Spud shop block, territories отражают `JEMClaims`, parties/events отражают authoritative server instance.

- Server-hosted Events и player-hosted Party визуально и текстово различаются; Activities, inbox storage и activity-feed queries отсутствуют полностью.
- Profile показывает full-body skin без recent achievements, reward actions, work contracts и UUID.
- Shops toolbar содержит только seller и purchasable-item search; seller даёт live suggestions. Trade card явно показывает input и output.
- Remote catalog открывает native Spud customer menu, если server chunk магазина загружен; seller может быть offline, а buyer client не обязан загружать chunk.
- Разрушенный или заменённый Spud BlockEntity немедленно удаляет catalog record; `CLOSED` и `STALE` записи не существуют.
- Spud trade не ограничивается claim permissions и сохраняет native management/stock protections.
- Building Projects are authenticated Xaero overlays with stable colors and seam-free unions. They are planning metadata only and never grant or deny world interaction.
- FTB Quests is a guide projection: it may explain Roles, Professions, Coins, towns, bosses and events, but it never becomes their state or reward owner.
- Shipping Box and Coin merchant screens are Economy projections: item policy is only `BUY_AND_SELL` or `NONE`, the server re-quotes every transaction, and the client never owns demand, quotas, offer use, inventory consumption or money mutation.
- Town Hall projects one server-owned settlement snapshot through Overview, Residents, Supplies, Happiness, Workforce, Defense, Treasury, Projects, Journal and Guide tabs. It separates real storage from pending/quarantined evidence, formats treasury/payroll in the five Coin denominations, and exposes missing Workers/blueprint capability honestly; the client cannot nominate accounts, create stock, pay a roster or edit another mayor's town.
- The SMP sidebar adds Roles, Professions, Wallet and Town only when their modules are loaded. These buttons execute the real server-validated screen command and never print a dead command hint.
- Shop rows display a numeric Coin price. A remote non-selectable purchase reserves one Wallet/cash-funded amount, rechecks the loaded physical Spud stock, delivers the real stack and credits the authenticated owner. Selectable Spud shops require choosing the exact item at their physical block until a bounded selection DTO is available.
- Sleep vote длится девять секунд, показывает каждого eligible player и его выбор, sleepers считаются автоматически, большинство решает, tie сохраняет ночь.
- Глобальная карта Xaero содержит только общее исследование terrain и подтверждённые открытия boss locations. Обычные native waypoints, home/base, names/colors/groups/settings и death points остаются личными; редактор Xaero не публикует их на сервер.
- Старые обычные Landmarks/Pending записи сохраняются в прежнем SavedData, но доступны только creator, включая /landmarks и map snapshot. Creator удаляет только свою сохранённую запись. Global boss discoveries не удаляются через чужой UI; локальная visibility/delete настройка third-party marker принадлежит native Xaero и не меняет серверное открытие. Active Blood Moon и Resource Rush показывают exact server-owned область всем игрокам на Xaero и исчезают вместе с завершением event. Active hosted Boss Fight показывает canonical structure boundary; Boss Raid переиспользует ту же boss-область и не накладывает второй слой. Контуры обычных claims по-прежнему показывают только собственные территории, без глобальной выдачи чужих names/coordinates.
- Event card показывает и карту, и server-authoritative Teleport. Resource Rush переносит внутрь области; обычный переход к combat event или discovered boss всегда высаживает снаружи outer collision surface с clearance полного player box. Телепорт асинхронно подготавливает только bounded target chunks, выбирает прочный сухой безопасный пол и отклоняется с понятным сообщением, если подтверждённой точки нет.
- Правый клик внутри event area на Xaero World Map показывает `Телепортироваться к границе события`; карта всегда переносит за ближайшую внешнюю границу через тот же server-authoritative travel path, а не выполняет клиентский `/tp`. Сервер дополнительно проверяет конечную позицию любого телепорта: Boss/Raid destination внутри закрытой арены заменяется на сохранённый staging point или безопасную точку снаружи, а Blood Moon и Resource Rush без закрытого player border не используют это выталкивание.
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

Локатор на shared dedicated server открывает найденное место без создания второй личной метки; singleplayer сохраняет прежнее native поведение. На JEM dedicated server native Xaero teleport action удаляется у personal/home/base/death waypoint и появляется только у server-owned discovered boss marker; packet передаёт UUID, а server повторно разрешает canonical arena, progression и safe outside arrival без доверия client coordinates. После полного удаления клиента гарантируется восстановление общего terrain и global bosses; удалённые обычные personal/death files не обещаются восстановленными. Старые явно сохранённые creator-scoped Landmarks остаются существующим частным server storage.

## Territories

- Игрок видит только territories, а не внутренние rectangle pieces.
- Bounds точны до blocks, minimum area равна двум blocks.
- Overlapping selections одного owner объединяются с original territory; quota считает union area, outline показывает только внешний контур без внутренних seams.
- `Claim Tool` — только stick с точным именем `Claim Tool`; anvil preview показывает glint.
- Selection разрешается по server-observed player chunk tracking, а не по Xaero/global generation.
- Permission menu плоское: all on/off, containers, fire, PvP, explosions, outside penetration, placement, breaking и beds.
- Anvils, enchanting tables, doors, gates, trapdoors, buttons, levers и note blocks всегда доступны для interaction.
- Fire spread owner-configurable и выключен по умолчанию; boss terrain damage запрещён независимо.
- Через PvP-disabled boundary блокируется player→boss и boss→player damage, но AI и movement сохраняются.
- Boundary рисуется в world render stage как high-contrast exterior outline без filled walls, остаётся привязанной к мировым координатам при движении камеры и видна с shader pipeline; overlap preview имеет отдельный цвет.

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


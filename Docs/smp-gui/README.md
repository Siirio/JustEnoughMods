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
- Sleep vote длится девять секунд, показывает каждого eligible player и его выбор, sleepers считаются автоматически, большинство решает, tie сохраняет ночь.
- Shared waypoints публикуются без administrator approval.
- Shared waypoint и event area — разные server-owned сущности. Creator или operator удаляет shared waypoint прямо из Xaero UI, и удаление синхронизируется всем клиентам; event area показывается отдельным map overlay и не маскируется под удаляемую метку.
- Event card показывает и карту, и server-authoritative Teleport. Resource Rush переносит внутрь области; принятый combat participant после старта attempt — внутрь своей арены; outsider и staging player — к точке снаружи. Телепорт асинхронно подготавливает target chunk и выбирает поверхность с прочным полом и двумя свободными блоками, поэтому отсутствие заранее загруженной «safe zone» не отклоняет действие и не блокирует server tick.
- Правый клик внутри event area на Xaero World Map показывает `Телепортироваться к границе события`; карта всегда переносит за ближайшую внешнюю границу через тот же server-authoritative travel path, а не выполняет клиентский `/tp`. Сервер дополнительно проверяет конечную позицию любого телепорта: Boss/Raid destination внутри закрытой арены заменяется на сохранённый staging point или безопасную точку снаружи, а Blood Moon и Resource Rush без закрытого player border не используют это выталкивание.
- Во время активного combat обычные portals, commands и travel items разрешены, но выход участника из bounds немедленно возвращает его к последней безопасной точке. Лидер получает выбор завершить attempt для всех, остальные — покинуть только свою попытку; Resource Rush не ограничивает вход и выход.
- Blood Moon, Resource Rush и hosted boss boundaries показывают весь server-owned perimeter как непрерывные заполненные полупрозрачные стены от packet `minY` до `maxY`. Каждая сторона является rasterized quad с opacity 20–35%, без wireframe, стоек, рёбер и промежутков; `NO_CULL` делает плоскость видимой изнутри и снаружи, а world depth скрывает только перекрытые terrain части. После того как server решил, что event area находится в радиусе показа, client не отсекает отдельные части уже принятого perimeter повторно.
- Boundary wall использует отдельный buffered untextured `POSITION_COLOR` material: цвет и alpha принадлежат vertex data и не зависят от entity-emissive texture/light/normal sampling конкретного shader pack. Геометрия, full-height bounds, opacity, `NO_CULL`, depth test и исключение Oculus shadow pass остаются единым защищённым контрактом для vanilla и shader rendering.
- Боевой телеграф всегда включён и разделяет три визуальные роли: разреженная заливка показывает всю опасную область, двойной контур однозначно отделяет опасное от безопасного, а светлая движущаяся полоса показывает время и направление срабатывания. Arena 3 и Arena 5 используют один cyan visual contract; impact использует отдельный яркий фронт с коротким затухающим следом и не оставляет warning-заливку после начала атаки.
- Текстовые реакции жителей показываются в HUD три секунды и полностью переносятся выше открытого чата. Одиночная реакция выводит только реплику. Сцена `MULTI_SPEAKER_OR_SEQUENCE` сервером закрепляет каждую последовательную реплику за другим жителем, а HUD добавляет его custom name или локализованную профессию и номер автора внутри сцены, чтобы разговор двух–четырёх жителей нельзя было принять за бессвязный монолог одного.

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

`visual-library/JEMLightDarkGUI` содержит одну производственную систему vanilla GUI для Java 1.20.1 и две согласованные темы: светлую `Sunlit Birch` с parchment/birch, walnut и copper конструкцией и тёмную `Moonlit Slate` со slate, aged brass и cyan акцентом. Обе темы сохраняют одинаковую geometry, atlas coordinates и semantic colors; тёмные containers оставляют светлые рабочие поверхности для читаемости vanilla dark labels без core-shader override. Готовые `JEM_GUI_Sunlit_Birch_1.20.1.zip` и `JEM_GUI_Moonlit_Slate_1.20.1.zip` принадлежат `Client/JustEnoughMods/resourcepacks`, а `build_light_dark_gui_packs.py` остаётся единственным owner их generated textures.

До создания нового изображения агент обязан проверить:

1. существующий atlas и icon mapping;
2. подходящий Minecraft/modded `ItemStack` и его model;
3. уже установленные textures, particles, sounds и GeckoLib assets;
4. лицензию внешнего model/texture;
5. нужен ли current tool escalation: Blockbench, concept PNG, licensed GLB или живой item/entity renderer.

Paper, dye и случайный 2D placeholder не заменяют семантическую иконку. General 3D asset допускается только вместе с source model, texture, pivots/bones, export path и renderer contract.

`GuiGraphics.enableScissor` всегда закрывается ровно одним `GuiGraphics.disableScissor`; raw RenderSystem call не заменяет pop GUI stack. Final Minecraft capture используется только по прямому запросу пользователя.


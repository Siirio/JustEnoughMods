# Сборщик: весовая станция — арт-кандидат и референсы

**20.10.2026: модель repaint R2 одобрена пользователем.** Текущие wood/iron pixels, face-sized UV, editable Blockbench/Java export и native views — в [model/README.md](model/README.md), точный статус в `model/art-revision.json`. Использовать эти текущие файлы вместо прежнего compressed atlas/UV. GUI остаётся отдельным кандидатом, runtime integration ещё не выполнена этой сессией.

20.10.2026. **SOURCE_EDITABLE / ART_CANDIDATE / NOT_INSTALLED.** Показанные пиксели не означают принятие пользователем и не являются записью работающего интерфейса Minecraft.

## Направление пользователя и исправления

- Первые два пользовательских скриншота показывают нынешние ящик и тесный GUI, которые требуется переделать.
- Сначала выбрана посылочная тема, затем пользователь её отверг: «не почтовый ящик тогда».
- Вместо неё выбран ящик с товарами и весами сверху. Выбор направления не означает одобрение конкретного generated render.
- Пользователь отдельно запретил список каждого предмета в правой панели. Содержимое видно только в товарных слотах; справа общий итог, лимиты, кнопка и короткий статус.
- Поздняя обязательная коррекция: UI, новые item icons и текстуры с самого начала рисовать в Aseprite, в конечном PNG-формате/размере для Minecraft. AI разрешён только для референсов. После этой коррекции AI generation не использовался.
- Дополнение про Settlements: исправить layout/overlap, подготовить общие слоты и кнопки с настоящими native controls. Внешние Aseprite-кандидаты Settlements теперь находятся в [галерее37 страниц](../settlements-pages-2026-10-20/index.html); runtime интеграция остаётся в плане.

## Источники и классификация

| Файл / источник | Роль |
| --- | --- |
| `reference/current-block.png`, `reference/current-ui.png` | исходные пользовательские скриншоты; требуется переделка |
| `reference/ai-weighing-block-reference.png` | AI reference, направление весовой станции; конкретная геометрия/пиксели не утверждены и не используются как текстура |
| `reference/ai-item-list-ui-rejected.png` | AI UI reference с отвергнутой правой построчной ведомостью; не production source |
| `reference/ai-compact-ui-reference.png` | AI reference после удаления списка; только подсказка композиции, не точная геометрия слотов/цифр и не игровой арт |
| [SDM Shipping Box](https://modrinth.com/mod/sdmbox) | осмотренный внешний аналог: узнаваемый единичный storage block, ясное разделение дерева/металла; не пользовательский taste выбор и не источник копируемых пикселей |
| [Ванильная бочка, изображение](https://staticc.sportskeeda.com/editor/2021/09/f585f-16305227012309-800.jpg) | ванильный масштаб, крупные швы и простой материал; внешний ориентир, не источник PNG |

Почтовые AI-варианты остаются в исходной истории генерации как USER_REJECTED/obsolete; ни один не был установлен. Ни `.mjs`, ни native paint loop не читают AI references.

## Фактически нарисовано в Aseprite

| Артефакт | Целевой размер | Состояние |
| --- | --- | --- |
| `native/shipping-ui.aseprite` | 368×272 | 4 редактируемых слоя: материалы, native slots, demo labels, demo native items |
| `native/shipping-ui.png` | 368×272 PNG | фон и слоты, без baked text/цен/предметов; исходный размер, без ресайза |
| `native/shipping-ui-preview-1x.png` | 368×272 | отдельный демонстрационный вид; не игровая текстура |
| `shipping-ui-preview-4x.png` | 1472×1088 | только nearest-neighbour просмотр исходных пикселей |
| `native/collector-icon.aseprite`, `.png` | 16×16 | авторский значок весового сборщика |
| `native/controls.aseprite`, `.png` | 128×128 | native18 slot, payout slot и 103×22 button normal/hover/focus/pressed/disabled |
| `native/block-front/side/top.aseprite`, `.png` | 16×16 каждый | первичный материал ящика; не законченная модель весов |
| `native/block-face-study.aseprite` | 48×16 | три реальные native-texture faces рядом, без преобразования исходных PNG |

Форма UI имеет нижнее продолжение только под инвентарём: нижняя правая область прозрачна, а не занята пустой коричневой плитой. Товарная сетка точно 9×6, оплата 9×1, инвентарь 9×3 + хотбар9. Шаг18, внутренний предмет16; эти координаты определены в recipe, а не оценены по AI-изображению. Верхняя правая область не содержит списка товаров.

Показанные балансы — демонстрация номиналов: к выплате128, личный остаток2 400, мировой12 000. Это не live snapshot и не результат продаж; это исторические иллюстративные числа; новый план использует ordinary caps400/10000, ресурсы экрана должны читать серверный snapshot. Существующие Coin front textures взяты у CurrencyCore без изменения. Три ванильные иконки для preview прочитаны из установленного Minecraft client JAR, оригиналы лежат только в игнорируемом `Server/local/shipping-art-20261020/native-reference-items`; в будущем Screen рисует настоящие ItemStack, а не эти PNG. Демонстрационный bitmap text — отдельный скрываемый слой; игра использует собственный Font и локализацию.

## Как нарисованы пиксели

`paint.mjs` содержит явно выбранные формы, ряды, палитру материалов и координаты; готовит запросы редактору. Пиксели, слои, наложение существующих native icons и PNG сохранены самим **Aseprite 1.3.18.6-dev**, а не Pillow/браузером/AI. Исходные ресурсы рисуются сразу в своих размерах; увеличенные preview не возвращаются в ресурсы.

Прямой MCP `draw_grid` для полного GUI получил Windows `spawn ENAMETOOLONG`. `paint-native.lua` исполняет те же операции установленного pinned MCP engine через JSON-файл внутри Aseprite. Установленный upstream не менялся, его исходники не копируются в проект. Рендеры и открытие финального `.aseprite` получены непосредственно через Aseprite MCP; повторное открытие показало четыре живых слоя. Локальные evidence: `Server/local/shipping-art-20261020/aseprite-evidence`.

Повторная отрисовка (только арт, не Minecraft):

```powershell
node Docs/art/shipping-station-2026-10-20/paint.mjs
& 'C:/Users/user/.codex/tools/aseprite/build-1.3.18.6/bin/aseprite.exe' --batch --script-param 'requests=C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/shipping-station-2026-10-20/aseprite-operations.json' --script 'C:/MyProjects/Minecraft/JustEnoughMods/Docs/art/shipping-station-2026-10-20/paint-native.lua'
```

Для preview нужны оригинальные project Coins и указанные локальные vanilla icons; это input, не повод скачивать или подставлять новые картинки. Рецепт основной панели/нового значка/текстур не использует их в качестве исходной текстуры.

## Осмотр и оставшиеся этапы

Осмотрены реальные PNG-render Aseprite: убран поштучный список, исправлена длинная сумма, пересекавшая соседний номинал, удалена пустая правая нижняя плита. Сетка имеет ровно54+9+36 реальных позиций рисунка; слоты и demo текст больше не пересекаются в кандидате. Контролы и item icon показаны отдельно.

Это первый native-pixel кандидат, не финальное художественное принятие. Внешняя геометрия весов, UV и настоящие виды Blockbench теперь сохранены в [model/README.md](model/README.md). Game layout/hit/focus/scissor, native ItemRenderer и настоящий Screen ещё относятся к реализации. Код не интегрирован, JAR не собирался, клиент/сервер/миры не запускались и не изменялись. Внешний asset export не доказывает отсутствие runtime-overlap у Settlements.

Активные точные Taste Pairs: `Minecraft/GUI/Forge-1.20.1-jem-smp-screens` и `Minecraft/Textures (Resource packs)/Blockbench-textures/Minecraft-pixel-uv-and-tiling`. Общий authoring contract — `Taste-Engine/aseprite/MCP.md`; более поздние инструкции пользователя про Aseprite/PNG превосходят старые hybrid-generated указания.

## Коррекция выравнивания и вторая вкладка

[Открыть обе вкладки](index.html). Левая сетка и player inventory сохранены в прежних координатах и пикселях. Колонка расчёта расширена, значок весов помещён внутри header, монеты/числа стоят на общей baseline, разделители с ними не пересекаются. Правый низ остаётся прозрачным.

Пользователь переносит обмен монет из `/smp market` во вторую вкладку сборщика. `native/shipping-exchange.aseprite/.png` и `shipping-exchange-preview-1x.png` показывают размен и получение обеспеченных монет из личного кошелька в инвентарь. Это отдельный арт-кандидат/план, не уже работающий Screen. Валютный owner, точный курс и будущие проверки menu/hidden slots описаны в плане реализации; рыночная витрина сохраняется.

Для будущего block renderer пользователь требует настоящие native diamond/fish ItemStack displays, а в узкой части дубовые брёвна `minecraft:oak_log`, не доски. По3–5 отдельных объёмных экземпляров каждого вида выступают над бортом. Текущие16×16 face studies не являются этой законченной моделью. При реализации разместить нативные render anchors в модели, сохранить один BlockEntity/storage owner и не выдавать декоративные предметы за содержимое слотов. Не рисовать fake vanilla icons и не переносить AI-пиксели в atlas.

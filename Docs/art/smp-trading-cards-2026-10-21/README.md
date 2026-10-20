# SMP: выступающие карточки и новые иконки

21.10.2026. **SOURCE_EDITABLE / REVISED ART CANDIDATE / NEEDS_DIRECTION / NOT INSTALLED.**

## Новое указание пользователя

Из трёх предыдущих вариантов пользователь выбрал **№1** как лучшую основу и отдельно выделил идею выступающих карточек с цветом события. При этом **все прежние иконки событий/кнопок отвергнуты**; указаны грубая детализация и плоские разделительные линии. Запрошена более творческая художественная работа с качеством, приближающимся к reference pictures. Это не одобрение старых иконок, не разрешение обновлять сервер и не подтверждение качества новой версии.

## Что сделано

- Один новый composed-кандидат выбранного направления, **1024×640 native**, нарисованный заново в Aseprite. Это новая площадь для исходных пикселей, не растянутый512×320 PNG.
- Три разные оригинальные сцены **202×198**: зомби на фоне красной луны/леса; страж в древней кладке; кирка, руда и вагонетка в шахте. Сцены показывают визуальную тему события, а не фактического выбранного босса или real-time состояние сервера.
- Самостоятельные цветные оболочки карточек, ступенчатые углы/верхние выступы, отдельные поднятые плашки названий, загнутые боковые возвраты, нижние плоскости и фигурные кнопки. Служебные прямые разделители прежнего макета удалены.
- Девять заново нарисованных semantic иконок **36×36**, каждая со своим `.aseprite` и PNG: профиль, игроки, группа, события, рынок, поселения, награды, расы, профессии. Появились отдельные слои материальных цветовых кластеров и бликов. Native16px imports из прошлого макета не увеличивались.
- Шесть слоёв composed source: timber/iron, navigation seats, card shells/plaques, authored illustrations/icons, preview-only text, local refinements. Все labels/data остаются отдельным preview layer; `smp-cards-surface.png` экспортирован без него.

**Просмотр:** [весь экран](smp-cards-review.png), [иконки крупно](icons-sheet-3x.png), [сцена рейда крупно](art-boss-raid-3x.png).

**Редактирование:** [экран](smp-cards.aseprite), [иконки](icons-sheet.aseprite); отдельные `icon-*.aseprite`, `art-*.aseprite` лежат рядом.

## Оригинальные references, осмотренные в этом проходе

Базовый путь: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/`.

| Оригинал | Классификация пользователя / наблюдаемые качества |
| --- | --- |
| `GUI/Reference/I_like/Снимок экрана 2026-10-16 181140.png` | GUI+12 positive: крупный Warden почти заполняет верх, notch/цветная оболочка, вынесенная плашка имени и нижние перекрывающиеся элементы |
| `GUI/Reference/I_like/Снимок экрана 2026-10-16 181146.png` | GUI+13 positive: разные предметные сцены, доминирующий персонаж, ступенчатая рамка, имя поверх границы изображения/светлой информации |
| `Icons/I_like/Снимок экрана 2026-10-16 181225.png` | positive: инструменты с самостоятельными связными силуэтами, золотыми/деревянными/стальными плоскостями и небольшими яркими акцентами |
| `Icons/I_like/Снимок экрана 2026-10-16 181238.png` | positive: предметные food silhouettes, hue-shifted материальные кластеры и выделенный свет; набор не заимствуется как предметы меню |
| `Icons/I_like/Снимок экрана 2026-10-20 214954.png` | positive: направленные кластеры листа/дерева и чёткое распределение свет/полутон/тень, не случайная texture noise |
| `Icons/I_like/Снимок экрана 2026-10-20 215015.png` | positive: читаемая круглая красная форма, глубокие тёмные области и выборочные highlights; это не разрешение превращать всё в pumpkin symbols |
| `GUI/Reference/I_like/Снимок экрана 2026-10-16 181037.png` | positive: фигурные связные framing subjects с опорами/перекрытиями вместо простого разделительного штриха; ornate weapons не копируются в общий SMP |
| `GUI/Reference/I_like/Снимок экрана 2026-10-20 214725.png` | positive: предметная форма wooden/paper panels, сгибы и неровные edges; не источник копируемых PNG |

Все восемь оригиналов открыты отдельно. Попытка старого GUI-пути для181029 установила перенос icon references в `Icons/I_like`; отсутствие по историческому адресу не было объявлено отсутствием всей библиотеки. Ни один reference не вырезался, не трассировался и не использовался как runtime texture.

Текущие personal taste/technical owners: exact GUI Pair `GUI/Forge-1.20.1-jem-smp-screens/{TASTE,TECHNICAL}.md`, общие `ART_DIRECTION.md` и `aseprite/MCP.md`. Item-generation Pair также прочитан для различения GUI icon family и production inventory-item assets. Изменён только scoped GUI taste feedback; прежние другие экранные решения сохранены.

## Production provenance

Канонический внешний study: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/aseprite/Projects/JEM-1.20.1/Studies/Smp-trading-cards-2026-10-21`.

`paint.lua` задаёт оригинальные формы, связанные плоскости и целевые пиксельные кластеры. Rasterization, слои, bitmap-font preview и PNG exports выполняются **в Aseprite1.3.18.6-dev**. Скрипт — сохраняемый способ редактирования исходных художественных решений, не внешний PNG rasterizer. PNG1× не проходили image conversion, resampling или downsampling. Font inputs — существующие read-only Minecraft font providers из art input cache; готовые reference/icon sprites не импортируются. Для повторного запуска скрипта этот локальный font cache нужен; редактируемые `.aseprite` самостоятельны.

Aseprite MCP повторно открыл и отрисовал реальные native sources: экран1×, icon sheet3× и отдельную сцену3×. Увеличенные изображения — исключительно review previews; исходные consumer-native PNG сохранены отдельно. Архив здесь содержит точные финальные art/source copies без engine executable и внешних reference images.

## Строгий visual review и оставшаяся разница

Новое сообщение пользователя разрешило эту переработку после прежнего C2. Внутри новой переработки выполнены C0, C1 и C2; следующие правки автоматически не продолжаются.

| Кандидат | Наблюдение и source correction |
| --- | --- |
| C0 | Доминирующие сцены и цветные card shells появились, но слой сцены перекрывал поднятую плашку; описания были слишком мелкими. FAIL на композицию/читаемость |
| C1 | Плашки сдвинуты под завершение сцены; карточка удлинена; описания разбиты на две крупные строки, убран мелкий footer. Нарисованы face-local clusters на персонажах/руде. Проверен реально обновлённый native source, а не старый PNG |
| C2 | Иконки получили отдельные материальные кластеры/локальный свет, header — широкие осмысленные древесные формы, close control — связную металлическую форму. Composed PNG и icon sheet осмотрены через Aseprite MCP |

Наблюдаемые улучшения: материал рам отличается от информации, сцены доминируют, названия/кнопки не перекрываются, меню имеет самостоятельные semantic symbols, увеличение детализации происходит в исходном canvas, не фильтром.

**Reference-fidelity gap остаётся:** оригинальные карточки используют более богатое освещение и textured3D subjects. Новые нарисованные voxel scenes имеют более крупные спокойные плоскости и менее сложную фактуру; отдельные icon faces/маска также проще лучших icon references. Экспорт, больший canvas и две правки не доказывают запрошенное «almost unnoticeable from reference pictures». Поэтому это **NEEDS_DIRECTION**, а не reference-level PASS или утверждённый финальный стиль.

Основание остановки: canonical [visual-auditor](C:/Users/user/.codex/skills/General/visual-auditor/SKILL.md) требует «Stop after C2» и при оставшемся failed/unobserved hard gate — честный best candidate без установки/заявления готовности. C0/C1 source/export evidence остаётся во внешнем study. Дальнейшая художественная итерация требует нового направления/разрешения пользователя; отсутствие runtime evidence не заменяется новым game launch.

Runtime rendering, разные реальные GUI scales, hit/focus/hover/disabled states, реальная skin привязка и пользовательская приёмка — **UNOBSERVED**. Это не написанный Screen, не новая gameplay регистрация и не изменение размера настоящего runtime GUI.

## Граница изменений

Нет builds/tests/linters/game launches, source Java/config/runtime resources, Server/MineRENT изменений, AutoModpack publication/application, profile mutation или baseline promotion. `jem.py status` был вызван, но stableClient inventory снова завершился PermissionError; подтверждения синхронизации этим запуском нет. Игровая приёмка не заявляется.

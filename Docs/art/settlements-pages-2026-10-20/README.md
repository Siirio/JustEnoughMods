# Settlements — страницы и семантические иконки

20.10.2026. **SOURCE_EDITABLE / ART_CANDIDATE / NOT_INSTALLED.**

[Открыть галерею всех 37 страниц и состояний](index.html). Картинки — внешний кандидат с примерными данными, не запись Minecraft и не утверждённый пользователем интерфейс. В production resources они не установлены. Родственный [второй таб сборщика](../shipping-station-2026-10-20/native/shipping-exchange-preview-1x.png) хранится у владельца сборщика.

## Запрос пользователя

Все страницы Settlements, без кнопок «Все жители»/«По имени»; поиск остаётся. Различимые иконки счастья, сытости, жилья/кровати и других показателей рядом с числами. Без пересечений и плавающих элементов. Пользователь отверг скучный плоский черновик и потребовал смотреть GUI references перед рисованием. AI не используется для production pixels.

## Просмотренные оригиналы и извлечение

Exact Pair: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/` (`TASTE.md` и `TECHNICAL.md`). Положительные оригиналы открыты из `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Reference/I_like/`:

| ID / оригинал | Видимые качества, применённые в кандидате |
| --- | --- |
| GUI+05 — `Снимок экрана 2026-10-16 181050.png` | Тёмная кладка со швами и сколами, ограниченный мох у соединений, тканевый выбранный элемент, отчётливое разделение материалов. |
| GUI+06 — `Снимок экрана 2026-10-16 181055.png` | Доски с выступающими концами, широкие осмысленные полосы древесины, светлый верх/тёмный нижний край, вложенные поверхности. |
| GUI+01 — `Снимок экрана 2026-10-16 181018.png` | Читаемые маленькие предметные силуэты и контраст с тёмным обрамлением; молнии/механику дерева умений не переносили. |

Классификация принадлежит пользователю; перечисленное применение — интерпретация Codex. Эти референсы не означают одобрение текущей палитры или каждой формы. Модели, валютные формулы и защищённые native trading/Xaero экраны из них не выводятся. Глобальный reference-inspection lock находится в `C:/Users/user/.codex/AGENTS.logic-locks.md`, процедура принадлежит `taste-engine`.

## Покрытие

- 01–07: выбор/основание, обзор, жители, карточки обычного жителя, рекрута и голема.
- 08–11: счастье/факторы, жилищные жалобы и распределение, общие сундуки, содержимое контейнера.
- 12–16: координаты дома/защитника, подтверждения удаления поселения, исключения жителя и перевода.
- 14 глав справки: founding, territory, feeding, housing, food satisfaction, safety, jobs, community, decoration, chest delivery, payroll, rebellion, treasury, tax. Показан первый viewport; кнопки перелистывания содержимого остаются. Текст взят из текущего русского resource владельца.
- 6 состояний: loading, unsupported, pending, uncertain, empty residents, unloaded chest.
- 1 compact resident layout, нарисованный отдельно, без уменьшения большого PNG.

Это покрытие JEM-owned SettlementScreen и основания поселения. Нативные окна Recruits hire/Noble, Market и Xaero этим кандидатом не заменяются. Карточка дома показывает реально доступные координаты; изометрия всего города не возвращается. Workers отсутствуют.

## Исходники и экспорт

- Каждый `native/<page>.aseprite`: RGB, три слоя — материалы; controls/bounds; скрываемые preview data. Основной размер512×320; compact320×320.
- Каждый `native/<page>.png`: целевой фон без baked text, чисел и примерных предметов. `pages/<page>.png` — populated preview в том же размере.
- `native/icon-*.aseprite/.png`: оригинальные семантические16×16 иконки. Настоящие монеты CurrencyCore переиспользуются без перерисовки. Vanilla item/font pixels в preview взяты из установленной Minecraft1.20.1 read-only; новые glyphs не выдаются за настоящие ItemStack render.
- `paint-pages.mjs` задаёт композицию и генерирует спецификацию. `paint-pages.lua` выполняет пиксельный рисунок и native-font painting **в Aseprite**; внешнего PNG-растеризатора и AI-cutouts нет. Пиксели не рисуются в HD с последующим уменьшением.
- Входы Minecraft для повторного запуска находятся в ignored `Server/local/settlements-art-20261020/native/`; `fonts.json` описывает bitmap font providers/размеры/ascent. Понадобится тот же разрешённый локальный vanilla source. Production game не зависит от этой папки.
- `layout-manifest.json`: размеры/route/кандидатные controls. Это контракт будущей интеграции, не второй runtime layout owner. Настоящий Screen должен использовать единый layout result для draw/hit/focus/scissor.
- `contact-1.png`–`contact-5.png`: нативные contact boards только для просмотра, не игровые textures.

## Проверено и что остаётся

Aseprite сохранил native PNG и редактируемые источники; просмотрены contact boards и основные страницы/обмен/суммы отдельно. В показанных кандидатах цифры и монеты разведены по собственным bounds. Разрешённого игрового запуска не было: корректность live Screen, GUI scale, длинных реальных имён, hit-testing и отсутствие runtime overlap ещё не подтверждены.

В каноническом `SettlementScreen.java` подготовлено узкое удаление resident filter/sort state; автоматический порядок по имени и поиск сохранены. Оплата долга остаётся в roster при наличии долга и доступна в recruit detail тем же серверным запросом. Формулы/ledger, native professions и авторитет сервера неизменны. Source patch не собран и не установлен в этом плановом этапе; другие незавершённые изменения того же модуля не включались в артефакт. Server/ManagedPack/native publication/MineRENT не менялись.

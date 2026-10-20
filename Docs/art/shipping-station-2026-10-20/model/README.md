# Сборщик: внешний Blockbench-кандидат

20.10.2026. **Repaint R2 — USER_APPROVED_ART.** Пользователь: «вооооо - approved» после показа реальных native Blockbench видов. Одобрены текущие материалы/UV и показанный вид блока; в игру ещё не установлено. Это не принятие GUI, gameplay или runtime renderer.

## Текущая версия для другой сессии

Использовать **repaint-r2** из `art-revision.json` и текущие файлы рядом. Не восстанавливать rejected R1 atlas/UV из раннего commit или промежуточной схемы `geometry-spec.json`. Geometry/12 native goods anchors сохранены; изменены authored материалы, UV и editor evidence. `paint-atlas.lua` теперь рисует новый atlas64×64 по `native-uv-layout.json`, а не старые16×16 плитки. Native UV операции — `native-uv-operations.json`.

Пользователь отверг сжатую частую текстуру на тонких деталях. Цепочка причины: частая рябь → целая16×16 плитка на тонкой грани → одинаковый размер UV для разной геометрии → материал-study использован как готовая объектная развёртка → у внешнего asset owner отсутствовал контракт плотности по настоящим граням. Исправление:57 осмысленно разделяемых островов на282 грани; целевая плотность1texel/model unit с ceil/min1 для дробных узких граней. Доскам1.6–1.8u даны2 ряда, подвесам/тонким краям чаш —1 ряд/колонка; рисунок не сжимается из16 рядов.64×64 потребовались для размещения реальных островов и gutters, не для увеличения детализации на поверхности.

Дерево — тёплые крупные направленные участки; железо — тихие тёмные плоскости и редкие контактные светлые пиксели, без многополосного хрома. Ближайшие реальные references: ванильные barrel_side/anvil и Create6.0.8 andesite_casing/depot, плюс выбранная пользователем station reference. Оригинальные native textures читались только для просмотра; их пиксели не вставлены в authored atlas. [Прежний отклонённый вид](repaint-r2/before-three-quarter.png) сохранён для сравнения.

Повторная отрисовка только арта: `python layout-native-uv.py`, затем Aseprite batch с параметрами `layout=<абсолютный native-uv-layout.json>` и `output=<выходной каталог>` через `paint-atlas.lua`. Импорт PNG/UV — только structured Blockbench MCP; capture-native-views.mjs снимает уже открытый свежий preview. Никакого Minecraft launch или gameDir mutation эти действия не требуют. Geometric optimization/FPS не заявлены:47 кубоидов тела и12 native item displays остались прежними.

[Общий вид](view-three-quarter.png) · [спереди](view-front.png) · [сзади](view-back.png) · [сверху](view-top.png) · [настоящий Blockbench viewport](blockbench-viewport.png)

## Что просил пользователь

Весовая станция: открытый деревянный ящик, железные весы с двумя чашами и латунными соединениями. Почтовая тема отклонена. Товары — **настоящие Minecraft diamonds/fish и дубовые брёвна**, не oak planks. Последнее уточнение: они **выступают над краем отдельной объёмной кучкой по3–5 экземпляров**, с разным наклоном и высотой; не лежат сжатыми на дне. Нативный generated item имеет собственную тонкую экструзию, не непрозрачную прямоугольную карточку. Это не запрос рисовать новую рыбу/алмаз вместо native моделей.

Кандидат содержит4diamond,4fish (2cod+2salmon),4oak_log. Часть нижних брёвен естественно перекрывается верхними. Верхний борт8.8, верхние брёвна11.9 model units; товары видны выше борта. Они лежат в разных местах и частично перекрывают друг друга; не гарантируется одновременная видимость всех12 экземпляров с каждой стороны. Весы и чаши остаются выше и читаются отдельно. Модель имеет опорную стойку до пола и внутренний поддержанный товарный настил; все формы помещаются в одну стандартную16-unit область.

## Файлы и игровой контракт

| Файл | Назначение |
| --- | --- |
| `shipping-station.bbmodel` | Полностью editable тело:47 кубоидов, один внешний authored atlas, четыре группы материалов. Товары отдельно по anchors |
| `shipping_station_body.json` | Native Blockbench Java export с explicit legacy codec1.9.0, совместимым с target1.20.1; texture identifier приведён к `jem_economy:block/shipping_station_atlas`. Кандидат, не уже установленный resource |
| `shipping-station-atlas.aseprite` + `.png` |64×64;3 живых слоя рисунка + скрытый UV guide; painted/exported самим Aseprite в конечном размере, без HD-конвертации |
| `goods-anchors.json` |12 item anchors/poses/scales в model units,16 на блок. Для будущего client renderer, не runtime authority |
| `shipping-station-goods-preview.bbmodel` | Editable preview с native generated-item extrusion/границами alpha; чужие native PNG не встроены и не распространяются |
| `prepare-native-preview.py` | Из своего установленного Minecraft1.20.1 client JAR восстановить локальные native inputs и ignored preview; не модифицирует gameDir |
| `blockbench-viewport.png`, `view-*.png` | Настоящие native desktop/offscreen виды Blockbench; offscreen MSAA выключен для честного осмотра native пикселей. Без Minecraft launch и AI image generation |

После `python prepare-native-preview.py <путь к своему Minecraft1.20.1 client JAR>` открыть `shipping-station-goods-local.bbmodel`. Обычный `shipping-station.bbmodel` открывается без этого шага, поскольку использует только наш atlas. Native PNG остаются ignored references; не продавать/выдавать их за авторские текстуры JEM.

**В игре** тело — обычный Java model JSON и authored atlas. Декоративные товары — настоящий native `ItemRenderer` / `ItemStack` с authored poses, а не сотни preview кубоидов. Учитывать настоящий FIXED display transform модели/ресурспака при применении anchors; JSON-координаты — композиционный контракт, не обещание корректного размера без renderer integration. Сначала сопоставить один экземпляр с editor view, затем размножить. Не flatten/сжимать items в плоскость, не ставить billboard quads. Native resource-pack/model overrides сохраняются.

Декоративные товары не являются новым хранилищем, не выдаются при ломании, не копируют количество реальных слотов. Один `jem_economy:shipping_box`, существующий Owner/storage/journal сохраняются. Collision/outline и ориентация должны быть явно согласованы с actual geometry при реализации; static preview этого не доказывает. Нельзя установить один body export и назвать весь запрос выполненным: нужны renderer, UI, две вкладки и серверная транзакция из [плана](../../../plans/2026-10-20-shipping-economy-implementation.md).

## Происхождение и просмотр

Перед геометрией снова открыта реальная картинка `../reference/ai-weighing-block-reference.png` и исходный пользовательский attachment23b0a77d. Извлечены silhouette открытого ящика, верхняя балка, парные подвешенные чаши, тёмный металл/латунные узлы и выступающие товары. AI изображение — **только reference**, его пиксели/вырезки не попали в материалы.

Тело построено структурными desktop Blockbench MCP операциями (`author-model.mjs`/`geometry-spec.json` — авторская история blockout), далее headless MCP geometry/UV. Текущий `paint-atlas.lua` рисует3 слоя Aseprite прямо64×64 и отдельный hidden guide. Preview/native alpha читалась Aseprite (`read-native-alpha.lua`); `arrange-native-goods.py` хранит **историческую**, не текущую material/UV раскладку из локального blockout. Final editable files выше — источник дальнейшей правки. Native sprite UV16×16 и сами commodity pixels новым repaint не менялись.

Осмотрены настоящие textured multi-angle outputs и native viewport. Убраны прямоугольные боковые грани карточек, корректированы native UV, удалён plank display; исправлен материал стойки. Repaint R2 дополнительно устранил ошибочную плотность на тонких деталях; пользователь одобрил показанные native виды. Formal multi-silhouette calibration/grey-stage approval не проводились. **Одобрение арта не является проверкой в игре или доказательством отсутствия runtime overlap.** После renderer integration отдельно сопоставить размер нативных товаров/FIXED transforms и actual game view с этим утверждённым артом, не повторять художественное голосование по уже принятому R2.

Активные пары: Minecraft cuboid models + Minecraft pixel UV/tiling из Taste Engine; M-C связная конструкция/T-A painted materials. Blockbench не использовался для рисования текстуры. Minecraft, local Server и MineRENT не запускались/не менялись этим артом.

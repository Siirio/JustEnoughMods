# Сборщик: внешний Blockbench-кандидат

20.10.2026. **Модель/UV подготовлены; в игру не установлено, пользовательской визуальной приёмки ещё нет.**

[Общий вид](view-three-quarter.png) · [спереди](view-front.png) · [сзади](view-back.png) · [сверху](view-top.png) · [настоящий Blockbench viewport](blockbench-viewport.png)

## Что просил пользователь

Весовая станция: открытый деревянный ящик, железные весы с двумя чашами и латунными соединениями. Почтовая тема отклонена. Товары — **настоящие Minecraft diamonds/fish и дубовые брёвна**, не oak planks. Последнее уточнение: они **выступают над краем отдельной объёмной кучкой по3–5 экземпляров**, с разным наклоном и высотой; не лежат сжатыми на дне. Нативный generated item имеет собственную тонкую экструзию, не непрозрачную прямоугольную карточку. Это не запрос рисовать новую рыбу/алмаз вместо native моделей.

Кандидат содержит4diamond,4fish (2cod+2salmon),4oak_log. Часть нижних брёвен естественно перекрывается верхними. Верхний борт8.8, верхние брёвна11.9 model units; товары видны выше борта. Они лежат в разных местах и частично перекрывают друг друга; не гарантируется одновременная видимость всех12 экземпляров с каждой стороны. Весы и чаши остаются выше и читаются отдельно. Модель имеет опорную стойку до пола и внутренний поддержанный товарный настил; все формы помещаются в одну стандартную16-unit область.

## Файлы и игровой контракт

| Файл | Назначение |
| --- | --- |
| `shipping-station.bbmodel` | Полностью editable тело:47 кубоидов, один внешний authored atlas, четыре группы материалов. Товары отдельно по anchors |
| `shipping_station_body.json` | Native Blockbench Java export с explicit legacy codec1.9.0, совместимым с target1.20.1; texture identifier приведён к `jem_economy:block/shipping_station_atlas`. Кандидат, не уже установленный resource |
| `shipping-station-atlas.aseprite` + `.png` |3 живых слоя,64×32; пиксели изначально painted/exported самим Aseprite, без HD-конвертации |
| `goods-anchors.json` |12 item anchors/poses/scales в model units,16 на блок. Для будущего client renderer, не runtime authority |
| `shipping-station-goods-preview.bbmodel` | Editable preview с native generated-item extrusion/границами alpha; чужие native PNG не встроены и не распространяются |
| `prepare-native-preview.py` | Из своего установленного Minecraft1.20.1 client JAR восстановить локальные native inputs и ignored preview; не модифицирует gameDir |
| `blockbench-viewport.png`, `view-*.png` | Настоящие editor/headless виды модели, без Minecraft launch; не AI image generation |

После `python prepare-native-preview.py <путь к своему Minecraft1.20.1 client JAR>` открыть `shipping-station-goods-local.bbmodel`. Обычный `shipping-station.bbmodel` открывается без этого шага, поскольку использует только наш atlas. Native PNG остаются ignored references; не продавать/выдавать их за авторские текстуры JEM.

**В игре** тело — обычный Java model JSON и authored atlas. Декоративные товары — настоящий native `ItemRenderer` / `ItemStack` с authored poses, а не сотни preview кубоидов. Учитывать настоящий FIXED display transform модели/ресурспака при применении anchors; JSON-координаты — композиционный контракт, не обещание корректного размера без renderer integration. Сначала сопоставить один экземпляр с editor view, затем размножить. Не flatten/сжимать items в плоскость, не ставить billboard quads. Native resource-pack/model overrides сохраняются.

Декоративные товары не являются новым хранилищем, не выдаются при ломании, не копируют количество реальных слотов. Один `jem_economy:shipping_box`, существующий Owner/storage/journal сохраняются. Collision/outline и ориентация должны быть явно согласованы с actual geometry при реализации; static preview этого не доказывает. Нельзя установить один body export и назвать весь запрос выполненным: нужны renderer, UI, две вкладки и серверная транзакция из [плана](../../../plans/2026-10-20-shipping-economy-implementation.md).

## Происхождение и просмотр

Перед геометрией снова открыта реальная картинка `../reference/ai-weighing-block-reference.png` и исходный пользовательский attachment23b0a77d. Извлечены silhouette открытого ящика, верхняя балка, парные подвешенные чаши, тёмный металл/латунные узлы и выступающие товары. AI изображение — **только reference**, его пиксели/вырезки не попали в материалы.

Тело построено структурными desktop Blockbench MCP операциями (`author-model.mjs`/`geometry-spec.json` — авторская история blockout), далее headless MCP geometry/UV. `paint-atlas.lua` рисует3 слоя Aseprite прямо64×32. Preview/native alpha читалась Aseprite (`read-native-alpha.lua`); `arrange-native-goods.py` хранит промежуточную structured-operation раскладкуr2 из локального blockout, final editable files выше являются источником для дальнейшей правки. Поздняяr3 коррекция привела UV native sprites к их фактическим16×16; исходные commodity pixels не рисовались заново.

Осмотрены настоящие textured multi-angle outputs и native viewport. Убраны прямоугольные боковые грани карточек, корректированы native UV, удалён plank display; исправлен материал стойки. Кандидат сравнивался с reference по silhouette/чашам/опоре/товарам. Formal multi-silhouette calibration/grey-stage approval не проводились; выбранная пользователем станция сразу стала одним кандидатом. Это **не USER_APPROVED, не проверка в игре и не доказательство отсутствия runtime overlap**. Сильнее всего ещё требует живой оценки размер нативных товаров после FIXED transforms и плотность рисунка на тонких досках/металле. Не скрывать это за успешным export.

Активные пары: Minecraft cuboid models + Minecraft pixel UV/tiling из Taste Engine; M-C связная конструкция/T-A painted materials. Blockbench не использовался для рисования текстуры. Minecraft, local Server и MineRENT не запускались/не менялись этим артом.

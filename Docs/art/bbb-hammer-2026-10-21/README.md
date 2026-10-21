# Building But Better — объёмный молоток

Пользователь запросил заменить плоский молоток на 3D-модель через ресурс-пак. Это локальное следующее обновление, без MineRENT, stable-профиля и изменения release baseline. Исходный предмет `bbb:hammer` остаётся предметом Building But Better2.0pre4: рецепт, использование, теги и взаимодействия не меняются.

## Контракт и оригинальные references

Consumer: Minecraft Java1.20.1, Blockbench Java Block/Item codec1.9.0. Семь кубоидов, область12×15×5 model units, один32×32 PNG, примерно один пиксель на model unit; маленькие крепления имеют отдельные маленькие UV-острова. Инвентарь — обычный16px item; рука имеет самостоятельные native display transforms. Это настоящий объём, а не утолщение силуэта исходного спрайта.

Просмотрены настоящие оригиналы:

- Installed `bbb-forge-2.0pre4.jar:assets/bbb/textures/item/hammer.png` — медная головка и тёмная деревянная рукоять. Его16×16 пиксели не копировались в новый atlas.
- `Taste-Engine/Pairs/Minecraft/Models/Reference/REF10_character_and_tools.png` — понятная функциональная масса инструмента, реальная толщина и fitted handle contact.
- `.../REF06_cyan_gold_equipment.png` — различимые плоскости металла, тёмный хват и небольшие конструктивные крепления; голубой цвет, эмблемы и fantasy motifs не переносились.
- Два пользовательских скриншота — context существующего интерфейса, не источник production pixels. Релевантных отрицательных оригиналов именно строительного молотка в активной коллекции не найдено; прежние отказы от несвязанных кубов и слабой толщины сохранены как ограничения формы.

Exact pairs: `Minecraft/Models/Blockbench-models/Minecraft-cuboid-entity-models` и `Minecraft/Textures (Resource packs)/Blockbench-textures/Minecraft-pixel-uv-and-tiling`. Native installed hammer/recipe — authoritative subject evidence; upstream repository inspected at https://github.com/starfish-studios/Building-But-Better .

Art direction: тяжёлая горизонтальная медная головка, две чуть расширенные ударные грани, центральная обойма с опирающейся на неё рукоятью, небольшой нижний наконечник. Светлые верхние плоскости, тёплая основная медь и тёмные нижние плоскости; рукоять темнее металла. Ортографические виды для формы, native display cameras для фактического item placement. Альтернативы узкой компактной головки и декоративного раздвоенного бойка не выбраны: простой широкий двухсторонний строительный молоток ближе к исходному предмету. Дополнительно просмотрены три настоящих силуэта: компактная8-unit головка (`study-compact.png`), выбранная12-unit (`black-front.png`) и широкая14-unit (`study-wide.png`). Компактная теряет тяжесть, широкая выглядит кувалдой; выбранная сохраняет строительный силуэт. Это позднее сравнительное ревью уже сохранённой C2, а не выполненный до painting этап; отдельные сравнительные исходники сохранены и не меняют финальную C2 или correction counter.

## Native источники и экспорт

- `hammer-final.bbmodel` — editable master финальной C2 с embedded новым atlas и native display transforms. Исходная геометрия создана live Blockbench MCP, UV/файловые операции выполнены headless MCP, display transforms сохранены live editor.
- `hammer.aseprite` —32×32, три живых слоя: пустой исходный, Copper faces and hardwood, Striking wear and grain accents. PNG экспортирован напрямую Aseprite, без генерации изображения, ресайза, cutouts или painting внутри Blockbench.
- `hammer.png` — прямой native-size export. Разные front/back/top/underside islands, отдельные ударные грани и четыре стороны деревянного хвата.
- `hammer-final.json` — actual native Java codec export с texture binding `bbb:item/hammer_3d`, particle texture и `gui_light=front`. Binding и gui lighting — параметры интеграции; кубоиды и UV не переписаны после экспорта.
- `build_resource_pack.py --output <path>` — воспроизводимый ZIP из этих двух файлов и pack.mcmeta, без изменения пикселей.

Canonical managed artifact: `Client/ManagedPack/config/paxi/resourcepacks/JEM Building Hammer.zip`. Уже установленный Paxi4.0 активирует required pack; личный options.txt не редактируется. AutoModpack доставляет этот ZIP обычным managed path. В ZIP только три файла: metadata, item JSON и новый PNG. Другие BBB items и GUI не переопределяются.

## Наблюдаемое ревью

C0: геометрия связана и читается как молоток. Первая UV-привязка использовала texture UV canvas16 при atlas32, из-за чего часть граней стала прозрачной. Она отклонена; defective source сохранён только во внешнем authoring workspace. `hammer-c0.bbmodel` здесь — ранний untextured geometry checkpoint.

C1: источник создан с правильным32×32 UV canvas до импорта текстуры. Все грани восстановлены, соблюдена плотность UV; просмотрены front/back/right/three-quarter. Native first-person capture выявил обрезанную головку.

C2: только first-person transforms уменьшены и развёрнуты, чтобы вся головка помещалась в кадре. `display-firstperson_righthand.png` показывает исправленный результат. `display-thirdperson_righthand.png` показывает контакт хвата с рукой native mannequin. `display-gui.png` — native inventory display preview, а не Minecraft screenshot.

Visual auditor: форма/связность/толщина/обратная сторона/материалы/UV/контраст/технический экспорт — AGENT_PASS по наблюдаемым видам. `gray-*` и `black-*` — actual rendered neutral/silhouette views front/back/left/right/top/three-quarter at128px. `view-*` — textured256px; native display captures —512px viewport. Самостоятельный новый art candidate после C2 не создавался, дополнительных correction rounds нет. Анимация не применима к статическому vanilla item model.

Игровое отображение, shader compatibility и пользовательское принятие — UNOBSERVED. Никакого запуска клиента или gameplay QA в этой работе не было. Сборка Java и установка через AutoModpack не заменяют эти наблюдения. Сравнение силуэтов выполнено поздно и отмечено отдельно. Результат не выдан за USER_APPROVED.


| Editor criterion | Verdict | Наблюдаемое основание |
| --- | --- | --- |
| Silhouette | 4/5 | Black six-view: головка, две ударные грани, хват и наконечник читаются без цвета |
| Depth | 4/5 | Gray right/three-quarter: объём5units, непрерывная рукоять и крепление |
| Proportions | 4/5 | Три настоящих silhouette studies: тяжёлая головка и более узкий длинный хват |
| Purposeful rotations | N/A | Прямоугольный строительный инструмент; искусственные наклоны деталей не нужны. Повороты руки/GUI принадлежат display transforms |
| Personality/intent | 4/5 | Медные ударные грани и простой рабочий силуэт, без чужих fantasy motifs |
| Focal point | 4/5 | Головка — самая крупная и светлая масса, хват темнее и вторичен |
| Materials | 4/5 | View captures: медь и дерево различимы по значению, направлению рисунка и цвету |
| Pixel shading | 4/5 | Светлый верх и тёмный низ поддерживают реальную толщину |
| Texture marks | 4/5 | Разные UV-острова и редкие wear accents, без случайного шума |
| Held-object integration | 4/5 | Native first/third-person captures: хват касается руки, головка не обрезана |
| Rig | N/A | Vanilla static Java item, без скелета, animation loader или самостоятельной entity |
| Apparent-scale readability | 4/5 editor proxy | Native GUI display и128px six-view сохраняют noun/focus; gameplay остаётся unverified |

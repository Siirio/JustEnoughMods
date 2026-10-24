# JEMFood — полный технический контракт v3

**Финальное решение:** standalone `Mods/JEMFood` / `jemfood`, Forge Minecraft 1.20.1, Java 17. Только Minecraft + Forge — hard dependencies. Старый JEMCompat остаётся для остальных совместимостей, но больше не владеет едой. Этот текст совместно с JSON является полным ТЗ для исполнения.

## 1. Схема качества (locked)

| Tier | Категория RU | EN | HEX | Кол-во | Таланты | Основа времени |
|---|---|---|---|---:|---:|---:|
| 0 | Обычная | Ordinary | #9298A2 | 320 | 0 | — |
| 1 | Вкусная | Tasty | #52AE75 | 266 | 1 | 2 минуты / 2400 тиков |
| 2 | Изысканная | Exquisite | #63B3E8 | 213 | 2 | 3 минуты / 3600 тиков |
| 3 | Шедевральная | Masterwork | #B17BEA | 49 | 3 | 4 минуты / 4800 тиков |

Итого **848 exact-ID**. Бонусных JEMFood блюд **517**, из них 11 улучшенных `native_only` имеют **0 дополнительных способностей**, поскольку уже обладают/могут обладать сильными родными эффектами. Обычная еда может иметь оригинальные native эффекты — они **не удаляются**. Категория означает кулинарный класс, а не точное число всех видимых зельных эффектов.

Нельзя делать авторанжирование в рантайме по имени, namespace, тегам, родным эффектам или редкости. Единственный авторитет — `Catalog/changes/food-quality-assignments.json`, полученный из `food-quality-v3.json` и скорректированный только на основании проверенного установленного источника. Не поддерживать вторую независимую таблицу в JAR: `data/jemfood/food_quality.json` — построенная проекция Atlas authority.

## 2. Новый эффект и UI

Регистрируется **ровно один** положительный MobEffect `jemfood:food_blessing`. Видимое имя: **«Благословение еды» / «Food Blessing»**. Одна общая иконка: `assets/jemfood/textures/mob_effect/food_blessing.png` (18×18, приложена). Никаких других jemfood MobEffect ID/иконок. «30 эффектов» = **30 внутрисистемных traits**, а не 30 MobEffect в реестре. Список имён, английских эквивалентов, описаний и чисел — `EFFECTS_RU_EN.md` и `traits[]` JSON. Каждое RU/EN название **максимум 2 слова**. Использовать `Component.translatable`, не хардкодить локализацию в Java. Готовые `assets/jemfood/lang/ru_ru.json` и `en_us.json` приложены: установить их как ресурсы JEMFood; контролировать соответствие `traits[]` JSON и не заводить вторую систему переводов.

**Отображение в обычном GUI инвентаря Minecraft**: иконка единственная, рядом стандартный таймер. При наведении на **именно эту иконку/запись**, а не просто на любое место экрана, показывать:

```
Благословение еды              02:36
Блюдо: Рагу из ...
Качество: Изысканная

Камнекожа
Дубовая кожа I, если доступна. Иначе Сопротивление I.
Действует: 00:12 / откат: 01:20

Зверобой
+5% урона обычным враждебным мобам.
Действует: 02:36
```

Это демонстрация верстки, данные и активная ветка эффекта должны приходить с сервера. У разных trait собственные имена/описания/состояния; все разделы внутри одного tooltip. Цвет строки ранга строго по tier; именам trait умеренное тематическое окрашивание, предпочтительно vanilla chat formatting. На tooltip предмета показывать ранг, названия talents, base duration, optional preferred/fallback, нативные свойства не маскировать, прогресс 0..5 variety; `native_only` явно помечать «Родные эффекты / Native effects». Если нет активного Blessing, hover UI не рисовать. Не вмешиваться в прочие HUD, JEI, Jade или другие моды, не занимать чужие координаты без clipping.

**Ровно одна иконка:** backend effect `minecraft:*` или сторонний native MobEffect допускается только для 3 bridge traits и должен быть `showParticles=false`, `showIcon=false`; не скрывать ничего вне точного JEM-owned instance. Из-за ограничений vanilla source-blind эффектов, если способ безопасно скрыть именно JEM-owned native instance/отследить collision не работает, этот путь не выпускать как проверенный: пользоваться custom реализацией и документированным ванильным fallback, сохраняя одну визуальную иконку. Никаких невидимых permanent effects.

## 3. Уникальность и распределение таланта

30 авторских traits, 5 доменов ×6: путешествия, добыча, защита, бой, питание. Все имена и описания в `EFFECTS_RU_EN.md`. Вкусная еда получает один выбранный trait, изысканная — два разных, шедевральная — три разных, но 11 `native_only` ноль. Одна семья еды определяет тематически подходящий пул, но **каждый exact-ID имеет список `blessing_traits[]` прямо в JSON**. Не пересобирать при запуске на основе хеша или очередности регистрации; не назначать случайные таланты каждый раз при употреблении.

Дубликаты одного trait в одном блюде запрещены. Слоты из других блюд не складываются: **только один JEM bundle одновременно**. Повторение одного trait у разных блюд разрешено, потому что оно помогает игрокам выбирать рецепты; разнообразие обеспечивают тематические комбинации. Для парных изысканных наборов статический план даёт **211 различных пар на 213 продуктов**. Для шедевральных JEM-блюд создано **40 различных троек**, остальная разница native-only. Отдельные эффекты модов не нужны для работы 27 custom traits.

## 4. Bridge effects и mod absence (mandatory)

Три мостовые способности используют проверяемый external effect ID с **явным vanilla fallback**:

| Trait | Preferred если установлен и доказано безопасен | Fallback без него | Амплификатор fallback | Первая выдача/повтор |
|---|---|---|---|---|
| Прыть / Fleetfoot | `irons_spellbooks:hastened` | `minecraft:speed` | 0 (I) | 20/30/40 c; 60 c cooldown |
| Пловец / Swimmer | `alexsmobs:orcas_might` | `minecraft:dolphins_grace` | 0 (I) | 20/30/40 c; 60 c cooldown |
| Камнекожа / Stonehide | `irons_spellbooks:oakskin` | `minecraft:resistance` | 0 (I) | 8/12/16 c; 120 c cooldown |

НЕТ реализации вида `if (!ModList.isLoaded()) skip food`. Мод блюда может присутствовать без мода эффекта; статус и его ранг остаются, резервное действие гарантировано vanilla Registry. Для exact native registered ID lookup проверить resolver, `null`/ошибка/несовместимость => fallback. Не выполнять `Class.forName` третьего мода на обязательном пути; использовать только `ResourceLocation`, registry lookup и необязательный строго охраняемый адаптер. **До проверки реального действия предпочтительного эффекта использовать fallback даже если registered ID найден**, потому что идентичность имени не гарантирует одинаковый баланс.

Повторная выдача backend эффекта допускается только когда старый JEM-owned effect закончился, соответствующий cooldown прошёл, текущий Blessing жив и нет независимого same-ID эффекта. Коллизия: независимый same-ID источник (native food, spell, potion, gear) всегда выигрывает без изменения усилителя, длительности, hidden chain. При отмене или очищении Blessing снимать **только собственный эффект**, со строгим fingerprint/identity ownership. Подмена более сильного чужого эффекта запрещена. Если источник нельзя достоверно узнать, оставить его неприкосновенным, не выдавать новые подвешенные эффекты. Защита от повторного получения absorption из старого кода остаётся частью миграции, но в новом JEMFood нет ни одного собственного absorption trait.

## 5. Авторитет сервера и hooks

**Обычная еда**: не изменяет текущий набор Blessing; если она отмечена `variety_eligible`, обновляет только рацион. **Новая повышенная**: успешно съел => заменяет только owned Blessing bundle на новый, активирует 1/2/3 traits; `native_only` повышенная => снимает старое JEM Blessing и обновляет разнообразие, но исходный food effect остаётся родным.

Один эффект на игрока, один bundle без дублей. Повтор того же ID в пределах действующего bundle не обновляет timer, пока осталось более 20% первоначальной длительности. Cooldown и разовая выдача не сбрасываются употреблением того же или другого блюда. Равноценные trait effects не стакаются через замещение блюд и различные sources. По истечении общего набора снять соответствующие атрибуты и эффекты.

- `LivingEntityUseItemEvent.Finish`: серверный player; event item BEFORE consumption, подтверждённая еда, проверка, что использование завершилось; творческий фальш-вызов/предметный клик без приёма пищи не награждает.
- Native Cake: award только после успешного уменьшения количества порций в блоке, не по самому `RightClickBlock`. Для 21 прочих placed-food маршрута проверить source JAR/bite method, native block ID, server success и не отменять чужие вызовы. Для неизвестного route fail-closed + отдельный gate; не выдать за любой RightClickBlock.
- Периодические эффекты: как можно меньше тиковых операций и **никаких** scanning entity/chunk/world, нет постоянно работающего планировщика по серверу. `PlayerTickEvent` на сервере только владельца, максимум активные traits и 5 timestamp keys. Содержимое класса MobEffect не перерегистрируется при смене блюда. Игрок без активности получает O(1) или пропуск.
- Атрибуты movementSpeed/attackSpeed/maxHealth/knockbackResist/stepHeight/swimSpeed: **один идентификатор modifier на конкретную способность**, одинаковая операция и строгая логика attach/detach без множества менеджеров. Не переписывать baseValue чужих модов. Forge-only должны компилироваться и работать; если native Forge attribute не существует, использовать проверенный допустимый hook, не подменяя движение клиентом.
- Боевые события: проверять source/server/player ownership/type, только итоговый разрешённый damage event, агрегировать bonuses сначала, `min(totalFoodBonus,0.07)`. Defense → `min(totalPhysicalFoodReduction,0.10)` для физического/снарядного и `min(totalHazardFoodReduction,0.30)` для огня/замерзания/яда/иссушения. Не выдавать damage до проверки iFrames/инвулнерации боссов; Food не является способом обхода combat protections. PvP outgoing JEMFood percentage boosts = 0. Снятие/возврат armor/health/food всякий раз только JEM-owned, без внешних снапшотов.
- XP и урожай: события должны быть реальными успешными (mature plant, confirmed fishing catch, nonboss mob XP), cap +1/+2 и собственные cooldown. Прерывание, creative, повторная отправка, неподходящий инструмент, protected block и client prediction не дают наград. Никакого дополнительного предметного лута в этой версии.
- При входе, смерти, измерении, clone, logout, рестарте и `/effect clear`: корректно снять текущий Food Blessing (включая hidden bridges и modifiers); разнообразие и cooldown сохраняются в persistent data. Нет permanent XP/HP/refill через перезаход.

## 6. Длительности и ограничения

Время в тиках: 20 ticks = 1 s, 24000 ticks = 1 Minecraft day. Обычный Blessing набор 2400/3600/4800 тиков (2/3/4 минуты). У мостов свои короткие эффекты по таблице. При наступлении cooldown re-proc допускается только в рамках жизни набора, не ретроактивно. Cooldowns считаются от overworld `gameTime` и сохраняются; отрицательные/будущие timestamp после восстановления не дают обхода. Вне игровой симуляции реальные часы не используются. Указанные 30 механик параметризованы по tier; см. `traits[]` JSON и `EFFECTS_RU_EN.md`.

**Скорость:** +5% от разнообразия живёт отдельно от Food Blessing как vanilla attribute modifier с фиксированным UUID, не стакается. Вариант `Secondwind` может кратковременно прибавить 4/6/8%, но не должен давать экспоненциальное накопление. При взаимодействии Satiety и Trailrunner использовать **максимальное** снижение истощения только по релевантному trigger, не произведение коэффициентов. Статус не вызывает постоянного скрытого лечения и не превращает Heal в damage reversal.

## 7. Variety

`variety_eligible = true` у **569 exact-ID**, в том числе у некоторых Ordinary vanilla foods. **5 разных** отмеченных ID за окно **72000 тиков** дают +5% `Attributes.MOVEMENT_SPEED`, операция `MULTIPLY_BASE`. Повтор того же ID только обновляет его timestamp, не повышает счёт. Точно один modifier. Если от уже учтённых IDs возрастает время окна и остаётся 4 — бонус исчезает. Сервер может продолжать работать без игрока, timestamp стареют на gameTime; остановленный сервер не старит их. Разнообразие не требует crafting модов. Именные примеры: vanilla apple, bread, carrot, baked potato, cooked beef.

## 8. Standalone installation / Migration

- `Mods/JEMFood` полностью автономный Gradle project, без runtime и build requirements JEMCompat, Guide, Mobs, Cookery, JEI, Iron's Spellbooks. Build compileOnly для сторонних JAR также не должно становиться обязательным в Forge-only smoke build. Использовать Java 17 и существующие модульные конвенции `Mods/MobHome` без копирования лишних систем.
- Из `Mods/JEMCompat` удалить/отключить прежнюю food logic **один раз**: `feature/food/FoodCatalog`, `FoodEvents`, `FoodNetwork`, `client/food/FoodTooltips`, mixin/food classes, JEMCompat food projection, registration и старые `assets/jemfood` строки (переместить в новый jar). Проследить references; никаких двух award sources или конфликтующих registry/status.
- Старый persistent tag `JemFoodQualityV1`: точно мигрировать важные `Meals` (timestamps), cooldown и состояние ownership. Не переносить старые JEM food buff как будто это новый blessing. Если старые owned-native эффекты в игроке точно опознаны fingerprint, убрать только их; если нет — оставить внешними до естественного конца. `JemFoodQualityV1` не ломать в случаях rollback; новое `JemFoodBlessingV2` хранить versioned и сделать idempotent migration receipt. Старый absorption debt не обнулять в середине живого окна; новый JEMFood не рефиллит absorption вообще. Вновь импортированные timestamps только для существующих разрешённых ID; от старого спящего профиля не считать новые ID задним числом.
- Если двух модов нет — Forge-only, не грузить mixin target другого модa; если есть только meal mod — fallback; если есть effect mod без meal mod — живой catalog без лишней регистрации/падения. Клиент и dedicated server нуждаются в JEMFood JAR; vanilla без Forge не поддерживается по определению.
- Для `client-only` эффекты не подходят: атрибуты/XP/бой серверные. Клиентские — только рендеринг tooltip, hover и отображение одной иконки.

## 9. Atlas и выпуск

Перед изменениями: `AGENTS.md`, `.agents/skills/jem-registry-keeper/SKILL.md`, `Catalog/README.md`, `Catalog/changes/schema.json`, `Docs/food/README.md`; выполнить `query_atlas.py behavior <id>` для репрезентативных точек, `maintain_atlas.py scan`. Единственный authority `Catalog/changes/food-quality-assignments.json` c действующим change record `food-quality.json`, переназначить owner на `JEMFood`, обновить `Catalog/effective` и историю. До изменений проверить новый исходник против текущего test jem поколения/manifest, не путать с original JEM. Распределение и все новые параметры в pack `data/jemfood/food_quality.json` только как projection. Другая клиентская работа по Guide/FTB не входит в эту задачу.

Сборка `jar`, `reobfJar`, unit tests, Forge-only smoke, full JEM integration на **test jem**, installed JAR hash and published generation manifest. Оригинальные JEM и MineRENT **не менять**. Стандартные обязательные команды после source: `python Catalog/generator/maintain_atlas.py rebuild`, `audit`, `scan`, `python Catalog/generator/validate_atlas.py`. Если smoke не смог пройти, отчёт «NOT VERIFIED», не продавать как готовое.

## 10. Неприятные edge cases — решение заранее

1. Несколько эффектов JEMFood не появляются отдельными строками в inventory: одна иконка и hover всё объясняют.
2. У скрытого native effect same-ID появился внешний источник? Не сокращать внешний и не восстанавливать чужое hidden chain; JEM не навязывает overwrite.
3. Milk `/effect clear` сняли Blessing — снимать только его модификаторы/owned bridges, cooldown не сбрасывать.
4. Cake piece повторно съеден — один callback на один успешный откус; в `native_only` не создаётся пустое bless.
5. Порция блока имеет другой registered item ID: использовать block identity для world bite, item identity для инвентаря, canonical mapping в JSON. Никаких NBT-expanded pseudo IDs.
6. После смерти JEMFood health modifiers удалены до восстановления HP и перед сохранением игрока. Синхронизация не копирует client state обратно на сервер.
7. Vanillla status `Food Blessing` с тем же Registry name не существует; резервировать `jemfood:food_blessing` единожды. Предпочитаемые эффекты отсутствуют? fallback на ваниль, если fallback сам не найден — fail visible (это несовместимость Minecraft версий).
8. HUD tooltip не должен нарушать работу Inventory Effects UI иных модов. Отдельный один Mixin к конкретному vanilla экрану допустим лишь после версии/сигнатуры проверки; никаких универсальных перехватчиков всего UI.

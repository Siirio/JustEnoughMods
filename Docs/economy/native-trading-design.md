# Current native economy contract — 2026-10-16

The later user-approved [building supply and permanent restrictions](market-building-supply.md)
overrides NPC sale eligibility and public-market admission in the older contract below.
Native presentation, lifecycle and funded settlement remain unchanged.

The current implementation extends the native merchant contract as requested in
the complete economy overhaul. The current coverage and remaining runtime evidence
are recorded in [merchant coverage](merchant-coverage.md). Earlier implementation
status below is historical and must not override this contract.

Native offers remain authoritative and preserve all item counts, original NBT,
progression, stock, demand and restock behavior. Global Market commodity policy is
reused when applicable. Exact native-only merchandise prices now cover the
previously unavailable vanilla equipment, enchanted books, maps, food and crafted
goods without admitting them to the general Market. This explicitly resolves the
older native-food exclusion conflict under the user's new preserve-all-native
trades requirement. Unique boss/progression rewards remain protected.

Every admitted monetary merchandise row has a reverse companion direction with
exact goods/NBT/counts and zero extra XP. Thirteen original vanilla professions
receive weighted, appropriate modded specialties gradually at levels 2–5. The NPC
persists each generated selection and native offer stock; no reopening/restart or
restock reroll exists. New profession registration is unnecessary.

Purchase quotes use real merchandise prices plus enchantment acquisition premiums,
not an emerald exchange rate. Discounts are bounded after racial events by both
half the base material budget and maximum primary-demand buyback plus one Copper.
Container buyback uses the same funded reserve, quota and ledger commit as ordinary
Market sales, with an explicit trusted native-merchandise policy resolver.
Server authority and native presentation remain unchanged. The separate display
work uses the shared Coin renderer and the actual five registered Coin items.

## Historical design evidence

# Нативная торговля Coins и Закупщик

Статус: схема и оба плана одобрены2026-10-14; реализация в исходниках выполнена
в этой сессии по команде «делай сам ... Сразу пуш». Изменены Economy, Roles и Server;
установленные JAR, игра и release baseline не обновлены. Сборка, применение Mixins
и визуальный результат не проверены: отдельного разрешения на эти действия нет.
Выполнено одно независимое read-only ревью исходников и один проход исправлений
четырёх существенных замечаний, без повторного ревью.
Основание: прямое решение пользователя от 2026-10-14 — заменить
валюту, сохранить нативную торговлю и Xaero, дать Закупщику торговое окно без чата.

## Утверждённый пользовательский контракт

- Обычные жители и странствующий торговец используют оригинальный MerchantScreen:
  нативный список, выбор предложения, два входных слота, результат, инвентарь,
  прокрутка, звуки, опыт, пополнение запасов и завершение взаимодействия.
- Coins сохраняют стоимости 1/10/100/1 000/10 000 медных и существующие assets.
  Никаких новых монет, глобальной перерисовки интерфейса или вырезания текстур из
  приложенных картинок. Например, 135 = 1 Gold + 3 Silver + 5 Copper.
- Закупщик открывает такое же нативное торговое окно со своими предложениями
  закупки. Нажатие на него не выводит спрос, цены, ошибки или инструкцию в чат.
  Он реально принимает товары; окно не ограничивается информационной сводкой.
- Xaero сохраняет исходные GUI, навигацию, zoom, управление и вид waypoint.
  Расшарены только исследованная местность и обнаруженные боссовые места;
  личные точки, дома и смерти не становятся публичными.
- Дополнительные JEM-слои территорий, проектов и событий можно скрыть; они не
  заменяют нативное содержимое. Отдельный SMP GUI не откатывается автоматически.
- Новый общий принцип: расширять интерфейс исходного мода; заменять его только
  после явного одобрения конкретного редизайна.

## Подтверждённое состояние до восстановления

Исходники проверены на main после `1bb1569`; установленная поверхность отдельно
зафиксирована в ночном отчёте и не считается владельцем исходных правил.

| Владелец | Что сейчас происходит | Последствие |
|---|---|---|
| CoinMerchantRuntime.interact | Отменяет обычное взаимодействие и вызывает NetworkHooks.openScreen | Не запускается исходное открытие торговли |
| CoinMerchantMenu | AbstractContainerMenu, 18 декоративных предложений, клик сразу исполняет обмен; quickMoveStack пустой | Нет привычного нативного списка/слотов/shift-click; список обрезан |
| CoinMerchantScreen | Собственное окно шириной176 | Нативная торговая композиция заменена |
| MarketRuntime.interact/bulletin | Закупщик печатает максимум8 строк в чат | Нет собственного торгового окна и прямой операции продажи |
| EconomyApi.processNpcSale | Ищет и удаляет товары в инвентаре игрока | Нельзя повторно вызвать после нативного списания из торгового контейнера |
| RaceTraitRuntime.trade | Возвращает изумруды по исходному emerald cost | Coin-покупка может создать изумруд без изумрудной оплаты |
| MapClient.render | Всегда рисует подключённые слои с fill0x38 и непрозрачным border, затем подписи | Нет пользовательского скрытия; визуальная перегрузка возможна, конкретный внешний вид пока не воспроизведён |

Точный Forge47.4.10 mapped JAR подтверждает MerchantScreen.imageWidth276,
семь видимых кнопок с прокруткой, MerchantMenu/Container/ResultSlot и их штатные
методы выбора, переноса и выдачи. Не копировать их реализацию в новый экран.

## Решение по архитектуре

### Нативная оболочка и предложения

Обычное открытие торговца остаётся нативным. Предпочтительная точка расширения —
нативный торговый контейнер/результат с узким серверным адаптером Coins; отсутствие
Forge/public seam обосновывается по точному bytecode прежде, чем добавлять Mixin.
Не сохранять отмену обычного взаимодействия как скрытую замену новым JEM-экраном.
MerchantScreen и его texture/geometry не заменяются.

Нативный MerchantOffer остаётся владельцем состава товара, количества, спроса,
репутационных изменений, uses/maxUses и restock. Клиент получает денежную проекцию
той же строки; она не становится вторым хранилищем офферов. Все строки сохраняются
в исходном порядке, включая NBT-товары и обмены с дополнительным входным товаром.
Не вводить прежний предел18. Не превращать ограничение рынка в исчезновение
нативного предложения. Спорные допускаемые товары нельзя молча разблокировать:
это отдельный явный конфликт действующей политики, описанный ниже.

### Платёж и предметы

Economy остаётся единственным владельцем суммы, Wallet, физической выдачи,
погашения lot и резерва. Пять денежных номиналов не пытаются занять пять слотов:
в нативной строке/tooltip показывается сумма и разложение; фактический расчёт
использует Wallet и штатное погашение валидных физических монет при нехватке.

Представление суммы не является настоящей монетой: оно не может попасть в
инвентарь, курсор, выброс, другое меню или стать входом для deposit. Получение
реальных монет возможно только через существующую серверную lot-выдачу. Деньги
при продаже зачисляются в Wallet с понятной обратной связью внутри торгового окна.

Сервер перед принятием результата проверяет действующее меню и выбранную строку,
торговца/дистанцию/авторизованный account, реальные предметы входных слотов,
актуальную цену, uses/лимиты/резерв, место выдачи и уникальную операцию. Один
commit перемещает товары и деньги, затем вызывает native notifyTrade ровно один
раз. Отказ не выдаёт товар/XP и не расходует stock. Закрытие/смерть/смена предложения
возвращают реальные входные предметы и отменяют неисполненный резерв.

Не использовать прежний processNpcSale поверх уже списанного нативного товара.
Общий владелец расчёта принимает проверенный источник предметов и точную целую
операцию; inventory-продажа Shipping Box и торговые слоты используют его без
дублирования удаления. Частичное allowance не исполняет половину нативного обмена.
Сервер формирует доступное предложение либо отказывает полной операции.

Перед native callback торговый игрок уже установлен. Контекст оплаты явно
отличает Coin settlement от действительно оплаченного emerald trade. Human
скидка применяется в соответствующей валюте один раз; изумрудный refund не
исполняется для Coin settlement. Нельзя чинить только порядок setTradingPlayer:
это активирует известную генерацию изумрудов раньше.

### Закупщик

Его предложения строятся Economy из текущего авторитетного demand, BUY_AND_SELL
политик, бюджета NPC_MARKET и индивидуальных/общих лимитов. Он не получает
случайные сделки обычной профессии. Обновление спроса, исчерпание резерва и лимитов
отображаются нативной доступностью строки и tooltip, без сообщений в чат.

Все подходящие строки доступны через нативную прокрутку; не оставлять limit8.
Закупка расходует тот же рынок и те же лимиты, что Shipping Box; нового Coin faucet
или отдельной таблицы цен нет. Перед take цена и доступность перепроверяются.
Специальный NPC определяется серверной идентичностью/tag, а не локализованным
именем. Обычные жители не превращаются в Закупщика.

Команда /market, если сохраняется как удалённый просмотр, открывает то же
представление в read-only режиме без чата. Дистанционный просмотр не разрешает
удалённую продажу, не загружает чанк торговца и не обходится поддельным пакетом.

### Xaero

Native GuiMap остаётся владельцем layout, клавиш, dropdown, масштаба и waypoint.
Дополнение добавляет только небольшой локальный контроль видимости отдельных
JEM-слоёв. По предлагаемому default дополнительные полигоны скрыты до включения;
native/shared terrain и известные boss locations сохраняются. Этот default —
выбор Codex для рассмотрения, а не уже установленное пользовательское правило.

При включении — тонкие контуры без постоянной широкой цветной заливки; подробная
подпись появляется по hover/выбору, не застилает личные waypoint. Скрытие влияет
только на клиентское рисование/превью и не меняет permissions, SavedData, события
или сетевую приватность. Нативные правые действия остаются; добавленные JEM-действия
дополняют их и не захватывают обычную мышь вне явно включённого режима проекта.
Скрытие слоя отменяет только незавершённый локальный draft, не сохранённый проект.
Одобренные объёмные границы событий в мире не затрагиваются.

## Уточнение пользователя: цена как иконки монет

Нынешние Coins-цены обычных жителей вычисляются по ItemPolicyRegistry, а не через
единый курс emerald. Например, цена зависит от buyCopper/sellCopper товара и
нативной корректировки стоимости. Поэтому «сохранить цены JEM» и «заменить только
изумруд на монеты, сохранив нативную цену» являются разными решениями.

Пользователь пояснил, что хочет видеть в исходном окне цену вида «иконка медной
монеты3, иконка золотой монеты12». Это требование к представлению суммы, а не
поручение задать новый обменный курс. 12Gold+3Copper =1203Copper. Для уже
оценённых сделок сохраняется нынешняя авторитетная сумма Economy; клиент лишь
раскладывает её в иконки и количества. Иконки и количества видны непосредственно
в строке сделки: пример пользователя должен читаться без наведения мыши.
Разложение не ограничивается двумя входными слотами и не изображает монеты как
реальные предметы этих слотов. Tooltip может пояснять расчёт и баланс, но не
заменяет показ цены в строке. Геометрию размещения нескольких номиналов необходимо
согласовать с нативным списком без нового окна и переполнения slot/stack.

Закупщик продолжает использовать demand/прайс Economy. Обычный native offer, который есть
у торговца, но помечен NONE системным рынком, требует явного разграничения:
сохранение всех native acquisitions нельзя одновременно выдавать за неизменность
старого строгого market gate. В реализации сначала выделяется отдельный native
currency contract и точно определяется существующая цена/допуск; отсутствующую
цену нельзя сочинить из UI-примера. Эта граница особенно важна для модовых/NBT
офферов и native diamond gear. Числа3/12 — пример отображения, не новый прайс.

## Поверхность реализации и поставка

### Реализованный source contract

- Native NPC opening не отменяется. MerchantMenu/Container получают узкий адаптер;
  старые CoinMerchantMenu/Provider/Screen удалены. Зарегистрированный coin_merchant
  ID сохранён как неиспользуемый alias нативного экрана.
- Coin result защищён от нативного pickup/clone/drop/pick-all. Native GameMode
  передаёт revision/selection/button/type/request nonce; сервер проверяет
  показанные и актуальные условия. Unquoted vanilla monetary take не исполняется.
  Item-only barter использует исходный result slot.
- Оба порядка реальных входов поддерживаются. Whole sale не вызывает прежнее
  inventory-удаление. Native notifyTrade вызывается один раз, actor установлен,
  Human получает Coin-скидку через Roles и не возвращает emerald.
- Закупщик имеет demand-derived native offers без limit8/18, статусы внутри окна.
  /market — transient read-only native projection без поиска NPC/chunk load.
- Xaero: три локальных native-dropdown toggle territories/projects/events,
  default off; контуры и одна hovered label, hide/logout отменяет local draft.
  Native/private waypoint и world event walls не менялись.
- Узкий payout cell показывает точную сумму одним медным номиналом, если полное
  разложение не помещается при читаемом масштабе; выбранная строка дополнительно
  показывает полное разложение иконками под native trade slots. Это Codex-решение
  о размещении; визуальная приёмка остаётся непроверенной.
- Snapshot/nonce ограничены4096 строками/256 последними request IDs на меню;
  refresh максимум раз в20ticks плюс немедленные локальные изменения/операции.
  Shift-click ограничен64 целыми сделками за запрос с проверкой цены, исходных
  item terms, средств/запасов/места; дальнейшие сделки — новый клик.
- Unknown/NBT/protected prices не сочиняются: предложения видны с причиной
  недоступности. Это не утверждение полной Coin-оценки native gear.
- Economy protocol2→3 требует matching client/server Economy и соответствующие
  Roles/Server JAR при последующей явно разрешённой сборке/поставке.

Economy: merchant adapter/runtime/menu, registry/client registration, network
currency projection, MarketRuntime, общий settlement API. Roles: native trade
callback/currency context. Server: MapClient, MapScreenMixin и локальные settings.
Исходная геометрия/ресурсы иконок монет остаются прежними.

Старая CoinMerchantScreen удалена; совместимость зарегистрированных
ID оценивается отдельно от удаления неиспользуемого класса. Нативный singleplayer
не получает SMP listeners, платежные Mixins или дополнительную authority.
Новый клиентский renderer/packet контракт требует matching client/server JAR.
Это не server-only выпуск. В текущем запросе не разрешены Gradle, игровые проверки,
native публикация или MineRENT; работа следующего этапа не выдаётся за уже сыгранную.

## Критерии приёмки для последующей явной проверки

- Реальное native окно обычного жителя и Закупщика, больше18 строк с прокруткой,
  оба входных товара/NBT, click и shift-click, возврат предметов при закрытии.
- 135Copper и стоимость больше64/несколько номиналов читаются без ложного
  наличия физических монет; preview не покидает меню.
- Недостаток денег/резерва/лимита/места, исчерпанный оффер, смена цены/дня/меню,
  повторный пакет и отключение между reserve/commit не дают деньги или товары дважды.
- Coin stock+XP+callback ровно один раз; Human не создаёт emerald за Coin-покупку;
  действительно нативная emerald-оплата сохраняет её корректный контракт.
- Закупщик ничего не печатает в чат; все необходимые состояния видимы в окне.
- Xaero исходный GUI/управление неизменны; toggle переживает reconnect, скрытый
  слой не перехватывает мышь, private waypoint/дом/смерть не уходят другим игрокам.
- До разрешённой сборки/игрового replay статус остаётся implementation-unverified;
  никаких автоматических тестов/fixtures в production не добавляется.

## Источники

- [Нативный merchant adapter](../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/mixin/MerchantMenuMixin.java)
- [Native settlement session](../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/NativeTradeSession.java)
- [Interaction interception](../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantRuntime.java)
- [Buyer/chat owner](../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/market/MarketRuntime.java)
- [Economy settlement](../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/api/EconomyApi.java)
- [Human refund](../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java)
- [Xaero layers](../../Mods/JEMServer/src/main/java/com/siirio/jemserver/client/xaero/MapClient.java)
- [Native map hooks](../../Mods/JEMServer/src/main/java/com/siirio/jemserver/mixin/xaero/MapScreenMixin.java)
- [Current merchant coverage](merchant-coverage.md)
- [Atlas applied stock owner](../../Catalog/changes/records/economy-native-merchant-stock.json)

Mapped Forge47.4.10 class signatures were inspected locally. User-supplied image
links are references only, not assets or evidence that the proposed GUI was rendered.


## Native monetary-cell correction - 2026-10-18

User direction (master brief section16): remove emerald money placeholders, show the existing five Coin denominations, correct selected prices/tooltips and preserve native merchandise, levels, professions, restock and actual payment. Supplied screenshots5/6 were absent, so no screenshot comparison or visual acceptance is claimed.

Exact local Forge1.20.1-47.4.10 mapped bytecode establishes the first offer icon at `topPos+19`, then20 pixels per row. Native cell x positions are `leftPos+10` (A), `+40` (B), `+73` (output); button hit rows begin at `topPos+18`. The old `topPos+20` quote lookup skipped the first row and used the previous quote for every subsequent row. That display error was independent of the server's correct selected-offer transaction owner.

`MerchantScreenMixin` now maps those exact coordinates and current `scrollOff` to the same `NativeTradePacket` quote used by selected purchases. The A money cell reserves the actual B item; two monetary inputs show one total, not two charges. Original goods, item counts, NBT and barter keep native rendering. Unpriced monetary cells show an unavailable marker and their existing server reason, never an invented amount or an emerald exchange rate.

Native `TradeOfferButton` monetary tooltips are narrowly suppressed before the Coin tooltip is drawn. Merchandise and barter tooltips remain native. Compact cells retain the complete denomination decomposition; their decision tooltip wraps full-size icons/counts to available screen width and includes an actual denial reason when needed. The selected amount uses measured localized-label width, monetary empty inputs have a non-inventory Coin face, and the existing guarded payout preview uses the leading quoted denomination. The old flat gray result-slot paint-over is removed; native resource-pack slot surfaces and highlighting remain.

Financial path inspected without changing it: `NativeTradeClickMixin` sends the acknowledged container/revision/offer/nonce; `NativeTradeSession.takeQuoted` compares current price/direction and original native terms, then `take` checks current funds/inputs/stock and output capacity. `NativeTradeSettlement.commit` uses the exact same `quote.copper()` for funded sale or reserved purchase settlement before goods move and native callbacks run. A cosmetic result preview is never a delivered physical coin. No new conversion, quote cache, offer list, transfer or restock owner was added.

Source correction and parent-owned production compilation are separate from runtime acceptance. No tests, automated static analysis or gameplay launch were run by this worker; vanilla/modded merchant, multi-denomination, barter, restock, inventory-conservation and GUI-scale acceptance remain pending.

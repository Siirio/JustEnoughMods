# Current native economy contract — 2026-10-16

## Видимый результат нативной сделки — 2026-10-26

Пользователь сообщил, что оплачиваемый товар выдавался по нажатию, но оставался невидимым в выходном слоте. Установленная по исходникам причина: клиентский `MerchantContainer.updateSellItem()` выполнял обычный пересчёт по предметам входных слотов после серверной проекции денежной котировки, хотя денежная оплата виртуальна. JEMEconomy сохраняет полученную от сервера проекцию для денежной сделки на клиенте. Выбор предложения не очищает локальный слот заранее, поскольку два предложения с одинаковым результатом могут не породить повторного slot packet. Native barter продолжает обычный пересчёт. Только серверная сессия разрешает получение результата, списывает Coins и учитывает stock/restock. Источник исправления — `mixin/ClientMerchantContainerMixin.java`, версия согласованного сетевого JAR обновляется вместе с клиентом и сервером

Статус: `in testing`. Исходный дефект и механизм исправления подтверждены чтением Forge 47.4.10 и JEM Economy. Появление товара в живом клиенте и принятие игроком ещё не подтверждены

## Current source contract — 2026-10-21

Source implementation / in testing. Runtime acceptance is unrun; production artifact delivery, native AutoModpack application and MineRENT deployment remain pending. This section does not claim a completed release.

The user's final overrides supersede earlier funding proposals and Buyer/wallet UI requirements below. CurrencyCore keeps the sole ledger, quote, physical liability and receipt owners. Coins of one denomination now use a world-local bearer series so native stacks merge up to64; operation-specific quantities remain in the server journal. Proven legacy Coins normalize lazily on encountered inventory/container/drop paths without a world scan or granting backing to unknown stamps. Withdrawal capacity includes partial matching stacks before debit. Legacy container recovery counts delivered quantities rather than treating a repeated pooled lot ID as a complete payout.

Approved base sale anchors per64goods: wheat10Copper, carrots8Copper, beetroot80Copper, gold ingots54Copper and diamonds1000Copper (1Ether). Fish prices remain unchanged. Exact rational fractions, carried account remainder and owning generation inputs retain these anchors; native Mending purchase has an8Gold minimum. Netherite ingot sale is admitted through its exact commodity policy; this does not open arbitrary netherite equipment. Ordinary account/global/family daily caps and artificial ordinary building stock are removed; finite NPC market funding, special disc limits, admission/progression restrictions, native villager stock/restock and bounded atomic delivery remain authoritative. Bulk purchases may fit the player's inventory; a private warehouse is no longer mandatory.

Starter grants remain20Gold with their existing finite funding and one-time receipts; no reserve redistribution or promise of40simultaneously funded starters was approved. The sole newly authorized production issuance is daily happiness income credited to the town treasury through CurrencyCore, with persisted town/day deduplication and supply accounting. It is not a market, wallet or starter emission permission. See the Settlements decision journal for the formula and standalone emerald distinction.

Shipping is a public terminal with one authenticated visitor at a time. Placement ownership still controls breaking. Goods and legacy payouts remain account-bound across session close/restart/full inventories, and automation is denied during a visitor session or incomplete transaction. Sales credit the current seller's ledger; physical withdrawal is explicit. The money tab owns deposit, withdrawal, transfer and icon-to-icon exchange with all five denominations available in each dropdown. Only nonzero denominations appear in amount summaries. Buyer creation/trading and the standalone wallet UI are retired while save compatibility, money data and shared native merchant services remain. Native MerchantScreen, offer counts/NBT/restock and ordinary Xaero controls remain protected.

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

### Ежедневный автоматический скупщик — Shipping Box

Блок ежедневной продажи — существующий `jem_economy:shipping_box`. Его действующий рецепт: сундук в центре и восемь любых досок вокруг. Рецепт не создаётся повторно: он уже присутствует в Economy и поставляемых JAR. Продажу содержимого в начале рыночного дня выполняет прежний `MarketRuntime` при `dayTime=1000`; ограничения, цены и учёт денег сохраняют своих владельцев.

Исправление обнаружения от 2026-10-20 добавляет существующий BlockItem в нативную творческую вкладку Functional Blocks через `BuildCreativeModeTabContentsEvent`. JEI 15.49 получает предметы из содержимого творческих вкладок; при `showHiddenIngredients=false` отсутствие этого предмета скрывало и поиск блока, и доступ к его уже существующему рецепту. Здесь не вводятся отдельный JEI-плагин, копия рецепта, второй скупщик или новая экономика. Внешний вид Market и его органы управления не меняются. Проверка пользователем в игре остаётся отдельной стадией.

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


## Shipping handoff implementation — 2026-10-20

The user authorized the shipping-economy handoff. CurrencyCore remains the only money and price owner. `item_policies.json` now supports `SELL_ONLY`, exact numerator/denominator prices, normalized material-family quota units, fixed-price discs and explicit building bulk tiers. `market_limits.json` owns ordinary daily400 personal/10000 global payout. Selling a disc uses its own shared one-per-account Minecraft-day quota, fixed10000 without demand/racial multiplication, and the actual funded NPC reserve; it does not consume the ordinary monetary allowance.

Purchase anchors retain oak64=70 and iron64=500. The user subsequently chose «Сохранить дешёвую стройку, скорректировать выкуп»: derived mineral/food/native buyback now follows the exact current recipe proof. Original proposed sale amounts are superseded by canonical fractions in `item_policies.json`. Exact normalized families retain9:1 arithmetic, while quartz/amethyst use actual4:1 evidence. Cod3, salmon4, beef4, leather3 and disc10000 remain fixed income anchors. Native offer counts remain unchanged.

`EconomySavedData` owns the account fractional claim. `quoteSale` adds the existing claim to an exact transaction. `reserveSale` transfers payout plus the change in rounded-up residual backing from NPC_MARKET into the existing MARKET_ESCROW and creates a terms-bound durable operation. One account cannot overlap pending fractional sales. A zero whole payout consumes no goods. Cancellation is permitted only before consumption/issuance; `commitSale` completes the existing inventory/payment lifecycle. Replay never advances the residual twice. Stored fractional receipts are not silently evicted; the explicit capacity bound stops additional operations pending maintenance. The shipping journal marks the new escrow route explicitly so legacy pending payments retain their original source.

Market retains its accepted storefront and adds Building materials with64/256/1024/4096 order sizes and server-produced full-order prices. Dirt/cobblestone1024 cost40, stone60, sand/gravel70, grass80, smooth stone90, stone brick/terracotta/glass100, brick180; oak remains1120 per1024. Building supplier stock is separate from sale quotas (65536 personal/262144 global units per Minecraft-day). Delivery uses ordinary legal stack sizes in the player's existing registered settlement storage, resolved through the actual `SettlementApi.ownedTowns`, `TownRecord.storage` and `SharedStorageService.handler` owners. Only loaded, modifiable, authorized inventories in the same dimension within128 blocks qualify. Pooled capacity is checked before debit; absence/full storage refuses before funds move. No Tom's Storage access or new project/fund system is invented.

`MarketBulkOrders` journals exact before/after slot snapshots, reserves real wallet funds into existing escrow, saves the native storage before final settlement to NPC_MARKET, and retains terms-bound request receipts. A process interruption with ambiguous external inventory state quarantines the order/escrow and blocks repeat delivery; it does not guess a rollback, refund, or mint compensation. This is an explicit reconciliation state, not a claim that crash/player acceptance has passed.

Current coverage and monetary proof are documented in [shipping-economy-current-audit.md](shipping-economy-current-audit.md). The current schema2 capture contains13935 actual loaded items and15416 recipes, separate from17187 static Atlas observations. All826 FoodProperties foods remain sellable and1051 proven building SKUs have explicit bulk prices. Every loaded item has one audit class; unknown remains NEEDS_REVIEW and gains no subsidized bulk. The supported exact proof covers15273 recipes with one active user-allowed copper automation exemption;142 handler/NBT/dynamic recipes remain explicitly outside its scope. No gameplay/player/crash acceptance is claimed.


## Shipping container, exchange and approved R2 integration —2026-10-20 source handoff

User direction replaces scheduled Shipping Box sales with the exact primary action «Забрать товар», keeps the actual inventory, removes the per-item right-column list/contextual item admission label, and adds a second exchange/withdrawal tab. JEMEconomy retains shipping_box registry identity, authenticated Owner, Instance,54 input+9 payout slots, existing saved stacks and transaction journal. MarketRuntime now refreshes dirty/revision previews and recovers only already-started transactions. Its cycle/demand refresh does not initiate sales. No generic hopper/pipe inventory authority is added.

ShippingBoxMenu.clickMenuButton accepts a sale only from the current valid owner's container on the sale tab. ShippingBoxBlockEntity.sellAndCollect first collects previously issued payout where inventory capacity permits, takes a fresh bounded quote, runs the existing prepared→funds reserved→input consumed→payout issued→committed phases, then collects the resulting physical Coins. Quote reads current CurrencyCore policies, exact rational price, material-family normalized quotas, ordinary account/global allowance and actual NPC reserve. It uses a bounded native-stack binary search, never a full registry scan per frame. The existing rational sale owner reserves the full payout/fraction backing into MARKET_ESCROW; native physical issuance still owns coin lots/provenance. Legacy pending transactions without rational price fields retain their old NPC source route. Zero payout consumes no goods. Fingerprint mismatch or recovery disagreement quarantines the existing journal instead of inventing a replacement payment.

The sale tab keeps real native slots and player inventory. Its right column shows one current total, ordinary account/global allowance and one localized result; it contains no repeated per-item rows. ShippingBoxLayout supplies shared screen/menu inventory geometry. All balances remain long and use native word-split menu synchronization rather than truncating high wallet amounts. Pending inventory transactions gate slot gestures/shift transfer, and tab1 hides and rejects interaction with container-owned input/payout slots. Remaining market duration fields remain native protocol values; displayed time, wherever present, uses the shared Minecraft-day convention.

The exchange tab is the same real owner container. CurrencyContainerAccess.permitsCurrencyAction requires tab1, current container identity and stillValid; EconomyNetwork also checks the dedicated full-market authority, canonical authenticated principal and matching containerId. Existing EconomyApi.exchangePhysical and withdraw perform money/coin operations. The UI selects among the five native denominations and checks exact divisibility for display; the server revalidates the actual operation. Withdrawal sends a positive requested amount bounded by the real wallet; it does not restore or mint money. Request UUIDs and result packets bind the acknowledged container; the screen has one pending request and ignores replies for another container. Existing personal inventory capacity and physical provenance rules remain the financial owner's responsibility.

The user approved the R2 Blockbench/Aseprite body shown in `Docs/art/shipping-station-2026-10-20/model/README.md`. Runtime body JSON/atlas and12 native display anchors now have source-owned integration: ShippingBoxRenderer renders4diamonds,4fish (cod/salmon) and4oak logs with actual native ItemRenderer FIXED models/pack overrides. They are decoration, not slots, synchronized stock, additional drops or an invented inventory. Horizontal facing rotates body and anchors together. ShippingStationShape derives outline/collision from the same axis-aligned body cuboids for each horizontal facing; a rotated body cuboid is rejected rather than silently misrepresented. The approved model's native item transforms/scale compensation are implementation details, not proof of an in-game match. ShippingBoxBlock keeps owner placement/use/break checks and preserved-stack drops; no duplicate contents are emitted for the decorative goods.

Preserved contracts: native merchant offers/restocking/slots, accepted Market artwork, CurrencyCore's sole ledger/prices, existing shipping identity/ownership/journal/real contents. Deliberate user changes: explicit immediate sale, clean total/result column, exchange/withdrawal tab and approved R2 weighing-station presentation. Final matching production artifacts, ordinary runtime sync, native generation publication/application, actual game geometry/GUI scale and player acceptance remain pending the root delivery receipt. No tests or game launch was performed for this documentation. The separately authorized future original migration/deployment is not asserted as executed.


Current evidence capture uses the existing opt-in `jem.economy.export` server startup path, schema2. It hashes current installed server JARs and records actual registered Item/Block classes, known capabilities, food properties, default stacks, crafting remainders, block entity registrations, block tags/redstone/loot tables, actual loaded recipe serializers/expanded ingredient alternatives/winning merged JSON, and exact Create6.0.8 rollable/fluid outputs, heat and duration. Market listings are captured after the actual shared admission owner, including exact full-order prices. Native provider factories retain explicit unsampled/world-dependent gaps; the old `NativeMerchantContracts` automated fixture projection is not called. The exporter does not craft, roll random outputs, spawn entities into a level or run tests.

The current exact expected-output authoring and its limitations are documented in [shipping-economy-current-audit.md](shipping-economy-current-audit.md). `shipping-monetary-potential-certificate.json` contains the accepted Fraction potentials and per-recipe inequalities. The original minimum-cost/direct-output report is explanatory only; it is not the arbitrary-chain proof.

## Current shipping and market correction - 2026-10-21

This supersedes the historical immediate-wallet/auto-collection and button wording above. New shipping sales use the existing funded physical issuance journal, leave stamped Coins in the nine "Оплата" slots, and show "Продать" or "Забрать оплату" according to actual output contents. Existing LedgerOnly operations retain their original replay route. Goods are never consumed for a zero quote or an output-capacity refusal. CurrencyCore remains the only owner of rational claims, escrow, supply and provenance.

The client menu uses a native SimpleContainer snapshot, not the client-world block entity with server-only visitor validity. Server properties are snapshotted once per tick; broadcasting refreshes the bounded quote. Native slots remain the source of goods and payment. Baked controls are removed from editable Aseprite backgrounds; actual buttons use the correct atlas rows, independent press state and fitted labels.

The user explicitly forbids ordinary closing until payment is collected. Escape and inventory close are blocked while payment or an unfinished journal exists. The current seller account/visitor UUID persists; forced disconnect/death or close does not release paid output. The same authenticated seller's session automatically resumes only near the loaded box, alive, with their default inventory menu. All manual acquisition and all player breaking are denied while payment remains. Other inventory screens are not replaced, and no teleport or financial compensation is invented. Boxes are explosion-proof and nonflammable, deny entity destruction, and retain piston protection. This is not protection from administrator world editing.

Market search is debounced250ms with one request in flight and queued latest text, cached stock/category classification and wrapped labels. Retry retains the catalog. Rows lacking both an available purchase and funded sale are omitted before pagination. Item-card click and R reuse the existing optional JEI adapter. Native music discs are buy-only for1Arcane (10000Copper).

All101 fish in installed Tide2.1.1-hotfix native definitions are sale commodities. Codex-selected buyback per fish is common1, uncommon3, rare8, very_rare20, legendary50Copper; existing purchase eligibility is preserved and purchase prices cannot undercut buyback. Accepted native NBT is only finite positive Double FishLength and Boolean-byte IsBucketable; size grants no premium. Dedicated-server built-in fishing data changes only native selection_weight by x0.9/x1.1/x1.25/x1.5/x1.75 respectively. These are relative conditional weights, not absolute catch probabilities. Rods, habitats, conditions and integrated singleplayer remain native. The modified native resource data retain MPL2.0 attribution.

Production compilation is recorded in the release receipt. No automated tests or gameplay launch, visual/runtime smoothness, forced-disconnect recovery, fishing probability sample or player acceptance is claimed.

## Native villager minimum payout, 2026-10-26

The latest user correction retains small positively priced villager sales and sets a minimum of one Copper for a complete native trade. NativeTradePricing.salePrice applies that minimum to a positive exact amount below one Copper. Both the server quote and EconomyApi MerchantContainer settlement use this same amount before the existing fractional-claim and funded NPC_MARKET journal. The native list therefore receives a real nonzero payment, not a cosmetic zero-price replacement or error text. Larger prices, unknown/prohibited admission, native stock/NBT and restocking remain intact. Market and Shipping Box retain their original rational prices and existing fractional claims are preserved. Shader settings are outside this change

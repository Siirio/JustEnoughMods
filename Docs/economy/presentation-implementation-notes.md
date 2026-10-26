# Economy monetary presentation — 2026-10-16

## Точный выбор монет в Shipping Box — 2026-10-26

По новому запросу пользователя вкладка физического обмена оставляет одно редактируемое исходное количество и автоматически показывает целевое количество. При смене исходного номинала поле заполняется доступным количеством из обычных 36 ячеек инвентаря. Снимок с сервера не затирает меньший ввод до следующей смены номинала или успешного обмена. Обмен списывает только полные группы выбранного курса, остаток остаётся исходными монетами. Расчёт справа использует отдельный выбранный номинал и число монет: "Внести" изымает ровно их, "Снять" выдаёт ровно их после списания точной стоимости из Wallet. Получатель использует то же видимое количество для цифрового перевода

Снимок сервера отдельно передаёт все доступные монеты для внесения и монеты в 36 обычных ячейках для физического размена. Поэтому монеты во второй руке не включаются в максимум операции, которая не может их списать. Сервер остаётся владельцем происхождения монет, ledger reservation, проверки баланса и вместимости. Сетевой контракт Economy повышен вместе с форматом снимка. Новых номиналов и курсов нет

Статус: исходники изменены, `in testing`. Вкладка сохраняет существующую 368×272 основу и native Coin item models. Фактический вид на GUI scale, работа с запечёнными элементами фона и player acceptance не подтверждены запуском клиента

The user requested actual Coin icons and exact automatic denominations throughout
existing monetary interfaces. The implementation retains native merchant/recruit
screens and the current SMP layout, palette, server authority and payment owners.
No currency textures or parallel balance system were introduced.

## Implemented source coverage

- `CoinAmounts` decomposes nonnegative copper-unit longs into the existing five
  denominations; zero is explicitly Copper 0. `CoinPriceRenderer` draws registered
  Economy item models and every denomination/count. Limited width scales the
  complete group rather than dropping the low-value remainder. Prices are never
  rounded. Rendering is client-only and creates no transferable money.
- Market cards show SELL down and BUY up rows, separate real prices and existing
  transaction buttons. The wheel reaches every row in the bounded server page,
  including rows beyond the visible card area; page/query/category changes reset
  local scrolling. Hover identifies the direction; server snapshots remain
  the only source of prices, availability and revision.
- Wallet balance, physical cash value and remaining account/global NPC budgets
  use the shared renderer. Physical coin inventory and exchange retain actual
  registered item models/counts. Numeric amount input still accepts copper units,
  as the transaction protocol does; it is not a rounded display conversion.
- Shipping-box payout preview and account/global budgets use Coin groups.
- Native merchant list currency cells use authoritative `NativeTradeQuote`
  projections for emerald cost A, cost B and payout legs; native goods/NBT,
  buttons, stock arrows, scroll and geometry remain. A selected exact amount
  appears beneath the slots. Monetary hover shows an expanded Coin group. The
  selected synthetic wallet-credit result cell uses the quoted denominations,
  while server result-slot guards retain authority over actual delivery.
- SMP shop prices, settlement treasury, projected/unpaid payroll, workforce wage
  and defeated-recruit buyback costs use the same renderer. Recruit details show
  hiring/wage fields when provided by the canonical snapshot. Settlement
  development shows the authoritative per-added-block expansion rate.
- Settlement founding uses server `FoundingCost`. Native recruit hiring uses a
  dedicated server quote for hire price and seven-day wage, clears the client
  quote on closing/logout, and replaces only the native currency presentation.
  The quote never authorizes hiring and never drives deductions.
- Native Noble recruitment retains its list, uses and village/cap requirements;
  server-quoted per-type hire costs replace native currency cells. The native Hire
  button exposes the seven-day wage as a Coin-formatted tooltip. Its currency
  affordability gate reads the server's quoted spendable balance and exact cost,
  while financial authority remains in the native server hiring adapter.

## Verified evidence

The standalone Java 17 `CoinAmountsContractTest` compiled and passed on
2026-10-16. It exercises zero, denomination boundaries, 8/20/300/1500/12345,
`Long.MAX_VALUE`, exact checked reconstruction, descending order and negative
rejection. Exact native merchant and Recruits 1.15.2 screen bytecode was inspected
to identify the currency drawing calls rather than guessing a new UI API.

Production builds, dedicated-server checks and publication are integrated and
reported by the task owner; this note does not claim they have passed.

## Remaining visual and API boundaries

No in-game screenshot has been inspected for this implementation. In particular,
GUI scales, tooltip layering, native Mixin application and recruitment quote
arrival ordering need an actual client view. Native merchant payout cells and
the native recruit button currency cell are narrow: all denominations remain
exact, but large mixed-denomination groups scale down there; the selected full
price/expanded tooltip or explicit full hire-price row provides readable detail.
Visual acceptance has not been claimed.

`CoinFormatter.format` and `formatAll` now compose `iconAmount` groups from the
`jem_economy:coins` bitmap font. Its glyphs reference the five existing Coin face
textures, and counts explicitly return to the native Minecraft font. This also
carries actual Coin icons through chat, action-bar and native Component tooltips
without relying on emoji or placeholder currency symbols. Source inspection
confirmed the integration; in-game font loading and appearance remain unverified. The existing map territory editor exposes a per-block
expansion rate; a full selected-polygon server quote is not supplied by that
interface. Other third-party monetary screens outside the inspected merchant,
RecruitHireScreen and NobleTradeScreen adapters must be traced before claiming
every installed-mod UI is covered.


## Compact Coin tooltips — 2026-10-21

User request: remove the excessive empty tooltip width. The shared CurrencyCore
CoinPriceRenderer previously forced at least180GUI pixels even for one Copper.
It now measures the widest rendered Coin row and wrapped reason text, excludes
the final inter-cell gap, and caps wrapping at240GUI pixels or available width.
All denominations, count digits, reason lines and screen-edge placement remain.
CurrencyCore1.0.3 is embedded by the refreshed Economy artifact so Forge selects
the new renderer over older compatible nested copies. Build completed; game
appearance/player acceptance remains in testing. No pricing or ledger behavior
changed. Leather remains buy12Copper/sell3Copper under the user's final request.

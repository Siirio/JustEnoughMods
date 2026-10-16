# Native merchant implementation notes — 2026-10-16

## Files owned by merchant implementation

- `merchant/NativeItemPricing.java`: exact native merchandise policy, unchanged
  canonical commodity policy precedence, NBT enchantment pricing and safe buyback.
- `merchant/ProfessionTradePools.java`: weighted per-profession/per-level additions,
  persisted individual choices, native serialized offers and paired reverse rows.
- `merchant/NativeTradeCatalog.java`: opt-in loaded native provider pool audit API.
- `merchant/NativePriceMath.java`: checked production price ratio/floor/discount arithmetic.
- `merchant/NativeTradePricing.java`: native sale quote path and final discount floor.
- `merchant/NativeTradeSettlement.java`: native container-sale entry to the existing
  EconomyApi financial owner; no new ledger or currency manager.
- `merchant/CoinMerchantRuntime.java`, `merchant/NativeTradeSession.java`: ordinary
  interaction/open-menu unlock refresh; Buyer remains separate.
- `data/jem_economy/economy/native_item_prices.json`: native merchandise base prices.
- `data/jem_economy/economy/profession_pools.json`: additions, prices, weights and stock.
- `src/test/python/test_native_merchandise.py`: deterministic production-data checks.
- `src/test/java/com/siirio/jemeconomy/merchant/NativePriceMathTest.java`: standalone
  execution of the exact arithmetic used by production quotes.
- `Docs/economy/merchant-coverage.md`, `native-trading-design.md`: owning contracts.

The root integration owns EconomyApi's explicit native-container-sale resolver,
loaded registry/recipe export, general Market policies, final CSV exports,
client rendering, Atlas records, affected builds, runtime, synchronization and
release publication. This worker did not build, install, start a runtime, commit
or push. No tests were added to ordinary production startup.

## Verification performed by merchant worker

`python Mods/JEMEconomy/src/test/python/test_native_merchandise.py`

Result: **6 tests passed**. The standalone Java production arithmetic test also
passed **134,414 assertions**, including discounts 0–100%, output batches 1–64,
ceiling rounding, strictly positive prices, maximum-demand buyback protection and
checked overflow/invalid input rejection. It was compiled with isolated `javac`
into the OS temporary directory and executed with `java`, without building or
installing a production artifact. The Python suite validates actual production JSON data and several
batch conversion bounds; it is not a Forge runtime or full-pack recipe proof.
Exact mapped Forge 47.4.10 class signatures and VillagerTrades bytecode were
inspected to establish real native constructors, offer getters, provider tables,
NBT enchantment access, rarity and treasure classification. Native item allowlist
was derived from real VillagerTrades Items/Blocks references, plus native generated
filled maps, enchanted books and suspicious stew. Modded specialty IDs were matched
against exact Atlas records and additionally require live Forge registry presence.

## Explicit limits for integration report

- No runtime/player acceptance is claimed here. Root integration must verify
  compile, loaded item existence, original provider coverage, persistence,
  stock/restock, price floors and transaction paths.
- Weighted choices vary by entity random source and are persisted. This does not
  promise every pair of villagers differs at every level; common choices have
  weight four and uncommon choices weight one.
- Native provider audit records randomized NBT samples, not all possible enchant,
  potion, map or biome variants. World-dependent map and unknown optional provider
  callbacks remain documented pool entries without executing unsafe audit effects.
- Unknown modded native goods remain visible/unavailable rather than receiving an
  invented price or bypassing a boss/World Tier gate.
- Recipe/processing arbitrage across the complete loaded modpack is owned by root
  integration; local checks are intentionally described narrowly.

## Adversarial integration findings

The successful opt-in root runtime dump confirms all 101 initial specialty rows
resolve to loaded item IDs. Loaded recipes expose native-only recipe-resale issues
that static spread checks cannot prove safe: multi-output cushions/sheets, arrow
lining, copper sheet, native melon/pointed-dripstone processing and cooked meats.
The root owns final JSON price correction and full recipe solver; no full-pack
arbitrage pass is claimed by these local tests. Native batch price floors apply
the +1 Copper spread once per offer, so the solver must use batch quantities or
a conservative fractional per-unit lower bound instead of multiplying the
single-item floor.

A separate ledger review identified repeated same-lot allocation in existing coin
exchange candidates, where `allocated.put` could overwrite a previous reservation;
this was reported to the root financial owner. Existing bounded receipt history
can also eventually forget a starter grant. Root owns fixes and validation of
these ledger findings. Same-thread transaction safety does not independently
prove atomic recovery across money/player/NPC files after a crash during saving.

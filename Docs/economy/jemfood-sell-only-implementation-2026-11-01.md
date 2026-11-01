# JEMFood sell-only implementation

## Result

- 848 supplied rows match the live JEMFood assignment owner by exact ID, quality tier and blessing traits
- 847 rows resolve to registered Item identities and now use canonical `SELL_ONLY`
- `natures_spirit:cheese_block` resolves only as a Block and remains excluded from inventory sale
- Existing policy directions before this change were 732 `SELL_ONLY`, 94 `BUY_AND_SELL` and 22 absent or `NONE`
- No JEMFood trait, duration, native effect bridge or blessing behavior changed

## Pricing and safeguards

- Denominations remain Copper 1, Silver 10, Gold 100, Aether 1,000 and Arcane 10,000
- Final per-unit payout ceilings are 30, 200, 600 and 1,000 Copper for tiers 0 through 3
- The cap is applied by the authoritative rational server quote after demand and before funded settlement
- 723 proposal rows were reduced by exact recipe-potential, minimum-quantum or placed-food portion bounds
- 830 inventory rows use a positive exact recipe potential from the 2026-10-20 runtime certificate
- 17 previously inactive endpoints have no positive bound in that certificate and use the minimum supported nominal value `1/1,000,000 Copper`
- The current purchase-root conversion audit reports zero unsafe single-output endpoints and zero unsafe simultaneous outputs with those final values
- Bakery paired slices are bounded to one quarter of the full food because installed bytecode exposes four cuts
- Dusty Decorations pie items are bounded to one quarter, one half and three quarters of the full pie
- `minecraft:enchanted_golden_apple`, `minecraft:golden_apple` and `minecraft:rotten_flesh` remain sell-only and receive the same final-cap and recipe-safety treatment

## Accounting

- Market purchase, bulk purchase and native NPC purchase require `buyable()` and reject every listed food
- Shipping Box, Market sell and native NPC sale resolve the same `ItemPolicy` and `salePrice`
- Existing exact-count removal, rational remainder accounting, persistent receipts, market escrow and NPC funding remain unchanged
- The stale 400/10,000 Copper daily payout constants are not active because current account and global money allowance return `Long.MAX_VALUE`
- Special disc quotas and funded market availability remain unchanged

## Evidence boundary

- Production `:JEMEconomy:jar` and `reobfJar` completed successfully with Java 17
- Project rules prohibit automated tests and gameplay test scenarios, so none were created or run
- The exact recipe-potential certificate is based on runtime capture `2026-10-20T18:27:03.876130700Z`
- A new exact LP reconstruction reached an optimum but could not issue a certificate because the pre-existing fixed-income anchor conflicts with `create:crafting/curiosities/cake`
- The explanatory minimum-cost pass did not converge within its bounded 64 passes, so it is reported as supporting evidence rather than a replacement exact certificate
- Current 2026-10-31 Atlas registry evidence supplied the live Item and Block identity check
- The ordinary local Server currently contains a newer unrelated Economy artifact, so this branch did not overwrite that runtime to obtain a new capture
- No MineRENT, stable profile, release baseline or production branch was changed

## Artifacts

- Canonical policies: `Mods/JEMEconomy/src/main/resources/data/jem_economy/economy/item_policies.json`
- Reproducible importer: `Mods/JEMEconomy/tools/import_jemfood_sell_prices.py`
- Final per-row report: `Docs/economy/jemfood-sell-only-prices-2026-11-01.csv`

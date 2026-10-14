# Phase 1 Coin Merchant Coverage

Policy has exactly two outcomes after the user's correction: `BUY_AND_SELL` or `NONE`. There is no directional sale-only, buy-only, earn-only, or blocked market state.

| Provider | Phase 1 handling | Notes |
|---|---|---|
| `minecraft:villager` | Supported through Coin overlay | Existing `MerchantOffer` list, uses, stock, profession XP/restock callback and result stack remain native. Emerald-direction offers are adapted only when the traded item is `BUY_AND_SELL`. |
| `minecraft:wandering_trader` | Supported through Coin overlay | Same `AbstractVillager` adapter. Unsupported/progression outputs are `NONE`. |
| Modded entities that actually subclass `AbstractVillager` and expose ordinary `MerchantOffer` | Conditionally supported | The runtime adapter discovers them by real class inheritance, not mod name. Exact item policy still gates every offer. |
| Vinery wandering winemaker and other custom merchant entities not deriving from `AbstractVillager` | Intentionally unsupported | No verified public transaction API was established; native emerald UI is not rewritten by a guessed adapter. |
| Shop/station blocks from Bakery, Beachparty, Brewery, Farm & Charm, HerbalBrews, Vinery and similar mods | Not merchant entities | Their native crafting/service blocks remain unchanged. They do not become NPC Coin sources. |
| Recruits and Workers | Not merchant providers | JEM Settlements owns workforce registration; payroll uses `EconomyApi.payTownPayroll`, not an emerald offer. |
| Spudacious Shops 1.10.2 | Existing JEMServer adapter retained | Shop blocks, stock and ownership remain native; numeric settlement already delegates to JEM Economy escrow. |

The overlay uses Wallet settlement because a vanilla two-input offer cannot safely represent all five denominations and consuming stamped physical stacks through a native merchant would strand `PHYSICAL_OUTSTANDING`. If Wallet lacks funds, the existing reserve route may deposit valid physical cash first. No emerald item is minted, renamed, or exchanged.

Native stock counting belongs to `AbstractVillager.notifyTrade`: the Coin adapter
calls that notification once after an accepted settlement and does not separately
increment `MerchantOffer.uses`. Overnight QA reproduced the old duplicate increment
with one prepared sale (uses 0→2; Wallet 100→130; total ledger supply 100,000).
The narrow correction remains in testing pending user approval. Native generation2
replay verified sale/purchase uses +1, native XP +2, correct Coin/goods movement
and unchanged empty-input/exhausted rejection. A prepared Human Coin purchase
minted one emerald while Coin ledger supply stayed 100,000. Trading-player attribution and Human emerald-refund
interaction remain separate open findings in the overnight report; stock correction
does not certify those paths or the whole Economy module.

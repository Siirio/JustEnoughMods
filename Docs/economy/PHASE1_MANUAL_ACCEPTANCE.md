# Phase 1 Economy — Manual Acceptance

Use only a disposable dedicated-server world with matching freshly built JEM Economy and JEM Settlements client/server JARs. Do not run these checks in the stable profile or production world. There is no automated test or fixture for this feature.

| Do this | Expected |
|---|---|
| Authenticate, craft a chest surrounded by eight planks, and place `jem_economy:shipping_box`. | Placement succeeds, faces the player, binds to the authenticated account, and opens only for that account. |
| Try placement before Server Login authentication. | Placement fails with the localized login message; no unowned box remains. |
| Open the box. | Six rows (54) are input, one separated row (9) is payout, followed by player inventory; Preview and Collect controls are visible. |
| Insert canonical wheat and hover it. | It enters an input slot and its tooltip shows both the current buy/sell market policy in Coin denominations. |
| Try emerald, an End Remastered eye, a coin, a damaged item, an enchanted item, a filled/NBT-bearing container, or an unknown modded item. | Manual insertion and shift-click into input are rejected. Policy is `NONE`; the source stack remains untouched. |
| Put approved goods in the box, press Preview Sale, then change the Minecraft day. | Preview is informational. Final processing re-quotes the active demand and remaining caps at settlement. |
| Reach overworld 06:00 (`dayTime % 24000 == 1000`). | One eligible sale runs for the current monotonic cycle. Accepted quantities are consumed once and provenance-backed physical Coins appear only in payout slots. |
| Collect payout, close/reopen, and wait during the same market cycle. | Collection never triggers another debit or issue; the box does not pay the same cycle twice. |
| Fill all nine payout slots, then reach 06:00. | Status says payout row is full; sellable inputs remain intact and no loose items or Coins spawn. |
| Unload a box across 06:00, then load it. | It may process one missed scheduled event against still-present input only. It does not force-load its chunk or accrue wall-clock sales. |
| Stop after `FUNDS_RESERVED`, `INPUT_CONSUMED`, or `PAYOUT_ISSUED` using a debugger/admin copy of disposable data, then restart. | Recovery resumes the durable phase with the same operation ID; ambiguous fingerprints quarantine instead of refunding and issuing simultaneously. |
| Place ten boxes owned by one account and fill all. | Combined Shipping Box, villager-sale and NPC-order payout remains at most 80 Copper for that account in the market day, not 80 per box. |
| Sell with two authenticated accounts. | Each has an 80-Copper cap while all accounts share the 1,500-Copper world cap and per-item world quantities. |
| Advance to a genuinely new monotonic market cycle. | Account/world/item counters reset once; two distinct demand groups become 150% and 125%. Wallet and Town Hall show the same groups. |
| Rewind `/time set` and cross an old 06:00 again. | Demand/caps do not reset and boxes do not resell an already processed cycle. |
| Set time forward to a new boundary. | At most the currently stored inventory is sold once; skipped empty days do not create accrued money. |
| Drain `NPC_MARKET`, then reach settlement. | Status says market budget exhausted, input stays, and no physical Coin lot is issued. Market balance does not reset daily. |
| Sell enough for mixed denominations. | Display and payout omit zero denominations; for example 19 is `1 Silver + 9 Copper`. Every stack has a valid lot/operation stamp. |
| Use `/coins deposit` after collecting payout, then withdraw the same value. | Valid lots redeem once, liability moves back to Wallet and a new withdrawal creates new backed lots; copied/redeemed stacks are rejected. |
| Open a vanilla villager or wandering trader. | The Coin trade overlay replaces the emerald UI. Offers whose traded item is `BUY_AND_SELL` show exact Coin value; all others show no market. |
| Buy an approved villager output. | Wallet is debited (valid held cash may first deposit), NPC Market receives the amount, item delivery occurs once, and native uses/XP/restock notification advances. |
| Sell an approved input to a villager. | Goods are consumed once, the funded NPC Market credits Wallet, shared quotas advance, and the native offer advances. No emerald is delivered. |
| Open a custom merchant that is not an `AbstractVillager`. | It is intentionally unsupported by Phase 1; no implicit emerald conversion or guessed adapter occurs. See `merchant-coverage.md`. |
| Inspect total ledger balances after shipping, villager buy/sell, physical deposit and withdrawal. | Digital accounts plus escrow plus `PHYSICAL_OUTSTANDING` remain exactly 100,000 Copper-units; any detected mismatch freezes the ledger. |

Acceptance remains `in testing` until the user performs this manual flow and approves it.

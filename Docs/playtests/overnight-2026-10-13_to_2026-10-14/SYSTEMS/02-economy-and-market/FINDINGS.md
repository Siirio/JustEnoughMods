# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0006

**HIGH — Coin merchant consumes two native stock uses for each one purchase/sale**

Status: **VERIFIED_IN_GAME_AWAITING_USER_APPROVAL** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T19:07:22.374063+00:00; latest: 2026-10-14T01:27:00.262425+00:00. Recheck: PASS for stock/accounting/rejection; no whole Economy approval.

**Mode/environment:** Actual non-op dedicated; prepared resource inputs/purchase offer

**Attempt/reproduction:** Replay PC18 thenPC20/21/23; explicit15beetroots/custom purchase NPC only scenario setup.

**Expected player experience:** One settlement increments native offer uses once, preserving daily stock and native XP/restock semantics.

**Actual behavior and impact:** Before: one actual sale use0->2. Matching Economy7df61604 through native generation2: sale2->3, XP2->4, Wallet130->160; two purchases uses0->1->2, XP0->2->4, Wallet160->100->40, bread12. Empty-input and affordable exhausted-use clicks left all state unchanged. Coin supply100000. Actor/refund defects remain open.

**Established cause / stated uncertainty:** Five whys: stock exhausts too early -> uses increment twice -> adapter increments before native notifyTrade -> native method already owns that mutation -> adapter duplicated native stock lifecycle instead of delegating it.

**Proposed owner correction and native reuse:** Remove only explicit increaseUses and retain native notifyTrade once; leave actor/refund lifecycle separate.

**Acceptance:** One accepted trade adds one stock use, one native XP callback and one native Forge trade event; rejected trades add none. Current profession trade XP is absent and must not be implied by that native event.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:villager`, `minecraft:wandering_trader`

**Source/evidence links:**

- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java:100](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java)
- [build/overnight-qa/AbstractVillager-bytecode.txt](../../../../../build/overnight-qa/AbstractVillager-bytecode.txt)
- [EVIDENCE/logs/merchant-native-before-after.txt](../../EVIDENCE/logs/merchant-native-before-after.txt)
- [EVIDENCE/traces/merchant-wallet-before-after.json](../../EVIDENCE/traces/merchant-wallet-before-after.json)
- [EVIDENCE/screenshots/actual/049-actual-wallet-after-prepared-sale.png](../../EVIDENCE/screenshots/actual/049-actual-wallet-after-prepared-sale.png)
- [EVIDENCE/logs/stock-fix-native-replay.txt](../../EVIDENCE/logs/stock-fix-native-replay.txt)
- [EVIDENCE/traces/stock-fix-native-commit.json](../../EVIDENCE/traces/stock-fix-native-commit.json)
- Canonical lookup for `minecraft:villager`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wandering_trader`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wandering_trader`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wandering_trader`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0021

**MEDIUM — NPC payout caps in current code contradict owning Economy documentation**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:41:41.781974+00:00; latest: 2026-10-13T20:30:59.067219+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Compare GLOBAL_DAILY_PAYOUT/ACCOUNT_DAILY_PAYOUT with owning docs and runtime tooltip/market snapshot after authentication.

**Expected player experience:** Owning docs, GUI/help and runtime implement one explicit approved economic budget.

**Actual behavior and impact:** Docs/DEVELOPMENT.md and manual acceptance specify 1500 world/80 account Copper per market day. Current MarketSavedData constants are 4000/240 and actual remaining/allowance methods consume those constants. This is a demonstrated contract/documentation mismatch; latest intended product balance must be resolved from approved direction before changing code.

**Established cause / stated uncertainty:** Source/domain documentation was not synchronized with the newer cap implementation. Whether the numbers changed intentionally is not established by tonight’s evidence.

**Proposed owner correction and native reuse:** Resolve latest approved cap values; update one canonical rule, all projections and acceptance together. Preserve fixed-supply/funded-market semantics rather than guessing which number should win.

**Acceptance:** Actual sale ceiling, Wallet/Town bulletin, documentation and exact acceptance agree on both world/account caps; rewind/replay cannot reset them.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/market/MarketSavedData.java:18](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/market/MarketSavedData.java)
- [Docs/DEVELOPMENT.md](../../../../DEVELOPMENT.md)
- [Docs/economy/PHASE1_MANUAL_ACCEPTANCE.md](../../../../economy/PHASE1_MANUAL_ACCEPTANCE.md)

## JEMQA-20261014-0026

**HIGH — Coin merchant native callback runs with no actor or the previous customer**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:56:30.332918+00:00; latest: 2026-10-13T20:30:59.188722+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Fresh villager: complete one Coin trade and inspect callback actor/criteria. With two real authenticated accounts, open the same merchant and alternate trades; compare payer to native event actor.

**Expected player experience:** Native villager criteria, race and installed native trade hooks belong to the authenticated player who paid.

**Actual behavior and impact:** No tradingPlayer is set when the Coin menu opens. execute calls merchant.notifyTrade before merchant.setTradingPlayer(player). Exact native bytecode reads tradingPlayer to trigger vanilla trade criteria and construct TradeWithVillagerEvent. First trade has null actor; another player opening the same merchant can cause the callback to use the previous customer. Payment still uses current player. Two-player gameplay was not simulated. Native Villager.rewardTradeXp also records lastTradedPlayer from that actor. Development-only VillagerTalking trade chatter is another consumer; it is not installed gameplay evidence tonight. Profession trade XP remains missing.

**Established cause / stated uncertainty:** Five whys: callback actor is wrong -> native reads merchant tradingPlayer -> adapter sets it only after callback -> screen opening never establishes the native trade lifecycle -> adapter reused result notification without its required actor/occupancy contract.

**Proposed owner correction and native reuse:** Establish validated native trading context when opening/executing and honor exclusive occupancy/lifecycle cleanup; do not reconstruct actor from a client ID. Coordinate with the duplicated stock-use fix.

**Acceptance:** First and subsequent trades attribute vanilla/native hooks to actual payer exactly once; concurrent/stale menus cannot attribute another customer or leave merchant permanently occupied.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:villager`, `minecraft:wandering_trader`

**Source/evidence links:**

- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantRuntime.java:26](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantRuntime.java)
- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java:106](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java)
- [build/overnight-qa/AbstractVillager-bytecode.txt](../../../../../build/overnight-qa/AbstractVillager-bytecode.txt)
- Canonical lookup for `minecraft:villager`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wandering_trader`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wandering_trader`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:wandering_trader`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0027

**HIGH — Human race refunds emeralds from a Coin transaction that spent no emeralds**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T19:56:30.341002+00:00; latest: 2026-10-14T01:27:00.278337+00:00. Recheck: FAIL; no fix applied.

**Mode/environment:** Actual prepared Coin purchase, not a measured natural exploitation chain

**Attempt/reproduction:** Human account: two supported native villager purchases with native costA >=10; inspect Coin debit and emerald inventory. Use legitimate coin funding, not creative proof of affordability. ActualPC22preparednativeoffer, humanchoseninUI, non-op.

**Expected player experience:** A race discount adjusts actual paid currency and does not mint a second currency from a nominal native offer.

**Actual behavior and impact:** CoinMerchantMenu pays through JEM Wallet/cash but retains the native emerald-priced MerchantOffer and calls notifyTrade. Human RaceTraitRuntime.trade refunds costA minus ceil(costA*0.90) as new emerald ItemStack. On a callback with a Human actor, a native cost of 20 emeralds grants 2 emeralds despite the purchase spending Coins. The callback-actor bug affects which player receives this; actual gameplay minting is not replayed. Actual prepared second Coin purchase minted1emerald: original native cost12, Human ceil(12*0.9)=11 refund1. Wallet paid60Coins; noemeraldspent. First purchase no refund because actor was assigned after native callback. PC22 failed; unwanted emerald removed after evidence.

**Established cause / stated uncertainty:** Five whys: emeralds appear without emerald payment -> race handler creates refund from native offer cost -> callback carries original emerald contract even after Coin conversion -> race integration assumes native currency was consumed -> two currency owners were connected without a settled-payment contract.

**Proposed owner correction and native reuse:** Apply Human discount to the actual validated Coin quote/settlement through the Economy owner, and retain emerald refund only for a proved native emerald transaction where appropriate. Preserve fixed Coin supply, native offer price/restock and approved singleplayer boundary.

**Acceptance:** Discount matches actual currency/value; no emerald is created by a Coin purchase, no other customer receives the perk, and native/coin state settles once.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:emerald`, `minecraft:villager`

**Source/evidence links:**

- [Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java:124](../../../../../Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java)
- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java:91](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java)
- [build/overnight-qa/AbstractVillager-bytecode.txt](../../../../../build/overnight-qa/AbstractVillager-bytecode.txt)
- [EVIDENCE/logs/stock-fix-native-replay.txt](../../EVIDENCE/logs/stock-fix-native-replay.txt)
- Canonical lookup for `minecraft:emerald`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:emerald`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:emerald`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:villager`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

## JEMQA-20261014-0038

**MEDIUM — Coin overlay blocks a natural novice Farmer pitchfork offer; exact ordinary-tool scope needs review**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-14T00:15:21.093941+00:00; latest: 2026-10-14T00:15:21.093952+00:00. Recheck: NOT_FIXED.

**Mode/environment:** Actual natural native offer; no purchase attempted

**Attempt/reproduction:** Open natural Farmer Coin menu, hover second offer and read unchanged native Offers.

**Expected player experience:** Ordinary native non-boss tool acquisition remains coherent with deliberate progression protection.

**Actual behavior and impact:** Natural novice Farmer offers12emeralds->farm_and_charm:pitchfork, max2, but JEM overlay says no market and cannot buy it. Native result has Damage0NBT; strict canonical-stack gate rejects any nonempty tag independently of ID policy. No automatic policy relaxation justified.

**Established cause / stated uncertainty:** Coin adapter accepts only BUY_AND_SELL canonical output; ItemPolicyRegistry.canonical rejects nonemptyNBT, including native undamaged Damage0. Exact pitchfork category/policy and safe recipe acquisition require review before inclusion.

**Proposed owner correction and native reuse:** Review this exact native ordinary tool and comparable outputs against owning progression contract; if approved, distinguish harmless zero-damage canonical metadata from custom/enchant/unsafe stacks in owner policy, not broad allowlist bypass.

**Acceptance:** Explicitly documented reason for pitchfork restriction or legitimate approved acquisition, without buying protected boss materials or arbitrary NBT.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `farm_and_charm:pitchfork`

**Source/evidence links:**

- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/policy/ItemPolicyRegistry.java:40](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/policy/ItemPolicyRegistry.java)
- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java:61](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java)
- [EVIDENCE/logs/merchant-native-before-after.txt](../../EVIDENCE/logs/merchant-native-before-after.txt)
- Canonical lookup for `farm_and_charm:pitchfork`: [Catalog/registries/items/farm_and_charm.json](../../../../../Catalog/registries/items/farm_and_charm.json) (registry identity must be respected).
- Canonical lookup for `farm_and_charm:pitchfork`: [Catalog/registries/blocks/farm_and_charm.json](../../../../../Catalog/registries/blocks/farm_and_charm.json) (registry identity must be respected).
- Canonical lookup for `farm_and_charm:pitchfork`: [Catalog/registries/entities/farm_and_charm.json](../../../../../Catalog/registries/entities/farm_and_charm.json) (registry identity must be respected).

## JEMQA-20261014-0039

**HIGH — Coin merchant hides direction and shows conflicting no-market tooltip plus overflowing instruction**

Status: **OPEN** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-14T00:15:21.115781+00:00; latest: 2026-10-14T00:15:21.115792+00:00. Recheck: NOT_FIXED.

**Mode/environment:** Actual non-op merchant UI; explicit admin resource setup for transaction only

**Attempt/reproduction:** Open natural Farmer menu and hover approved beetroot offer, then one prepared sale.

**Expected player experience:** Before committing a trade, player sees exact input->output, direction, count, price and wallet status.

**Actual behavior and impact:** One beetroot icon says3silver but not whether player buys or sells15. Its generic tooltip also says no market although actual sale succeeds. Confirmation instruction spills out of176pxframe into JEI. Click commits immediately; no explicit trade row or wallet value.

**Established cause / stated uncertainty:** Display icon is renamed via setHoverName, addingNBT. Generic policy tooltip rejects that synthetic tagged display stack, while execute uses original untagged offer. CoinMerchantScreen renders long instruction unwrapped and has no direction projection.

**Proposed owner correction and native reuse:** Use semantic native input->output rows with explicit Sell15beetroot->30Copper, live authoritative quote/affordance and wallet; keep synthetic presentation metadata separate from original policy context; wrap instructions. Preserve current native slot execution and ledger authority.

**Acceptance:** Offer reads unambiguously and does not contradict supported policy; exact native sale/rejection state visible at actualGUI4.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `minecraft:beetroot`

**Source/evidence links:**

- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java:50](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantMenu.java)
- [Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/client/CoinMerchantScreen.java:19](../../../../../Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/client/CoinMerchantScreen.java)
- [EVIDENCE/screenshots/actual/047-native-farmer-beetroot-sale-offer.png](../../EVIDENCE/screenshots/actual/047-native-farmer-beetroot-sale-offer.png)
- [EVIDENCE/screenshots/actual/048-one-sale-consumed-fifteen-beetroots.png](../../EVIDENCE/screenshots/actual/048-one-sale-consumed-fifteen-beetroots.png)
- Canonical lookup for `minecraft:beetroot`: [Catalog/registries/items/minecraft.json](../../../../../Catalog/registries/items/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:beetroot`: [Catalog/registries/blocks/minecraft.json](../../../../../Catalog/registries/blocks/minecraft.json) (registry identity must be respected).
- Canonical lookup for `minecraft:beetroot`: [Catalog/registries/entities/minecraft.json](../../../../../Catalog/registries/entities/minecraft.json) (registry identity must be respected).

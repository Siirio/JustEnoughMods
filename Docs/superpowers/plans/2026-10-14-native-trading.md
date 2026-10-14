# Native Coin Trading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking. Reviewed and approved2026-10-14; user selected native inline execution and immediate push. Source implemented; compilation/runtime/visual verification remain unperformed under the project execution prohibition.

**Goal:** Use native villager trading for five Coin denominations and give the Buyer real native purchase offers without chat.

**Architecture:** Native MerchantMenu, MerchantContainer, MerchantResultSlot and MerchantScreen retain their lifecycle. Economy owns quotes and settlement; a narrow dedicated-server adapter replaces only monetary legs, while the client draws synchronized Coin price icons. Native acquisition and the system-market policy must not be conflated.

**Tech Stack:** Java17, Forge1.20.1/47.4.10, JEMEconomy, JEMIdentity, JEMRoles, existing Economy ledger/physical-lot services.

**Spec:** [Approved design](../../economy/native-trading-design.md). [Xaero plan](2026-10-14-xaero-native-presentation.md) can proceed independently.

## Global Constraints

- Original MerchantScreen geometry, offer order/count, two real input slots, scrolling, native restock and callbacks remain the owner.
- Copper1/Silver10/Gold100/Aether1000/Arcane10000; original item assets; price icons/counts visible in the row, not only a tooltip.
- Wallet and physical-lot settlement remain server authoritative; display coins never become transferable items.
- Buyer interactions, errors and market display emit no chat; no remote selling.
- Dedicated-server-only gameplay; integrated singleplayer remains native.
- Preserve fixed supply, reviewed market eligibility and existing authoritative prices. Copper3/Gold12 is a display example, not a new gear price or emerald course.
- No automated tests/fixtures. No build, game launch, linter, formatter or static analysis without the exact current-turn request. No stable/runtime/baseline mutation or release without authorization.

## Review Focus

1. NBT/modded and secondary-input offers must remain present with their original item semantics.
2. Shift-click, cursor pickup, stale packets and disconnects must not duplicate delivery or money.
3. Human discounts must apply once in the paid currency, without emerald minting.
4. Buyer demand/day changes, limited allowance and exhausted reserve must reject whole trades safely.
5. Large prices and multiple denominations must remain readable without producing unbacked physical coins.

### Task 1: Native lifecycle seam and quote contract

**Files:**
- Modify `Mods/JEMEconomy/src/main/java/com/siirio/jemeconomy/merchant/CoinMerchantRuntime.java` — replace custom-screen interception with native-session attachment.
- Create `merchant/NativeTradeQuote.java` and `merchant/NativeTradeSession.java` under the same Java package root — immutable monetary projection and per-native-menu operation context.
- Modify `network/EconomyNetwork.java` — bounded quote/state synchronization.
- Modify `client/EconomyClient.java` — synchronized presentation context, not another offer owner.
- Add only proven necessary native-container/result/screen Mixins and resource/build registration to JEMEconomy after seam inspection; the module currently has no Mixin config.

**Proposed data contract:**
```java
public record NativeTradeQuote(int offerIndex, Direction direction,
        long copper, boolean available, String reasonKey) {
    public enum Direction { PLAYER_BUYS, PLAYER_SELLS, ITEM_BARTER }
}
// Packet/session identity: native containerId + merchant entityId + revision.
// Client may select an offer; it cannot supply accepted price or payment account.
```

- [x] Run Atlas lookup and before-impact scan for villagers, wandering traders, Economy, Roles callbacks and all priced item-policy owners.
- [x] Inspect exact mapped native `MerchantMenu.clicked/quickMoveStack`, `MerchantContainer.updateSellItem`, `MerchantResultSlot.remove/onTake`, packet selection and `notifyTrade` bytecode. Record where validation precedes removal, where output is transferred and where uses/XP fire.
- [x] Prefer public API/Forge events; if these cannot express pre-take monetary validation, record why and use narrowly gated Mixins. Do not clone vanilla classes or guess injection descriptors.
- [x] Resolve the exact native MerchantScreen extension Taste Engine pair from approved vanilla presentation and installed assets before price-layout code.
- [x] Preserve every original offer and derive a quote from existing Economy policy/prices when available. Apply current native demand adjustments consistently, retaining secondary items and NBT. Item-only barter remains native.
- [x] Inventory all native offers lacking an authoritative Coin quote, including gear. Keep them visible, explicitly unavailable with a reason; do not invent an exchange rate, silently convert to free, re-enable emerald currency or alter global market gates. Record the concrete missing price/eligibility decision for the user before claiming complete currency coverage.
- [x] Bind session to the actual native menu/merchant/account; synchronize only bounded quote/status projections with container/merchant/revision identity. Reject invalid indexes, negative/overflow sums, wrong menu and stale revision. Clear client context on close/logout.
- [x] Stop opening `CoinMerchantProvider`/the custom18-slot menu for native villagers; do not erase registered IDs until registry/network compatibility is reviewed.

### Task 2: Exact-once funded settlement and native callbacks

**Files:** `api/EconomyApi.java`, new `merchant/NativeTradeSettlement.java`, `merchant/NativeTradeSession.java`, proven native result/container Mixins, `Mods/JEMRoles/src/main/java/com/siirio/jemroles/trait/RaceTraitRuntime.java`.

**Interfaces:** `NativeTradeSettlement` consumes the authenticated player, native session, current server quote and actual trade-container inputs. It returns the existing Economy result/status plus exact consumed/delivered units; client-supplied stacks or accounts are not accepted.

- [x] Extract source-aware sale ownership from `processNpcSale` without changing Shipping Box inventory behavior. Trade-container sales validate identical item/tags and full required quantities; no second inventory scan/removal after native consumption.
- [x] Requote immediately before take: authenticated dedicated player, living/in-range merchant, exact menu/selection/revision, original offer identity/uses, item inputs, available funds/reserve/allowance and delivery destination.
- [x] Use a server-generated session operation key for the single transfer/consume/deliver sequence. Replays return recorded outcome without another output, quota commit or callback. Reuse existing backed cash redemption and funded reserve/settle/refund APIs.
- [x] Preserve native item consumption for nonmonetary legs and exactly one callback/XP/use increment after successful settlement. Denial must leave money, items and stock unchanged. Prevent every output-transfer path, including quick move and cursor pickup, from bypassing validation.
- [x] Never expose a synthetic Coin or emerald payout as a transferable sale result. Represent the sale's Wallet credit as a guarded native result action; interception must cover remove, quick move, pickup, cloning and dropping before enabling it. If the inspected native seam cannot guarantee this, stop this task and revise the written architecture rather than shipping a bypass.
- [x] Maintain a per-thread scoped paid-currency context around native callback with `try/finally` cleanup. Roles reads that explicit context for Coin trades; no soft optional reflection/default that can refund emeralds after Coin payment. Preserve its established emerald path for truly emerald-paid native singleplayer trades.
- [x] Set the native trading player before callback. Apply Human benefit in the paid currency once, not both quote adjustment and emerald refund.
- [x] Close/death/disconnect clears context and refunds only a confirmed unresolved reserve; return original input items through the native container lifecycle. Do not remove matching items from unrelated inventory slots to compensate for failed delivery.
- [x] Review the actual diff for the five failure classes above. No automated fixtures or runtime scenarios are created/run.

### Task 3: Native price rendering and Buyer offers

**Files:** native MerchantScreen Mixin/client quote context, `physical/CoinDenomination.java` and existing coin item registrations as read-only assets, `market/MarketRuntime.java`, a new `merchant/BuyerTradeOffers.java`, Economy localization files; old menu/screen/client registration only as required to cease opening them.

- [x] Draw existing Coin item icons and numeric counts directly in the native offer price area. Use descending denomination division/remainder as already established in CoinFormatter, keeping large quantities as amounts, not fake ItemStack counts. Tooltips explain balance/status but do not replace visible amounts.
- [x] Preserve native seven-row viewport, scroll/selection, result/input/inventory coordinates and native textures. Fit multi-denomination prices within the existing row using compact icon/count groups; no new merchant window. Inspect actual visual output only after the user explicitly requests a launch/check.
- [x] Build Buyer offers from reviewed sellable demand-group policies with existing Economy quotes and stock/limit/reserve state. Do not truncate to eight/eighteen. Keep Buyer identity server-tag based; do not assign normal random profession trades.
- [x] Open a native Buyer merchant session. Refresh availability after demand rollover/settlement; execute whole container-item sales through Task2's owner. Do not allow stale quotes or partial allowance to consume a full input.
- [x] Remove Buyer bulletin/error chat paths, including authentication failures. Put reasons/limits/countdown/balance in synchronized native screen status/tooltip. If `/market` remains, it opens read-only presentation with no sales and no NPC chunk load.
- [x] Preserve dedicated-only opening/payment and native singleplayer; keep compatible registered items/menu IDs as needed. Review newly exercised channels and document matching JAR requirements.

### Task 4: Canonical history and source completion

**Files:** `Docs/economy/native-trading-design.md`, `merchant-coverage.md`, relevant owning Roles documentation, `CHANGELOG.md`, applicable `Catalog/changes` and `Catalog/effective` records.

- [x] Record actual supported offers and explicitly unresolved authoritative prices, replacing obsolete18-slot coverage descriptions without falsely marking all offers available.
- [x] Update same-task Atlas change/effective records and run mandatory Atlas validation.
- [x] Record merchant replacement and Buyer chat defects as `Bug / Both Sided / concrete owner cause / in testing`; record the currency callback defect at its actual owner. Name native-session settlement and no-chat Buyer offers as the resolution mechanism; user acceptance alone may promote runtime issues to resolved.
- [x] Review task-owned source/resources/docs/Atlas diff, commit and push the intended branch. Do not stage unrelated Server configs, world saves, credentials, existing coverage-summary changes or runtime archives.
- [x] State source-only completion honestly: no new JAR, installed client/server or runtime claim without explicit build/deployment/acceptance evidence.

## Later explicit acceptance and delivery

An explicit requested manual check uses only `test jem`: inspect ordinary native trading and Buyer; select/scroll more than18 offers; buy/sell with NBT and secondary inputs; try full inventory and insufficient Wallet/reserve/limits; use shift-click and close/reopen; verify stock/XP and Human currency behavior; confirm Buyer produces no chat. This is a future human acceptance checklist, not an automated runtime scenario. A matching Economy/Roles/Server client-and-server update is required after explicit build/release authorization; source approval is not deployment authorization.

# JEM Economy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one fixed-supply Coin ledger spanning digital balances and five backed physical denominations, funded transfers, escrowed market operations and Coin-only settlement for physical Spud's Shops.

**Architecture:** Keep standalone `JEMEconomy` as the sole balance/ledger/escrow/physical-issuance owner. It registers stackable Copper/Silver/Gold/Aether/Arcane coin items symmetrically for Forge compatibility, while only the dedicated server exposes Wallet, issuance, redemption, commerce and treasury mutation. JEMServer keeps physical Spud shop discovery and UI interception, delegating settlement to `EconomyApi`. Physical coins and digital Wallet balances are two forms of the same fixed Coin supply; no Lightman's balance or parallel currency is introduced.

**Tech Stack:** Java 17, Forge 1.20.1, SavedData snapshot plus append journal records, DeferredRegister item registry, ordinary low-complexity item-model JSON, SimpleChannel, Spud's Shops 1.10.2 adapter.

**Spec:** ZIP entry `04_ECONOMICS_CODEX_PROMPT.md`.

## Phase 1 Economy Addendum — implemented 2026-10-13

The user replaced the earlier directional policy taxonomy: the executable market now permits only `BUY_AND_SELL` or `NONE`. Phase 1 adds the owned 54-input/9-payout Shipping Box, deterministic 06:00 market cycles, shared 1,500/world and 80/account NPC payout limits, two daily demand groups, direct backed container issuance, Coin villager overlay, full exact-ID audit artifacts, FTB onramp, Wallet/Town Hall bulletin and Phase 2 API contracts. Manual approval remains governed by `Docs/economy/PHASE1_MANUAL_ACCEPTANCE.md`.

## Global Constraints

- Fixed initial supply 100,000: Central 60,000, NPC Market 15,000, Operations 20,000, Contingency 5,000.
- Dedicated-server player monetary writes use the shared connection-bound authenticated principal. `online-mode=false` alone does not freeze Economy; only a missing or still-locked authentication provider fails that player's mutation closed. Economy remains disabled in integrated singleplayer because it is shared-server state, not local progression.
- Every monetary change is a funded transfer; no `setBalance`, activity minting or barter fallback. Physical coin issuance moves value from a funded balance to `PHYSICAL_OUTSTANDING`; redemption moves the same backed value out of that liability exactly once.
- Register exactly `copper_coin=1`, `silver_coin=10`, `gold_coin=100`, `aether_coin=1000`, `arcane_coin=10000`, max stack 64. Their original native-size textures/models are production assets, not crops from the concept sheet.
- Global conservation counts digital accounts, escrow and physical outstanding once: the initial total remains 100,000 Coins. Creative/operator/tampered/copied item stacks never create redeemable value.
- Preserve the existing Spud's Shops blocks, ownership, discovery, UI and physical inventory. Safely migrate every payment route to Coin-only settlement; pending settlement output is never sellable and no parallel balance exists.
- No tests or test infrastructure. Compile the production module and affected JEMServer production sources with test tasks excluded after implementation.

## Review Focus

- Conservation holds across balances, fees, every escrow phase and physical outstanding value.
- Idempotency keys make replayed grants, purchases, bids and settlements no-ops.
- Item-and-money transactions expose durable recoverable phases instead of claiming in-memory atomicity.
- Stale price/stock versions cannot charge the buyer.
- Unsafe identity freezes writes without fabricating fallback money.
- A withdraw/deposit replay, stale inventory save or copied signed stack cannot redeem or issue more than the durably backed quantity.

---

### Task 1: Scaffold `JEMEconomy`

**Files:**
- Create: `Mods/JEMEconomy/{build.gradle,gradle.properties,settings.gradle}`
- Create: `Mods/JEMEconomy/src/main/resources/META-INF/mods.toml`
- Create: `.../pack.mcmeta`
- Create: `.../java/com/siirio/jemeconomy/JemEconomy.java`

- [ ] Create a Forge-only Java-17 owner module with disabled test tasks and embedded menu support.
- [ ] Register five coin items and their model resources on both physical sides; register ledger/config/commands and economy gameplay listeners only for dedicated servers while keeping client screen/network registration multiplayer-compatible.

### Task 2: Implement Typed Accounts and the Conservation Ledger

**Files:**
- Create: `.../ledger/AccountType.java`
- Create: `.../ledger/LedgerAccount.java`
- Create: `.../ledger/LedgerEntry.java`
- Create: `.../ledger/LedgerSavedData.java`
- Create: `.../ledger/LedgerService.java`
- Create: `.../ledger/SupplyPolicy.java`
- Create: `.../api/EconomyApi.java`
- Create: `.../api/TransferRequest.java`
- Create: `.../api/TransferResult.java`

**Interfaces:**
```java
public record TransferRequest(UUID debit, UUID credit, long amount,
        String idempotencyKey, TransferReason reason, @Nullable UUID relatedId) {}

public static TransferResult transfer(MinecraftServer server, TransferRequest request);
```

- [ ] Initialize the four reserve accounts once and persist immutable configured supply, physical outstanding liability and privileged audited deltas.
- [ ] Reject zero/negative/overflowing amounts, missing accounts, unsafe principals, duplicate keys and insufficient balances before mutation.
- [ ] Commit balanced debit/credit/fee legs as one ledger entry and freeze purchases on a detected conservation mismatch rather than correcting balances.

### Task 3: Register Five Physical Coins and Durable Issuance

**Files:**
- Create: `.../registry/EconomyItems.java`
- Create: `.../physical/CoinDenomination.java`
- Create: `.../physical/CoinProvenance.java`
- Create: `.../physical/PhysicalCoinService.java`
- Create: `.../physical/PhysicalCoinSavedData.java`
- Create: `.../physical/PhysicalCoinOperation.java`
- Create: `.../physical/PhysicalCoinPhase.java`
- Create: `.../resources/assets/jem_economy/textures/item/*_coin_{front,back,rim}.png`
- Create: `.../resources/assets/jem_economy/models/item/*_coin.json`

- [ ] Register exactly five noncraftable max-stack-64 items with base values 1/10/100/1,000/10,000 and natural RU/EN names/tooltips.
- [ ] Author each 32x32 front/back/rim set anew from the approved motifs; never crop or extract the supplied concept sheet. Use a thin stepped-disc JSON model whose GUI, ground, fixed and held transforms expose a real front, back and rim.
- [ ] Persist bounded backed issuance lots, denomination quantities, redeemed quantities, operation IDs and phases; signed/tagged metadata alone is never sufficient to authorize value.
- [ ] Preflight full inventory fit, then durably move wallet value to physical outstanding and deliver all stacks once. Ambiguous recovery freezes the operation instead of issuing and refunding.
- [ ] Validate and consume selected genuine coin stacks once, then move their exact backed value from physical outstanding to an authenticated wallet or authorized treasury. Support safe consolidation/change-making without discarding provenance.

### Task 4: Implement Identity, Wallet and Funded Grants

**Files:**
- Create: `.../identity/EconomyIdentityPolicy.java`
- Create: `.../wallet/WalletService.java`
- Create: `.../wallet/StarterGrantService.java`
- Create: `.../wallet/WalletHistory.java`

- [ ] Resolve accounts only from verified principals; never create or transfer by raw display name.
- [ ] Transfer the one-time 100-Coin starter grant from Central Reserve with `starterGrant:<principal>` only when identity is verified and funds exist.
- [ ] Retain last 50 bounded history records and expose available versus escrowed totals without revealing unrelated private account details.
- [ ] Expose denomination preview, physically held valid summary, `Deposit all valid`, selected deposit and checked minimal-stack withdrawal; inventory coins remain outside digital balance until redeemed.

### Task 5: Implement Durable Escrow and Mailbox Delivery

**Files:**
- Create: `.../escrow/EscrowType.java`
- Create: `.../escrow/EscrowRecord.java`
- Create: `.../escrow/EscrowPhase.java`
- Create: `.../escrow/EscrowService.java`
- Create: `.../escrow/ItemFingerprint.java`
- Create: `.../delivery/DeliveryMailbox.java`
- Create: `.../api/EscrowRequest.java`
- Create: `.../api/EscrowResult.java`

- [ ] Persist restricted ItemStacks, owner, offer/version and phases `RESERVED_ITEMS`, `FUNDS_HELD`, `SETTLED`, `DELIVERED`, `REFUNDED`.
- [ ] Make each phase transition idempotent and ensure an item can be delivered or refunded once, never both.
- [ ] Deliver to inventory when capacity exists, otherwise retain in a secure owner mailbox; never drop paid valuables at feet.

### Task 6: Implement Listings, Buy Orders and Auctions

**Files:**
- Create: `.../market/MarketListing.java`
- Create: `.../market/BuyOrder.java`
- Create: `.../market/Auction.java`
- Create: `.../market/MarketService.java`
- Create: `.../market/MarketSavedData.java`
- Create: `.../market/MarketPriceHistory.java`

- [ ] Implement 15 active listings/account, 1..7 day durations, partial quantities, exact item fingerprints and 2% ceil fee with zero fee below 10 Coins.
- [ ] Reserve 100% buy-order budget and decrement/refund the same escrow as real items fulfill/cancel.
- [ ] Reserve auction bids, refund the superseded bidder in the same ledger operation and settle exactly once by persisted epoch.
- [ ] Compute a 14-day informational price range only from at least five settled trades across three verified buyers and two sellers after excluding linked/self/refunded cycles.

### Task 7: Implement Funded Public Orders and Reserve Budgets

**Files:**
- Create: `.../commission/PublicCommission.java`
- Create: `.../commission/CommissionService.java`
- Create: `.../commission/ReserveBudget.java`

- [ ] Fund NPC orders from NPC Market Reserve with world-wide 1,500/day and 80/account/day UTC limits.
- [ ] Fund event payouts from Operations Reserve with 500/event-day global limit and idempotent event/account keys.
- [ ] Mark exhausted orders unfunded without negative balances or automatic issuance.

### Task 8: Convert Spud Shops to Coin-Only Settlement

**Files:**
- Create: `.../compat/spud/SpudCoinSettlement.java`
- Create: `.../compat/spud/SpudOfferVersion.java`
- Create: `.../compat/spud/SpudShopLock.java`
- Modify: `Mods/JEMServer/src/main/java/com/siirio/jemserver/mixin/shops/ShopEntityMixin.java`
- Modify: `.../ShopInventoryMixin.java`
- Modify: `.../RemoteShopClientMixin.java`
- Modify: `.../smp/shops/RemoteShop.java`
- Modify: `.../smp/shops/ShopIndex.java`
- Modify: `Mods/JEMServer/src/main/resources/jem_server_shops.mixins.json`

**Interfaces:**
```java
CoinPurchaseResult purchase(ServerPlayer buyer, ShopRef shop, int expectedVersion,
        ItemFingerprint expectedItem, int quantity, long expectedPrice, UUID requestId);
```

- [ ] Preserve shop block, owner, real stock and existing protections while replacing every normal player payment path with integer Coin price metadata.
- [ ] Reject diamonds, emeralds and all other payment items in owner UI, hoppers, remote menus and bulk/automation routes with the localized Coin-only message.
- [ ] Lock shop+listing+ledger, recheck identity/owner/loaded stock/version/capacity and settle stock, buyer debit, seller credit and reserve fee through recoverable escrow.
- [ ] Offer explicit `Use coins from inventory`: validate selected backed cash, redeem only the required value under the purchase operation, combine with wallet funds only after confirmation, and return overpayment to Wallet by default. Reuse normal issuance if physical change is selected.
- [ ] Keep all native barter interaction disabled when Economy is absent or frozen; do not expose unsafe dual currency.

### Task 9: Implement Wallet and Market Screens

**Files:**
- Create: `.../network/EconomyNetwork.java`
- Create: `.../client/WalletScreen.java`
- Create: `.../client/MarketScreen.java`
- Create: `.../client/ListingEditor.java`
- Create: `.../client/TransactionHistoryScreen.java`
- Create: `.../client/EconomyClientState.java`

- [ ] Implement Available/In Escrow/Physically Held/Recent Activity, Send/Deposit/Withdraw/Market/Settings and the six market tabs from bounded server DTOs.
- [ ] Require preview and second confirmation above 5,000 Coins; display exact recipient, amount, fee, resulting balance and receipt ID.
- [ ] Preview exact denomination counts, required inventory slots, physical-loss policy, selected cash tender and wallet change without exposing lot IDs/NBT.
- [ ] Render frozen safe mode prominently and disable mutation controls without hiding market information.

### Task 10: Add Commands, Localization and Documentation

**Files:**
- Create: `.../command/EconomyCommands.java`
- Create: `.../resources/assets/jem_economy/lang/en_us.json`
- Create: `.../resources/assets/jem_economy/lang/ru_ru.json`
- Modify: `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpSidebar.java`
- Modify: `Docs/DEVELOPMENT.md`
- Modify: `Docs/smp-gui/README.md`
- Modify: `Docs/SERVER_COMMANDS.md`
- Modify: `CHANGELOG.md`

- [ ] Register `/coins`, `/coins deposit`, `/coins withdraw <amount>`, `/market`, status and OP4 supply/recovery commands; supply mint is default-off, nonce-confirmed, reasoned and capped at 1% per 30 UTC days.
- [ ] Add semantic Coin/market navigation and preserve current shop catalog behavior outside settlement.
- [ ] Document ledger ownership, all five item IDs/asset paths, denomination math, physical outstanding, issuance/redemption/consolidation/recovery, cash loss policy, safe mode, Coin-only Spud cash/wallet policy and user-supplied monetary decisions.


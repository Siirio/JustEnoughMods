# JEM Economy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans`. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one fixed-supply Coin ledger, funded transfers, escrowed market operations and Coin-only settlement for physical Spud's Shops.

**Architecture:** Create standalone `JEMEconomy` as the sole balance/ledger/escrow owner. JEMServer keeps physical Spud shop discovery and UI interception, delegating settlement to `EconomyApi`. No physical redeemable coin and no Lightman's balance are introduced.

**Tech Stack:** Java 17, Forge 1.20.1, SavedData snapshot plus append journal records, SimpleChannel, Spud's Shops 1.10.2 adapter.

**Spec:** ZIP entry `04_ECONOMICS_CODEX_PROMPT.md`.

## Global Constraints

- Fixed initial supply 100,000: Central 60,000, NPC Market 15,000, Operations 20,000, Contingency 5,000.
- Dedicated-server player monetary writes use the shared connection-bound authenticated principal. `online-mode=false` alone does not freeze Economy; only a missing or still-locked authentication provider fails that player's mutation closed. Economy remains disabled in integrated singleplayer because it is shared-server state, not local progression.
- Every monetary change is a funded transfer; no `setBalance`, activity minting, barter fallback or item coin.
- Preserve the existing Spud's Shops blocks, ownership, discovery, UI and physical inventory. Safely migrate every payment route to Coin-only settlement; pending settlement output is never sellable and no parallel balance exists.
- No tests or test infrastructure. Compile the production module and affected JEMServer production sources with test tasks excluded after implementation.

## Review Focus

- Conservation holds across balances, fees and every escrow phase.
- Idempotency keys make replayed grants, purchases, bids and settlements no-ops.
- Item-and-money transactions expose durable recoverable phases instead of claiming in-memory atomicity.
- Stale price/stock versions cannot charge the buyer.
- Unsafe identity freezes writes without fabricating fallback money.

---

### Task 1: Scaffold `JEMEconomy`

**Files:**
- Create: `Mods/JEMEconomy/{build.gradle,gradle.properties,settings.gradle}`
- Create: `Mods/JEMEconomy/src/main/resources/META-INF/mods.toml`
- Create: `.../pack.mcmeta`
- Create: `.../java/com/siirio/jemeconomy/JemEconomy.java`

- [ ] Create a Forge-only Java-17 owner module with disabled test tasks and embedded menu support.
- [ ] Register ledger/config/commands only for dedicated servers and keep client screen/network registration multiplayer-compatible.

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

- [ ] Initialize the four reserve accounts once and persist immutable configured supply plus privileged audited deltas.
- [ ] Reject zero/negative/overflowing amounts, missing accounts, unsafe principals, duplicate keys and insufficient balances before mutation.
- [ ] Commit balanced debit/credit/fee legs as one ledger entry and freeze purchases on a detected conservation mismatch rather than correcting balances.

### Task 3: Implement Identity, Wallet and Funded Grants

**Files:**
- Create: `.../identity/EconomyIdentityPolicy.java`
- Create: `.../wallet/WalletService.java`
- Create: `.../wallet/StarterGrantService.java`
- Create: `.../wallet/WalletHistory.java`

- [ ] Resolve accounts only from verified principals; never create or transfer by raw display name.
- [ ] Transfer the one-time 100-Coin starter grant from Central Reserve with `starterGrant:<principal>` only when identity is verified and funds exist.
- [ ] Retain last 50 bounded history records and expose available versus escrowed totals without revealing unrelated private account details.

### Task 4: Implement Durable Escrow and Mailbox Delivery

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

### Task 5: Implement Listings, Buy Orders and Auctions

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

### Task 6: Implement Funded Public Orders and Reserve Budgets

**Files:**
- Create: `.../commission/PublicCommission.java`
- Create: `.../commission/CommissionService.java`
- Create: `.../commission/ReserveBudget.java`

- [ ] Fund NPC orders from NPC Market Reserve with world-wide 1,500/day and 80/account/day UTC limits.
- [ ] Fund event payouts from Operations Reserve with 500/event-day global limit and idempotent event/account keys.
- [ ] Mark exhausted orders unfunded without negative balances or automatic issuance.

### Task 7: Convert Spud Shops to Coin-Only Settlement

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
- [ ] Keep all native barter interaction disabled when Economy is absent or frozen; do not expose unsafe dual currency.

### Task 8: Implement Wallet and Market Screens

**Files:**
- Create: `.../network/EconomyNetwork.java`
- Create: `.../client/WalletScreen.java`
- Create: `.../client/MarketScreen.java`
- Create: `.../client/ListingEditor.java`
- Create: `.../client/TransactionHistoryScreen.java`
- Create: `.../client/EconomyClientState.java`

- [ ] Implement Available/In Escrow/Recent Activity, Send/Receive/Market/Settings and the six market tabs from bounded server DTOs.
- [ ] Require preview and second confirmation above 5,000 Coins; display exact recipient, amount, fee, resulting balance and receipt ID.
- [ ] Render frozen safe mode prominently and disable mutation controls without hiding market information.

### Task 9: Add Commands, Localization and Documentation

**Files:**
- Create: `.../command/EconomyCommands.java`
- Create: `.../resources/assets/jem_economy/lang/en_us.json`
- Create: `.../resources/assets/jem_economy/lang/ru_ru.json`
- Modify: `Mods/JEMServer/src/main/java/com/siirio/jemserver/client/smp/SmpSidebar.java`
- Modify: `Docs/DEVELOPMENT.md`
- Modify: `Docs/smp-gui/README.md`
- Modify: `Docs/SERVER_COMMANDS.md`
- Modify: `CHANGELOG.md`

- [ ] Register `/coins`, `/market`, status and OP4 supply commands; supply mint is default-off, nonce-confirmed, reasoned and capped at 1% per 30 UTC days.
- [ ] Add semantic Coin/market navigation and preserve current shop catalog behavior outside settlement.
- [ ] Document ledger ownership, safe mode, Coin-only Spud policy, recovery phases and user-supplied monetary decisions.


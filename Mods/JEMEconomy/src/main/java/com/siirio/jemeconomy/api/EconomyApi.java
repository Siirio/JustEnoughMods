package com.siirio.jemeconomy.api;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemeconomy.physical.PhysicalCoinService;
import com.siirio.jemeconomy.market.MarketSavedData;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemcurrency.CurrencyAuthority;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class EconomyApi {
    private static final long STARTER_GRANT = 2_000L;

    private EconomyApi() {
    }

    /** Reload-safe scripting query; does not construct or read a currency ledger. */
    public static boolean isEnabled() {
        return isEnabled(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
    }

    /** Recipe callback policy; its native resource generation is reconciled after world config loading. */
    public static boolean recipePolicyEnabled() {
        return com.siirio.jemeconomy.policy.EconomyRecipePolicy.query();
    }

    public static boolean isEnabled(MinecraftServer server) {
        return server != null && server.overworld() != null && com.siirio.jemeconomy.config.EconomyConfig.SPEC.isLoaded()
                && com.siirio.jemeconomy.config.EconomyConfig.ENABLED.get()
                && com.siirio.jemeconomy.policy.EconomyRecipePolicy.applied(server);
    }

    public static long balance(ServerPlayer player) {
        if (!CurrencyAuthority.active(player)) return 0L;
        return IdentityPolicy.resolve(player).map(principal -> {
            ensureStarter(player, principal.accountId());
            return EconomySavedData.get(player.getServer()).balance(principal.accountId());
        }).orElse(0L);
    }

    public static TransferResult send(ServerPlayer sender, UUID recipient, long amount, UUID requestId) {
        if (!CurrencyAuthority.active(sender)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        var principal = IdentityPolicy.resolve(sender);
        if (principal.isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        ensureStarter(sender, principal.get().accountId());
        return EconomySavedData.get(sender.getServer()).transfer(principal.get().accountId(), recipient, amount,
                "playerSend:" + principal.get().accountId() + ":" + requestId);
    }

    public static TransferResult fundedSystemTransfer(ServerPlayer authenticatedActor, SystemAccount source, UUID destination,
                                                      long amount, String idempotencyKey) {
        if (!CurrencyAuthority.active(authenticatedActor)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        if (IdentityPolicy.resolve(authenticatedActor).isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        return EconomySavedData.get(authenticatedActor.getServer()).transfer(source.id(), destination, amount, idempotencyKey);
    }

    public static PhysicalCoinService.WithdrawResult withdraw(ServerPlayer player, long amount, UUID requestId) {
        var principal = IdentityPolicy.resolve(player);
        if (!CurrencyAuthority.active(player)) return new PhysicalCoinService.WithdrawResult(PhysicalCoinService.Status.UNAVAILABLE, 0L, java.util.Map.of());
        if (principal.isEmpty()) return new PhysicalCoinService.WithdrawResult(PhysicalCoinService.Status.UNAUTHENTICATED, 0L, java.util.Map.of());
        ensureStarter(player, principal.get().accountId());
        return PhysicalCoinService.withdraw(player, amount, requestId);
    }

    public static PhysicalCoinService.WithdrawResult withdrawExact(ServerPlayer player,
            com.siirio.jemeconomy.physical.CoinDenomination denomination, int count, UUID requestId) {
        var principal = IdentityPolicy.resolve(player);
        if (!CurrencyAuthority.active(player)) return new PhysicalCoinService.WithdrawResult(PhysicalCoinService.Status.UNAVAILABLE, 0L, java.util.Map.of());
        if (principal.isEmpty()) return new PhysicalCoinService.WithdrawResult(PhysicalCoinService.Status.UNAUTHENTICATED, 0L, java.util.Map.of());
        ensureStarter(player, principal.get().accountId());
        return PhysicalCoinService.withdrawExact(player, denomination, count, requestId);
    }

    public static PhysicalCoinService.DepositResult depositAll(ServerPlayer player, UUID requestId) {
        return PhysicalCoinService.depositAll(player, requestId);
    }

    public static PhysicalCoinService.DepositResult depositExact(ServerPlayer player,
            com.siirio.jemeconomy.physical.CoinDenomination denomination, int count, UUID requestId) {
        return PhysicalCoinService.depositExact(player, denomination, count, requestId);
    }

    public static PhysicalCoinService.Status spendOneGold(ServerPlayer player, UUID requestId) {
        return PhysicalCoinService.spendOneGold(player, requestId);
    }

    public static PhysicalCoinService.Status spendPhysical(ServerPlayer player, long amount, UUID requestId, String purpose) {
        return PhysicalCoinService.spend(player, amount, requestId, purpose);
    }

    public static PhysicalCoinService.ExchangeResult exchangePhysical(ServerPlayer player,
            com.siirio.jemeconomy.physical.CoinDenomination source, int sourceCount,
            com.siirio.jemeconomy.physical.CoinDenomination target, UUID requestId) {
        return PhysicalCoinService.exchange(player, source, sourceCount, target, requestId);
    }

    public static PhysicalCoinService.HeldSummary heldCash(ServerPlayer player) {
        return PhysicalCoinService.heldSummary(player);
    }

    public static long physicalOutstanding(ServerPlayer player) {
        if (!CurrencyAuthority.active(player) || IdentityPolicy.resolve(player).isEmpty()) return 0L;
        return EconomySavedData.get(player.getServer()).physicalOutstanding();
    }

    public static MarketResult reserveMarketPayment(ServerPlayer buyer, long amount, UUID requestId) {
        if (!CurrencyAuthority.active(buyer)) return new MarketResult(MarketStatus.UNAVAILABLE, 0L);
        var principal = IdentityPolicy.resolve(buyer);
        if (principal.isEmpty()) return new MarketResult(MarketStatus.UNAUTHENTICATED, 0L);
        ensureStarter(buyer, principal.get().accountId());
        String key = "marketReserve:" + requestId;
        TransferResult transfer = EconomySavedData.get(buyer.getServer()).transfer(
                principal.get().accountId(), SystemAccount.MARKET_ESCROW.id(), amount, key);
        if (transfer.status() == TransferResult.Status.INSUFFICIENT_FUNDS) {
            PhysicalCoinService.DepositResult cash = PhysicalCoinService.depositAll(
                    buyer, UUID.nameUUIDFromBytes((key + ":cash").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (cash.status() == PhysicalCoinService.Status.COMMITTED) {
                transfer = EconomySavedData.get(buyer.getServer()).transfer(
                        principal.get().accountId(), SystemAccount.MARKET_ESCROW.id(), amount, key);
            }
        }
        return new MarketResult(switch (transfer.status()) {
            case COMMITTED -> MarketStatus.RESERVED;
            case REPLAYED -> MarketStatus.REPLAYED;
            case INSUFFICIENT_FUNDS -> MarketStatus.INSUFFICIENT_FUNDS;
            case UNAUTHENTICATED -> MarketStatus.UNAUTHENTICATED;
            default -> MarketStatus.UNAVAILABLE;
        }, amount);
    }

    public static MarketResult settleMarketPayment(ServerPlayer actor, UUID seller, long amount, UUID requestId) {
        if (!CurrencyAuthority.active(actor) || IdentityPolicy.resolve(actor).isEmpty()) {
            return new MarketResult(MarketStatus.UNAUTHENTICATED, 0L);
        }
        TransferResult transfer = EconomySavedData.get(actor.getServer()).transfer(
                SystemAccount.MARKET_ESCROW.id(), seller, amount, "marketSettle:" + requestId);
        return new MarketResult(transfer.status() == TransferResult.Status.COMMITTED
                ? MarketStatus.COMMITTED : transfer.status() == TransferResult.Status.REPLAYED
                ? MarketStatus.REPLAYED : MarketStatus.UNAVAILABLE, amount);
    }

    public static MarketResult refundMarketPayment(ServerPlayer actor, long amount, UUID requestId) {
        var principal = IdentityPolicy.resolve(actor);
        if (!CurrencyAuthority.active(actor) || principal.isEmpty()) return new MarketResult(MarketStatus.UNAUTHENTICATED, 0L);
        TransferResult transfer = EconomySavedData.get(actor.getServer()).transfer(
                SystemAccount.MARKET_ESCROW.id(), principal.get().accountId(), amount, "marketRefund:" + requestId);
        return new MarketResult(transfer.status() == TransferResult.Status.COMMITTED
                ? MarketStatus.REFUNDED : transfer.status() == TransferResult.Status.REPLAYED
                ? MarketStatus.REPLAYED : MarketStatus.UNAVAILABLE, amount);
    }

    public static UUID townAccount(UUID townId) {
        return UUID.nameUUIDFromBytes(("jem:town:" + townId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static long accountBalance(ServerPlayer authenticatedActor, UUID account) {
        if (!CurrencyAuthority.active(authenticatedActor) || IdentityPolicy.resolve(authenticatedActor).isEmpty()) return 0L;
        return EconomySavedData.get(authenticatedActor.getServer()).balance(account);
    }

    public static TransferResult depositToAccount(ServerPlayer player, UUID destination, long amount, UUID requestId) {
        if (!CurrencyAuthority.active(player)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        ensureStarter(player, principal.get().accountId());
        String key = "accountDeposit:" + requestId;
        TransferResult transfer = EconomySavedData.get(player.getServer()).transfer(principal.get().accountId(), destination, amount, key);
        if (transfer.status() == TransferResult.Status.INSUFFICIENT_FUNDS) {
            PhysicalCoinService.DepositResult cash = PhysicalCoinService.depositAll(player,
                    UUID.nameUUIDFromBytes((key + ":cash").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (cash.status() == PhysicalCoinService.Status.COMMITTED) {
                transfer = EconomySavedData.get(player.getServer()).transfer(principal.get().accountId(), destination, amount, key);
            }
        }
        return transfer;
    }

    public static TransferResult withdrawFromAccount(ServerPlayer authenticatedActor, UUID source, long amount, UUID requestId) {
        if (!CurrencyAuthority.active(authenticatedActor)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        var principal = IdentityPolicy.resolve(authenticatedActor);
        if (principal.isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        return EconomySavedData.get(authenticatedActor.getServer()).transfer(source, principal.get().accountId(), amount,
                "accountWithdraw:" + requestId);
    }

    public static ItemQuote quoteItem(ServerPlayer authenticatedActor, ItemStack stack, int quantity) {
        if (!CurrencyAuthority.active(authenticatedActor) || quantity <= 0) return new ItemQuote(QuoteStatus.UNAVAILABLE, 0L, 0L, 100, 0L);
        var principal = IdentityPolicy.resolve(authenticatedActor);
        var policy = ItemPolicyRegistry.policy(stack);
        if (principal.isEmpty()) return new ItemQuote(QuoteStatus.UNAUTHENTICATED, 0L, 0L, 100, 0L);
        if (com.siirio.jemeconomy.policy.TradeRestrictions.saleProhibited(stack) || policy.isEmpty() || !policy.get().sellable()) return new ItemQuote(QuoteStatus.NONE, 0L, 0L, 100, 0L);
        MarketSavedData market = MarketSavedData.get(authenticatedActor.getServer());
        int multiplier = market.multiplier(authenticatedActor.getServer(), policy.get().demandGroup());
        var raw = policy.get().salePrice(quantity,multiplier);
        long sell = EconomySavedData.get(authenticatedActor.getServer()).quoteSale(principal.get().accountId(),raw.numerator(),raw.denominator()).payout();
        long buy = policy.get().purchaseTotal(quantity);
        return new ItemQuote(QuoteStatus.BUY_AND_SELL, sell, buy, multiplier,
                market.accountRemaining(authenticatedActor.getServer(), principal.get().accountId()));
    }

    public static MarketCatalogSnapshot marketCatalog(ServerPlayer player, String rawSearch, String rawCategory, int requestedPage) {
        if (!CurrencyAuthority.active(player)) return new MarketCatalogSnapshot(0L, 0, 0, "", "blocks", 0L, new int[5], 0L, List.of(), "jem.economy.market.unavailable");
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return new MarketCatalogSnapshot(0L, 0, 0, "", "blocks", 0L, new int[5], 0L, List.of(), "jem.economy.identity_required");
        ensureStarter(player, principal.get().accountId());
        String search = rawSearch == null ? "" : rawSearch.strip().toLowerCase(Locale.ROOT);
        if (search.length() > 64) search = search.substring(0, 64);
        String category = rawCategory == null ? "blocks" : rawCategory.toLowerCase(Locale.ROOT);
        if (!category.matches("(food|plants|resources|blocks|drops)(_(1|8|64))?|building(64|256|1024|4096)")) category = "blocks";
        final int orderQuantity = category.startsWith("building") ? Integer.parseInt(category.substring(8)) : category.contains("_") ? Integer.parseInt(category.substring(category.indexOf('_')+1)) : 1;
        final String selectedSearch = search;
        final String selectedCategory = category.contains("_") ? category.substring(0,category.indexOf('_')) : category;
        MarketSavedData market = MarketSavedData.get(player.getServer());
        var ledger=EconomySavedData.get(player.getServer());
        long reserve=ledger.balance(SystemAccount.NPC_MARKET.id());
        var listings = com.siirio.jemeconomy.policy.MarketSupply.listings(player.getServer(), selectedCategory).stream()
                .filter(row -> com.siirio.jemeconomy.policy.MarketSearch.matches(row, selectedSearch))
                .filter(row -> {
                    var policy=row.policy();
                    if(policy.buyable()&&market.purchaseRemaining(player.getServer(),principal.get().accountId(),policy)>=orderQuantity)return true;
                    if(!policy.sellable()||reserve<=0||market.accountItemRemaining(player.getServer(),principal.get().accountId(),policy)<orderQuantity
                            ||market.globalItemRemaining(player.getServer(),policy)<orderQuantity)return false;
                    var price=policy.salePrice(orderQuantity,market.multiplier(player.getServer(),policy.demandGroup()));
                    var quote=ledger.quoteSale(principal.get().accountId(),price.numerator(),price.denominator());
                    return quote.payout()>0&&quote.fundingRequired()<=reserve;
                }).toList();
        int pageSize = 12;
        int pages = Math.max(1, (listings.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int start = Math.min(listings.size(), page * pageSize);
        int end = Math.min(listings.size(), start + pageSize);
        List<MarketItemSnapshot> rows = listings.subList(start, end).stream().map(row -> {
            var policy = row.policy();
            ItemStack stack = row.stack();
            int multiplier = market.multiplier(player.getServer(), policy.demandGroup());
            var allowance = policy.sellable() ? market.allowanceRational(player.getServer(),principal.get().accountId(),policy,orderQuantity)
                    : new MarketSavedData.QuoteAllowance(0,0,"blocked");
            var raw = policy.salePrice(orderQuantity,multiplier);
            var quote = EconomySavedData.get(player.getServer()).quoteSale(principal.get().accountId(),raw.numerator(),raw.denominator());
            long sell = policy.sellable() ? quote.payout() : 0L;
            int accountRemaining = policy.sellable() ? market.accountItemRemaining(player.getServer(), principal.get().accountId(), policy) : 0;
            int globalRemaining = policy.sellable() ? market.globalItemRemaining(player.getServer(), policy) : 0;
            return new MarketItemSnapshot(row.id(), stack, policy.purchaseTotal(orderQuantity), sell, policy.marketCategory(),
                    accountRemaining, globalRemaining, policy.buyable() && !stack.isEmpty() && market.purchaseRemaining(player.getServer(), principal.get().accountId(), policy) >= orderQuantity,
                    policy.sellable() && !stack.isEmpty() && allowance.units() >= orderQuantity && sell > 0
                            && EconomySavedData.get(player.getServer()).balance(SystemAccount.NPC_MARKET.id()) >= quote.fundingRequired());
        }).toList();
        MarketBulletin bulletin = marketBulletin(player);
        int[] physicalCounts = new int[5];
        var held = heldCash(player);
        for (var denomination : com.siirio.jemeconomy.physical.CoinDenomination.values())
            physicalCounts[denomination.ordinal()] = held.counts().getOrDefault(denomination, 0);
        return new MarketCatalogSnapshot(market.revision(player.getServer()), page, pages, search, category,
                balance(player), physicalCounts, bulletin.ticksUntilUpdate(), rows, "");
    }

    public static MarketPurchaseResult purchaseMarketItem(ServerPlayer buyer, net.minecraft.resources.ResourceLocation id,
                                                           int quantity, long observedRevision, UUID requestId) {
        return purchaseMarketItem(buyer,id,quantity,observedRevision,requestId,false);
    }

    public static MarketPurchaseResult purchaseMarketItem(ServerPlayer buyer, net.minecraft.resources.ResourceLocation id,
                                                           int quantity, long observedRevision, UUID requestId, boolean bulk) {
        if (!CurrencyAuthority.active(buyer) || id == null || requestId == null || quantity <= 0 || quantity > 4096)
            return new MarketPurchaseResult(MarketPurchaseStatus.INVALID, 0L, 0);
        var principal = IdentityPolicy.resolve(buyer);
        if (principal.isEmpty()) return new MarketPurchaseResult(MarketPurchaseStatus.UNAUTHENTICATED, 0L, 0);
        var listing = com.siirio.jemeconomy.policy.MarketSupply.resolve(buyer.getServer(), id);
        if (listing == null) return new MarketPurchaseResult(MarketPurchaseStatus.NONE, 0L, 0);
        var policy = listing.policy();
        if (!policy.buyable()) return new MarketPurchaseResult(MarketPurchaseStatus.NONE,0,0);
        if (bulk) {
            if (!java.util.Set.of(64,256,1024,4096).contains(quantity) || policy.bulk1024() == null)
                return new MarketPurchaseResult(MarketPurchaseStatus.INVALID,0,0);
            ensureStarter(buyer,principal.get().accountId());
            return com.siirio.jemeconomy.market.MarketBulkOrders.get(buyer.getServer()).purchase(buyer,listing,quantity,observedRevision,requestId);
        }
        if (quantity > 64) return new MarketPurchaseResult(MarketPurchaseStatus.INVALID,0,0);
        MarketSavedData market = MarketSavedData.get(buyer.getServer());
        if (market.revision(buyer.getServer()) != observedRevision)
            return new MarketPurchaseResult(MarketPurchaseStatus.STALE, 0L, 0);
        if (market.purchaseRemaining(buyer.getServer(), principal.get().accountId(), policy) < quantity)
            return new MarketPurchaseResult(MarketPurchaseStatus.UNAVAILABLE, 0L, 0);
        ItemStack delivery = listing.stack().copy();
        delivery.setCount(quantity);
        if (quantity > delivery.getMaxStackSize() || !canFit(buyer, delivery))
            return new MarketPurchaseResult(MarketPurchaseStatus.NO_SPACE, 0L, 0);
        long total;
        try { total = policy.purchaseTotal(quantity); }
        catch (ArithmeticException overflow) { return new MarketPurchaseResult(MarketPurchaseStatus.INVALID, 0L, 0); }
        ensureStarter(buyer, principal.get().accountId());
        String key = "marketCatalogBuy:" + principal.get().accountId() + ":" + requestId;
        TransferResult transfer = EconomySavedData.get(buyer.getServer()).transfer(principal.get().accountId(), SystemAccount.NPC_MARKET.id(), total, key);
        if (transfer.status() == TransferResult.Status.INSUFFICIENT_FUNDS) {
            var cash = PhysicalCoinService.depositAll(buyer, UUID.nameUUIDFromBytes((key + ":cash").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (cash.status() == PhysicalCoinService.Status.COMMITTED)
                transfer = EconomySavedData.get(buyer.getServer()).transfer(principal.get().accountId(), SystemAccount.NPC_MARKET.id(), total, key);
        }
        if (transfer.status() == TransferResult.Status.REPLAYED) return new MarketPurchaseResult(MarketPurchaseStatus.REPLAYED, total, quantity);
        if (transfer.status() == TransferResult.Status.INSUFFICIENT_FUNDS) return new MarketPurchaseResult(MarketPurchaseStatus.INSUFFICIENT_FUNDS, 0L, 0);
        if (transfer.status() != TransferResult.Status.COMMITTED) return new MarketPurchaseResult(MarketPurchaseStatus.UNAVAILABLE, 0L, 0);
        ItemStack remainder = delivery.copy();
        if (!buyer.getInventory().add(remainder) || !remainder.isEmpty()) {
            EconomySavedData.get(buyer.getServer()).transfer(SystemAccount.NPC_MARKET.id(), principal.get().accountId(), total, key + ":refund");
            return new MarketPurchaseResult(MarketPurchaseStatus.NO_SPACE, 0L, 0);
        }
        buyer.getInventory().setChanged();
        market.recordPurchase(buyer.getServer(), principal.get().accountId(), policy, quantity);
        buyer.containerMenu.broadcastChanges();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new EconomyOperationEvent(buyer.server, buyer.getUUID(), requestId,
                EconomyOperationEvent.Kind.MARKET_PURCHASE, delivery));
        return new MarketPurchaseResult(MarketPurchaseStatus.COMMITTED, total, quantity);
    }

    private static boolean canFit(ServerPlayer player, ItemStack delivery) {
        int room = 0;
        for (ItemStack current : player.getInventory().items) {
            if (current.isEmpty()) room += delivery.getMaxStackSize();
            else if (ItemStack.isSameItemSameTags(current, delivery)) room += Math.max(0, current.getMaxStackSize() - current.getCount());
            if (room >= delivery.getCount()) return true;
        }
        return false;
    }

    public static NpcSaleResult processNpcSale(ServerPlayer seller, ItemStack offered, int quantity, UUID requestId) {
        if (!CurrencyAuthority.active(seller) || requestId == null || quantity <= 0) return new NpcSaleResult(NpcSaleStatus.UNAVAILABLE, 0L, 0);
        var principal = IdentityPolicy.resolve(seller); var optional = ItemPolicyRegistry.policy(offered);
        if (com.siirio.jemeconomy.policy.TradeRestrictions.saleProhibited(offered)) return new NpcSaleResult(NpcSaleStatus.NONE, 0L, 0);
        if (principal.isEmpty()) return new NpcSaleResult(NpcSaleStatus.UNAUTHENTICATED, 0L, 0);
        if (optional.isEmpty() || !optional.get().sellable()) return new NpcSaleResult(NpcSaleStatus.NONE, 0L, 0);
        int held = 0;
        for (ItemStack stack : seller.getInventory().items) if (ItemPolicyRegistry.matchesSaleGoods(offered,stack)) held += stack.getCount();
        if (held < quantity) return new NpcSaleResult(NpcSaleStatus.INVENTORY_CHANGED, 0L, 0);
        var policy = optional.get(); MarketSavedData market = MarketSavedData.get(seller.getServer());
        var allowed = market.allowanceRational(seller.getServer(), principal.get().accountId(), policy, quantity);
        if (allowed.units() <= 0) return new NpcSaleResult(NpcSaleStatus.LIMIT_REACHED, 0L, 0);
        String key = "npcSale:" + principal.get().accountId() + ":" + requestId;
        var raw = policy.salePrice(allowed.units(),market.multiplier(seller.getServer(),policy.demandGroup()));
        var ledger = EconomySavedData.get(seller.getServer());
        var reservation = ledger.reserveSale(principal.get().accountId(),key,raw.numerator(),raw.denominator());
        if (reservation.status() == EconomySavedData.SaleStatus.REPLAYED) return new NpcSaleResult(NpcSaleStatus.REPLAYED,0,0);
        if (reservation.status() == EconomySavedData.SaleStatus.INSUFFICIENT_FUNDS) return new NpcSaleResult(NpcSaleStatus.MARKET_BUDGET_EXHAUSTED,0,0);
        if (reservation.status() != EconomySavedData.SaleStatus.RESERVED) return new NpcSaleResult(NpcSaleStatus.UNAVAILABLE,0,0);
        TransferResult transfer = ledger.transfer(SystemAccount.MARKET_ESCROW.id(), principal.get().accountId(), allowed.payout(), key + ":payment");
        if (transfer.status() == TransferResult.Status.INSUFFICIENT_FUNDS) return new NpcSaleResult(NpcSaleStatus.MARKET_BUDGET_EXHAUSTED, 0L, 0);
        if (transfer.status() != TransferResult.Status.COMMITTED && transfer.status() != TransferResult.Status.REPLAYED) return new NpcSaleResult(NpcSaleStatus.UNAVAILABLE, 0L, 0);
        if (transfer.status() == TransferResult.Status.COMMITTED) {
            int remaining = allowed.units();
            for (ItemStack stack : seller.getInventory().items) if (remaining > 0 && ItemPolicyRegistry.matchesSaleGoods(offered,stack)) { int take = Math.min(remaining, stack.getCount()); stack.shrink(take); remaining -= take; }
            var id = ForgeRegistries.ITEMS.getKey(offered.getItem());
            if (id == null || !market.commit(seller.getServer(), key, principal.get().accountId(), java.util.Map.of(id, allowed.units()), allowed.payout())) {
                return new NpcSaleResult(NpcSaleStatus.QUARANTINED, allowed.payout(), allowed.units());
            }
            ledger.commitSale(principal.get().accountId(),key);
            seller.getInventory().setChanged();
        }
        return new NpcSaleResult(transfer.status() == TransferResult.Status.REPLAYED ? NpcSaleStatus.REPLAYED : NpcSaleStatus.COMMITTED,
                allowed.payout(), allowed.units());
    }

    /** Native merchant inputs, exact full trade. Shipping Box retains its inventory sale API. */
    public static NpcSaleResult processNpcSaleFromContainer(ServerPlayer seller,
            net.minecraft.world.inventory.MerchantContainer inputs, ItemStack offered, int quantity,
            long expectedPayout, int sourceSlot, UUID requestId) {
        return settleNpcContainerSale(seller, inputs, offered, quantity, expectedPayout, sourceSlot, requestId,
                ItemPolicyRegistry.policy(offered).filter(policy -> policy.sellable()
                        && !com.siirio.jemeconomy.policy.TradeRestrictions.saleProhibited(offered)));
    }

    /** Trusted native merchandise may retain enchantments/NBT; never used by the global Market. */
    public static NpcSaleResult processNativeNpcSaleFromContainer(ServerPlayer seller,
            net.minecraft.world.inventory.MerchantContainer inputs, ItemStack offered, int quantity,
            long expectedPayout, int sourceSlot, UUID requestId) {
        return settleNpcContainerSale(seller, inputs, offered, quantity, expectedPayout, sourceSlot, requestId,
                com.siirio.jemeconomy.merchant.NativeItemPricing.policy(offered));
    }

    private static NpcSaleResult settleNpcContainerSale(ServerPlayer seller,
            net.minecraft.world.inventory.MerchantContainer inputs, ItemStack offered, int quantity,
            long expectedPayout, int sourceSlot, UUID requestId,
            java.util.Optional<com.siirio.jemeconomy.policy.ItemPolicy> optional) {
        if (!CurrencyAuthority.active(seller) || requestId == null || quantity <= 0 || sourceSlot < 0 || sourceSlot > 1)
            return new NpcSaleResult(NpcSaleStatus.UNAVAILABLE, 0L, 0);
        var principal = IdentityPolicy.resolve(seller);
        if (principal.isEmpty()) return new NpcSaleResult(NpcSaleStatus.UNAUTHENTICATED, 0L, 0);
        if (optional.isEmpty()) return new NpcSaleResult(NpcSaleStatus.NONE, 0L, 0);
        ItemStack actual = inputs.getItem(sourceSlot);
        if (!com.siirio.jemeconomy.merchant.NativeItemPricing.matchesGoods(offered, actual) || actual.getCount() < quantity)
            return new NpcSaleResult(NpcSaleStatus.INVENTORY_CHANGED, 0L, 0);
        var policy = optional.get();
        MarketSavedData market = MarketSavedData.get(seller.getServer());
        var canonical = ItemPolicyRegistry.policy(offered).filter(p -> p.sellable());
        var ledger = EconomySavedData.get(seller.getServer());
        var raw = com.siirio.jemeconomy.merchant.NativeTradePricing.salePrice(offered,quantity,market.multiplier(seller.getServer(),policy.demandGroup()));
        long payout = raw != null ? ledger.quoteSale(principal.get().accountId(),raw.numerator(),raw.denominator()).payout()
                : com.siirio.jemeconomy.merchant.NativeItemPricing.saleCopper(offered,quantity,market.multiplier(seller.getServer(),policy.demandGroup()));
        var allowed = market.allowanceExactTrade(seller.getServer(), principal.get().accountId(), policy, quantity, payout);
        if (allowed.units() != quantity || allowed.payout() != expectedPayout)
            return new NpcSaleResult(NpcSaleStatus.LIMIT_REACHED, 0L, 0);
        String key = "nativeSale:" + principal.get().accountId() + ":" + requestId;
        if (raw != null) {
            var reservation = ledger.reserveSale(principal.get().accountId(),key,raw.numerator(),raw.denominator());
            if (reservation.status() == EconomySavedData.SaleStatus.REPLAYED) return new NpcSaleResult(NpcSaleStatus.REPLAYED,0,0);
            if (reservation.status() == EconomySavedData.SaleStatus.INSUFFICIENT_FUNDS) return new NpcSaleResult(NpcSaleStatus.MARKET_BUDGET_EXHAUSTED,0,0);
            if (reservation.status() != EconomySavedData.SaleStatus.RESERVED) return new NpcSaleResult(NpcSaleStatus.UNAVAILABLE,0,0);
        }
        var transfer = ledger.transfer(raw != null ? SystemAccount.MARKET_ESCROW.id() : SystemAccount.NPC_MARKET.id(),
                principal.get().accountId(), allowed.payout(), key + ":payment");
        if (transfer.status() == TransferResult.Status.INSUFFICIENT_FUNDS)
            return new NpcSaleResult(NpcSaleStatus.MARKET_BUDGET_EXHAUSTED, 0L, 0);
        if (transfer.status() != TransferResult.Status.COMMITTED)
            return new NpcSaleResult(transfer.status() == TransferResult.Status.REPLAYED ? NpcSaleStatus.REPLAYED : NpcSaleStatus.UNAVAILABLE, 0L, 0);
        // Same server-thread preflight and commit: no callbacks can change quota between them.
        if (!market.commitNative(seller.getServer(),key,principal.get().accountId(),policy,quantity,allowed.payout()))
            throw new IllegalStateException("Native market allowance changed during synchronous settlement");
        if (raw != null) ledger.commitSale(principal.get().accountId(),key);
        // Session consumes both native nonmoney inputs once after this monetary commit.
        return new NpcSaleResult(NpcSaleStatus.COMMITTED, allowed.payout(), quantity);
    }

    public static TransferResult payTownPayroll(ServerPlayer authenticatedActor, UUID townId, UUID workerAccount,
                                                 long amount, UUID requestId) {
        if (!CurrencyAuthority.active(authenticatedActor)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        if (IdentityPolicy.resolve(authenticatedActor).isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        return EconomySavedData.get(authenticatedActor.getServer()).transfer(townAccount(townId), workerAccount, amount,
                "townPayroll:" + townId + ":" + workerAccount + ":" + requestId);
    }

    /** Scheduled settlement debit. The town was previously created by an authenticated principal; no live client chooses either account. */
    public static TransferResult settleTownPayroll(MinecraftServer server, UUID townId, long amount, long period) {
        if (!CurrencyAuthority.active(server) || townId == null || amount <= 0L || period < 0L) {
            return new TransferResult(TransferResult.Status.INVALID, null, 0L, 0L);
        }
        return EconomySavedData.get(server).transfer(townAccount(townId), SystemAccount.CENTRAL_RESERVE.id(), amount,
                "payroll:" + townId + ":" + period);
    }

    /** Same ledger and finite-money sink for recruits hired by an employer without a town. */
    public static TransferResult settleEmployerPayroll(MinecraftServer server, UUID employerAccount, long amount, long period) {
        if (!CurrencyAuthority.active(server) || employerAccount == null || amount <= 0L || period < 0L)
            return new TransferResult(TransferResult.Status.INVALID, null, 0L, 0L);
        return EconomySavedData.get(server).transfer(employerAccount, SystemAccount.CENTRAL_RESERVE.id(), amount,
                "employerPayroll:" + employerAccount + ":" + period);
    }

    public static long townBalance(MinecraftServer server, UUID townId) {
        return !CurrencyAuthority.active(server) || townId == null ? 0L : EconomySavedData.get(server).balance(townAccount(townId));
    }

    public static MarketBulletin marketBulletin(ServerPlayer authenticatedActor) {
        if (!CurrencyAuthority.active(authenticatedActor)) return new MarketBulletin("", "", 0L, 0L, 0L);
        var principal = IdentityPolicy.resolve(authenticatedActor); if (principal.isEmpty()) return new MarketBulletin("", "", 0L, 0L, 0L);
        MarketSavedData market = MarketSavedData.get(authenticatedActor.getServer()); var demand = market.demand(authenticatedActor.getServer());
        long time = authenticatedActor.getServer().overworld().getDayTime();
        long next = Math.addExact(Math.multiplyExact(Math.floorDiv(time - 1_000L, 24_000L) + 1L, 24_000L), 1_000L);
        return new MarketBulletin(demand.primary(), demand.secondary(), Math.max(0L, next - time),
                market.accountRemaining(authenticatedActor.getServer(), principal.get().accountId()), market.globalRemaining(authenticatedActor.getServer()));
    }

    public enum MarketStatus { RESERVED, COMMITTED, REFUNDED, REPLAYED, INSUFFICIENT_FUNDS, UNAUTHENTICATED, UNAVAILABLE }
    public record MarketResult(MarketStatus status, long amount) { }
    public enum QuoteStatus { BUY_AND_SELL, NONE, UNAUTHENTICATED, UNAVAILABLE }
    public record ItemQuote(QuoteStatus status, long sellCopper, long buyCopper, int demandPercent, long accountDailyRemaining) { }
    public enum NpcSaleStatus { COMMITTED, REPLAYED, NONE, LIMIT_REACHED, MARKET_BUDGET_EXHAUSTED, INVENTORY_CHANGED, UNAUTHENTICATED, UNAVAILABLE, QUARANTINED }
    public record NpcSaleResult(NpcSaleStatus status, long paidCopper, int units) { }
    public enum MarketPurchaseStatus { COMMITTED, REPLAYED, NONE, STALE, NO_SPACE, INSUFFICIENT_FUNDS, UNAUTHENTICATED, UNAVAILABLE, INVALID, QUARANTINED }
    public record MarketPurchaseResult(MarketPurchaseStatus status, long paidCopper, int units) { }
    public record MarketBulletin(String primaryGroup, String secondaryGroup, long ticksUntilUpdate, long accountRemaining, long globalRemaining) { }

    private static void ensureStarter(ServerPlayer player, UUID accountId) {
        if (isEnabled(player.getServer())) {
            var data = EconomySavedData.get(player.getServer());
            if (com.siirio.jemidentity.api.DedicatedTestProfile.active(player.getServer())) data.ensureTestGrant(player);
            else data.ensureStarter(accountId, STARTER_GRANT);
        }
    }
}

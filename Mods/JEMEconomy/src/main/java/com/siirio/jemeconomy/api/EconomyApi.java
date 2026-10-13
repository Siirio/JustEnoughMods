package com.siirio.jemeconomy.api;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemeconomy.physical.PhysicalCoinService;
import com.siirio.jemeconomy.market.MarketSavedData;
import com.siirio.jemeconomy.policy.ItemPolicyRegistry;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class EconomyApi {
    private static final long STARTER_GRANT = 100L;

    private EconomyApi() {
    }

    public static long balance(ServerPlayer player) {
        if (!DedicatedSmp.active(player)) return 0L;
        return IdentityPolicy.resolve(player).map(principal -> {
            ensureStarter(player, principal.accountId());
            return EconomySavedData.get(player.getServer()).balance(principal.accountId());
        }).orElse(0L);
    }

    public static TransferResult send(ServerPlayer sender, UUID recipient, long amount, UUID requestId) {
        if (!DedicatedSmp.active(sender)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        var principal = IdentityPolicy.resolve(sender);
        if (principal.isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        ensureStarter(sender, principal.get().accountId());
        return EconomySavedData.get(sender.getServer()).transfer(principal.get().accountId(), recipient, amount,
                "playerSend:" + principal.get().accountId() + ":" + requestId);
    }

    public static TransferResult fundedSystemTransfer(ServerPlayer authenticatedActor, SystemAccount source, UUID destination,
                                                      long amount, String idempotencyKey) {
        if (!DedicatedSmp.active(authenticatedActor)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        if (IdentityPolicy.resolve(authenticatedActor).isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        return EconomySavedData.get(authenticatedActor.getServer()).transfer(source.id(), destination, amount, idempotencyKey);
    }

    public static PhysicalCoinService.WithdrawResult withdraw(ServerPlayer player, long amount, UUID requestId) {
        var principal = IdentityPolicy.resolve(player);
        if (!DedicatedSmp.active(player)) return new PhysicalCoinService.WithdrawResult(PhysicalCoinService.Status.UNAVAILABLE, 0L, java.util.Map.of());
        if (principal.isEmpty()) return new PhysicalCoinService.WithdrawResult(PhysicalCoinService.Status.UNAUTHENTICATED, 0L, java.util.Map.of());
        ensureStarter(player, principal.get().accountId());
        return PhysicalCoinService.withdraw(player, amount, requestId);
    }

    public static PhysicalCoinService.DepositResult depositAll(ServerPlayer player, UUID requestId) {
        return PhysicalCoinService.depositAll(player, requestId);
    }

    public static PhysicalCoinService.HeldSummary heldCash(ServerPlayer player) {
        return PhysicalCoinService.heldSummary(player);
    }

    public static long physicalOutstanding(ServerPlayer player) {
        if (!DedicatedSmp.active(player) || IdentityPolicy.resolve(player).isEmpty()) return 0L;
        return EconomySavedData.get(player.getServer()).physicalOutstanding();
    }

    public static MarketResult reserveMarketPayment(ServerPlayer buyer, long amount, UUID requestId) {
        if (!DedicatedSmp.active(buyer)) return new MarketResult(MarketStatus.UNAVAILABLE, 0L);
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
        if (!DedicatedSmp.active(actor) || IdentityPolicy.resolve(actor).isEmpty()) {
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
        if (!DedicatedSmp.active(actor) || principal.isEmpty()) return new MarketResult(MarketStatus.UNAUTHENTICATED, 0L);
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
        if (!DedicatedSmp.active(authenticatedActor) || IdentityPolicy.resolve(authenticatedActor).isEmpty()) return 0L;
        return EconomySavedData.get(authenticatedActor.getServer()).balance(account);
    }

    public static TransferResult depositToAccount(ServerPlayer player, UUID destination, long amount, UUID requestId) {
        if (!DedicatedSmp.active(player)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
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
        if (!DedicatedSmp.active(authenticatedActor)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        var principal = IdentityPolicy.resolve(authenticatedActor);
        if (principal.isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        return EconomySavedData.get(authenticatedActor.getServer()).transfer(source, principal.get().accountId(), amount,
                "accountWithdraw:" + requestId);
    }

    public static ItemQuote quoteItem(ServerPlayer authenticatedActor, ItemStack stack, int quantity) {
        if (!DedicatedSmp.active(authenticatedActor) || quantity <= 0) return new ItemQuote(QuoteStatus.UNAVAILABLE, 0L, 0L, 100, 0L);
        var principal = IdentityPolicy.resolve(authenticatedActor);
        var policy = ItemPolicyRegistry.policy(stack);
        if (principal.isEmpty()) return new ItemQuote(QuoteStatus.UNAUTHENTICATED, 0L, 0L, 100, 0L);
        if (policy.isEmpty() || !policy.get().sellable()) return new ItemQuote(QuoteStatus.NONE, 0L, 0L, 100, 0L);
        MarketSavedData market = MarketSavedData.get(authenticatedActor.getServer());
        int multiplier = market.multiplier(authenticatedActor.getServer(), policy.get().demandGroup());
        long sellUnit = Math.floorDiv(Math.multiplyExact(policy.get().sellCopper(), multiplier), 100L);
        long sell = Math.multiplyExact(sellUnit, quantity);
        long buy = Math.multiplyExact(policy.get().buyCopper(), quantity);
        return new ItemQuote(QuoteStatus.BUY_AND_SELL, sell, buy, multiplier,
                market.accountRemaining(authenticatedActor.getServer(), principal.get().accountId()));
    }

    public static NpcSaleResult processNpcSale(ServerPlayer seller, ItemStack offered, int quantity, UUID requestId) {
        if (!DedicatedSmp.active(seller) || requestId == null || quantity <= 0) return new NpcSaleResult(NpcSaleStatus.UNAVAILABLE, 0L, 0);
        var principal = IdentityPolicy.resolve(seller); var optional = ItemPolicyRegistry.policy(offered);
        if (principal.isEmpty()) return new NpcSaleResult(NpcSaleStatus.UNAUTHENTICATED, 0L, 0);
        if (optional.isEmpty() || !optional.get().sellable()) return new NpcSaleResult(NpcSaleStatus.NONE, 0L, 0);
        int held = 0;
        for (ItemStack stack : seller.getInventory().items) if (ItemStack.isSameItemSameTags(stack, offered)) held += stack.getCount();
        if (held < quantity) return new NpcSaleResult(NpcSaleStatus.INVENTORY_CHANGED, 0L, 0);
        var policy = optional.get(); MarketSavedData market = MarketSavedData.get(seller.getServer());
        long unit = Math.floorDiv(Math.multiplyExact(policy.sellCopper(), market.multiplier(seller.getServer(), policy.demandGroup())), 100L);
        var allowed = market.allowance(seller.getServer(), principal.get().accountId(), policy, quantity, unit);
        if (allowed.units() <= 0) return new NpcSaleResult(NpcSaleStatus.LIMIT_REACHED, 0L, 0);
        String key = "npcSale:" + principal.get().accountId() + ":" + requestId;
        TransferResult transfer = EconomySavedData.get(seller.getServer()).transfer(SystemAccount.NPC_MARKET.id(), principal.get().accountId(), allowed.payout(), key);
        if (transfer.status() == TransferResult.Status.INSUFFICIENT_FUNDS) return new NpcSaleResult(NpcSaleStatus.MARKET_BUDGET_EXHAUSTED, 0L, 0);
        if (transfer.status() != TransferResult.Status.COMMITTED && transfer.status() != TransferResult.Status.REPLAYED) return new NpcSaleResult(NpcSaleStatus.UNAVAILABLE, 0L, 0);
        if (transfer.status() == TransferResult.Status.COMMITTED) {
            int remaining = allowed.units();
            for (ItemStack stack : seller.getInventory().items) if (remaining > 0 && ItemStack.isSameItemSameTags(stack, offered)) { int take = Math.min(remaining, stack.getCount()); stack.shrink(take); remaining -= take; }
            var id = ForgeRegistries.ITEMS.getKey(offered.getItem());
            if (id == null || !market.commit(seller.getServer(), key, principal.get().accountId(), java.util.Map.of(id, allowed.units()), allowed.payout())) {
                return new NpcSaleResult(NpcSaleStatus.QUARANTINED, allowed.payout(), allowed.units());
            }
            seller.getInventory().setChanged();
        }
        return new NpcSaleResult(transfer.status() == TransferResult.Status.REPLAYED ? NpcSaleStatus.REPLAYED : NpcSaleStatus.COMMITTED,
                allowed.payout(), allowed.units());
    }

    public static TransferResult payTownPayroll(ServerPlayer authenticatedActor, UUID townId, UUID workerAccount,
                                                 long amount, UUID requestId) {
        if (!DedicatedSmp.active(authenticatedActor)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        if (IdentityPolicy.resolve(authenticatedActor).isEmpty()) return new TransferResult(TransferResult.Status.UNAUTHENTICATED, null, 0L, 0L);
        return EconomySavedData.get(authenticatedActor.getServer()).transfer(townAccount(townId), workerAccount, amount,
                "townPayroll:" + townId + ":" + workerAccount + ":" + requestId);
    }

    public static MarketBulletin marketBulletin(ServerPlayer authenticatedActor) {
        if (!DedicatedSmp.active(authenticatedActor)) return new MarketBulletin("", "", 0L, 0L, 0L);
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
    public record MarketBulletin(String primaryGroup, String secondaryGroup, long ticksUntilUpdate, long accountRemaining, long globalRemaining) { }

    private static void ensureStarter(ServerPlayer player, UUID accountId) {
        EconomySavedData.get(player.getServer()).transfer(SystemAccount.CENTRAL_RESERVE.id(), accountId, STARTER_GRANT,
                "starterGrant:" + accountId);
    }
}

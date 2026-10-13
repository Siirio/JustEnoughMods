package com.siirio.jemeconomy.api;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemeconomy.physical.PhysicalCoinService;
import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

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

    public enum MarketStatus { RESERVED, COMMITTED, REFUNDED, REPLAYED, INSUFFICIENT_FUNDS, UNAUTHENTICATED, UNAVAILABLE }
    public record MarketResult(MarketStatus status, long amount) { }

    private static void ensureStarter(ServerPlayer player, UUID accountId) {
        EconomySavedData.get(player.getServer()).transfer(SystemAccount.CENTRAL_RESERVE.id(), accountId, STARTER_GRANT,
                "starterGrant:" + accountId);
    }
}

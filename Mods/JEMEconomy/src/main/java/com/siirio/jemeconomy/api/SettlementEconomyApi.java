package com.siirio.jemeconomy.api;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemeconomy.persistence.NativeSavedDataCommit;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Settlement financial boundary. Successful scheduled operations are durable before acknowledgement.
 * A failed commit throws; the caller retains its intent and retries the same receipt key. */
public final class SettlementEconomyApi {
    private SettlementEconomyApi() { }

    public enum Status { COMMITTED, REPLAYED, INSUFFICIENT_FUNDS, UNAUTHENTICATED, INVALID, UNAVAILABLE }
    public record Result(Status status, UUID receiptId, long sourceBalance, long destinationBalance) { }
    public record Receipt(UUID receipt, UUID source, UUID destination, long amount) { }

    private static Result result(TransferResult result) {
        Status status = switch (result.status()) {
            case COMMITTED -> Status.COMMITTED;
            case REPLAYED -> Status.REPLAYED;
            case INSUFFICIENT_FUNDS -> Status.INSUFFICIENT_FUNDS;
            case UNAUTHENTICATED -> Status.UNAUTHENTICATED;
            case INVALID -> Status.INVALID;
            case UNAVAILABLE -> Status.UNAVAILABLE;
        };
        return new Result(status, result.receiptId(), result.sourceBalance(), result.destinationBalance());
    }

    private static Result commit(MinecraftServer server, TransferResult result) {
        if (result.status() == TransferResult.Status.COMMITTED || result.status() == TransferResult.Status.REPLAYED)
            NativeSavedDataCommit.save(server, EconomySavedData.get(server), "jem_economy");
        return result(result);
    }

    public static boolean empty(MinecraftServer server, UUID account) {
        return EconomySavedData.get(server).balance(account) == 0L;
    }

    public static Result deposit(ServerPlayer player, UUID account, long amount, UUID request) {
        return result(EconomyApi.depositToAccount(player, account, amount, request));
    }

    public static Result withdraw(ServerPlayer player, UUID account, long amount, UUID request) {
        return result(EconomyApi.withdrawFromAccount(player, account, amount, request));
    }

    public static Result townPayroll(MinecraftServer server, UUID town, long amount, long period) {
        return commit(server, EconomyApi.settleTownPayroll(server, town, amount, period));
    }

    public static Result employerPayroll(MinecraftServer server, UUID owner, long amount, long period) {
        return commit(server, EconomyApi.settleEmployerPayroll(server, owner, amount, period));
    }

    public static Result dailyTax(MinecraftServer server, UUID town, long amount, long day) {
        if (!EconomyApi.isEnabled(server)) return result(new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L));
        String key = "settlementDailyTax:" + town + ":" + day;
        var ledger = EconomySavedData.get(server);
        var transfer = ledger.hasReceipt(key)
                ? ledger.transfer(SystemAccount.OPERATIONS.id(), EconomyApi.townAccount(town), amount, key)
                : ledger.issueSettlementIncome(town, EconomyApi.townAccount(town), amount, day);
        return commit(server, transfer);
    }

    public static List<Receipt> history(MinecraftServer server, UUID town) {
        return EconomySavedData.get(server).receiptHistory(EconomyApi.townAccount(town), 16).stream()
                .map(row -> new Receipt(row.receipt(), row.source(), row.destination(), row.amount())).toList();
    }
}

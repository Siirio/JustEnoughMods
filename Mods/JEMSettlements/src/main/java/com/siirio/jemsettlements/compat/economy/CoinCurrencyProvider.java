package com.siirio.jemsettlements.compat.economy;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.api.SettlementEconomyApi;
import com.siirio.jemsettlements.currency.SettlementCoinProvider;
import com.siirio.jemsettlements.currency.TransferResult;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Loaded only after Economy presence is established. Economy remains the sole Coin state owner. */
public final class CoinCurrencyProvider implements SettlementCoinProvider {
    private static TransferResult convert(SettlementEconomyApi.Result result) {
        TransferResult.Status status = switch (result.status()) {
            case COMMITTED -> TransferResult.Status.COMMITTED;
            case REPLAYED -> TransferResult.Status.REPLAYED;
            case INSUFFICIENT_FUNDS -> TransferResult.Status.INSUFFICIENT_FUNDS;
            case UNAUTHENTICATED -> TransferResult.Status.UNAUTHENTICATED;
            case INVALID -> TransferResult.Status.INVALID;
            case UNAVAILABLE -> TransferResult.Status.UNAVAILABLE;
        };
        return new TransferResult(status, result.receiptId(), result.sourceBalance(), result.destinationBalance());
    }
    public boolean enabled(MinecraftServer server) { return EconomyApi.isEnabled(server); }
    public long balance(ServerPlayer player) { return EconomyApi.balance(player); }
    public long held(ServerPlayer player) { return EconomyApi.heldCash(player).value(); }
    public long accountBalance(ServerPlayer player, UUID account) { return EconomyApi.accountBalance(player, account); }
    public long townBalance(MinecraftServer server, UUID town) { return EconomyApi.townBalance(server, town); }
    public boolean empty(MinecraftServer server, UUID account) { return SettlementEconomyApi.empty(server, account); }
    public TransferResult deposit(ServerPlayer player, UUID account, long amount, UUID request) { return convert(SettlementEconomyApi.deposit(player, account, amount, request)); }
    public TransferResult withdraw(ServerPlayer player, UUID account, long amount, UUID request) { return convert(SettlementEconomyApi.withdraw(player, account, amount, request)); }
    public TransferResult townPayroll(MinecraftServer server, UUID town, long amount, long period) { return convert(SettlementEconomyApi.townPayroll(server, town, amount, period)); }
    public TransferResult employerPayroll(MinecraftServer server, UUID owner, long amount, long period) { return convert(SettlementEconomyApi.employerPayroll(server, owner, amount, period)); }
    public TransferResult dailyTax(MinecraftServer server, UUID town, long amount, long day) { return convert(SettlementEconomyApi.dailyTax(server, town, amount, day)); }
    public ListTag history(MinecraftServer server, UUID town) {
        ListTag rows = new ListTag(); UUID account = EconomyApi.townAccount(town);
        for (var receipt : SettlementEconomyApi.history(server, town)) {
            CompoundTag row = new CompoundTag(); row.putUUID("Receipt", receipt.receipt());
            row.putUUID("Source", receipt.source()); row.putUUID("Destination", receipt.destination());
            boolean incoming = receipt.destination().equals(account);
            row.putString("Kind", incoming ? "incoming" : "outgoing");
            row.putLong("Amount", receipt.amount()); row.putBoolean("Incoming", incoming); rows.add(row);
        }
        return rows;
    }
    public CompoundTag bulletin(ServerPlayer player) {
        var bulletin = EconomyApi.marketBulletin(player); CompoundTag result = new CompoundTag();
        result.putString("MarketPrimary", bulletin.primaryGroup()); result.putString("MarketSecondary", bulletin.secondaryGroup());
        result.putLong("MarketTicks", bulletin.ticksUntilUpdate()); result.putLong("MarketAccountRemaining", bulletin.accountRemaining());
        result.putLong("MarketGlobalRemaining", bulletin.globalRemaining()); return result;
    }
    public Component format(long amount) { return com.siirio.jemeconomy.physical.CoinFormatter.format(amount); }
}

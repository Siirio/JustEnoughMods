package com.siirio.jemsettlements.compat.economy;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.SystemAccount;
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
    private static TransferResult convert(com.siirio.jemeconomy.ledger.TransferResult result) {
        return new TransferResult(TransferResult.Status.valueOf(result.status().name()), result.receiptId(), result.sourceBalance(), result.destinationBalance());
    }
    public boolean enabled(MinecraftServer server) { return EconomyApi.isEnabled(server); }
    public long balance(ServerPlayer player) { return EconomyApi.balance(player); }
    public long held(ServerPlayer player) { return EconomyApi.heldCash(player).value(); }
    public long accountBalance(ServerPlayer player, UUID account) { return EconomyApi.accountBalance(player, account); }
    public long townBalance(MinecraftServer server, UUID town) { return EconomyApi.townBalance(server, town); }
    public boolean empty(MinecraftServer server, UUID account) { return EconomySavedData.get(server).balance(account) == 0L; }
    public TransferResult deposit(ServerPlayer player, UUID account, long amount, UUID request) { return convert(EconomyApi.depositToAccount(player, account, amount, request)); }
    public TransferResult withdraw(ServerPlayer player, UUID account, long amount, UUID request) { return convert(EconomyApi.withdrawFromAccount(player, account, amount, request)); }
    public TransferResult townPayroll(MinecraftServer server, UUID town, long amount, long period) { return durable(server, EconomyApi.settleTownPayroll(server, town, amount, period)); }
    public TransferResult employerPayroll(MinecraftServer server, UUID owner, long amount, long period) { return durable(server, EconomyApi.settleEmployerPayroll(server, owner, amount, period)); }
    private static TransferResult durable(MinecraftServer server, com.siirio.jemeconomy.ledger.TransferResult result) {
        if (result.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED
                || result.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED)
            com.siirio.jemsettlements.payroll.TaxDurability.save(server, EconomySavedData.get(server), "jem_economy");
        return convert(result);
    }
    public TransferResult dailyTax(MinecraftServer server, UUID town, long amount, long day) {
        if (!enabled(server)) return new TransferResult(TransferResult.Status.UNAVAILABLE, null, 0L, 0L);
        String key = "settlementDailyTax:" + town + ":" + day;
        var ledger = EconomySavedData.get(server);
        var result = ledger.hasReceipt(key)
                ? ledger.transfer(SystemAccount.OPERATIONS.id(), EconomyApi.townAccount(town), amount, key)
                : ledger.issueSettlementIncome(town, EconomyApi.townAccount(town), amount, day);
        if (result.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.COMMITTED
                || result.status() == com.siirio.jemeconomy.ledger.TransferResult.Status.REPLAYED)
            com.siirio.jemsettlements.payroll.TaxDurability.save(server, ledger, "jem_economy");
        return convert(result);
    }
    public ListTag history(MinecraftServer server, UUID town) {
        ListTag rows = new ListTag(); UUID account = EconomyApi.townAccount(town);
        for (var receipt : EconomySavedData.get(server).receiptHistory(account, 16)) {
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

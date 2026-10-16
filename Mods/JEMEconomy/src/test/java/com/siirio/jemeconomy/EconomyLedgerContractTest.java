package com.siirio.jemeconomy;

import com.siirio.jemeconomy.ledger.*;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Explicitly authorized economy-overhaul contract suite; invokes the real production ledger. */
public final class EconomyLedgerContractTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        EconomySavedData ledger = new EconomySavedData(); UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        UUID reserve = SystemAccount.CENTRAL_RESERVE.id();
        require(ledger.transfer(reserve, alice, 1000, "grant").status() == TransferResult.Status.COMMITTED, "funded transfer");
        require(ledger.transfer(reserve, alice, 1000, "grant").status() == TransferResult.Status.REPLAYED, "duplicate is replay");
        require(ledger.balance(alice) == 1000, "duplicate does not mint");
        require(ledger.transfer(reserve, alice, 999, "grant").status() == TransferResult.Status.INVALID, "request binds amount");
        require(ledger.transfer(reserve, bob, 1000, "grant").status() == TransferResult.Status.INVALID, "request binds recipient");
        require(ledger.transfer(alice, bob, -1, "negative").status() == TransferResult.Status.INVALID, "negative rejected");
        require(ledger.transfer(alice, bob, Long.MAX_VALUE, "overflow").status() == TransferResult.Status.INSUFFICIENT_FUNDS, "overspend rejected");
        for (int i = 0; i < 300; i++) {
            require(ledger.transfer(i % 2 == 0 ? alice : bob, i % 2 == 0 ? bob : alice, 1, "move:" + i).status() == TransferResult.Status.COMMITTED, "alternating transfer");
        }
        require(ledger.balance(alice) + ledger.balance(bob) == 1000, "finite player money conserved");
        long total = ledger.balance(alice) + ledger.balance(bob);
        for (var account : SystemAccount.values()) total += ledger.balance(account.id());
        require(total == 100_000, "whole economy conserved");
        var load = EconomySavedData.class.getDeclaredMethod("load", CompoundTag.class); load.setAccessible(true);
        EconomySavedData restored = (EconomySavedData) load.invoke(null, ledger.save(new CompoundTag()));
        require(!restored.frozen() && restored.balance(alice) == ledger.balance(alice), "restart preserves ledger");
        require(restored.transfer(reserve, alice, 1000, "grant").status() == TransferResult.Status.REPLAYED, "restart preserves receipts");
        require(restored.transfer(reserve, alice, 999, "grant").status() == TransferResult.Status.INVALID, "restart preserves bound terms");
        var cash = restored.prepareWithdraw(alice, 9, UUID.randomUUID());
        require(cash.status() == EconomySavedData.PhysicalStatus.PREPARED, "backed withdrawal");
        restored.commitWithdraw(cash.operationId());
        var copper = cash.lots().stream().filter(lot -> lot.denomination() == com.siirio.jemeconomy.physical.CoinDenomination.COPPER).findFirst().orElseThrow();
        var duplicate = java.util.List.of(
                new EconomySavedData.DepositCandidate(0, copper.id(), copper.denomination(), 9),
                new EconomySavedData.DepositCandidate(1, copper.id(), copper.denomination(), 9));
        var exchange = restored.prepareExchange(alice, duplicate, copper.denomination(), 10,
                com.siirio.jemeconomy.physical.CoinDenomination.SILVER, UUID.randomUUID());
        require(exchange.status() == EconomySavedData.PhysicalStatus.INVALID, "duplicated lots cannot buy extra value");
        UUID newcomer = UUID.randomUUID(); restored.ensureStarter(newcomer, 100);
        for (int i = 0; i < 50_010; i++) restored.transfer(i % 2 == 0 ? alice : bob, i % 2 == 0 ? bob : alice, 1, "trim:" + i);
        restored.ensureStarter(newcomer, 100);
        require(restored.balance(newcomer) == 100, "starter grant survives receipt trimming");
        System.out.println("Economy ledger contracts passed: " + checks);
    }
    private static void require(boolean condition, String label) {
        checks++; if (!condition) throw new AssertionError(label);
    }
}

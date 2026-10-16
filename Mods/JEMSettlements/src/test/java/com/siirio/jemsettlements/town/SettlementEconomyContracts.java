package com.siirio.jemsettlements.town;

import com.siirio.jemsettlements.payroll.EmployerPayroll;
import com.siirio.jemsettlements.payroll.PayrollClock;
import com.siirio.jemsettlements.payroll.PayrollPeriod;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Run with the production Minecraft runtime classpath; no world/server/save is opened. */
public final class SettlementEconomyContracts {
    private static int assertions;
    public static void main(String[] args) {
        UUID owner = UUID.randomUUID(), townId = UUID.randomUUID(), soldier = UUID.randomUUID();
        ResourceLocation type = new ResourceLocation("recruits", "recruit");
        WorkforceRecord record = new WorkforceRecord(soldier, townId, owner, WorkforceType.RECRUIT, "recruits", type, true, 1_000L);
        check(record.nextWageGameTime() == 169_000L, "first paycheck is seven Minecraft days after enlistment");
        check(!PayrollClock.due(record.nextWageGameTime(), 168_999L), "new recruit does not enter a previous employer week");
        check(PayrollClock.due(record.nextWageGameTime(), 169_000L), "exact due tick included");
        record.scheduleWage(record.nextWageGameTime());
        record = WorkforceRecord.load(record.save());
        check(record.nextWageGameTime() == 337_000L, "restart keeps scheduled week rather than rescheduling");
        record.seen(200_000L);
        check(record.nextWageGameTime() == 337_000L, "chunk/dimension observation does not change payday");
        record.penalized(0L); record.penalized(0L);
        check(record.missedPayrolls() == 1, "replayed unpaid week counts once");
        record.penalized(1L); record.paid(0L);
        check(record.missedPayrolls() == 1, "payment of old arrears does not forgive a different missed week");
        record.paid(0L);
        check(record.missedPayrolls() == 1, "replayed paid week does not forgive another debt");
        record.penalized(2L); record.penalized(3L); record.rebel();
        record.markDead(); record = WorkforceRecord.load(record.save()); record.seen(400_000L); record.paid(3L);
        check(record.loyalty() == WorkforceRecord.Loyalty.REBELLIOUS, "death/reload/late payment cannot erase rebellion");
        record.defeated(); record = WorkforceRecord.load(record.save());
        check(record.loyalty() == WorkforceRecord.Loyalty.DEFEATED, "defeat survives restart without automatic rehire");
        record.rehired(500_000L);
        check(record.loyalty() == WorkforceRecord.Loyalty.LOYAL && record.nextWageGameTime() == 668_000L, "paid rehire starts a fresh seven-day contract");
        check(record.hireGeneration() == 1L, "old payroll snapshots are distinct from rehired contract");
        record.dismissed(); record = WorkforceRecord.load(record.save());
        check(!record.settlementActive() && !record.coinWageEnabled(), "dismissal stops future accrual and survives reload");
        WorkforceRecord arrears = new WorkforceRecord(UUID.randomUUID(), townId, owner, WorkforceType.RECRUIT, "recruits", type, true, 0L);
        arrears.penalized(0L); arrears.paid(1L);
        arrears = WorkforceRecord.load(arrears.save());
        check(arrears.missedPayrolls() == 1, "newer affordable period does not forgive larger old arrears");
        arrears.paid(0L);
        check(arrears.missedPayrolls() == 0, "late older arrears payment clears its own penalty after newer period paid");
        arrears.penalized(2L); arrears.paid(0L);
        check(arrears.missedPayrolls() == 1, "replayed old payment cannot clear a distinct later penalty");
        var legacyTag = arrears.save(); legacyTag.remove("UnpaidPenaltyPeriods");
        WorkforceRecord legacy = WorkforceRecord.load(legacyTag);
        legacy.initializePenaltyPeriods(List.of(2L)); legacy.paid(2L);
        check(legacy.missedPayrolls() == 0, "legacy penalty identity rebuilt from unpaid journal rosters");
        var period = new PayrollPeriod(3L, 169_000L, List.of(new PayrollPeriod.Wage(soldier, type, 20L, 1L)));
        EmployerPayroll book = new EmployerPayroll(); book.schedule(period); period.paid(UUID.randomUUID());
        book = EmployerPayroll.load(book.save());
        check(book.nextPayrollIndex() == 4L && book.payroll().get(0).paid(), "personal payroll journal retains replay/receipt state");
        check(book.payroll().get(0).roster().get(0).generation() == 1L, "saved wage binds exact hire contract");
        TownRecord town = new TownRecord(townId, owner, "Contract", new ResourceLocation("minecraft", "overworld"), new BlockPos(0, 64, 0));
        town.initializePayroll(0L); town.initializePayroll(20L);
        check(town.nextPayrollGameTime() == 168_000L, "tick-zero employer initialization is idempotent");
        town.schedule(period); town = TownRecord.load(town.save());
        check(town.payroll().get(0).paid() && town.nextPayrollIndex() == 4L, "town payroll retains authoritative receipt");
        var noop = TerritoryQuote.union(-48, -48, 48, 48, 0, 0, 0, 0);
        check(noop.addedCopper() == 0L, "inside territory quote is a no-op");
        var row = TerritoryQuote.union(-48, -48, 48, 48, 49, 0, 49, 0);
        check(row.addedCopper() == 97L, "one added block-wide column costs exact 97 copper");
        var replay = TerritoryQuote.union(row.minX(), row.minZ(), row.maxX(), row.maxZ(), 49, 0, 49, 0);
        check(replay.addedCopper() == 0L, "repeated expansion cannot charge again");
        var radius = TerritoryQuote.union(-48, -48, 48, 48, -49, -49, 49, 49);
        check(radius.addedCopper() == 392L, "legacy radius and map share inclusive area pricing");
        var extreme = TerritoryQuote.union(-48, -48, 48, 48, Integer.MIN_VALUE, 0, Integer.MIN_VALUE, 0);
        check(extreme.addedCopper() > 0L, "untrusted coordinate arithmetic does not wrap negative");
        boolean overflow = false;
        try { PayrollClock.nextDue(Long.MAX_VALUE); } catch (ArithmeticException expected) { overflow = true; }
        check(overflow, "clock overflow is rejected");
        System.out.println("Settlement economy contracts: " + assertions + " passed");
    }
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
}

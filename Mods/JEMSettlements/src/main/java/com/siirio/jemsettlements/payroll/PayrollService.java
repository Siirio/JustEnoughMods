package com.siirio.jemsettlements.payroll;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.currency.SettlementCurrency;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemsettlements.compat.recruits.RecruitReadinessAdapter;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;

public final class PayrollService {
    public static final long PERIOD_TICKS = PayrollClock.WEEK;
    private PayrollService() { }
    public static void tick(MinecraftServer server) {
        if (!com.siirio.jemsettlements.runtime.SettlementAuthority.active(server)) return;
        SettlementSavedData data = SettlementSavedData.get(server);
        long now = server.overworld().getGameTime();
        if (net.minecraftforge.fml.ModList.get().isLoaded("recruits")) com.siirio.jemsettlements.compat.recruits.RecruitRuntime.adopt(server);
        for (WorkforceRecord record : data.workforce()) {
            for (var level : server.getAllLevels()) {
                var entity = level.getEntity(record.entityId());
                if (entity != null && entity.isAlive()) { record.observeName(entity); record.verifyNativeEntityType(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), now); break; }
            }
            if (record.type() == WorkforceType.RECRUIT && data.town(record.townId()).isEmpty()) data.employer(record.ownerId());
        }
        for (var town : data.towns()) process(server, data, town.id(), town, false, now);
        for (UUID owner : data.employers()) process(server, data, owner, data.employer(owner), true, now);
        data.changed();
    }

    private static void process(MinecraftServer server, SettlementSavedData data, UUID account,
                                PayrollBook book, boolean personal, long now) {
        List<WorkforceRecord> recruits = data.workforce(account).stream()
                .filter(value -> value.type() == WorkforceType.RECRUIT)
                .sorted(Comparator.comparing(WorkforceRecord::entityId)).toList();
        for (WorkforceRecord record : recruits) record.initializePenaltyPeriods(book.payroll().stream()
                .filter(period -> period.currentCurrency() && !period.paid() && period.roster().stream().anyMatch(wage -> wage.soldierId().equals(record.entityId()) && wage.generation() == record.hireGeneration()))
                .map(PayrollPeriod::index).toList());
        for (WorkforceRecord record : recruits) if (record.nextWageGameTime() == 0L) {
            long lastDue = book.payroll().stream().filter(period -> period.roster().stream().anyMatch(wage -> wage.soldierId().equals(record.entityId())))
                    .mapToLong(PayrollPeriod::dueGameTime).max().orElse(record.enlistedGameTime());
            record.migrateWageClock(lastDue);
        }
        for (PayrollPeriod period : book.payroll()) if (!period.paid()) settle(server, data, account, book, personal, period);
        // Settle before the next week: three missed weeks stop future accrual,
        // including when catching up after an unloaded/offline interval.
        for (int batch = 0; batch < 8; batch++) {
            long due = recruits.stream().filter(WorkforceRecord::settlementActive)
                    .filter(value -> RecruitRank.of(value.nativeEntityType()) != RecruitRank.UNSUPPORTED)
                    .mapToLong(WorkforceRecord::nextWageGameTime).filter(value -> PayrollClock.due(value, now)).min().orElse(Long.MAX_VALUE);
            if (due == Long.MAX_VALUE) break;
            List<WorkforceRecord> roster = recruits.stream().filter(WorkforceRecord::settlementActive)
                    .filter(value -> RecruitRank.of(value.nativeEntityType()) != RecruitRank.UNSUPPORTED)
                    .filter(value -> value.nextWageGameTime() == due).toList();
            List<PayrollPeriod.Wage> wages = roster.stream().map(value -> new PayrollPeriod.Wage(value.entityId(), value.nativeEntityType(),
                    RecruitRank.of(value.nativeEntityType()).wage(), value.hireGeneration())).toList();
            PayrollPeriod period = new PayrollPeriod(book.nextPayrollIndex(), due, wages);
            book.schedule(period); roster.forEach(value -> value.scheduleWage(due));
            settle(server, data, account, book, personal, period);
        }
    }

    private static void settle(MinecraftServer server, SettlementSavedData data, UUID account,
                               PayrollBook book, boolean personal, PayrollPeriod period) {
        if (!period.currentCurrency()) return;
        if (period.total() <= 0L) { period.paid(null); return; }
        TransferResult result = personal ? SettlementCurrency.settleEmployerPayroll(server, account, period.total(), period.index())
                : SettlementCurrency.settleTownPayroll(server, account, period.total(), period.index());
        if (result.status() == TransferResult.Status.COMMITTED || result.status() == TransferResult.Status.REPLAYED) {
            period.paid(result.receiptId());
            for (var wage : period.roster()) data.workforceEntity(wage.soldierId()).filter(value -> value.hireGeneration() == wage.generation())
                    .ifPresent(value -> value.paid(period.index()));
            book.payrollRecovered(); return;
        }
        if (result.status() != TransferResult.Status.INSUFFICIENT_FUNDS) return;
        boolean newlyMissed = false;
        for (PayrollPeriod.Wage wage : period.roster()) {
            if (RecruitRank.of(wage.nativeType()) == RecruitRank.UNSUPPORTED) continue;
            WorkforceRecord record = data.workforceEntity(wage.soldierId()).orElse(null);
            if (record == null || record.hireGeneration() != wage.generation() || record.lastPenaltyPeriod() >= period.index()) continue;
            record.penalized(period.index()); newlyMissed = true;
            server.getAllLevels().forEach(level -> {
                var entity = level.getEntity(record.entityId());
                if (entity != null && net.minecraftforge.fml.ModList.get().isLoaded("recruits")) com.siirio.jemsettlements.compat.recruits.RecruitRuntime.penalize(entity);
            });
            if (record.missedPayrolls() >= 3) record.rebel();
        }
        if (newlyMissed) book.payrollMissed();
    }
}

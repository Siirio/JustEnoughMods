package com.siirio.jemsettlements.payroll;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemsettlements.compat.recruits.RecruitReadinessAdapter;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;

public final class PayrollService {
    public static final long PERIOD_TICKS = 168_000L;
    private PayrollService() { }
    public static void tick(MinecraftServer server) {
        SettlementSavedData data = SettlementSavedData.get(server); long now = server.overworld().getGameTime();
        for (TownRecord town : data.towns()) {
            for (WorkforceRecord record : data.workforce(town.id())) for (var level : server.getAllLevels()) {
                var entity = level.getEntity(record.entityId());
                if (entity != null && entity.isAlive()) { record.verifyNativeEntityType(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), now); break; }
            }
            List<WorkforceRecord> recruits = data.workforce(town.id()).stream().filter(value -> value.type() == WorkforceType.RECRUIT && value.coinWageEnabled() && value.settlementActive() && RecruitRank.of(value.nativeEntityType()) != RecruitRank.UNSUPPORTED).sorted(Comparator.comparing(WorkforceRecord::entityId)).toList();
            if (recruits.isEmpty()) continue; town.initializePayroll(recruits.stream().mapToLong(WorkforceRecord::enlistedGameTime).filter(value -> value > 0L).min().orElse(now));
            int created = 0; while (now >= town.nextPayrollGameTime() && created++ < 8) {
                long index = town.nextPayrollIndex(); List<PayrollPeriod.Wage> roster = recruits.stream().map(value -> new PayrollPeriod.Wage(value.entityId(), value.nativeEntityType(), RecruitRank.of(value.nativeEntityType()).wage())).toList();
                town.schedule(new PayrollPeriod(index, town.nextPayrollGameTime(), roster));
            }
            for (PayrollPeriod period : town.payroll()) if (!period.paid() && now >= period.dueGameTime()) settle(server, data, town, period);
        }
        data.changed();
    }
    private static void settle(MinecraftServer server, SettlementSavedData data, TownRecord town, PayrollPeriod period) {
        if (period.total() <= 0L) { period.paid(null); return; }
        TransferResult result = EconomyApi.settleTownPayroll(server, town.id(), period.total(), period.index());
        if (result.status() == TransferResult.Status.COMMITTED || result.status() == TransferResult.Status.REPLAYED) {
            period.paid(result.receiptId()); period.roster().forEach(wage -> data.workforceEntity(wage.soldierId()).ifPresent(value -> value.paid(period.index()))); town.payrollRecovered(); return;
        }
        if (result.status() != TransferResult.Status.INSUFFICIENT_FUNDS) return;
        boolean newlyMissed = false;
        for (PayrollPeriod.Wage wage : period.roster()) {
            WorkforceRecord record = data.workforceEntity(wage.soldierId()).orElse(null); if (record == null || record.lastPenaltyPeriod() >= period.index()) continue;
            record.penalized(period.index()); newlyMissed = true;
            server.getAllLevels().forEach(level -> { var entity = level.getEntity(record.entityId()); if (entity instanceof AbstractRecruitEntity recruit && recruit.isAlive()) RecruitReadinessAdapter.applyPayrollPenalty(recruit, 10.0F); });
            if (record.missedPayrolls() >= 3) record.rebel();
        }
        if (newlyMissed) town.payrollMissed();
    }
}

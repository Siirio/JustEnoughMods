package com.siirio.jemsettlements.compat.recruits;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.SystemAccount;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.payroll.RecruitRank;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Native hire still owns ownership, groups, caps, equipment and combat. */
public final class RecruitHiringService {
    private static final ThreadLocal<Boolean> NOBLE_HIRED = ThreadLocal.withInitial(() -> false);
    public static void beginNoble() { NOBLE_HIRED.set(false); }
    public static boolean nobleHired() { return NOBLE_HIRED.get(); }
    private static final String GENERATION = "JemCoinHireGeneration";
    private RecruitHiringService() { }
    public static long quote(AbstractRecruitEntity recruit) {
        long base = RecruitRank.of(BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType())).hiringCost();
        // Native experience represents the existing combat buffs, rather than a
        // second world-tier or equipment authority. At most three times base.
        return Math.addExact(base, Math.multiplyExact(base, Math.min(40, Math.max(0, recruit.getXpLevel() - 1))) / 20L);
    }
    public static boolean mayHire(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (!SettlementAuthority.active(player) || !recruit.isAlive() || recruit.getIsOwned() || !recruit.canBeHired()
                || recruit.level() != player.level() || SettlementAuthority.principal(player).isEmpty()
                || RecruitRank.of(BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType())) == RecruitRank.UNSUPPORTED) return false;
        var record = SettlementSavedData.get(player.getServer()).workforceEntity(recruit.getUUID()).orElse(null);
        return record == null || record.loyalty() == WorkforceRecord.Loyalty.LOYAL
                || record.loyalty() == WorkforceRecord.Loyalty.DEFEATED && record.ownerId().equals(SettlementAuthority.principal(player).orElseThrow());
    }
    /** Called after native cancellation/cap checks, immediately before ownership changes. */
    public static boolean charge(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (!mayHire(player, recruit)) return false;
        long generation = recruit.getPersistentData().getLong(GENERATION);
        UUID request = UUID.nameUUIDFromBytes(("recruitHire:" + recruit.getUUID() + ":" + generation + ":"
                + SettlementAuthority.principal(player).orElseThrow()).getBytes(StandardCharsets.UTF_8));
        var result = EconomyApi.depositToAccount(player, SystemAccount.OPERATIONS.id(), quote(recruit), request);
        if (result.status() == TransferResult.Status.COMMITTED || result.status() == TransferResult.Status.REPLAYED) return true;
        player.sendSystemMessage(Component.translatable("jem.settlements.recruit.hire_unfunded"), true);
        return false;
    }
    public static void adoptExisting(net.minecraft.server.MinecraftServer server, AbstractRecruitEntity recruit) {
        if (!com.siirio.jemsettlements.runtime.SettlementAuthority.active(server) || !recruit.getIsOwned() || recruit.getOwnerUUID() == null
                || RecruitRank.of(BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType())) == RecruitRank.UNSUPPORTED) return;
        SettlementSavedData data = SettlementSavedData.get(server);
        if (data.entityAssigned(recruit.getUUID())) return;
        ServerPlayer player = server.getPlayerList().getPlayer(recruit.getOwnerUUID());
        var principal = SettlementAuthority.principal(player);
        if (principal.isEmpty()) return;
        UUID owner = principal.get();
        UUID assignment = data.ownedTowns(owner).stream().filter(town -> town.dimension().equals(recruit.level().dimension().location()) && town.contains(recruit.blockPosition()))
                .map(town -> town.id()).findFirst().orElse(owner);
        // Existing hired troops are grandfathered, never charged a second hiring fee.
        data.add(new WorkforceRecord(recruit.getUUID(), assignment, owner, WorkforceType.RECRUIT, "recruits",
                BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType()), true, server.overworld().getGameTime()));
        if (assignment.equals(owner)) data.employer(owner);
    }

    public static void hired(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (!SettlementAuthority.active(player)) return;
        NOBLE_HIRED.set(true);
        recruit.getPersistentData().putLong(GENERATION, Math.addExact(recruit.getPersistentData().getLong(GENERATION), 1L));
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        UUID owner = SettlementAuthority.principal(player).orElseThrow();
        WorkforceRecord record = data.workforceEntity(recruit.getUUID()).orElse(null);
        if (record != null) {
            if (!record.ownerId().equals(owner)) { record.employer(owner); record.assignTown(owner); data.employer(owner); }
            record.rehired(player.getServer().overworld().getGameTime()); recruit.setNoAi(false); }
        else {
            // A personal employer uses the same SavedData/roster, never a fake town.
            UUID assignment = data.ownedTowns(owner).stream().filter(town -> town.dimension().equals(player.serverLevel().dimension().location()))
                    .min(java.util.Comparator.comparingDouble(town -> town.center().distSqr(player.blockPosition())))
                    .map(town -> town.id()).orElse(owner);
            data.add(new WorkforceRecord(recruit.getUUID(), assignment, owner, WorkforceType.RECRUIT, "recruits",
                    BuiltInRegistries.ENTITY_TYPE.getKey(recruit.getType()), true, player.getServer().overworld().getGameTime()));
            if (assignment.equals(owner)) data.employer(owner);
        }
        data.changed();
    }
}

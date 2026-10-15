package com.siirio.jemsettlements.compat.recruits;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.workforce.WorkforceRecord;
import com.siirio.jemsettlements.workforce.WorkforceType;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/** Persistent settlement-owned state layered on the native Recruits entity. */
public final class RecruitRebellionService {
    private RecruitRebellionService() {}

    public static void tick(MinecraftServer server) {
        SettlementSavedData data = SettlementSavedData.get(server);
        for (var town : data.towns()) for (WorkforceRecord record : data.workforce(town.id())) {
            if (record.type() != WorkforceType.RECRUIT || !record.living() || record.loyalty() == WorkforceRecord.Loyalty.LOYAL) continue;
            AbstractRecruitEntity recruit = null;
            for (var level : server.getAllLevels()) {
                var entity = level.getEntity(record.entityId());
                if (entity instanceof AbstractRecruitEntity found && found.isAlive()) { recruit = found; break; }
            }
            if (recruit == null) continue;
            if (record.loyalty() == WorkforceRecord.Loyalty.DEFEATED) {
                if (record.ownerId().equals(recruit.getOwnerUUID()) && recruit.getIsOwned()) {
                    recruit.setNoAi(false); recruit.setHealth(Math.max(1.0F, recruit.getHealth())); record.rehired(); data.changed();
                } else {
                    recruit.setNoAi(true); recruit.setHealth(Math.max(1.0F, recruit.getHealth())); recruit.clearTarget(); recruit.getNavigation().stop();
                }
                continue;
            }
            if (recruit.getOwnerUUID() != null) recruit.disband(null, false, true);
            recruit.setNoAi(false); recruit.setPersistenceRequired();
            ServerPlayer owner = owner(server, record.ownerId());
            if (owner == null || !owner.isAlive() || owner.level() != recruit.level()) { recruit.clearTarget(); continue; }
            recruit.setTarget(owner);
            recruit.getNavigation().moveTo(owner, 1.05D);
            if (recruit.tickCount % 20 == 0 && recruit.distanceToSqr(owner) <= 7.0D && recruit.getSensing().hasLineOfSight(owner)) recruit.doHurtTarget(owner);
        }
    }

    public static boolean interceptDefeat(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity recruit) || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getAmount() < recruit.getHealth() || player.getServer() == null) return false;
        SettlementSavedData data = SettlementSavedData.get(player.getServer());
        WorkforceRecord record = data.workforceEntity(recruit.getUUID()).orElse(null);
        if (record == null || record.loyalty() != WorkforceRecord.Loyalty.REBELLIOUS) return false;
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty() || !record.ownerId().equals(principal.get().accountId())) return false;
        event.setAmount(0.0F); recruit.setHealth(1.0F); recruit.clearTarget(); recruit.getNavigation().stop(); recruit.setNoAi(true);
        record.defeated(); data.changed();
        player.sendSystemMessage(Component.translatable("jem.settlements.recruit.defeated"), true);
        return true;
    }

    public static boolean interceptInteraction(ServerPlayer player, AbstractRecruitEntity recruit) {
        WorkforceRecord record = SettlementSavedData.get(player.getServer()).workforceEntity(recruit.getUUID()).orElse(null);
        if (record == null || record.loyalty() == WorkforceRecord.Loyalty.LOYAL) return false;
        var principal = IdentityPolicy.resolve(player);
        if (record.loyalty() == WorkforceRecord.Loyalty.DEFEATED && principal.isPresent() && record.ownerId().equals(principal.get().accountId())) {
            recruit.openHireGUI(player);
        } else player.sendSystemMessage(Component.translatable("jem.settlements.recruit.hostile"), true);
        return true;
    }

    public static boolean openBuyback(ServerPlayer player, UUID entityId) {
        WorkforceRecord record = SettlementSavedData.get(player.getServer()).workforceEntity(entityId).orElse(null);
        if (record == null || record.loyalty() != WorkforceRecord.Loyalty.DEFEATED) return false;
        for (var level : player.getServer().getAllLevels()) {
            var entity = level.getEntity(entityId);
            if (entity instanceof AbstractRecruitEntity recruit && recruit.isAlive()
                    && player.level() == recruit.level() && player.distanceToSqr(recruit) <= 256.0D) {
                recruit.openHireGUI(player); return true;
            }
        }
        return false;
    }

    private static ServerPlayer owner(MinecraftServer server, UUID accountId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var principal = IdentityPolicy.resolve(player);
            if (principal.isPresent() && principal.get().accountId().equals(accountId)) return player;
        }
        return null;
    }
}

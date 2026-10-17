package com.siirio.jemsettlements.compat.recruits;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.network.SettlementNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
/** Registered only when Recruits exists; native signatures never enter the common subscriber. */
public final class RecruitRuntime {
    @SubscribeEvent public static void dismissed(com.talhanation.recruits.RecruitEvent.Dismissed event) {
        if (!(event.getRecruit().level() instanceof ServerLevel level) || !SettlementAuthority.active(level.getServer())) return;
        var data = SettlementSavedData.get(level.getServer());
        data.workforceEntity(event.getRecruit().getUUID()).ifPresent(record -> { record.dismissed(); data.changed(); });
    }
    @SubscribeEvent public static void opened(net.minecraftforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player && SettlementAuthority.active(player)
                && event.getContainer() instanceof com.talhanation.recruits.inventory.RecruitHireMenu menu)
            SettlementNetwork.sendHireQuote(player, menu.getRecruitEntity());
    }

    @SubscribeEvent public static void refreshHireQuote(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END || event.getServer().getTickCount()%20!=0 || !SettlementAuthority.active(event.getServer()))return;
        for(ServerPlayer player:event.getServer().getPlayerList().getPlayers())
            if(player.containerMenu instanceof com.talhanation.recruits.inventory.RecruitHireMenu menu)
                SettlementNetwork.sendHireQuote(player,menu.getRecruitEntity());
    }

    @SubscribeEvent public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && SettlementAuthority.active(player)
            && event.getTarget() instanceof com.talhanation.recruits.entities.AbstractRecruitEntity recruit
            && RecruitRebellionService.interceptInteraction(player, recruit)) event.setCanceled(true);
    }
    @SubscribeEvent public static void damage(net.minecraftforge.event.entity.living.LivingDamageEvent event) { RecruitRebellionService.interceptDefeat(event); }
    public static void adopt(net.minecraft.server.MinecraftServer server) {
        for (var level : server.getAllLevels()) for (var entity : level.getAllEntities())
            if (entity instanceof com.talhanation.recruits.entities.AbstractRecruitEntity recruit && recruit.isAlive()) RecruitHiringService.adoptExisting(server, recruit);
    }
    public static void penalize(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof com.talhanation.recruits.entities.AbstractRecruitEntity recruit && recruit.isAlive()) RecruitReadinessAdapter.applyPayrollPenalty(recruit, 10.0F);
    }
    public static boolean openManagement(ServerPlayer player, net.minecraft.world.entity.Entity entity, java.util.UUID account) {
        if (!(entity instanceof com.talhanation.recruits.entities.AbstractRecruitEntity recruit) || !RecruitsAdapter.ownedBy(entity, account)) return false;
        recruit.openGUI(player); return true;
    }

}

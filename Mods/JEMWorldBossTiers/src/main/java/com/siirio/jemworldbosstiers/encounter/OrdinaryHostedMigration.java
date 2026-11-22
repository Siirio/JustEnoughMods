package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.api.WorldTierApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One-time tolerant removal of retired ordinary hosted-combat state. */
@Mod.EventBusSubscriber(modid = JemWorldBossTiers.MOD_ID)
public final class OrdinaryHostedMigration {
    private OrdinaryHostedMigration() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof LivingEntity boss)
                || WorldTierApi.profile(boss).isEmpty()) return;
        var data = boss.getPersistentData();
        if (data.contains("JEMHostedEncounter")) HostedEncounters.restoreNative(boss);
        HostedLobby.release(boss);
        HostedGroup.release(boss);
        data.remove("JEMHostedEncounter");
        data.remove("JEMHostedLobby");
        data.remove("JEMHostedBoss");
        data.remove("JEMHostedSupportArena");
        data.remove("JEMHostedAttempt");
        data.remove("JEMHostedBarrier");
    }
}

package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class BloodMoonSpawnGuard {
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void entity(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && EventHooks.activeBloodMoonSpawn(level, event.getEntity()))
            event.setCanceled(false);
    }
}

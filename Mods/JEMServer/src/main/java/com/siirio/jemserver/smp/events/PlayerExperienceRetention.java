package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class PlayerExperienceRetention {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drops(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && EventSession.forPlayer(player) == null)
            event.setDroppedExperience(0);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getOriginal() instanceof ServerPlayer original)
                || EventSession.forPlayer(original) != null) return;
        event.getEntity().experienceProgress = original.experienceProgress;
        event.getEntity().experienceLevel = original.experienceLevel;
        event.getEntity().totalExperience = original.totalExperience;
    }

    private PlayerExperienceRetention() {}
}

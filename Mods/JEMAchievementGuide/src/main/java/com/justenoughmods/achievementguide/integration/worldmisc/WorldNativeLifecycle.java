package com.justenoughmods.achievementguide.integration.worldmisc;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_advancements")
public final class WorldNativeLifecycle {
    private WorldNativeLifecycle() {}
    @SubscribeEvent public static void damage(LivingDamageEvent event) {
        if (!event.isCanceled() && event.getAmount() > 0) WorldOceanObservation.damaged(event.getEntity(), event.getSource().getEntity());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer actor) { WorldOceanObservation.clear(actor); WorldMaelstromProof.clear(actor); WorldHungerGuardProof.clear(actor); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer actor) { WorldMaelstromProof.restore(actor); WorldHungerGuardProof.restore(actor); }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { WorldOceanObservation.clear(); WorldMaelstromProof.clear(); WorldBambooProof.clear(); WorldHungerGuardProof.clear(); }
}

package com.justenoughmods.achievementguide.integration.nature;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class NatureSessions {
    private NatureSessions() {}
    private static void clear(Entity entity) {
        if (entity instanceof ServerPlayer) {
            NatureHarvestProof.reset(entity.getUUID());
            LotusMovementProof.reset(entity.getUUID());
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { clear(event.getOriginal()); clear(event.getEntity()); }
    @SubscribeEvent public static void death(LivingDeathEvent event) { clear(event.getEntity()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { NatureHarvestProof.clear(); LotusMovementProof.clear(); }
}

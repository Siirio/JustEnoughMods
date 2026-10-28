package com.justenoughmods.achievementguide.integration.photo;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_advancements", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PhotoLifecycle {
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if (ModList.get().isLoaded("exposure")) {
            PhotoImages.clear(event.getServer()); PhotoCameraProof.clear(); PhotoCraftProof.clear(); PhotoPrintingProof.clear();
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ModList.get().isLoaded("exposure")) PhotoImages.logout(event.getEntity().getUUID());
    }
    private PhotoLifecycle() {}
}

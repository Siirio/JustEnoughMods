package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.integration.BackpackCaptureProof;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

public final class BookBackpackActions {
    private BookBackpackActions() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookBackpackActions.class); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var proof = event.getOriginal().getPersistentData().get(BackpackCaptureProof.KEY);
        if (proof != null) event.getEntity().getPersistentData().put(BackpackCaptureProof.KEY, proof.copy());
    }
    @SubscribeEvent public static void stopped(ServerStoppingEvent event) { BackpackCaptureProof.clear(); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { BackpackCaptureProof.clear(); }
}

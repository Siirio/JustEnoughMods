package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.integration.NativeTerminalExtractionEvent;
import com.justenoughmods.achievementguide.integration.StorageControllerProof;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Linked only when both native storage owners are installed. */
public final class BookControllerActions {
    private BookControllerActions() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookControllerActions.class); }
    @SubscribeEvent public static void extracted(NativeTerminalExtractionEvent event) { StorageControllerProof.extracted(event); }
}

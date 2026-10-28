package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.integration.NativeTerminalExtractionEvent;
import com.justenoughmods.achievementguide.integration.TomsStockSignalProof;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BookTerminalActions {
    private BookTerminalActions() {}
    static void register() { MinecraftForge.EVENT_BUS.register(BookTerminalActions.class); }
    @SubscribeEvent public static void extracted(NativeTerminalExtractionEvent event) { TomsStockSignalProof.extracted(event); }
}

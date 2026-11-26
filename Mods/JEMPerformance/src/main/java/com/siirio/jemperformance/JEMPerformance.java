package com.siirio.jemperformance;

import com.mapsyncer.platform.PlatformManager;
import com.mapsyncer.platform.UpdateMode;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(JEMPerformance.MOD_ID)
public final class JEMPerformance {
    public static final String MOD_ID = "jem_performance";

    private final SavedRegionPublisher publisher = new SavedRegionPublisher();

    public JEMPerformance() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerStarting(ServerStartingEvent event) {
        if (PlatformManager.getPlatform().getIncrementalUpdateMode() == UpdateMode.TICK) {
            PlatformManager.getPlatform().setIncrementalUpdateIntervalTicks(2400);
            PlatformManager.getPlatform().saveConfig();
        }
        publisher.start(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        publisher.tick(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        publisher.stop(event.getServer());
    }

}

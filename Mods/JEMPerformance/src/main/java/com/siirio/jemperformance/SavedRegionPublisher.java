package com.siirio.jemperformance;

import com.mapsyncer.server.ConversionOrchestrator;
import com.mapsyncer.server.ServerSyncHandlerLogic;
import com.mapsyncer.platform.PlatformManager;
import com.mapsyncer.platform.UpdateMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scans persisted MCA files only. Native Minecraft saves remain the sole world-data writer. */
final class SavedRegionPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SavedRegionPublisher.class);

    private MinecraftServer server;
    private ExecutorService worker;
    private Future<?> scan;
    private int elapsedTicks;

    void start(MinecraftServer currentServer) {
        stop(server);
        server = currentServer;
        elapsedTicks = 0;
        worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "jem-saved-region-scan");
            thread.setDaemon(true);
            return thread;
        });
    }

    void tick(MinecraftServer currentServer) {
        if (currentServer != server || worker == null) return;
        if (PlatformManager.getPlatform().getIncrementalUpdateMode() != UpdateMode.TICK) return;
        int interval = PlatformManager.getPlatform().getIncrementalUpdateIntervalTicks();
        if (elapsedTicks < interval) elapsedTicks++;
        if (elapsedTicks < interval) return;
        if (currentServer.getPlayerList().getPlayerCount() == 0) return;
        if (scan != null && !scan.isDone()) return;
        if (ConversionOrchestrator.isRunning() || ServerSyncHandlerLogic.isSyncActive()) return;

        elapsedTicks = 0;
        scan = worker.submit(() -> {
            try {
                ConversionOrchestrator.performIncrementalScan(currentServer);
            } catch (RuntimeException failure) {
                LOGGER.error("Saved-region map scan failed; the next interval will retry changed regions", failure);
            }
        });
    }

    void stop(MinecraftServer currentServer) {
        if (currentServer == null || currentServer != server) return;
        if (worker != null) worker.shutdownNow();
        worker = null;
        scan = null;
        server = null;
        elapsedTicks = 0;
    }
}

package com.siirio.jemserver.mixin.xaero.shared;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import obscura.unified.client.ClientBridge;
import obscura.unified.client.MapLocation;
import obscura.unified.client.SyncLimits;
import obscura.unified.client.SyncWorker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMapSession;

@Pseudo
@Mixin(targets = "obscura.unified.client.SyncSession", remap = false)
public abstract class SharedMapSyncMixin {
    @Shadow @Final private static long WORKER_PASS_MILLIS;
    @Shadow @Final private ClientBridge bridge;
    @Shadow @Final private SyncWorker worker;
    @Shadow private volatile boolean closed;
    @Shadow private volatile boolean connected;
    @Shadow private volatile boolean syncEnabled;
    @Shadow private volatile boolean sharedMapMode;
    @Shadow private volatile SyncLimits limits;
    @Shadow private volatile long nextReconcileAtMillis;
    @Shadow private long dimensionSettledAt;
    @Shadow protected abstract void runReconcile(String dimension);

    @Unique private final Map<String, Long> jem$reconciledAt = new HashMap<>();
    @Unique private volatile boolean jem$reconcileQueued;
    @Unique private long jem$lastSharedPass;

    @Redirect(method = "runReconcile", at = @At(value = "INVOKE", target = "Lobscura/unified/client/ClientBridge;currentLocation()Lobscura/unified/client/MapLocation;"))
    private MapLocation jem$dimensionLocation(ClientBridge access, String dimension) {
        return access.locationFor(dimension);
    }

    @Redirect(method = "runReconcile", at = @At(value = "FIELD", target = "Lobscura/unified/client/SyncSession;nextReconcileAtMillis:J", opcode = Opcodes.PUTFIELD))
    private void jem$currentDimensionSchedule(@Coerce Object session, long next, String dimension) {
        if (dimension.equals(bridge.currentDimensionId())) nextReconcileAtMillis = next;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jem$sharedDimensions(long now, CallbackInfo callback) {
        if (closed || !connected || !syncEnabled || !sharedMapMode || jem$reconcileQueued
                || now < dimensionSettledAt || now - jem$lastSharedPass < WORKER_PASS_MILLIS) return;
        var connection = Minecraft.getInstance().getConnection();
        var session = WorldMapSession.getCurrentSession();
        if (connection == null || session == null || !session.isUsable()) return;
        var world = session.getMapProcessor().getMapWorld();
        if (world == null) return;
        String current = bridge.currentDimensionId();
        for (var key : connection.levels()) {
            String dimension = key.location().toString();
            if (dimension.equals(current)) continue;
            Long previous = jem$reconciledAt.get(dimension);
            if (previous != null && now - previous < limits.reconcileIntervalMillis()) continue;
            if (world.getDimension(key) == null) world.createDimensionUnsynced(key);
            var location = bridge.locationFor(dimension);
            if (location == null || !location.isComplete()) continue;
            jem$reconcileQueued = true;
            if (worker.submit(() -> jem$reconcile(dimension))) {
                jem$reconciledAt.put(dimension, now);
                jem$lastSharedPass = now;
            } else {
                jem$reconcileQueued = false;
            }
            return;
        }
    }

    @Unique
    private void jem$reconcile(String dimension) {
        try {
            if (!closed && connected && syncEnabled && sharedMapMode) runReconcile(dimension);
        } finally {
            jem$reconcileQueued = false;
        }
    }
}

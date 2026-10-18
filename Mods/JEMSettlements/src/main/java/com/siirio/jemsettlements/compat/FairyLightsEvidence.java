package com.siirio.jemsettlements.compat;

import me.paulf.fairylights.server.capability.CapabilityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Read actual native outgoing connections, never light items or bare fasteners. */
public final class FairyLightsEvidence {
    private FairyLightsEvidence() { }
    public static boolean decorated(ServerLevel level, BlockPos pos) {
        var blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getCapability(CapabilityHandler.FASTENER_CAP).map(fastener ->
                fastener.getOwnConnections().stream().limit(32).anyMatch(connection -> !connection.isRemoved()
                        && !connection.isDynamic() && connection.getDestination().get(level, false).isPresent())).orElse(false);
    }
}

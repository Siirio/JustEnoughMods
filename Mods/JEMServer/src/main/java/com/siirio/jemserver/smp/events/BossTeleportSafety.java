package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BossTeleportSafety {
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    public static Decision validate(ServerPlayer player, ServerLevel level, Vec3 requested) {
        if (APPLYING.get() || player.hasPermissions(2)) return Decision.allow(requested);
        return BloodMoonSolidBoundary.validateTeleport(player, level, requested);
    }

    public static Decision validate(Entity entity, ServerLevel level, Vec3 requested) {
        if (entity instanceof ServerPlayer player) return validate(player, level, requested);
        return APPLYING.get() || SolidBoundary.permitsTeleport(entity, level, requested)
                ? Decision.allow(requested) : Decision.redirect(null);
    }

    public static boolean permitsDimensionChange(Entity entity, ServerLevel level) {
        if (APPLYING.get() || entity instanceof ServerPlayer player && player.hasPermissions(2)) return true;
        if (entity instanceof ServerPlayer player) {
            Decision decision = validate(player, level, player.position());
            return !decision.matched() || player.position().equals(decision.destination());
        }
        return !SolidBoundary.contained(entity);
    }

    public static void apply(ServerPlayer player, ServerLevel level, Vec3 destination, float yaw, float pitch) {
        APPLYING.set(true);
        try {
            player.teleportTo(level, destination.x, destination.y, destination.z, yaw, pitch);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
        } finally {
            APPLYING.remove();
        }
    }

    public record Decision(boolean matched, Vec3 destination) {
        public static Decision unmatched() { return new Decision(false, null); }
        public static Decision allow(Vec3 destination) { return new Decision(true, destination); }
        public static Decision redirect(Vec3 destination) { return new Decision(true, destination); }
    }

    private BossTeleportSafety() {}
}

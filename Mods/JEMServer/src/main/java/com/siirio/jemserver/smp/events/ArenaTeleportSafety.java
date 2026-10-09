package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ArenaTeleportSafety {
    private static final ThreadLocal<Boolean> REDIRECTING=ThreadLocal.withInitial(()->false);

    public static Decision validate(ServerPlayer player,ServerLevel level,Vec3 requested) {
        if(REDIRECTING.get()) return Decision.allow(requested);
        Decision hosted=HostedBoundary.validateTeleport(player,level,requested);
        return hosted.matched()?hosted:StructureStaging.validateTeleport(player,level,requested);
    }

    public static void redirect(ServerPlayer player,ServerLevel level,Vec3 destination,float yaw,float pitch) {
        REDIRECTING.set(true);
        try {
            player.teleportTo(level,destination.x,destination.y,destination.z,yaw,pitch);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance=0;
        } finally {
            REDIRECTING.remove();
        }
    }

    public record Decision(boolean matched,Vec3 destination) {
        public static Decision unmatched() {
            return new Decision(false,null);
        }

        public static Decision allow(Vec3 requested) {
            return new Decision(true,requested);
        }

        public static Decision redirect(Vec3 destination) {
            return new Decision(true,destination);
        }
    }

    private ArenaTeleportSafety() {}
}

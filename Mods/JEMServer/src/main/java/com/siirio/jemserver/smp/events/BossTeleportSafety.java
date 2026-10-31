package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpEnvironment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BossTeleportSafety {
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    public static Decision validate(ServerPlayer player, ServerLevel level, Vec3 requested) {
        if (!SmpEnvironment.active(level.getServer()) || APPLYING.get() || player.hasPermissions(2)) return Decision.allow(requested);
        Decision structure = StructureStaging.safeTeleport(player, level, requested);
        return structure.matched() ? structure : BloodMoonSolidBoundary.validateTeleport(player, level, requested);
    }

    public static Decision validate(Entity entity, ServerLevel level, Vec3 requested) {
        if (!SmpEnvironment.active(level.getServer())) return Decision.allow(requested);
        if (entity instanceof ServerPlayer player) return validate(player, level, requested);
        return APPLYING.get() || SolidBoundary.permitsTeleport(entity, level, requested)
                ? Decision.allow(requested) : Decision.redirect(null);
    }

    public static boolean permitsDimensionChange(Entity entity, ServerLevel level) {
        if (!SmpEnvironment.active(level.getServer())) return true;
        if (APPLYING.get() || entity instanceof ServerPlayer player && player.hasPermissions(2)) return true;
        return !SolidBoundary.contained(entity);
    }

    public static Decision validatePosition(Entity entity,ServerLevel level,Vec3 requested) {
        if(!SmpEnvironment.active(level.getServer()) || APPLYING.get() || entity instanceof ServerPlayer player && player.hasPermissions(2)
                || StructureStaging.canonicalPlacement(entity,level,requested)) return Decision.allow(requested);
        var source=SolidBoundary.movementBox(entity);
        var target=SolidBoundary.destinationBox(entity,requested);
        return SolidBoundary.permitsMovement(entity,source,target) ? Decision.allow(requested) : Decision.redirect(null);
    }

    public static void apply(ServerPlayer player, ServerLevel level, Vec3 destination, float yaw, float pitch) {
        boolean previous=APPLYING.get();
        APPLYING.set(true);
        try {
            if(player.isPassenger()) player.stopRiding();
            if(player.isVehicle()) player.ejectPassengers();
            player.teleportTo(level, destination.x, destination.y, destination.z, yaw, pitch);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
        } finally {
            if(previous) APPLYING.set(true); else APPLYING.remove();
        }
    }

    /** Server-internal same-dimension ejection. Preserve the entire existing passenger family. */
    static boolean apply(Entity entity,ServerLevel level,Vec3 destination) {
        if(entity.level()!=level || !level.noCollision(entity,SolidBoundary.destinationBox(entity,destination))) return false;
        Entity root=entity.getRootVehicle();
        var family=root.getSelfAndPassengers().toList();
        Vec3 delta=destination.subtract(entity.position());
        boolean previous=APPLYING.get();
        APPLYING.set(true);
        try {
            for(Entity member:family) {
                Vec3 target=member.position().add(delta);
                if(member instanceof ServerPlayer player) player.connection.teleport(target.x,target.y,target.z,player.getYRot(),player.getXRot());
                else member.teleportTo(target.x,target.y,target.z);
                member.setDeltaMovement(Vec3.ZERO);
                member.fallDistance=0;
            }
            return entity.position().distanceToSqr(destination)<1.0E-6;
        } finally {
            if(previous) APPLYING.set(true); else APPLYING.remove();
        }
    }

    public record Decision(boolean matched, Vec3 destination) {
        public static Decision unmatched() { return new Decision(false, null); }
        public static Decision allow(Vec3 destination) { return new Decision(true, destination); }
        public static Decision redirect(Vec3 destination) { return new Decision(true, destination); }
    }

    private BossTeleportSafety() {}
}

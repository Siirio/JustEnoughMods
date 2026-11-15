package com.siirio.jemcompat.compat.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class RollClearance {
    private static final double MAX_HORIZONTAL_ADJUSTMENT = 0.5D;
    private static final double COLLISION_EPSILON = 1.0E-7D;

    private RollClearance() {}

    public static void restore(Player player) {
        AABB compactBox = player.getBoundingBox();
        player.refreshDimensions();
        if (!player.isAlive() || player.noPhysics || player.isPassenger()
                || player.level().isClientSide && !player.isLocalPlayer()) {
            return;
        }

        Pose nativePose = player.getPose();
        AABB nativeBox = player.getBoundingBox();
        if (!hasLoadedCollisionArea(player.level(), nativeBox) || fits(player, nativeBox)) {
            return;
        }

        if (player.getForcedPose() == null) {
            // Player.updatePlayerPose can resume the ordinary pose once it fits again.
            if (tryPose(player, Pose.CROUCHING) || tryPose(player, Pose.SWIMMING)) {
                return;
            }
            player.setPose(nativePose);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            moveToClearance(serverPlayer, compactBox, nativeBox);
        }
    }

    private static boolean tryPose(Player player, Pose pose) {
        if (player.getPose() == pose) {
            return fits(player, player.getBoundingBox());
        }
        AABB proposed = player.getDimensions(pose).makeBoundingBox(player.position());
        if (!hasLoadedCollisionArea(player.level(), proposed) || !fits(player, proposed)) {
            return false;
        }
        player.setPose(pose);
        return fits(player, player.getBoundingBox());
    }

    private static void moveToClearance(ServerPlayer player, AABB compactBox, AABB nativeBox) {
        Level level = player.level();
        Vec3 center = nativeBox.getCenter();
        double width = nativeBox.getXsize();
        double height = nativeBox.getYsize();
        double depth = nativeBox.getZsize();
        AABB candidateCenters = new AABB(center, center).inflate(MAX_HORIZONTAL_ADJUSTMENT, COLLISION_EPSILON,
                MAX_HORIZONTAL_ADJUSTMENT);
        AABB requiredArea = candidateCenters.inflate(width, height, depth);
        if (!hasLoadedCollisionArea(level, requiredArea)) {
            return;
        }
        var target = level.findFreePosition(player, Shapes.create(candidateCenters), center, width, height, depth);
        if (target.isEmpty()) {
            return;
        }

        Vec3 point = target.get();
        Vec3 movement = new Vec3(point.x - center.x, 0.0D, point.z - center.z);
        if (movement.lengthSqr() <= COLLISION_EPSILON * COLLISION_EPSILON
                || movement.lengthSqr() > MAX_HORIZONTAL_ADJUSTMENT * MAX_HORIZONTAL_ADJUSTMENT
                || !fits(player, nativeBox.move(movement))) {
            return;
        }

        if (!canMoveThrough(player, compactBox, movement) || !canMoveThrough(player, nativeBox, movement)) {
            return;
        }
        Vec3 destination = player.position().add(movement);
        // An arena boundary can reject the server position change.
        player.setPos(destination.x, destination.y, destination.z);
        if (player.position().equals(destination)) {
            player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        }
    }

    private static boolean canMoveThrough(ServerPlayer player, AABB box, Vec3 movement) {
        Level level = player.level();
        AABB path = box.expandTowards(movement);
        Vec3 allowed = Entity.collideBoundingBox(player, movement, box, level, level.getEntityCollisions(player, path));
        return movement.distanceToSqr(allowed) <= COLLISION_EPSILON * COLLISION_EPSILON;
    }

    private static boolean fits(Player player, AABB box) {
        Level level = player.level();
        return box.minX >= level.getWorldBorder().getMinX() && box.maxX <= level.getWorldBorder().getMaxX()
                && box.minZ >= level.getWorldBorder().getMinZ() && box.maxZ <= level.getWorldBorder().getMaxZ()
                && level.noCollision(player, box.deflate(COLLISION_EPSILON));
    }

    private static boolean hasLoadedCollisionArea(Level level, AABB box) {
        return level.hasChunksAt(Mth.floor(box.minX) - 1, Mth.floor(box.minZ) - 1,
                Mth.floor(box.maxX) + 1, Mth.floor(box.maxZ) + 1);
    }
}

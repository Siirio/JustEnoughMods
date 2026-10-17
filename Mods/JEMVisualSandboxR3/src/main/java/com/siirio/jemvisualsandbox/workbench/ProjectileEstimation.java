package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared zero-spread launch and bounded native-collision prediction; creates no world entity. */
public final class ProjectileEstimation {
    private static final double GRAVITY = (double) .03F;
    private static final double AIR_DRAG = (double) .99F;
    private static final double WATER_DRAG = (double) .8F;
    public record Prediction(List<Vec3> points, HitResult impact, boolean complete) {
        public Prediction { points = List.copyOf(points); }
    }
    private ProjectileEstimation() {}

    public static Vec3 initialPosition(Player owner) {
        return new Vec3(owner.getX(), owner.getEyeY() - (double) .1F, owner.getZ());
    }

    /** Same float trig, normalized launch and inherited movement as shootFromRotation(…,1.5,0). */
    public static Vec3 initialVelocity(Player owner) {
        float radians = (float) Math.PI / 180F;
        float x = -Mth.sin(owner.getYRot() * radians) * Mth.cos(owner.getXRot() * radians);
        float y = -Mth.sin(owner.getXRot() * radians);
        float z = Mth.cos(owner.getYRot() * radians) * Mth.cos(owner.getXRot() * radians);
        Vec3 motion = owner.getDeltaMovement();
        return new Vec3(x, y, z).normalize().scale(1.5).add(motion.x, owner.onGround() ? 0 : motion.y, motion.z);
    }

    public static void launch(ThrownEnderpearl pearl, Player owner) {
        pearl.setPos(initialPosition(owner));
        pearl.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0, 1.5F, 0);
    }

    /** Reuse the detached probe; never tick it or add it to a level. */
    public static Prediction predict(Player owner, ThrownEnderpearl probe, int requestedTicks) {
        Level level = owner.level();
        Vec3 point = initialPosition(owner), velocity = initialVelocity(owner);
        List<Vec3> points = new ArrayList<>();
        points.add(point);
        boolean leftOwner = false;
        int limit = Mth.clamp(requestedTicks, 1, 160);
        for (int tick = 0; tick < limit; tick++) {
            Vec3 next = point.add(velocity);
            if (!level.hasChunkAt(BlockPos.containing(next)) || next.y < level.getMinBuildHeight() - 16) break;
            probe.setPos(point);
            probe.setDeltaMovement(velocity);
            if (!leftOwner) leftOwner = level.getEntities(probe,
                    probe.getBoundingBox().expandTowards(velocity).inflate(1),
                    entity -> !entity.isSpectator() && entity.isPickable()).stream()
                    .noneMatch(entity -> entity.getRootVehicle() == owner.getRootVehicle());
            final boolean mayHitOwner = leftOwner;
            // Native swept box (+1), truncated block ray, target boxes (+0.3), and closest hit.
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(probe,
                    entity -> entity.canBeHitByProjectile()
                            && (mayHitOwner || !owner.isPassengerOfSameVehicle(entity)));
            if (hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getLocation());
                return new Prediction(points, hit, true);
            }
            // Native throwable drag samples its CURRENT box before moving to next.
            double drag = touchesWater(level, probe.getBoundingBox()) ? WATER_DRAG : AIR_DRAG;
            velocity = drag == AIR_DRAG ? airVelocityAfterTick(velocity) : velocity.scale(drag).add(0, -GRAVITY, 0);
            point = next;
            points.add(point);
        }
        return new Prediction(points, null, false);
    }

    /** Native dry-air throwable step, also used by the GUI's detached demonstration. */
    public static Vec3 airVelocityAfterTick(Vec3 velocity) {
        return velocity.scale(AIR_DRAG).add(0, -GRAVITY, 0);
    }

    private static boolean touchesWater(Level level, AABB box) {
        AABB interior = box.deflate(.001);
        for (BlockPos position : BlockPos.betweenClosed(Mth.floor(interior.minX), Mth.floor(interior.minY), Mth.floor(interior.minZ),
                Mth.ceil(interior.maxX) - 1, Mth.ceil(interior.maxY) - 1, Mth.ceil(interior.maxZ) - 1)) {
            var fluid = level.getFluidState(position);
            if (fluid.is(FluidTags.WATER) && position.getY() + fluid.getHeight(level, position) >= interior.minY) return true;
        }
        return false;
    }
}

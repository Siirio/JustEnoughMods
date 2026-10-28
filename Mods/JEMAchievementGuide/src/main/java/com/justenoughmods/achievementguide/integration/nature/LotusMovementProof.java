package com.justenoughmods.achievementguide.integration.nature;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** One physically connected native Lotus ride per actor, never a simulated lift. */
public final class LotusMovementProof {
    public record Probe(ServerLevel level, BlockPos head) {}
    public record Step(Probe probe, ServerPlayer actor, Vec3 before, Vec3 after) {}
    public record Descent(Probe probe, ServerPlayer actor, Vec3 before) {}
    private static final class Ride {
        final ServerPlayer actor;
        final ServerLevel level;
        final double beganY;
        BlockPos head;
        double nativeRise;
        Vec3 descendingFrom;
        Ride(Step step) {
            actor = step.actor; level = step.probe.level; beganY = step.before.y; head = step.probe.head;
        }
    }
    private static final Map<UUID, Ride> rides = new HashMap<>();
    private LotusMovementProof() {}

    public static Probe probe(ServerLevel level, BlockPos head) {
        return NatureNativeFacts.block(level.getBlockState(head), "lotus_flower") ? new Probe(level, head.immutable()) : null;
    }

    private static boolean standing(ServerPlayer actor, Probe probe) {
        if (probe == null || !NatureActors.eligible(actor) || actor.level() != probe.level || !actor.onGround() || actor.isPassenger()) return false;
        var box = actor.getBoundingBox();
        // Exact native flower collision surface is 1.5/16 high, with X/Z bounds2..14/16.
        return Math.abs(actor.getY() - (probe.head.getY() + 1.5 / 16.0)) <= 0.01
                && box.maxX > probe.head.getX() + 2.0 / 16.0 && box.minX < probe.head.getX() + 14.0 / 16.0
                && box.maxZ > probe.head.getZ() + 2.0 / 16.0 && box.minZ < probe.head.getZ() + 14.0 / 16.0;
    }

    public static Step move(Player player, MoverType type, Vec3 requested, Probe probe) {
        boolean rider = player instanceof ServerPlayer actor && standing(actor, probe) && type == MoverType.SHULKER_BOX;
        Vec3 before = player.position();
        player.move(type, requested);
        Vec3 after = player.position();
        return rider && after.y > before.y && Double.isFinite(after.x) && Double.isFinite(after.y) && Double.isFinite(after.z)
                ? new Step(probe, (ServerPlayer) player, before, after) : null;
    }

    public static void grew(Step step) {
        if (step == null || !NatureActors.eligible(step.actor) || step.actor.serverLevel() != step.probe.level
                || step.actor.position().distanceToSqr(step.after) > 1.0E-8
                || !NatureNativeFacts.block(step.probe.level.getBlockState(step.probe.head), "lotus_stem")
                || !NatureNativeFacts.block(step.probe.level.getBlockState(step.probe.head.above()), "lotus_flower")) return;
        Ride ride = rides.get(step.actor.getUUID());
        if (ride == null || ride.actor != step.actor || ride.level != step.probe.level || !ride.head.equals(step.probe.head)
                || ride.descendingFrom != null) ride = new Ride(step);
        ride.nativeRise += step.after.y - step.before.y;
        ride.head = step.probe.head.above();
        rides.put(step.actor.getUUID(), ride);
    }

    public static Descent descending(ServerLevel level, BlockPos head, Player selectedByNativeOwner) {
        Probe probe = probe(level, head);
        if (!(selectedByNativeOwner instanceof ServerPlayer actor) || !standing(actor, probe) || !actor.isShiftKeyDown()) return null;
        Ride ride = rides.get(actor.getUUID());
        return ride != null && ride.actor == actor && ride.level == level && ride.head.equals(head)
                && ride.nativeRise >= 2 && actor.getY() - ride.beganY >= 2
                ? new Descent(probe, actor, actor.position()) : null;
    }

    public static void descended(Descent descent) {
        if (descent == null || !NatureActors.eligible(descent.actor)) return;
        Ride ride = rides.get(descent.actor.getUUID());
        if (ride == null || ride.actor != descent.actor || ride.level != descent.probe.level
                || !ride.head.equals(descent.probe.head) || descent.actor.position().distanceToSqr(descent.before) > 1.0E-8
                || NatureNativeFacts.block(ride.level.getBlockState(ride.head), "lotus_flower")
                || !NatureNativeFacts.block(ride.level.getBlockState(ride.head.below()), "lotus_flower")) return;
        ride.head = ride.head.below();
        ride.descendingFrom = descent.before;
    }

    public static void acceptedMovement(ServerPlayer actor, Vec3 from) {
        Ride ride = rides.get(actor.getUUID());
        if (ride == null) return;
        var box = actor.getBoundingBox();
        boolean connected = NatureActors.eligible(actor) && ride.actor == actor && ride.level == actor.level() && !actor.isPassenger()
                && ride.level.getChunkSource().getChunkNow(ride.head.getX() >> 4, ride.head.getZ() >> 4) != null
                && NatureNativeFacts.block(ride.level.getBlockState(ride.head), "lotus_flower")
                && box.maxX > ride.head.getX() + 2.0 / 16.0 && box.minX < ride.head.getX() + 14.0 / 16.0
                && box.maxZ > ride.head.getZ() + 2.0 / 16.0 && box.minZ < ride.head.getZ() + 14.0 / 16.0;
        if (!connected) { reset(actor.getUUID()); return; }
        if (ride.descendingFrom != null) {
            if (ride.descendingFrom.distanceToSqr(from) > 1.0E-8 || actor.getY() > from.y
                    || actor.position().distanceToSqr(from) - actor.getDeltaMovement().lengthSqr() > 100.0) {
                reset(actor.getUUID());
            } else if (actor.getY() < from.y) {
                NatureActors.emit(actor, "v3_world_lotus_lift");
                reset(actor.getUUID());
            }
        } else if (actor.getY() < ride.head.getY() || actor.getY() > ride.head.getY() + 1.5) reset(actor.getUUID());
    }

    public static void reset(UUID actor) { rides.remove(actor); }
    public static void clear() { rides.clear(); }
}

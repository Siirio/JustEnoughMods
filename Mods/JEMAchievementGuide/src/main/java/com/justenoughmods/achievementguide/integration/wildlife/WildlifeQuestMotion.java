package com.justenoughmods.achievementguide.integration.wildlife;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Native force witnesses are settled only at accepted server player movement. */
public final class WildlifeQuestMotion {
    public interface NativeForce { boolean jem$wildlifeForceActive(ServerPlayer actor); }
    public interface NativeRescuer { boolean jem$wildlifeRescuerOwns(ServerPlayer actor); }
    public record Move(ServerPlayer actor, Vec3 before) {}
    private WildlifeQuestMotion() {}
    public static void force(ServerPlayer actor, Entity source, String kind, Vec3 delta, UUID toolOrigin) {
        if (WildlifeQuestProof.real(actor) == null || delta.lengthSqr() <= 0) return;
        var data = WildlifeQuestProof.data(actor); String key = "motion_" + kind; var proof = data.getCompound(key);
        if (!proof.hasUUID("source") || !proof.getUUID("source").equals(source.getUUID())) { proof = new CompoundTag(); proof.putDouble("distance", 0); }
        proof.putUUID("source", source.getUUID()); proof.putLong("until", actor.level().getGameTime() + 3);
        proof.putDouble("dx", delta.x); proof.putDouble("dy", delta.y); proof.putDouble("dz", delta.z);
        if (toolOrigin != null) proof.putUUID("tool", toolOrigin); data.put(key, proof);
    }
    public static void waterLeap(ServerPlayer actor, UUID origin) {
        if (origin == null) return; var proof = new CompoundTag(); proof.putUUID("tool", origin);
        proof.putDouble("y", actor.getY()); proof.putLong("until", actor.level().getGameTime() + 35); WildlifeQuestProof.data(actor).put("boots_leap", proof);
    }
    public static void climb(ServerPlayer actor, boolean active) {
        var data = WildlifeQuestProof.data(actor);
        if (!active) { data.remove("climb_start_y"); data.remove("climb_until"); return; }
        if (!data.contains("climb_start_y") || data.getLong("climb_until") < actor.level().getGameTime()) data.putDouble("climb_start_y", actor.getY());
        data.putLong("climb_until", actor.level().getGameTime() + 2);
    }
    public static void accepted(Move move) {
        if (move == null || WildlifeQuestProof.real(move.actor()) == null) return;
        ServerPlayer actor = move.actor(); Vec3 actual = actor.position().subtract(move.before());
        if (!Double.isFinite(actual.lengthSqr()) || actual.lengthSqr() == 0 || actual.lengthSqr() > 100) return;
        var data = WildlifeQuestProof.data(actor); long time = actor.level().getGameTime();
        if (actual.horizontalDistanceSqr() > 0.0001) data.putLong("native_walk_time", time);
        for (String kind : List.of("grapple", "spectre")) {
            String key = "motion_" + kind; var proof = data.getCompound(key);
            if (!proof.hasUUID("source")) continue;
            Entity source = actor.serverLevel().getEntity(proof.getUUID("source"));
            if (proof.getLong("until") < time || !(source instanceof NativeForce force) || !force.jem$wildlifeForceActive(actor)) { data.remove(key); continue; }
            Vec3 nativeForce = new Vec3(proof.getDouble("dx"), proof.getDouble("dy"), proof.getDouble("dz"));
            if (actual.dot(nativeForce) <= 0) continue;
            double traveled = Math.min(5, proof.getDouble("distance") + actual.length()); proof.putDouble("distance", traveled); data.put(key, proof);
            if (kind.equals("spectre") && traveled >= 5) { WildlifeQuestProof.emit(actor, "v3_fauna_exotic_travel"); data.remove(key); }
            if (kind.equals("grapple") && proof.hasUUID("tool") && WildlifeQuestTools.consume(actor, "wildlife_grapple", proof.getUUID("tool"))) {
                WildlifeQuestProof.emit(actor, "v3_fauna_ocean_finds"); data.remove(key);
            }
        }
        var leap = data.getCompound("boots_leap");
        if (leap.hasUUID("tool")) {
            if (leap.getLong("until") < time) data.remove("boots_leap");
            else if (actual.y > 0 && actor.getY() > leap.getDouble("y") + 0.25 && !actor.isInWater()
                    && WildlifeQuestTools.consume(actor, "wildlife_boots", leap.getUUID("tool"))) { WildlifeQuestProof.emit(actor, "v3_fauna_ocean_finds"); data.remove("boots_leap"); }
        }
        if (data.contains("climb_start_y") && data.getLong("climb_until") >= time && actual.y > 0 && actor.getY() - data.getDouble("climb_start_y") >= 3) WildlifeQuestProof.emit(actor, "v3_fauna_wild_gear_climb");
    }
    public static void rescued(ServerPlayer actor, Entity cosmaw) {
        var proof = new CompoundTag(); proof.putUUID("source", cosmaw.getUUID()); proof.putLong("until", actor.level().getGameTime() + 200);
        proof.putDouble("x", actor.getX()); proof.putDouble("y", actor.getY()); proof.putDouble("z", actor.getZ()); WildlifeQuestProof.data(actor).put("cosmaw_return", proof);
    }
    public static void settled(ServerPlayer actor) {
        if (!actor.getPersistentData().contains(WildlifeQuestProof.KEY)) return;
        var data = WildlifeQuestProof.data(actor); var proof = data.getCompound("cosmaw_return"); if (!proof.hasUUID("source")) return;
        Entity source = actor.serverLevel().getEntity(proof.getUUID("source"));
        if (proof.getLong("until") < actor.level().getGameTime() || !(source instanceof NativeRescuer rescuer) || !rescuer.jem$wildlifeRescuerOwns(actor)) { data.remove("cosmaw_return"); return; }
        if (actor.isAlive() && actor.onGround() && !actor.isPassenger() && !actor.isInLava() && !actor.isOnFire() && actor.fallDistance == 0
                && actor.distanceToSqr(proof.getDouble("x"), proof.getDouble("y"), proof.getDouble("z")) < 256) {
            WildlifeQuestProof.emit(actor, "v3_fauna_exotic_travel"); data.remove("cosmaw_return");
        }
    }
}

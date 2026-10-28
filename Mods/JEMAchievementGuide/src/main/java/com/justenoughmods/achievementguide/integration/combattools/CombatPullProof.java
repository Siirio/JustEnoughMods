package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Native tentacle force followed by accepted actual motion, never launch/velocity alone. */
public final class CombatPullProof {
    private static final String KEY = "JemQuestTentaclePullV4";
    private CombatPullProof() {}
    public static void applied(ServerPlayer actor, Entity target, Vec3 force) {
        if (!CombatToolProof.real(actor) || target.level() != actor.level() || force.lengthSqr() <= 0.000001) return;
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID());
        proof.putLong("until", actor.level().getGameTime() + 4); proof.putDouble("x", target.getX()); proof.putDouble("y", target.getY()); proof.putDouble("z", target.getZ());
        proof.putDouble("dx", force.x); proof.putDouble("dy", force.y); proof.putDouble("dz", force.z);
        target.getPersistentData().put(KEY, proof);
    }
    public static void moved(Entity target) {
        if (target.level().isClientSide) return;
        CompoundTag proof = target.getPersistentData().getCompound(KEY);
        if (proof.isEmpty()) return;
        if (!proof.hasUUID("actor") || target.level().getGameTime() > proof.getLong("until")) { target.getPersistentData().remove(KEY); return; }
        Vec3 delta = target.position().subtract(proof.getDouble("x"), proof.getDouble("y"), proof.getDouble("z"));
        Vec3 force = new Vec3(proof.getDouble("dx"), proof.getDouble("dy"), proof.getDouble("dz"));
        if (delta.lengthSqr() > 0.000001 && delta.dot(force) > 0.000001 && target.level().getPlayerByUUID(proof.getUUID("actor")) instanceof ServerPlayer actor) {
            target.getPersistentData().remove(KEY); CombatToolProof.emit(actor, "tentacle_route");
        }
    }
}

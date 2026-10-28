package com.justenoughmods.achievementguide.integration.combattools;
import java.util.ArrayDeque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
/** Native force is pending until the same entity genuinely moves, never velocity-only completion. */
public final class CombatRuneControlProof {
    private static final String KEY = "JemQuestRuneControlV4";
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Vec3> calls = new ArrayDeque<>(); int overflow; }
    private CombatRuneControlProof() {}
    public static void clear() { CALLS.remove(); }
    public static void begin(Entity target) { Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= CombatToolProof.LIMIT) calls.overflow++; else calls.calls.push(target.getDeltaMovement()); }
    public static void end(Entity target) {
        Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; } Vec3 before = calls.calls.isEmpty() ? null : calls.calls.pop(); if (calls.calls.isEmpty()) CALLS.remove();
        CombatRuneProof.Origin origin = CombatRuneProof.control();
        if (origin == null || before == null || target.level() != origin.level() || target.getDeltaMovement().distanceToSqr(before) <= 0.000001) return;
        CompoundTag proof = origin.identity().copy(); proof.putLong("until", origin.level().getGameTime() + 4); proof.putDouble("x", target.getX()); proof.putDouble("y", target.getY()); proof.putDouble("z", target.getZ());
        Vec3 changed = target.getDeltaMovement().subtract(before); proof.putDouble("dx", changed.x); proof.putDouble("dy", changed.y); proof.putDouble("dz", changed.z); target.getPersistentData().put(KEY, proof);
    }
    public static void moved(Entity target) {
        CompoundTag proof = target.getPersistentData().getCompound(KEY); if (proof.isEmpty() || !(target.level() instanceof ServerLevel level)) return;
        if (!proof.hasUUID("actor") || proof.getLong("until") < level.getGameTime()) { target.getPersistentData().remove(KEY); return; }
        Vec3 delta = target.position().subtract(proof.getDouble("x"), proof.getDouble("y"), proof.getDouble("z"));
        if (delta.lengthSqr() <= 0.000001 || delta.dot(new Vec3(proof.getDouble("dx"), proof.getDouble("dy"), proof.getDouble("dz"))) <= 0) return;
        target.getPersistentData().remove(KEY); CombatRuneProof.enter(new CombatRuneProof.Origin(level, proof.getUUID("actor"), proof.copy()), true); try { CombatRuneProof.committed(target); } finally { CombatRuneProof.leave(); }
    }
}

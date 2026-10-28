package com.justenoughmods.achievementguide.integration.combattools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

/** Bounded provenance for actual player water placement and its committed native flow. */
public final class CombatWaterProof {
    private static final Map<ServerLevel, LinkedHashMap<Long, Water>> WATER = new WeakHashMap<>();
    private record Water(UUID actor, long until) {}
    private static final ThreadLocal<Placements> PLACEMENTS = ThreadLocal.withInitial(Placements::new);
    private static final class Placements { final ArrayDeque<Boolean> calls = new ArrayDeque<>(); int overflow; }
    private CombatWaterProof() {}
    public static void begin(LevelAccessor world, BlockPos pos) { Placements calls = PLACEMENTS.get(); if (calls.overflow > 0 || calls.calls.size() >= CombatToolProof.LIMIT) calls.overflow++; else calls.calls.push(world.getFluidState(pos).is(FluidTags.WATER)); }
    public static void end(Entity actor, LevelAccessor world, BlockPos pos, boolean accepted) { Placements calls = PLACEMENTS.get(); if (calls.overflow > 0) { calls.overflow--; return; } Boolean before = calls.calls.isEmpty() ? null : calls.calls.pop(); if (calls.calls.isEmpty()) PLACEMENTS.remove(); if (before != null) placed(actor, world, pos, before, accepted); }
    private static void put(ServerLevel level, BlockPos pos, Water proof) {
        LinkedHashMap<Long, Water> cells = WATER.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        if (cells.size() >= 256 && !cells.containsKey(pos.asLong())) cells.remove(cells.keySet().iterator().next()); cells.put(pos.asLong(), proof);
    }
    public static void placed(Entity user, LevelAccessor world, BlockPos pos, boolean wasWater, boolean accepted) {
        if (CombatToolProof.real(user) && world instanceof ServerLevel level && user.level() == level && accepted && !wasWater && level.getFluidState(pos).is(FluidTags.WATER))
            put(level, pos, new Water(user.getUUID(), level.getGameTime() + 1200));
    }
    public static void flowed(LevelAccessor world, BlockPos pos, Direction direction) {
        if (!(world instanceof ServerLevel level) || !level.getFluidState(pos).is(FluidTags.WATER)) return;
        LinkedHashMap<Long, Water> cells = WATER.get(level); if (cells == null) return;
        Water source = cells.get(pos.relative(direction.getOpposite()).asLong());
        if (source != null && source.until >= level.getGameTime()) put(level, pos, source);
    }
    public static void converted(Entity target) {
        if (!(target.level() instanceof ServerLevel level) || !target.isInWater()) return;
        LinkedHashMap<Long, Water> cells = WATER.get(level); if (cells == null) return;
        AABB box = target.getBoundingBox(); UUID actor = null; int read = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (read++ >= CombatToolProof.LIMIT) return;
            if (level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER)) {
                Water proof = cells.get(pos.asLong());
                if (proof == null || proof.until < level.getGameTime() || actor != null && !actor.equals(proof.actor)) return;
                actor = proof.actor;
            }
        }
        if (actor != null) { CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor); target.getPersistentData().put(CombatCorpseProof.WATER, proof); }
    }
    public static void changed(LevelAccessor world, BlockPos pos) { if (world instanceof ServerLevel level && !level.getFluidState(pos).is(FluidTags.WATER)) { LinkedHashMap<Long, Water> cells = WATER.get(level); if (cells != null) cells.remove(pos.asLong()); } }
    public static void clear() { WATER.clear(); PLACEMENTS.remove(); }
}

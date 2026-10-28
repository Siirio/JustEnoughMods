package com.justenoughmods.achievementguide.integration.combattools;

import com.justenoughmods.achievementguide.mixin.CombatTrialStateQuestAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Proven native mob lineage and real combat participation in the current native cycle. */
public final class CombatTrialProof {
    private static final String CYCLE = "JemQuestNativeTrialCycleV4", MOB = "JemQuestNativeTrialMobV4";
    private CombatTrialProof() {}
    public static void activated(BlockEntity spawner, boolean wasActive, boolean active) {
        if (spawner.getLevel() instanceof ServerLevel && !wasActive && active) {
            CompoundTag proof = new CompoundTag(); proof.putUUID("cycle", UUID.randomUUID()); proof.put("actors", new ListTag());
            spawner.getPersistentData().put(CYCLE, proof); spawner.setChanged();
        }
    }
    public static void spawnedLast(BlockEntity spawner) {
        if (!(spawner.getLevel() instanceof ServerLevel level) || !(spawner instanceof CombatTrialStateQuestAccess access) || !access.jem$active() || access.jem$mobs().isEmpty()) return;
        UUID mob = access.jem$mobs().get(access.jem$mobs().size() - 1);
        CompoundTag cycle = spawner.getPersistentData().getCompound(CYCLE);
        Entity target = level.getEntity(mob);
        if (target == null || !target.isAlive() || !cycle.hasUUID("cycle") || !target.getPersistentData().getBoolean("TrialSpawned")) return;
        CompoundTag proof = new CompoundTag(); proof.putUUID("cycle", cycle.getUUID("cycle")); proof.put("source", NbtUtils.writeBlockPos(spawner.getBlockPos()));
        target.getPersistentData().put(MOB, proof);
    }
    public static void defeated(LivingEntity mob, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer actor) || !CombatToolProof.real(actor) || !(mob.level() instanceof ServerLevel level)) return;
        CompoundTag proof = mob.getPersistentData().getCompound(MOB);
        if (!proof.hasUUID("cycle") || !proof.contains("source")) return;
        BlockPos pos = NbtUtils.readBlockPos(proof.getCompound("source"));
        if (!level.hasChunkAt(pos)) return;
        BlockEntity spawner = level.getBlockEntity(pos);
        if (!(spawner instanceof CombatTrialStateQuestAccess access) || !access.jem$active()) return;
        CompoundTag cycle = spawner.getPersistentData().getCompound(CYCLE);
        if (!cycle.hasUUID("cycle") || !cycle.getUUID("cycle").equals(proof.getUUID("cycle"))) return;
        ListTag actors = cycle.getList("actors", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(CombatToolProof.LIMIT, actors.size()); index++) if (actors.getCompound(index).hasUUID("actor") && actors.getCompound(index).getUUID("actor").equals(actor.getUUID())) return;
        if (actors.size() < CombatToolProof.LIMIT) { CompoundTag row = new CompoundTag(); row.putUUID("actor", actor.getUUID()); actors.add(row); cycle.put("actors", actors); spawner.setChanged(); }
    }
    public static Runnable reward(BlockEntity spawner, Runnable original) {
        CompoundTag captured = spawner.getPersistentData().getCompound(CYCLE).copy();
        return CombatDropProof.wrap(() -> {
            CompoundTag current = spawner.getPersistentData().getCompound(CYCLE);
            if (spawner.isRemoved() || !(spawner instanceof CombatTrialStateQuestAccess access) || !access.jem$active() || access.jem$remaining() != 0
                    || !captured.hasUUID("cycle") || !current.hasUUID("cycle") || !captured.getUUID("cycle").equals(current.getUUID("cycle"))) return null;
            List<UUID> actors = new ArrayList<>(); ListTag rows = captured.getList("actors", Tag.TAG_COMPOUND);
            for (int index = 0; index < Math.min(CombatToolProof.LIMIT, rows.size()); index++) if (rows.getCompound(index).hasUUID("actor")) actors.add(rows.getCompound(index).getUUID("actor"));
            return new CombatDropProof.Context(actors, "trial_clear");
        }, original);
    }
}

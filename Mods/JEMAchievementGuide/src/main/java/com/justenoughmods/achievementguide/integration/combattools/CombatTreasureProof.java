package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CombatTreasureProof {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Run> calls = new ArrayDeque<>(); int overflow; }
    private record Run(ServerPlayer actor, BlockEntity chest, BlockPos pos, boolean valid) {}
    private CombatTreasureProof() {}
    public static void clear() { CALLS.remove(); }
    public static void begin(Level level, BlockPos pos, Player player) {
        Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= CombatToolProof.LIMIT) { calls.overflow++; return; }
        BlockEntity chest = level.getBlockEntity(pos); ServerPlayer actor = CombatToolProof.real(player) ? (ServerPlayer)player : null;
        CompoundTag proof = chest == null ? new CompoundTag() : chest.getPersistentData().getCompound(CombatCorpseProof.CHEST);
        CompoundTag nativeState = chest == null ? new CompoundTag() : chest.saveWithoutMetadata();
        boolean valid = actor != null && chest != null && proof.hasUUID("actor") && proof.getUUID("actor").equals(actor.getUUID())
                && nativeState.hasUUID("Owner") && nativeState.getUUID("Owner").equals(actor.getUUID())
                && level.getBlockState(pos).hasProperty(BlockStateProperties.OPEN) && !level.getBlockState(pos).getValue(BlockStateProperties.OPEN)
                && !proof.getList("loot", Tag.TAG_COMPOUND).isEmpty() && proof.getList("loot", Tag.TAG_COMPOUND).equals(nativeState.getList("Items", Tag.TAG_COMPOUND));
        calls.calls.push(new Run(actor, chest, pos.immutable(), valid));
        CombatDropProof.enter(valid ? new CombatDropProof.Context(List.of(actor.getUUID()), "deadbeard") : null);
    }
    public static void spawned(Entity output, boolean accepted) {
        Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.isEmpty()) return;
        Run run = calls.calls.peek();
        if (run.valid && opened(run)) CombatDropProof.spawned(output, accepted);
    }
    public static void end(boolean accepted) {
        Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; }
        Run run = calls.calls.isEmpty() ? null : calls.calls.pop(); CombatDropProof.leave(); if (calls.calls.isEmpty()) CALLS.remove();
        if (run != null && run.valid && accepted && opened(run)) run.chest.getPersistentData().remove(CombatCorpseProof.CHEST);
    }
    private static boolean opened(Run run) { Level level = run.chest.getLevel(); return level != null && level.getBlockEntity(run.pos) == run.chest && level.getBlockState(run.pos).hasProperty(BlockStateProperties.OPEN) && level.getBlockState(run.pos).getValue(BlockStateProperties.OPEN); }
}

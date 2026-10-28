package com.justenoughmods.achievementguide.integration.worldmisc;

import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** The actual native ConversionTask retains the actor of its paid pollen invocation. */
public final class WorldBambooProof {
    public static final class Use {
        public final ServerPlayer actor;
        public final ServerLevel level;
        public final BlockPos root;
        public final ItemStack stack;
        public final int count;
        public final boolean pollen;
        public boolean paid;
        public boolean awarded;
        Use(PlayerInteractEvent.RightClickBlock event) {
            actor = event.getEntity() instanceof ServerPlayer player && WorldNativeQuestProof.real(player) ? player : null;
            level = actor == null ? null : actor.serverLevel();
            root = event.getPos().immutable(); stack = event.getItemStack(); count = stack.getCount();
            pollen = WorldNativeQuestProof.item(stack, "spider_overhaul:bottle_of_spider_pollen");
        }
    }
    private static final class Calls { final ArrayDeque<Use> rows = new ArrayDeque<>(); int overflow; }
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private WorldBambooProof() {}
    public static void begin(PlayerInteractEvent.RightClickBlock event) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.rows.size() >= 16) { calls.overflow++; return; }
        calls.rows.push(new Use(event));
    }
    public static void end(PlayerInteractEvent.RightClickBlock event) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Use use = calls.rows.poll(); if (calls.rows.isEmpty()) CALLS.remove();
        if (use != null && use.actor == event.getEntity() && use.level == event.getLevel()
                && use.root.equals(event.getPos()) && WorldNativeQuestProof.real(use.actor)
                && use.pollen && use.count - use.stack.getCount() == 1
                && event.getCancellationResult().consumesAction()) use.paid = true;
    }
    public static Use constructed(BlockPos root) {
        Calls calls = CALLS.get(); Use use = calls.overflow > 0 ? null : calls.rows.peek();
        return use != null && use.actor != null && use.root.equals(root) && use.pollen ? use : null;
    }
    public static void converted(Use use, ServerLevel level, BlockPos pos, BlockState before) {
        if (use == null || !use.paid || use.awarded || use.level != level || !use.root.equals(pos)
                || !WorldNativeQuestProof.real(use.actor) || use.actor.level() != level
                || !(before.is(Blocks.BAMBOO) || before.is(Blocks.BAMBOO_SAPLING))
                || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals("spider_overhaul:giant_bamboo_block")) return;
        use.awarded = true;
        WorldNativeQuestProof.emit(use.actor, "v3_combat_spider_pollen");
    }
    public static void clear() { CALLS.remove(); }
}

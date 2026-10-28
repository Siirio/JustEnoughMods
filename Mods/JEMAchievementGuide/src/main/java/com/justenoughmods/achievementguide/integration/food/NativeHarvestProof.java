package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Actual accepted output from the native producer, committed only after its world mutation. */
public final class NativeHarvestProof {
    public static final class Run {
        public final ServerPlayer actor;
        public final ServerLevel level;
        public final BlockPos pos;
        public final BlockState before;
        public final String kind;
        public final List<ItemEntity> drops = new ArrayList<>();
        ItemStack expected;
        Run(ServerPlayer actor, BlockPos pos, BlockState before, String kind) {
            this.actor = actor; level = actor.serverLevel(); this.pos = pos.immutable(); this.before = before; this.kind = kind;
        }
    }
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    private NativeHarvestProof() {}
    public static Run begin(ServerPlayer actor, BlockPos pos, BlockState before, String kind) {
        CURRENT.remove();
        if (RemainingFoodActors.real(actor) == null || kind.isEmpty()) return null;
        Run run = new Run(actor, pos, before, kind); CURRENT.set(run); return run;
    }
    public static void end(Run run) { if (CURRENT.get() == run) CURRENT.remove(); }
    public static Run current() { return CURRENT.get(); }
    public static void expect(Level level, BlockPos pos, ItemStack source) {
        Run run = CURRENT.get();
        if (run != null && run.level == level && run.pos.equals(pos) && run.expected == null) run.expected = source;
    }
    public static void expectedEnd(ItemStack source) {
        Run run = CURRENT.get(); if (run != null && run.expected == source) run.expected = null;
    }
    public static void inserted(Entity entity, boolean accepted) {
        Run run = CURRENT.get();
        if (accepted && run != null && entity.level() == run.level && entity instanceof ItemEntity drop
                && !drop.getItem().isEmpty() && drop.getItem() == run.expected && run.drops.size() < 32) run.drops.add(drop);
    }
    public static boolean mature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                return state.getValue(age).equals(age.getPossibleValues().stream().max(Integer::compareTo).orElse(-1));
            }
        }
        return false;
    }
    public static boolean reset(BlockState before, BlockState after) {
        if (!after.is(before.getBlock())) return false;
        for (var property : before.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) return after.getValue(age) < before.getValue(age);
        }
        return false;
    }
    public static String plantedKind(ServerPlayer actor, BlockPos pos, BlockState state) {
        if (NativeFoodSources.block(state).equals("snowyspirit:ginger") && mature(state)) return "ginger";
        var planted = NativePlantProof.original(actor.serverLevel(), pos, state);
        if (planted == null || !planted.actor().equals(actor.getUUID())) return "";
        if (planted.kind().equals("ginger") && mature(state)) return "ginger";
        if (planted.kind().equals("cassowary_orchard") && mature(state)) return "orchard";
        if (planted.kind().equals("fiddlehead_output")) return "fiddlehead";
        return "";
    }
    public static void commit(Run run, boolean nativeSuccess) {
        end(run);
        if (!nativeSuccess || run == null || RemainingFoodActors.real(run.actor) == null) return;
        if (run.kind.equals("harvest_again")) {
            if (!run.drops.isEmpty() && reset(run.before, run.level.getBlockState(run.pos))) {
                QuestActionTrigger.INSTANCE.emit(run.actor, "v3_food_harvest_again");
                if (NativeFoodSources.block(run.before).equals("snowyspirit:ginger") && mature(run.before)) {
                    for (ItemEntity drop : run.drops) if (RemainingFoodActors.item(drop.getItem()).equals("snowyspirit:ginger")) {
                        NativeFoodSources.get(run.level).certify(run.level, run.actor.getUUID(), "ginger", drop.getItem(), drop.getItem().getCount());
                    }
                }
            }
            return;
        }
        for (ItemEntity drop : run.drops) {
            String id = RemainingFoodActors.item(drop.getItem());
            if (run.kind.equals("ginger") && id.equals("snowyspirit:ginger")) NativeFoodSources.get(run.level).certify(run.level, run.actor.getUUID(), "ginger", drop.getItem(), drop.getItem().getCount());
            if (run.kind.equals("orchard") && (id.equals("primal:litchi") || id.equals("primal:kiwano") || id.equals("primal:starfruit"))) {
                FoodDroppedProof.update(drop, "v3_fauna_cassowary_orchard", drop.getItem().getCount(), run.actor.getUUID());
            }
            if (run.kind.equals("fiddlehead") && id.equals("alexscaves:fiddlehead")) FoodDroppedProof.update(drop, "v3_cave_cave_garden", drop.getItem().getCount(), run.actor.getUUID());
        }
    }
}

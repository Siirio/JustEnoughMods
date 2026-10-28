package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class NativeFertilizerProof {
    public static final class Run {
        final ServerPlayer actor;
        final ItemStack stack;
        final int count;
        boolean thornwood;
        Run(ServerPlayer actor, ItemStack stack) { this.actor = actor; this.stack = stack; count = stack.getCount(); }
    }
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    private NativeFertilizerProof() {}
    public static void begin(ServerPlayer actor, ItemStack stack) { CURRENT.remove(); if (RemainingFoodActors.real(actor) != null) CURRENT.set(new Run(actor, stack)); }
    public static void grew(net.minecraft.server.level.ServerLevel level) { Run run = CURRENT.get(); if (run != null && run.actor.level() == level) run.thornwood = true; }
    public static void end(boolean nativeSuccess) {
        Run run = CURRENT.get(); CURRENT.remove();
        if (run != null && nativeSuccess && run.thornwood && run.count - run.stack.getCount() == 1) QuestActionTrigger.INSTANCE.emit(run.actor, "food_cave_thornwood");
    }
}

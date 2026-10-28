package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** The native armor bonus getter and the real boosted food mutation within one actual meal. */
public final class PrimordialFoodProof {
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    public static final class Run {
        final ServerPlayer actor;
        final ItemStack meal;
        final int count;
        final FoodProperties food;
        final FoodData hunger;
        int bonus;
        boolean committed;
        Run(ServerPlayer actor, ItemStack meal, FoodProperties food) {
            this.actor = actor; this.meal = meal; count = meal.getCount();
            this.food = food; hunger = actor.getFoodData();
        }
    }
    public record Mutation(Run run, int food, float saturation) {}
    private PrimordialFoodProof() {}

    public static Run begin(ServerPlayer actor, ItemStack meal) {
        CURRENT.remove();
        var food = meal.getFoodProperties(actor);
        if (meal.isEmpty() || food == null || actor.isSpectator() || actor.getAbilities().instabuild
                || actor instanceof net.minecraftforge.common.util.FakePlayer) return null;
        var run = new Run(actor, meal, food);
        CURRENT.set(run);
        return run;
    }

    public static void nativeBonus(LivingEntity actor, int bonus) {
        var run = CURRENT.get();
        if (run != null && run.actor == actor && bonus > 0) run.bonus = bonus;
    }

    public static Mutation beforeMutation(FoodData target, int nutrition, float saturation) {
        var run = CURRENT.get();
        if (run == null || run.hunger != target || run.bonus < 1 || nutrition <= run.food.getNutrition()
                || !Float.isFinite(saturation) || saturation <= run.food.getSaturationModifier()) return null;
        return new Mutation(run, target.getFoodLevel(), target.getSaturationLevel());
    }

    public static void afterMutation(FoodData target, Mutation before) {
        if (before == null || before.run != CURRENT.get() || before.run.hunger != target) return;
        if (target.getFoodLevel() > before.food || target.getSaturationLevel() > before.saturation) before.run.committed = true;
    }

    public static void finish(Run run) {
        CURRENT.remove();
        if (run != null && run.committed && run.bonus > 0 && run.actor.getFoodData() == run.hunger
                && run.count - run.meal.getCount() == 1) {
            QuestActionTrigger.INSTANCE.emit(run.actor, "v3_cave_primitive_tactics");
        }
    }
}

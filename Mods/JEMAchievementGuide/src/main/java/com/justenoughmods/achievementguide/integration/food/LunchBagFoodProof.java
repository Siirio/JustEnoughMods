package com.justenoughmods.achievementguide.integration.food;

import com.github.ysbbbbbb.kaleidoscopecookery.item.TransmutationLunchBagItem;
import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.ItemStackHandler;

/** Observes one native 1.7.0 lunch-bag use without becoming a second food owner. */
public final class LunchBagFoodProof {
    private record Run(ItemStack bag, ServerPlayer actor, Level level, int foodCount, int hunger) {}

    private static final ThreadLocal<Deque<Run>> RUNS = ThreadLocal.withInitial(ArrayDeque::new);

    public static void begin(ItemStack bag, Level level, LivingEntity entity) {
        Deque<Run> runs = RUNS.get();
        if (runs.size() >= 16) runs.clear();
        ServerPlayer actor = eligible(entity, level);
        runs.push(new Run(bag, actor, level, actor == null ? 0 : foodCount(bag),
                actor == null ? 0 : actor.getFoodData().getFoodLevel()));
    }

    public static void finish(ItemStack bag, Level level, LivingEntity entity, ItemStack nativeResult) {
        Run before = RUNS.get().poll();
        if (before == null || before.bag != bag || before.actor == null || before.actor != entity
                || before.level != level || nativeResult.isEmpty() || nativeResult.getItem() != bag.getItem()) return;
        int consumed = before.foodCount - foodCount(bag);
        if (consumed >= 2 && before.actor.getFoodData().getFoodLevel() > before.hunger)
            QuestActionTrigger.INSTANCE.emit(before.actor, "v3_food_lunch_bag");
    }

    private static ServerPlayer eligible(LivingEntity entity, Level level) {
        return entity instanceof ServerPlayer player && !(player instanceof FakePlayer)
                && player.level() == level && !level.isClientSide && !player.isSpectator()
                && !player.getAbilities().instabuild ? player : null;
    }

    private static int foodCount(ItemStack bag) {
        ItemStackHandler items = TransmutationLunchBagItem.getItems(bag);
        int count = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stored = items.getStackInSlot(slot);
            if (!stored.isEmpty() && stored.getItem().getFoodProperties(stored, null) != null)
                count += stored.getCount();
        }
        return count;
    }

    private LunchBagFoodProof() {}
}

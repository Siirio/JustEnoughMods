package com.justenoughmods.achievementguide.integration.creaturecare;

import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Short native call scope, bounded accepted outputs, no world scans. */
public final class CreatureCareOutputs {
    public static final class Run {
        public final Entity producer;
        public final ServerPlayer actor;
        public final List<ItemEntity> drops = new ArrayList<>();
        private Run(Entity producer, ServerPlayer actor) { this.producer = producer; this.actor = actor; }
    }
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    private CreatureCareOutputs() {}
    public static Run begin(Entity producer, ServerPlayer actor) {
        CURRENT.remove();
        if (actor == null || producer.level() != actor.level()) return null;
        Run run = new Run(producer, actor); CURRENT.set(run); return run;
    }
    public static void inserted(Entity entity, boolean accepted) {
        Run run = CURRENT.get();
        if (run != null && accepted && entity instanceof ItemEntity drop && entity.level() == run.actor.level()
                && run.drops.size() < 32 && !drop.getItem().isEmpty()) run.drops.add(drop);
        CreatureCareEggs.inserted(entity, accepted);
    }
    public static void end(Run run) { if (CURRENT.get() == run) CURRENT.remove(); }
    public static void collected(Run run, ItemEntity drop, String action) {
        if (run != null && run.drops.contains(drop) && run.actor.serverLevel().getEntity(drop.getUUID()) == drop) {
            FoodDroppedProof.update(drop, action, drop.getItem().getCount(), run.actor.getUUID());
        }
    }
    public static boolean conserved(List<ItemStack> before, List<ItemStack> after, List<ItemEntity> drops) {
        List<ItemStack> remaining = new ArrayList<>();
        before.forEach(stack -> { if (!stack.isEmpty()) remaining.add(stack.copy()); });
        List<ItemStack> outputs = new ArrayList<>();
        after.forEach(stack -> { if (!stack.isEmpty()) outputs.add(stack); });
        drops.forEach(drop -> outputs.add(drop.getItem()));
        for (ItemStack output : outputs) {
            int count = output.getCount();
            for (ItemStack source : remaining) {
                if (ItemStack.isSameItemSameTags(source, output)) {
                    int used = Math.min(count, source.getCount()); source.shrink(used); count -= used;
                }
            }
            if (count != 0) return false;
        }
        return remaining.stream().allMatch(ItemStack::isEmpty);
    }
}

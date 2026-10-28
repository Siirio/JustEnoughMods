package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.mixin.CompactingQuestAccess;
import com.justenoughmods.achievementguide.mixin.UpgradeWrapperQuestAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;

/** Exact native compacting calls inside one authenticated carried-backpack tick. */
public final class BackpackCompactingProof {
    private static final ThreadLocal<Run> RUN = new ThreadLocal<>();
    private static final class Run {
        final Object wrapper;
        final IItemHandler inventory;
        final ServerPlayer actor;
        ItemStack debit = ItemStack.EMPTY;
        Run(Object wrapper, IItemHandler inventory, ServerPlayer actor) { this.wrapper = wrapper; this.inventory = inventory; this.actor = actor; }
    }
    private BackpackCompactingProof() {}

    public static void compact(Object wrapper, IItemHandlerSimpleInserter inventory, int slot, Entity entity, Level level) {
        Run previous = RUN.get();
        RUN.remove();
        if (entity instanceof ServerPlayer actor && actor.level() == level && !(actor instanceof FakePlayer)
                && !actor.isSpectator() && !actor.getAbilities().instabuild && inventory.getSlots() <= 4096
                && ((IUpgradeWrapper) wrapper).isEnabled()
                && ((UpgradeWrapperQuestAccess) wrapper).jem$questStorageWrapper() instanceof IBackpackWrapper) RUN.set(new Run(wrapper, inventory, actor));
        try {
            ((CompactingQuestAccess) wrapper).jem$nativeCompactSlotAndQueued(inventory, slot);
        } finally {
            if (previous == null) RUN.remove(); else RUN.set(previous);
        }
    }

    public static void extracted(Object wrapper, IItemHandler inventory, ItemStack requested, ItemStack removed, boolean simulate) {
        Run run = RUN.get();
        if (simulate || run == null || run.wrapper != wrapper || run.inventory != inventory) return;
        run.debit = !removed.isEmpty() && ItemStack.isSameItemSameTags(removed, requested)
                && removed.getCount() == requested.getCount() ? removed.copy() : ItemStack.EMPTY;
    }

    public static long count(IItemHandler inventory, ItemStack kind) {
        if (inventory.getSlots() > 4096) return -1;
        long result = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, kind)) result += stack.getCount();
        }
        return result;
    }

    public static void inserted(Object wrapper, IItemHandler inventory, ItemStack result, ItemStack remaining, long before, boolean simulate) {
        Run run = RUN.get();
        if (simulate || run == null || run.wrapper != wrapper || run.inventory != inventory) return;
        ItemStack debit = run.debit;
        run.debit = ItemStack.EMPTY;
        if (debit.isEmpty() || result.isEmpty() || ItemStack.isSameItemSameTags(debit, result) || before < 0
                || !remaining.isEmpty() && !ItemStack.isSameItemSameTags(remaining, result)) return;
        int accepted = result.getCount() - remaining.getCount();
        if (accepted > 0 && count(inventory, result) - before == accepted) QuestActionTrigger.INSTANCE.emit(run.actor, "storage_compact_commit");
    }
}

package com.justenoughmods.achievementguide.integration.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;

public final class WildlifeQuestWork {
    private WildlifeQuestWork() {}
    public static final class Deposit {
        public final TamableAnimal crow; public final ServerPlayer actor; public final ItemFrame frame; public final ItemStack before;
        public final BlockPos pos; public final BlockEntity container;
        public IItemHandler handler; public int inventoryBefore; public int inserted;
        public Deposit(TamableAnimal crow, ServerPlayer actor, ItemFrame frame) {
            this.crow = crow; this.actor = actor; this.frame = frame; before = crow.getMainHandItem().copy();
            pos = frame.getPos().relative(frame.getDirection().getOpposite()); container = frame.level().getBlockEntity(pos);
        }
    }
    public static int count(IItemHandler handler, ItemStack variant) {
        int count = 0;
        for (int i = 0; i < Math.min(256, handler.getSlots()); i++) if (ItemStack.isSameItemSameTags(handler.getStackInSlot(i), variant)) count += handler.getStackInSlot(i).getCount();
        return count;
    }
    public static void deposited(Deposit run) {
        if (run == null || run.handler == null || run.inserted <= 0 || run.frame.isRemoved() || run.container == null
                || run.frame.level().getBlockEntity(run.pos) != run.container || !run.crow.isTame() || !run.actor.getUUID().equals(run.crow.getOwnerUUID())
                || !ItemStack.isSameItem(run.frame.getItem(), run.before)) return;
        ItemStack after = run.crow.getMainHandItem();
        if (run.before.getCount() - after.getCount() == run.inserted && (after.isEmpty() || ItemStack.isSameItemSameTags(run.before, after))
                && count(run.handler, run.before) - run.inventoryBefore == run.inserted) WildlifeQuestProof.emit(run.actor, "v3_fauna_animal_work");
    }
}

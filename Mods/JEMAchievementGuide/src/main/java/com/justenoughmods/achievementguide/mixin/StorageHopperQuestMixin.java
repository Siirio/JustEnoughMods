package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes a real capability transfer while the exact native container menu is open. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.upgrades.hopper.HopperUpgradeWrapper", remap = false)
public abstract class StorageHopperQuestMixin {
    @Unique private ServerLevel jem$level;
    @Unique private BlockPos jem$position;
    @Unique private IItemHandler jem$source;
    @Unique private IItemHandler jem$destination;
    @Unique private ItemStack jem$removed = ItemStack.EMPTY;
    @Unique private int jem$sourceSlot;
    @Unique private int jem$sourceBefore;
    @Unique private long jem$destinationBefore;
    @Unique private boolean jem$committed;

    @Inject(method = "tick", at = @At("HEAD"))
    private void jem$nativeLocation(Entity entity, Level level, BlockPos pos, CallbackInfo callback) {
        jem$level = level instanceof ServerLevel serverLevel ? serverLevel : null;
        jem$position = pos.immutable();
    }
    @Inject(method = "tick", at = @At("RETURN"))
    private void jem$endTick(Entity entity, Level level, BlockPos pos, CallbackInfo callback) { jem$level = null; jem$position = null; }

    @Inject(method = "moveItems", at = @At("HEAD"))
    private void jem$beginTransfer(IItemHandler source, IItemHandler destination, FilterLogic filter, CallbackInfoReturnable<Boolean> callback) {
        jem$source = source;
        jem$destination = destination;
        jem$removed = ItemStack.EMPTY;
        jem$committed = false;
    }

    @Redirect(method = "moveItems", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/items/IItemHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeExtraction(IItemHandler source, int slot, int amount, boolean simulate) {
        ItemStack before = ItemStack.EMPTY;
        if (!simulate && source == jem$source && source != jem$destination && jem$destination.getSlots() <= 4096) {
            before = source.getStackInSlot(slot).copy();
            jem$sourceBefore = before.getCount();
            jem$sourceSlot = slot;
            jem$destinationBefore = jem$count(jem$destination, before);
        }
        ItemStack removed = source.extractItem(slot, amount, simulate);
        if (!simulate) jem$removed = !before.isEmpty() && ItemStack.isSameItemSameTags(before, removed) ? removed.copy() : ItemStack.EMPTY;
        return removed;
    }

    @Redirect(method = "moveItems", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/util/InventoryHelper;insertIntoInventory(Lnet/minecraft/world/item/ItemStack;Lnet/minecraftforge/items/IItemHandler;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeInsertion(ItemStack offered, IItemHandler destination, boolean simulate) {
        ItemStack remaining = InventoryHelper.insertIntoInventory(offered, destination, simulate);
        if (!simulate && destination == jem$destination && !jem$removed.isEmpty()) {
            ItemStack after = jem$source.getStackInSlot(jem$sourceSlot);
            int sourceRemaining = ItemStack.isSameItemSameTags(after, jem$removed) ? after.getCount() : 0;
            int inserted = jem$removed.getCount() - remaining.getCount();
            jem$committed = inserted > 0 && jem$sourceBefore - sourceRemaining == jem$removed.getCount()
                    && jem$count(destination, jem$removed) - jem$destinationBefore == inserted;
        }
        return remaining;
    }

    @Inject(method = "moveItems", at = @At("RETURN"))
    private void jem$finishedTransfer(IItemHandler source, IItemHandler destination, FilterLogic filter, CallbackInfoReturnable<Boolean> callback) {
        if (jem$committed && callback.getReturnValue() && jem$level != null) {
            var wrapper = ((UpgradeWrapperQuestAccess) this).jem$questStorageWrapper();
            for (var actor : jem$level.getServer().getPlayerList().getPlayers()) {
                if (actor.containerMenu instanceof StorageContainerMenu menu && menu.getStorageWrapper() == wrapper
                        && menu.getStorageBlockEntity().getLevel() == jem$level
                        && menu.getStorageBlockEntity().getBlockPos().equals(jem$position) && menu.stillValid(actor)) {
                    QuestActionTrigger.INSTANCE.emit(actor, "storage_hopper");
                }
            }
        }
        jem$source = jem$destination = null;
        jem$removed = ItemStack.EMPTY;
        jem$committed = false;
    }

    @Unique private static long jem$count(IItemHandler inventory, ItemStack kind) {
        long count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, kind)) count += stack.getCount();
        }
        return count;
    }
}

package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper.TargetSlot;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper", remap = false)
public abstract class BackpackRefillQuestMixin {
    @Unique private ServerPlayer jem$owner;
    @Unique private ItemStack jem$kind = ItemStack.EMPTY;
    @Unique private long jem$before;
    @Unique private long jem$debited;

    @Inject(method = "tick", at = @At("HEAD"))
    private void jem$carriedOwner(Entity entity, Level level, BlockPos pos, CallbackInfo callback) {
        jem$owner = entity instanceof ServerPlayer actor && ((IUpgradeWrapper) this).isEnabled() ? actor : null;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void jem$endTick(Entity entity, Level level, BlockPos pos, CallbackInfo callback) { jem$owner = null; }

    @Inject(method = "tryRefillFilter", at = @At("HEAD"))
    private void jem$beforeRefill(Entity entity, IItemHandler destination, ItemStack filter, TargetSlot target, CallbackInfo callback) {
        jem$kind = ItemStack.EMPTY;
        jem$debited = 0;
        if (entity == jem$owner && !filter.isEmpty() && destination.getSlots() <= 128) {
            jem$kind = filter.copy();
            jem$before = jem$count(destination, filter);
        }
    }

    @Redirect(method = "tryRefillFilter", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/util/InventoryHelper;extractFromInventory(Lnet/minecraft/world/item/ItemStack;Lnet/minecraftforge/items/IItemHandler;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeDebit(ItemStack requested, IItemHandler source, boolean simulate) {
        ItemStack removed = InventoryHelper.extractFromInventory(requested, source, simulate);
        if (!simulate && !jem$kind.isEmpty() && source == ((UpgradeWrapperQuestAccess) this).jem$questStorageWrapper().getInventoryForUpgradeProcessing()
                && ItemStack.isSameItemSameTags(jem$kind, removed)) jem$debited += removed.getCount();
        return removed;
    }

    @Inject(method = "tryRefillFilter", at = @At("RETURN"))
    private void jem$afterRefill(Entity entity, IItemHandler destination, ItemStack filter, TargetSlot target, CallbackInfo callback) {
        ItemStack kind = jem$kind;
        jem$kind = ItemStack.EMPTY;
        if (entity == jem$owner && !kind.isEmpty() && jem$debited > 0
                && jem$count(destination, kind) - jem$before == jem$debited) {
            QuestActionTrigger.INSTANCE.emit(jem$owner, "v3_store_refill");
        }
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

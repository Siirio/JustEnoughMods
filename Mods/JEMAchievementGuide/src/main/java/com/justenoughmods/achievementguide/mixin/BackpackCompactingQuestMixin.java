package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.BackpackCompactingProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeWrapper", remap = false)
public abstract class BackpackCompactingQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/compacting/CompactingUpgradeWrapper;compactSlotAndQueued(Lnet/p3pp3rf1y/sophisticatedcore/inventory/IItemHandlerSimpleInserter;I)V"))
    private void jem$nativeCompact(CompactingUpgradeWrapper wrapper, IItemHandlerSimpleInserter inventory, int slot, Entity entity, Level level, BlockPos position) {
        BackpackCompactingProof.compact(wrapper, inventory, slot, entity, level);
    }

    @Redirect(method = "tryCompacting", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/util/InventoryHelper;extractFromInventory(Lnet/minecraft/world/item/ItemStack;Lnet/minecraftforge/items/IItemHandler;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeDebit(ItemStack requested, IItemHandler inventory, boolean simulate) {
        ItemStack removed = InventoryHelper.extractFromInventory(requested, inventory, simulate);
        BackpackCompactingProof.extracted(this, inventory, requested, removed, simulate);
        return removed;
    }

    @Redirect(method = "tryCompacting", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/inventory/IItemHandlerSimpleInserter;insertItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeOutput(IItemHandlerSimpleInserter inventory, ItemStack result, boolean simulate) {
        ItemStack original = result.copy();
        long before = simulate ? -1 : BackpackCompactingProof.count(inventory, original);
        ItemStack remaining = inventory.insertItem(result, simulate);
        BackpackCompactingProof.inserted(this, inventory, original, remaining, before, simulate);
        return remaining;
    }
}

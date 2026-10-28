package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SophisticatedFilterProof;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.voiding.VoidUpgradeWrapper", remap = false)
public abstract class SophisticatedVoidCommitQuestMixin {
    @Unique private final Map<Integer, SophisticatedFilterProof.PendingVoid> jem$voidProofs = new HashMap<>();
    @Redirect(method = "onSlotChange", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/FilterLogic;matchesFilter(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$nativeFilter(FilterLogic filter, ItemStack item, IItemHandler inventory, int slot) {
        boolean matched = filter.matchesFilter(item);
        var pending = SophisticatedFilterProof.queued((VoidUpgradeWrapper)(Object)this, inventory, slot, item, matched);
        if (pending == null) jem$voidProofs.remove(slot);
        else if (jem$voidProofs.size() < 128) jem$voidProofs.put(slot, pending);
        return matched;
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/p3pp3rf1y/sophisticatedcore/inventory/InventoryHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeVoid(InventoryHandler inventory, int slot, int amount, boolean simulate) {
        var pending = jem$voidProofs.remove(slot);
        ItemStack before = pending == null ? ItemStack.EMPTY : inventory.getStackInSlot(slot).copy();
        ItemStack removed = inventory.extractItem(slot, amount, simulate);
        if (!simulate) SophisticatedFilterProof.voided((VoidUpgradeWrapper)(Object)this, inventory, slot, before, removed, pending);
        return removed;
    }
}

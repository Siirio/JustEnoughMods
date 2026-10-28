package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StorageCraftProof;
import com.tom.storagemod.tile.CraftingTerminalBlockEntity;
import com.tom.storagemod.util.StoredItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.CraftingTerminalBlockEntity", remap = false)
public abstract class StorageCraftDebitQuestMixin {
    @Redirect(method = "craft", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/inventory/CraftingContainer;m_7407_(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$gridDebit(CraftingContainer matrix, int slot, int count) {
        ItemStack removed = matrix.removeItem(slot, count);
        CraftingTerminalBlockEntity terminal = (CraftingTerminalBlockEntity) (Object) this;
        if (matrix == terminal.getCraftingInv()) StorageCraftProof.consumed(terminal, removed);
        return removed;
    }

    @Redirect(method = "craft", at = @At(value = "INVOKE", target =
            "Lcom/tom/storagemod/tile/CraftingTerminalBlockEntity;pullStack(Lcom/tom/storagemod/util/StoredItemStack;J)Lcom/tom/storagemod/util/StoredItemStack;"))
    private StoredItemStack jem$networkDebit(CraftingTerminalBlockEntity terminal, StoredItemStack requested, long count) {
        StoredItemStack removed = terminal.pullStack(requested, count);
        if (removed != null && ItemStack.isSameItemSameTags(requested.getStack(), removed.getStack())) {
            StorageCraftProof.pulled(terminal, removed.getQuantity());
        }
        return removed;
    }
}

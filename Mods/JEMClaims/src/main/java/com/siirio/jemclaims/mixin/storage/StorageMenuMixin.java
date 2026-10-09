package com.siirio.jemclaims.mixin.storage;

import com.siirio.jemclaims.compat.storage.StorageAccess;
import com.tom.storagemod.gui.StorageTerminalMenu;
import com.tom.storagemod.tile.StorageTerminalBlockEntity;
import com.tom.storagemod.util.StoredItemStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = StorageTerminalMenu.class, remap = false)
public abstract class StorageMenuMixin {
    @Shadow protected Inventory pinv;

    @Redirect(method = "m_38946_", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/StorageTerminalBlockEntity;getStacks()Ljava/util/Map;"))
    private Map<StoredItemStack, Long> jemclaims$visible(StorageTerminalBlockEntity terminal) {
        terminal.getStacks();
        return StorageAccess.as(pinv.player, () -> {
            Map<StoredItemStack, Long> visible = new HashMap<>();
            IItemHandler inventory = ((StorageTerminalAccessor) terminal).jemclaims$inventory();
            if (inventory != null) {
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!stack.isEmpty()) visible.merge(new StoredItemStack(stack), (long) stack.getCount(), Long::sum);
                }
            }
            return visible;
        });
    }

    @Redirect(method = {"onInteract", "shiftClickItems"}, at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/StorageTerminalBlockEntity;pushStack(Lcom/tom/storagemod/util/StoredItemStack;)Lcom/tom/storagemod/util/StoredItemStack;"))
    private StoredItemStack jemclaims$push(StorageTerminalBlockEntity terminal, StoredItemStack stack) {
        return StorageAccess.as(pinv.player, () -> terminal.pushStack(stack));
    }

    @Redirect(method = "onInteract", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/StorageTerminalBlockEntity;pushStack(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jemclaims$pushItem(StorageTerminalBlockEntity terminal, ItemStack stack) {
        return StorageAccess.as(pinv.player, () -> terminal.pushStack(stack));
    }

    @Redirect(method = "onInteract", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/StorageTerminalBlockEntity;pullStack(Lcom/tom/storagemod/util/StoredItemStack;J)Lcom/tom/storagemod/util/StoredItemStack;"))
    private StoredItemStack jemclaims$pull(StorageTerminalBlockEntity terminal, StoredItemStack stack, long count) {
        return StorageAccess.as(pinv.player, () -> terminal.pullStack(stack, count));
    }

    @Redirect(method = "onInteract", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/StorageTerminalBlockEntity;pushOrDrop(Lnet/minecraft/world/item/ItemStack;)V"))
    private void jemclaims$returnItem(StorageTerminalBlockEntity terminal, ItemStack stack) {
        StorageAccess.as(pinv.player, () -> { terminal.pushOrDrop(stack); return null; });
    }
}

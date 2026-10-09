package com.siirio.jemclaims.mixin.storage;

import com.siirio.jemclaims.compat.storage.StorageAccess;
import com.tom.storagemod.tile.CraftingTerminalBlockEntity;
import com.tom.storagemod.util.StoredItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftingTerminalBlockEntity.class, remap = false)
public abstract class CraftingTerminalMixin {
    @Redirect(method = "craft", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/CraftingTerminalBlockEntity;pullStack(Lcom/tom/storagemod/util/StoredItemStack;J)Lcom/tom/storagemod/util/StoredItemStack;"))
    private StoredItemStack jemclaims$refill(CraftingTerminalBlockEntity terminal, StoredItemStack stack, long count, Player player) {
        return StorageAccess.as(player, () -> terminal.pullStack(stack, count));
    }

    @Redirect(method = "craft", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/CraftingTerminalBlockEntity;pushStack(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jemclaims$remainder(CraftingTerminalBlockEntity terminal, ItemStack stack, Player player) {
        return StorageAccess.as(player, () -> terminal.pushStack(stack));
    }

    @Redirect(method = "clear", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/CraftingTerminalBlockEntity;pushStack(Lcom/tom/storagemod/util/StoredItemStack;)Lcom/tom/storagemod/util/StoredItemStack;"))
    private StoredItemStack jemclaims$clear(CraftingTerminalBlockEntity terminal, StoredItemStack stack, Player player) {
        return StorageAccess.as(player, () -> terminal.pushStack(stack));
    }
}

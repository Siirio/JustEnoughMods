package com.siirio.jemclaims.mixin.storage;

import com.siirio.jemclaims.compat.storage.StorageAccess;
import com.tom.storagemod.gui.CraftingTerminalMenu;
import com.tom.storagemod.tile.CraftingTerminalBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftingTerminalMenu.class, remap = false)
public abstract class CraftingMenuMixin {
    @Redirect(method = "shiftClickItems", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/CraftingTerminalBlockEntity;pushStack(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jemclaims$push(CraftingTerminalBlockEntity terminal, ItemStack stack, Player player, int slot) {
        return StorageAccess.as(player, () -> terminal.pushStack(stack));
    }
}

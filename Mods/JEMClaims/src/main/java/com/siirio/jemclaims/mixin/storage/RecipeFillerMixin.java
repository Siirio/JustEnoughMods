package com.siirio.jemclaims.mixin.storage;

import com.siirio.jemclaims.compat.storage.StorageAccess;
import com.tom.storagemod.tile.CraftingTerminalBlockEntity;
import com.tom.storagemod.util.StoredItemStack;
import com.tom.storagemod.util.TerminalCraftingFiller;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TerminalCraftingFiller.class, remap = false)
public abstract class RecipeFillerMixin {
    @Shadow private Player player;

    @Redirect(method = "placeRecipe", at = @At(value = "INVOKE", target = "Lcom/tom/storagemod/tile/CraftingTerminalBlockEntity;pullStack(Lcom/tom/storagemod/util/StoredItemStack;J)Lcom/tom/storagemod/util/StoredItemStack;"))
    private StoredItemStack jemclaims$ingredient(CraftingTerminalBlockEntity terminal, StoredItemStack stack, long count) {
        return StorageAccess.as(player, () -> terminal.pullStack(stack, count));
    }
}

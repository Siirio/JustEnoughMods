package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import com.justenoughmods.achievementguide.integration.StorageCraftProof;
import com.justenoughmods.achievementguide.integration.StorageNetworkAccess;
import com.tom.storagemod.gui.CraftingTerminalMenu;
import com.tom.storagemod.tile.CraftingTerminalBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.CraftingTerminalMenu$Result", remap = false)
public abstract class StorageCraftQuestMixin {
    @Redirect(method = "m_142406_", at = @At(value = "INVOKE", target =
            "Lcom/tom/storagemod/tile/CraftingTerminalBlockEntity;craft(Lnet/minecraft/world/entity/player/Player;)V"))
    private void jem$observeCraft(CraftingTerminalBlockEntity terminal, Player player, Player originalPlayer, ItemStack result) {
        if (player instanceof ServerPlayer actor && originalPlayer == player
                && actor.containerMenu instanceof CraftingTerminalMenu menu && menu.getPlayer() == actor
                && menu.getCraftingResultSlot() == (Object) this && menu.getTerminal() == terminal
                && terminal.canInteractWith(actor)
                && ((StorageCraftRecipeQuestAccess) terminal).jem$questRecipe() != null
                && ((StorageCraftRecipeQuestAccess) terminal).jem$questRecipe().matches(terminal.getCraftingInv(), actor.level())
                && ((StorageNetworkAccess) terminal).jem$questStorageNetwork() != null) {
            ItemStack taken = result.isEmpty() ? ((QuestResultSnapshot) menu).jem$questQuickResult() : result;
            StorageCraftProof.craft(terminal, actor, taken.copy());
        } else terminal.craft(player);
    }
}

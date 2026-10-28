package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import com.tom.storagemod.gui.CraftingTerminalMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.CraftingTerminalMenu", remap = false)
public abstract class StorageCraftMenuQuestMixin implements QuestResultSnapshot {
    @Unique private ItemStack jem$quickResult = ItemStack.EMPTY;
    @Override public ItemStack jem$questQuickResult() { return jem$quickResult; }

    @Inject(method = "shiftClickItems", at = @At("HEAD"))
    private void jem$beforeTransfer(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        CraftingTerminalMenu menu = (CraftingTerminalMenu) (Object) this;
        jem$quickResult = slot == 0 && player.containerMenu == menu ? menu.getCraftingResultSlot().getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "shiftClickItems", at = @At("RETURN"))
    private void jem$afterTransfer(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        jem$quickResult = ItemStack.EMPTY;
    }
}

package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CaveMapMenuQuestMixin implements QuestResultSnapshot {
    @Unique private ItemStack jem$quickResult = ItemStack.EMPTY;
    @Override public ItemStack jem$questQuickResult() { return jem$quickResult; }

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void jem$beforeCraft(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        CraftingMenu menu = (CraftingMenu) (Object) this;
        jem$quickResult = slot == 0 && player.containerMenu == menu ? menu.getSlot(0).getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void jem$afterCraft(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        jem$quickResult = ItemStack.EMPTY;
    }
}

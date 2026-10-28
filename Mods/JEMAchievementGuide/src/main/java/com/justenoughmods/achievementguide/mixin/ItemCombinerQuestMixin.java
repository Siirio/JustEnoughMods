package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerQuestMixin implements QuestResultSnapshot {
    @Unique private ItemStack jem$quickResult = ItemStack.EMPTY;

    @Override public ItemStack jem$questQuickResult() { return jem$quickResult; }

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void jem$beforeQuickTake(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        ItemCombinerMenu menu = (ItemCombinerMenu) (Object) this;
        jem$quickResult = slot == menu.getResultSlot() ? menu.getSlot(slot).getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void jem$afterQuickTake(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        jem$quickResult = ItemStack.EMPTY;
    }
}

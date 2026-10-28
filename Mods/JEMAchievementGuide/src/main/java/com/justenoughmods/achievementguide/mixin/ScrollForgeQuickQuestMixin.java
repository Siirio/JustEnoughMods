package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScrollForgeMenu.class, remap = false)
public abstract class ScrollForgeQuickQuestMixin implements QuestResultSnapshot {
    @Unique private ItemStack jem$quickResult = ItemStack.EMPTY;
    @Override public ItemStack jem$questQuickResult() { return jem$quickResult; }

    @Inject(method = "m_7648_", at = @At("HEAD"))
    private void jem$beforeQuickTake(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        ScrollForgeMenu menu = (ScrollForgeMenu) (Object) this;
        jem$quickResult = slot >= 0 && slot < menu.slots.size() && menu.getSlot(slot) == menu.getResultSlot()
                ? menu.getResultSlot().getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "m_7648_", at = @At("RETURN"))
    private void jem$afterQuickTake(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        jem$quickResult = ItemStack.EMPTY;
    }
}

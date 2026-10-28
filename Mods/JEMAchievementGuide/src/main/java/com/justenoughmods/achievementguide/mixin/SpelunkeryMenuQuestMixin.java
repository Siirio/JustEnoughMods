package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.inventory.SpelunkeryTableMenu", remap = false)
public abstract class SpelunkeryMenuQuestMixin implements QuestResultSnapshot {
    @Unique private ItemStack jem$quickResult = ItemStack.EMPTY;
    @Override public ItemStack jem$questQuickResult() { return jem$quickResult; }

    @Inject(method = "m_7648_", at = @At("HEAD"))
    private void jem$beforeWithdrawal(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        jem$quickResult = slot == 2 && player.containerMenu == menu ? menu.getSlot(2).getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "m_7648_", at = @At("RETURN"))
    private void jem$afterWithdrawal(Player player, int slot, CallbackInfoReturnable<ItemStack> callback) {
        jem$quickResult = ItemStack.EMPTY;
    }
}

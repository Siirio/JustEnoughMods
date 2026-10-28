package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestResultSnapshot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A copy survives the native merge emptying its result argument, only within this transaction. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public abstract class BackpackCraftMenuQuestMixin implements QuestResultSnapshot {
    @Unique private ItemStack jem$quickResult = ItemStack.EMPTY;

    @Override public ItemStack jem$questQuickResult() { return jem$quickResult; }

    @Inject(method = "m_7648_", at = @At("HEAD"))
    private void jem$beforeQuickTake(Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        jem$quickResult = ItemStack.EMPTY;
        if ((Object) this instanceof BackpackContainer && player.containerMenu == (Object) this) {
            AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
            if (index >= 0 && index < menu.slots.size() && menu.getSlot(index) instanceof ResultSlot) {
                jem$quickResult = menu.getSlot(index).getItem().copy();
            }
        }
    }

    @Inject(method = "m_7648_", at = @At("RETURN"))
    private void jem$afterQuickTake(Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        jem$quickResult = ItemStack.EMPTY;
    }
}

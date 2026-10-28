package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.TomsFilterProof;
import net.minecraft.nbt.CompoundTag;
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
@Mixin(targets = "com.tom.storagemod.gui.InventoryConnectorFilterMenu", remap = false)
public abstract class TomsFilterButtonsQuestMixin {
    @Unique private CompoundTag jem$filterButton;
    @Unique private CompoundTag jem$filterQuickMove;
    @Inject(method = "m_6366_", at = @At("HEAD"))
    private void jem$beforeButton(Player actor, int button, CallbackInfoReturnable<Boolean> callback) {
        jem$filterButton = TomsFilterProof.beforeConfiguration((AbstractContainerMenu)(Object)this, actor);
    }
    @Inject(method = "m_6366_", at = @At("RETURN"))
    private void jem$afterButton(Player actor, int button, CallbackInfoReturnable<Boolean> callback) {
        var before = jem$filterButton; jem$filterButton = null;
        TomsFilterProof.configured((AbstractContainerMenu)(Object)this, actor, before);
    }
    @Inject(method = "m_7648_", at = @At("HEAD"))
    private void jem$beforeQuickMove(Player actor, int slot, CallbackInfoReturnable<ItemStack> callback) {
        jem$filterQuickMove = TomsFilterProof.beforeConfiguration((AbstractContainerMenu)(Object)this, actor);
    }
    @Inject(method = "m_7648_", at = @At("RETURN"))
    private void jem$afterQuickMove(Player actor, int slot, CallbackInfoReturnable<ItemStack> callback) {
        var before = jem$filterQuickMove; jem$filterQuickMove = null;
        TomsFilterProof.configured((AbstractContainerMenu)(Object)this, actor, before);
    }
}

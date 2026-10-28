package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.TomsFilterProof;
import com.tom.storagemod.gui.StorageTerminalMenu;
import com.tom.storagemod.util.StoredItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tom.storagemod.gui.StorageTerminalMenu", remap = false)
public abstract class TomsFilterTerminalQuestMixin {
    @Inject(method = "onInteract", at = @At("HEAD"))
    private void jem$beforeInteraction(StoredItemStack item, StorageTerminalMenu.SlotAction action, boolean modifier, CallbackInfo callback) {
        TomsFilterProof.begin((StorageTerminalMenu)(Object)this);
    }
    @Inject(method = "onInteract", at = @At("RETURN"))
    private void jem$afterInteraction(StoredItemStack item, StorageTerminalMenu.SlotAction action, boolean modifier, CallbackInfo callback) {
        TomsFilterProof.end((StorageTerminalMenu)(Object)this);
    }
    @Inject(method = "m_7648_", at = @At("HEAD"))
    private void jem$beforeQuickMove(Player actor, int slot, CallbackInfoReturnable<ItemStack> callback) {
        TomsFilterProof.begin((StorageTerminalMenu)(Object)this);
    }
    @Inject(method = "m_7648_", at = @At("RETURN"))
    private void jem$afterQuickMove(Player actor, int slot, CallbackInfoReturnable<ItemStack> callback) {
        TomsFilterProof.end((StorageTerminalMenu)(Object)this);
    }
}

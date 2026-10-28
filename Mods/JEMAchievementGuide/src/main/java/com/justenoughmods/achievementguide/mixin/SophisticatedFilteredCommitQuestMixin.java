package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SophisticatedFilterProof;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native Modifiable overrides do not call the base simple-insertion implementation. */
@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.FilteredItemHandler$Modifiable", remap = false)
public abstract class SophisticatedFilteredCommitQuestMixin {
    @Inject(method = "insertItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void jem$beforeSimpleInsert(ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.beginEndpoint((IItemHandler)this, item, true, simulate);
    }
    @Inject(method = "insertItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void jem$afterSimpleInsert(ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.endEndpoint((IItemHandler)this, callback.getReturnValue());
    }
    @Inject(method = "extractItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void jem$beforeSimpleExtract(ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.beginEndpoint((IItemHandler)this, item, false, simulate);
    }
    @Inject(method = "extractItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void jem$afterSimpleExtract(ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.endEndpoint((IItemHandler)this, callback.getReturnValue());
    }
}

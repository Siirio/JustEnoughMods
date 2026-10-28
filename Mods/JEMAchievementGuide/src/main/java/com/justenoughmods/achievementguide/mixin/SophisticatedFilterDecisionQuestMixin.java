package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.SophisticatedFilterProof;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.FilteredItemHandler", remap = false)
public abstract class SophisticatedFilterDecisionQuestMixin {
    @Inject(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void jem$beforeSlotInsert(int slot, ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.beginEndpoint((IItemHandler)this, item, true, simulate);
    }
    @Inject(method = "insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void jem$afterSlotInsert(int slot, ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.endEndpoint((IItemHandler)this, callback.getReturnValue());
    }
    @Inject(method = "insertItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void jem$beforeSimpleInsert(ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.beginEndpoint((IItemHandler)this, item, true, simulate);
    }
    @Inject(method = "insertItem(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void jem$afterSimpleInsert(ItemStack item, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.endEndpoint((IItemHandler)this, callback.getReturnValue());
    }
    @Inject(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void jem$beforeSlotExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.beginExtraction((IItemHandler)this, slot, simulate);
    }
    @Inject(method = "extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void jem$afterSlotExtract(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> callback) {
        SophisticatedFilterProof.endEndpoint((IItemHandler)this, callback.getReturnValue());
    }
    @Inject(method = "matchesFilters", at = @At("RETURN"))
    private void jem$nativeDecision(ItemStack item, List<FilterLogic> filters, CallbackInfoReturnable<Boolean> callback) {
        SophisticatedFilterProof.decision(this, item, filters, callback.getReturnValue());
    }
}

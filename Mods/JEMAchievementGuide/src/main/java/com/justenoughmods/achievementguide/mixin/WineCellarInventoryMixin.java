package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.WineCellarProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.satisfy.vinery.core.block.entity.StorageBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StorageBlockEntity.class, remap = false)
public abstract class WineCellarInventoryMixin {
    @Inject(method = "setStack", at = @At("HEAD"))
    private void jem$replace(int slot, ItemStack stack, CallbackInfo callback) { WineCellarProof.invalidate((StorageBlockEntity)(Object)this, slot); }
    @Inject(method = "removeStack", at = @At("HEAD"))
    private void jem$remove(int slot, CallbackInfoReturnable<ItemStack> callback) { WineCellarProof.invalidate((StorageBlockEntity)(Object)this, slot); }
    @Inject(method = "setInventory", at = @At("HEAD"))
    private void jem$replaceAll(CallbackInfo callback) { WineCellarProof.clear((StorageBlockEntity)(Object)this); }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) { WineCellarProof.save((StorageBlockEntity)(Object)this, tag); }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) { WineCellarProof.load((StorageBlockEntity)(Object)this, tag); }
}

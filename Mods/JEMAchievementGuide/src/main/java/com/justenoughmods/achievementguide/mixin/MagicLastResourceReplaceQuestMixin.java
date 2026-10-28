package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastResourceProof;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MagicLastResourceReplaceQuestMixin {
    @Inject(method = "setItem", at = @At("HEAD"))
    private void jem$set(int slot, ItemStack item, CallbackInfo callback) { if (slot == 2) MagicLastResourceProof.inserted((BlockEntity)(Object)this, item); }
    @Inject(method = "removeItem", at = @At("HEAD"))
    private void jem$remove(int slot, int count, CallbackInfoReturnable<ItemStack> callback) { if (slot == 2) MagicLastResourceProof.invalidated((BlockEntity)(Object)this); }
    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"))
    private void jem$take(int slot, CallbackInfoReturnable<ItemStack> callback) { if (slot == 2) MagicLastResourceProof.invalidated((BlockEntity)(Object)this); }
    @Inject(method = "clearContent", at = @At("HEAD"))
    private void jem$clear(CallbackInfo callback) { MagicLastResourceProof.invalidated((BlockEntity)(Object)this); }
}

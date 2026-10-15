package com.siirio.jemroles.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies the native held-item drinking pose when JEM starts using a lava bucket for Nether Hearteds. */
@Mixin(Item.class)
public abstract class LavaBucketUseMixin {
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void jemRoles$lavaDrinkDuration(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (stack.is(Items.LAVA_BUCKET)) callback.setReturnValue(32);
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void jemRoles$lavaDrinkAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> callback) {
        if (stack.is(Items.LAVA_BUCKET)) callback.setReturnValue(UseAnim.DRINK);
    }
}

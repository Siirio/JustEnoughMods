package com.siirio.jemvisualsandbox.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class SandboxLavaUseMixin {
    private static boolean sandbox$active() {
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        return server!=null && !server.isDedicatedServer();
    }
    @Inject(method="getUseDuration",at=@At("HEAD"),cancellable=true)
    private void sandbox$duration(ItemStack stack,CallbackInfoReturnable<Integer> result) {
        if(sandbox$active() && stack.is(Items.LAVA_BUCKET))result.setReturnValue(32);
    }
    @Inject(method="getUseAnimation",at=@At("HEAD"),cancellable=true)
    private void sandbox$animation(ItemStack stack,CallbackInfoReturnable<UseAnim> result) {
        if(sandbox$active() && stack.is(Items.LAVA_BUCKET))result.setReturnValue(UseAnim.DRINK);
    }
}

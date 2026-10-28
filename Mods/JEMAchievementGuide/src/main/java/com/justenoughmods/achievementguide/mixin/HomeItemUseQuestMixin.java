package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeItemUseProof;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class HomeItemUseQuestMixin {
    @Inject(method = "useOn", at = @At("HEAD"))
    private void jem$before(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        HomeItemUseProof.begin((ItemStack)(Object)this, context);
    }
    @Inject(method = "useOn", at = @At("RETURN"))
    private void jem$after(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        HomeItemUseProof.finish((ItemStack)(Object)this, context, callback.getReturnValue());
    }
}

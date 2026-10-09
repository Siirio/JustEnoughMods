package com.siirio.jemserver.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.siirio.jemserver.client.smp.EventBoundaryRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class BloodMoonMoonMixin {
    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void jem$redMoon(CallbackInfo ci) {
        if (EventBoundaryRenderer.bloodMoon()) RenderSystem.setShaderColor(1, .12f, .1f, 1);
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void jem$restoreSkyColor(CallbackInfo ci) {
        RenderSystem.setShaderColor(1,1,1,1);
    }
}

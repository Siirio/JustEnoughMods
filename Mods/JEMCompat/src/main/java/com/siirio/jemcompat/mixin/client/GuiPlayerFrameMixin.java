package com.siirio.jemcompat.mixin.client;

import com.siirio.jemcompat.client.render.GuiPlayerRenderScope;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GuiPlayerFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void jem$resetGuiPlayerScope(float partialTick, long finishTime, boolean renderLevel, CallbackInfo callback) { GuiPlayerRenderScope.reset(); }
}

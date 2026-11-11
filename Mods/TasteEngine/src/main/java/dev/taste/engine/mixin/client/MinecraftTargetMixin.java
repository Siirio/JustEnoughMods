package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.taste.engine.render.WorldTargetRouter;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftTargetMixin {
    @Inject(method = "getMainRenderTarget", at = @At("RETURN"), cancellable = true)
    private void taste$routeOwnedWorldTarget(CallbackInfoReturnable<RenderTarget> callback) {
        RenderTarget routed = WorldTargetRouter.current();
        if (routed != null) callback.setReturnValue(routed);
    }
}

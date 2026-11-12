package dev.taste.engine.mixin.client;

import dev.taste.engine.render.TasteRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererColorMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void taste$applyComposedColor(float partialTick, long nanoTime, boolean renderWorld, CallbackInfo callback) {
        TasteRenderer.instance().applyGuiColor();
    }
}

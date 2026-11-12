package dev.taste.engine.mixin.client;

import dev.taste.engine.material.SpriteAnimationClock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
abstract class TextureAtlasSpriteAnimationMixin {
    @Inject(method = "createTicker", at = @At("RETURN"), cancellable = true)
    private void taste$followAnimation(CallbackInfoReturnable<TextureAtlasSprite.Ticker> callback) {
        TextureAtlasSprite.Ticker ticker = callback.getReturnValue();
        if (ticker != null) callback.setReturnValue(SpriteAnimationClock.wrap((TextureAtlasSprite) (Object) this, ticker));
    }
}

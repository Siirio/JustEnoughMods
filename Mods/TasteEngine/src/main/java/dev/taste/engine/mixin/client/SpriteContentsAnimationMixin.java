package dev.taste.engine.mixin.client;

import dev.taste.engine.material.SpriteAnimationClock;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
abstract class SpriteContentsAnimationMixin {
    @Inject(method = "createAnimatedTexture", at = @At("RETURN"))
    private void taste$rememberAnimation(FrameSize frame, int width, int height, AnimationMetadataSection metadata,
            CallbackInfoReturnable<Object> callback) {
        SpriteAnimationClock.register((SpriteContents) (Object) this, frame, width, height, metadata,
                callback.getReturnValue() != null);
    }
}

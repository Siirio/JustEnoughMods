package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererTransparencyAccessor {
    @Accessor("transparencyChain") PostChain taste$transparencyChain();
    @Accessor("translucentTarget") void taste$setTranslucentTarget(RenderTarget target);
    @Accessor("itemEntityTarget") void taste$setItemEntityTarget(RenderTarget target);
    @Accessor("particlesTarget") void taste$setParticlesTarget(RenderTarget target);
    @Accessor("weatherTarget") void taste$setWeatherTarget(RenderTarget target);
    @Accessor("cloudsTarget") void taste$setCloudsTarget(RenderTarget target);
}

package dev.taste.engine.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface EntityDispatcherShadowAccessor {
    @Accessor("shouldRenderShadow")
    boolean taste$renderShadowEnabled();
}

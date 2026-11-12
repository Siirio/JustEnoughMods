package dev.taste.engine.mixin.client;

import dev.taste.engine.render.forward.EntityForwardLighting;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderDispatcher.class)
abstract class EntityDispatcherForwardMixin {
    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FF"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void taste$decorateForwardBuffers(Args args) {
        args.set(4, EntityForwardLighting.wrap(args.get(0), args.get(4)));
    }
}

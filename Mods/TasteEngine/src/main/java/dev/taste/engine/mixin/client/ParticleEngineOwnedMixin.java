package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.taste.engine.render.OwnedParticlePass;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleEngine.class)
abstract class ParticleEngineOwnedMixin {
    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
    private void taste$submitOwnedParticle(Particle particle, VertexConsumer output, Camera camera, float partialTick) {
        if (!OwnedParticlePass.intercept(particle, camera, partialTick)) particle.render(output, camera, partialTick);
    }
}

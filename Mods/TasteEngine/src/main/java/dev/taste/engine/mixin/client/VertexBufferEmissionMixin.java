package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.taste.engine.render.forward.NativeEmissionCapture;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
abstract class VertexBufferEmissionMixin {
    @Inject(method = "_drawWithShader", at = @At("RETURN"))
    private void taste$captureNativeEmission(Matrix4f modelView, Matrix4f projection, ShaderInstance shader,
            CallbackInfo callback) {
        NativeEmissionCapture.replay((VertexBuffer) (Object) this, modelView, projection, shader);
    }
}

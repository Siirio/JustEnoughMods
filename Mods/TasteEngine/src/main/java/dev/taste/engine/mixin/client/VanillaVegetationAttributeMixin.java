package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexFormatElement.class)
abstract class VanillaVegetationAttributeMixin {
    @Inject(method = "setupBufferState", at = @At("RETURN"))
    private void taste$readAnchor(int index, long offset, int stride, CallbackInfo ci) {
        VertexFormatElement element = (VertexFormatElement) (Object) this;
        if (stride == 32 && offset == 28 && element.getUsage() == VertexFormatElement.Usage.NORMAL
                && element.getType() == VertexFormatElement.Type.BYTE && element.getCount() == 3)
            GlStateManager._vertexAttribPointer(index, 4, GL11.GL_BYTE, true, stride, offset);
    }
}

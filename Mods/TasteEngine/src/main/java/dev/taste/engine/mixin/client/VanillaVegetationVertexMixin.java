package dev.taste.engine.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.taste.engine.effects.VegetationMotion;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
abstract class VanillaVegetationVertexMixin {
    @Shadow private ByteBuffer buffer;
    @Shadow private int nextElementByte;
    @Shadow private VertexFormat format;

    @Inject(method = "endVertex", at = @At("RETURN"))
    private void taste$anchor(CallbackInfo ci) {
        if (format == DefaultVertexFormat.BLOCK) taste$writeAnchor(nextElementByte - 32);
    }

    @Inject(method = "putBulkData", at = @At("RETURN"), remap = false)
    private void taste$bulkAnchors(ByteBuffer source, CallbackInfo ci) {
        if (format != DefaultVertexFormat.BLOCK || source.limit() % 32 != 0) return;
        for (int offset = nextElementByte - source.limit(); offset < nextElementByte; offset += 32) taste$writeAnchor(offset);
    }

    @Unique
    private void taste$writeAnchor(int start) {
        if (start >= 0) buffer.put(start + 31, (byte) VegetationMotion.vertex(buffer.getFloat(start + 4)));
    }
}

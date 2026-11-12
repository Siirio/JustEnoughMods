package dev.taste.engine.mixin.client;

import dev.taste.engine.render.embeddium.EmbeddiumVegetation;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl.VanillaLikeChunkVertex", remap = false)
abstract class EmbeddiumVegetationExpandedMixin {
    @Inject(method = "lambda$getEncoder$0", at = @At("RETURN"), require = 0)
    private static void taste$anchor(long address, @Coerce Object material, @Coerce Object vertex,
            int section, CallbackInfoReturnable<Long> cir) {
        int flags = EmbeddiumVegetation.flags(vertex);
        if (flags != 0) MemoryUtil.memPutInt(address + 24L, MemoryUtil.memGetInt(address + 24L) | flags);
    }
}

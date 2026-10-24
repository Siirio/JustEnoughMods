package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.revival.NativeBossStorage;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the actual write future without competing with native EntityStorage redirects. */
@Mixin(IOWorker.class)
public abstract class NativeBossWriteAckMixin {
    @Inject(method = "store(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private void jemWorldBossTiers$writeAcknowledgement(ChunkPos chunk, CompoundTag tag,
            CallbackInfoReturnable<CompletableFuture<Void>> result) {
        NativeBossStorage.capture((IOWorker) (Object) this, chunk, tag, result.getReturnValue());
    }
}

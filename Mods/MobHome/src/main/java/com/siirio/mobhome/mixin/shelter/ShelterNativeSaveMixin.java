package com.siirio.mobhome.mixin.shelter;

import com.siirio.mobhome.feature.shelter.ShelterPersistence;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Expose the actual native write future only to the synchronous shelter commit in progress. */
@Mixin(EntityStorage.class)
public abstract class ShelterNativeSaveMixin {
    @Redirect(method = "storeEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/IOWorker;store(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> jemcompat$captureCompanionWrite(IOWorker worker, ChunkPos chunk, CompoundTag tag) {
        CompletableFuture<Void> future = worker.store(chunk, tag);
        ShelterPersistence.capture(worker, chunk, tag, future);
        return future;
    }
}

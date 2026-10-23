package com.siirio.jemcompat.mixin.shelter;

import com.siirio.jemcompat.feature.shelter.CompanionData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Acknowledge actual native entity IO, rather than confusing addFreshEntity with a durable save. */
@Mixin(EntityStorage.class)
public abstract class ShelterNativeSaveMixin {
    @Shadow @Final private ServerLevel level;
    @Unique private final Map<Long, Long> jemcompat$latestWrite = new HashMap<>();
    @Unique private long jemcompat$writeSequence;

    @Redirect(method = "storeEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/IOWorker;store(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> jemcompat$acknowledgeCompanionSave(IOWorker worker, ChunkPos chunk, CompoundTag tag) {
        long sequence = ++jemcompat$writeSequence; long key = chunk.toLong(); jemcompat$latestWrite.put(key, sequence);
        Map<UUID, Long> epochs = new HashMap<>();
        if (tag != null) for (Tag element : tag.getList("Entities", Tag.TAG_COMPOUND)) {
            CompoundTag entity = (CompoundTag) element; long epoch = entity.getCompound("ForgeData").getLong(CompanionData.ENTITY_EPOCH);
            if (epoch > 0 && entity.hasUUID("UUID")) epochs.put(entity.getUUID("UUID"), epoch);
        }
        CompletableFuture<Void> future = worker.store(chunk, tag);
        future.whenComplete((ignored, failure) -> level.getServer().execute(() -> {
            if (!Long.valueOf(sequence).equals(jemcompat$latestWrite.get(key))) return;
            jemcompat$latestWrite.remove(key);
            if (failure != null || epochs.isEmpty()) return;
            CompanionData data = CompanionData.get(level); boolean changed = false;
            for (var entry : epochs.entrySet()) {
                CompanionData.Pet pet = data.pet(entry.getKey());
                if (pet != null && pet.restoring && pet.epoch == entry.getValue() && pet.pending != null) {
                    pet.pending = null; pet.restoring = false; pet.blocked = ""; changed = true;
                }
            }
            if (changed) data.flush(level);
        }));
        return future;
    }
}

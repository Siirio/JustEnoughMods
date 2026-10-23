package com.siirio.jemcompat.feature.shelter;

import com.siirio.jemcompat.mixin.shelter.ShelterEntityManagerAccess;
import com.siirio.jemcompat.mixin.shelter.ShelterLevelAccess;
import com.siirio.jemcompat.mixin.shelter.ShelterStorageAccess;
import java.util.UUID;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;

/** Native chunks for one companion and its independently persisted parts commit before simulation resumes. */
public final class ShelterPersistence {
    private static final ThreadLocal<Write> CURRENT = new ThreadLocal<>();

    private static final class Write {
        final IOWorker worker;
        final long chunk;
        final java.util.Set<UUID> ids;
        final long epoch;
        CompletableFuture<Void> future;
        final java.util.Set<UUID> included = new java.util.HashSet<>();
        Write(IOWorker worker, long chunk, java.util.Set<UUID> ids, long epoch) {
            this.worker = worker; this.chunk = chunk; this.ids = ids; this.epoch = epoch;
        }
    }
    private ShelterPersistence() {}

    public static boolean ready(ServerLevel level, ChunkPos chunk) {
        return level.hasChunkAt(chunk.getWorldPosition()) && level.areEntitiesLoaded(chunk.toLong());
    }

    public static boolean ready(ServerLevel level, Entity entity) {
        return CompanionAdapters.persistenceEntities(entity).stream().allMatch(part -> ready(level, part.chunkPosition()));
    }

    /** Called by the EntityStorage redirect on the same server thread that requested serialization. */
    public static void capture(IOWorker worker, ChunkPos chunk, CompoundTag tag, CompletableFuture<Void> future) {
        Write write = CURRENT.get();
        if (write == null || write.worker != worker || write.chunk != chunk.toLong()) return;
        if (write.future != null) throw new IllegalStateException("Multiple native writes in one shelter commit");
        write.future = future;
        if (tag == null) return;
        for (Tag row : tag.getList("Entities", Tag.TAG_COMPOUND)) {
            CompoundTag entity = (CompoundTag) row;
            if (entity.hasUUID("UUID") && write.ids.contains(entity.getUUID("UUID"))
                    && entity.getCompound("ForgeData").getLong(CompanionData.ENTITY_EPOCH) == write.epoch) {
                write.included.add(entity.getUUID("UUID"));
            }
        }
    }

    /**
     * Requires an accepted native entity and an already-durable restoring intent. Failure propagates;
     * the caller must not release a partially committed entity to ordinary simulation/interaction.
     */
    public static void commit(ServerLevel level, Entity entity, CompanionData data, CompanionData.Pet pet) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Shelter commit requires server thread");
        if (CURRENT.get() != null) throw new IllegalStateException("Nested shelter native commit");
        ChunkPos chunk = entity.chunkPosition();
        if (!ready(level, chunk) || entity.level() != level || level.getEntity(pet.id) != entity
                || !pet.restoring || pet.pending == null || !entity.getUUID().equals(pet.id)
                || entity.getPersistentData().getLong(CompanionData.ENTITY_EPOCH) != pet.epoch) {
            throw new IllegalStateException("Invalid shelter native commit state");
        }
        var manager = ((ShelterLevelAccess) level).jemcompat$entityManager();
        var access = (ShelterEntityManagerAccess) manager;
        if (!(access.jemcompat$permanentStorage() instanceof EntityStorage storage)) {
            throw new IllegalStateException("Unsupported native companion entity storage");
        }
        IOWorker worker = ((ShelterStorageAccess) storage).jemcompat$worker();
        List<Entity> nativeEntities = CompanionAdapters.persistenceEntities(entity);
        if (nativeEntities.isEmpty() || !nativeEntities.contains(entity)) throw new IllegalStateException("Missing native companion head");
        var chunks = new LinkedHashMap<Long, java.util.Set<UUID>>();
        var identities = new java.util.HashSet<UUID>();
        for (Entity part : nativeEntities) {
            if (!identities.add(part.getUUID()) || part.level() != level || level.getEntity(part.getUUID()) != part
                    || part.getPersistentData().getLong(CompanionData.ENTITY_EPOCH) != pet.epoch
                    || !ready(level, part.chunkPosition())) {
                throw new IllegalStateException("Incomplete native companion persistence chain");
            }
            chunks.computeIfAbsent(part.chunkPosition().toLong(), ignored -> new java.util.HashSet<>()).add(part.getUUID());
        }
        var writes = new ArrayList<Write>();
        try {
            // Native no-op post-store callback saves live entities without unloading them or changing statuses.
            for (var entry : chunks.entrySet()) {
                Write write = new Write(worker, entry.getKey(), entry.getValue(), pet.epoch);
                CURRENT.set(write);
                if (!access.jemcompat$storeChunkSections(write.chunk, ignored -> {}) || write.future == null
                        || !write.included.containsAll(write.ids)) {
                    throw new IllegalStateException("Native shelter write omitted a restored UUID/epoch");
                }
                writes.add(write);
                CURRENT.remove();
            }
            // IOWorker's mailbox uses Util.ioPool; neither completion requires a server-thread continuation.
            for (Write write : writes) write.future.join();
            worker.synchronize(true).join();
            CompoundTag pending = pet.pending;
            boolean restoring = pet.restoring;
            String blocked = pet.blocked;
            pet.pending = null; pet.restoring = false; pet.blocked = "";
            try { data.flush(level); }
            catch (RuntimeException failure) {
                pet.pending = pending; pet.restoring = restoring; pet.blocked = blocked; data.setDirty();
                throw failure;
            }
        } finally { CURRENT.remove(); }
    }
}

package com.siirio.jemworldbosstiers.revival;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeBossStorageTest {
    @Test void failedNativeWriteCannotAcknowledgeSpawn() {
        UUID boss = UUID.randomUUID();
        CompoundTag entity = new CompoundTag();
        entity.putUUID("UUID", boss);
        ListTag entities = new ListTag(); entities.add(entity);
        CompoundTag chunk = new CompoundTag(); chunk.put("Entities", entities);
        var write = new NativeBossStorage.Write(null, new ChunkPos(0, 0), boss, true);
        write.observed(chunk, CompletableFuture.failedFuture(new IOException("disk full")));
        assertThrows(CompletionException.class, write::await);
    }

    @Test void missingUuidCannotBeAcknowledgedAsCreated() {
        var write = new NativeBossStorage.Write(null, new ChunkPos(0, 0), UUID.randomUUID(), true);
        assertThrows(IllegalStateException.class, () -> write.observed(new CompoundTag(), CompletableFuture.completedFuture(null)));
        assertThrows(IllegalStateException.class, write::await);
    }

    @Test void retirementRequiresSerializedAbsenceIncludingPassengers() {
        UUID boss = UUID.randomUUID();
        CompoundTag passenger = new CompoundTag(); passenger.putUUID("UUID", boss);
        ListTag passengers = new ListTag(); passengers.add(passenger);
        CompoundTag mount = new CompoundTag(); mount.put("Passengers", passengers);
        ListTag entities = new ListTag(); entities.add(mount);
        CompoundTag chunk = new CompoundTag(); chunk.put("Entities", entities);
        var write = new NativeBossStorage.Write(null, new ChunkPos(0, 0), boss, false);
        assertThrows(IllegalStateException.class, () -> write.observed(chunk, CompletableFuture.completedFuture(null)));
        write.observed(null, CompletableFuture.completedFuture(null));
        assertDoesNotThrow(write::await);
    }
}

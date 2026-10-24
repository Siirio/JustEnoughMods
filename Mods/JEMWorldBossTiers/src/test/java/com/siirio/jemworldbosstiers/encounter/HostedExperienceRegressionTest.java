package com.siirio.jemworldbosstiers.encounter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

class HostedExperienceRegressionTest {
    @TempDir Path directory;

    @BeforeAll static void version() { net.minecraft.SharedConstants.tryDetectVersion(); }

    private MinecraftServer server() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getWorldPath(LevelResource.ROOT)).thenReturn(directory);
        return server;
    }

    @Test void initialWriteFailureRetainsOriginalPersonalExperienceForRetry() throws Exception {
        HostedRewards data = new HostedRewards(new CompoundTag());
        MinecraftServer server = server();
        UUID recipient = UUID.randomUUID();
        Files.writeString(directory.resolve("data"), "blocked storage");
        assertThrows(IllegalStateException.class, () -> data.enqueueExperience(server, "attempt", List.of(recipient), 120));
        assertEquals(120, data.pending().getCompound("ExperienceBatch:attempt").getInt(recipient.toString()));
        Files.delete(directory.resolve("data"));
        data.enqueueExperience(server, "attempt", List.of(recipient), 999);
        assertEquals(120, data.pending().getCompound("Experience:" + recipient).getInt("attempt"));
    }

    @Test void restartPreservesOfflineXpAndDuplicateCaptureCannotReplaceAmount() throws Exception {
        HostedRewards data = new HostedRewards(new CompoundTag());
        MinecraftServer server = server();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        data.enqueueExperience(server, "attempt", List.of(first, second), 120);
        CompoundTag saved = NbtIo.readCompressed(directory.resolve("data/jem_hosted_boss_rewards.dat").toFile()).getCompound("data");
        HostedRewards restarted = new HostedRewards(saved);
        restarted.enqueueExperience(server, "attempt", List.of(first, second), 999);
        assertEquals(120, restarted.pending().getCompound("Experience:" + first).getInt("attempt"));
        assertEquals(120, restarted.pending().getCompound("Experience:" + second).getInt("attempt"));
        assertFalse(restarted.pending().contains("ExperienceBatch:attempt"));
    }

    @Test void preparedOfflineXpReplaysWithoutAStillLoadedBossOrSettlementCallback() throws Exception {
        UUID recipient = UUID.randomUUID();
        CompoundTag snapshot = new CompoundTag(), root = new CompoundTag();
        snapshot.putInt(recipient.toString(), 120);
        root.put("ExperienceBatch:attempt", snapshot);
        HostedRewards restarted = new HostedRewards(root);
        restarted.retryExperienceBatches(server());
        assertEquals(120, restarted.pending().getCompound("Experience:" + recipient).getInt("attempt"));
        assertTrue(restarted.pending().getCompound("ExperienceIssued").getBoolean("attempt"));
        assertFalse(restarted.pending().contains("ExperienceBatch:attempt"));
    }

    @Test void nativeAutosaveFailureRetainsXpJournalAndDoesNotAbortServer() throws Exception {
        HostedRewards data = new HostedRewards(new CompoundTag());
        Files.writeString(directory.resolve("data"), "blocked storage");
        data.setDirty();
        assertDoesNotThrow(() -> data.save(directory.resolve("data/jem_hosted_boss_rewards.dat").toFile()));
        assertTrue(data.isDirty());
    }
}

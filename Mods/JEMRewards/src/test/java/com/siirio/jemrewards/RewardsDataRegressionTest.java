package com.siirio.jemrewards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

class RewardsDataRegressionTest {
    @TempDir Path directory;

    @BeforeAll static void version() { net.minecraft.SharedConstants.tryDetectVersion(); }

    private MinecraftServer server() {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getWorldPath(LevelResource.ROOT)).thenReturn(directory);
        when(server.getPlayerList()).thenReturn(mock(PlayerList.class));
        return server;
    }

    private static ListTag grant(UUID owner, String value) {
        CompoundTag container = new CompoundTag();
        container.putString("SourceId", "attempt");
        container.putString("FrozenRoll", value);
        CompoundTag row = new CompoundTag();
        row.putUUID("Player", owner);
        row.put("Container", container);
        ListTag grants = new ListTag();
        grants.add(row);
        return grants;
    }

    @Test void firstWriteFailureRetainsExactRollAndNeverAcknowledgesDurability() throws Exception {
        RewardsData data = new RewardsData(new CompoundTag());
        MinecraftServer server = server();
        UUID owner = UUID.randomUUID();
        Files.writeString(directory.resolve("data"), "blocked storage");
        assertThrows(IllegalStateException.class, () -> data.enqueueBatch(server, "attempt", grant(owner, "first")));
        assertEquals("first", data.pending().getList("Batch:attempt", 10).getCompound(0).getCompound("Container").getString("FrozenRoll"));
        assertFalse(data.batchCommitted("attempt"));
        Files.delete(directory.resolve("data"));
        data.enqueueBatch(server, "attempt", grant(owner, "rerolled"));
        assertTrue(data.batchCommitted("attempt"));
        assertEquals("first", data.pending().getList("RewardContainers:" + owner, 10).getCompound(0).getString("FrozenRoll"));
    }

    @Test void restartAndDuplicateCallbackCannotIssueASecondPersonalGrant() throws Exception {
        MinecraftServer server = server();
        UUID owner = UUID.randomUUID();
        RewardsData data = new RewardsData(new CompoundTag());
        data.enqueueBatch(server, "attempt", grant(owner, "first"));
        CompoundTag persisted = NbtIo.readCompressed(directory.resolve("data/jem_rewards.dat").toFile()).getCompound("data");
        RewardsData restarted = new RewardsData(persisted);
        restarted.enqueueBatch(server, "attempt", grant(owner, "second"));
        assertEquals(1, restarted.pending().getList("RewardContainers:" + owner, 10).size());
        assertTrue(restarted.batchCommitted("attempt"));
    }

    @Test void offlineRecipientsReceiveIndependentFullContainers() {
        MinecraftServer server = server();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        ListTag grants = grant(first, "full loot");
        grants.add(grant(second, "full loot").getCompound(0));
        RewardsData data = new RewardsData(new CompoundTag());
        data.enqueueBatch(server, "attempt", grants);
        assertEquals("full loot", data.pending().getList("RewardContainers:" + first, 10).getCompound(0).getString("FrozenRoll"));
        assertEquals("full loot", data.pending().getList("RewardContainers:" + second, 10).getCompound(0).getString("FrozenRoll"));
        data.pending().getList("RewardContainers:" + first, 10).getCompound(0).putString("FrozenRoll", "claimed");
        assertEquals("full loot", data.pending().getList("RewardContainers:" + second, 10).getCompound(0).getString("FrozenRoll"));
    }

    @Test void ordinaryAutosaveIoFailureRemainsDirtyWithoutKillingRetryProcess() throws Exception {
        RewardsData data = new RewardsData(new CompoundTag());
        Files.writeString(directory.resolve("data"), "blocked storage");
        data.setDirty();
        assertDoesNotThrow(() -> data.save(directory.resolve("data/jem_rewards.dat").toFile()));
        assertTrue(data.isDirty());
    }
}

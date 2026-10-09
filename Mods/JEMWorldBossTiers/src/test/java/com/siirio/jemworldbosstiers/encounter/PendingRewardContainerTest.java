package com.siirio.jemworldbosstiers.encounter;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PendingRewardContainerTest {
    @Test
    void completedContainerKeepsLaterSlotsStableAcrossReloadUntilAllItemsAreTaken() {
        UUID player = UUID.randomUUID();
        ListTag containers = new ListTag();
        for (int index = 0; index < 2; index++) {
            CompoundTag container = new CompoundTag();
            container.putUUID("Id", UUID.randomUUID());
            ListTag slots = new ListTag();
            CompoundTag item = new CompoundTag();
            item.putString("id", "minecraft:diamond");
            item.putByte("Count", (byte) (index == 0 ? 0 : 7));
            slots.add(item);
            container.put("Items", slots);
            containers.add(container);
        }
        UUID secondId = containers.getCompound(1).getUUID("Id");
        CompoundTag data = new CompoundTag();
        data.put("RewardContainers:" + player, containers);
        HostedRewards storage = new HostedRewards(data);
        PendingRewardContainer.clearCompleted(storage, player, containers);
        storage = new HostedRewards(storage.save(new CompoundTag()));
        containers = PendingRewardContainer.containers(storage, player);
        assertEquals(2, containers.size());
        assertEquals(secondId, containers.getCompound(1).getUUID("Id"));
        assertEquals(0, containers.getCompound(0).getList("Items", Tag.TAG_COMPOUND).getCompound(0).getByte("Count"));
        containers.getCompound(1).getList("Items", Tag.TAG_COMPOUND).getCompound(0).putByte("Count", (byte) 0);
        PendingRewardContainer.clearCompleted(storage, player, containers);
        assertTrue(PendingRewardContainer.containers(storage, player).isEmpty());
    }

    @Test
    void legacyMigrationIsPersistentAndIdempotentWithoutTouchingRecovery() {
        UUID player = UUID.randomUUID();
        CompoundTag original = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:diamond_block");
        item.putByte("Count", (byte) 7);
        CompoundTag enchantment = new CompoundTag();
        enchantment.putString("custom", "preserved");
        item.put("tag", enchantment);
        items.add(item);
        original.put(player.toString(), items);
        original.putDouble("Recovery:" + player, 0.8);
        HostedRewards storage = new HostedRewards(original);
        ListTag migrated = PendingRewardContainer.containers(storage, player);
        assertEquals(1, migrated.size());
        assertEquals(items, migrated.getCompound(0).getList("Items", Tag.TAG_COMPOUND));
        assertFalse(original.contains(player.toString()));
        UUID stableId = migrated.getCompound(0).getUUID("Id");
        for (int login = 0; login < 1000; login++) {
            storage = new HostedRewards(storage.save(new CompoundTag()));
            ListTag reloaded = PendingRewardContainer.containers(storage, player);
            assertEquals(1, reloaded.size());
            assertEquals(stableId, reloaded.getCompound(0).getUUID("Id"));
            assertEquals(items, reloaded.getCompound(0).getList("Items", Tag.TAG_COMPOUND));
            assertEquals(0.8, storage.pending().getDouble("Recovery:" + player));
        }
    }
}

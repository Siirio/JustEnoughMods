package com.siirio.jemworldbosstiers.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.siirio.jemworldbosstiers.revival.ArenaRecord;

class WorldTierDataTest {
    @Test
    void savesSchemaVersionTwoWithoutDiscardingHistoricalDefeats() {
        WorldTierData data = new WorldTierData();
        ResourceLocation boss = ResourceLocation.fromNamespaceAndPath("test", "boss");
        data.recordDefeat(boss);

        CompoundTag saved = data.save(new CompoundTag());
        WorldTierData loaded = WorldTierData.load(saved);

        assertEquals(2, saved.getInt("SchemaVersion"));
        assertTrue(loaded.defeatedBosses().contains(boss));
    }

    @Test
    void arenaUnlockAndActiveEncounterPersist() {
        WorldTierData data = new WorldTierData();
        ResourceLocation boss = ResourceLocation.fromNamespaceAndPath("test", "boss");
        ArenaRecord arena = ArenaRecord.create("arena-1", boss, "minecraft:overworld", 0, 0, 0, 20, 20, 20).unlock()
                .activate(java.util.UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), 50L).orElseThrow();
        data.putArena(arena);

        WorldTierData loaded = WorldTierData.load(data.save(new CompoundTag()));

        assertEquals(arena, loaded.arena("arena-1").orElseThrow());
    }
}

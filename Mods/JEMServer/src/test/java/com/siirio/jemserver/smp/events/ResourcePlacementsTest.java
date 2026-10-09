package com.siirio.jemserver.smp.events;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourcePlacementsTest {
    @Test
    void densePlacementHistoryCompactsWithoutDisablingOtherChunksAfterReload() {
        var data = new ResourcePlacements();
        for (int x = 0; x < 4; x++) data.record("minecraft:overworld", BlockPos.asLong(x, 64, 0), 4);
        data.record("minecraft:overworld", BlockPos.asLong(32, 64, 0), 4);
        data = ResourcePlacements.load(data.save(new CompoundTag()));
        assertTrue(data.excluded("minecraft:overworld", BlockPos.asLong(15, 80, 15)));
        assertTrue(data.excluded("minecraft:overworld", BlockPos.asLong(32, 64, 0)));
        assertFalse(data.excluded("minecraft:overworld", BlockPos.asLong(48, 64, 0)));
        assertFalse(data.excluded("minecraft:the_nether", BlockPos.asLong(0, 64, 0)));
        assertEquals(1, data.save(new CompoundTag()).getCompound("chunks").getLongArray("minecraft:overworld").length);
    }

    @Test
    void sparseHistoryKeepsBoundedFailClosedFallbackAndLegacyHistory() {
        var data = new ResourcePlacements();
        for (int x = 0; x < 5; x++) data.record("minecraft:overworld", BlockPos.asLong(x * 16, 64, 0), 4);
        var saved = data.save(new CompoundTag());
        assertTrue(saved.getBoolean("full"));
        assertEquals(4, saved.getCompound("dimensions").getLongArray("minecraft:overworld").length);
        assertTrue(ResourcePlacements.load(saved).excluded("minecraft:the_end", BlockPos.asLong(0, 64, 0)));
        saved.remove("chunks");
        saved.putBoolean("full", false);
        assertTrue(ResourcePlacements.load(saved).excluded("minecraft:overworld", BlockPos.asLong(16, 64, 0)));
    }
}

package com.siirio.jemcompat.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CampaignSavedDataLocatorTest {
    @Test
    void remembersLocatedStructureByCampaignTarget() {
        CampaignSavedData data = new CampaignSavedData();
        BlockPos position = new BlockPos(512, 72, -384);

        assertNull(data.locatedStructure("main/maledictus"));
        data.locatedStructure("main/maledictus", position);

        assertEquals(position, data.locatedStructure("main/maledictus"));
    }

    @Test
    void savesLocatedStructuresInCampaignData() {
        CampaignSavedData data = new CampaignSavedData();
        BlockPos position = new BlockPos(-1024, 64, 2048);
        data.locatedStructure("main/ancient_remnant", position);

        CompoundTag saved = data.save(new CompoundTag());

        assertEquals("main/ancient_remnant", saved.getList("LocatedStructures", CompoundTag.TAG_COMPOUND)
                .getCompound(0).getString("Target"));
        assertEquals(position.asLong(), saved.getList("LocatedStructures", CompoundTag.TAG_COMPOUND)
                .getCompound(0).getLong("Position"));
    }
}

package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RetiredRaidStateRegressionTest {
    @Test void legacyReinforcementStateCannotReviveWavesAndUnrelatedDataSurvives() {
        var tag = new CompoundTag();
        var event = UUID.randomUUID(); tag.putUUID("id", event);
        tag.putInt("raidAlliesSpawned", 20); tag.putInt("raidReinforcementStage", 2);
        var allies = new ListTag(); allies.add(StringTag.valueOf(UUID.randomUUID().toString())); tag.put("raidAllies", allies);
        tag.put("raidBossInitialHealth", new CompoundTag()); tag.putString("legacyEvidence", "keep");
        var saved = EventNbtCodec.encode(EventNbtCodec.decodeEventState(tag));
        assertFalse(saved.contains("raidAlliesSpawned")); assertFalse(saved.contains("raidReinforcementStage"));
        assertFalse(saved.contains("raidAllies")); assertFalse(saved.contains("raidBossInitialHealth"));
        assertEquals(event, saved.getUUID("id")); assertEquals("keep", saved.getString("legacyEvidence"));
    }
}

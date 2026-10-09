package com.siirio.jemserver.smp.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BloodMoonDeathsTest {
    @Test
    void finishedEventsRetainUnclaimedSafeDeathInventory() {
        var event = new CompoundTag();
        event.putString("activity", "BLOOD_MOON");
        event.putString("state", "COMPLETED");
        var members = new CompoundTag();
        var player = new CompoundTag();
        player.put("safeInventory", new ListTag());
        members.put("player", player);
        event.put("members", members);
        assertTrue(BloodMoonDeaths.hasRecovery(event));
        player.remove("safeInventory");
        assertFalse(BloodMoonDeaths.hasRecovery(event));
        player.putBoolean("safeRespawn", true);
        assertTrue(BloodMoonDeaths.hasRecovery(event));
    }
}

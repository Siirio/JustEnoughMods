package com.siirio.jemserver.smp.events;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviveSupportTest {
    @Test
    void persistedActivePartyKeepsAcceptedMultiplayerSessionAcrossReload() {
        UUID player = UUID.randomUUID();
        var party = party(player);
        var saved = party.copy();
        assertEquals(party.getUUID("bossEntity"), ReviveSupport.persistedSession(saved, player, "minecraft:overworld"));
        assertNull(ReviveSupport.persistedSession(saved, UUID.randomUUID(), "minecraft:overworld"));
        assertNull(ReviveSupport.persistedSession(saved, player, "minecraft:the_nether"));
        saved.getCompound("members").getCompound(player.toString()).putBoolean("accepted", false);
        assertNull(ReviveSupport.persistedSession(saved, player, "minecraft:overworld"));
    }

    @Test
    void endedUnstartedSoloAndBloodMoonPartiesCannotRestoreHostedSession() {
        UUID player = UUID.randomUUID();
        var party = party(player);
        for (String state : new String[]{"DRAFT", "CLOSED", "CANCELLED", "COMPLETED", "FAILED", "SKIPPED"}) {
            party.putString("state", state);
            assertNull(ReviveSupport.persistedSession(party, player, "minecraft:overworld"));
        }
        party.putString("state", "ACTIVE");
        party.putBoolean("solo", true);
        assertNull(ReviveSupport.persistedSession(party, player, "minecraft:overworld"));
        party.putBoolean("solo", false);
        party.putBoolean("bloodMoon", true);
        assertNull(ReviveSupport.persistedSession(party, player, "minecraft:overworld"));
        party.putBoolean("bloodMoon", false);
        party.getCompound("members").getAllKeys().removeIf(key -> !key.equals(player.toString()));
        assertNull(ReviveSupport.persistedSession(party, player, "minecraft:overworld"));
    }

    @Test
    void activeNativeRitualUsesPartySessionUntilBossAppears() {
        UUID player = UUID.randomUUID();
        var party = party(player);
        party.putUUID("id", UUID.randomUUID());
        party.remove("bossEntity");
        assertNull(ReviveSupport.persistedSession(party, player, "minecraft:overworld"));
        party.putBoolean("nativePending", true);
        assertEquals(party.getUUID("id"), ReviveSupport.persistedSession(party.copy(), player, "minecraft:overworld"));
        party.putString("state", "COMPLETED");
        assertNull(ReviveSupport.persistedSession(party, player, "minecraft:overworld"));
    }

    private static CompoundTag party(UUID player) {
        var party = new CompoundTag();
        party.putString("activity", "BOSS");
        party.putString("state", "ACTIVE");
        party.putString("dimension", "minecraft:overworld");
        party.putUUID("bossEntity", UUID.randomUUID());
        var members = new CompoundTag();
        for (UUID id : new UUID[]{player, UUID.randomUUID()}) {
            var member = new CompoundTag();
            member.putBoolean("accepted", true);
            members.put(id.toString(), member);
        }
        party.put("members", members);
        return party;
    }
}

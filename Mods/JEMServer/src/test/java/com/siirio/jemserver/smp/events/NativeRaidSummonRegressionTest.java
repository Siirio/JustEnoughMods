package com.siirio.jemserver.smp.events;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class NativeRaidSummonRegressionTest extends NativeRegistryFixture {
    @Test void terminalCleanupPreservesEveryNativeSummonIncludingBeyondTheRetiredCap() {
        var event=UUID.randomUUID();
        for(int count=0;count<25;count++) {
            var nativeMob=mock(Entity.class);
            var tag=new CompoundTag();
            tag.putString("forge:spawn_type","MOB_SUMMONED");
            tag.putUUID(RaidEvent.EVENT_ID,event); tag.putUUID(EventSession.SESSION,event);
            tag.putBoolean(EventSession.SPAWNED,true); tag.putString(EventSession.TYPE,"boss_raid");
            tag.putString("native_mod_state","keep"); when(nativeMob.getPersistentData()).thenReturn(tag);
            assertTrue(RaidEvent.clearNativeSummon(nativeMob));
            assertFalse(tag.hasUUID(RaidEvent.EVENT_ID)); assertFalse(tag.hasUUID(EventSession.SESSION));
            assertEquals("keep",tag.getString("native_mod_state"));
            assertEquals("MOB_SUMMONED",tag.getString("forge:spawn_type"));
            verify(nativeMob,never()).discard();
            assertFalse(RaidEvent.clearNativeSummon(nativeMob));
        }
    }

    @Test void cleanupDoesNotEraseAnUnrelatedEventSession() {
        var nativeMob=mock(Entity.class); var tag=new CompoundTag();
        tag.putString("forge:spawn_type","MOB_SUMMONED");
        tag.putUUID(RaidEvent.EVENT_ID,UUID.randomUUID());
        var unrelated=UUID.randomUUID(); tag.putUUID(EventSession.SESSION,unrelated);
        when(nativeMob.getPersistentData()).thenReturn(tag);
        assertTrue(RaidEvent.clearNativeSummon(nativeMob));
        assertEquals(unrelated,tag.getUUID(EventSession.SESSION));
        verify(nativeMob,never()).discard();
    }
}

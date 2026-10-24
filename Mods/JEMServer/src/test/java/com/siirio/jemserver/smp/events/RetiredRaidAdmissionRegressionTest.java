package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.EventState;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class RetiredRaidAdmissionRegressionTest extends NativeRegistryFixture {
    @Test void canceledRetiredBossCannotBeReadmittedByAnActiveRaid() {
        var server=mock(MinecraftServer.class);
        var level=mock(ServerLevel.class); when(level.getServer()).thenReturn(server);
        var entity=mock(Entity.class); var bossId=UUID.randomUUID();
        when(entity.getUUID()).thenReturn(bossId);
        var attempt=UUID.randomUUID(); var tag=new CompoundTag(); tag.putUUID(RaidEvent.EVENT_ID,attempt);
        when(entity.getPersistentData()).thenReturn(tag);
        var active=new EventState(); active.id(attempt); active.state("ACTIVE"); active.activity("BOSS_RAID");
        var smp=mock(SmpData.class,RETURNS_DEEP_STUBS);
        when(smp.events().current(attempt)).thenReturn(active);
        var ownership=new WorldTierData(); ownership.retireBossIdentity("registered-arena",bossId);
        try(var tiers=mockStatic(WorldTierData.class); var stores=mockStatic(SmpData.class);
            var runtime=mockStatic(EventRuntime.class); var moon=mockStatic(BloodMoon.class)) {
            tiers.when(()->WorldTierData.get(server)).thenReturn(ownership);
            stores.when(()->SmpData.get(server)).thenReturn(smp);
            runtime.when(()->EventRuntime.enabled(server)).thenReturn(true);
            runtime.when(()->EventRuntime.available(server,active)).thenReturn(true);
            moon.when(()->BloodMoon.adoptSummon(level,entity)).thenReturn(true);
            var arrival=new EntityJoinLevelEvent(entity,level,true);
            arrival.setCanceled(true);
            new EventHooks().entity(arrival);
            assertTrue(arrival.isCanceled());
            assertTrue(ownership.bossIdentityRetired(bossId));
            assertEquals("ACTIVE",active.state());
            moon.verifyNoInteractions();
        }
    }
}

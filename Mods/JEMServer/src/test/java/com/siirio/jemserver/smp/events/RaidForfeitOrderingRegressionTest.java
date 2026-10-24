package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.EventState;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaidForfeitOrderingRegressionTest extends NativeRegistryFixture {
    @Test void hostRaidForfeitClosesCanonicalCombatBeforeReplacementAndEventReset() {
        var server = mock(MinecraftServer.class); var level = mock(ServerLevel.class);
        var boss = mock(LivingEntity.class); var world = mock(WorldTierData.class);
        var event = new EventState(); var bossId = UUID.randomUUID(); var attempt = UUID.randomUUID();
        event.activity("BOSS_RAID"); event.bossEntity(bossId); event.attemptId(attempt);
        when(level.getEntity(bossId)).thenReturn(boss);
        when(world.hostedAttempts()).thenReturn(Map.of()); when(world.hostedSettlements()).thenReturn(Map.of());
        var operations = new ArrayList<String>();
        try (var tiers = mockStatic(WorldTierData.class); var regions = mockStatic(EventRegions.class);
             var hosted = mockStatic(HostedEncounterApi.class)) {
            tiers.when(() -> WorldTierData.get(server)).thenReturn(world);
            regions.when(() -> EventRegions.level(server, event)).thenReturn(level);
            hosted.when(() -> HostedEncounterApi.attemptId(boss)).thenReturn(attempt);
            hosted.when(() -> HostedEncounterApi.cancel(boss)).thenAnswer(call -> {
                operations.add("canonical-close-acknowledged"); return true;
            });
            RaidEvent.closeBeforeReset(() -> RaidEvent.closeHostedAttempt(server, event),
                    () -> operations.add("replacement-and-event-reset"));
            hosted.verify(() -> HostedEncounterApi.cancel(boss));
            assertEquals(List.of("canonical-close-acknowledged", "replacement-and-event-reset"), operations);
        }
    }

    @Test void failedOrNewerCanonicalAttemptCannotBeResetOrReplaced() {
        var server = mock(MinecraftServer.class); var level = mock(ServerLevel.class);
        var boss = mock(LivingEntity.class); var world = mock(WorldTierData.class);
        var event = new EventState(); var bossId = UUID.randomUUID(); var attempt = UUID.randomUUID();
        event.activity("BOSS_RAID"); event.bossEntity(bossId); event.attemptId(attempt);
        when(level.getEntity(bossId)).thenReturn(boss);
        var reset = mock(Runnable.class);
        try (var tiers = mockStatic(WorldTierData.class); var regions = mockStatic(EventRegions.class);
             var hosted = mockStatic(HostedEncounterApi.class)) {
            tiers.when(() -> WorldTierData.get(server)).thenReturn(world);
            regions.when(() -> EventRegions.level(server, event)).thenReturn(level);
            hosted.when(() -> HostedEncounterApi.attemptId(boss)).thenReturn(attempt);
            hosted.when(() -> HostedEncounterApi.cancel(boss)).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () -> RaidEvent.closeBeforeReset(
                    () -> RaidEvent.closeHostedAttempt(server, event), reset));
            verifyNoInteractions(reset);
            hosted.clearInvocations();
            hosted.when(() -> HostedEncounterApi.attemptId(boss)).thenReturn(UUID.randomUUID());
            assertThrows(IllegalArgumentException.class, () -> RaidEvent.closeBeforeReset(
                    () -> RaidEvent.closeHostedAttempt(server, event), reset));
            hosted.verify(() -> HostedEncounterApi.cancel(boss), never());
            verifyNoInteractions(reset);
        }
    }
}

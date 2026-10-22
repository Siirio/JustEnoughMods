package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class EventAttempts {
    public static boolean start(ServerPlayer host, UUID eventId, EventParticipants participants) {
        var serverForMutation = host.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return startInScope(host, eventId, participants); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> startInScope(host, eventId, participants));
    }

    private static boolean startInScope(ServerPlayer host, UUID eventId, EventParticipants participants) {
        SmpRecords.require(host.getUUID().equals(participants.host()), "owner_required");
        var data = SmpData.get(host.server);
        var event = data.events().current(eventId);
        SmpRecords.require(EventRuntime.available(host.server, event), "unavailable");
        SmpRecords.require(event != null && event.state().equals("ACTIVE")
                && EventRegions.near(event, host.serverLevel(), host.blockPosition()), "travel_to_arena");
        if (event.combatStarted()) return false;
        var prepared = event.copy();
        var encounter = new EncounterContext(prepared);
        encounter.initialize(host, participants);
        SmpRecords.require(encounter.prepare(host.server), "no_safe_arrival");
        encounter.awaitEntry(EventRegions.level(host.server, event));
        prepared.solo( participants.solo());
        prepared.attemptId( UUID.randomUUID());
        data.events().replace(prepared);

        return true;
    }

    private EventAttempts() {}
}

package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class EventAttempts {
    public static boolean start(ServerPlayer host, UUID eventId, EventParticipants participants) {
        SmpRecords.require(host.getUUID().equals(participants.host()), "owner_required");
        var data = SmpData.get(host.server);
        var event = data.find("events", eventId);
        SmpRecords.require(event != null && event.getString("state").equals("ACTIVE")
                && EventRegions.near(event, host.serverLevel(), host.blockPosition()), "travel_to_arena");
        if (event.getBoolean("combatStarted")) return false;
        var prepared = event.copy();
        var encounter = new EncounterContext(prepared);
        encounter.initialize(host, participants);
        SmpRecords.require(encounter.prepare(host.server), "no_safe_arrival");
        encounter.awaitEntry(EventRegions.level(host.server, event));
        prepared.putBoolean("solo", participants.solo());
        prepared.putUUID("attemptId", UUID.randomUUID());
        event.merge(prepared);
        data.changed(event);
        return true;
    }

    private EventAttempts() {}
}

package com.siirio.jemserver.smp.events;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public record EventParticipants(UUID host, List<UUID> accepted, boolean solo) {
    public EventParticipants {
        var participants = new LinkedHashSet<UUID>();
        participants.add(host);
        if (!solo) participants.addAll(accepted);
        accepted = List.copyOf(participants);
    }
}

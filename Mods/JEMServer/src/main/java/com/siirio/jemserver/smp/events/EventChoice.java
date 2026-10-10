package com.siirio.jemserver.smp.events;

import java.util.UUID;

public record EventChoice(UUID id, EventEntryKind type, EventEntryKind mode, boolean solo) {
    public EventChoice {
        boolean valid = type == EventEntryKind.BOSS_STRUCTURE
                ? mode == EventEntryKind.BOSS_FIGHT || mode == EventEntryKind.BOSS_RAID : mode == type;
        if (!valid) throw new IllegalArgumentException("Invalid event entry mode");
    }
}

package com.siirio.jemserver.smp.events;

import java.util.UUID;
import java.util.List;

public record EventPrompt(UUID id,String type,String title,String entityType,boolean raidAvailable,String state,List<String> occupants) {
    public EventPrompt {
        occupants = List.copyOf(occupants);
    }
}

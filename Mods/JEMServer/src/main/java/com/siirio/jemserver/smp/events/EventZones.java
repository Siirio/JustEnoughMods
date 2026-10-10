package com.siirio.jemserver.smp.events;

import java.util.List;

public record EventZones(List<EventBoundary> bounds) {
    public EventZones {
        bounds = List.copyOf(bounds);
    }
}

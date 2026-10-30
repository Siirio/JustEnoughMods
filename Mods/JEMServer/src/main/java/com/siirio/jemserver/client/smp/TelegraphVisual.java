package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.events.EventTelegraph;

record TelegraphVisual(EventTelegraph packet, long expires, EventSurfaceSampler surface) {}

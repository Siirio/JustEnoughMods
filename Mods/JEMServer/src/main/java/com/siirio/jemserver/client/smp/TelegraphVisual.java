package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.events.EventTelegraph;
import java.util.List;

record TelegraphVisual(EventTelegraph packet, long expires, List<EventSurfaceCell> cells) {}

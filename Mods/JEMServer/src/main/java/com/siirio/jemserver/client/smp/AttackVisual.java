package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.events.EventAttackVfx;
import java.util.List;

record AttackVisual(EventAttackVfx packet, long started, long expires, List<EventSurfaceCell> cells) {}

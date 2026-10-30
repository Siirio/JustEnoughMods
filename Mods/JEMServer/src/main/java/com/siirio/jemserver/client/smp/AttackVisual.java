package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.events.EventAttackVfx;

record AttackVisual(EventAttackVfx packet, long started, long expires, EventSurfaceSampler surface) {}

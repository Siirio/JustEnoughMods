package com.siirio.jemserver.smp.events;

import java.util.List;
import java.util.UUID;

/** Cache membership only; row state always comes from the current owning command. */
record ActiveEventSnapshot(int tick, List<UUID> ids) {}

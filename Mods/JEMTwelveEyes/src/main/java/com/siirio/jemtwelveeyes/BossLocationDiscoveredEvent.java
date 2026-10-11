package com.siirio.jemtwelveeyes;

import com.siirio.jemtwelveeyes.api.BossLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class BossLocationDiscoveredEvent extends Event {
    private final MinecraftServer server;
    private final BossLocation location;
    private boolean shared;

    public BossLocationDiscoveredEvent(MinecraftServer server, BossLocation location) {
        this.server = server;
        this.location = location;
    }

    public MinecraftServer server() { return server; }

    public BossLocation location() { return location; }

    public boolean shared() { return shared; }

    public void markShared() { shared = true; }
}

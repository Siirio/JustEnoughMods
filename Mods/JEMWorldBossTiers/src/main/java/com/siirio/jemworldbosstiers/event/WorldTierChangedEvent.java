package com.siirio.jemworldbosstiers.event;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class WorldTierChangedEvent extends Event {
    private final MinecraftServer server;
    private final int previousTier;
    private final int currentTier;

    public WorldTierChangedEvent(MinecraftServer server, int previousTier, int currentTier) {
        this.server = server;
        this.previousTier = previousTier;
        this.currentTier = currentTier;
    }

    public MinecraftServer server() {
        return server;
    }

    public int previousTier() {
        return previousTier;
    }

    public int currentTier() {
        return currentTier;
    }
}

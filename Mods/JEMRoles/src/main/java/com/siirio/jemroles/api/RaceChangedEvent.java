package com.siirio.jemroles.api;

import com.siirio.jemroles.race.RaceId;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class RaceChangedEvent extends Event {
    private final ServerPlayer player;
    private final RaceId race;
    public RaceChangedEvent(ServerPlayer player, RaceId race) { this.player = player; this.race = race; }
    public ServerPlayer player() { return player; }
    public RaceId race() { return race; }
}

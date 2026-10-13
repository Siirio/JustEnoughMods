package com.siirio.jemprofessions.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class ProfessionProgressEvent extends Event {
    private final ServerPlayer player;
    private final ProfessionProgressRequest request;
    private final ProfessionProgressResult result;

    public ProfessionProgressEvent(ServerPlayer player, ProfessionProgressRequest request, ProfessionProgressResult result) {
        this.player = player;
        this.request = request;
        this.result = result;
    }

    public ServerPlayer player() { return player; }
    public ProfessionProgressRequest request() { return request; }
    public ProfessionProgressResult result() { return result; }
}

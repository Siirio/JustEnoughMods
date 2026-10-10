package com.siirio.jemdrill.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class DrillLifecycleEvent extends Event {
    private final ServerPlayer player;
    private final DrillAction action;
    private final int servicedCount;
    private final boolean finalService;
    private final boolean movingContraption;

    public DrillLifecycleEvent(ServerPlayer player, DrillAction action, int servicedCount, boolean finalService, boolean movingContraption) {
        this.player = player;
        this.action = action;
        this.servicedCount = servicedCount;
        this.finalService = finalService;
        this.movingContraption = movingContraption;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public DrillAction getAction() {
        return action;
    }

    public int getServicedCount() {
        return servicedCount;
    }

    public boolean isMovingContraption() {
        return movingContraption;
    }

    public boolean isFinalService() {
        return finalService;
    }
}

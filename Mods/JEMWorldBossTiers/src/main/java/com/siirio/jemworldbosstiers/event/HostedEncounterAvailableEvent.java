package com.siirio.jemworldbosstiers.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public final class HostedEncounterAvailableEvent extends Event {
    private final ServerPlayer actor;
    private final LivingEntity boss;

    public HostedEncounterAvailableEvent(ServerPlayer actor, LivingEntity boss) {
        this.actor = actor;
        this.boss = boss;
    }

    public ServerPlayer actor() { return actor; }
    public LivingEntity boss() { return boss; }
}

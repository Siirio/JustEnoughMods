package com.siirio.jemworldbosstiers.event;

import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public final class HostedEncounterClosedEvent extends Event {
    private final LivingEntity boss;
    private final boolean raid;
    private final boolean success;
    private final Set<UUID> eligibleParticipants;

    public HostedEncounterClosedEvent(LivingEntity boss, boolean raid, boolean success, Set<UUID> eligibleParticipants) {
        this.boss = boss;
        this.raid = raid;
        this.success = success;
        this.eligibleParticipants = Set.copyOf(eligibleParticipants);
    }

    public LivingEntity boss() { return boss; }
    public boolean raid() { return raid; }
    public boolean success() { return success; }
    public Set<UUID> eligibleParticipants() { return eligibleParticipants; }
}

package com.siirio.jemworldbosstiers.event;

import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

public final class HostedEncounterClosedEvent extends Event {
    private final LivingEntity boss;
    private final boolean raid;
    private final boolean success;
    private final Set<UUID> eligibleParticipants;
    private final MinecraftServer server;
    private final UUID bossId;
    private final UUID attemptId;
    private final ResourceLocation dimension;
    private final CompoundTag frozenState;

    public HostedEncounterClosedEvent(LivingEntity boss, boolean raid, boolean success, Set<UUID> eligibleParticipants) {
        this(boss.getServer(), boss.getUUID(), boss.getPersistentData().getCompound("JEMHostedEncounter").getUUID("Attempt"),
                boss.level().dimension().location(), boss.getPersistentData().getCompound("JEMHostedEncounter"), raid, success, eligibleParticipants, boss);
    }

    /** A dead boss need not remain loaded for a durable terminal transition to finish. */
    public HostedEncounterClosedEvent(MinecraftServer server, UUID bossId, UUID attemptId, ResourceLocation dimension,
                                      CompoundTag frozenState, boolean raid, boolean success,
                                      Set<UUID> eligibleParticipants, LivingEntity boss) {
        this.boss = boss;
        this.server = server;
        this.bossId = bossId;
        this.attemptId = attemptId;
        this.dimension = dimension;
        this.frozenState = frozenState.copy();
        this.raid = raid;
        this.success = success;
        this.eligibleParticipants = Set.copyOf(eligibleParticipants);
    }

    public LivingEntity boss() { return boss; }
    public boolean raid() { return raid; }
    public boolean success() { return success; }
    public Set<UUID> eligibleParticipants() { return eligibleParticipants; }
    public MinecraftServer server() { return server; }
    public UUID bossId() { return bossId; }
    public UUID attemptId() { return attemptId; }
    public ResourceLocation dimension() { return dimension; }
    public CompoundTag frozenState() { return frozenState.copy(); }
}

package com.siirio.mobhome.feature.shelter;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Immutable notification of a validated, flushed home assignment by its real owner. */
public final class CompanionHomeEvent extends Event {
    private final ServerPlayer player;
    private final UUID pet;
    private final String dimension;
    private final BlockPos home;
    private final boolean bound;

    public CompanionHomeEvent(ServerPlayer player, UUID pet, String dimension, BlockPos home, boolean bound) {
        this.player = player;
        this.pet = pet;
        this.dimension = dimension;
        this.home = home.immutable();
        this.bound = bound;
    }

    public ServerPlayer player() { return player; }
    public UUID pet() { return pet; }
    public String dimension() { return dimension; }
    public BlockPos home() { return home; }
    public boolean bound() { return bound; }
}

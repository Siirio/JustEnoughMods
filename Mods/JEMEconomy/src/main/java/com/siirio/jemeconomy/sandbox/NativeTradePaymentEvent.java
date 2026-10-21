package com.siirio.jemeconomy.sandbox;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

/** Settlements supplies validated resident/town terms at the exact payment commit. */
public final class NativeTradePaymentEvent extends Event {
    public final ServerPlayer player;
    public final Entity merchant;
    public final UUID operation;
    public final long amount;
    private UUID town;
    private int percent;
    public NativeTradePaymentEvent(ServerPlayer player, Entity merchant, UUID operation, long amount) {
        this.player = player; this.merchant = merchant; this.operation = operation; this.amount = amount;
    }
    public void town(UUID id, int rate) {
        if (id == null || (rate != 0 && rate != 5 && rate != 10 && rate != 15)) throw new IllegalArgumentException("Invalid town tax");
        town = id; percent = rate;
    }
    public UUID town() { return town; }
    public int percent() { return percent; }
}

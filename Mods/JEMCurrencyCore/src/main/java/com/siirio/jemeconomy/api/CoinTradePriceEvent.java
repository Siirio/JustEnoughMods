package com.siirio.jemeconomy.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Roles may adjust a native purchase quote in its actual paid currency. */
public final class CoinTradePriceEvent extends Event {
    private final ServerPlayer player;
    private long copper;
    public CoinTradePriceEvent(ServerPlayer player, long copper) { this.player = player; this.copper = copper; }
    public ServerPlayer player() { return player; }
    public long copper() { return copper; }
    public void setCopper(long copper) {
        if (copper <= 0L || copper > this.copper) throw new IllegalArgumentException("Invalid Coin discount");
        this.copper = copper;
    }
}

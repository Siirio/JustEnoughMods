package com.siirio.jemeconomy.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Roles may adjust a native purchase quote in its actual paid currency. */
public final class CoinTradePriceEvent extends Event {
    private final ServerPlayer player;
    private com.siirio.jemeconomy.policy.RationalPrice price;
    public CoinTradePriceEvent(ServerPlayer player, long copper) { this(player,new com.siirio.jemeconomy.policy.RationalPrice(copper,1)); }
    public CoinTradePriceEvent(ServerPlayer player,com.siirio.jemeconomy.policy.RationalPrice price) { this.player=player;this.price=price; }
    public com.siirio.jemeconomy.policy.RationalPrice price() { return price; }
    public void discountPercent(int percent) {
        if(percent<0||percent>=100)throw new IllegalArgumentException("Invalid discount");
        price=price.multiply(100-percent).divide(100);
    }
    public ServerPlayer player() { return player; }
    public long copper() { return price.ceil(); }
    public void setCopper(long copper) {
        if (copper <= 0L || copper > price.ceil()) throw new IllegalArgumentException("Invalid Coin discount");
        this.price = new com.siirio.jemeconomy.policy.RationalPrice(copper,1);
    }
}

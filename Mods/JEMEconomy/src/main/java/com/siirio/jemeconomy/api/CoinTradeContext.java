package com.siirio.jemeconomy.api;

import net.minecraft.server.level.ServerPlayer;

/** Scoped native callback context: a Coin trade must never refund emeralds. */
public final class CoinTradeContext implements AutoCloseable {
    private static final ThreadLocal<ServerPlayer> ACTOR = new ThreadLocal<>();
    private final ServerPlayer previous;
    private CoinTradeContext(ServerPlayer actor) { previous = ACTOR.get(); ACTOR.set(actor); }
    public static CoinTradeContext enter(ServerPlayer actor) { return new CoinTradeContext(actor); }
    public static boolean active(ServerPlayer actor) { return ACTOR.get() == actor; }
    @Override public void close() { if (previous == null) ACTOR.remove(); else ACTOR.set(previous); }
}

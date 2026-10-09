package com.siirio.jemworldbosstiers.api;

import net.minecraft.server.level.ServerPlayer;

public final class BossRevivalApi {
    private static Handler handler;

    private BossRevivalApi() {
    }

    public static void install(Handler value) {
        handler = value;
    }

    public static Result reviveNearest(ServerPlayer player, int radius) {
        return handler == null ? Result.UNAVAILABLE : handler.reviveNearest(player, radius);
    }

    public enum Result {
        CREATED,
        NO_ARENA,
        ACTIVE,
        UNAVAILABLE
    }

    @FunctionalInterface
    public interface Handler {
        Result reviveNearest(ServerPlayer player, int radius);
    }
}

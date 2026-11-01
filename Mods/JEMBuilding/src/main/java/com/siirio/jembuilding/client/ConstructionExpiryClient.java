package com.siirio.jembuilding.client;

import com.siirio.jembuilding.network.BuildingNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ConstructionExpiryClient {
    private static BlockPos cachedPos;
    private static long remainingAtResponse = -1L;
    private static long responseGameTime;
    private static long lastRequestGameTime = Long.MIN_VALUE;

    public static long remaining(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return -1L;
        long gameTime = client.level.getGameTime();
        if (!pos.equals(cachedPos)) {
            cachedPos = pos.immutable();
            remainingAtResponse = -1L;
            lastRequestGameTime = gameTime;
            BuildingNetwork.requestExpiry(pos);
        } else if (gameTime - lastRequestGameTime >= 20L || gameTime < lastRequestGameTime) {
            lastRequestGameTime = gameTime;
            BuildingNetwork.requestExpiry(pos);
        }
        if (!pos.equals(cachedPos) || remainingAtResponse < 0L) return -1L;
        return Math.max(0L, remainingAtResponse - Math.max(0L, gameTime - responseGameTime));
    }

    public static void receive(BlockPos pos, long remainingTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !pos.equals(cachedPos)) return;
        remainingAtResponse = remainingTicks;
        responseGameTime = client.level.getGameTime();
    }

    private ConstructionExpiryClient() { }
}

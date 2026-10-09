package com.siirio.jemcompat.feature.backtobed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class DailyTeleportData {
    private static final String LAST_USE_DAY = "jemcompat_backtobed_last_use_day";

    private DailyTeleportData() {
    }

    public static boolean canActivate(ServerPlayer player, long currentDay) {
        CompoundTag data = persistedData(player);
        return GroupTeleportPolicy.canActivate(currentDay, data.contains(LAST_USE_DAY, Tag.TAG_LONG), data.getLong(LAST_USE_DAY));
    }

    public static void markActivated(ServerPlayer player, long currentDay) {
        CompoundTag root = player.getPersistentData();
        CompoundTag data = persistedData(player);
        data.putLong(LAST_USE_DAY, currentDay);
        root.put(Player.PERSISTED_NBT_TAG, data);
    }

    private static CompoundTag persistedData(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }
}

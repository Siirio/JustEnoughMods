package com.siirio.jemcompat.feature.transport;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class FireworkBoostPolicy {
    private static final String STATE_KEY = "JemElytraBoostBudget";
    private static final String REMAINING_KEY = "BoostsRemaining";
    private static final String REFILL_AT_KEY = "RefillAtEpochMillis";
    private static final int BATCH_SIZE = 8;
    private static final long REFILL_MILLIS = 2L * 60L * 1000L;

    private FireworkBoostPolicy() {
    }

    public static boolean consume(ServerPlayer player) {
        CompoundTag state = state(player);
        long now = System.currentTimeMillis();
        int savedRemaining = state.contains(REMAINING_KEY) ? state.getInt(REMAINING_KEY) : BATCH_SIZE;
        int remaining = Math.max(0, Math.min(BATCH_SIZE, savedRemaining));
        if (remaining <= 0 && now >= state.getLong(REFILL_AT_KEY)) {
            remaining = BATCH_SIZE;
            state.putInt(REMAINING_KEY, remaining);
            state.remove(REFILL_AT_KEY);
        }
        if (remaining <= 0) {
            player.displayClientMessage(Component.translatable("message.jemcompat.elytra_boost.cooldown",
                    durationLabel(Math.max(0L, state.getLong(REFILL_AT_KEY) - now))), true);
            return false;
        }

        remaining--;
        state.putInt(REMAINING_KEY, remaining);
        if (remaining == 0) {
            state.putLong(REFILL_AT_KEY, now + REFILL_MILLIS);
            player.displayClientMessage(Component.translatable("message.jemcompat.elytra_boost.empty",
                    Component.translatable("duration.jemcompat.minutes", 2)), true);
        } else {
            state.remove(REFILL_AT_KEY);
            player.displayClientMessage(Component.translatable("message.jemcompat.elytra_boost.remaining",
                    remaining, BATCH_SIZE), true);
        }
        return true;
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        CompoundTag originalPersisted = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!originalPersisted.contains(STATE_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return;
        }
        persisted(event.getEntity()).put(STATE_KEY, originalPersisted.getCompound(STATE_KEY).copy());
    }

    private static CompoundTag state(Player player) {
        CompoundTag persisted = persisted(player);
        if (!persisted.contains(STATE_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            persisted.put(STATE_KEY, new CompoundTag());
        }
        return persisted.getCompound(STATE_KEY);
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static Component durationLabel(long remainingMillis) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1000L);
        if (seconds < 60L) {
            return Component.translatable("duration.jemcompat.seconds", seconds);
        }
        return Component.translatable("duration.jemcompat.minutes", (seconds + 59L) / 60L);
    }
}

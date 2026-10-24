package com.siirio.jemidentity.api;

import net.minecraft.network.chat.Component;

/** Shared presentation only; saved and network times retain native units. */
public final class MinecraftDays {
    private MinecraftDays() {}

    public static Component duration(long remainingTicks) {
        if (remainingTicks >= 24_000L) {
            long days = remainingTicks / 24_000L;
            String form = days % 10L == 1L && days % 100L != 11L ? "one"
                    : days % 10L >= 2L && days % 10L <= 4L && (days % 100L < 12L || days % 100L > 14L) ? "few" : "many";
            return Component.translatable("duration.jemidentity.days." + (days == 1L ? "single" : form), days,
                    remainingTicks % 24_000L == 0L ? "" : " +");
        }
        long seconds = Math.max(0L, remainingTicks / 20L + (remainingTicks % 20L > 0L ? 1L : 0L));
        if (seconds < 60L) return Component.translatable("duration.jemidentity.seconds", seconds);
        return seconds % 60L == 0L ? Component.translatable("duration.jemidentity.minutes", seconds / 60L)
                : Component.translatable("duration.jemidentity.minutes_seconds", seconds / 60L, seconds % 60L);
    }

    /** Retained for binary callers; new chat/network consumers pass the localized component. */
    public static String format(long remainingTicks) { return duration(remainingTicks).getString(); }
}

package com.siirio.jemfood.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Presentation units only. Simulation and network durations remain native. */
public final class FoodTime {
    private FoodTime() {}
    public static MutableComponent duration(int ticks) {
        long remaining = Math.max(0, ticks);
        if (remaining >= 24000) {
            long days = remaining / 24000;
            String form = days % 10 == 1 && days % 100 != 11 ? "one"
                    : days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 12 || days % 100 > 14) ? "few" : "many";
            var result = Component.translatable("tooltip.jemfood.time.day." + (days == 1 ? "single" : form), days);
            return remaining % 24000 == 0 ? result : result.append(" +");
        }
        long wholeSeconds = (remaining + 19) / 20;
        long minutes = wholeSeconds / 60;
        long seconds = wholeSeconds % 60;
        if (minutes == 0) return Component.translatable("tooltip.jemfood.time.seconds", seconds);
        if (seconds == 0) return Component.translatable("tooltip.jemfood.time.minutes", minutes);
        return Component.translatable("tooltip.jemfood.time.minutes_seconds", minutes, seconds);
    }
}

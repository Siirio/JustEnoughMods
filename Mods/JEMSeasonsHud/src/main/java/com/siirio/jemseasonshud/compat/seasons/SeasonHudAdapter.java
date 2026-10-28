package com.siirio.jemseasonshud.compat.seasons;

import com.siirio.jemseasonshud.client.SeasonHudState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModConfig;
import sereneseasons.season.SeasonHandlerClient;

import java.util.List;
import java.util.Locale;

/** Loaded only after the client has established that Serene Seasons is installed. */
public final class SeasonHudAdapter {
    private SeasonHudAdapter() {}

    public static SeasonHudState read(ClientLevel level, BlockPos position) {
        if (!ModConfig.seasons.isDimensionWhitelisted(level.dimension())) {
            return new SeasonHudState(false, 0, List.of(Component.translatable("hud.jemcompat.no_seasons")));
        }
        // Avoid reading the helper before the native client season handler has initialized this dimension.
        if (!SeasonHandlerClient.clientSeasonCycleTicks.containsKey(level.dimension())) {
            return new SeasonHudState(false, 0, List.of(Component.translatable("hud.jemcompat.waiting_seasons")));
        }
        var state = SeasonHelper.getSeasonState(level);
        int cycle = state.getCycleDuration();
        int seasonDuration = state.getSeasonDuration();
        if (cycle <= 0 || seasonDuration <= 0) {
            return new SeasonHudState(false, 0, List.of(Component.translatable("hud.jemcompat.no_seasons")));
        }
        int elapsed = Math.floorMod(state.getSeasonCycleTicks(), cycle);
        int percent = (int) (100L * (elapsed % seasonDuration) / seasonDuration);
        var name = Component.translatable("desc.sereneseasons." + state.getSubSeason().name().toLowerCase(Locale.ROOT));
        var lines = new java.util.ArrayList<Component>();
        lines.add(name);
        if (SeasonHelper.usesTropicalSeasons(level.getBiome(position))) {
            lines.add(Component.translatable("desc.sereneseasons." + state.getTropicalSeason().name().toLowerCase(Locale.ROOT)));
        }
        lines.add(Component.translatable("hud.jemcompat.season_progress", percent));
        return new SeasonHudState(true, (double) elapsed / cycle, List.copyOf(lines));
    }
}

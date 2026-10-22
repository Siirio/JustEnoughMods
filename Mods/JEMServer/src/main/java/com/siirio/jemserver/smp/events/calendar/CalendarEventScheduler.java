package com.siirio.jemserver.smp.events.calendar;

import com.siirio.jemserver.smp.SmpConfig;
import java.time.Instant;
import java.time.ZoneId;
import net.minecraft.server.MinecraftServer;

public final class CalendarEventScheduler {
    private static long nextTick;

    private CalendarEventScheduler() {
    }

    public static void tick(MinecraftServer server) {
        if (!com.siirio.jemserver.smp.events.EventRuntime.enabled(server)) return;
        long gameTime = server.overworld().getGameTime();
        if (gameTime < nextTick) return;
        nextTick = gameTime + 1200L;
        Instant now = Instant.now();
        ZoneId zone = ZoneId.of(SmpConfig.CALENDAR_TIMEZONE.get());
        CalendarEventData data = CalendarEventData.get(server);
        if (data.forcedEvent().isPresent()) return;
        for (CalendarEvent event : CalendarEvent.values()) reconcile(server, data, event, now, zone);
    }

    private static void reconcile(MinecraftServer server, CalendarEventData data, CalendarEvent event, Instant now, ZoneId zone) {
        int year = event.seasonYear(now, zone);
        CalendarEvent.Window window = event.window(year, zone);
        CalendarEventState desired = now.isBefore(window.start()) ? CalendarEventState.PLANNED
                : window.active(now) ? CalendarEventState.ACTIVE : CalendarEventState.CLEANED;
        CalendarEventState current = data.state(event).orElse(null);
        if (desired == CalendarEventState.CLEANED && current != CalendarEventState.ENDED
                && current != CalendarEventState.CLEANED) {
            if (data.transition(event, year, CalendarEventState.ENDED, window.start(), window.end())) announce(server, event, CalendarEventState.ENDED);
            return;
        }
        if (data.transition(event, year, desired, window.start(), window.end())) announce(server, event, desired);
    }

    static void announce(MinecraftServer server, CalendarEvent event, CalendarEventState state) {
        if (state != CalendarEventState.ACTIVE && state != CalendarEventState.ENDED) return;
        if (state == CalendarEventState.ENDED) HolidayActivities.cleanup(server);
        for (var player : server.getPlayerList().getPlayers()) {
            if (!HolidayCommands.muted(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        state == CalendarEventState.ACTIVE ? "jem.holiday.started" : "jem.holiday.ended",
                        net.minecraft.network.chat.Component.translatable(event.translationKey())));
            }
        }
    }

    public static void login(net.minecraft.server.level.ServerPlayer player) {
        if (!com.siirio.jemserver.smp.events.EventRuntime.enabled(player.getServer())) return;
        if (HolidayCommands.muted(player)) return;
        CalendarEventData data = CalendarEventData.get(player.getServer());
        data.forcedEvent().or(() -> java.util.Arrays.stream(CalendarEvent.values())
                .filter(event -> data.state(event).orElse(null) == CalendarEventState.ACTIVE).findFirst())
                .ifPresent(event -> player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "jem.holiday.active_notice", net.minecraft.network.chat.Component.translatable(event.translationKey()))));
    }

    public static void reset() { nextTick = 0L; }
}

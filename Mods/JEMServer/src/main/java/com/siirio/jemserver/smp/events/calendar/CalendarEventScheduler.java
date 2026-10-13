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
        long gameTime = server.overworld().getGameTime();
        if (gameTime < nextTick) return;
        nextTick = gameTime + 1200L;
        Instant now = Instant.now();
        ZoneId zone = ZoneId.of(SmpConfig.CALENDAR_TIMEZONE.get());
        CalendarEventData data = CalendarEventData.get(server);
        if (data.forcedEvent().isPresent()) return;
        for (CalendarEvent event : CalendarEvent.values()) reconcile(data, event, now, zone);
    }

    private static void reconcile(CalendarEventData data, CalendarEvent event, Instant now, ZoneId zone) {
        int year = event.seasonYear(now, zone);
        CalendarEvent.Window window = event.window(year, zone);
        CalendarEventState desired = now.isBefore(window.start()) ? CalendarEventState.PLANNED
                : window.active(now) ? CalendarEventState.ACTIVE : CalendarEventState.CLEANED;
        CalendarEventState current = data.state(event).orElse(null);
        if (desired == CalendarEventState.CLEANED && current != CalendarEventState.ENDED
                && current != CalendarEventState.CLEANED) {
            data.transition(event, year, CalendarEventState.ENDED, window.start(), window.end());
            return;
        }
        data.transition(event, year, desired, window.start(), window.end());
    }

    public static void reset() { nextTick = 0L; }
}

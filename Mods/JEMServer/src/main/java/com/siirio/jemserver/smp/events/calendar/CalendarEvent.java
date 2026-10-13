package com.siirio.jemserver.smp.events.calendar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

public enum CalendarEvent {
    HALLOWEEN,
    NEW_YEAR;

    public Window window(int seasonYear, ZoneId zone) {
        LocalDateTime start = this == HALLOWEEN
                ? LocalDateTime.of(seasonYear, Month.OCTOBER, 29, 0, 0)
                : LocalDateTime.of(seasonYear, Month.DECEMBER, 24, 0, 0);
        LocalDateTime end = this == HALLOWEEN
                ? LocalDateTime.of(seasonYear, Month.NOVEMBER, 1, 0, 0)
                : LocalDateTime.of(seasonYear + 1, Month.JANUARY, 7, 0, 0);
        return new Window(start.atZone(zone).toInstant(), end.atZone(zone).toInstant());
    }

    public int seasonYear(Instant now, ZoneId zone) {
        var date = now.atZone(zone).toLocalDate();
        if (this == NEW_YEAR && date.getMonth() == Month.JANUARY && date.getDayOfMonth() < 7) return date.getYear() - 1;
        return date.getYear();
    }

    public record Window(Instant start, Instant end) {
        public boolean active(Instant now) { return !now.isBefore(start) && now.isBefore(end); }
    }
}

package com.siirio.jemserver.smp.events;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public final class WeeklySchedule {
    public static final ZoneId ZONE = ZoneId.of("Asia/Almaty");
    public static final Set<String> TYPES = Set.of("FISHING", "RESOURCE_RUSH", "BLOOD_MOON", "BOSS_RAID", "COOKING_SHOW");
    public record Slot(String id, String type, long planned) {}

    public static List<Slot> slots(Instant now, List<? extends String> config) {
        LocalDate monday =
                now.atZone(ZONE)
                        .toLocalDate()
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var result = new ArrayList<Slot>();
        for (String definition : config) {
            String[] fields = definition.split(":");
            if (fields.length != 4 || !TYPES.contains(fields[0]))
                throw new IllegalArgumentException("Invalid event schedule entry: " + definition);
            var day = DayOfWeek.valueOf(fields[1]);
            var time = LocalTime.of(Integer.parseInt(fields[2]), Integer.parseInt(fields[3]));
            long planned =
                    monday.plusDays(day.getValue() - 1)
                            .atTime(time)
                            .atZone(ZONE)
                            .toInstant()
                            .toEpochMilli();
            result.add(new Slot(monday + ":" + definition, fields[0], planned));
        }
        if (result.stream().map(Slot::id).distinct().count() != result.size())
            throw new IllegalArgumentException("Duplicate weekly event slot");
        result.sort(Comparator.comparingLong(Slot::planned));
        return List.copyOf(result);
    }

    public static long weekStart(Instant now) {
        return now.atZone(ZONE)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay()
                .atZone(ZONE)
                .toInstant()
                .toEpochMilli();
    }

    private WeeklySchedule() {}
}

package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.WeeklySchedule;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeeklyScheduleTest {
    private static final List<String> VALID=List.of("RESOURCE_RUSH:TUESDAY:18:00","RESOURCE_RUSH:SATURDAY:14:00","FISHING:WEDNESDAY:18:00","FISHING:SATURDAY:10:00","FISHING:SUNDAY:10:00");
    @Test void restartRetainsSlotIdentityAndBothMissedRushes(){var thursday=WeeklySchedule.slots(Instant.parse("2026-10-01T08:00:00Z"),VALID);var sunday=WeeklySchedule.slots(Instant.parse("2026-10-04T23:00:00Z"),VALID);assertEquals(thursday,sunday);assertEquals(2,sunday.stream().filter(s->s.type().equals("RESOURCE_RUSH")).count());}
    @Test void followingWeekNeverReplaysOldSlots(){var first=WeeklySchedule.slots(Instant.parse("2026-10-04T23:00:00Z"),VALID);var next=WeeklySchedule.slots(Instant.parse("2026-10-05T00:00:00Z"),VALID);assertTrue(next.stream().noneMatch(slot->first.stream().anyMatch(old->old.id().equals(slot.id()))));}
    @Test void rejectsSchedulesWithoutSundayFishing(){assertThrows(IllegalArgumentException.class,()->WeeklySchedule.slots(Instant.EPOCH,VALID.stream().map(s->s.replace("FISHING:SUNDAY","FISHING:FRIDAY")).toList()));}
}

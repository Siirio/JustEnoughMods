package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpConfig;
import com.siirio.jemserver.smp.SmpData;
import java.time.Instant;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class EventSchedule {
    public static List<String> definitions(MinecraftServer server) {
        var row = SmpData.get(server).settings().schedule();
        return row == null || !row.hasEntries() ? List.copyOf(SmpConfig.SCHEDULE.get()) : row.entries();
    }
    public static void replace(MinecraftServer server, List<String> definitions) {
        WeeklySchedule.slots(Instant.now(), definitions);
        var data = SmpData.get(server);
        data.edit(() -> { data.settings().replaceSchedule(definitions); EventScheduler.scheduleChanged(); });
    }
    public static void reset(MinecraftServer server) {
        var data = SmpData.get(server);
        data.edit(() -> { data.settings().resetSchedule(); EventScheduler.scheduleChanged(); });
    }
    private EventSchedule() {}
}

package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpConfig;
import com.siirio.jemserver.smp.SmpData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EventSchedule {
    private static final String TABLE = "settings";
    private static final String KIND = "event_schedule";
    private static final String ENTRIES = "entries";
    private static final UUID SERVER = new UUID(0, 0);

    public static List<String> definitions(MinecraftServer server) {
        var row = row(SmpData.get(server));
        if (row == null || !row.contains(ENTRIES, Tag.TAG_LIST))
            return List.copyOf(SmpConfig.SCHEDULE.get());
        var entries = row.getList(ENTRIES, Tag.TAG_STRING);
        var result = new ArrayList<String>(entries.size());
        entries.forEach(entry -> result.add(entry.getAsString()));
        return List.copyOf(result);
    }

    public static void replace(MinecraftServer server, List<String> definitions) {
        WeeklySchedule.slots(Instant.now(), definitions);
        var data = SmpData.get(server);
        var row = row(data);
        if (row == null) {
            row = data.create(TABLE, SERVER);
            row.putString("kind", KIND);
        }
        var entries = new ListTag();
        definitions.forEach(definition -> entries.add(StringTag.valueOf(definition)));
        row.put(ENTRIES, entries);
        data.changed(row);
        EventScheduler.scheduleChanged();
    }

    public static void reset(MinecraftServer server) {
        var data = SmpData.get(server);
        var row = row(data);
        if (row != null) {
            row.remove(ENTRIES);
            data.changed(row);
        }
        EventScheduler.scheduleChanged();
    }

    private static CompoundTag row(SmpData data) {
        return data.all(TABLE).stream()
                .filter(candidate -> candidate.getString("kind").equals(KIND))
                .findFirst()
                .orElse(null);
    }

    private EventSchedule() {}
}

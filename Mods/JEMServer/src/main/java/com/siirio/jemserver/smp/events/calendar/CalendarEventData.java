package com.siirio.jemserver.smp.events.calendar;

import java.time.Instant;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class CalendarEventData extends SavedData {
    private static final String NAME = "jem_calendar_events";
    private final CompoundTag events = new CompoundTag();
    private final CompoundTag transitions = new CompoundTag();
    private String forcedEvent = "";

    public static CalendarEventData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CalendarEventData::load, CalendarEventData::new, NAME);
    }

    public Optional<CalendarEventState> state(CalendarEvent event) {
        String value = events.getCompound(event.name()).getString("State");
        try { return value.isBlank() ? Optional.empty() : Optional.of(CalendarEventState.valueOf(value)); }
        catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }

    public boolean transition(CalendarEvent event, int year, CalendarEventState state, Instant start, Instant end) {
        String id = event.name() + ":" + year + ":" + state.name();
        if (transitions.getBoolean(id)) return false;
        CompoundTag row = new CompoundTag();
        row.putInt("Year", year);
        row.putString("State", state.name());
        row.putLong("StartUtc", start.toEpochMilli());
        row.putLong("EndUtc", end.toEpochMilli());
        row.putString("TransitionId", id);
        events.put(event.name(), row);
        transitions.putBoolean(id, true);
        setDirty();
        return true;
    }

    public Optional<CalendarEvent> forcedEvent() {
        try { return forcedEvent.isBlank() ? Optional.empty() : Optional.of(CalendarEvent.valueOf(forcedEvent)); }
        catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }

    public void force(CalendarEvent event) { forcedEvent = event.name(); setDirty(); }
    public void clearForce() { forcedEvent = ""; setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("Events", events.copy());
        tag.put("Transitions", transitions.copy());
        tag.putString("ForcedEvent", forcedEvent);
        return tag;
    }

    private static CalendarEventData load(CompoundTag tag) {
        CalendarEventData data = new CalendarEventData();
        data.events.merge(tag.getCompound("Events"));
        data.transitions.merge(tag.getCompound("Transitions"));
        data.forcedEvent = tag.getString("ForcedEvent");
        return data;
    }
}

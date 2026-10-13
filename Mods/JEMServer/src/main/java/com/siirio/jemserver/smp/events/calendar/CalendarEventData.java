package com.siirio.jemserver.smp.events.calendar;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class CalendarEventData extends SavedData {
    private static final String NAME = "jem_calendar_events";
    private final CompoundTag events = new CompoundTag();
    private final CompoundTag transitions = new CompoundTag();
    private final CompoundTag activities = new CompoundTag();
    private final CompoundTag gifts = new CompoundTag();
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

    public Optional<CalendarEvent> activeEvent() {
        Optional<CalendarEvent> forced = forcedEvent();
        return forced.isPresent() ? forced : java.util.Arrays.stream(CalendarEvent.values())
                .filter(event -> state(event).orElse(null) == CalendarEventState.ACTIVE).findFirst();
    }

    public boolean joined(UUID account, CalendarEvent event, String path) {
        return activities.getCompound(account.toString()).getBoolean(seasonKey(event) + ":joined:" + path);
    }

    public void join(UUID account, CalendarEvent event, String path) {
        CompoundTag row = activities.getCompound(account.toString());
        row.putBoolean(seasonKey(event) + ":joined:" + path, true);
        activities.put(account.toString(), row);
        setDirty();
    }

    public int progress(UUID account, CalendarEvent event, String objective, int amount, int cap) {
        CompoundTag row = activities.getCompound(account.toString());
        String key = seasonKey(event) + ":progress:" + objective;
        int next = Math.min(cap, Math.max(0, row.getInt(key)) + Math.max(0, amount));
        if (next != row.getInt(key)) {
            row.putInt(key, next);
            activities.put(account.toString(), row);
            setDirty();
        }
        return next;
    }

    public boolean rewardOnce(UUID account, CalendarEvent event, String objective) {
        CompoundTag row = activities.getCompound(account.toString());
        String key = seasonKey(event) + ":reward:" + objective;
        if (row.getBoolean(key)) return false;
        row.putBoolean(key, true);
        activities.put(account.toString(), row);
        setDirty();
        return true;
    }

    public UUID escrowGift(UUID sender, String senderName, UUID target, String targetName, net.minecraft.world.item.ItemStack stack) {
        UUID id = UUID.randomUUID();
        CompoundTag gift = new CompoundTag();
        gift.putUUID("Id", id); gift.putUUID("Sender", sender); gift.putUUID("Target", target);
        gift.putString("SenderName", senderName); gift.putString("TargetName", targetName);
        gift.put("Item", stack.save(new CompoundTag())); gift.putLong("Created", System.currentTimeMillis());
        gifts.put(id.toString(), gift); setDirty(); return id;
    }

    public Optional<CompoundTag> gift(UUID id) {
        return id == null || !gifts.contains(id.toString(), net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? Optional.empty() : Optional.of(gifts.getCompound(id.toString()).copy());
    }

    public void removeGift(UUID id) { gifts.remove(id.toString()); setDirty(); }

    private String seasonKey(CalendarEvent event) {
        return event.name() + ":" + events.getCompound(event.name()).getInt("Year");
    }

    public void force(CalendarEvent event) { forcedEvent = event.name(); setDirty(); }
    public void clearForce() { forcedEvent = ""; setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("Events", events.copy());
        tag.put("Transitions", transitions.copy());
        tag.put("Activities", activities.copy());
        tag.put("Gifts", gifts.copy());
        tag.putString("ForcedEvent", forcedEvent);
        return tag;
    }

    private static CalendarEventData load(CompoundTag tag) {
        CalendarEventData data = new CalendarEventData();
        data.events.merge(tag.getCompound("Events"));
        data.transitions.merge(tag.getCompound("Transitions"));
        data.activities.merge(tag.getCompound("Activities"));
        data.gifts.merge(tag.getCompound("Gifts"));
        data.forcedEvent = tag.getString("ForcedEvent");
        return data;
    }
}

package com.siirio.jemroles.race;

import net.minecraft.nbt.CompoundTag;

public final class RaceRecord {
    public static final int SCHEMA = 2;
    private RaceId race;
    private final long selectedAt;
    private long activeTicks;
    private boolean freeSwitchUsed;
    private long lastRespecAt;
    private long professionBonusDay = Long.MIN_VALUE;
    private int professionBonusToday;

    public RaceRecord(RaceId race, long selectedAt) {
        this(race, selectedAt, 0L, false, 0L);
    }

    private RaceRecord(RaceId race, long selectedAt, long activeTicks, boolean freeSwitchUsed, long lastRespecAt) {
        this.race = race;
        this.selectedAt = selectedAt;
        this.activeTicks = Math.max(0L, activeTicks);
        this.freeSwitchUsed = freeSwitchUsed;
        this.lastRespecAt = Math.max(0L, lastRespecAt);
    }

    public RaceId race() { return race; }
    public long selectedAt() { return selectedAt; }
    public long activeTicks() { return activeTicks; }
    public boolean freeSwitchUsed() { return freeSwitchUsed; }
    public long lastRespecAt() { return lastRespecAt; }
    public boolean freeSwitchAvailable() { return !freeSwitchUsed && activeTicks < 36_000L; }
    public void addActiveTicks(long ticks) { activeTicks = Math.addExact(activeTicks, Math.max(0L, ticks)); }
    public void change(RaceId next, boolean consumeFreeSwitch) {
        race = next;
        if (consumeFreeSwitch) freeSwitchUsed = true;
        lastRespecAt = System.currentTimeMillis();
    }

    public int reserveProfessionBonus(long utcDay, int requested) {
        if (professionBonusDay != utcDay) {
            professionBonusDay = utcDay;
            professionBonusToday = 0;
        }
        int granted = Math.max(0, Math.min(requested, 250 - professionBonusToday));
        professionBonusToday += granted;
        return granted;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Schema", SCHEMA);
        tag.putString("Race", race.name());
        tag.putLong("SelectedAt", selectedAt);
        tag.putLong("ActiveTicks", activeTicks);
        tag.putBoolean("FreeSwitchUsed", freeSwitchUsed);
        tag.putLong("LastRespecAt", lastRespecAt);
        tag.putLong("ProfessionBonusDay", professionBonusDay);
        tag.putInt("ProfessionBonusToday", professionBonusToday);
        return tag;
    }

    static RaceRecord load(CompoundTag tag) {
        RaceId id = RaceId.parse(tag.getString("Race")).orElseThrow();
        RaceRecord record = new RaceRecord(id, tag.getLong("SelectedAt"), tag.getLong("ActiveTicks"), tag.getBoolean("FreeSwitchUsed"), tag.getLong("LastRespecAt"));
        if (tag.contains("ProfessionBonusDay")) record.professionBonusDay = tag.getLong("ProfessionBonusDay");
        record.professionBonusToday = Math.max(0, Math.min(250, tag.getInt("ProfessionBonusToday")));
        return record;
    }
}

package com.siirio.jemroles.race;

import net.minecraft.nbt.CompoundTag;

public final class RaceRecord {
    public static final int SCHEMA = 5;
    private long reselectReadyAt;
    private String testChoiceEpoch = "";
    private boolean choiceRequired;
    private RaceId race;
    private final long selectedAt;
    private long activeTicks;
    private boolean freeSwitchUsed;
    private long lastRespecAt;
    private int spellTier = 1;
    private int spellProgress;
    private final CompoundTag cooldowns = new CompoundTag();
    private final CompoundTag mechanics = new CompoundTag();
    public CompoundTag mechanics() { return mechanics; }
    private int resonance;
    private int waterExposure;
    private int lavaExposure;
    private boolean livingPath;

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
    public long remainingReselectTicks(long gameTime) { return Math.max(0L, reselectReadyAt - gameTime); }
    public void beginReselectWait(long gameTime) { reselectReadyAt = Math.addExact(gameTime, 24_000L); }
    public boolean choiceRequired() { return choiceRequired; }
    public boolean prepareTestChoice(String epoch) {
        if (epoch.equals(testChoiceEpoch)) return false;
        testChoiceEpoch = epoch; choiceRequired = true; return true;
    }
    public void confirmChoice() { choiceRequired = false; }
    public void addActiveTicks(long ticks) { activeTicks = Math.addExact(activeTicks, Math.max(0L, ticks)); }
    public void change(RaceId next, boolean consumeFreeSwitch) {
        race = next;
        spellTier = 1;
        spellProgress = 0;
        new java.util.ArrayList<>(cooldowns.getAllKeys()).forEach(cooldowns::remove);
        resonance = 0;
        waterExposure = 0;
        lavaExposure = 0;
        livingPath = false;
        if (consumeFreeSwitch) freeSwitchUsed = true;
        lastRespecAt = System.currentTimeMillis();
    }

    public int spellTier() { return spellTier; }
    public int spellProgress() { return spellProgress; }
    public int resonance() { return resonance; }
    public int waterExposure() { return waterExposure; }
    public int lavaExposure() { return lavaExposure; }
    public boolean livingPath() { return livingPath; }
    public void livingPath(boolean enabled) { livingPath = enabled; }
    public void waterExposure(int ticks) { waterExposure = Math.max(0, ticks); }
    public void lavaExposure(int ticks) { lavaExposure = Math.max(0, ticks); }
    public void resonance(int value) { resonance = Math.max(0, Math.min(100, value)); }
    public void recordSignatureUse() {
        spellProgress = Math.min(30, spellProgress + 1);
        spellTier = spellProgress >= 30 ? 3 : spellProgress >= 10 ? 2 : 1;
    }
    public long cooldown(String ability) { return cooldowns.getLong(ability); }
    public boolean ready(String ability, long gameTime) { return cooldown(ability) <= gameTime; }
    public void cooldown(String ability, long readyAt) { cooldowns.putLong(ability, Math.max(0L, readyAt)); }
    public long remaining(String ability, long gameTime) { return Math.max(0L, cooldown(ability) - gameTime); }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Schema", SCHEMA);
        tag.putLong("ReselectReadyAt", reselectReadyAt);
        tag.putString("TestChoiceEpoch", testChoiceEpoch);
        tag.putBoolean("ChoiceRequired", choiceRequired);
        tag.put("Mechanics", mechanics.copy());
        tag.putString("Race", race.name());
        tag.putLong("SelectedAt", selectedAt);
        tag.putLong("ActiveTicks", activeTicks);
        tag.putBoolean("FreeSwitchUsed", freeSwitchUsed);
        tag.putLong("LastRespecAt", lastRespecAt);
        tag.putInt("SpellTier", spellTier);
        tag.putInt("SpellProgress", spellProgress);
        tag.put("Cooldowns", cooldowns.copy());
        tag.putInt("Resonance", resonance);
        tag.putInt("WaterExposure", waterExposure);
        tag.putInt("LavaExposure", lavaExposure);
        tag.putBoolean("LivingPath", livingPath);
        return tag;
    }

    static RaceRecord load(CompoundTag tag) {
        RaceId id = RaceId.parse(tag.getString("Race")).orElseThrow();
        RaceRecord record = new RaceRecord(id, tag.getLong("SelectedAt"), tag.getLong("ActiveTicks"), tag.getBoolean("FreeSwitchUsed"), tag.getLong("LastRespecAt"));
        // Legacy records receive one ready change without resetting their race or progress.
        record.reselectReadyAt = Math.max(0L, tag.getLong("ReselectReadyAt"));
        record.testChoiceEpoch = tag.getString("TestChoiceEpoch");
        record.choiceRequired = tag.getBoolean("ChoiceRequired");
        if (tag.contains("Mechanics")) record.mechanics.merge(tag.getCompound("Mechanics"));
        record.spellProgress = Math.max(0, Math.min(30, tag.getInt("SpellProgress")));
        record.spellTier = Math.max(1, Math.min(3, tag.contains("SpellTier") ? tag.getInt("SpellTier") :
                (record.spellProgress >= 30 ? 3 : record.spellProgress >= 10 ? 2 : 1)));
        if (tag.contains("Cooldowns")) {
            CompoundTag saved = tag.getCompound("Cooldowns");
            saved.getAllKeys().forEach(key -> record.cooldowns.putLong(key, saved.getLong(key)));
        }
        record.resonance = Math.max(0, Math.min(100, tag.getInt("Resonance")));
        record.waterExposure = Math.max(0, tag.getInt("WaterExposure"));
        record.lavaExposure = Math.max(0, tag.getInt("LavaExposure"));
        record.livingPath = tag.getBoolean("LivingPath");
        return record;
    }
}

package com.siirio.jemroles.race;

import net.minecraft.nbt.CompoundTag;

public final class RaceRecord {
    public static final int SCHEMA = 6;
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
    private int resonance;
    private int waterExposure;
    private int lavaExposure;
    private boolean livingPath;

    /** Compatibility snapshot; callers cannot mutate saved mechanics. */
    public CompoundTag mechanics() { return mechanics.copy(); }

    public int uses(int raceIndex) {
        int value = mechanics.getBoolean("Migrated") ? mechanics.getInt("Uses" + raceIndex)
                : raceIndex == race.ordinal() ? spellProgress : 0;
        return Math.max(0, Math.min(com.siirio.jemvisualsandbox.workbench.RacePowerStages.requiredUses(3), value));
    }

    public int bloomUpgrade() { return mechanics.getInt("BloomUpgrade"); }
    public void bloomUpgrade(int next) { mechanics.putInt("BloomUpgrade", next); }
    public long mechanicsCooldown(String ability) { return mechanics.getLong("Cooldown" + race.ordinal() + "_" + ability); }
    public void mechanicsCooldown(String ability, long readyAt) { mechanics.putLong("Cooldown" + race.ordinal() + "_" + ability, readyAt); }
    public void awardUtilityUse() {
        mechanics.putInt("Uses" + race.ordinal(), Math.min(com.siirio.jemvisualsandbox.workbench.RacePowerStages.requiredUses(3), uses(race.ordinal()) + 1));
    }
    public int developmentTier(int raceIndex, int normal) {
        String key = "DevelopmentTier" + raceIndex;
        return mechanics.contains(key) ? mechanics.getInt(key) : normal;
    }
    public int developmentBloomUpgrade(int normal) {
        return mechanics.contains("DevelopmentBloomUpgrade") ? mechanics.getInt("DevelopmentBloomUpgrade") : normal;
    }
    public void setDevelopmentTier(int raceIndex, int value) { mechanics.putInt("DevelopmentTier" + raceIndex, value); }
    public void setDevelopmentBloomUpgrade(int value) { mechanics.putInt("DevelopmentBloomUpgrade", value); }

    boolean mechanicsMigrated() { return mechanics.getBoolean("Migrated"); }

    boolean initializeMechanics(long gameTime) {
        boolean changed = false;
        if (!mechanics.getBoolean("Migrated")) {
            mechanics.putInt("Uses" + race.ordinal(), spellProgress);
            for (String key : new String[]{"signature", "guardian", "gravity", "void_projectile", "lava_drink", "second_blossom"})
                mechanics.putLong("Cooldown" + race.ordinal() + "_" + key, cooldown(key));
            mechanics.putInt("BloomUpgrade", race.ordinal() == 2
                    ? com.siirio.jemvisualsandbox.workbench.RacePowerStages.stageForUses(mechanics.getInt("Uses2")) : 0);
            mechanics.putBoolean("Migrated", true);
            changed = true;
        }
        if (!mechanics.getBoolean("GuardianPenaltyV3")) {
            long guardianReadyAt = mechanics.getLong("Cooldown0_guardian");
            long remaining = Math.max(0L, guardianReadyAt - gameTime);
            if (remaining > RaceRules.GUARDIAN_COOLDOWN.get()) {
                mechanics.putLong("Cooldown0_guardian_penalty",
                        gameTime + Math.min(remaining, RaceRules.GUARDIAN_PENALTY_TICKS));
                mechanics.putLong("Cooldown0_guardian",
                        gameTime + Math.min(remaining, RaceRules.GUARDIAN_COOLDOWN.get()));
            }
            mechanics.putBoolean("GuardianPenaltyV3", true);
            changed = true;
        }
        if (!mechanics.getBoolean("GuardianCooldownV2")) {
            long maximum = gameTime + RaceRules.GUARDIAN_COOLDOWN.get();
            if (mechanics.getLong("Cooldown0_guardian") > maximum) mechanics.putLong("Cooldown0_guardian", maximum);
            mechanics.putBoolean("GuardianCooldownV2", true);
            changed = true;
        }
        return changed;
    }

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

    private RaceRecord(RaceRecord source) {
        race = source.race;
        selectedAt = source.selectedAt;
        reselectReadyAt = source.reselectReadyAt;
        testChoiceEpoch = source.testChoiceEpoch;
        choiceRequired = source.choiceRequired;
        activeTicks = source.activeTicks;
        freeSwitchUsed = source.freeSwitchUsed;
        lastRespecAt = source.lastRespecAt;
        spellTier = source.spellTier;
        spellProgress = source.spellProgress;
        cooldowns.merge(source.cooldowns.copy());
        mechanics.merge(source.mechanics.copy());
        resonance = source.resonance;
        waterExposure = source.waterExposure;
        lavaExposure = source.lavaExposure;
        livingPath = source.livingPath;
    }

    RaceRecord copy() { return new RaceRecord(this); }

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

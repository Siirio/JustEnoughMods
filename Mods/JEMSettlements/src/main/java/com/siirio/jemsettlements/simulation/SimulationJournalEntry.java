package com.siirio.jemsettlements.simulation;

import net.minecraft.nbt.CompoundTag;

public record SimulationJournalEntry(long startGameTime, long endGameTime, int simulatedDays, int skippedDays, String result) {
    public CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putLong("Start", startGameTime); tag.putLong("End", endGameTime); tag.putInt("SimulatedDays", simulatedDays); tag.putInt("SkippedDays", skippedDays); tag.putString("Result", result); return tag; }
    public static SimulationJournalEntry load(CompoundTag tag) { return new SimulationJournalEntry(tag.getLong("Start"), tag.getLong("End"), tag.getInt("SimulatedDays"), tag.getInt("SkippedDays"), tag.getString("Result")); }
}

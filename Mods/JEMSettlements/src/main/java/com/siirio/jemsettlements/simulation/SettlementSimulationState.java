package com.siirio.jemsettlements.simulation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class SettlementSimulationState {
    private long lastObservedWorldGameTime;
    private long lastSimulatedThroughGameTime;
    private long inactiveSinceGameTime;
    private long lastActivationGameTime;
    private boolean active = true;
    private final List<SimulationJournalEntry> journal = new ArrayList<>();
    public long lastObservedWorldGameTime() { return lastObservedWorldGameTime; }
    public long lastSimulatedThroughGameTime() { return lastSimulatedThroughGameTime; }
    public long inactiveSinceGameTime() { return inactiveSinceGameTime; }
    public long lastActivationGameTime() { return lastActivationGameTime; }
    public boolean active() { return active; }
    public List<SimulationJournalEntry> journal() { return List.copyOf(journal); }
    public void observeActive(long gameTime) { active = true; lastObservedWorldGameTime = Math.max(lastObservedWorldGameTime, gameTime); lastSimulatedThroughGameTime = Math.max(lastSimulatedThroughGameTime, gameTime); }
    public void deactivate(long gameTime) { if (active) { active = false; inactiveSinceGameTime = gameTime; lastObservedWorldGameTime = Math.max(lastObservedWorldGameTime, gameTime); } }
    public boolean canActivate(long gameTime) { return !active && gameTime - lastActivationGameTime >= 200L; }
    public void activate(long gameTime, SimulationJournalEntry entry) { active = true; lastActivationGameTime = gameTime; lastObservedWorldGameTime = gameTime; lastSimulatedThroughGameTime = gameTime; journal.add(entry); while (journal.size() > 20) journal.remove(0); }
    public CompoundTag save() { CompoundTag tag = new CompoundTag(); tag.putLong("Observed", lastObservedWorldGameTime); tag.putLong("SimulatedThrough", lastSimulatedThroughGameTime); tag.putLong("InactiveSince", inactiveSinceGameTime); tag.putLong("LastActivation", lastActivationGameTime); tag.putBoolean("Active", active); ListTag rows = new ListTag(); journal.forEach(value -> rows.add(value.save())); tag.put("Journal", rows); return tag; }
    public static SettlementSimulationState load(CompoundTag tag) { SettlementSimulationState state = new SettlementSimulationState(); state.lastObservedWorldGameTime = tag.getLong("Observed"); state.lastSimulatedThroughGameTime = tag.getLong("SimulatedThrough"); state.inactiveSinceGameTime = tag.getLong("InactiveSince"); state.lastActivationGameTime = tag.getLong("LastActivation"); state.active = !tag.contains("Active") || tag.getBoolean("Active"); ListTag rows = tag.getList("Journal", Tag.TAG_COMPOUND); for (int i = 0; i < rows.size(); i++) state.journal.add(SimulationJournalEntry.load(rows.getCompound(i))); return state; }
}

package dev.taste.engine.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Keeps temporal state isolated per dimension and nested view. */
public final class ViewHistoryRegistry {
    private final Map<WorldViewScope.ViewKey, TemporalHistory> histories = new LinkedHashMap<>();
    public TemporalHistory history(WorldViewScope.ViewKey key) { return histories.computeIfAbsent(key, ignored -> new TemporalHistory()); }
    public void invalidateAll(TemporalHistory.Reason reason) { histories.values().forEach(history -> history.invalidate(reason)); }
    public void retainOnly(WorldViewScope.ViewKey key) { histories.keySet().removeIf(candidate -> !candidate.equals(key)); }
    public void clear() { histories.clear(); }
    public int size() { return histories.size(); }
}

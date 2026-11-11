package dev.taste.engine.render;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AllocationLedger {
    public record Allocation(String owner, long bytes, long generation, boolean staged) { }
    private final Map<String, Allocation> allocations = new LinkedHashMap<>();
    public synchronized void record(String id, Allocation allocation) { allocations.put(id, allocation); }
    public synchronized void remove(String id) { allocations.remove(id); }
    public synchronized long total(boolean includeStaged) { return allocations.values().stream().filter(value -> includeStaged || !value.staged()).mapToLong(Allocation::bytes).sum(); }
    public synchronized Map<String, Allocation> snapshot() { return Map.copyOf(allocations); }
    public synchronized void clear() { allocations.clear(); }
}

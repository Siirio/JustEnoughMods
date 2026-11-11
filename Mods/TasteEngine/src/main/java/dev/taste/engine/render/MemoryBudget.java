package dev.taste.engine.render;

import java.util.LinkedHashMap;
import java.util.Map;

/** Accounts active and staged allocations so configuration transactions cannot overcommit VRAM. */
public final class MemoryBudget {
    private final long limitBytes;
    private final Map<String, Long> active = new LinkedHashMap<>();
    private final Map<String, Long> staged = new LinkedHashMap<>();

    public MemoryBudget(long limitBytes) {
        if (limitBytes < 1) throw new IllegalArgumentException("Positive memory budget required");
        this.limitBytes = limitBytes;
    }

    public synchronized boolean canStage(String key, long bytes) {
        if (bytes < 0) throw new IllegalArgumentException("Negative allocation");
        Map<String, Long> candidate = staged.isEmpty() ? active : staged;
        long withoutPrior = candidate.values().stream().mapToLong(Long::longValue).sum()
                - candidate.getOrDefault(key, 0L);
        return withoutPrior + bytes <= limitBytes;
    }

    public synchronized void stage(String key, long bytes) {
        if (!canStage(key, bytes)) throw new IllegalStateException("Memory budget exceeded by " + key);
        if (staged.isEmpty()) staged.putAll(active);
        staged.put(key, bytes);
    }

    public synchronized void commit() {
        if (staged.isEmpty()) return;
        active.clear();
        active.putAll(staged);
        staged.clear();
    }

    public synchronized void rollback() { staged.clear(); }
    public synchronized void release(String key) { active.remove(key); staged.remove(key); }
    public synchronized void clear() { active.clear(); staged.clear(); }
    public synchronized long limitBytes() { return limitBytes; }
    public synchronized long activeBytes() { return active.values().stream().mapToLong(Long::longValue).sum(); }
    public synchronized long stagedBytes() { return staged.values().stream().mapToLong(Long::longValue).sum(); }
    public synchronized Map<String, Long> activeAllocations() { return Map.copyOf(active); }
}

package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventCookingProgress {
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private Boolean rewarded;
    boolean rewardedChanged;
    private Map<String, Integer> quantities;
    boolean quantitiesChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventCookingProgress copy() {
        var copy = new EventCookingProgress();
        copy.source = source.copy();
        copy.rewarded = rewarded;
        copy.rewardedChanged = rewardedChanged;
        copy.quantities = quantities;
        copy.quantitiesChanged = quantitiesChanged;
        return copy;
    }
    public EventCookingProgress snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
    }

    public boolean rewarded() { return rewarded == null ? false : rewarded; }
    public void rewarded(boolean value) { requireMutable(); rewardedChanged = true; rewarded = value; }
    public boolean hasRewarded() { return rewarded != null; }
    public void clearRewarded() { requireMutable(); rewardedChanged = true; rewarded = null; }
    public Map<String, Integer> quantities() { return quantities == null ? Map.of() : quantities; }
    public void quantities(Map<String, Integer> value) { requireMutable(); quantitiesChanged = true; quantities = Collections.unmodifiableMap(new LinkedHashMap<>(value)); }
    public boolean hasQuantities() { return quantities != null; }
    public void clearQuantities() { requireMutable(); quantitiesChanged = true; quantities = null; }
}

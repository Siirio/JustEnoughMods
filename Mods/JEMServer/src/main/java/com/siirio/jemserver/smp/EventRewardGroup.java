package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventRewardGroup {
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private Integer wave;
    boolean waveChanged;
    private String titleKey;
    boolean titleKeyChanged;
    private NativeNbtPayload items;
    boolean itemsChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventRewardGroup copy() {
        var copy = new EventRewardGroup();
        copy.source = source.copy();
        copy.wave = wave;
        copy.waveChanged = waveChanged;
        copy.titleKey = titleKey;
        copy.titleKeyChanged = titleKeyChanged;
        copy.items = items;
        copy.itemsChanged = itemsChanged;
        return copy;
    }
    public EventRewardGroup snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
    }

    public int wave() { return wave == null ? 0 : wave; }
    public void wave(int value) { requireMutable(); waveChanged = true; wave = value; }
    public boolean hasWave() { return wave != null; }
    public void clearWave() { requireMutable(); waveChanged = true; wave = null; }
    public String titleKey() { return titleKey == null ? "" : titleKey; }
    public void titleKey(String value) { requireMutable(); titleKeyChanged = true; titleKey = value; }
    public boolean hasTitleKey() { return titleKey != null; }
    public void clearTitleKey() { requireMutable(); titleKeyChanged = true; titleKey = null; }
    public NativeNbtPayload items() { return items == null ? null : items; }
    public void items(NativeNbtPayload value) { requireMutable(); itemsChanged = true; items = value; }
    public boolean hasItems() { return items != null; }
    public void clearItems() { requireMutable(); itemsChanged = true; items = null; }
}

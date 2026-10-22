package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventWeather {
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private Integer clear;
    boolean clearChanged;
    private Integer rain;
    boolean rainChanged;
    private Integer thunder;
    boolean thunderChanged;
    private Boolean raining;
    boolean rainingChanged;
    private Boolean thundering;
    boolean thunderingChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventWeather copy() {
        var copy = new EventWeather();
        copy.source = source.copy();
        copy.clear = clear;
        copy.clearChanged = clearChanged;
        copy.rain = rain;
        copy.rainChanged = rainChanged;
        copy.thunder = thunder;
        copy.thunderChanged = thunderChanged;
        copy.raining = raining;
        copy.rainingChanged = rainingChanged;
        copy.thundering = thundering;
        copy.thunderingChanged = thunderingChanged;
        return copy;
    }
    public EventWeather snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
    }

    public int clear() { return clear == null ? 0 : clear; }
    public void clear(int value) { requireMutable(); clearChanged = true; clear = value; }
    public boolean hasClear() { return clear != null; }
    public void clearClear() { requireMutable(); clearChanged = true; clear = null; }
    public int rain() { return rain == null ? 0 : rain; }
    public void rain(int value) { requireMutable(); rainChanged = true; rain = value; }
    public boolean hasRain() { return rain != null; }
    public void clearRain() { requireMutable(); rainChanged = true; rain = null; }
    public int thunder() { return thunder == null ? 0 : thunder; }
    public void thunder(int value) { requireMutable(); thunderChanged = true; thunder = value; }
    public boolean hasThunder() { return thunder != null; }
    public void clearThunder() { requireMutable(); thunderChanged = true; thunder = null; }
    public boolean raining() { return raining == null ? false : raining; }
    public void raining(boolean value) { requireMutable(); rainingChanged = true; raining = value; }
    public boolean hasRaining() { return raining != null; }
    public void clearRaining() { requireMutable(); rainingChanged = true; raining = null; }
    public boolean thundering() { return thundering == null ? false : thundering; }
    public void thundering(boolean value) { requireMutable(); thunderingChanged = true; thundering = value; }
    public boolean hasThundering() { return thundering != null; }
    public void clearThundering() { requireMutable(); thunderingChanged = true; thundering = null; }
}

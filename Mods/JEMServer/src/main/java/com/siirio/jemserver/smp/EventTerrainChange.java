package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventTerrainChange {
    NativeNbtPayload legacyOriginal;
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private NativeNbtPayload original;
    boolean originalChanged;
    private NativeNbtPayload temporary;
    boolean temporaryChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventTerrainChange copy() {
        var copy = new EventTerrainChange();
        copy.source = source.copy();
        copy.legacyOriginal = legacyOriginal;
        copy.original = original;
        copy.originalChanged = originalChanged;
        copy.temporary = temporary;
        copy.temporaryChanged = temporaryChanged;
        return copy;
    }
    public EventTerrainChange snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
    }

    public NativeNbtPayload original() { return original == null ? null : original; }
    public void original(NativeNbtPayload value) { requireMutable(); originalChanged = true; original = value; }
    public boolean hasOriginal() { return original != null; }
    public void clearOriginal() { requireMutable(); originalChanged = true; original = null; }
    public NativeNbtPayload temporary() { return temporary == null ? null : temporary; }
    public void temporary(NativeNbtPayload value) { requireMutable(); temporaryChanged = true; temporary = value; }
    public boolean hasTemporary() { return temporary != null; }
    public void clearTemporary() { requireMutable(); temporaryChanged = true; temporary = null; }
    public boolean hasLegacyOriginal() { return legacyOriginal != null; }
    public NativeNbtPayload legacyOriginal() { return legacyOriginal; }
}

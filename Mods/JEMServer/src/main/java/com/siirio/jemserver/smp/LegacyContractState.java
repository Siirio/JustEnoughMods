package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Historical contract obligations retained until the rewards owner acknowledges their return. */
public final class LegacyContractState implements SmpRecord {
    private final CompoundTag original;
    private final UUID id;
    private final UUID owner;
    private final long created;
    private int revision;
    private String state;
    private String disposition;
    private ListTag escrow;
    private boolean frozen;

    private LegacyContractState(CompoundTag source) {
        original = source.copy();
        id = source.hasUUID("id") ? source.getUUID("id") : null;
        owner = source.hasUUID("owner") ? source.getUUID("owner") : null;
        created = source.getLong("created");
        revision = source.getInt("revision");
        state = source.getString("state");
        disposition = source.contains("legacyDisposition") ? source.getString("legacyDisposition") : null;
        escrow = source.contains("escrow", Tag.TAG_LIST) ? source.getList("escrow", Tag.TAG_COMPOUND).copy() : null;
    }

    static LegacyContractState decode(CompoundTag source) { return new LegacyContractState(source); }
    CompoundTag encode() {
        var result = original.copy();
        if (revision != original.getInt("revision")) result.putInt("revision", revision);
        if (!state.equals(original.getString("state"))) result.putString("state", state);
        if (disposition == null) result.remove("legacyDisposition");
        else if (!original.contains("legacyDisposition") || !disposition.equals(original.getString("legacyDisposition"))) result.putString("legacyDisposition", disposition);
        if (escrow == null) result.remove("escrow");
        else if (!escrow.equals(original.getList("escrow", Tag.TAG_COMPOUND))) result.put("escrow", escrow.copy());
        return result;
    }
    private LegacyContractState(LegacyContractState other) {
        original = other.original.copy();
        id = other.id; owner = other.owner; created = other.created; revision = other.revision;
        state = other.state; disposition = other.disposition;
        escrow = other.escrow == null ? null : other.escrow.copy();
    }
    public LegacyContractState copy() { return new LegacyContractState(this); }
    public LegacyContractState snapshot() { var copy = copy(); copy.frozen = true; return copy; }
    private void mutable() { if (frozen) throw new IllegalStateException("Immutable contract snapshot"); }
    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public boolean hasOwner() { return owner != null; }
    public long created() { return created; }
    public int revision() { return revision; }
    void revision(int value) { mutable(); revision = value; }
    public String state() { return state; }
    public void state(String value) { mutable(); state = java.util.Objects.requireNonNull(value); }
    public boolean hasLegacyDisposition() { return disposition != null; }
    public String legacyDisposition() { return disposition == null ? "" : disposition; }
    public void legacyDisposition(String value) { mutable(); disposition = java.util.Objects.requireNonNull(value); }
    public boolean hasEscrow() { return escrow != null; }
    /** Native item serialization supplied directly to the independent rewards import boundary. */
    public ListTag escrow() { return escrow == null ? new ListTag() : escrow.copy(); }
    public void clearEscrow() { mutable(); escrow = null; }
}

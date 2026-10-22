package com.siirio.jemserver.smp;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** A preserved native entity plus the exact operator-approved quarantine identity. */
public final class BossQuarantineState implements SmpRecord {
    private final CompoundTag original, entity;
    private final UUID id, keeper, token, previousCanonical;
    private final String arena, dimension, operator;
    private final long created;
    private int revision;
    private String state;
    private boolean frozen;
    private BossQuarantineState(CompoundTag source) {
        original = source.copy(); entity = source.getCompound("entity").copy();
        id = uuid(source, "id"); keeper = uuid(source, "keeper"); token = uuid(source, "token"); previousCanonical = uuid(source, "previousCanonical");
        arena = source.getString("arena"); dimension = source.getString("dimension"); operator = source.getString("operator");
        created = source.getLong("created"); revision = source.getInt("revision"); state = source.getString("state");
    }
    private BossQuarantineState(BossQuarantineState other) {
        original = other.original.copy(); entity = other.entity.copy();
        id = other.id; keeper = other.keeper; token = other.token; previousCanonical = other.previousCanonical;
        arena = other.arena; dimension = other.dimension; operator = other.operator;
        created = other.created; revision = other.revision; state = other.state;
    }
    private static UUID uuid(CompoundTag source, String key) { return source.hasUUID(key) ? source.getUUID(key) : null; }
    static BossQuarantineState decode(CompoundTag source) { return new BossQuarantineState(source); }
    public static BossQuarantineState preserve(UUID id, UUID keeper, UUID token, String arena, String dimension,
            String operator, long created, UUID previousCanonical, CompoundTag nativeEntity) {
        var source = new CompoundTag();
        source.putUUID("id", id); source.putUUID("keeper", keeper); source.putUUID("token", token);
        source.putString("arena", arena); source.putString("dimension", dimension); source.putString("operator", operator);
        source.putLong("created", created); source.putString("state", "PRESERVED_BEFORE_REMOVAL");
        source.putUUID("previousCanonical", previousCanonical); source.put("entity", nativeEntity.copy());
        return decode(source);
    }
    CompoundTag encode() {
        var result = original.copy();
        if (revision != original.getInt("revision")) result.putInt("revision", revision);
        if (!state.equals(original.getString("state"))) result.putString("state", state);
        return result;
    }
    public BossQuarantineState copy() { return new BossQuarantineState(this); }
    public BossQuarantineState snapshot() { var result = copy(); result.frozen = true; return result; }
    private void mutable() { if (frozen) throw new IllegalStateException("Immutable quarantine snapshot"); }
    public UUID id() { return id; }
    public UUID owner() { return null; }
    public UUID keeper() { return keeper; }
    public boolean hasKeeper() { return keeper != null; }
    public UUID token() { return token; }
    public UUID previousCanonical() { return previousCanonical; }
    public String arena() { return arena; }
    public String dimension() { return dimension; }
    public String operator() { return operator; }
    public long created() { return created; }
    public int revision() { return revision; }
    void revision(int value) { mutable(); revision = value; }
    public String state() { return state; }
    public void state(String value) { mutable(); state = java.util.Objects.requireNonNull(value); }
    /** Native entity payload is isolated from the stored quarantine identity. */
    public CompoundTag entitySnapshot() { return entity.copy(); }
}

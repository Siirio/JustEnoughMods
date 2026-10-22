package com.siirio.jemserver.smp;

import java.util.*;

/** Typed state; snapshots reject mutation, including nested values. */
public final class PartyMember {
    private boolean frozen;
    private boolean eliminated, eliminatedPresent, eliminatedChanged;
    public boolean eliminated() { return eliminated; }
    public void eliminated(boolean value) { requireMutable(); eliminated = value; eliminatedPresent = true; eliminatedChanged = true; }

    private boolean legacyArenaBoundaryCleared;
    public void clearLegacyArenaBoundary() { requireMutable(); legacyArenaBoundaryCleared = true; }
    private PartyMemberNbtCodec.Source source = new PartyMemberNbtCodec.Source();
    private String name = "";
    private boolean namePresent, nameChanged;
    private long joinedAt = 0;
    private boolean joinedAtPresent, joinedAtChanged;
    private boolean accepted = false;
    private boolean acceptedPresent, acceptedChanged;
    private boolean ready = false;
    private boolean readyPresent, readyChanged;
    private boolean simulated = false;
    private boolean simulatedPresent, simulatedChanged;
    private boolean raidArenaExitPending = false;
    private boolean raidArenaExitPendingPresent, raidArenaExitPendingChanged;
    private float hp = 0;
    private boolean hpPresent, hpChanged;
    private float maxHp = 0;
    private boolean maxHpPresent, maxHpChanged;
    private int ping = 0;
    private boolean pingPresent, pingChanged;
    private WorldPosition returnPoint;
    private boolean returnPointChanged, returnPending, returnPendingChanged;
    public WorldPosition returnPoint() { return returnPoint; }
    public void returnPoint(WorldPosition value) { requireMutable(); returnPoint = value; returnPointChanged = true; }
    public void clearReturnPoint() { returnPoint(null); }
    public boolean returnPending() { return returnPending; }
    public void returnPending(boolean value) { requireMutable(); returnPending = value; returnPendingChanged = true; }
    public void clearReturnPending() { returnPending(false); }

    public String name() { return name; }
    public boolean hasName() { return namePresent; }
    public void name(String value) { requireMutable(); name = value; namePresent = true; nameChanged = true; }
    public void clearName() { requireMutable(); name = ""; namePresent = false; nameChanged = true; }
    public long joinedAt() { return joinedAt; }
    public boolean hasJoinedAt() { return joinedAtPresent; }
    public void joinedAt(long value) { requireMutable(); joinedAt = value; joinedAtPresent = true; joinedAtChanged = true; }
    public void clearJoinedAt() { requireMutable(); joinedAt = 0; joinedAtPresent = false; joinedAtChanged = true; }
    public boolean accepted() { return accepted; }
    public boolean hasAccepted() { return acceptedPresent; }
    public void accepted(boolean value) { requireMutable(); accepted = value; acceptedPresent = true; acceptedChanged = true; }
    public void clearAccepted() { requireMutable(); accepted = false; acceptedPresent = false; acceptedChanged = true; }
    public boolean ready() { return ready; }
    public boolean hasReady() { return readyPresent; }
    public void ready(boolean value) { requireMutable(); ready = value; readyPresent = true; readyChanged = true; }
    public void clearReady() { requireMutable(); ready = false; readyPresent = false; readyChanged = true; }
    public boolean simulated() { return simulated; }
    public boolean hasSimulated() { return simulatedPresent; }
    public void simulated(boolean value) { requireMutable(); simulated = value; simulatedPresent = true; simulatedChanged = true; }
    public void clearSimulated() { requireMutable(); simulated = false; simulatedPresent = false; simulatedChanged = true; }
    public boolean raidArenaExitPending() { return raidArenaExitPending; }
    public boolean hasRaidArenaExitPending() { return raidArenaExitPendingPresent; }
    public void raidArenaExitPending(boolean value) { requireMutable(); raidArenaExitPending = value; raidArenaExitPendingPresent = true; raidArenaExitPendingChanged = true; }
    public void clearRaidArenaExitPending() { requireMutable(); raidArenaExitPending = false; raidArenaExitPendingPresent = false; raidArenaExitPendingChanged = true; }
    public float hp() { return hp; }
    public boolean hasHp() { return hpPresent; }
    public void hp(float value) { requireMutable(); hp = value; hpPresent = true; hpChanged = true; }
    public void clearHp() { requireMutable(); hp = 0; hpPresent = false; hpChanged = true; }
    public float maxHp() { return maxHp; }
    public boolean hasMaxHp() { return maxHpPresent; }
    public void maxHp(float value) { requireMutable(); maxHp = value; maxHpPresent = true; maxHpChanged = true; }
    public void clearMaxHp() { requireMutable(); maxHp = 0; maxHpPresent = false; maxHpChanged = true; }
    public int ping() { return ping; }
    public boolean hasPing() { return pingPresent; }
    public void ping(int value) { requireMutable(); ping = value; pingPresent = true; pingChanged = true; }
    public void clearPing() { requireMutable(); ping = 0; pingPresent = false; pingChanged = true; }
    private void requireMutable() { if (frozen) throw new IllegalStateException("Cannot mutate an SMP snapshot"); }
    public PartyMember copy() {
        var copy = new PartyMember();
        copy.source = source;
        copy.eliminated = eliminated; copy.eliminatedPresent = eliminatedPresent; copy.eliminatedChanged = eliminatedChanged;
        copy.legacyArenaBoundaryCleared = legacyArenaBoundaryCleared;
        copy.name = name;
        copy.namePresent = namePresent;
        copy.nameChanged = nameChanged;
        copy.joinedAt = joinedAt;
        copy.joinedAtPresent = joinedAtPresent;
        copy.joinedAtChanged = joinedAtChanged;
        copy.accepted = accepted;
        copy.acceptedPresent = acceptedPresent;
        copy.acceptedChanged = acceptedChanged;
        copy.ready = ready;
        copy.readyPresent = readyPresent;
        copy.readyChanged = readyChanged;
        copy.simulated = simulated;
        copy.simulatedPresent = simulatedPresent;
        copy.simulatedChanged = simulatedChanged;
        copy.raidArenaExitPending = raidArenaExitPending;
        copy.raidArenaExitPendingPresent = raidArenaExitPendingPresent;
        copy.raidArenaExitPendingChanged = raidArenaExitPendingChanged;
        copy.hp = hp;
        copy.hpPresent = hpPresent;
        copy.hpChanged = hpChanged;
        copy.maxHp = maxHp;
        copy.maxHpPresent = maxHpPresent;
        copy.maxHpChanged = maxHpChanged;
        copy.ping = ping;
        copy.pingPresent = pingPresent;
        copy.pingChanged = pingChanged;
        copy.returnPoint = returnPoint;
        copy.returnPointChanged = returnPointChanged;
        copy.returnPending = returnPending;
        copy.returnPendingChanged = returnPendingChanged;
        return copy;
    }
    public PartyMember snapshot() { var copy = copy(); copy.frozen = true;  return copy; }

    /** Serialization boundary, nested solely for private state access. */
    static final class Codec {
    static PartyMember decode(net.minecraft.nbt.CompoundTag tag) {
        var value = new PartyMember();
        value.source = new PartyMemberNbtCodec.Source(tag);
        value.eliminated = tag.getBoolean("eliminated"); value.eliminatedPresent = tag.contains("eliminated");
            value.namePresent = tag.contains("name");
            value.name = tag.getString("name");
            value.joinedAtPresent = tag.contains("joinedAt");
            value.joinedAt = tag.getLong("joinedAt");
            value.acceptedPresent = tag.contains("accepted");
            value.accepted = tag.getBoolean("accepted");
            value.readyPresent = tag.contains("ready");
            value.ready = tag.getBoolean("ready");
            value.simulatedPresent = tag.contains("simulated");
            value.simulated = tag.getBoolean("simulated");
            value.raidArenaExitPendingPresent = tag.contains("raidArenaExitPending");
            value.raidArenaExitPending = tag.getBoolean("raidArenaExitPending");
            value.hpPresent = tag.contains("hp");
            value.hp = tag.getFloat("hp");
            value.maxHpPresent = tag.contains("maxHp");
            value.maxHp = tag.getFloat("maxHp");
            value.pingPresent = tag.contains("ping");
            value.ping = tag.getInt("ping");
        if (tag.contains("partyReturnDimension")) value.returnPoint = new WorldPosition(new net.minecraft.resources.ResourceLocation(tag.getString("partyReturnDimension")), tag.getDouble("partyReturnX"), tag.getDouble("partyReturnY"), tag.getDouble("partyReturnZ"), tag.getFloat("partyReturnYaw"), tag.getFloat("partyReturnPitch"));
        value.returnPending = tag.getBoolean("partyReturnPending");
        return value;
    }
    static net.minecraft.nbt.CompoundTag encode(PartyMember value) {
        var tag = value.source.copy();
        if (value.eliminatedChanged) tag.putBoolean("eliminated", value.eliminated);
        if (value.legacyArenaBoundaryCleared) for (String key : List.of("arenaEntered", "arenaInsideX", "arenaInsideY", "arenaInsideZ")) tag.remove(key);
            if (value.nameChanged) { if (value.namePresent) tag.putString("name", value.name); else tag.remove("name"); }
            if (value.joinedAtChanged) { if (value.joinedAtPresent) tag.putLong("joinedAt", value.joinedAt); else tag.remove("joinedAt"); }
            if (value.acceptedChanged) { if (value.acceptedPresent) tag.putBoolean("accepted", value.accepted); else tag.remove("accepted"); }
            if (value.readyChanged) { if (value.readyPresent) tag.putBoolean("ready", value.ready); else tag.remove("ready"); }
            if (value.simulatedChanged) { if (value.simulatedPresent) tag.putBoolean("simulated", value.simulated); else tag.remove("simulated"); }
            if (value.raidArenaExitPendingChanged) { if (value.raidArenaExitPendingPresent) tag.putBoolean("raidArenaExitPending", value.raidArenaExitPending); else tag.remove("raidArenaExitPending"); }
            if (value.hpChanged) { if (value.hpPresent) tag.putFloat("hp", value.hp); else tag.remove("hp"); }
            if (value.maxHpChanged) { if (value.maxHpPresent) tag.putFloat("maxHp", value.maxHp); else tag.remove("maxHp"); }
            if (value.pingChanged) { if (value.pingPresent) tag.putInt("ping", value.ping); else tag.remove("ping"); }
        if (value.returnPendingChanged) { if (value.returnPending) tag.putBoolean("partyReturnPending", true); else tag.remove("partyReturnPending"); }
        if (value.returnPointChanged) {
            if (value.returnPoint == null) {
                for (String key : List.of("partyReturnDimension", "partyReturnX", "partyReturnY", "partyReturnZ", "partyReturnYaw", "partyReturnPitch")) tag.remove(key);
            } else {
                var point = value.returnPoint;
                tag.putString("partyReturnDimension", point.dimension().toString());
                tag.putDouble("partyReturnX", point.x()); tag.putDouble("partyReturnY", point.y()); tag.putDouble("partyReturnZ", point.z());
                tag.putFloat("partyReturnYaw", point.yaw()); tag.putFloat("partyReturnPitch", point.pitch());
            }
        }
        return tag;
    }
    }
}

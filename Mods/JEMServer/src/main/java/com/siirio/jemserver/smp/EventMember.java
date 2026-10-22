package com.siirio.jemserver.smp;

import java.util.*;

/** Typed event state. Frozen snapshots cannot mutate canonical or nested state. */
public final class EventMember {
    private Integer waves;
    boolean wavesChanged;
    private Boolean raidArenaExitPending;
    boolean raidArenaExitPendingChanged;
    private boolean frozen;
    EventNbtCodec.Source source = new EventNbtCodec.Source();
    private String name;
    boolean nameChanged;
    private String originDimension;
    boolean originDimensionChanged;
    private Integer actualDeaths;
    boolean actualDeathsChanged;
    private Integer kills;
    boolean killsChanged;
    private Integer revives;
    boolean revivesChanged;
    private Long joinedAt;
    boolean joinedAtChanged;
    private Long encounterArrival;
    boolean encounterArrivalChanged;
    private Long eventDisconnectedAt;
    boolean eventDisconnectedAtChanged;
    private Long downedAt;
    boolean downedAtChanged;
    private Boolean accepted;
    boolean acceptedChanged;
    private Boolean ready;
    boolean readyChanged;
    private Boolean eliminated;
    boolean eliminatedChanged;
    private Boolean contributed;
    boolean contributedChanged;
    private Boolean supported;
    boolean supportedChanged;
    private Boolean safeRespawn;
    boolean safeRespawnChanged;
    private Boolean awaitingReturn;
    boolean awaitingReturnChanged;
    private Boolean encounterTeleported;
    boolean encounterTeleportedChanged;
    private Boolean eventReturnPending;
    boolean eventReturnPendingChanged;
    private Boolean eventAbsenceDefeatPending;
    boolean eventAbsenceDefeatPendingChanged;
    private Double damage;
    boolean damageChanged;
    private Double safeX;
    boolean safeXChanged;
    private Double safeY;
    boolean safeYChanged;
    private Double safeZ;
    boolean safeZChanged;
    private Double originX;
    boolean originXChanged;
    private Double originY;
    boolean originYChanged;
    private Double originZ;
    boolean originZChanged;
    private Float originYaw;
    boolean originYawChanged;
    private Float originPitch;
    boolean originPitchChanged;
    private NativeNbtPayload safeInventory;
    boolean safeInventoryChanged;
    private NativeNbtPayload safeDrops;
    boolean safeDropsChanged;

    private void requireMutable() { if (frozen) throw new IllegalStateException("Immutable event snapshot"); }
    public EventMember copy() {
        var copy = new EventMember();
        copy.source = source.copy();
        copy.raidArenaExitPending = raidArenaExitPending; copy.raidArenaExitPendingChanged = raidArenaExitPendingChanged;
        copy.waves = waves; copy.wavesChanged = wavesChanged;
        copy.name = name;
        copy.nameChanged = nameChanged;
        copy.originDimension = originDimension;
        copy.originDimensionChanged = originDimensionChanged;
        copy.actualDeaths = actualDeaths;
        copy.actualDeathsChanged = actualDeathsChanged;
        copy.kills = kills;
        copy.killsChanged = killsChanged;
        copy.revives = revives;
        copy.revivesChanged = revivesChanged;
        copy.joinedAt = joinedAt;
        copy.joinedAtChanged = joinedAtChanged;
        copy.encounterArrival = encounterArrival;
        copy.encounterArrivalChanged = encounterArrivalChanged;
        copy.eventDisconnectedAt = eventDisconnectedAt;
        copy.eventDisconnectedAtChanged = eventDisconnectedAtChanged;
        copy.downedAt = downedAt;
        copy.downedAtChanged = downedAtChanged;
        copy.accepted = accepted;
        copy.acceptedChanged = acceptedChanged;
        copy.ready = ready;
        copy.readyChanged = readyChanged;
        copy.eliminated = eliminated;
        copy.eliminatedChanged = eliminatedChanged;
        copy.contributed = contributed;
        copy.contributedChanged = contributedChanged;
        copy.supported = supported;
        copy.supportedChanged = supportedChanged;
        copy.safeRespawn = safeRespawn;
        copy.safeRespawnChanged = safeRespawnChanged;
        copy.awaitingReturn = awaitingReturn;
        copy.awaitingReturnChanged = awaitingReturnChanged;
        copy.encounterTeleported = encounterTeleported;
        copy.encounterTeleportedChanged = encounterTeleportedChanged;
        copy.eventReturnPending = eventReturnPending;
        copy.eventReturnPendingChanged = eventReturnPendingChanged;
        copy.eventAbsenceDefeatPending = eventAbsenceDefeatPending;
        copy.eventAbsenceDefeatPendingChanged = eventAbsenceDefeatPendingChanged;
        copy.damage = damage;
        copy.damageChanged = damageChanged;
        copy.safeX = safeX;
        copy.safeXChanged = safeXChanged;
        copy.safeY = safeY;
        copy.safeYChanged = safeYChanged;
        copy.safeZ = safeZ;
        copy.safeZChanged = safeZChanged;
        copy.originX = originX;
        copy.originXChanged = originXChanged;
        copy.originY = originY;
        copy.originYChanged = originYChanged;
        copy.originZ = originZ;
        copy.originZChanged = originZChanged;
        copy.originYaw = originYaw;
        copy.originYawChanged = originYawChanged;
        copy.originPitch = originPitch;
        copy.originPitchChanged = originPitchChanged;
        copy.safeInventory = safeInventory;
        copy.safeInventoryChanged = safeInventoryChanged;
        copy.safeDrops = safeDrops;
        copy.safeDropsChanged = safeDropsChanged;
        return copy;
    }
    public EventMember snapshot() { var copy = copy(); copy.freeze(); return copy; }
    void freeze() {
        frozen = true;
    }

    public String name() { return name == null ? "" : name; }
    public void name(String value) { requireMutable(); nameChanged = true; name = value; }
    public boolean hasName() { return name != null; }
    public void clearName() { requireMutable(); nameChanged = true; name = null; }
    public String originDimension() { return originDimension == null ? "" : originDimension; }
    public void originDimension(String value) { requireMutable(); originDimensionChanged = true; originDimension = value; }
    public boolean hasOriginDimension() { return originDimension != null; }
    public void clearOriginDimension() { requireMutable(); originDimensionChanged = true; originDimension = null; }
    public int actualDeaths() { return actualDeaths == null ? 0 : actualDeaths; }
    public void actualDeaths(int value) { requireMutable(); actualDeathsChanged = true; actualDeaths = value; }
    public boolean hasActualDeaths() { return actualDeaths != null; }
    public void clearActualDeaths() { requireMutable(); actualDeathsChanged = true; actualDeaths = null; }
    public int kills() { return kills == null ? 0 : kills; }
    public void kills(int value) { requireMutable(); killsChanged = true; kills = value; }
    public boolean hasKills() { return kills != null; }
    public void clearKills() { requireMutable(); killsChanged = true; kills = null; }
    public int revives() { return revives == null ? 0 : revives; }
    public void revives(int value) { requireMutable(); revivesChanged = true; revives = value; }
    public boolean hasRevives() { return revives != null; }
    public void clearRevives() { requireMutable(); revivesChanged = true; revives = null; }
    public long joinedAt() { return joinedAt == null ? 0L : joinedAt; }
    public void joinedAt(long value) { requireMutable(); joinedAtChanged = true; joinedAt = value; }
    public boolean hasJoinedAt() { return joinedAt != null; }
    public void clearJoinedAt() { requireMutable(); joinedAtChanged = true; joinedAt = null; }
    public long encounterArrival() { return encounterArrival == null ? 0L : encounterArrival; }
    public void encounterArrival(long value) { requireMutable(); encounterArrivalChanged = true; encounterArrival = value; }
    public boolean hasEncounterArrival() { return encounterArrival != null; }
    public void clearEncounterArrival() { requireMutable(); encounterArrivalChanged = true; encounterArrival = null; }
    public long eventDisconnectedAt() { return eventDisconnectedAt == null ? 0L : eventDisconnectedAt; }
    public void eventDisconnectedAt(long value) { requireMutable(); eventDisconnectedAtChanged = true; eventDisconnectedAt = value; }
    public boolean hasEventDisconnectedAt() { return eventDisconnectedAt != null; }
    public void clearEventDisconnectedAt() { requireMutable(); eventDisconnectedAtChanged = true; eventDisconnectedAt = null; }
    public long downedAt() { return downedAt == null ? 0L : downedAt; }
    public void downedAt(long value) { requireMutable(); downedAtChanged = true; downedAt = value; }
    public boolean hasDownedAt() { return downedAt != null; }
    public void clearDownedAt() { requireMutable(); downedAtChanged = true; downedAt = null; }
    public boolean accepted() { return accepted == null ? false : accepted; }
    public void accepted(boolean value) { requireMutable(); acceptedChanged = true; accepted = value; }
    public boolean hasAccepted() { return accepted != null; }
    public void clearAccepted() { requireMutable(); acceptedChanged = true; accepted = null; }
    public boolean ready() { return ready == null ? false : ready; }
    public void ready(boolean value) { requireMutable(); readyChanged = true; ready = value; }
    public boolean hasReady() { return ready != null; }
    public void clearReady() { requireMutable(); readyChanged = true; ready = null; }
    public boolean eliminated() { return eliminated == null ? false : eliminated; }
    public void eliminated(boolean value) { requireMutable(); eliminatedChanged = true; eliminated = value; }
    public boolean hasEliminated() { return eliminated != null; }
    public void clearEliminated() { requireMutable(); eliminatedChanged = true; eliminated = null; }
    public boolean contributed() { return contributed == null ? false : contributed; }
    public void contributed(boolean value) { requireMutable(); contributedChanged = true; contributed = value; }
    public boolean hasContributed() { return contributed != null; }
    public void clearContributed() { requireMutable(); contributedChanged = true; contributed = null; }
    public boolean supported() { return supported == null ? false : supported; }
    public void supported(boolean value) { requireMutable(); supportedChanged = true; supported = value; }
    public boolean hasSupported() { return supported != null; }
    public void clearSupported() { requireMutable(); supportedChanged = true; supported = null; }
    public boolean safeRespawn() { return safeRespawn == null ? false : safeRespawn; }
    public void safeRespawn(boolean value) { requireMutable(); safeRespawnChanged = true; safeRespawn = value; }
    public boolean hasSafeRespawn() { return safeRespawn != null; }
    public void clearSafeRespawn() { requireMutable(); safeRespawnChanged = true; safeRespawn = null; }
    public boolean awaitingReturn() { return awaitingReturn == null ? false : awaitingReturn; }
    public void awaitingReturn(boolean value) { requireMutable(); awaitingReturnChanged = true; awaitingReturn = value; }
    public boolean hasAwaitingReturn() { return awaitingReturn != null; }
    public void clearAwaitingReturn() { requireMutable(); awaitingReturnChanged = true; awaitingReturn = null; }
    public boolean encounterTeleported() { return encounterTeleported == null ? false : encounterTeleported; }
    public void encounterTeleported(boolean value) { requireMutable(); encounterTeleportedChanged = true; encounterTeleported = value; }
    public boolean hasEncounterTeleported() { return encounterTeleported != null; }
    public void clearEncounterTeleported() { requireMutable(); encounterTeleportedChanged = true; encounterTeleported = null; }
    public boolean eventReturnPending() { return eventReturnPending == null ? false : eventReturnPending; }
    public void eventReturnPending(boolean value) { requireMutable(); eventReturnPendingChanged = true; eventReturnPending = value; }
    public boolean hasEventReturnPending() { return eventReturnPending != null; }
    public void clearEventReturnPending() { requireMutable(); eventReturnPendingChanged = true; eventReturnPending = null; }
    public boolean eventAbsenceDefeatPending() { return eventAbsenceDefeatPending == null ? false : eventAbsenceDefeatPending; }
    public void eventAbsenceDefeatPending(boolean value) { requireMutable(); eventAbsenceDefeatPendingChanged = true; eventAbsenceDefeatPending = value; }
    public boolean hasEventAbsenceDefeatPending() { return eventAbsenceDefeatPending != null; }
    public void clearEventAbsenceDefeatPending() { requireMutable(); eventAbsenceDefeatPendingChanged = true; eventAbsenceDefeatPending = null; }
    public double damage() { return damage == null ? 0D : damage; }
    public void damage(double value) { requireMutable(); damageChanged = true; damage = value; }
    public boolean hasDamage() { return damage != null; }
    public void clearDamage() { requireMutable(); damageChanged = true; damage = null; }
    public double safeX() { return safeX == null ? 0D : safeX; }
    public void safeX(double value) { requireMutable(); safeXChanged = true; safeX = value; }
    public boolean hasSafeX() { return safeX != null; }
    public void clearSafeX() { requireMutable(); safeXChanged = true; safeX = null; }
    public double safeY() { return safeY == null ? 0D : safeY; }
    public void safeY(double value) { requireMutable(); safeYChanged = true; safeY = value; }
    public boolean hasSafeY() { return safeY != null; }
    public void clearSafeY() { requireMutable(); safeYChanged = true; safeY = null; }
    public double safeZ() { return safeZ == null ? 0D : safeZ; }
    public void safeZ(double value) { requireMutable(); safeZChanged = true; safeZ = value; }
    public boolean hasSafeZ() { return safeZ != null; }
    public void clearSafeZ() { requireMutable(); safeZChanged = true; safeZ = null; }
    public double originX() { return originX == null ? 0D : originX; }
    public void originX(double value) { requireMutable(); originXChanged = true; originX = value; }
    public boolean hasOriginX() { return originX != null; }
    public void clearOriginX() { requireMutable(); originXChanged = true; originX = null; }
    public double originY() { return originY == null ? 0D : originY; }
    public void originY(double value) { requireMutable(); originYChanged = true; originY = value; }
    public boolean hasOriginY() { return originY != null; }
    public void clearOriginY() { requireMutable(); originYChanged = true; originY = null; }
    public double originZ() { return originZ == null ? 0D : originZ; }
    public void originZ(double value) { requireMutable(); originZChanged = true; originZ = value; }
    public boolean hasOriginZ() { return originZ != null; }
    public void clearOriginZ() { requireMutable(); originZChanged = true; originZ = null; }
    public float originYaw() { return originYaw == null ? 0F : originYaw; }
    public void originYaw(float value) { requireMutable(); originYawChanged = true; originYaw = value; }
    public boolean hasOriginYaw() { return originYaw != null; }
    public void clearOriginYaw() { requireMutable(); originYawChanged = true; originYaw = null; }
    public float originPitch() { return originPitch == null ? 0F : originPitch; }
    public void originPitch(float value) { requireMutable(); originPitchChanged = true; originPitch = value; }
    public boolean hasOriginPitch() { return originPitch != null; }
    public void clearOriginPitch() { requireMutable(); originPitchChanged = true; originPitch = null; }
    public NativeNbtPayload safeInventory() { return safeInventory == null ? null : safeInventory; }
    public void safeInventory(NativeNbtPayload value) { requireMutable(); safeInventoryChanged = true; safeInventory = value; }
    public boolean hasSafeInventory() { return safeInventory != null; }
    public void clearSafeInventory() { requireMutable(); safeInventoryChanged = true; safeInventory = null; }
    public NativeNbtPayload safeDrops() { return safeDrops == null ? null : safeDrops; }
    public void safeDrops(NativeNbtPayload value) { requireMutable(); safeDropsChanged = true; safeDrops = value; }
    public boolean hasSafeDrops() { return safeDrops != null; }
    public void clearSafeDrops() { requireMutable(); safeDropsChanged = true; safeDrops = null; }
    public int waves() { return waves == null ? 0 : waves; }
    public void waves(int value) { requireMutable(); wavesChanged = true; waves = value; }
    public boolean hasWaves() { return waves != null; }
    public void clearWaves() { requireMutable(); wavesChanged = true; waves = null; }
    public boolean raidArenaExitPending() { return Boolean.TRUE.equals(raidArenaExitPending); }
    public void raidArenaExitPending(boolean value) { requireMutable(); raidArenaExitPending = value; raidArenaExitPendingChanged = true; }
    public boolean hasRaidArenaExitPending() { return raidArenaExitPending != null; }
    public void clearRaidArenaExitPending() { requireMutable(); raidArenaExitPending = null; raidArenaExitPendingChanged = true; }
}

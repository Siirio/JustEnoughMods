package com.siirio.jemserver.smp;

import java.util.*;

/** Typed state; snapshots reject mutation, including nested values. */
public final class PartyState implements SmpRecord {
    private boolean frozen;
    private PartyStateNbtCodec.Source source = new PartyStateNbtCodec.Source();
    private long ends;
    private boolean endsPresent, endsChanged;
    public long ends() { return ends; }
    public boolean hasEnds() { return endsPresent; }
    public void ends(long value) { requireMutable(); ends = value; endsPresent = true; endsChanged = true; }
    public void clearEnds() { requireMutable(); ends = 0; endsPresent = false; endsChanged = true; }
    private String arenaId = "";
    private boolean arenaIdPresent, arenaIdChanged;
    public String arenaId() { return arenaId; }
    public boolean hasArenaId() { return arenaIdPresent; }
    public void arenaId(String value) { requireMutable(); arenaId = value; arenaIdPresent = true; arenaIdChanged = true; }
    public void clearArenaId() { requireMutable(); arenaId = ""; arenaIdPresent = false; arenaIdChanged = true; }
    private String raidArenaId = "";
    private boolean raidArenaIdPresent, raidArenaIdChanged;
    public String raidArenaId() { return raidArenaId; }
    public boolean hasRaidArenaId() { return raidArenaIdPresent; }
    public void raidArenaId(String value) { requireMutable(); raidArenaId = value; raidArenaIdPresent = true; raidArenaIdChanged = true; }
    public void clearRaidArenaId() { requireMutable(); raidArenaId = ""; raidArenaIdPresent = false; raidArenaIdChanged = true; }
    private int radius;
    private boolean radiusPresent, radiusChanged;
    public int radius() { return radius; }
    public boolean hasRadius() { return radiusPresent; }
    public void radius(int value) { requireMutable(); radius = value; radiusPresent = true; radiusChanged = true; }
    public void clearRadius() { requireMutable(); radius = 0; radiusPresent = false; radiusChanged = true; }
    private int minX;
    private boolean minXPresent, minXChanged;
    public int minX() { return minX; }
    public boolean hasMinX() { return minXPresent; }
    public void minX(int value) { requireMutable(); minX = value; minXPresent = true; minXChanged = true; }
    public void clearMinX() { requireMutable(); minX = 0; minXPresent = false; minXChanged = true; }
    private int minY;
    private boolean minYPresent, minYChanged;
    public int minY() { return minY; }
    public boolean hasMinY() { return minYPresent; }
    public void minY(int value) { requireMutable(); minY = value; minYPresent = true; minYChanged = true; }
    public void clearMinY() { requireMutable(); minY = 0; minYPresent = false; minYChanged = true; }
    private int minZ;
    private boolean minZPresent, minZChanged;
    public int minZ() { return minZ; }
    public boolean hasMinZ() { return minZPresent; }
    public void minZ(int value) { requireMutable(); minZ = value; minZPresent = true; minZChanged = true; }
    public void clearMinZ() { requireMutable(); minZ = 0; minZPresent = false; minZChanged = true; }
    private int maxX;
    private boolean maxXPresent, maxXChanged;
    public int maxX() { return maxX; }
    public boolean hasMaxX() { return maxXPresent; }
    public void maxX(int value) { requireMutable(); maxX = value; maxXPresent = true; maxXChanged = true; }
    public void clearMaxX() { requireMutable(); maxX = 0; maxXPresent = false; maxXChanged = true; }
    private int maxY;
    private boolean maxYPresent, maxYChanged;
    public int maxY() { return maxY; }
    public boolean hasMaxY() { return maxYPresent; }
    public void maxY(int value) { requireMutable(); maxY = value; maxYPresent = true; maxYChanged = true; }
    public void clearMaxY() { requireMutable(); maxY = 0; maxYPresent = false; maxYChanged = true; }
    private int maxZ;
    private boolean maxZPresent, maxZChanged;
    public int maxZ() { return maxZ; }
    public boolean hasMaxZ() { return maxZPresent; }
    public void maxZ(int value) { requireMutable(); maxZ = value; maxZPresent = true; maxZChanged = true; }
    public void clearMaxZ() { requireMutable(); maxZ = 0; maxZPresent = false; maxZChanged = true; }
    private UUID id = null;
    private boolean idPresent, idChanged;
    private UUID owner = null;
    private boolean ownerPresent, ownerChanged;
    private UUID bossEntity = null;
    private boolean bossEntityPresent, bossEntityChanged;
    private UUID eventId = null;
    private boolean eventIdPresent, eventIdChanged;
    private UUID structureId = null;
    private boolean structureIdPresent, structureIdChanged;
    private UUID partyTeleportSession = null;
    private boolean partyTeleportSessionPresent, partyTeleportSessionChanged;
    private int revision = 0;
    private boolean revisionPresent, revisionChanged;
    private long created = 0;
    private boolean createdPresent, createdChanged;
    private String title = "";
    private boolean titlePresent, titleChanged;
    private String activity = "";
    private boolean activityPresent, activityChanged;
    private String description = "";
    private boolean descriptionPresent, descriptionChanged;
    private String language = "";
    private boolean languagePresent, languageChanged;
    private String name = "";
    private boolean namePresent, nameChanged;
    private String state = "";
    private boolean statePresent, stateChanged;
    private String dimension = "";
    private boolean dimensionPresent, dimensionChanged;
    private String bossType = "";
    private boolean bossTypePresent, bossTypeChanged;
    private String structureBoss = "";
    private boolean structureBossPresent, structureBossChanged;
    private long scheduled = 0;
    private boolean scheduledPresent, scheduledChanged;
    private long position = 0;
    private boolean positionPresent, positionChanged;
    private int limit = 0;
    private boolean limitPresent, limitChanged;
    private boolean approval = false;
    private boolean approvalPresent, approvalChanged;
    private boolean solo = false;
    private boolean soloPresent, soloChanged;
    private boolean bloodMoon = false;
    private boolean bloodMoonPresent, bloodMoonChanged;
    private boolean raid = false;
    private boolean raidPresent, raidChanged;
    private boolean nativePending = false;
    private boolean nativePendingPresent, nativePendingChanged;
    private final Map<UUID, PartyMember> members = new LinkedHashMap<>();
    private final Map<UUID, Long> invitations = new LinkedHashMap<>();
    private boolean invitationsPresent, invitationsCleared;
    private final Set<UUID> invitationChanges = new LinkedHashSet<>();
    private StructureBounds structureBounds;
    private boolean structureBoundsChanged, membersChanged, invitationsChanged;
    public Map<UUID, PartyMember> members() { return Collections.unmodifiableMap(members); }
    public PartyMember member(UUID id) { return members.get(id); }
    public void addMember(UUID id, PartyMember member) { requireMutable(); members.put(id, member); membersChanged = true; }
    public void removeMember(UUID id) { requireMutable(); members.remove(id); membersChanged = true; }
    public Map<UUID, Long> invitations() { return Collections.unmodifiableMap(invitations); }
    public void invite(UUID id, long time) { requireMutable(); invitations.put(id, time); invitationsPresent = true; invitationChanges.add(id); invitationsChanged = true; }
    public void removeInvitation(UUID id) { requireMutable(); if (invitations.remove(id) != null) { invitationChanges.add(id); invitationsChanged = true; } }
    public void clearInvitations() { requireMutable(); invitations.clear(); invitationsPresent = false; invitationsCleared = true; invitationChanges.clear(); invitationsChanged = true; }
    public StructureBounds structureBounds() { return structureBounds; }
    public boolean hasStructureBounds() { return structureBounds != null; }
    public void structureBounds(StructureBounds value) { requireMutable(); structureBounds = value; structureBoundsChanged = true; }
    public void clearStructureBounds() { structureBounds(null); }

    public UUID id() { return id; }
    public boolean hasId() { return idPresent; }
    public void id(UUID value) { requireMutable(); id = value; idPresent = true; idChanged = true; }
    public void clearId() { requireMutable(); id = null; idPresent = false; idChanged = true; }
    public UUID owner() { return owner; }
    public boolean hasOwner() { return ownerPresent; }
    public void owner(UUID value) { requireMutable(); owner = value; ownerPresent = true; ownerChanged = true; }
    public void clearOwner() { requireMutable(); owner = null; ownerPresent = false; ownerChanged = true; }
    public UUID bossEntity() { return bossEntity; }
    public boolean hasBossEntity() { return bossEntityPresent; }
    public void bossEntity(UUID value) { requireMutable(); bossEntity = value; bossEntityPresent = true; bossEntityChanged = true; }
    public void clearBossEntity() { requireMutable(); bossEntity = null; bossEntityPresent = false; bossEntityChanged = true; }
    public UUID eventId() { return eventId; }
    public boolean hasEventId() { return eventIdPresent; }
    public void eventId(UUID value) { requireMutable(); eventId = value; eventIdPresent = true; eventIdChanged = true; }
    public void clearEventId() { requireMutable(); eventId = null; eventIdPresent = false; eventIdChanged = true; }
    public UUID structureId() { return structureId; }
    public boolean hasStructureId() { return structureIdPresent; }
    public void structureId(UUID value) { requireMutable(); structureId = value; structureIdPresent = true; structureIdChanged = true; }
    public void clearStructureId() { requireMutable(); structureId = null; structureIdPresent = false; structureIdChanged = true; }
    public UUID partyTeleportSession() { return partyTeleportSession; }
    public boolean hasPartyTeleportSession() { return partyTeleportSessionPresent; }
    public void partyTeleportSession(UUID value) { requireMutable(); partyTeleportSession = value; partyTeleportSessionPresent = true; partyTeleportSessionChanged = true; }
    public void clearPartyTeleportSession() { requireMutable(); partyTeleportSession = null; partyTeleportSessionPresent = false; partyTeleportSessionChanged = true; }
    public int revision() { return revision; }
    public boolean hasRevision() { return revisionPresent; }
    public void revision(int value) { requireMutable(); revision = value; revisionPresent = true; revisionChanged = true; }
    public void clearRevision() { requireMutable(); revision = 0; revisionPresent = false; revisionChanged = true; }
    public long created() { return created; }
    public boolean hasCreated() { return createdPresent; }
    public void created(long value) { requireMutable(); created = value; createdPresent = true; createdChanged = true; }
    public void clearCreated() { requireMutable(); created = 0; createdPresent = false; createdChanged = true; }
    public String title() { return title; }
    public boolean hasTitle() { return titlePresent; }
    public void title(String value) { requireMutable(); title = value; titlePresent = true; titleChanged = true; }
    public void clearTitle() { requireMutable(); title = ""; titlePresent = false; titleChanged = true; }
    public String activity() { return activity; }
    public boolean hasActivity() { return activityPresent; }
    public void activity(String value) { requireMutable(); activity = value; activityPresent = true; activityChanged = true; }
    public void clearActivity() { requireMutable(); activity = ""; activityPresent = false; activityChanged = true; }
    public String description() { return description; }
    public boolean hasDescription() { return descriptionPresent; }
    public void description(String value) { requireMutable(); description = value; descriptionPresent = true; descriptionChanged = true; }
    public void clearDescription() { requireMutable(); description = ""; descriptionPresent = false; descriptionChanged = true; }
    public String language() { return language; }
    public boolean hasLanguage() { return languagePresent; }
    public void language(String value) { requireMutable(); language = value; languagePresent = true; languageChanged = true; }
    public void clearLanguage() { requireMutable(); language = ""; languagePresent = false; languageChanged = true; }
    public String name() { return name; }
    public boolean hasName() { return namePresent; }
    public void name(String value) { requireMutable(); name = value; namePresent = true; nameChanged = true; }
    public void clearName() { requireMutable(); name = ""; namePresent = false; nameChanged = true; }
    public String state() { return state; }
    public boolean hasState() { return statePresent; }
    public void state(String value) { requireMutable(); state = value; statePresent = true; stateChanged = true; }
    public void clearState() { requireMutable(); state = ""; statePresent = false; stateChanged = true; }
    public String dimension() { return dimension; }
    public boolean hasDimension() { return dimensionPresent; }
    public void dimension(String value) { requireMutable(); dimension = value; dimensionPresent = true; dimensionChanged = true; }
    public void clearDimension() { requireMutable(); dimension = ""; dimensionPresent = false; dimensionChanged = true; }
    public String bossType() { return bossType; }
    public boolean hasBossType() { return bossTypePresent; }
    public void bossType(String value) { requireMutable(); bossType = value; bossTypePresent = true; bossTypeChanged = true; }
    public void clearBossType() { requireMutable(); bossType = ""; bossTypePresent = false; bossTypeChanged = true; }
    public String structureBoss() { return structureBoss; }
    public boolean hasStructureBoss() { return structureBossPresent; }
    public void structureBoss(String value) { requireMutable(); structureBoss = value; structureBossPresent = true; structureBossChanged = true; }
    public void clearStructureBoss() { requireMutable(); structureBoss = ""; structureBossPresent = false; structureBossChanged = true; }
    public long scheduled() { return scheduled; }
    public boolean hasScheduled() { return scheduledPresent; }
    public void scheduled(long value) { requireMutable(); scheduled = value; scheduledPresent = true; scheduledChanged = true; }
    public void clearScheduled() { requireMutable(); scheduled = 0; scheduledPresent = false; scheduledChanged = true; }
    public long position() { return position; }
    public boolean hasPosition() { return positionPresent; }
    public void position(long value) { requireMutable(); position = value; positionPresent = true; positionChanged = true; }
    public void clearPosition() { requireMutable(); position = 0; positionPresent = false; positionChanged = true; }
    public int limit() { return limit; }
    public boolean hasLimit() { return limitPresent; }
    public void limit(int value) { requireMutable(); limit = value; limitPresent = true; limitChanged = true; }
    public void clearLimit() { requireMutable(); limit = 0; limitPresent = false; limitChanged = true; }
    public boolean approval() { return approval; }
    public boolean hasApproval() { return approvalPresent; }
    public void approval(boolean value) { requireMutable(); approval = value; approvalPresent = true; approvalChanged = true; }
    public void clearApproval() { requireMutable(); approval = false; approvalPresent = false; approvalChanged = true; }
    public boolean solo() { return solo; }
    public boolean hasSolo() { return soloPresent; }
    public void solo(boolean value) { requireMutable(); solo = value; soloPresent = true; soloChanged = true; }
    public void clearSolo() { requireMutable(); solo = false; soloPresent = false; soloChanged = true; }
    public boolean bloodMoon() { return bloodMoon; }
    public boolean hasBloodMoon() { return bloodMoonPresent; }
    public void bloodMoon(boolean value) { requireMutable(); bloodMoon = value; bloodMoonPresent = true; bloodMoonChanged = true; }
    public void clearBloodMoon() { requireMutable(); bloodMoon = false; bloodMoonPresent = false; bloodMoonChanged = true; }
    public boolean raid() { return raid; }
    public boolean hasRaid() { return raidPresent; }
    public void raid(boolean value) { requireMutable(); raid = value; raidPresent = true; raidChanged = true; }
    public void clearRaid() { requireMutable(); raid = false; raidPresent = false; raidChanged = true; }
    public boolean nativePending() { return nativePending; }
    public boolean hasNativePending() { return nativePendingPresent; }
    public void nativePending(boolean value) { requireMutable(); nativePending = value; nativePendingPresent = true; nativePendingChanged = true; }
    public void clearNativePending() { requireMutable(); nativePending = false; nativePendingPresent = false; nativePendingChanged = true; }
    private void requireMutable() { if (frozen) throw new IllegalStateException("Cannot mutate an SMP snapshot"); }
    public PartyState copy() {
        var copy = new PartyState();
        copy.source = source;
        copy.maxZ = maxZ; copy.maxZPresent = maxZPresent; copy.maxZChanged = maxZChanged;
        copy.maxY = maxY; copy.maxYPresent = maxYPresent; copy.maxYChanged = maxYChanged;
        copy.maxX = maxX; copy.maxXPresent = maxXPresent; copy.maxXChanged = maxXChanged;
        copy.minZ = minZ; copy.minZPresent = minZPresent; copy.minZChanged = minZChanged;
        copy.minY = minY; copy.minYPresent = minYPresent; copy.minYChanged = minYChanged;
        copy.minX = minX; copy.minXPresent = minXPresent; copy.minXChanged = minXChanged;
        copy.radius = radius; copy.radiusPresent = radiusPresent; copy.radiusChanged = radiusChanged;
        copy.raidArenaId = raidArenaId; copy.raidArenaIdPresent = raidArenaIdPresent; copy.raidArenaIdChanged = raidArenaIdChanged;
        copy.arenaId = arenaId; copy.arenaIdPresent = arenaIdPresent; copy.arenaIdChanged = arenaIdChanged;
        copy.ends = ends; copy.endsPresent = endsPresent; copy.endsChanged = endsChanged;
        copy.id = id;
        copy.idPresent = idPresent;
        copy.idChanged = idChanged;
        copy.owner = owner;
        copy.ownerPresent = ownerPresent;
        copy.ownerChanged = ownerChanged;
        copy.bossEntity = bossEntity;
        copy.bossEntityPresent = bossEntityPresent;
        copy.bossEntityChanged = bossEntityChanged;
        copy.eventId = eventId;
        copy.eventIdPresent = eventIdPresent;
        copy.eventIdChanged = eventIdChanged;
        copy.structureId = structureId;
        copy.structureIdPresent = structureIdPresent;
        copy.structureIdChanged = structureIdChanged;
        copy.partyTeleportSession = partyTeleportSession;
        copy.partyTeleportSessionPresent = partyTeleportSessionPresent;
        copy.partyTeleportSessionChanged = partyTeleportSessionChanged;
        copy.revision = revision;
        copy.revisionPresent = revisionPresent;
        copy.revisionChanged = revisionChanged;
        copy.created = created;
        copy.createdPresent = createdPresent;
        copy.createdChanged = createdChanged;
        copy.title = title;
        copy.titlePresent = titlePresent;
        copy.titleChanged = titleChanged;
        copy.activity = activity;
        copy.activityPresent = activityPresent;
        copy.activityChanged = activityChanged;
        copy.description = description;
        copy.descriptionPresent = descriptionPresent;
        copy.descriptionChanged = descriptionChanged;
        copy.language = language;
        copy.languagePresent = languagePresent;
        copy.languageChanged = languageChanged;
        copy.name = name;
        copy.namePresent = namePresent;
        copy.nameChanged = nameChanged;
        copy.state = state;
        copy.statePresent = statePresent;
        copy.stateChanged = stateChanged;
        copy.dimension = dimension;
        copy.dimensionPresent = dimensionPresent;
        copy.dimensionChanged = dimensionChanged;
        copy.bossType = bossType;
        copy.bossTypePresent = bossTypePresent;
        copy.bossTypeChanged = bossTypeChanged;
        copy.structureBoss = structureBoss;
        copy.structureBossPresent = structureBossPresent;
        copy.structureBossChanged = structureBossChanged;
        copy.scheduled = scheduled;
        copy.scheduledPresent = scheduledPresent;
        copy.scheduledChanged = scheduledChanged;
        copy.position = position;
        copy.positionPresent = positionPresent;
        copy.positionChanged = positionChanged;
        copy.limit = limit;
        copy.limitPresent = limitPresent;
        copy.limitChanged = limitChanged;
        copy.approval = approval;
        copy.approvalPresent = approvalPresent;
        copy.approvalChanged = approvalChanged;
        copy.solo = solo;
        copy.soloPresent = soloPresent;
        copy.soloChanged = soloChanged;
        copy.bloodMoon = bloodMoon;
        copy.bloodMoonPresent = bloodMoonPresent;
        copy.bloodMoonChanged = bloodMoonChanged;
        copy.raid = raid;
        copy.raidPresent = raidPresent;
        copy.raidChanged = raidChanged;
        copy.nativePending = nativePending;
        copy.nativePendingPresent = nativePendingPresent;
        copy.nativePendingChanged = nativePendingChanged;
        copy.structureBounds = structureBounds;
        copy.structureBoundsChanged = structureBoundsChanged;
        copy.membersChanged = membersChanged;
        copy.invitationsChanged = invitationsChanged;
        members.forEach((id, member) -> copy.members.put(id, member.copy()));
        copy.invitations.putAll(invitations); copy.invitationsPresent = invitationsPresent; copy.invitationsCleared = invitationsCleared;
        copy.invitationChanges.addAll(invitationChanges);
        return copy;
    }
    public PartyState snapshot() { var copy = copy(); copy.frozen = true; copy.members.replaceAll((id, member) -> member.snapshot()); return copy; }

    /** Serialization boundary, nested solely for private state access. */
    static final class Codec {
    static PartyState decode(net.minecraft.nbt.CompoundTag tag) {
        var value = new PartyState();
        value.source = new PartyStateNbtCodec.Source(tag);
        value.maxZ = tag.getInt("maxZ"); value.maxZPresent = tag.contains("maxZ");
        value.maxY = tag.getInt("maxY"); value.maxYPresent = tag.contains("maxY");
        value.maxX = tag.getInt("maxX"); value.maxXPresent = tag.contains("maxX");
        value.minZ = tag.getInt("minZ"); value.minZPresent = tag.contains("minZ");
        value.minY = tag.getInt("minY"); value.minYPresent = tag.contains("minY");
        value.minX = tag.getInt("minX"); value.minXPresent = tag.contains("minX");
        value.radius = tag.getInt("radius"); value.radiusPresent = tag.contains("radius");
        value.raidArenaId = tag.getString("raidArenaId"); value.raidArenaIdPresent = tag.contains("raidArenaId");
        value.arenaId = tag.getString("arenaId"); value.arenaIdPresent = tag.contains("arenaId");
        value.ends = tag.getLong("ends"); value.endsPresent = tag.contains("ends");
            value.idPresent = tag.hasUUID("id");
            if (value.idPresent) value.id = tag.getUUID("id");
            value.ownerPresent = tag.hasUUID("owner");
            if (value.ownerPresent) value.owner = tag.getUUID("owner");
            value.bossEntityPresent = tag.hasUUID("bossEntity");
            if (value.bossEntityPresent) value.bossEntity = tag.getUUID("bossEntity");
            value.eventIdPresent = tag.hasUUID("eventId");
            if (value.eventIdPresent) value.eventId = tag.getUUID("eventId");
            value.structureIdPresent = tag.hasUUID("structureId");
            if (value.structureIdPresent) value.structureId = tag.getUUID("structureId");
            value.partyTeleportSessionPresent = tag.hasUUID("partyTeleportSession");
            if (value.partyTeleportSessionPresent) value.partyTeleportSession = tag.getUUID("partyTeleportSession");
            value.revisionPresent = tag.contains("revision");
            value.revision = tag.getInt("revision");
            value.createdPresent = tag.contains("created");
            value.created = tag.getLong("created");
            value.titlePresent = tag.contains("title");
            value.title = tag.getString("title");
            value.activityPresent = tag.contains("activity");
            value.activity = tag.getString("activity");
            value.descriptionPresent = tag.contains("description");
            value.description = tag.getString("description");
            value.languagePresent = tag.contains("language");
            value.language = tag.getString("language");
            value.namePresent = tag.contains("name");
            value.name = tag.getString("name");
            value.statePresent = tag.contains("state");
            value.state = tag.getString("state");
            value.dimensionPresent = tag.contains("dimension");
            value.dimension = tag.getString("dimension");
            value.bossTypePresent = tag.contains("bossType");
            value.bossType = tag.getString("bossType");
            value.structureBossPresent = tag.contains("structureBoss");
            value.structureBoss = tag.getString("structureBoss");
            value.scheduledPresent = tag.contains("scheduled");
            value.scheduled = tag.getLong("scheduled");
            value.positionPresent = tag.contains("position");
            value.position = tag.getLong("position");
            value.limitPresent = tag.contains("limit");
            value.limit = tag.getInt("limit");
            value.approvalPresent = tag.contains("approval");
            value.approval = tag.getBoolean("approval");
            value.soloPresent = tag.contains("solo");
            value.solo = tag.getBoolean("solo");
            value.bloodMoonPresent = tag.contains("bloodMoon");
            value.bloodMoon = tag.getBoolean("bloodMoon");
            value.raidPresent = tag.contains("raid");
            value.raid = tag.getBoolean("raid");
            value.nativePendingPresent = tag.contains("nativePending");
            value.nativePending = tag.getBoolean("nativePending");
        var members = tag.getCompound("members");
        for (String id : members.getAllKeys()) value.members.put(UUID.fromString(id), PartyMemberNbtCodec.decode(members.getCompound(id)));
        value.invitationsPresent = tag.contains("invitations");
        var invitations = tag.getCompound("invitations");
        for (String id : invitations.getAllKeys()) value.invitations.put(UUID.fromString(id), invitations.getLong(id));
        int[] bounds = tag.getIntArray("structureBounds");
        if (bounds.length == 6) value.structureBounds = new StructureBounds(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        return value;
    }
    static net.minecraft.nbt.CompoundTag encode(PartyState value) {
        var tag = value.source.copy();
        if (value.maxZChanged) { if (value.maxZPresent) tag.putInt("maxZ", value.maxZ); else tag.remove("maxZ"); }
        if (value.maxYChanged) { if (value.maxYPresent) tag.putInt("maxY", value.maxY); else tag.remove("maxY"); }
        if (value.maxXChanged) { if (value.maxXPresent) tag.putInt("maxX", value.maxX); else tag.remove("maxX"); }
        if (value.minZChanged) { if (value.minZPresent) tag.putInt("minZ", value.minZ); else tag.remove("minZ"); }
        if (value.minYChanged) { if (value.minYPresent) tag.putInt("minY", value.minY); else tag.remove("minY"); }
        if (value.minXChanged) { if (value.minXPresent) tag.putInt("minX", value.minX); else tag.remove("minX"); }
        if (value.radiusChanged) { if (value.radiusPresent) tag.putInt("radius", value.radius); else tag.remove("radius"); }
        if (value.raidArenaIdChanged) { if (value.raidArenaIdPresent) tag.putString("raidArenaId", value.raidArenaId); else tag.remove("raidArenaId"); }
        if (value.arenaIdChanged) { if (value.arenaIdPresent) tag.putString("arenaId", value.arenaId); else tag.remove("arenaId"); }
        if (value.endsChanged) { if (value.endsPresent) tag.putLong("ends", value.ends); else tag.remove("ends"); }
            if (value.idChanged) { if (value.idPresent) tag.putUUID("id", value.id); else tag.remove("id"); }
            if (value.ownerChanged) { if (value.ownerPresent) tag.putUUID("owner", value.owner); else tag.remove("owner"); }
            if (value.bossEntityChanged) { if (value.bossEntityPresent) tag.putUUID("bossEntity", value.bossEntity); else tag.remove("bossEntity"); }
            if (value.eventIdChanged) { if (value.eventIdPresent) tag.putUUID("eventId", value.eventId); else tag.remove("eventId"); }
            if (value.structureIdChanged) { if (value.structureIdPresent) tag.putUUID("structureId", value.structureId); else tag.remove("structureId"); }
            if (value.partyTeleportSessionChanged) { if (value.partyTeleportSessionPresent) tag.putUUID("partyTeleportSession", value.partyTeleportSession); else tag.remove("partyTeleportSession"); }
            if (value.revisionChanged) { if (value.revisionPresent) tag.putInt("revision", value.revision); else tag.remove("revision"); }
            if (value.createdChanged) { if (value.createdPresent) tag.putLong("created", value.created); else tag.remove("created"); }
            if (value.titleChanged) { if (value.titlePresent) tag.putString("title", value.title); else tag.remove("title"); }
            if (value.activityChanged) { if (value.activityPresent) tag.putString("activity", value.activity); else tag.remove("activity"); }
            if (value.descriptionChanged) { if (value.descriptionPresent) tag.putString("description", value.description); else tag.remove("description"); }
            if (value.languageChanged) { if (value.languagePresent) tag.putString("language", value.language); else tag.remove("language"); }
            if (value.nameChanged) { if (value.namePresent) tag.putString("name", value.name); else tag.remove("name"); }
            if (value.stateChanged) { if (value.statePresent) tag.putString("state", value.state); else tag.remove("state"); }
            if (value.dimensionChanged) { if (value.dimensionPresent) tag.putString("dimension", value.dimension); else tag.remove("dimension"); }
            if (value.bossTypeChanged) { if (value.bossTypePresent) tag.putString("bossType", value.bossType); else tag.remove("bossType"); }
            if (value.structureBossChanged) { if (value.structureBossPresent) tag.putString("structureBoss", value.structureBoss); else tag.remove("structureBoss"); }
            if (value.scheduledChanged) { if (value.scheduledPresent) tag.putLong("scheduled", value.scheduled); else tag.remove("scheduled"); }
            if (value.positionChanged) { if (value.positionPresent) tag.putLong("position", value.position); else tag.remove("position"); }
            if (value.limitChanged) { if (value.limitPresent) tag.putInt("limit", value.limit); else tag.remove("limit"); }
            if (value.approvalChanged) { if (value.approvalPresent) tag.putBoolean("approval", value.approval); else tag.remove("approval"); }
            if (value.soloChanged) { if (value.soloPresent) tag.putBoolean("solo", value.solo); else tag.remove("solo"); }
            if (value.bloodMoonChanged) { if (value.bloodMoonPresent) tag.putBoolean("bloodMoon", value.bloodMoon); else tag.remove("bloodMoon"); }
            if (value.raidChanged) { if (value.raidPresent) tag.putBoolean("raid", value.raid); else tag.remove("raid"); }
            if (value.nativePendingChanged) { if (value.nativePendingPresent) tag.putBoolean("nativePending", value.nativePending); else tag.remove("nativePending"); }
        var members = new net.minecraft.nbt.CompoundTag();
        value.members.forEach((id, member) -> members.put(id.toString(), PartyMemberNbtCodec.encode(member)));
        if (value.membersChanged || !members.equals(tag.getCompound("members"))) tag.put("members", members);
        if (value.invitationsChanged) {
            var invitations = value.invitationsCleared ? new net.minecraft.nbt.CompoundTag() : tag.getCompound("invitations");
            value.invitationChanges.forEach(id -> { if (value.invitations.containsKey(id)) invitations.putLong(id.toString(), value.invitations.get(id)); else invitations.remove(id.toString()); });
            if (!value.invitationsPresent) tag.remove("invitations"); else tag.put("invitations", invitations);
        }
        if (value.structureBoundsChanged) {
            var bounds = value.structureBounds;
            if (bounds == null) tag.remove("structureBounds");
            else tag.putIntArray("structureBounds", new int[]{bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
        }
        return tag;
    }
    }
}

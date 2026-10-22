package com.siirio.jemserver.smp;

import java.util.*;

/** Typed state; snapshots reject mutation, including nested values. */
public final class ProfileState implements SmpRecord {
    private boolean frozen;
    private ProfileStateNbtCodec.Source source = new ProfileStateNbtCodec.Source();
    private UUID id = null;
    private boolean idPresent, idChanged;
    private UUID owner = null;
    private boolean ownerPresent, ownerChanged;
    private int revision = 0;
    private boolean revisionPresent, revisionChanged;
    private long created = 0;
    private boolean createdPresent, createdChanged;
    private String state = "";
    private boolean statePresent, stateChanged;
    private long firstJoin = 0;
    private boolean firstJoinPresent, firstJoinChanged;
    private String name = "";
    private boolean namePresent, nameChanged;
    private int playTicks = 0;
    private boolean playTicksPresent, playTicksChanged;
    private int mobsKilled = 0;
    private boolean mobsKilledPresent, mobsKilledChanged;
    private String rewardAvailability = "";
    private boolean rewardAvailabilityPresent, rewardAvailabilityChanged;
    private long partiesHosted = 0;
    private boolean partiesHostedPresent, partiesHostedChanged;
    private long partiesJoined = 0;
    private boolean partiesJoinedPresent, partiesJoinedChanged;
    private long bossesHosted = 0;
    private boolean bossesHostedPresent, bossesHostedChanged;
    private long bossAssists = 0;
    private boolean bossAssistsPresent, bossAssistsChanged;
    private long raidsCompleted = 0;
    private boolean raidsCompletedPresent, raidsCompletedChanged;
    private long bloodMoonClears = 0;
    private boolean bloodMoonClearsPresent, bloodMoonClearsChanged;
    private long fishingEvents = 0;
    private boolean fishingEventsPresent, fishingEventsChanged;
    private long creaturesTamed = 0;
    private boolean creaturesTamedPresent, creaturesTamedChanged;
    private long landmarksCreated = 0;
    private boolean landmarksCreatedPresent, landmarksCreatedChanged;
    private long biomesExplored = 0;
    private boolean biomesExploredPresent, biomesExploredChanged;
    private long animalsTamed = 0;
    private boolean animalsTamedPresent, animalsTamedChanged;
    private long tameSpecies = 0;
    private boolean tameSpeciesPresent, tameSpeciesChanged;
    private long fishingMilestones = 0;
    private boolean fishingMilestonesPresent, fishingMilestonesChanged;
    private final Set<String> discoveredBiomes = new LinkedHashSet<>(), tamedSpecies = new LinkedHashSet<>();
    private final Set<UUID> favorites = new LinkedHashSet<>();
    private final Map<String, Long> eventStats = new LinkedHashMap<>();
    private List<ProfessionSnapshot> professions = List.of();
    private NativeItems deliveries = NativeItems.empty();
    private boolean deliveriesPresent;
    private final Set<UUID> favoriteChanges = new LinkedHashSet<>();
    private DeliveryClaim deliveryClaim;
    private RewardMigration rewardMigration;
    private boolean discoveriesChanged, speciesChanged, favoritesChanged, eventStatsChanged, professionsChanged, deliveriesChanged, deliveryClaimChanged, rewardMigrationChanged;
    public record ProfessionSnapshot(String id, long xp, int level) {}
    public record DeliveryClaim(UUID id, NativeItems remaining) {}
    public record RewardMigration(UUID id, NativeItems items, boolean committed) {}
    public Set<String> discoveredBiomes() { return Collections.unmodifiableSet(discoveredBiomes); }
    public boolean discoverBiome(String id) { requireMutable(); boolean added = discoveredBiomes.add(id); discoveriesChanged |= added; return added; }
    public Set<String> tamedSpecies() { return Collections.unmodifiableSet(tamedSpecies); }
    public void tameSpecies(String id) { requireMutable(); speciesChanged |= tamedSpecies.add(id); }
    public Set<UUID> favorites() { return Collections.unmodifiableSet(favorites); }
    public void favorite(UUID id, boolean value) { requireMutable(); if (value) favorites.add(id); else favorites.remove(id); favoritesChanged = true; favoriteChanges.add(id); }
    public Map<String, Long> eventStats() { return Collections.unmodifiableMap(eventStats); }
    public void countEvent(String activity) { requireMutable(); eventStats.merge(activity, 1L, Long::sum); eventStatsChanged = true; }
    public List<ProfessionSnapshot> professions() { return professions; }
    public void professions(List<ProfessionSnapshot> value) { requireMutable(); professions = List.copyOf(value); professionsChanged = true; }
    public NativeItems deliveries() { return deliveries; }
    public void deliveries(NativeItems value) { requireMutable(); deliveries = value; deliveriesPresent = true; deliveriesChanged = true; }
    public void clearDeliveries() { requireMutable(); deliveries = NativeItems.empty(); deliveriesPresent = false; deliveriesChanged = true; }
    public DeliveryClaim deliveryClaim() { return deliveryClaim; }
    public void deliveryClaim(DeliveryClaim value) { requireMutable(); deliveryClaim = value; deliveryClaimChanged = true; }
    public void clearDeliveryClaim() { deliveryClaim(null); }
    public RewardMigration rewardMigration() { return rewardMigration; }
    public void rewardMigration(RewardMigration value) { requireMutable(); rewardMigration = value; rewardMigrationChanged = true; }
    public void clearRewardMigration() { rewardMigration(null); }

    public UUID id() { return id; }
    public boolean hasId() { return idPresent; }
    public void id(UUID value) { requireMutable(); id = value; idPresent = true; idChanged = true; }
    public void clearId() { requireMutable(); id = null; idPresent = false; idChanged = true; }
    public UUID owner() { return owner; }
    public boolean hasOwner() { return ownerPresent; }
    public void owner(UUID value) { requireMutable(); owner = value; ownerPresent = true; ownerChanged = true; }
    public void clearOwner() { requireMutable(); owner = null; ownerPresent = false; ownerChanged = true; }
    public int revision() { return revision; }
    public boolean hasRevision() { return revisionPresent; }
    public void revision(int value) { requireMutable(); revision = value; revisionPresent = true; revisionChanged = true; }
    public void clearRevision() { requireMutable(); revision = 0; revisionPresent = false; revisionChanged = true; }
    public long created() { return created; }
    public boolean hasCreated() { return createdPresent; }
    public void created(long value) { requireMutable(); created = value; createdPresent = true; createdChanged = true; }
    public void clearCreated() { requireMutable(); created = 0; createdPresent = false; createdChanged = true; }
    public String state() { return state; }
    public boolean hasState() { return statePresent; }
    public void state(String value) { requireMutable(); state = value; statePresent = true; stateChanged = true; }
    public void clearState() { requireMutable(); state = ""; statePresent = false; stateChanged = true; }
    public long firstJoin() { return firstJoin; }
    public boolean hasFirstJoin() { return firstJoinPresent; }
    public void firstJoin(long value) { requireMutable(); firstJoin = value; firstJoinPresent = true; firstJoinChanged = true; }
    public void clearFirstJoin() { requireMutable(); firstJoin = 0; firstJoinPresent = false; firstJoinChanged = true; }
    public String name() { return name; }
    public boolean hasName() { return namePresent; }
    public void name(String value) { requireMutable(); name = value; namePresent = true; nameChanged = true; }
    public void clearName() { requireMutable(); name = ""; namePresent = false; nameChanged = true; }
    public int playTicks() { return playTicks; }
    public boolean hasPlayTicks() { return playTicksPresent; }
    public void playTicks(int value) { requireMutable(); playTicks = value; playTicksPresent = true; playTicksChanged = true; }
    public void clearPlayTicks() { requireMutable(); playTicks = 0; playTicksPresent = false; playTicksChanged = true; }
    public int mobsKilled() { return mobsKilled; }
    public boolean hasMobsKilled() { return mobsKilledPresent; }
    public void mobsKilled(int value) { requireMutable(); mobsKilled = value; mobsKilledPresent = true; mobsKilledChanged = true; }
    public void clearMobsKilled() { requireMutable(); mobsKilled = 0; mobsKilledPresent = false; mobsKilledChanged = true; }
    public String rewardAvailability() { return rewardAvailability; }
    public boolean hasRewardAvailability() { return rewardAvailabilityPresent; }
    public void rewardAvailability(String value) { requireMutable(); rewardAvailability = value; rewardAvailabilityPresent = true; rewardAvailabilityChanged = true; }
    public void clearRewardAvailability() { requireMutable(); rewardAvailability = ""; rewardAvailabilityPresent = false; rewardAvailabilityChanged = true; }
    public long partiesHosted() { return partiesHosted; }
    public boolean hasPartiesHosted() { return partiesHostedPresent; }
    public void partiesHosted(long value) { requireMutable(); partiesHosted = value; partiesHostedPresent = true; partiesHostedChanged = true; }
    public void clearPartiesHosted() { requireMutable(); partiesHosted = 0; partiesHostedPresent = false; partiesHostedChanged = true; }
    public long partiesJoined() { return partiesJoined; }
    public boolean hasPartiesJoined() { return partiesJoinedPresent; }
    public void partiesJoined(long value) { requireMutable(); partiesJoined = value; partiesJoinedPresent = true; partiesJoinedChanged = true; }
    public void clearPartiesJoined() { requireMutable(); partiesJoined = 0; partiesJoinedPresent = false; partiesJoinedChanged = true; }
    public long bossesHosted() { return bossesHosted; }
    public boolean hasBossesHosted() { return bossesHostedPresent; }
    public void bossesHosted(long value) { requireMutable(); bossesHosted = value; bossesHostedPresent = true; bossesHostedChanged = true; }
    public void clearBossesHosted() { requireMutable(); bossesHosted = 0; bossesHostedPresent = false; bossesHostedChanged = true; }
    public long bossAssists() { return bossAssists; }
    public boolean hasBossAssists() { return bossAssistsPresent; }
    public void bossAssists(long value) { requireMutable(); bossAssists = value; bossAssistsPresent = true; bossAssistsChanged = true; }
    public void clearBossAssists() { requireMutable(); bossAssists = 0; bossAssistsPresent = false; bossAssistsChanged = true; }
    public long raidsCompleted() { return raidsCompleted; }
    public boolean hasRaidsCompleted() { return raidsCompletedPresent; }
    public void raidsCompleted(long value) { requireMutable(); raidsCompleted = value; raidsCompletedPresent = true; raidsCompletedChanged = true; }
    public void clearRaidsCompleted() { requireMutable(); raidsCompleted = 0; raidsCompletedPresent = false; raidsCompletedChanged = true; }
    public long bloodMoonClears() { return bloodMoonClears; }
    public boolean hasBloodMoonClears() { return bloodMoonClearsPresent; }
    public void bloodMoonClears(long value) { requireMutable(); bloodMoonClears = value; bloodMoonClearsPresent = true; bloodMoonClearsChanged = true; }
    public void clearBloodMoonClears() { requireMutable(); bloodMoonClears = 0; bloodMoonClearsPresent = false; bloodMoonClearsChanged = true; }
    public long fishingEvents() { return fishingEvents; }
    public boolean hasFishingEvents() { return fishingEventsPresent; }
    public void fishingEvents(long value) { requireMutable(); fishingEvents = value; fishingEventsPresent = true; fishingEventsChanged = true; }
    public void clearFishingEvents() { requireMutable(); fishingEvents = 0; fishingEventsPresent = false; fishingEventsChanged = true; }
    public long creaturesTamed() { return creaturesTamed; }
    public boolean hasCreaturesTamed() { return creaturesTamedPresent; }
    public void creaturesTamed(long value) { requireMutable(); creaturesTamed = value; creaturesTamedPresent = true; creaturesTamedChanged = true; }
    public void clearCreaturesTamed() { requireMutable(); creaturesTamed = 0; creaturesTamedPresent = false; creaturesTamedChanged = true; }
    public long landmarksCreated() { return landmarksCreated; }
    public boolean hasLandmarksCreated() { return landmarksCreatedPresent; }
    public void landmarksCreated(long value) { requireMutable(); landmarksCreated = value; landmarksCreatedPresent = true; landmarksCreatedChanged = true; }
    public void clearLandmarksCreated() { requireMutable(); landmarksCreated = 0; landmarksCreatedPresent = false; landmarksCreatedChanged = true; }
    public long biomesExplored() { return biomesExplored; }
    public boolean hasBiomesExplored() { return biomesExploredPresent; }
    public void biomesExplored(long value) { requireMutable(); biomesExplored = value; biomesExploredPresent = true; biomesExploredChanged = true; }
    public void clearBiomesExplored() { requireMutable(); biomesExplored = 0; biomesExploredPresent = false; biomesExploredChanged = true; }
    public long animalsTamed() { return animalsTamed; }
    public boolean hasAnimalsTamed() { return animalsTamedPresent; }
    public void animalsTamed(long value) { requireMutable(); animalsTamed = value; animalsTamedPresent = true; animalsTamedChanged = true; }
    public void clearAnimalsTamed() { requireMutable(); animalsTamed = 0; animalsTamedPresent = false; animalsTamedChanged = true; }
    public long tameSpecies() { return tameSpecies; }
    public boolean hasTameSpecies() { return tameSpeciesPresent; }
    public void tameSpecies(long value) { requireMutable(); tameSpecies = value; tameSpeciesPresent = true; tameSpeciesChanged = true; }
    public void clearTameSpecies() { requireMutable(); tameSpecies = 0; tameSpeciesPresent = false; tameSpeciesChanged = true; }
    public long fishingMilestones() { return fishingMilestones; }
    public boolean hasFishingMilestones() { return fishingMilestonesPresent; }
    public void fishingMilestones(long value) { requireMutable(); fishingMilestones = value; fishingMilestonesPresent = true; fishingMilestonesChanged = true; }
    public void clearFishingMilestones() { requireMutable(); fishingMilestones = 0; fishingMilestonesPresent = false; fishingMilestonesChanged = true; }
    private void requireMutable() { if (frozen) throw new IllegalStateException("Cannot mutate an SMP snapshot"); }
    public ProfileState copy() {
        var copy = new ProfileState();
        copy.source = source;
        copy.id = id;
        copy.idPresent = idPresent;
        copy.idChanged = idChanged;
        copy.owner = owner;
        copy.ownerPresent = ownerPresent;
        copy.ownerChanged = ownerChanged;
        copy.revision = revision;
        copy.revisionPresent = revisionPresent;
        copy.revisionChanged = revisionChanged;
        copy.created = created;
        copy.createdPresent = createdPresent;
        copy.createdChanged = createdChanged;
        copy.state = state;
        copy.statePresent = statePresent;
        copy.stateChanged = stateChanged;
        copy.firstJoin = firstJoin;
        copy.firstJoinPresent = firstJoinPresent;
        copy.firstJoinChanged = firstJoinChanged;
        copy.name = name;
        copy.namePresent = namePresent;
        copy.nameChanged = nameChanged;
        copy.playTicks = playTicks;
        copy.playTicksPresent = playTicksPresent;
        copy.playTicksChanged = playTicksChanged;
        copy.mobsKilled = mobsKilled;
        copy.mobsKilledPresent = mobsKilledPresent;
        copy.mobsKilledChanged = mobsKilledChanged;
        copy.rewardAvailability = rewardAvailability;
        copy.rewardAvailabilityPresent = rewardAvailabilityPresent;
        copy.rewardAvailabilityChanged = rewardAvailabilityChanged;
        copy.partiesHosted = partiesHosted;
        copy.partiesHostedPresent = partiesHostedPresent;
        copy.partiesHostedChanged = partiesHostedChanged;
        copy.partiesJoined = partiesJoined;
        copy.partiesJoinedPresent = partiesJoinedPresent;
        copy.partiesJoinedChanged = partiesJoinedChanged;
        copy.bossesHosted = bossesHosted;
        copy.bossesHostedPresent = bossesHostedPresent;
        copy.bossesHostedChanged = bossesHostedChanged;
        copy.bossAssists = bossAssists;
        copy.bossAssistsPresent = bossAssistsPresent;
        copy.bossAssistsChanged = bossAssistsChanged;
        copy.raidsCompleted = raidsCompleted;
        copy.raidsCompletedPresent = raidsCompletedPresent;
        copy.raidsCompletedChanged = raidsCompletedChanged;
        copy.bloodMoonClears = bloodMoonClears;
        copy.bloodMoonClearsPresent = bloodMoonClearsPresent;
        copy.bloodMoonClearsChanged = bloodMoonClearsChanged;
        copy.fishingEvents = fishingEvents;
        copy.fishingEventsPresent = fishingEventsPresent;
        copy.fishingEventsChanged = fishingEventsChanged;
        copy.creaturesTamed = creaturesTamed;
        copy.creaturesTamedPresent = creaturesTamedPresent;
        copy.creaturesTamedChanged = creaturesTamedChanged;
        copy.landmarksCreated = landmarksCreated;
        copy.landmarksCreatedPresent = landmarksCreatedPresent;
        copy.landmarksCreatedChanged = landmarksCreatedChanged;
        copy.biomesExplored = biomesExplored;
        copy.biomesExploredPresent = biomesExploredPresent;
        copy.biomesExploredChanged = biomesExploredChanged;
        copy.animalsTamed = animalsTamed;
        copy.animalsTamedPresent = animalsTamedPresent;
        copy.animalsTamedChanged = animalsTamedChanged;
        copy.tameSpecies = tameSpecies;
        copy.tameSpeciesPresent = tameSpeciesPresent;
        copy.tameSpeciesChanged = tameSpeciesChanged;
        copy.fishingMilestones = fishingMilestones;
        copy.fishingMilestonesPresent = fishingMilestonesPresent;
        copy.fishingMilestonesChanged = fishingMilestonesChanged;
        copy.professions = professions;
        copy.deliveries = deliveries; copy.deliveriesPresent = deliveriesPresent;
        copy.favoriteChanges.addAll(favoriteChanges);
        copy.deliveryClaim = deliveryClaim;
        copy.rewardMigration = rewardMigration;
        copy.discoveriesChanged = discoveriesChanged;
        copy.speciesChanged = speciesChanged;
        copy.favoritesChanged = favoritesChanged;
        copy.eventStatsChanged = eventStatsChanged;
        copy.professionsChanged = professionsChanged;
        copy.deliveriesChanged = deliveriesChanged;
        copy.deliveryClaimChanged = deliveryClaimChanged;
        copy.rewardMigrationChanged = rewardMigrationChanged;
        copy.discoveredBiomes.addAll(discoveredBiomes); copy.tamedSpecies.addAll(tamedSpecies);
        copy.favorites.addAll(favorites); copy.eventStats.putAll(eventStats);
        return copy;
    }
    public ProfileState snapshot() { var copy = copy(); copy.frozen = true;  return copy; }

    /** Serialization boundary, nested solely for private state access. */
    static final class Codec {
    static ProfileState decode(net.minecraft.nbt.CompoundTag tag) {
        var value = new ProfileState();
        value.source = new ProfileStateNbtCodec.Source(tag);
            value.idPresent = tag.hasUUID("id");
            if (value.idPresent) value.id = tag.getUUID("id");
            value.ownerPresent = tag.hasUUID("owner");
            if (value.ownerPresent) value.owner = tag.getUUID("owner");
            value.revisionPresent = tag.contains("revision");
            value.revision = tag.getInt("revision");
            value.createdPresent = tag.contains("created");
            value.created = tag.getLong("created");
            value.statePresent = tag.contains("state");
            value.state = tag.getString("state");
            value.firstJoinPresent = tag.contains("firstJoin");
            value.firstJoin = tag.getLong("firstJoin");
            value.namePresent = tag.contains("name");
            value.name = tag.getString("name");
            value.playTicksPresent = tag.contains("playTicks");
            value.playTicks = tag.getInt("playTicks");
            value.mobsKilledPresent = tag.contains("mobsKilled");
            value.mobsKilled = tag.getInt("mobsKilled");
            value.rewardAvailabilityPresent = tag.contains("rewardAvailability");
            value.rewardAvailability = tag.getString("rewardAvailability");
            value.partiesHostedPresent = tag.contains("partiesHosted");
            value.partiesHosted = tag.getLong("partiesHosted");
            value.partiesJoinedPresent = tag.contains("partiesJoined");
            value.partiesJoined = tag.getLong("partiesJoined");
            value.bossesHostedPresent = tag.contains("bossesHosted");
            value.bossesHosted = tag.getLong("bossesHosted");
            value.bossAssistsPresent = tag.contains("bossAssists");
            value.bossAssists = tag.getLong("bossAssists");
            value.raidsCompletedPresent = tag.contains("raidsCompleted");
            value.raidsCompleted = tag.getLong("raidsCompleted");
            value.bloodMoonClearsPresent = tag.contains("bloodMoonClears");
            value.bloodMoonClears = tag.getLong("bloodMoonClears");
            value.fishingEventsPresent = tag.contains("fishingEvents");
            value.fishingEvents = tag.getLong("fishingEvents");
            value.creaturesTamedPresent = tag.contains("creaturesTamed");
            value.creaturesTamed = tag.getLong("creaturesTamed");
            value.landmarksCreatedPresent = tag.contains("landmarksCreated");
            value.landmarksCreated = tag.getLong("landmarksCreated");
            value.biomesExploredPresent = tag.contains("biomesExplored");
            value.biomesExplored = tag.getLong("biomesExplored");
            value.animalsTamedPresent = tag.contains("animalsTamed");
            value.animalsTamed = tag.getLong("animalsTamed");
            value.tameSpeciesPresent = tag.contains("tameSpecies");
            value.tameSpecies = tag.getLong("tameSpecies");
            value.fishingMilestonesPresent = tag.contains("fishingMilestones");
            value.fishingMilestones = tag.getLong("fishingMilestones");
        var biomes = tag.getCompound("discoveredBiomes");
        for (String id : biomes.getAllKeys()) if (biomes.getBoolean(id)) value.discoveredBiomes.add(id);
        var species = tag.getCompound("tamedSpecies");
        for (String id : species.getAllKeys()) if (species.getBoolean(id)) value.tamedSpecies.add(id);
        var favorites = tag.getCompound("shopFavorites");
        for (String id : favorites.getAllKeys()) if (favorites.getBoolean(id)) value.favorites.add(UUID.fromString(id));
        var stats = tag.getCompound("eventStats");
        for (String activity : stats.getAllKeys()) value.eventStats.put(activity, stats.getLong(activity));
        var professions = new ArrayList<ProfileState.ProfessionSnapshot>();
        for (net.minecraft.nbt.Tag entry : tag.getList("professions", net.minecraft.nbt.Tag.TAG_COMPOUND)) { var row = (net.minecraft.nbt.CompoundTag) entry; professions.add(new ProfileState.ProfessionSnapshot(row.getString("id"), row.getLong("xp"), row.getInt("level"))); }
        value.professions = List.copyOf(professions);
        value.deliveriesPresent = tag.contains("deliveries");
        value.deliveries = NativeItems.fromNbt(tag.getList("deliveries", net.minecraft.nbt.Tag.TAG_COMPOUND));
        if (tag.contains("deliveryClaim", net.minecraft.nbt.Tag.TAG_COMPOUND)) { var row = tag.getCompound("deliveryClaim"); value.deliveryClaim = new ProfileState.DeliveryClaim(row.getUUID("id"), NativeItems.fromNbt(row.getList("remaining", net.minecraft.nbt.Tag.TAG_COMPOUND))); }
        if (tag.contains("rewardMigration", net.minecraft.nbt.Tag.TAG_COMPOUND)) { var row = tag.getCompound("rewardMigration"); value.rewardMigration = new ProfileState.RewardMigration(row.getUUID("id"), NativeItems.fromNbt(row.getList("items", net.minecraft.nbt.Tag.TAG_COMPOUND)), row.getBoolean("committed")); }
        return value;
    }
    static net.minecraft.nbt.CompoundTag encode(ProfileState value) {
        var tag = value.source.copy();
            if (value.idChanged) { if (value.idPresent) tag.putUUID("id", value.id); else tag.remove("id"); }
            if (value.ownerChanged) { if (value.ownerPresent) tag.putUUID("owner", value.owner); else tag.remove("owner"); }
            if (value.revisionChanged) { if (value.revisionPresent) tag.putInt("revision", value.revision); else tag.remove("revision"); }
            if (value.createdChanged) { if (value.createdPresent) tag.putLong("created", value.created); else tag.remove("created"); }
            if (value.stateChanged) { if (value.statePresent) tag.putString("state", value.state); else tag.remove("state"); }
            if (value.firstJoinChanged) { if (value.firstJoinPresent) tag.putLong("firstJoin", value.firstJoin); else tag.remove("firstJoin"); }
            if (value.nameChanged) { if (value.namePresent) tag.putString("name", value.name); else tag.remove("name"); }
            if (value.playTicksChanged) { if (value.playTicksPresent) tag.putInt("playTicks", value.playTicks); else tag.remove("playTicks"); }
            if (value.mobsKilledChanged) { if (value.mobsKilledPresent) tag.putInt("mobsKilled", value.mobsKilled); else tag.remove("mobsKilled"); }
            if (value.rewardAvailabilityChanged) { if (value.rewardAvailabilityPresent) tag.putString("rewardAvailability", value.rewardAvailability); else tag.remove("rewardAvailability"); }
            if (value.partiesHostedChanged) { if (value.partiesHostedPresent) tag.putLong("partiesHosted", value.partiesHosted); else tag.remove("partiesHosted"); }
            if (value.partiesJoinedChanged) { if (value.partiesJoinedPresent) tag.putLong("partiesJoined", value.partiesJoined); else tag.remove("partiesJoined"); }
            if (value.bossesHostedChanged) { if (value.bossesHostedPresent) tag.putLong("bossesHosted", value.bossesHosted); else tag.remove("bossesHosted"); }
            if (value.bossAssistsChanged) { if (value.bossAssistsPresent) tag.putLong("bossAssists", value.bossAssists); else tag.remove("bossAssists"); }
            if (value.raidsCompletedChanged) { if (value.raidsCompletedPresent) tag.putLong("raidsCompleted", value.raidsCompleted); else tag.remove("raidsCompleted"); }
            if (value.bloodMoonClearsChanged) { if (value.bloodMoonClearsPresent) tag.putLong("bloodMoonClears", value.bloodMoonClears); else tag.remove("bloodMoonClears"); }
            if (value.fishingEventsChanged) { if (value.fishingEventsPresent) tag.putLong("fishingEvents", value.fishingEvents); else tag.remove("fishingEvents"); }
            if (value.creaturesTamedChanged) { if (value.creaturesTamedPresent) tag.putLong("creaturesTamed", value.creaturesTamed); else tag.remove("creaturesTamed"); }
            if (value.landmarksCreatedChanged) { if (value.landmarksCreatedPresent) tag.putLong("landmarksCreated", value.landmarksCreated); else tag.remove("landmarksCreated"); }
            if (value.biomesExploredChanged) { if (value.biomesExploredPresent) tag.putLong("biomesExplored", value.biomesExplored); else tag.remove("biomesExplored"); }
            if (value.animalsTamedChanged) { if (value.animalsTamedPresent) tag.putLong("animalsTamed", value.animalsTamed); else tag.remove("animalsTamed"); }
            if (value.tameSpeciesChanged) { if (value.tameSpeciesPresent) tag.putLong("tameSpecies", value.tameSpecies); else tag.remove("tameSpecies"); }
            if (value.fishingMilestonesChanged) { if (value.fishingMilestonesPresent) tag.putLong("fishingMilestones", value.fishingMilestones); else tag.remove("fishingMilestones"); }
        if (value.discoveriesChanged) { var rows = tag.getCompound("discoveredBiomes"); value.discoveredBiomes.forEach(id -> { if (!rows.getBoolean(id)) rows.putBoolean(id, true); }); tag.put("discoveredBiomes", rows); }
        if (value.speciesChanged) { var rows = tag.getCompound("tamedSpecies"); value.tamedSpecies.forEach(id -> { if (!rows.getBoolean(id)) rows.putBoolean(id, true); }); tag.put("tamedSpecies", rows); }
        if (value.favoritesChanged) { var rows = tag.getCompound("shopFavorites"); value.favoriteChanges.forEach(id -> { if (value.favorites.contains(id)) rows.putBoolean(id.toString(), true); else rows.remove(id.toString()); }); tag.put("shopFavorites", rows); }
        if (value.eventStatsChanged) { var rows = tag.getCompound("eventStats"); value.eventStats.forEach((key, count) -> { if (rows.getLong(key) != count || !rows.contains(key)) rows.putLong(key, count); }); tag.put("eventStats", rows); }
        if (value.professionsChanged) { var rows = new net.minecraft.nbt.ListTag(); for (var profession : value.professions) { var row = new net.minecraft.nbt.CompoundTag(); row.putString("id", profession.id()); row.putLong("xp", profession.xp()); row.putInt("level", profession.level()); rows.add(row); } tag.put("professions", rows); }
        if (value.deliveriesChanged) { if (!value.deliveriesPresent) tag.remove("deliveries"); else tag.put("deliveries", value.deliveries.toNbt()); }
        if (value.deliveryClaimChanged) { if (value.deliveryClaim == null) tag.remove("deliveryClaim"); else { var row = tag.getCompound("deliveryClaim"); if (!row.hasUUID("id") || !row.getUUID("id").equals(value.deliveryClaim.id())) row = new net.minecraft.nbt.CompoundTag(); row.putUUID("id", value.deliveryClaim.id()); row.put("remaining", value.deliveryClaim.remaining().toNbt()); tag.put("deliveryClaim", row); } }
        if (value.rewardMigrationChanged) { if (value.rewardMigration == null) tag.remove("rewardMigration"); else { var row = tag.getCompound("rewardMigration"); if (!row.hasUUID("id") || !row.getUUID("id").equals(value.rewardMigration.id())) row = new net.minecraft.nbt.CompoundTag(); row.putUUID("id", value.rewardMigration.id()); row.put("items", value.rewardMigration.items().toNbt()); if (value.rewardMigration.committed() || row.contains("committed")) row.putBoolean("committed", value.rewardMigration.committed()); tag.put("rewardMigration", row); } }
        return tag;
    }
    }
}

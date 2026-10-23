package com.siirio.jemcompat.feature.shelter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** One overworld journal owns stable companion identity, home assignment and pending native recovery data. */
public final class CompanionData extends SavedData {
    public static final String ENTITY_EPOCH = "JemShelterEpoch";
    public static final String FOLLOW_HOLD = "JemShelterOwnerTeleportHold";
    private final Map<UUID, Pet> pets = new LinkedHashMap<>();
    private static final Map<net.minecraft.server.MinecraftServer, CompanionData> SESSIONS = new java.util.WeakHashMap<>();

    public static final class Pet {
        public final UUID id;
        public UUID owner;
        public String species = "", name = "", dimension = "", homeDimension = "", blocked = "";
        public BlockPos lastPosition = BlockPos.ZERO, home;
        public long epoch;
        public boolean supported, restoring, provisional, custodian;
        public CompoundTag pending;
        public Pet(UUID id) { this.id = id; }
        public boolean assigned(String dimension, BlockPos anchor) { return home != null && home.equals(anchor) && homeDimension.equals(dimension); }
    }
    public static CompanionData get(ServerLevel level) {
        var server = level.getServer(); CompanionData existing = SESSIONS.get(server); if (existing != null) return existing;
        var path = path(level); CompanionData loaded;
        try {
            // DimensionDataStorage's generic reader catches corruption and returns null. A recovery journal must
            // never silently become a fresh empty index, so validate before attaching to the native save scheduler.
            loaded = java.nio.file.Files.exists(path) ? load(net.minecraft.nbt.NbtIo.readCompressed(path.toFile()).getCompound("data")) : new CompanionData();
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException("Cannot read JEM companion recovery journal", failure); }
        server.overworld().getDataStorage().set("jem_companions", loaded); SESSIONS.put(server, loaded); return loaded;
    }
    public Pet pet(UUID id) { return pets.get(id); }
    public Pet ensure(UUID id) { return pets.computeIfAbsent(id, Pet::new); }
    public List<Pet> owned(UUID owner) {
        return pets.values().stream().filter(p -> owner.equals(p.owner))
                .sorted(Comparator.comparing((Pet p) -> p.name).thenComparing(p -> p.species).thenComparing(p -> p.id)).toList();
    }
    public Pet occupant(String dimension, BlockPos home) {
        return pets.values().stream().filter(p -> p.assigned(dimension, home)).findFirst().orElse(null);
    }
    public List<Pet> pendingHomes(String dimension) {
        return pets.values().stream().filter(p -> p.pending != null && p.home != null && p.homeDimension.equals(dimension)).toList();
    }
    public void removeHome(String dimension, BlockPos home) {
        Pet pet = occupant(dimension, home);
        if (pet != null) { pet.home = null; pet.homeDimension = ""; pet.blocked = ""; setDirty(); }
    }
    public void flush(ServerLevel level) {
        setDirty(); save(path(level).toFile());
    }
    private static java.nio.file.Path path(ServerLevel level) {
        return level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").resolve("jem_companions.dat");
    }
    @Override public void save(java.io.File target) {
        if (!isDirty()) return;
        java.nio.file.Path temporary = null;
        try {
            var destination = target.toPath(); java.nio.file.Files.createDirectories(destination.getParent());
            CompoundTag wrapped = new CompoundTag(); wrapped.put("data", save(new CompoundTag()));
            net.minecraft.nbt.NbtUtils.addCurrentDataVersion(wrapped);
            temporary = java.nio.file.Files.createTempFile(destination.getParent(), "jem_companions.", ".tmp");
            net.minecraft.nbt.NbtIo.writeCompressed(wrapped, temporary.toFile());
            try (var channel = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) { channel.force(true); }
            if (!wrapped.equals(net.minecraft.nbt.NbtIo.readCompressed(temporary.toFile()))) throw new java.io.IOException("Recovery journal readback mismatch");
            java.nio.file.Files.move(temporary, destination, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            setDirty(false);
        } catch (java.io.IOException failure) {
            // Unlike native SavedData.save, do not swallow the error or clear dirty state and continue a death.
            throw new java.io.UncheckedIOException("Cannot commit JEM companion recovery journal", failure);
        } finally {
            if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); }
            catch (java.io.IOException cleanupFailure) { com.mojang.logging.LogUtils.getLogger().warn("Cannot remove temporary companion journal {}", temporary, cleanupFailure); }
        }
    }
    private static CompanionData load(CompoundTag root) {
        CompanionData data = new CompanionData();
        if (root.getInt("Version") != 1 && root.getInt("Version") != 2) throw new IllegalStateException("Unsupported JEM companion journal version");
        ListTag rows = root.getList("Pets", Tag.TAG_COMPOUND);
        for (Tag row : rows) {
            CompoundTag tag = (CompoundTag) row;
            if (!tag.hasUUID("Id") || !tag.hasUUID("Owner")) throw new IllegalStateException("Invalid JEM companion identity");
            Pet pet = new Pet(tag.getUUID("Id")); pet.owner = tag.getUUID("Owner");
            pet.species = tag.getString("Species"); pet.name = tag.getString("Name"); pet.dimension = tag.getString("Dimension");
            pet.lastPosition = BlockPos.of(tag.getLong("LastPosition")); pet.supported = tag.getBoolean("Supported");
            pet.provisional = tag.getBoolean("Provisional");
            pet.custodian = tag.getBoolean("Custodian");
            pet.epoch = tag.getLong("Epoch"); pet.blocked = tag.getString("Blocked"); pet.restoring = tag.getBoolean("Restoring");
            if (tag.contains("Home", Tag.TAG_LONG)) { pet.home = BlockPos.of(tag.getLong("Home")); pet.homeDimension = tag.getString("HomeDimension"); }
            if (tag.contains("Pending", Tag.TAG_COMPOUND)) pet.pending = tag.getCompound("Pending").copy();
            if (data.pets.put(pet.id, pet) != null) throw new IllegalStateException("Duplicate JEM companion identity");
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag root) {
        root.putInt("Version", 2); ListTag rows = new ListTag();
        for (Pet pet : new ArrayList<>(pets.values())) {
            if (pet.owner == null) continue;
            CompoundTag tag = new CompoundTag(); tag.putUUID("Id", pet.id); tag.putUUID("Owner", pet.owner);
            tag.putString("Species", pet.species); tag.putString("Name", pet.name); tag.putString("Dimension", pet.dimension);
            tag.putLong("LastPosition", pet.lastPosition.asLong()); tag.putBoolean("Supported", pet.supported);
            tag.putBoolean("Provisional", pet.provisional);
            tag.putBoolean("Custodian", pet.custodian);
            tag.putLong("Epoch", pet.epoch); tag.putString("Blocked", pet.blocked); tag.putBoolean("Restoring", pet.restoring);
            if (pet.home != null) { tag.putLong("Home", pet.home.asLong()); tag.putString("HomeDimension", pet.homeDimension); }
            if (pet.pending != null) tag.put("Pending", pet.pending.copy()); rows.add(tag);
        }
        root.put("Pets", rows); return root;
    }
}

package com.siirio.jemserver.migration.backpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stages offline Anvil changes without opening any original file for writing. */
public final class LegacyBackpackRegions {
    private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final Set<String> SHELVES = Set.of("oak", "spruce", "birch", "jungle",
            "dark_oak", "acacia", "crimson", "warped", "cherry");
    private static final Set<String> DIRECTIONS = Set.of("north", "south", "east", "west", "up", "down");

    private LegacyBackpackRegions() {}

    public record StagedFile(Path original, Path staged) {}

    public static List<StagedFile> stage(Path sourceRegion, Path workDirectory, String sourceKey,
                                        LegacyBackpackConverter.Result result) throws IOException {
        requireRegularSource(sourceRegion);
        Matcher name = REGION_NAME.matcher(sourceRegion.getFileName().toString());
        if (!name.matches()) throw new IOException("Invalid Anvil region name: " + sourceRegion);
        int regionX;
        int regionZ;
        try {
            regionX = Integer.parseInt(name.group(1));
            regionZ = Integer.parseInt(name.group(2));
            Math.multiplyExact(regionX, 32);
            Math.multiplyExact(regionZ, 32);
        } catch (ArithmeticException | NumberFormatException invalid) {
            throw new IOException("Invalid region coordinates: " + sourceRegion, invalid);
        }
        Path stagedDirectory = null;
        RegionFile output = null;
        try (FileChannel input = FileChannel.open(sourceRegion, StandardOpenOption.READ)) {
            if (input.size() == 0) return List.of();
            if (input.size() < 8192) throw new IOException("Truncated region header: " + sourceRegion);
            ByteBuffer header = ByteBuffer.allocate(8192);
            readFully(input, header, 0);
            validateAllocations(header, input.size(), sourceRegion);
            for (int slot = 0; slot < 1024; slot++) {
                int location = header.getInt(slot * 4);
                if (location == 0) continue;
                ChunkPos pos = new ChunkPos(regionX * 32 + (slot & 31), regionZ * 32 + (slot >> 5));
                CompoundTag original = readChunk(input, location, sourceRegion.getParent(), pos);
                CompoundTag changed = original.copy();
                String key = sourceKey + "/chunk[" + pos.x + "," + pos.z + "]";
                convertHolders(changed, key);
                convertPalettes(changed, key);
                verifyHolders(original, changed, pos, key);
                LegacyBackpackConverter.convertTree(changed, key, result);
                if (original.equals(changed)) continue;
                if (output == null) {
                    Files.createDirectories(workDirectory);
                    stagedDirectory = Files.createTempDirectory(workDirectory, "region-");
                    Path stagedRegion = stagedDirectory.resolve(sourceRegion.getFileName());
                    Files.copy(sourceRegion, stagedRegion, StandardCopyOption.COPY_ATTRIBUTES);
                    copyExternalFiles(sourceRegion.getParent(), stagedDirectory, regionX, regionZ);
                    output = new RegionFile(stagedRegion, stagedDirectory, true);
                }
                try (DataOutputStream chunkOutput = output.getChunkDataOutputStream(pos)) {
                    NbtIo.write(changed, chunkOutput);
                }
                try (DataInputStream written = output.getChunkDataInputStream(pos)) {
                    if (written == null || !changed.equals(NbtIo.read(written)))
                        throw new IOException("Staged region chunk did not round-trip at " + key);
                }
            }
        } finally {
            if (output != null) output.close();
        }
        if (stagedDirectory == null) return List.of();
        List<StagedFile> files = new ArrayList<>();
        files.add(new StagedFile(sourceRegion, stagedDirectory.resolve(sourceRegion.getFileName())));
        try (var paths = Files.list(stagedDirectory)) {
            for (Path staged : paths.filter(path -> path.getFileName().toString().endsWith(".mcc")).sorted().toList()) {
                Path original = sourceRegion.getParent().resolve(staged.getFileName());
                if (!Files.exists(original) || Files.mismatch(original, staged) != -1) {
                    files.add(new StagedFile(original, staged));
                }
            }
        }
        return files;
    }

    private static void validateAllocations(ByteBuffer header, long size, Path source) throws IOException {
        for (int slot = 0; slot < 1024; slot++) {
            int location = header.getInt(slot * 4);
            if (location == 0) continue;
            int start = location >>> 8;
            int count = location & 255;
            // Native writers can leave the final sector without its unused zero padding.
            if (start < 2 || count == 0 || start + (long) count > (size + 4095) / 4096) {
                throw new IOException("Invalid chunk allocation in " + source + " at slot " + slot);
            }
            for (int previous = 0; previous < slot; previous++) {
                int other = header.getInt(previous * 4);
                if (other == 0) continue;
                int otherStart = other >>> 8;
                if (start < otherStart + (other & 255) && otherStart < start + count) {
                    throw new IOException("Overlapping chunk allocations in " + source);
                }
            }
        }
    }

    private static CompoundTag readChunk(FileChannel input, int location, Path directory, ChunkPos pos)
            throws IOException {
        int sector = location >>> 8;
        int sectors = location & 255;
        long offset = (long) sector * 4096;
        if (sector < 2 || sectors == 0 || sector + (long) sectors > (input.size() + 4095) / 4096) {
            throw new IOException("Invalid region allocation for " + pos + " in " + directory);
        }
        ByteBuffer prefix = ByteBuffer.allocate(5);
        readFully(input, prefix, offset);
        int length = prefix.getInt(0);
        int compression = prefix.get(4) & 255;
        RegionFileVersion version = RegionFileVersion.fromId(compression & 127);
        if (version == null || length < 1 || length > sectors * 4096 - 4
                || offset + 4L + length > input.size()) {
            throw new IOException("Invalid region chunk encoding for " + pos + " in " + directory);
        }
        InputStream compressed;
        if ((compression & 128) != 0) {
            if (length != 1) throw new IOException("Invalid external chunk stub for " + pos);
            Path external = externalPath(directory, pos);
            requireRegularSource(external);
            compressed = Files.newInputStream(external);
        } else {
            ByteBuffer bytes = ByteBuffer.allocate(length - 1);
            readFully(input, bytes, offset + 5);
            compressed = new ByteArrayInputStream(bytes.array());
        }
        try (InputStream raw = compressed; DataInputStream decoded = new DataInputStream(version.wrap(raw))) {
            CompoundTag chunk = NbtIo.read(decoded);
            if (chunk == null) throw new IOException("Empty region chunk NBT for " + pos);
            return chunk;
        }
    }

    private static void readFully(FileChannel input, ByteBuffer bytes, long offset) throws IOException {
        while (bytes.hasRemaining()) {
            int read = input.read(bytes, offset + bytes.position());
            if (read < 0) throw new IOException("Unexpected end of Anvil region");
            if (read == 0) throw new IOException("Unable to read Anvil region");
        }
    }

    private static Path externalPath(Path directory, ChunkPos pos) {
        return directory.resolve("c." + pos.x + "." + pos.z + ".mcc");
    }

    private static void copyExternalFiles(Path source, Path staged, int regionX, int regionZ) throws IOException {
        for (int slot = 0; slot < 1024; slot++) {
            Path path = externalPath(source, new ChunkPos(regionX * 32 + (slot & 31), regionZ * 32 + (slot >> 5)));
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                requireRegularSource(path);
                Files.copy(path, staged.resolve(path.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    private static void requireRegularSource(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IOException("Expected a regular migration source file: " + path);
        }
    }

    private static ListTag compounds(CompoundTag parent, String field, String key) throws IOException {
        if (!parent.contains(field)) return new ListTag();
        Tag value = parent.get(field);
        if (!(value instanceof ListTag list) || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IOException("Unsupported " + field + " list at " + key);
        }
        return list;
    }

    private static void convertHolders(CompoundTag chunk, String key) throws IOException {
        ListTag entities = compounds(chunk, "block_entities", key);
        for (int index = 0; index < entities.size(); index++) {
            CompoundTag holder = entities.getCompound(index);
            String id = holder.getString("id");
            if (!id.startsWith("backpacked:")) continue;
            if (!id.equals("backpacked:shelf") && !id.equals("backpacked:backpack_dock")) {
                throw new IOException("Unknown Backpacked block entity " + id + " at " + key);
            }
            if (holder.contains("Backpack") && !holder.contains("Backpack", Tag.TAG_COMPOUND)) {
                throw new IOException("Invalid holder Backpack at " + key);
            }
            CompoundTag backpack = holder.getCompound("Backpack").copy();
            if (backpack.getString("id").equals("minecraft:air")
                    && backpack.contains("Count", Tag.TAG_ANY_NUMERIC) && backpack.getInt("Count") == 0) {
                if (backpack.contains("tag") || backpack.contains("ForgeCaps")) {
                    throw new IOException("Empty holder backpack has unexpected metadata at " + key);
                }
                backpack = new CompoundTag();
            }
            if (id.equals("backpacked:shelf")) {
                if (holder.contains("Container") && !holder.contains("Container", Tag.TAG_COMPOUND)) {
                    throw new IOException("Invalid shelf Container at " + key);
                }
                ListTag copies = compounds(holder.getCompound("Container"), "Items", key);
                if (copies.size() > 1 || (!copies.isEmpty() && (copies.getCompound(0).getByte("Slot") & 255) != 0)) {
                    throw new IOException("Unexpected extra shelf slots at " + key);
                }
                if (backpack.isEmpty() && !copies.isEmpty() && copies.getCompound(0).getInt("Count") > 0) {
                    throw new IOException("Shelf cached backpack has no canonical root backpack at " + key);
                }
                // The old loader overwrites Container slot zero with the canonical root Backpack.
                if (holder.contains("Items")) {
                    ListTag items = compounds(holder, "Items", key);
                    if (!items.isEmpty() && backpack.isEmpty()) throw new IOException("Shelf contents without a backpack at " + key);
                    if (!backpack.isEmpty()) {
                        if (backpack.contains("tag") && !backpack.contains("tag", Tag.TAG_COMPOUND)) {
                            throw new IOException("Invalid shelf backpack tag at " + key);
                        }
                        CompoundTag tag = backpack.getCompound("tag");
                        tag.put("Items", items.copy());
                        backpack.put("tag", tag);
                    }
                }
            }
            CompoundTag barrel = new CompoundTag();
            barrel.putString("id", "minecraft:barrel");
            for (String coordinate : List.of("x", "y", "z")) {
                if (!holder.contains(coordinate, Tag.TAG_INT)) throw new IOException("Missing holder coordinate at " + key);
                barrel.putInt(coordinate, holder.getInt(coordinate));
            }
            for (String field : List.of("CustomName", "Lock")) {
                if (holder.contains(field, Tag.TAG_STRING)) barrel.put(field, holder.get(field).copy());
            }
            ListTag items = new ListTag();
            if (!backpack.isEmpty()) {
                if (!backpack.getString("id").equals("backpacked:backpack")) {
                    throw new IOException("Unexpected item in Backpacked holder at " + key);
                }
                if (!backpack.contains("Count", Tag.TAG_ANY_NUMERIC) || backpack.getInt("Count") != 1) {
                    throw new IOException("Invalid backpack count in holder at " + key);
                }
                backpack.putByte("Slot", (byte) 0);
                items.add(backpack);
            }
            barrel.put("Items", items);
            entities.set(index, barrel);
        }
    }

    private static void convertPalettes(CompoundTag chunk, String key) throws IOException {
        ListTag sections = compounds(chunk, "sections", key);
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            CompoundTag section = sections.getCompound(sectionIndex);
            if (!section.contains("block_states")) continue;
            if (!section.contains("block_states", Tag.TAG_COMPOUND)) throw new IOException("Invalid block_states at " + key);
            CompoundTag states = section.getCompound("block_states");
            ListTag palette = compounds(states, "palette", key);
            boolean legacy = false;
            for (int index = 0; index < palette.size(); index++) {
                if (palette.getCompound(index).getString("Name").startsWith("backpacked:")) legacy = true;
            }
            if (!legacy) continue;
            if (palette.isEmpty() || palette.size() > 4096) throw new IOException("Unsupported block palette at " + key);
            Map<CompoundTag, Integer> unique = new LinkedHashMap<>();
            int[] remap = new int[palette.size()];
            ListTag replacement = new ListTag();
            for (int index = 0; index < palette.size(); index++) {
                CompoundTag state = palette.getCompound(index).copy();
                String id = state.getString("Name");
                if (id.startsWith("backpacked:")) {
                    String local = id.substring("backpacked:".length());
                    boolean shelf = local.endsWith("_backpack_shelf")
                            && SHELVES.contains(local.substring(0, local.length() - "_backpack_shelf".length()));
                    if (!local.equals("backpack_dock") && !shelf) throw new IOException("Unknown Backpacked block " + id + " at " + key);
                    String facing = state.getCompound("Properties").getString("facing");
                    state = new CompoundTag();
                    state.putString("Name", "minecraft:barrel");
                    CompoundTag properties = new CompoundTag();
                    properties.putString("facing", DIRECTIONS.contains(facing) ? facing : "north");
                    properties.putString("open", "false");
                    state.put("Properties", properties);
                }
                Integer mapped = unique.get(state);
                if (mapped == null) {
                    mapped = unique.size();
                    unique.put(state, mapped);
                    replacement.add(state);
                }
                remap[index] = mapped;
            }
            int oldBits = bits(palette.size());
            int oldPerLong = 64 / oldBits;
            long[] oldData = states.getLongArray("data");
            if (palette.size() == 1) {
                if (states.contains("data") && (!states.contains("data", Tag.TAG_LONG_ARRAY) || oldData.length != 0)) {
                    throw new IOException("Unexpected singleton palette data at " + key);
                }
            } else if (!states.contains("data", Tag.TAG_LONG_ARRAY) || oldData.length != (4096 + oldPerLong - 1) / oldPerLong) {
                throw new IOException("Unsupported packed block data at " + key);
            }
            int newBits = bits(replacement.size());
            int newPerLong = 64 / newBits;
            long[] newData = replacement.size() == 1 ? new long[0] : new long[(4096 + newPerLong - 1) / newPerLong];
            long mask = (1L << oldBits) - 1;
            for (int block = 0; block < 4096; block++) {
                int oldIndex = palette.size() == 1 ? 0 : (int) ((oldData[block / oldPerLong] >>> ((block % oldPerLong) * oldBits)) & mask);
                if (oldIndex >= remap.length) throw new IOException("Out-of-range palette index at " + key);
                if (newData.length != 0) newData[block / newPerLong] |= (long) remap[oldIndex] << ((block % newPerLong) * newBits);
            }
            states.put("palette", replacement);
            if (newData.length == 0) states.remove("data");
            else states.putLongArray("data", newData);
        }
    }

    private static void verifyHolders(CompoundTag original, CompoundTag changed, ChunkPos pos, String key)
            throws IOException {
        Set<String> occupied = new HashSet<>();
        for (Tag entry : compounds(original, "block_entities", key)) {
            CompoundTag holder = (CompoundTag) entry;
            String id = holder.getString("id");
            if (!id.equals("backpacked:shelf") && !id.equals("backpacked:backpack_dock")) continue;
            int x = holder.getInt("x");
            int y = holder.getInt("y");
            int z = holder.getInt("z");
            if (Math.floorDiv(x, 16) != pos.x || Math.floorDiv(z, 16) != pos.z) {
                throw new IOException("Backpacked holder belongs to a different chunk at " + key);
            }
            if (!occupied.add(x + "," + y + "," + z)) {
                throw new IOException("Duplicate Backpacked holder position at " + key);
            }
            CompoundTag section = null;
            int sectionY = Math.floorDiv(y, 16);
            for (Tag sectionEntry : compounds(changed, "sections", key)) {
                CompoundTag candidate = (CompoundTag) sectionEntry;
                if (!candidate.contains("Y", Tag.TAG_ANY_NUMERIC)) {
                    throw new IOException("Missing numeric chunk section Y at " + key);
                }
                if (candidate.getInt("Y") == sectionY) {
                    if (section != null) throw new IOException("Duplicate holder section at " + key);
                    section = candidate;
                }
            }
            if (section == null || !section.contains("block_states", Tag.TAG_COMPOUND)) {
                throw new IOException("Backpacked holder has no block-state section at " + key);
            }
            CompoundTag states = section.getCompound("block_states");
            ListTag palette = compounds(states, "palette", key);
            if (palette.isEmpty() || palette.size() > 4096) {
                throw new IOException("Unsupported holder block palette at " + key);
            }
            int block = Math.floorMod(y, 16) * 256 + Math.floorMod(z, 16) * 16 + Math.floorMod(x, 16);
            int index = 0;
            if (palette.size() > 1) {
                int width = bits(palette.size());
                int perLong = 64 / width;
                long[] data = states.getLongArray("data");
                if (!states.contains("data", Tag.TAG_LONG_ARRAY) || data.length != (4096 + perLong - 1) / perLong) {
                    throw new IOException("Unsupported holder block-state data at " + key);
                }
                index = (int) ((data[block / perLong] >>> ((block % perLong) * width)) & ((1L << width) - 1));
            }
            if (index >= palette.size() || !palette.getCompound(index).getString("Name").equals("minecraft:barrel")) {
                throw new IOException("Converted Backpacked holder does not occupy a barrel block at " + key);
            }
        }
    }

    private static int bits(int count) {
        return Math.max(4, 32 - Integer.numberOfLeadingZeros(count - 1));
    }
}

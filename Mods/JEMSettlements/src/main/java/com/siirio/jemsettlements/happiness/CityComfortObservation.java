package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

/** Resumable loaded-only geometry observation; intermediate evidence is private to the job. */
final class CityComfortObservation {
    private static final int GRAPH_LIMIT = 32768, ROOM_LIMIT = 4096, SAMPLE_LIMIT = 128;
    private static final Yield YIELD = new Yield();
    private static final class Yield extends RuntimeException {
        Yield() { super(null, null, false, false); }
    }
    final TownRecord town;
    final String signature;
    private final List<BlockPos> beds;
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Map<BlockPos, Double> standing = new HashMap<>();
    private final Map<BlockPos, Boolean> roofs = new HashMap<>();
    private final Map<BlockPos, String> plants = new HashMap<>();
    private final Set<BlockPos> chunks = new HashSet<>();
    private final Map<String, Integer> sourceCounts = new HashMap<>();
    private final ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
    private final Map<BlockPos, BlockPos> streets = new LinkedHashMap<>();
    private final Set<BlockPos> room = new HashSet<>(), entrances = new LinkedHashSet<>(), groupedBeds = new HashSet<>();
    private final List<Set<BlockPos>> homes = new ArrayList<>();
    private final Map<BlockPos, String> sources = new LinkedHashMap<>();
    private final Set<String> greeneryKinds = new HashSet<>(), sampleKinds = new HashSet<>();
    private List<BlockPos> samples = List.of();
    private int phase, bedIndex, sampleIndex, gardenIndex, lit, green, connected, reads, operations;
    private boolean initialized;
    boolean done, unavailable;

    CityComfortObservation(SettlementSavedData data, TownRecord town) {
        this.town = town; beds = beds(data, town); signature = signature(data, town);
    }

    private static List<BlockPos> beds(SettlementSavedData data, TownRecord town) {
        return data.residents(town.id()).stream().filter(resident -> resident.living() && resident.home().isPresent())
                .map(resident -> resident.home().get()).filter(home -> home.dimension().location().equals(town.dimension()) && town.contains(home.pos()))
                .map(home -> home.pos().immutable()).distinct().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
    }

    static String signature(SettlementSavedData data, TownRecord town) {
        return town.territoryMinX() + ":" + town.territoryMaxX() + ":" + town.territoryMinZ() + ":" + town.territoryMaxZ() + ":" + beds(data, town);
    }

    int advance(ServerLevel level, int budget) {
        reads = budget; operations = 256;
        try {
            if (beds.size() > 256) abort();
            while (!done) {
                if (operations-- <= 0) throw YIELD;
                if (phase == 0) observeHomes(level);
                else if (phase == 1) observeStreets(level);
                else if (phase == 2) prepareSamples(level);
                else observeSample(level);
            }
        } catch (Yield ignored) { }
        return Math.max(1, budget - reads);
    }

    private void abort() { unavailable = true; done = true; throw YIELD; }

    private BlockState state(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return Blocks.AIR.defaultBlockState();
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) abort();
        chunks.add(new BlockPos(pos.getX() >> 4, 0, pos.getZ() >> 4));
        BlockState cached = blocks.get(pos); if (cached != null) return cached;
        if (reads <= 0) throw YIELD;
        if (blocks.size() >= 524288) abort();
        reads--; var value = level.getBlockState(pos); blocks.put(pos.immutable(), value); return value;
    }

    /** Support heights and swept body clearance include slabs/stairs and operable doors. */
    private double feet(ServerLevel level, BlockPos pos) {
        Double prior = standing.get(pos); if (prior != null) return prior;
        var below = state(level, pos.below()); var shape = below.getCollisionShape(level, pos.below());
        double offset = shape.isEmpty() ? -2 : shape.max(Direction.Axis.Y) - 1;
        if (offset < -.5 || !below.getFluidState().isEmpty()) { standing.put(pos, Double.NaN); return Double.NaN; }
        var body = new AABB(pos.getX() + .2, pos.getY() + offset + .01, pos.getZ() + .2,
                pos.getX() + .8, pos.getY() + offset + 1.95, pos.getZ() + .8);
        double value = clear(level, body) ? pos.getY() + offset : Double.NaN;
        standing.put(pos.immutable(), value); return value;
    }

    private boolean clear(ServerLevel level, AABB body) {
        for (BlockPos at : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY, body.minZ), BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
            var block = state(level, at);
            if (block.getBlock() instanceof DoorBlock && (!block.is(Blocks.IRON_DOOR) || block.getValue(DoorBlock.OPEN))) continue;
            if (!block.getFluidState().isEmpty()) return false;
            for (AABB shape : block.getCollisionShape(level, at).toAabbs())
                if (body.intersects(shape.move(at.getX(), at.getY(), at.getZ()))) return false;
        }
        return true;
    }

    private boolean edge(ServerLevel level, BlockPos from, BlockPos to) {
        double a = feet(level, from), b = feet(level, to);
        if (Double.isNaN(a) || Double.isNaN(b) || Math.abs(a - b) > 1.25) return false;
        double bottom = Math.max(a, b);
        return clear(level, new AABB(Math.min(from.getX(), to.getX()) + .2, bottom + .01,
                Math.min(from.getZ(), to.getZ()) + .2, Math.max(from.getX(), to.getX()) + .8,
                bottom + 1.95, Math.max(from.getZ(), to.getZ()) + .8));
    }

    private List<BlockPos> neighbours(BlockPos pos) {
        var result = new ArrayList<BlockPos>(12);
        for (Direction direction : Direction.Plane.HORIZONTAL)
            for (int dy : new int[] {0, -1, 1}) {
                BlockPos next = pos.relative(direction).offset(0, dy, 0);
                if (town.contains(next)) result.add(next);
            }
        return result;
    }

    private boolean paving(ServerLevel level, BlockPos pos) {
        var state = state(level, pos.below()); Block block = state.getBlock();
        var key = ForgeRegistries.BLOCKS.getKey(block); String path = key == null ? "" : key.getPath();
        return block instanceof DirtPathBlock || block instanceof SlabBlock || block instanceof StairBlock
                || state.is(Blocks.GRAVEL) || state.is(BlockTags.PLANKS) || state.is(BlockTags.STONE_BRICKS)
                || state.is(Blocks.BRICKS)
                || path.endsWith("_bricks") || path.endsWith("_tiles") || path.startsWith("polished_")
                || state.is(Blocks.STONE) || state.is(Blocks.SMOOTH_STONE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    private boolean covered(ServerLevel level, BlockPos pos) {
        Boolean cached = roofs.get(pos); if (cached != null) return cached;
        for (int dy = 2; dy <= 24; dy++) {
            BlockPos at = pos.above(dy); var block = state(level, at);
            if (!block.is(BlockTags.LEAVES) && !block.getCollisionShape(level, at).isEmpty()) { roofs.put(pos, true); return true; }
        }
        roofs.put(pos.immutable(), false); return false;
    }

    private void observeHomes(ServerLevel level) {
        if (bedIndex >= beds.size()) {
            if (homes.isEmpty()) { done = true; return; }
            phase = 1; initialized = false; frontier.clear(); return;
        }
        BlockPos bed = beds.get(bedIndex);
        if (groupedBeds.contains(bed)) { bedIndex++; return; }
        if (!initialized) {
            BlockState bedState = state(level, bed);
            if (!(bedState.getBlock() instanceof BedBlock)) { groupedBeds.add(bed); bedIndex++; return; }
            Direction facing = bedState.getValue(BedBlock.FACING);
            BlockPos other = bed.relative(bedState.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD ? facing.getOpposite() : facing);
            for (BlockPos half : List.of(bed, other)) for (BlockPos seed : neighbours(half)) {
                if (!Double.isNaN(feet(level, seed)) && Math.abs(feet(level, seed) - bed.getY()) <= 1.25
                        && room.add(seed)) frontier.addLast(seed);
            }
            initialized = true;
        }
        if (frontier.isEmpty()) {
            groupedBeds.add(bed);
            for (BlockPos candidate : beds) for (Direction direction : Direction.Plane.HORIZONTAL)
                if (room.contains(candidate.relative(direction))) { groupedBeds.add(candidate); break; }
            var house = Set.copyOf(entrances); if (house.isEmpty() || !homes.contains(house)) homes.add(house);
            room.clear(); entrances.clear(); initialized = false; bedIndex++; return;
        }
        BlockPos current = frontier.peekFirst();
        if (!covered(level, current)) {
            // Traverse internal doors; a house ends at its reachable outdoor transition.
            entrances.add(current);
        } else {
            for (BlockPos next : neighbours(current)) {
                if (room.contains(next) || !edge(level, current, next)) continue;
                if (Math.abs(next.getX() - bed.getX()) > 32 || Math.abs(next.getZ() - bed.getZ()) > 32
                        || Math.abs(next.getY() - bed.getY()) > 24 || room.size() >= ROOM_LIMIT) abort();
                room.add(next); frontier.addLast(next);
            }
        }
        frontier.removeFirst();
    }

    private void observeStreets(ServerLevel level) {
        if (!initialized) {
            // The bell's immediate accessible surroundings seed continuous paved routes.
            for (BlockPos pos : neighbours(town.center())) if (paving(level, pos) && !Double.isNaN(feet(level, pos)) && !streets.containsKey(pos)) {
                streets.put(pos, pos); frontier.addLast(pos);
            }
            initialized = true;
        }
        if (frontier.isEmpty()) { phase = 2; return; }
        BlockPos current = frontier.peekFirst();
        for (BlockPos next : neighbours(current)) {
            if (streets.containsKey(next) || !paving(level, next) || !edge(level, current, next)) continue;
            if (streets.size() >= GRAPH_LIMIT) abort();
            streets.put(next, current); frontier.addLast(next);
        }
        frontier.removeFirst();
    }

    private void prepareSamples(ServerLevel level) {
        // This phase is retriable: all counters are committed only after its reads finish.
        int count = 0; var points = new LinkedHashSet<BlockPos>();
        for (Set<BlockPos> home : homes) {
            BlockPos route = null;
            for (BlockPos entrance : home.stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList()) {
                points.add(entrance);
                if (streets.containsKey(entrance)) { route = entrance; break; }
                for (BlockPos adjacent : neighbours(entrance)) if (streets.containsKey(adjacent) && edge(level, entrance, adjacent)) { route = adjacent; break; }
                if (route != null) break;
            }
            if (route == null) continue;
            count++;
            BlockPos position = route;
            while (true) {
                boolean added = points.add(position); BlockPos parent = streets.get(position);
                if (position.equals(parent)) break;
                // An entrance may already be in points even though its route was not traced yet.
                if (!added && !home.contains(position)) break;
                position = parent;
            }
        }
        var ordered = points.stream().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
        var selected = new ArrayList<BlockPos>(); int size = Math.min(SAMPLE_LIMIT, ordered.size());
        for (int i = 0; i < size; i++) selected.add(ordered.get(i * ordered.size() / size));
        samples = selected; connected = count; phase = 3;
    }

    private void observeSample(ServerLevel level) {
        if (sampleIndex >= samples.size()) {
            for (BlockPos chunk : chunks)
                if (level.getChunkSource().getChunkNow(chunk.getX(), chunk.getZ()) == null) abort();
            done = true; return;
        }
        BlockPos sample = samples.get(sampleIndex);
        // Radius seven also retains the actual nearby street lamps for city inspection.
        if (gardenIndex < 3375) {
            int dx = gardenIndex % 15 - 7, dz = gardenIndex / 15 % 15 - 7, dy = gardenIndex / 225 - 7;
            int distance = dx * dx + dy * dy + dz * dz;
            if (distance <= 49) {
                BlockPos pos = sample.offset(dx, dy, dz); var block = state(level, pos);
                if (block.getLightEmission(level, pos) > 0) record(pos, "Lighting");
                if (distance <= 16) {
                    String kind = greenery(level, pos, block);
                    if (kind != null && visible(level, sample.above(), pos)) { sampleKinds.add(kind); record(pos, "Greenery"); }
                }
            }
            gardenIndex++; return;
        }
        state(level, sample);
        boolean illuminated = level.getBrightness(LightLayer.BLOCK, sample) >= 8;
        boolean paved = paving(level, sample);
        boolean doorway = state(level, sample).getBlock() instanceof DoorBlock;
        // All reads precede committing counters, so a yielding sample cannot count twice.
        if (illuminated) lit++;
        if (!sampleKinds.isEmpty()) green++;
        greeneryKinds.addAll(sampleKinds); sampleKinds.clear();
        if (paved) record(sample.below(), "Path");
        else if (doorway) record(sample, "Path");
        gardenIndex = 0; sampleIndex++;
    }

    private void record(BlockPos pos, String category) {
        int limit = category.equals("Greenery") ? 192 : 160;
        if (sources.containsKey(pos) || sourceCounts.getOrDefault(category, 0) >= limit) return;
        sources.put(pos.immutable(), category); sourceCounts.merge(category, 1, Integer::sum);
    }

    private String greenery(ServerLevel level, BlockPos pos, BlockState block) {
        if (plants.containsKey(pos)) return plants.get(pos);
        String kind = null;
        if (block.getBlock() instanceof FlowerPotBlock pot && pot.getContent() != Blocks.AIR) kind = "potted";
        else if (block.is(BlockTags.FLOWERS) && cluster(level, pos, BlockTags.FLOWERS)) kind = "flowerbed";
        else if (block.is(BlockTags.LEAVES)) {
            for (int dx = -2; dx <= 2 && kind == null; dx++) for (int dz = -2; dz <= 2 && kind == null; dz++)
                for (int dy = -5; dy <= 1; dy++) {
                    BlockPos log = pos.offset(dx, dy, dz);
                    if (state(level, log).is(BlockTags.LOGS) && state(level, log.below()).is(BlockTags.LOGS)) { kind = "tree"; break; }
                }
            if (kind == null && cluster(level, pos, BlockTags.LEAVES)) kind = "hedge";
        }
        plants.put(pos.immutable(), kind); return kind;
    }

    private boolean cluster(ServerLevel level, BlockPos origin, net.minecraft.tags.TagKey<Block> tag) {
        var cells = new LinkedHashSet<BlockPos>(); cells.add(origin);
        for (int round = 0; round < 2 && cells.size() < 3; round++) {
            var prior = List.copyOf(cells);
            for (BlockPos cell : prior) for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = cell.relative(direction);
                if (state(level, next).is(tag)) cells.add(next);
                if (cells.size() >= 3) return true;
            }
        }
        return false;
    }

    private boolean visible(ServerLevel level, BlockPos eye, BlockPos target) {
        double dx = target.getX() - eye.getX(), dy = target.getY() - eye.getY(), dz = target.getZ() - eye.getZ();
        int steps = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 4);
        for (int i = 1; i < steps; i++) {
            double fraction = i / (double) steps;
            BlockPos at = BlockPos.containing(eye.getX() + .5 + dx * fraction, eye.getY() + .5 + dy * fraction, eye.getZ() + .5 + dz * fraction);
            if (at.equals(eye) || at.equals(target)) continue;
            var block = state(level, at);
            if (!block.is(BlockTags.LEAVES) && block.isSolidRender(level, at)) return false;
        }
        return true;
    }

    CompoundTag result(long now) {
        var tag = new CompoundTag(); tag.putBoolean("Confirmed", true); tag.putLong("Time", now); tag.putString("Signature", signature);
        tag.putDouble("Path", homes.isEmpty() ? 0 : 25D * connected / homes.size());
        tag.putDouble("Lighting", samples.isEmpty() ? 0 : 25D * lit / samples.size());
        tag.putDouble("Greenery", samples.isEmpty() ? 0 : 20D * green / samples.size() + 1.25 * greeneryKinds.size());
        tag.putInt("Houses", homes.size()); tag.putInt("ConnectedHouses", connected); tag.putInt("StreetSamples", samples.size());
        var positions = new ListTag(); sources.forEach((pos, category) -> {
            var row = new CompoundTag(); row.putLong("Position", pos.asLong()); row.putString("Category", category);
            var block = blocks.get(pos); var id = block == null ? null : ForgeRegistries.BLOCKS.getKey(block.getBlock());
            if (id != null) row.putString("Block", id.toString()); positions.add(row);
        }); tag.put("Sources", positions); return tag;
    }
}

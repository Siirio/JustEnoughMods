package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

/** Bounded observations of physical interiors; never claims POIs or owns navigation. */
public final class ResidentialRooms {
    public record Snapshot(int area, Set<BlockPos> floors, boolean complete, boolean unavailable) {
        public Snapshot { floors = Set.copyOf(floors); }
    }
    public enum RoomRelation { SAME, DIFFERENT, PENDING, UNAVAILABLE }
    private record Key(ResourceKey<Level> dimension, BlockPos bed, boolean privacy) { }
    private record Result(Key key, Snapshot snapshot, Set<BlockPos> beds, long expires) { }
    private static final Snapshot PENDING = new Snapshot(0, Set.of(), false, false);
    private static final Snapshot UNKNOWN = new Snapshot(0, Set.of(), false, true);
    private static final Map<Key, Job> JOBS = new LinkedHashMap<>();
    private static final List<Result> CACHE = new ArrayList<>();
    private static int reads;
    private static final Pause PAUSE = new Pause();
    private static final class Pause extends RuntimeException {
        private Pause() { super(null, null, false, false); }
    }
    private ResidentialRooms() { }

    /** Shared resumable traversal. A yielding observation leaves its frontier intact. */
    static final class Graph<P> {
        final Set<P> cells = new HashSet<>();
        final ArrayDeque<P> frontier = new ArrayDeque<>();
        final int limit;
        boolean bounded;
        Graph(int limit) { this.limit = limit; }
        void seed(P cell) { if (cells.add(cell)) frontier.addLast(cell); }
        boolean advance(java.util.function.Function<P, List<P>> neighbours,
                        java.util.function.BiPredicate<P, P> connected, java.util.function.Predicate<P> within) {
            while (!frontier.isEmpty()) {
                P current = frontier.peekFirst();
                for (P next : neighbours.apply(current)) {
                    if (cells.contains(next) || !connected.test(current, next)) continue;
                    if (!within.test(next) || cells.size() >= limit) { bounded = true; return true; }
                    seed(next);
                }
                frontier.removeFirst();
            }
            return true;
        }
        Set<P> census(boolean complete, boolean unavailable) {
            return complete && !unavailable && !bounded ? Set.copyOf(cells) : Set.of();
        }
    }

    /** A room divider spans the full living-height column; furniture and low partitions do not. */
    static boolean roomDivider(boolean door, List<double[]> spans) {
        if (door) return true;
        var sorted = new ArrayList<>(spans);
        sorted.sort(java.util.Comparator.comparingDouble(span -> span[0]));
        double reached = .01;
        for (double[] span : sorted) {
            if (span[0] > reached + .001) return false;
            reached = Math.max(reached, span[1]);
            if (reached >= 1.95) return true;
        }
        return false;
    }

    record VerticalPassage(double min, double max) { }
    /** Traverse at the higher actual support surface, including fractional slab/stair heights. */
    static VerticalPassage sweptHeight(double fromFeet, double toFeet, double bodyHeight) {
        double feet = Math.max(fromFeet, toFeet);
        return new VerticalPassage(feet + .01, feet + bodyHeight);
    }
    /** Native humanoid one-block jumps do not turn 1.5-block fences into usable stairs. */
    static boolean reachableStep(double fromFeet, double toFeet) { return toFeet - fromFeet <= 1.25; }

    public static void clear() { JOBS.clear(); CACHE.clear(); }

    public static Snapshot request(ServerLevel level, BlockPos bed) { return request(level, bed, false); }

    /** Door-separated same-floor connectivity, including the usable air above bed furniture. */
    public static RoomRelation sameFloorRoom(ServerLevel level, BlockPos first, BlockPos second) {
        if (first.getY() != second.getY()) return RoomRelation.DIFFERENT;
        Snapshot room = request(level, first, true);
        if (room.unavailable()) return RoomRelation.UNAVAILABLE;
        if (!room.complete()) {
            for (Job job : JOBS.values()) if (job.key.privacy() && job.key.dimension().equals(level.dimension())
                    && (job.key.bed().equals(first) || job.floors.contains(first)) && job.floors.contains(second))
                return RoomRelation.SAME;
            return RoomRelation.PENDING;
        }
        return room.floors().contains(second) ? RoomRelation.SAME : RoomRelation.DIFFERENT;
    }

    private static Snapshot request(ServerLevel level, BlockPos bed, boolean privacy) {
        if (!SettlementAuthority.active(level.getServer())) return UNKNOWN;
        Key key = new Key(level.dimension(), bed.immutable(), privacy);
        long now = level.getGameTime();
        CACHE.removeIf(result -> result.expires() <= now || result.expires() - now > 400);
        for (Result result : CACHE) {
            if (!result.key().dimension().equals(key.dimension()) || result.key().privacy() != privacy) continue;
            if (result.key().equals(key)) return result.snapshot();
            if (result.snapshot().complete() && result.beds().contains(bed)) return result.snapshot();
        }
        if (JOBS.containsKey(key)) return PENDING;
        for (Job job : JOBS.values()) if (job.key.dimension().equals(key.dimension()) && job.key.privacy() == privacy
                && job.containsBed(bed)) return PENDING;
        if (JOBS.size() >= 4) return UNKNOWN;
        JOBS.put(key, new Job(key));
        return PENDING;
    }

    public static void invalidate(ServerLevel level, BlockPos changed) {
        CACHE.removeIf(result -> affected(result.key(), level, changed));
        JOBS.entrySet().removeIf(entry -> affected(entry.getKey(), level, changed));
    }
    private static boolean affected(Key key, ServerLevel level, BlockPos changed) {
        return key.dimension().equals(level.dimension()) && Math.abs(key.bed().getX() - changed.getX()) <= 34
                && Math.abs(key.bed().getZ() - changed.getZ()) <= 34 && Math.abs(key.bed().getY() - changed.getY()) <= 50;
    }

    public static void tick(MinecraftServer server) {
        if (!SettlementAuthority.active(server)) return;
        reads = 1024;
        var jobs = new ArrayList<>(JOBS.values());
        jobs.sort(java.util.Comparator.comparing(job -> !job.key.privacy()));
        for (Job job : jobs) {
            if (reads <= 0) break;
            ServerLevel level = server.getLevel(job.key.dimension());
            if (level == null) { JOBS.remove(job.key); continue; }
            try { job.advance(level); } catch (Pause ignored) { }
            if (job.done) {
                Snapshot snapshot = job.unknown ? UNKNOWN : new Snapshot(job.floors.size(), job.graph.census(true, false), true, false);
                var beds = new HashSet<BlockPos>();
                if (!job.unknown) job.blocks.forEach((pos, block) -> { if (job.containsBed(pos)) beds.add(pos); });
                CACHE.add(new Result(job.key, snapshot, Set.copyOf(beds), level.getGameTime() + (job.unknown ? 20 : 200)));
                while (CACHE.size() > 128) CACHE.remove(0);
                JOBS.remove(job.key);
            }
        }
    }

    private static final class Job {
        final Key key;
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final Map<BlockPos, Boolean> walkable = new HashMap<>(), covered = new HashMap<>();
        final Graph<BlockPos> graph = new Graph<>(4096);
        final Set<BlockPos> floors = graph.cells;
        final List<BlockPos> seeds = new ArrayList<>();
        int seed;
        boolean initialized, done, unknown;
        Job(Key key) { this.key = key; }

        boolean containsBed(BlockPos bed) {
            if (key.privacy()) return floors.contains(bed);
            BlockState block = blocks.get(bed);
            if (block == null || !(block.getBlock() instanceof BedBlock)) return false;
            for (Direction direction : Direction.Plane.HORIZONTAL)
                if (floors.contains(bed.relative(direction))) return true;
            return false;
        }

        BlockState state(ServerLevel level, BlockPos pos) {
            BlockState prior = blocks.get(pos);
            if (prior != null) return prior;
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() || !level.hasChunkAt(pos)) {
                unknown = true; done = true; throw PAUSE;
            }
            if (reads <= 0) throw PAUSE;
            reads--;
            BlockState value = level.getBlockState(pos);
            blocks.put(pos.immutable(), value);
            // A sparse open-volume scan must also have a hard memory bound.
            if (blocks.size() > 65536) { unknown = true; done = true; throw PAUSE; }
            return value;
        }

        void advance(ServerLevel level) {
            if (!initialized) {
                BlockState bed = state(level, key.bed());
                if (!(bed.getBlock() instanceof BedBlock)) { unknown = true; done = true; return; }
                BlockPos head = bed.getValue(BedBlock.PART) == BedPart.HEAD ? key.bed()
                        : key.bed().relative(bed.getValue(BedBlock.FACING));
                BlockPos foot = head.relative(bed.getValue(BedBlock.FACING).getOpposite());
                for (BlockPos half : new BlockPos[] {head, foot}) {
                    if (key.privacy()) seeds.add(half);
                    else for (Direction direction : Direction.Plane.HORIZONTAL) seeds.add(half.relative(direction));
                }
                initialized = true;
            }
            while (seed < seeds.size()) {
                BlockPos candidate = seeds.get(seed);
                if (walkable(level, candidate)) add(candidate);
                seed++;
            }
            // Fully loaded obstructed adjacency proves zero usable area, rather than unknown.
            done = graph.advance(current -> {
                var next = new ArrayList<BlockPos>();
                for (Direction direction : Direction.Plane.HORIZONTAL)
                    for (int dy : key.privacy() ? new int[] {0} : new int[] {0, 1, -1})
                        next.add(current.relative(direction).offset(0, dy, 0));
                return next;
            }, (current, next) -> walkable(level, next) && edge(level, current, next),
                    next -> Math.abs(next.getX() - key.bed().getX()) <= 32
                            && Math.abs(next.getZ() - key.bed().getZ()) <= 32 && Math.abs(next.getY() - key.bed().getY()) <= 24);
            unknown |= graph.bounded;
        }

        void add(BlockPos pos) {
            graph.seed(pos.immutable());
        }

        boolean walkable(ServerLevel level, BlockPos pos) {
            Boolean prior = walkable.get(pos);
            if (prior != null) return prior;
            BlockState feet = state(level, pos), below = state(level, pos.below());
            boolean bed = feet.getBlock() instanceof BedBlock;
            boolean result;
            if (key.privacy() && feet.getBlock() instanceof DoorBlock) result = false;
            else if (bed && !key.privacy()) result = false;
            else {
                double height = bed ? .5625 : supportOffset(level, pos);
                result = height >= -.5 && !below.getFluidState().isSource()
                        && (key.privacy() ? !privacyBoundary(level, pos, new AABB(pos).deflate(.2, 0, .2)) : clear(level, pos, height));
            }
            walkable.put(pos.immutable(), result);
            return result;
        }

        double supportOffset(ServerLevel level, BlockPos pos) {
            var support = state(level, pos.below()).getCollisionShape(level, pos.below());
            return support.isEmpty() ? -1 : support.max(Direction.Axis.Y) - 1;
        }

        boolean clear(ServerLevel level, BlockPos pos, double offset) {
            // Privacy connectivity describes shared room air above low bed furniture, not standing on a mattress.
            double top = key.privacy() && state(level, pos).getBlock() instanceof BedBlock ? 1.95 : offset + 1.95;
            AABB body = new AABB(.2, offset + .01, .2, .8, top, .8);
            for (int dy = (int) Math.floor(offset); dy <= (int) Math.floor(top); dy++) {
                BlockPos at = pos.offset(0, dy, 0);
                BlockState block = state(level, at);
                if (block.getBlock() instanceof DoorBlock) {
                    if (key.privacy()) return false;
                    continue;
                }
                if (key.privacy() && block.getBlock() instanceof BedBlock && dy == 0) continue;
                if (!block.getFluidState().isEmpty()) return false;
                for (AABB box : block.getCollisionShape(level, at).toAabbs())
                    if (body.intersects(box.move(0, dy, 0))) return false;
            }
            return true;
        }

        boolean edge(ServerLevel level, BlockPos from, BlockPos to) {
            double y = Math.max(from.getY(), to.getY());
            double top = y + 1.95;
            double bottom = y + .01;
            if (!key.privacy()) {
                double fromFeet = from.getY() + supportOffset(level, from);
                double toFeet = to.getY() + supportOffset(level, to);
                if (!reachableStep(fromFeet, toFeet)) return false;
                var height = sweptHeight(fromFeet, toFeet, 1.95);
                bottom = height.min(); top = height.max();
            }
            if (key.privacy() && (state(level, from).getBlock() instanceof BedBlock
                    || state(level, to).getBlock() instanceof BedBlock)) bottom += .5625;
            AABB passage = new AABB(Math.min(from.getX(), to.getX()) + .2, bottom,
                    Math.min(from.getZ(), to.getZ()) + .2, Math.max(from.getX(), to.getX()) + .8,
                    top, Math.max(from.getZ(), to.getZ()) + .8);
            if (key.privacy()) {
                for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(Math.min(from.getX(), to.getX()), from.getY(), Math.min(from.getZ(), to.getZ())),
                        new BlockPos(Math.max(from.getX(), to.getX()), from.getY(), Math.max(from.getZ(), to.getZ()))))
                    if (privacyBoundary(level, pos, passage)) return false;
                return true;
            }
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(passage.minX, passage.minY, passage.minZ),
                    BlockPos.containing(passage.maxX, passage.maxY, passage.maxZ))) {
                BlockState block = state(level, pos);
                if (block.getBlock() instanceof DoorBlock) {
                    if (key.privacy()) return false;
                    continue;
                }
                if (!block.getFluidState().isEmpty()) return false;
                for (AABB box : block.getCollisionShape(level, pos).toAabbs())
                    if (passage.intersects(box.move(pos.getX(), pos.getY(), pos.getZ()))) return false;
            }
            return true;
        }

        boolean privacyBoundary(ServerLevel level, BlockPos pos, AABB passage) {
            var spans = new ArrayList<double[]>();
            boolean door = false;
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos at = pos.above(dy);
                BlockState block = state(level, at);
                door |= block.getBlock() instanceof DoorBlock;
                for (AABB box : block.getCollisionShape(level, at).toAabbs()) {
                    AABB world = box.move(at.getX(), at.getY(), at.getZ());
                    if (world.maxX > passage.minX && world.minX < passage.maxX
                            && world.maxZ > passage.minZ && world.minZ < passage.maxZ)
                        spans.add(new double[] {box.minY + dy, box.maxY + dy});
                }
            }
            return roomDivider(door, spans);
        }

        boolean roof(ServerLevel level, BlockPos pos) {
            Boolean prior = covered.get(pos);
            if (prior != null) return prior;
            for (int dy = 2; dy <= 24; dy++) {
                BlockPos above = pos.offset(0, dy, 0);
                if (above.getY() >= level.getMaxBuildHeight()) break;
                if (!state(level, above).getCollisionShape(level, above).isEmpty()) {
                    covered.put(pos.immutable(), true); return true;
                }
            }
            covered.put(pos.immutable(), false);
            return false;
        }
    }
}

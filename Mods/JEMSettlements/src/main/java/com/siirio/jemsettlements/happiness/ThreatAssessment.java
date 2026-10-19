package com.siirio.jemsettlements.happiness;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Access;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Cell;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Result;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Sight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/** Cached, tick-distributed local collision flood; never invokes native path creation or loads chunks. */
public final class ThreatAssessment {
    public enum Kind { NONE, GLASS, OPEN }
    public record Assessment(Kind kind, boolean complete) { }
    private record Pair(UUID resident, UUID hostile) { }
    private record Key(GlobalPos origin, GlobalPos destination, Pair pair, int width, int height, BlockPos breakingDoor) { }
    private static final class Job {
        final Key key;
        ThreatRouteModel.Search search;
        final Sight sight;
        final long created;
        long finished = -1;
        Assessment assessment;
        Job(Key key, ThreatRouteModel.Search search, Sight sight, long created) {
            this.key = key; this.search = search; this.sight = sight; this.created = created;
            assessment = projection(Result.PENDING, sight);
        }
    }
    private static final int MAX_CACHE = 2048, MAX_PENDING = 256, CELL_BUDGET = 128, JOB_SLICE = 16;
    private static final Map<Key, Job> CACHE = new LinkedHashMap<>();
    private static final Map<Pair, Key> LATEST = new LinkedHashMap<>();
    private static final ArrayDeque<Job> PENDING = new ArrayDeque<>();
    private ThreatAssessment() { }
    public static void clear() { CACHE.clear(); PENDING.clear(); LATEST.clear(); }
    public static void invalidate(ServerLevel level, BlockPos changed) {
        CACHE.entrySet().removeIf(entry -> entry.getKey().origin().dimension().equals(level.dimension()));
        PENDING.removeIf(job -> job.key.origin().dimension().equals(level.dimension()));
        LATEST.entrySet().removeIf(entry -> entry.getValue().origin().dimension().equals(level.dimension()));
    }
    public static Assessment request(ServerLevel level, LivingEntity resident, Mob hostile) {
        long now = level.getGameTime();
        BlockPos from = new BlockPos(hostile.getBlockX(), (int) Math.ceil(hostile.getY()), hostile.getBlockZ());
        BlockPos to = new BlockPos(resident.getBlockX(), (int) Math.ceil(resident.getY()), resident.getBlockZ());
        BlockPos door = breakingDoor(level, hostile);
        Pair pair = new Pair(resident.getUUID(), hostile.getUUID());
        Key key = new Key(GlobalPos.of(level.dimension(), from), GlobalPos.of(level.dimension(), to), pair,
                (int) Math.ceil(hostile.getBbWidth() * 100), (int) Math.ceil(hostile.getBbHeight() * 100), door);
        Key old = LATEST.get(pair);
        if (old != null && !old.equals(key)) { PENDING.remove(CACHE.remove(old)); LATEST.remove(pair); }
        Job previous = CACHE.get(key);
        if (previous != null && previous.finished >= 0 && now >= previous.finished
                && now - previous.finished < HousingConditionConfig.FEAR_INTERVAL.get()) return previous.assessment;
        if (previous != null && previous.finished < 0) return previous.assessment;
        Sight sight = sight(level, resident.getEyePosition(), hostile.getEyePosition());
        if (sight != Sight.CLEAR && PENDING.size() >= MAX_PENDING) return new Assessment(Kind.NONE, false);
        int radius = HousingConditionConfig.FEAR_RADIUS.get() + 2;
        var grid = new CollisionGrid(level, hostile, key.width() / 100D, key.height() / 100D, door);
        Job job = new Job(key, new ThreatRouteModel.Search(grid, cell(from), cell(to), radius, 2048), sight, now);
        if (CACHE.size() >= MAX_CACHE) {
            Key eldest = CACHE.keySet().iterator().next(); Job removed = CACHE.remove(eldest); PENDING.remove(removed); LATEST.remove(eldest.pair(), eldest);
        }
        CACHE.put(key, job); LATEST.put(pair, key);
        if (sight == Sight.CLEAR) { job.finished = now; job.search = null; }
        else PENDING.addLast(job);
        return job.assessment;
    }
    public static void tick(MinecraftServer server) {
        int budget = CELL_BUDGET;
        while (budget > 0 && !PENDING.isEmpty()) {
            Job job = PENDING.removeFirst();
            if (CACHE.get(job.key) != job) continue;
            if (server.overworld().getGameTime() - job.created > 2L * HousingConditionConfig.FEAR_INTERVAL.get()) {
                job.assessment = projection(Result.UNKNOWN, job.sight); job.finished = server.overworld().getGameTime(); job.search = null; continue;
            }
            int slice = Math.min(budget, JOB_SLICE); budget -= slice;
            Result result = job.search.advance(slice);
            if (result == Result.PENDING) PENDING.addLast(job);
            else {
                job.assessment = projection(result, job.sight);
                job.finished = server.overworld().getGameTime();
                job.search = null;
            }
        }
    }
    private static Cell cell(BlockPos pos) { return new Cell(pos.getX(), pos.getY(), pos.getZ()); }
    private static BlockPos position(Cell cell) { return new BlockPos(cell.x(), cell.y(), cell.z()); }
    private static final class CollisionGrid implements ThreatRouteModel.Grid {
        final ServerLevel level; final Mob mob; final double width, height; final BlockPos door;
        final Map<BlockPos, BlockState> states = new java.util.HashMap<>();
        CollisionGrid(ServerLevel level, Mob mob, double width, double height, BlockPos door) {
            this.level = level; this.mob = mob; this.width = width; this.height = height; this.door = door;
        }
        private AABB box(Cell cell) { return new AABB(cell.x() + .5 - width / 2, cell.y() + .01, cell.z() + .5 - width / 2,
                cell.x() + .5 + width / 2, cell.y() + height - .01, cell.z() + .5 + width / 2); }
        private BlockState state(BlockPos pos) {
            BlockState known = states.get(pos);
            if (known != null) return known;
            BlockState observed = level.getBlockState(pos);
            if (states.size() < 2048) states.put(pos.immutable(), observed);
            return observed;
        }
        @Override public Access stand(Cell cell) {
            BlockPos feet = position(cell), below = feet.below();
            if (!level.hasChunkAt(below)) return Access.UNKNOWN;
            BlockState support = state(below);
            var shape = support.getCollisionShape(level, below, net.minecraft.world.phys.shapes.CollisionContext.of(mob));
            if (shape.isEmpty() || !state(feet).getFluidState().isEmpty()) return Access.BLOCKED;
            Access clearance = clear(box(cell));
            if (clearance != Access.OPEN) return clearance;
            // Integer-grid support is certified only at its actual collision surface; never float over slabs.
            return ThreatCollisionPolicy.supported(below.getY() + shape.max(net.minecraft.core.Direction.Axis.Y), cell.y()) ? Access.OPEN : Access.UNKNOWN;
        }
        @Override public Access step(Cell from, Cell to) {
            AABB start = box(from), end = box(to);
            if (to.y() > from.y()) {
                if (!ThreatCollisionPolicy.canStep(from.y(), to.y(), mob.maxUpStep())) return Access.UNKNOWN;
                Access lift = clear(start.expandTowards(0, to.y() - from.y(), 0));
                if (lift != Access.OPEN) return lift;
                return clear(start.move(0, to.y() - from.y(), 0).minmax(end));
            }
            Access horizontal = clear(start.expandTowards(to.x() - from.x(), 0, to.z() - from.z()));
            return horizontal == Access.OPEN && to.y() < from.y() ? clear(end.expandTowards(0, from.y() - to.y(), 0)) : horizontal;
        }
        private Access clear(AABB bounds) {
            for (BlockPos pos : BlockPos.betweenClosed(ThreatCollisionPolicy.firstOwner(bounds.minX), ThreatCollisionPolicy.firstOwner(bounds.minY), ThreatCollisionPolicy.firstOwner(bounds.minZ),
                    ThreatCollisionPolicy.lastOwner(bounds.maxX), ThreatCollisionPolicy.lastOwner(bounds.maxY), ThreatCollisionPolicy.lastOwner(bounds.maxZ))) {
                if (!level.hasChunkAt(pos)) return Access.UNKNOWN;
                BlockState state = state(pos);
                if (door != null && pos.getX() == door.getX() && pos.getZ() == door.getZ()
                        && (pos.getY() == door.getY() || pos.getY() == door.getY() + 1) && state.getBlock() instanceof DoorBlock) continue;
                for (AABB shape : state.getCollisionShape(level, pos, net.minecraft.world.phys.shapes.CollisionContext.of(mob)).toAabbs()) {
                    AABB actual = shape.move(pos);
                    if (ThreatCollisionPolicy.overlaps(actual.minX, actual.minY, actual.minZ, actual.maxX, actual.maxY, actual.maxZ,
                            bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)) return Access.BLOCKED;
                }
            }
            return Access.OPEN;
        }
    }
    private static Assessment projection(Result result, Sight sight) {
        var evidence = ThreatRouteModel.assess(result, sight);
        return new Assessment(evidence.threat() == ThreatRouteModel.Threat.NORMAL ? Kind.OPEN
                : evidence.threat() == ThreatRouteModel.Threat.GLASS ? Kind.GLASS : Kind.NONE, evidence.complete());
    }
    private static Sight sight(ServerLevel level, Vec3 from, Vec3 to) {
        boolean[] glass = {false}; int[] visited = {0};
        return BlockGetter.traverseBlocks(from, to, level, (world, pos) -> {
            if (++visited[0] > 256 || !world.hasChunkAt(pos)) return Sight.UNKNOWN;
            var state = world.getBlockState(pos);
            var collision = state.getCollisionShape(world, pos);
            if (collision.isEmpty() || collision.clip(from, to, pos) == null) return null;
            if (state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES)) { glass[0] = true; return null; }
            return Sight.BLOCKED;
        }, ignored -> glass[0] ? Sight.GLASS : Sight.CLEAR);
    }
    private static BlockPos breakingDoor(ServerLevel level, Mob hostile) {
        if (hostile.goalSelector.getRunningGoals().noneMatch(goal -> goal.getGoal() instanceof BreakDoorGoal)) return null;
        BlockPos nearest = null; double distance = 6.25;
        for (BlockPos pos : BlockPos.betweenClosed(hostile.blockPosition().offset(-2, -1, -2), hostile.blockPosition().offset(2, 2, 2))) {
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.OPEN) || !state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)) continue;
            BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.immutable() : pos.below();
            double candidate = lower.distToCenterSqr(hostile.position());
            if (candidate < distance) { distance = candidate; nearest = lower; }
        }
        return nearest;
    }
}

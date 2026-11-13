package com.siirio.jemlightingsandbox.terrain;

import com.siirio.jemlightingsandbox.LightingSettings;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeBlock;

/** Indexes native emitters from resident client columns without requesting any chunks. */
public final class ChunkLightIndex {
    private static final int LOOKUPS_PER_TICK = 96;
    private static final int REVALIDATIONS_PER_TICK = 32;
    private static final int REVALIDATION_INTERVAL = 20;
    private static final int LIGHTS_PER_SUBCELL = 4;
    private static final int SECTION_CELL_MASK = 4095;
    private static final ClassValue<Boolean> CONTEXTUAL_EMISSION = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getLightEmission", BlockState.class, BlockGetter.class, BlockPos.class)
                        .getDeclaringClass() != IForgeBlock.class;
            } catch (NoSuchMethodException exception) {
                // A transformed mod class can still participate through Forge's contextual call.
                return true;
            }
        }
    };

    private final Long2ObjectOpenHashMap<Column> columns = new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet lookups = new LongLinkedOpenHashSet();
    private final LongLinkedOpenHashSet changes = new LongLinkedOpenHashSet();
    private final PriorityQueue<Column> pending = new PriorityQueue<>(Comparator
            .comparingInt((Column column) -> column.priority).thenComparingLong(column -> column.key));
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    private ClientLevel level;
    private Window window;
    private Discovery discovery;
    private Discovery revalidation;
    private int ticksUntilRevalidation;
    private int viewSectionY;
    private long revision;

    public void tick(ClientLevel currentLevel, Vec3 camera, int chunkRadius, LightingSettings.Mode mode) {
        if (level != currentLevel) {
            clear();
            level = currentLevel;
        }
        int cameraX = SectionPos.blockToSectionCoord(Mth.floor(camera.x));
        int cameraY = SectionPos.blockToSectionCoord(Mth.floor(camera.y));
        int cameraZ = SectionPos.blockToSectionCoord(Mth.floor(camera.z));
        Window next = new Window(cameraX, cameraZ, Mth.clamp(chunkRadius, 2, 32));
        boolean moved = !next.equals(window) || cameraY != viewSectionY;
        viewSectionY = cameraY;
        if (!next.equals(window)) {
            moveWindow(next);
        }
        if (moved) {
            pending.clear();
            for (Column column : columns.values()) {
                column.queued = false;
                enqueue(column);
            }
        }

        // Give both arriving packets and the stationary outward scan a share of each tick.
        int packetBudget = discovery == null ? LOOKUPS_PER_TICK : LOOKUPS_PER_TICK / 2;
        int used = 0;
        while (!lookups.isEmpty() && used < packetBudget) {
            refreshColumn(lookups.removeFirstLong(), true);
            used++;
        }
        while (discovery != null && used < LOOKUPS_PER_TICK) {
            refreshColumn(discovery.advance(), false);
            if (discovery.finished()) {
                discovery = null;
            }
            used++;
        }
        revalidateColumns();

        int reads = switch (mode) {
            case SLOW -> 4;
            case FAST -> 12;
            case REALTIME -> 20;
        };
        for (int sectionBudget = 128; sectionBudget > 0 && reads > 0 && !pending.isEmpty(); sectionBudget--) {
            Column column = pending.remove();
            column.queued = false;
            if (columns.get(column.key) != column) {
                continue;
            }
            LevelChunk chunk = loadedChunk(column.key);
            if (chunk != column.chunk) {
                refreshColumn(column.key, false);
                continue;
            }

            int sectionIndex = column.closestDirtySection(column.referenceSection(viewSectionY));
            column.dirtySections.clear(sectionIndex);
            if (column.preferredSection == sectionIndex) {
                column.preferredSection = -1;
            }
            var section = chunk.getSection(sectionIndex);
            if (section.hasOnlyAir() || !section.maybeHas(ChunkLightIndex::mayEmit)) {
                publishSection(column, sectionIndex, List.of());
            } else {
                publishSection(column, sectionIndex, scanSection(column, sectionIndex));
                reads--;
            }
            enqueue(column);
        }
    }

    public void invalidate(BlockPos position) {
        if (window == null || level.isOutsideBuildHeight(position.getY())) {
            return;
        }
        int chunkX = SectionPos.blockToSectionCoord(position.getX());
        int chunkZ = SectionPos.blockToSectionCoord(position.getZ());
        int sectionY = SectionPos.blockToSectionCoord(position.getY());
        // Contextual mod emission may depend on a neighbor across a section boundary.
        int minX = SectionPos.blockToSectionCoord(position.getX() - 1);
        int maxX = SectionPos.blockToSectionCoord(position.getX() + 1);
        int minZ = SectionPos.blockToSectionCoord(position.getZ() - 1);
        int maxZ = SectionPos.blockToSectionCoord(position.getZ() + 1);
        int minY = SectionPos.blockToSectionCoord(position.getY() - 1);
        int maxY = SectionPos.blockToSectionCoord(position.getY() + 1);
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                Column column = columns.get(ChunkPos.asLong(x, z));
                if (column == null) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    int index = column.chunk.getSectionIndexFromSectionY(y);
                    if (index >= 0 && index < column.chunk.getSectionsCount()) {
                        column.dirtySections.set(index);
                    }
                }
                if (x == chunkX && z == chunkZ) {
                    column.preferredSection = column.chunk.getSectionIndexFromSectionY(sectionY);
                }
                if (column.queued) {
                    pending.remove(column);
                    column.queued = false;
                }
                enqueue(column);
            }
        }
    }

    public void invalidateChunk(ChunkPos chunk) {
        if (window != null && window.contains(chunk.x, chunk.z)) {
            lookups.addAndMoveToFirst(chunk.toLong());
        }
    }

    public List<TerrainLightVolume.Light> sources(int chunkX, int chunkZ) {
        Column column = columns.get(ChunkPos.asLong(chunkX, chunkZ));
        return column == null ? List.of() : column.sources;
    }

    public List<ChunkPos> drainChanges() {
        if (changes.isEmpty()) {
            return List.of();
        }
        var result = new ArrayList<ChunkPos>(changes.size());
        var positions = changes.iterator();
        while (positions.hasNext()) {
            result.add(new ChunkPos(positions.nextLong()));
        }
        changes.clear();
        return result;
    }

    public long revision() {
        return revision;
    }

    public int coveredChunks() {
        return columns.size();
    }

    public void clear() {
        columns.clear();
        pending.clear();
        lookups.clear();
        changes.clear();
        window = null;
        discovery = null;
        revalidation = null;
        ticksUntilRevalidation = 0;
        level = null;
        revision++;
    }

    private void moveWindow(Window next) {
        Window previous = window;
        window = next;
        var entries = columns.long2ObjectEntrySet().fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            if (!next.contains(entry.getLongKey())) {
                if (!entry.getValue().sources.isEmpty()) {
                    changed(entry.getLongKey());
                }
                entries.remove();
            }
        }
        if (previous == null || !next.intersects(previous)) {
            lookups.clear();
            discovery = new Discovery(next);
            revalidation = null;
            return;
        }
        var queued = lookups.iterator();
        while (queued.hasNext()) {
            if (!next.contains(queued.nextLong())) {
                queued.remove();
            }
        }
        // Continue the original outward scan and queue only newly entered strips.
        for (int z = next.minZ(); z <= next.maxZ(); z++) {
            if (z < previous.minZ() || z > previous.maxZ()) {
                queueStrip(next.minX(), next.maxX(), z);
            } else {
                queueStrip(next.minX(), Math.min(next.maxX(), previous.minX() - 1), z);
                queueStrip(Math.max(next.minX(), previous.maxX() + 1), next.maxX(), z);
            }
        }
    }

    private void queueStrip(int minX, int maxX, int z) {
        for (int x = minX; x <= maxX; x++) {
            lookups.add(ChunkPos.asLong(x, z));
        }
    }

    private void revalidateColumns() {
        // Forge posts events for individual packet loads and drops. Cache resizing can evict
        // columns without posting a drop, so periodically recheck resident chunk identity.
        if (revalidation == null && discovery == null && --ticksUntilRevalidation <= 0) {
            revalidation = new Discovery(window);
        }
        for (int count = 0; count < REVALIDATIONS_PER_TICK && revalidation != null; count++) {
            long key = revalidation.advance();
            if (columns.containsKey(key)) {
                refreshColumn(key, false);
            }
            if (revalidation.finished()) {
                revalidation = null;
                ticksUntilRevalidation = REVALIDATION_INTERVAL;
            }
        }
    }

    private void refreshColumn(long key, boolean invalidateSections) {
        if (!window.contains(key)) {
            return;
        }
        LevelChunk chunk = loadedChunk(key);
        Column previous = columns.get(key);
        if (previous != null && previous.chunk != chunk) {
            columns.remove(key);
            if (!previous.sources.isEmpty()) {
                changed(key);
            }
            previous = null;
        }
        if (chunk == null || (previous != null && !invalidateSections)) {
            return;
        }
        Column column = previous == null ? new Column(key, chunk) : previous;
        columns.put(key, column);
        column.dirtySections.set(0, chunk.getSectionsCount());
        enqueue(column);
    }

    private LevelChunk loadedChunk(long key) {
        return level.getChunkSource().getChunk(ChunkPos.getX(key), ChunkPos.getZ(key), ChunkStatus.FULL, false);
    }

    private void enqueue(Column column) {
        if (!column.queued && !column.dirtySections.isEmpty()) {
            column.queued = true;
            int horizontalDistance = Math.max(Math.abs(ChunkPos.getX(column.key) - window.x),
                    Math.abs(ChunkPos.getZ(column.key) - window.z));
            int reference = column.referenceSection(viewSectionY);
            int sectionDistance = Math.abs(column.closestDirtySection(reference) - reference);
            column.priority = horizontalDistance * 4 + sectionDistance;
            if (column.preferredSection >= 0) {
                column.priority -= 48;
            }
            pending.add(column);
        }
    }

    private static boolean mayEmit(BlockState state) {
        return state.getLightEmission() > 0 || (!state.isAir() && CONTEXTUAL_EMISSION.get(state.getBlock().getClass()));
    }

    private List<TerrainLightVolume.Light> scanSection(Column column, int sectionIndex) {
        int startX = column.chunk.getPos().getMinBlockX();
        int startY = SectionPos.sectionToBlockCoord(column.chunk.getSectionYFromSectionIndex(sectionIndex));
        int startZ = column.chunk.getPos().getMinBlockZ();
        var section = column.chunk.getSection(sectionIndex);
        // Keep several nearby emitters per 4×4×4 subcell when lava or modded emissive blocks fill a section.
        int[] brightest = new int[64 * LIGHTS_PER_SUBCELL];
        for (int index = 0; index <= SECTION_CELL_MASK; index++) {
            int x = index & 15;
            int z = (index >> 4) & 15;
            int y = index >> 8;
            BlockState state = section.getBlockState(x, y, z);
            if (!mayEmit(state)) {
                continue;
            }
            cursor.set(startX + x, startY + y, startZ + z);
            int emission = Mth.clamp(state.getLightEmission(level, cursor), 0, 15);
            if (emission == 0) {
                continue;
            }
            int source = (emission << 12) | (SECTION_CELL_MASK - index);
            int offset = ((x >> 2) + 4 * ((z >> 2) + 4 * (y >> 2))) * LIGHTS_PER_SUBCELL;
            for (int slot = offset; slot < offset + LIGHTS_PER_SUBCELL; slot++) {
                if (source > brightest[slot]) {
                    int displaced = brightest[slot];
                    brightest[slot] = source;
                    source = displaced;
                }
                if (source == 0) {
                    break;
                }
            }
        }

        var sources = new ArrayList<TerrainLightVolume.Light>();
        for (int source : brightest) {
            if (source == 0) {
                continue;
            }
            int index = SECTION_CELL_MASK - (source & SECTION_CELL_MASK);
            int x = index & 15;
            int z = (index >> 4) & 15;
            int y = index >> 8;
            cursor.set(startX + x, startY + y, startZ + z);
            sources.add(TerrainLightVolume.lightAt(level, cursor, section.getBlockState(x, y, z), source >> 12));
        }
        return List.copyOf(sources);
    }

    private void publishSection(Column column, int index, List<TerrainLightVolume.Light> lights) {
        List<TerrainLightVolume.Light> previous = column.sections.getOrDefault(index, List.of());
        if (previous.equals(lights)) {
            return;
        }
        if (lights.isEmpty()) {
            column.sections.remove(index);
        } else {
            column.sections.put(index, lights);
        }
        var combined = new ArrayList<TerrainLightVolume.Light>();
        for (int section = 0; section < column.chunk.getSectionsCount(); section++) {
            combined.addAll(column.sections.getOrDefault(section, List.of()));
        }
        column.sources = List.copyOf(combined);
        changed(column.key);
    }

    private void changed(long key) {
        changes.add(key);
        revision++;
    }

    private static final class Column {
        private final long key;
        private final LevelChunk chunk;
        private final Map<Integer, List<TerrainLightVolume.Light>> sections = new HashMap<>();
        private final BitSet dirtySections = new BitSet();
        private List<TerrainLightVolume.Light> sources = List.of();
        private int preferredSection = -1;
        private int priority;
        private boolean queued;

        private Column(long key, LevelChunk chunk) {
            this.key = key;
            this.chunk = chunk;
        }

        private int referenceSection(int viewSectionY) {
            return Mth.clamp(chunk.getSectionIndexFromSectionY(viewSectionY), 0, chunk.getSectionsCount() - 1);
        }

        private int closestDirtySection(int reference) {
            if (preferredSection >= 0 && dirtySections.get(preferredSection)) {
                return preferredSection;
            }
            int upper = dirtySections.nextSetBit(Math.max(0, reference));
            int lower = dirtySections.previousSetBit(reference);
            return upper < 0 || (lower >= 0 && reference - lower <= upper - reference) ? lower : upper;
        }
    }

    private record Window(int x, int z, int radius) {
        private int minX() { return x - radius; }
        private int maxX() { return x + radius; }
        private int minZ() { return z - radius; }
        private int maxZ() { return z + radius; }

        private boolean contains(long key) {
            return contains(ChunkPos.getX(key), ChunkPos.getZ(key));
        }

        private boolean contains(int chunkX, int chunkZ) {
            return chunkX >= minX() && chunkX <= maxX() && chunkZ >= minZ() && chunkZ <= maxZ();
        }

        private boolean intersects(Window other) {
            return maxX() >= other.minX() && minX() <= other.maxX()
                    && maxZ() >= other.minZ() && minZ() <= other.maxZ();
        }
    }

    private static final class Discovery {
        private final Window window;
        private int ring;
        private int index;

        private Discovery(Window window) {
            this.window = window;
        }

        private long advance() {
            if (ring == 0) {
                ring++;
                return ChunkPos.asLong(window.x, window.z);
            }
            int edgeLength = ring * 2;
            int edge = index / edgeLength;
            int offset = index % edgeLength;
            int x = switch (edge) {
                case 0 -> -ring + offset;
                case 1 -> ring;
                case 2 -> ring - offset;
                default -> -ring;
            };
            int z = switch (edge) {
                case 0 -> -ring;
                case 1 -> -ring + offset;
                case 2 -> ring;
                default -> ring - offset;
            };
            long result = ChunkPos.asLong(window.x + x, window.z + z);
            if (++index == 8 * ring) {
                index = 0;
                ring++;
            }
            return result;
        }

        private boolean finished() {
            return ring > window.radius;
        }
    }
}

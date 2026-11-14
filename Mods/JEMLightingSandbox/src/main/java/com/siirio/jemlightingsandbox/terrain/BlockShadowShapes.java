package com.siirio.jemlightingsandbox.terrain;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/** A palette for the native block geometry visible to the terrain shadow rays. */
public final class BlockShadowShapes {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int TEXTURE_WIDTH = 64;
    public static final int TEXTURE_HEIGHT = 256;
    static final int EMPTY = 0;
    static final int FULL = 1;
    private static final int FOLIAGE = 13;
    private static final int FIRST_CUSTOM = 14;
    private static final int BOX_LIMIT = 8;
    private static final int CUTOUT_FACE_LIMIT = 8;
    private static final int CUTOUT_FACE_TEXELS = 7;
    private static final int MASK_SIZE = 16;
    private static final int FACE_LIMIT = 96;
    private static final int GRID_LIMIT = 4096;
    private static final int GRID_SCALE = 256;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int[][] AXIS_ORDERS = {
            {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
    };
    private static final Comparator<Box> LARGEST_FIRST = Comparator.comparingInt(Box::volume).reversed()
            .thenComparingInt(Box::minX).thenComparingInt(Box::minY).thenComparingInt(Box::minZ)
            .thenComparingInt(Box::maxX).thenComparingInt(Box::maxY).thenComparingInt(Box::maxZ);

    private final FloatBuffer texture = ByteBuffer.allocateDirect(TEXTURE_WIDTH * TEXTURE_HEIGHT * 4 * Float.BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final RandomSource random = RandomSource.create(42L);
    private final Map<BlockState, Integer> stateIds = new IdentityHashMap<>();
    private final Map<BlockState, Integer> cutoutIds = new IdentityHashMap<>();
    private final Map<List<Box>, Integer> ids = new HashMap<>();
    private int nextBoxId = FIRST_CUSTOM;
    private int nextCutoutId = TEXTURE_HEIGHT - 1;
    private FloatBuffer published;

    int id(BlockState state, ClientLevel level, BlockPos position) {
        if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) {
            return EMPTY;
        }
        boolean foliage = state.is(BlockTags.LEAVES);
        if (!foliage && state.isSolidRender(level, position)) {
            return FULL;
        }
        var block = state.getBlock();
        if (state.getRenderShape() == RenderShape.MODEL && !state.hasBlockEntity()) {
            Integer cutout = cutoutIds.get(state);
            if (cutout == null) {
                cutout = cutoutId(state, position, foliage);
                cutoutIds.put(state, cutout);
            }
            if (cutout != EMPTY) {
                return cutout;
            }
        }
        if (block.hasDynamicShape() || state.hasOffsetFunction() || state.hasBlockEntity()) {
            return nativeShape(state, level, position);
        }
        Integer known = stateIds.get(state);
        if (known != null) {
            return known;
        }
        int created = staticShape(state, level, position);
        stateIds.put(state, created);
        return created;
    }

    FloatBuffer texture() {
        if (published == null) {
            published = texture.asReadOnlyBuffer();
        }
        return published;
    }

    boolean isCutout(int id) {
        return id > nextCutoutId;
    }

    private int cutoutId(BlockState state, BlockPos position, boolean foliage) {
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        long seed = state.getSeed(position);
        random.setSeed(seed);
        var faces = new ArrayList<BakedQuad>();
        try {
            for (var layer : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                if (layer != RenderType.cutout() && layer != RenderType.cutoutMipped()
                        && !(foliage && layer == RenderType.solid())) {
                    continue;
                }
                for (Direction direction : DIRECTIONS) {
                    random.setSeed(seed);
                    faces.addAll(model.getQuads(state, direction, random, ModelData.EMPTY, layer));
                }
                random.setSeed(seed);
                faces.addAll(model.getQuads(state, null, random, ModelData.EMPTY, layer));
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not sample cutout model for {}", state, exception);
            return foliage ? FOLIAGE : EMPTY;
        }
        int stride = DefaultVertexFormat.BLOCK.getIntegerSize();
        if (faces.isEmpty() || faces.size() > CUTOUT_FACE_LIMIT || nextCutoutId < nextBoxId
                || faces.stream().anyMatch(face -> face.getVertices().length != 4 * stride)) {
            return foliage ? FOLIAGE : EMPTY;
        }
        int id = nextCutoutId--;
        int row = id * TEXTURE_WIDTH * 4;
        for (int face = 0; face < faces.size(); face++) {
            writeCutoutFace(row + face * CUTOUT_FACE_TEXELS * 4, faces.get(face), stride);
        }
        texture.put(row + (TEXTURE_WIDTH - 1) * 4, faces.size());
        texture.put(row + (TEXTURE_WIDTH - 1) * 4 + 1, foliage ? 0.64f : 0.30f);
        published = null;
        return id;
    }

    private void writeCutoutFace(int offset, BakedQuad face, int stride) {
        int[] vertices = face.getVertices();
        for (int axis = 0; axis < 3; axis++) {
            float start = Float.intBitsToFloat(vertices[axis]);
            texture.put(offset + axis, start);
            texture.put(offset + 4 + axis, Float.intBitsToFloat(vertices[stride + axis]) - start);
            texture.put(offset + 8 + axis, Float.intBitsToFloat(vertices[3 * stride + axis]) - start);
        }
        var sprite = face.getSprite();
        var image = sprite.contents();
        int frame = image.getUniqueFrames().findFirst().orElse(0);
        float u = Float.intBitsToFloat(vertices[4]);
        float v = Float.intBitsToFloat(vertices[5]);
        float us = Float.intBitsToFloat(vertices[stride + 4]) - u;
        float vs = Float.intBitsToFloat(vertices[stride + 5]) - v;
        float ut = Float.intBitsToFloat(vertices[3 * stride + 4]) - u;
        float vt = Float.intBitsToFloat(vertices[3 * stride + 5]) - v;
        for (int row = 0; row < MASK_SIZE; row++) {
            float t = (row + 0.5f) / MASK_SIZE;
            int mask = 0;
            for (int column = 0; column < MASK_SIZE; column++) {
                float s = (column + 0.5f) / MASK_SIZE;
                int x = spritePixel(u + s * us + t * ut, sprite.getU0(), sprite.getU1(), image.width());
                int y = spritePixel(v + s * vs + t * vt, sprite.getV0(), sprite.getV1(), image.height());
                if (!image.isTransparent(frame, x, y)) {
                    mask |= 1 << column;
                }
            }
            texture.put(offset + 12 + row, mask);
        }
    }

    private static int spritePixel(float uv, float min, float max, int size) {
        return Mth.clamp(Mth.floor((uv - min) * size / (max - min)), 0, size - 1);
    }

    private int staticShape(BlockState state, ClientLevel level, BlockPos position) {
        if (state.getRenderShape() == RenderShape.MODEL) {
            try {
                List<Box> boxes = modelShape(state, position);
                if (boxes != null) {
                    if (!state.canOcclude() && boxes.stream().anyMatch(box -> box.volume() == GRID_SCALE * GRID_SCALE * GRID_SCALE)) {
                        return EMPTY;
                    }
                    return paletteId(boxes);
                }
            } catch (RuntimeException exception) {
                LOGGER.debug("Using native outline for block shadow shape {}", state, exception);
            }
        }
        return nativeShape(state, level, position);
    }

    @Nullable
    private List<Box> modelShape(BlockState state, BlockPos position) {
        var model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        long seed = state.getSeed(position);
        random.setSeed(seed);
        var layers = model.getRenderTypes(state, random, ModelData.EMPTY);
        var faces = new ArrayList<Face>();
        boolean opaqueLayer = false;
        for (var layer : layers) {
            if (layer == RenderType.translucent()) {
                continue;
            }
            opaqueLayer = true;
            for (Direction direction : DIRECTIONS) {
                random.setSeed(seed);
                if (!appendFaces(faces, model.getQuads(state, direction, random, ModelData.EMPTY, layer))) {
                    return null;
                }
            }
            random.setSeed(seed);
            if (!appendFaces(faces, model.getQuads(state, null, random, ModelData.EMPTY, layer))) {
                return null;
            }
        }
        if (!opaqueLayer && !layers.isEmpty()) {
            return List.of();
        }
        List<Box> boxes = reconstruct(faces);
        return boxes.isEmpty() ? null : boxes;
    }

    private int nativeShape(BlockState state, ClientLevel level, BlockPos position) {
        var shape = state.getShape(level, position);
        if (shape.isEmpty()) {
            return EMPTY;
        }
        if (Block.isShapeFullBlock(shape)) {
            return state.canOcclude() ? FULL : EMPTY;
        }
        var boxes = new ArrayList<Box>();
        for (AABB bounds : shape.toAabbs()) {
            Box box = new Box(grid(bounds.minX), grid(bounds.minY), grid(bounds.minZ),
                    grid(bounds.maxX), grid(bounds.maxY), grid(bounds.maxZ));
            if (box.volume() > 0) {
                boxes.add(box);
            }
        }
        return paletteId(boxes);
    }

    private int paletteId(List<Box> boxes) {
        if (boxes.isEmpty()) {
            return EMPTY;
        }
        boxes.sort(LARGEST_FIRST);
        if (boxes.get(0).volume() == GRID_SCALE * GRID_SCALE * GRID_SCALE) {
            return FULL;
        }
        List<Box> key = List.copyOf(boxes.subList(0, Math.min(boxes.size(), BOX_LIMIT)));
        Integer existing = ids.get(key);
        if (existing != null) {
            return existing;
        }
        if (nextBoxId > nextCutoutId) {
            return EMPTY;
        }
        int id = nextBoxId++;
        ids.put(key, id);
        for (int index = 0; index < key.size(); index++) {
            Box box = key.get(index);
            int offset = (id * TEXTURE_WIDTH + index * 2) * 4;
            writeCorner(offset, box.minX, box.minY, box.minZ);
            writeCorner(offset + 4, box.maxX, box.maxY, box.maxZ);
        }
        published = null;
        return id;
    }

    private void writeCorner(int start, int x, int y, int z) {
        texture.put(start, (float) x / GRID_SCALE);
        texture.put(start + 1, (float) y / GRID_SCALE);
        texture.put(start + 2, (float) z / GRID_SCALE);
        texture.put(start + 3, 1F);
    }

    private static boolean appendFaces(List<Face> faces, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (faces.size() == FACE_LIMIT) {
                return false;
            }
            Face face = faceOf(quad);
            if (face == null) {
                return false;
            }
            faces.add(face);
        }
        return true;
    }

    @Nullable
    private static Face faceOf(BakedQuad quad) {
        int[] data = quad.getVertices();
        int stride = DefaultVertexFormat.BLOCK.getIntegerSize();
        if (data.length != 4 * stride) {
            return null;
        }
        int[] low = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] high = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int vertex = 0; vertex < 4; vertex++) {
            for (int axis = 0; axis < 3; axis++) {
                int value = Math.round(Float.intBitsToFloat(data[vertex * stride + axis]) * GRID_SCALE);
                low[axis] = Math.min(low[axis], value);
                high[axis] = Math.max(high[axis], value);
            }
        }
        Direction direction = quad.getDirection();
        int axis = direction.getAxis().ordinal();
        int first = (axis + 1) % 3;
        int second = (axis + 2) % 3;
        if (low[axis] != high[axis] || low[first] == high[first] || low[second] == high[second]) {
            return null;
        }
        int corners = 0;
        for (int vertex = 0; vertex < 4; vertex++) {
            int firstValue = Math.round(Float.intBitsToFloat(data[vertex * stride + first]) * GRID_SCALE);
            int secondValue = Math.round(Float.intBitsToFloat(data[vertex * stride + second]) * GRID_SCALE);
            if ((firstValue != low[first] && firstValue != high[first])
                    || (secondValue != low[second] && secondValue != high[second])) {
                return null;
            }
            int corner = (firstValue == high[first] ? 1 : 0) | (secondValue == high[second] ? 2 : 0);
            corners |= 1 << corner;
        }
        return corners == 15 ? new Face(axis, direction.getAxisDirection() == Direction.AxisDirection.POSITIVE,
                low[axis], low, high) : null;
    }

    private static List<Box> reconstruct(List<Face> faces) {
        if (faces.isEmpty()) {
            return List.of();
        }
        int[][] cuts = new int[3][];
        int[] size = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            var values = new TreeSet<Integer>();
            for (Face face : faces) {
                values.add(Mth.clamp(face.low[axis], 0, GRID_SCALE));
                values.add(Mth.clamp(face.high[axis], 0, GRID_SCALE));
            }
            cuts[axis] = values.stream().mapToInt(Integer::intValue).toArray();
            size[axis] = cuts[axis].length - 1;
        }
        int count = size[0] * size[1] * size[2];
        if (count <= 0 || count > GRID_LIMIT) {
            return List.of();
        }
        boolean[] occupied = new boolean[count];
        int[] center = new int[3];
        for (int z = 0; z < size[2]; z++) {
            center[2] = cuts[2][z] + cuts[2][z + 1];
            for (int y = 0; y < size[1]; y++) {
                center[1] = cuts[1][y] + cuts[1][y + 1];
                for (int x = 0; x < size[0]; x++) {
                    center[0] = cuts[0][x] + cuts[0][x + 1];
                    occupied[cellIndex(size, x, y, z)] = inside(faces, center);
                }
            }
        }
        return pack(cuts, size, occupied);
    }

    private static boolean inside(List<Face> faces, int[] center) {
        int[] below = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int[] above = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] belowDirection = new int[3];
        int[] aboveDirection = new int[3];
        for (Face face : faces) {
            int axis = face.axis;
            int first = (axis + 1) % 3;
            int second = (axis + 2) % 3;
            if (center[first] <= 2 * face.low[first] || center[first] >= 2 * face.high[first]
                    || center[second] <= 2 * face.low[second] || center[second] >= 2 * face.high[second]) {
                continue;
            }
            int plane = 2 * face.plane;
            if (plane < center[axis] && plane >= below[axis]) {
                if (plane > below[axis] || !face.positive) {
                    belowDirection[axis] = face.positive ? 1 : -1;
                }
                below[axis] = plane;
            } else if (plane > center[axis] && plane <= above[axis]) {
                if (plane < above[axis] || face.positive) {
                    aboveDirection[axis] = face.positive ? 1 : -1;
                }
                above[axis] = plane;
            }
        }
        for (int axis = 0; axis < 3; axis++) {
            if (belowDirection[axis] == -1 && aboveDirection[axis] == 1) {
                return true;
            }
        }
        return false;
    }

    private static List<Box> pack(int[][] cuts, int[] size, boolean[] occupied) {
        var boxes = new ArrayList<Box>();
        boolean[] uncovered = occupied.clone();
        for (int index = 0; index < uncovered.length; index++) {
            if (!uncovered[index]) {
                continue;
            }
            int[] seed = {index % size[0], index / size[0] % size[1], index / (size[0] * size[1])};
            int bestVolume = -1;
            int[] bestLow = seed;
            int[] bestHigh = seed;
            for (int[] order : AXIS_ORDERS) {
                int[] low = seed.clone();
                int[] high = seed.clone();
                for (int axis : order) {
                    while (low[axis] > 0 && hasLayer(occupied, size, low, high, axis, low[axis] - 1)) {
                        low[axis]--;
                    }
                    while (high[axis] + 1 < size[axis] && hasLayer(occupied, size, low, high, axis, high[axis] + 1)) {
                        high[axis]++;
                    }
                }
                int volume = (cuts[0][high[0] + 1] - cuts[0][low[0]]) * (cuts[1][high[1] + 1] - cuts[1][low[1]])
                        * (cuts[2][high[2] + 1] - cuts[2][low[2]]);
                if (volume > bestVolume) {
                    bestVolume = volume;
                    bestLow = low;
                    bestHigh = high;
                }
            }
            for (int z = bestLow[2]; z <= bestHigh[2]; z++) {
                for (int y = bestLow[1]; y <= bestHigh[1]; y++) {
                    for (int x = bestLow[0]; x <= bestHigh[0]; x++) {
                        uncovered[cellIndex(size, x, y, z)] = false;
                    }
                }
            }
            boxes.add(new Box(cuts[0][bestLow[0]], cuts[1][bestLow[1]], cuts[2][bestLow[2]],
                    cuts[0][bestHigh[0] + 1], cuts[1][bestHigh[1] + 1], cuts[2][bestHigh[2] + 1]));
        }
        return boxes;
    }

    private static boolean hasLayer(boolean[] occupied, int[] size, int[] low, int[] high, int axis, int layer) {
        int left = axis == 0 ? layer : low[0];
        int right = axis == 0 ? layer : high[0];
        int bottom = axis == 1 ? layer : low[1];
        int top = axis == 1 ? layer : high[1];
        int near = axis == 2 ? layer : low[2];
        int far = axis == 2 ? layer : high[2];
        for (int z = near; z <= far; z++) {
            for (int y = bottom; y <= top; y++) {
                for (int x = left; x <= right; x++) {
                    if (!occupied[cellIndex(size, x, y, z)]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int cellIndex(int[] size, int x, int y, int z) {
        return x + size[0] * (y + size[1] * z);
    }

    private static int grid(double value) {
        return Mth.clamp((int) Math.round(value * GRID_SCALE), 0, GRID_SCALE);
    }

    private record Face(int axis, boolean positive, int plane, int[] low, int[] high) {}

    private record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private int volume() {
            return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
        }
    }
}

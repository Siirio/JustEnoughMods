package dev.taste.engine.light;

import dev.taste.engine.effects.VegetationMotion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import dev.taste.engine.material.MaterialResolver;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.client.model.data.ModelData;

/** Re-emits static Forge model geometry from a loaded client section. */
public final class VoxelSectionGeometry {
    public static final int VERTEX_FLOATS = 12;
    public static final int QUAD_VERTICES = 4;
    static final int MAX_QUADS = 24_576;
    private static final int QUAD_FLOATS = VERTEX_FLOATS * QUAD_VERTICES;

    public record Coordinate(int x, int y, int z) {
        public long key() { return SectionPos.asLong(x, y, z); }
        public int blockX() { return x * 16; }
        public int blockY() { return y * 16; }
        public int blockZ() { return z * 16; }
    }

    public record Surface(ResourceLocation sprite, MaterialQuery.Layer layer, int tintIndex, MaterialDescription material) { }

    /** Vertex order is position XYZ, normalized sky light, atlas UV, RGBA, packed block light U, native emission. */
    public static final class Mesh {
        private final float[] vertices;
        private final List<Surface> surfaces;
        private final float[] ambient;
        private final int[] rawColor;
        private final byte[] wind;

        private Mesh(float[] vertices, List<Surface> surfaces, float[] ambient, int[] rawColor, byte[] wind) {
            this.vertices = vertices;
            this.surfaces = List.copyOf(surfaces);
            this.ambient = ambient;
            this.rawColor = rawColor;
            this.wind = wind;
        }

        public int quads() { return surfaces.size(); }
        public Surface surface(int quad) { return surfaces.get(quad); }
        public FloatBuffer vertices() { return FloatBuffer.wrap(vertices).asReadOnlyBuffer(); }
        public float ambient(int vertex) { return ambient[vertex]; }
        public int rawColor(int vertex) { return rawColor[vertex]; }
        public int wind(int vertex) { return Byte.toUnsignedInt(wind[vertex]); }
        public long estimatedBytes() {
            return (long) (vertices.length + ambient.length + rawColor.length) * Float.BYTES
                    + wind.length + (long) surfaces.size() * 8L;
        }
    }

    private record MaterialKey(BlockState state, ResourceLocation sprite, MaterialQuery.Layer layer, int tintIndex) { }

    /** Returns null when a section is absent or its model emission exceeds the bounded capture. */
    public Mesh emit(ClientLevel level, Coordinate coordinate, MaterialResolver materials) {
        int sectionIndex = coordinate.y() - level.getMinSection();
        if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) return null;
        var chunk = level.getChunkSource().getChunk(coordinate.x(), coordinate.z(), false);
        if (chunk == null) return null;
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        Sink sink = new Sink(materials);
        if (section.hasOnlyAir()) return sink.finish();
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create(0L);
        PoseStack pose = new PoseStack();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (state.getRenderShape() != RenderShape.MODEL || state.isAir()) continue;
                    position.set(coordinate.blockX() + x, coordinate.blockY() + y, coordinate.blockZ() + z);
                    BakedModel model = blocks.getBlockModel(state);
                    ModelData base = level.getModelDataManager().getAt(position);
                    ModelData data = model.getModelData(level, position, state, base == null ? ModelData.EMPTY : base);
                    random.setSeed(state.getSeed(position));
                    pose.pushPose();
                    try {
                        pose.translate(x, y, z);
                        for (RenderType type : model.getRenderTypes(state, random, data)) {
                            MaterialQuery.Layer layer = layer(type);
                            if (layer == null) continue;
                            sink.begin(state, layer, state.getLightEmission(level, position) / 15.0F, y);
                            blocks.renderBatched(state, position, level, pose, sink, true, random, data, type);
                            if (sink.overflow) return null;
                        }
                    } finally {
                        pose.popPose();
                    }
                }
            }
        }
        return sink.finish();
    }

    private static MaterialQuery.Layer layer(RenderType type) {
        if (type == RenderType.solid()) return MaterialQuery.Layer.SOLID;
        if (type == RenderType.cutout()) return MaterialQuery.Layer.CUTOUT;
        if (type == RenderType.cutoutMipped()) return MaterialQuery.Layer.CUTOUT_MIPPED;
        return null;
    }

    private static final class Sink implements VertexConsumer {
        private final MaterialResolver materials;
        private final Map<MaterialKey, Surface> materialCache = new HashMap<>();
        private final List<Surface> surfaces = new ArrayList<>();
        private float[] output = new float[0];
        private final float[] quad = new float[QUAD_FLOATS];
        private final float[] quadAmbient = new float[QUAD_VERTICES];
        private final int[] quadRawColor = new int[QUAD_VERTICES];
        private final byte[] quadWind = new byte[QUAD_VERTICES];
        private float[] ambient = new float[0];
        private int[] rawColor = new int[0];
        private byte[] wind = new byte[0];
        private int vertices;
        private boolean overflow;
        private BlockState state;
        private MaterialQuery.Layer layer;
        private Surface surface;
        private float x, y, z, u, v, sky, block, emission;
        private float baseY;
        private float red = 1.0F, green = 1.0F, blue = 1.0F, alpha = 1.0F;

        Sink(MaterialResolver materials) { this.materials = materials; }

        void begin(BlockState state, MaterialQuery.Layer layer, float emission, float baseY) {
            this.state = state;
            this.layer = layer;
            this.emission = emission;
            this.baseY = baseY;
        }

        Mesh finish() {
            int count = surfaces.size();
            return new Mesh(Arrays.copyOf(output, count * QUAD_FLOATS), surfaces,
                    Arrays.copyOf(ambient, count * QUAD_VERTICES), Arrays.copyOf(rawColor, count * QUAD_VERTICES),
                    Arrays.copyOf(wind, count * QUAD_VERTICES));
        }

        @Override public void putBulkData(PoseStack.Pose pose, BakedQuad baked, float[] brightness,
                float red, float green, float blue, float alpha, int[] lights, int overlay, boolean useBakedColor) {
            if (overflow) return;
            ResourceLocation sprite = baked.getSprite().contents().name();
            MaterialKey key = new MaterialKey(state, sprite, layer, baked.getTintIndex());
            surface = materialCache.computeIfAbsent(key, item -> new Surface(item.sprite(), item.layer(),
                    item.tintIndex(), materials.resolve(MaterialQuery.block(item.state(), item.sprite(),
                            item.layer(), item.tintIndex()))));
            int[] bakedVertices = baked.getVertices();
            for (int vertex = 0; vertex < QUAD_VERTICES; vertex++) {
                int packed = useBakedColor ? bakedVertices[vertex * 8 + 3] : 0xFFFFFFFF;
                int r = channel(red, packed);
                int g = channel(green, packed >>> 8);
                int b = channel(blue, packed >>> 16);
                quadAmbient[vertex] = brightness[vertex];
                quadRawColor[vertex] = r << 16 | g << 8 | b;
            }
            try {
                VertexConsumer.super.putBulkData(pose, baked, brightness, red, green, blue, alpha,
                        lights, overlay, useBakedColor);
            } finally {
                vertices = 0;
                surface = null;
            }
        }

        private static int channel(float tint, int color) {
            return Math.max(0, Math.min(255, Math.round(tint * (color & 255))));
        }

        @Override public VertexConsumer vertex(double x, double y, double z) {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            return this;
        }

        @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.red = red / 255.0F;
            this.green = green / 255.0F;
            this.blue = blue / 255.0F;
            this.alpha = alpha / 255.0F;
            return this;
        }

        @Override public VertexConsumer uv(float u, float v) { this.u = u; this.v = v; return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { block = u; sky = v / 240.0F; return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void defaultColor(int red, int green, int blue, int alpha) { color(red, green, blue, alpha); }
        @Override public void unsetDefaultColor() { color(255, 255, 255, 255); }

        @Override public void endVertex() {
            if (surface == null || overflow) return;
            quadWind[vertices] = (byte) VegetationMotion.vertex(state, y - baseY);
            int index = vertices++ * VERTEX_FLOATS;
            quad[index] = x;
            quad[index + 1] = y;
            quad[index + 2] = z;
            quad[index + 3] = sky;
            quad[index + 4] = u;
            quad[index + 5] = v;
            quad[index + 6] = red;
            quad[index + 7] = green;
            quad[index + 8] = blue;
            quad[index + 9] = alpha;
            quad[index + 10] = block;
            quad[index + 11] = emission;
            if (vertices < QUAD_VERTICES) return;
            if (surfaces.size() >= MAX_QUADS) {
                overflow = true;
                return;
            }
            int offset = surfaces.size() * QUAD_FLOATS;
            if (offset + QUAD_FLOATS > output.length) {
                int grown = Math.max(QUAD_FLOATS * 64, output.length * 2);
                output = Arrays.copyOf(output, Math.min(MAX_QUADS * QUAD_FLOATS, grown));
                ambient = Arrays.copyOf(ambient, output.length / VERTEX_FLOATS);
                rawColor = Arrays.copyOf(rawColor, output.length / VERTEX_FLOATS);
                wind = Arrays.copyOf(wind, output.length / VERTEX_FLOATS);
            }
            System.arraycopy(quad, 0, output, offset, QUAD_FLOATS);
            int vertexOffset = offset / VERTEX_FLOATS;
            System.arraycopy(quadAmbient, 0, ambient, vertexOffset, QUAD_VERTICES);
            System.arraycopy(quadRawColor, 0, rawColor, vertexOffset, QUAD_VERTICES);
            System.arraycopy(quadWind, 0, wind, vertexOffset, QUAD_VERTICES);
            surfaces.add(surface);
            vertices = 0;
        }
    }
}

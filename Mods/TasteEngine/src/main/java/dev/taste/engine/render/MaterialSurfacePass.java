package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.taste.engine.client.ClientState;
import dev.taste.engine.light.ShadowRenderer;
import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import dev.taste.engine.material.MaterialResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.Tags;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Renders material identity and geometry from the native baked model and fluid emitters. */
public final class MaterialSurfacePass implements AutoCloseable {
    private static final int WATER = 1;
    private static final int GLASS = 2;
    private static final int LAVA = 3;
    private static final int ICE = 4;
    private static final int EMISSIVE = 5;
    private static final int FIRE = 6;
    private static final int ORE = 7;
    private static final int PORTAL = 8;
    private static final int SOUL = 9;
    private static final int SECTION_RADIUS = 5;
    private static final int MAX_SECTIONS = 512;
    private static final int SECTION_BUILDS = 3;
    private static final long MAX_CACHE_BYTES = 64L * 1024 * 1024;
    private static final String TARGET = "material-surface";
    public static final String SURFACE_OPTICS_TARGET = "material-surface-optics";
    public static final String GLASS_SHADOW_TARGET = "glass-shadow-transmission";
    public static final String GLASS_SHADOW_SECOND_TARGET = "glass-shadow-transmission-second";
    private static final int[] SURFACE_ATTACHMENTS = {GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1};
    private static final float[] ZERO = {0, 0, 0, 0};
    private record Coordinate(int x, int y, int z, float distance) {
        long key() { return SectionPos.asLong(x, y, z); }
    }
    private record Mesh(LevelChunkSection section, VertexBuffer vertices, long bytes) { }
    private record Visible(Coordinate coordinate, float x, float y, float z, Mesh mesh) { }
    private record Cascade(int index, Matrix4f projection, List<Visible> meshes) { }
    private record BlockMaterial(BlockState state, ResourceLocation sprite, MaterialQuery.Layer layer, int tintIndex) { }
    private record FluidMaterial(FluidState state, ResourceLocation sprite, MaterialQuery.Layer layer) { }
    private record SurfaceStyle(int family, int payload, int colorRgb, int cutoff, int optics) { }
    private static final SurfaceStyle NO_SURFACE = new SurfaceStyle(0, 0, -1, 0, 0);

    private final Map<Long, Mesh> meshes = new LinkedHashMap<>(32, 0.75F, true);
    private final Map<BlockState, Boolean> blockCandidates = new HashMap<>();
    private final Map<FluidState, Boolean> fluidCandidates = new HashMap<>();
    private final Map<BlockMaterial, MaterialDescription> blockMaterials = new HashMap<>();
    private final Map<FluidMaterial, MaterialDescription> fluidMaterials = new HashMap<>();
    private final BufferBuilder builder = new BufferBuilder(65_536);
    private final MeshConsumer consumer = new MeshConsumer(builder);
    private final RandomSource random = RandomSource.create();
    private long worldEpoch = -1;
    private long buildFrame = -1;
    private int worldBuilds;
    private int shadowBuilds;
    private long cacheBytes;
    private ShaderInstance shader;
    private ShaderInstance transmissionShader;
    private TextureTarget opticsTarget;

    public List<TextureTarget> drawGlassTransmission(TargetPool targets, FrameSnapshot frame, MaterialResolver materials,
            List<ShadowRenderer.MapLayer> shadows) throws IOException {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || shadows.isEmpty()) {
            targets.release(GLASS_SHADOW_TARGET);
            targets.release(GLASS_SHADOW_SECOND_TARGET);
            return List.of();
        }
        if (frame.worldEpoch() != worldEpoch) {
            clearMeshes();
            worldEpoch = frame.worldEpoch();
        }
        List<Cascade> cascades = new ArrayList<>();
        for (int index = 0; index < Math.min(4, shadows.size()); index++) {
            Matrix4f projection = shadows.get(index).view().projection();
            List<Visible> visible = visibleMeshes(level, frame, materials, projection, true);
            if (!visible.isEmpty()) cascades.add(new Cascade(index, projection, visible));
        }
        if (cascades.isEmpty()) {
            targets.release(GLASS_SHADOW_TARGET);
            targets.release(GLASS_SHADOW_SECOND_TARGET);
            return List.of();
        }
        if (transmissionShader == null) {
            transmissionShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation("taste", "taste_glass_transmission"), DefaultVertexFormat.NEW_ENTITY);
        }
        int size = Math.min(768, shadows.get(0).resolution());
        TextureTarget first = targets.acquire(new TargetDescriptor(GLASS_SHADOW_TARGET, size * 2, size * 2,
                TargetDescriptor.Format.RGBA16F, true, 1, 1, 1));
        TextureTarget second = targets.acquire(new TargetDescriptor(GLASS_SHADOW_SECOND_TARGET, size * 2, size * 2,
                TargetDescriptor.Format.RGBA16F, true, 1, 1, 1));
        drawTransmissionLayer(first, null, size, cascades);
        drawTransmissionLayer(second, first, size, cascades);
        return List.of(first, second);
    }

    private void drawTransmissionLayer(TextureTarget atlas, TextureTarget first, int size, List<Cascade> cascades) {
        RenderSystem.disableScissor();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        atlas.setClearColor(1, 1, 1, 1);
        atlas.clear(Minecraft.ON_OSX);
        atlas.bindWrite(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        BufferUploader.invalidate();
        int blockAtlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
        transmissionShader.setSampler("BlockAtlas", blockAtlas);
        transmissionShader.setSampler("FirstDepth", first == null ? blockAtlas : first.getDepthTextureId());
        transmissionShader.getUniform("PeelLayer").set(first == null ? 0F : 1F);
        Uniform projection = transmissionShader.getUniform("ViewProjection");
        Uniform sectionOrigin = transmissionShader.getUniform("SectionOrigin");
        int previousWinding = GL11.glGetInteger(GL11.GL_FRONT_FACE);
        try {
            GL11.glFrontFace(GL11.GL_CCW);
            transmissionShader.apply();
            for (Cascade cascade : cascades) {
                int index = cascade.index();
                RenderSystem.viewport(index % 2 * size, index / 2 * size, size, size);
                projection.set(cascade.projection());
                projection.upload();
                for (Visible item : cascade.meshes()) {
                    if (item.mesh().vertices().isInvalid()) continue;
                    sectionOrigin.set(item.x(), item.y(), item.z());
                    sectionOrigin.upload();
                    item.mesh().vertices().bind();
                    item.mesh().vertices().draw();
                }
            }
        } finally {
            VertexBuffer.unbind();
            transmissionShader.clear();
            GL11.glFrontFace(previousWinding);
        }
    }

    /** R stores IOR, G stores opacity and A marks a rasterized material surface. */
    public TextureTarget surfaceOptics() { return opticsTarget; }
    public TextureTarget draw(TargetPool targets, FrameSnapshot frame, PipelineParameters parameters,
            MaterialResolver materials) throws IOException {
        opticsTarget = null;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        boolean water = parameters.value("water.enabled").getAsBoolean();
        boolean glass = parameters.value("glass.enhanced").getAsBoolean();
        boolean lava = parameters.value("lava.enabled").getAsBoolean();
        boolean emission = parameters.value("emission.enabled").getAsBoolean();
        if (level == null || !(water || glass || lava || emission)) {
            targets.release(TARGET);
            targets.release(SURFACE_OPTICS_TARGET);
            if (level == null || !parameters.value("shadows.coloredGlass").getAsBoolean()) clearMeshes();
            return null;
        }
        if (frame.worldEpoch() != worldEpoch) {
            clearMeshes();
            worldEpoch = frame.worldEpoch();
        }
        Matrix4f viewProjection = matrix(frame.projection()).mul(matrix(frame.view()));
        List<Visible> visible = visibleMeshes(level, frame, materials, viewProjection, false);
        if (visible.isEmpty()) {
            targets.release(TARGET);
            targets.release(SURFACE_OPTICS_TARGET);
            return null;
        }
        if (shader == null) {
            shader = new ShaderInstance(minecraft.getResourceManager(),
                    new ResourceLocation("taste", "taste_surface_metadata"), DefaultVertexFormat.NEW_ENTITY);
        }
        TextureTarget target = targets.acquire(new TargetDescriptor(TARGET, frame.width(), frame.height(),
                TargetDescriptor.Format.RGBA32F, true, 1, 1, 1));
        TextureTarget optics = targets.acquire(new TargetDescriptor(SURFACE_OPTICS_TARGET, frame.width(), frame.height(),
                TargetDescriptor.Format.RGBA16F, false, 1, 1, 1));
        RenderSystem.disableScissor();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1,
                GL11.GL_TEXTURE_2D, optics.getColorTextureId(), 0);
        GL20.glDrawBuffers(SURFACE_ATTACHMENTS);
        if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            detachOptics();
            throw new IOException("Surface optics framebuffer is incomplete");
        }
        GL30.glClearBufferfv(GL11.GL_COLOR, 1, ZERO);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        BufferUploader.invalidate();
        shader.setSampler("BlockAtlas", minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
        shader.getUniform("ViewProjection").set(viewProjection);
        shader.getUniform("ActiveFamilies").set(water ? 1F : 0F, glass ? 1F : 0F, lava ? 1F : 0F, emission ? 1F : 0F);
        shader.getUniform("EmissionSelectors").set(parameters.value("emission.fire").getAsBoolean() ? 1F : 0F,
                parameters.value("emission.ores").getAsBoolean() ? 1F : 0F,
                parameters.value("emission.portals").getAsBoolean() ? 1F : 0F);
        Uniform sectionOrigin = shader.getUniform("SectionOrigin");
        try {
            shader.apply();
            for (Visible item : visible) {
                sectionOrigin.set(item.x(), item.y(), item.z());
                sectionOrigin.upload();
                item.mesh().vertices().bind();
                item.mesh().vertices().draw();
            }
        } finally {
            VertexBuffer.unbind();
            shader.clear();
            detachOptics();
        }
        opticsTarget = optics;
        return target;
    }

    private static void detachOptics() {
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, 0, 0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
    }

    private List<Visible> visibleMeshes(ClientLevel level, FrameSnapshot frame, MaterialResolver materials,
            Matrix4f viewProjection, boolean shadow) {
        if (buildFrame != frame.frameIndex()) {
            buildFrame = frame.frameIndex();
            worldBuilds = 0;
            shadowBuilds = 0;
        }
        FrameSnapshot.Vec3d eye = frame.cameraWorld();
        FrameSnapshot.Vec3d origin = frame.origin();
        int centerX = SectionPos.blockToSectionCoord(eye.x());
        int centerY = SectionPos.blockToSectionCoord(eye.y());
        int centerZ = SectionPos.blockToSectionCoord(eye.z());
        FrustumIntersection frustum = new FrustumIntersection(viewProjection);
        List<Coordinate> coordinates = new ArrayList<>();
        for (int z = centerZ - SECTION_RADIUS; z <= centerZ + SECTION_RADIUS; z++) {
            for (int x = centerX - SECTION_RADIUS; x <= centerX + SECTION_RADIUS; x++) {
                int minY = Math.max(level.getMinSection(), centerY - SECTION_RADIUS);
                int maxY = Math.min(level.getMaxSection() - 1, centerY + SECTION_RADIUS);
                for (int y = minY; y <= maxY; y++) {
                    float px = (float) (x * 16 - origin.x());
                    float py = (float) (y * 16 - origin.y());
                    float pz = (float) (z * 16 - origin.z());
                    if (!frustum.testAab(px, py, pz, px + 16, py + 16, pz + 16)) continue;
                    double dx = x * 16 + 8 - eye.x();
                    double dy = y * 16 + 8 - eye.y();
                    double dz = z * 16 + 8 - eye.z();
                    coordinates.add(new Coordinate(x, y, z, (float) (dx * dx + dy * dy + dz * dz)));
                }
            }
        }
        coordinates.sort(Comparator.comparingDouble(Coordinate::distance));
        List<Visible> visible = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            LevelChunk chunk = level.getChunkSource().getChunk(coordinate.x(), coordinate.z(), ChunkStatus.FULL, false);
            if (chunk == null) {
                remove(coordinate.key());
                continue;
            }
            LevelChunkSection section = chunk.getSections()[coordinate.y() - level.getMinSection()];
            Mesh mesh = meshes.get(coordinate.key());
            if (mesh == null || mesh.section() != section) {
                if ((shadow ? shadowBuilds : worldBuilds) >= SECTION_BUILDS) continue;
                remove(coordinate.key());
                boolean hasSurfaces = !section.hasOnlyAir() && section.maybeHas(state ->
                        blockCandidate(state, materials) || fluidCandidate(state.getFluidState(), materials));
                if (hasSurfaces) {
                    mesh = build(level, section, coordinate, materials);
                    if (shadow) shadowBuilds++;
                    else worldBuilds++;
                } else {
                    mesh = new Mesh(section, null, 0);
                }
                meshes.put(coordinate.key(), mesh);
                cacheBytes += mesh.bytes();
            }
            if (mesh.vertices() != null) {
                visible.add(new Visible(coordinate, (float) (coordinate.x() * 16 - origin.x()),
                        (float) (coordinate.y() * 16 - origin.y()), (float) (coordinate.z() * 16 - origin.z()), mesh));
            }
        }
        while (meshes.size() > MAX_SECTIONS || cacheBytes > MAX_CACHE_BYTES)
            remove(meshes.keySet().iterator().next());
        visible.removeIf(item -> item.mesh().vertices().isInvalid());
        return visible;
    }

    private Mesh build(ClientLevel level, LevelChunkSection section, Coordinate coordinate, MaterialResolver materials) {
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        PoseStack pose = new PoseStack();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        float mipCutoff = ClientState.embeddiumInstalled() ? 0.1F : 0.5F;
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        try {
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockState state = section.getBlockState(x, y, z);
                if (state.isAir()) continue;
                position.set(coordinate.x() * 16 + x, coordinate.y() * 16 + y, coordinate.z() * 16 + z);
                FluidState fluid = state.getFluidState();
                if (fluidCandidate(fluid, materials)) {
                    MaterialQuery.Layer layer = layer(ItemBlockRenderTypes.getRenderLayer(fluid));
                    if (layer != null) {
                        consumer.fluid(materials, level, position, fluid, layer, mipCutoff);
                        blocks.renderLiquid(position, level, consumer, state, fluid);
                    }
                }
                if (!blockCandidate(state, materials)) continue;
                BakedModel model = blocks.getBlockModel(state);
                ModelData base = level.getModelDataManager().getAt(position);
                ModelData modelData = model.getModelData(level, position, state, base == null ? ModelData.EMPTY : base);
                random.setSeed(state.getSeed(position));
                pose.pushPose();
                try {
                    pose.translate(x, y, z);
                    for (RenderType renderType : model.getRenderTypes(state, random, modelData)) {
                        MaterialQuery.Layer layer = layer(renderType);
                        if (layer == null) continue;
                        consumer.block(materials, level, position, state, layer, mipCutoff);
                        blocks.renderBatched(state, position, level, pose, consumer, true, random, modelData, renderType);
                    }
                } finally {
                    pose.popPose();
                }
            }
        } catch (RuntimeException failure) {
            BufferBuilder.RenderedBuffer unused = builder.endOrDiscardIfEmpty();
            if (unused != null) unused.release();
            consumer.clear();
            throw failure;
        }
        consumer.clear();
        BufferBuilder.RenderedBuffer rendered = builder.endOrDiscardIfEmpty();
        if (rendered == null) return new Mesh(section, null, 0);
        long bytes = rendered.vertexBuffer().remaining();
        VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            vbo.bind();
            vbo.upload(rendered);
            return new Mesh(section, vbo, bytes);
        } catch (RuntimeException failure) {
            vbo.close();
            throw failure;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private boolean blockCandidate(BlockState state, MaterialResolver materials) {
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) return false;
        return blockCandidates.computeIfAbsent(state, key -> {
            MaterialQuery coarse = MaterialQuery.block(key, null, null, -1);
            return family(key, materials.resolve(coarse)) != 0 || materials.mayClassifySurface(coarse);
        });
    }

    private boolean fluidCandidate(FluidState state, MaterialResolver materials) {
        if (state.isEmpty()) return false;
        return fluidCandidates.computeIfAbsent(state, key -> {
            MaterialQuery.Layer layer = layer(ItemBlockRenderTypes.getRenderLayer(key));
            if (layer == null) return false;
            MaterialQuery coarse = MaterialQuery.fluid(key, null, layer, -1);
            return family(null, materials.resolve(coarse)) != 0 || materials.mayClassifySurface(coarse);
        });
    }

    private MaterialDescription blockMaterial(MaterialResolver materials, BlockState state, ResourceLocation sprite,
            MaterialQuery.Layer layer, int tintIndex) {
        BlockMaterial key = new BlockMaterial(state, sprite, layer, tintIndex);
        return blockMaterials.computeIfAbsent(key, value -> materials.resolve(
                MaterialQuery.block(value.state(), value.sprite(), value.layer(), value.tintIndex())));
    }

    private MaterialDescription fluidMaterial(MaterialResolver materials, FluidState state, ResourceLocation sprite,
            MaterialQuery.Layer layer) {
        FluidMaterial key = new FluidMaterial(state, sprite, layer);
        return fluidMaterials.computeIfAbsent(key, value -> materials.resolve(
                MaterialQuery.fluid(value.state(), value.sprite(), value.layer(), -1)));
    }

    private static int family(BlockState state, MaterialDescription material) {
        MaterialDescription.Flags flags = material.flags();
        if (flags.water()) return WATER;
        if (flags.lava()) return LAVA;
        if (flags.ice()) return ICE;
        if (flags.glass()) return GLASS;
        if (material.metadata().family() == MaterialDescription.Family.PORTAL) return PORTAL;
        if (state != null) {
            if (state.is(Blocks.FIRE)) return FIRE;
            if (state.is(Blocks.NETHER_PORTAL)) return PORTAL;
            if (state.is(Blocks.SOUL_FIRE)) return SOUL;
        }
        if (material.emission() <= 0) return 0;
        if (state != null) {
            if (state.is(Tags.Blocks.ORES)) return ORE;
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id.getNamespace().equals("minecraft") && id.getPath().startsWith("soul_")) return SOUL;
        }
        return EMISSIVE;
    }

    private static SurfaceStyle style(BlockState state, MaterialDescription material, MaterialQuery.Layer layer,
            float mipCutoff, int flow, ClientLevel level, BlockPos position) {
        int family = family(state, material);
        if (family == 0) return NO_SURFACE;
        int payload = 0;
        int colorRgb = -1;
        MaterialDescription.Metadata optics = material.metadata();
        if (family == WATER || family == LAVA) {
            payload = flow;
        } else if (family == GLASS || family == ICE) {
            if (optics.rainDroplets() && level.dimensionType().hasSkyLight()
                    && level.getHeight(Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ()) <= position.getY() + 1) {
                payload |= 1;
            }
            if (optics.castTransmission()) {
                payload |= 2 | (Math.min(1023, Math.round(optics.thickness() * 128)) << 2);
                if (optics.absorptionColorRgb() != null) colorRgb = optics.absorptionColorRgb();
            }
        } else if (family >= EMISSIVE) {
            float strength = material.emission();
            if (strength == 0 && state != null) {
                if (state.is(Blocks.NETHER_PORTAL)) strength = 0.8F;
                else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) strength = 1.0F;
            }
            payload = Math.min(65_535, Math.round(strength * 256));
            if (optics.emissionColorRgb() != null) colorRgb = optics.emissionColorRgb();
        }
        float threshold = switch (layer) {
            case SOLID -> 0.0F;
            case CUTOUT -> Math.max(0.1F, optics.cutoutThreshold());
            case CUTOUT_MIPPED -> Math.max(mipCutoff, optics.cutoutThreshold());
            default -> 0.004F;
        };
        int ior = Math.round((material.ior() - 1.0F) * 127.5F);
        int opacity = Math.round(optics.opacity() * 255.0F);
        int packedOptics = ior | (opacity << 8);
        return new SurfaceStyle(family, payload, colorRgb, Math.min(65_535, Math.round(threshold * 65_536)), packedOptics);
    }

    private static MaterialQuery.Layer layer(RenderType type) {
        if (type == RenderType.solid()) return MaterialQuery.Layer.SOLID;
        if (type == RenderType.cutoutMipped()) return MaterialQuery.Layer.CUTOUT_MIPPED;
        if (type == RenderType.cutout()) return MaterialQuery.Layer.CUTOUT;
        if (type == RenderType.translucent()) return MaterialQuery.Layer.TRANSLUCENT;
        return null;
    }
    private static int packedFlow(Vec3 flow) {
        double speed = Math.sqrt(flow.x * flow.x + flow.z * flow.z);
        if (speed < 1.0e-5) return 0;
        double phase = Math.atan2(flow.z, flow.x) / (Math.PI * 2);
        int direction = (int) Math.round((phase - Math.floor(phase)) * 255);
        int magnitude = Math.min(255, (int) Math.round(speed * 255));
        return direction | (magnitude << 8);
    }

    public void dirty(BlockPos position) {
        int x = SectionPos.blockToSectionCoord(position.getX());
        int y = SectionPos.blockToSectionCoord(position.getY());
        int z = SectionPos.blockToSectionCoord(position.getZ());
        remove(SectionPos.asLong(x, y, z));
        if ((position.getX() & 15) == 0) remove(SectionPos.asLong(x - 1, y, z));
        if ((position.getX() & 15) == 15) remove(SectionPos.asLong(x + 1, y, z));
        if ((position.getY() & 15) == 0) remove(SectionPos.asLong(x, y - 1, z));
        if ((position.getY() & 15) == 15) remove(SectionPos.asLong(x, y + 1, z));
        if ((position.getZ() & 15) == 0) remove(SectionPos.asLong(x, y, z - 1));
        if ((position.getZ() & 15) == 15) remove(SectionPos.asLong(x, y, z + 1));
    }

    private void remove(long key) {
        Mesh mesh = meshes.remove(key);
        if (mesh == null) return;
        if (mesh.vertices() != null) mesh.vertices().close();
        cacheBytes -= mesh.bytes();
    }

    private void clearMeshes() {
        meshes.values().forEach(mesh -> {
            if (mesh.vertices() != null) mesh.vertices().close();
        });
        meshes.clear();
        blockCandidates.clear();
        fluidCandidates.clear();
        blockMaterials.clear();
        fluidMaterials.clear();
        cacheBytes = 0;
        buildFrame = -1;
    }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 values) {
        return new Matrix4f().set(values.columnMajor());
    }

    public long allocatedBytes() { return cacheBytes; }

    @Override public void close() {
        clearMeshes();
        opticsTarget = null;
        if (shader != null) {
            shader.close();
            shader = null;
        }
        if (transmissionShader != null) {
            transmissionShader.close();
            transmissionShader = null;
        }
    }

    private final class MeshConsumer implements VertexConsumer {
        private record Point(float x, float y, float z, int red, int green, int blue, int alpha, float u, float v) { }

        private static final float UV_MARGIN = 0.000002F;
        private final BufferBuilder target;
        private final Point[] quad = new Point[4];
        private int count;
        private MaterialResolver materials;
        private ClientLevel level;
        private BlockPos position;
        private BlockState state;
        private FluidState fluid;
        private TextureAtlasSprite[] sprites;
        private MaterialQuery.Layer layer;
        private float mipCutoff;
        private int flow;
        private SurfaceStyle selected;
        private float x, y, z, u, v, nx, ny, nz;
        private int r = 255, g = 255, b = 255, a = 255;

        private MeshConsumer(BufferBuilder target) { this.target = target; }

        private void block(MaterialResolver materials, ClientLevel level, BlockPos position, BlockState state,
                MaterialQuery.Layer layer, float mipCutoff) {
            clear();
            this.materials = materials;
            this.level = level;
            this.position = position;
            this.state = state;
            this.layer = layer;
            this.mipCutoff = mipCutoff;
        }

        private void fluid(MaterialResolver materials, ClientLevel level, BlockPos position, FluidState fluid,
                MaterialQuery.Layer layer, float mipCutoff) {
            clear();
            this.materials = materials;
            this.level = level;
            this.position = position;
            this.fluid = fluid;
            this.layer = layer;
            this.mipCutoff = mipCutoff;
            flow = packedFlow(fluid.getFlow(level, position));
            sprites = ForgeHooksClient.getFluidSprites(level, position, fluid);
        }

        @Override public void putBulkData(PoseStack.Pose pose, BakedQuad baked, float[] brightness,
                float red, float green, float blue, float alpha, int[] lights, int overlay, boolean useBakedColor) {
            if (state == null) return;
            ResourceLocation sprite = baked.getSprite().contents().name();
            MaterialDescription description = blockMaterial(materials, state, sprite, layer, baked.getTintIndex());
            selected = style(state, description, layer, mipCutoff, 0, level, position);
            try {
                if (selected.family() != 0) {
                    VertexConsumer.super.putBulkData(pose, baked, brightness, red, green, blue, alpha,
                            lights, overlay, useBakedColor);
                }
            } finally {
                selected = null;
                count = 0;
            }
        }

        @Override public VertexConsumer vertex(double x, double y, double z) {
            this.x = (float) x; this.y = (float) y; this.z = (float) z; return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) {
            this.r = r; this.g = g; this.b = b; this.a = a; return this;
        }
        @Override public VertexConsumer uv(float u, float v) { this.u = u; this.v = v; return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { nx = x; ny = y; nz = z; return this; }
        @Override public void defaultColor(int r, int g, int b, int a) { color(r, g, b, a); }
        @Override public void unsetDefaultColor() { color(255, 255, 255, 255); }

        @Override public void endVertex() {
            quad[count++] = new Point(x, y, z, r, g, b, a, u, v);
            if (count < 4) return;
            count = 0;
            SurfaceStyle style = selected == null ? immediateStyle() : selected;
            if (style.family() == 0) return;

            Vector3f edgeA = new Vector3f(quad[1].x - quad[0].x, quad[1].y - quad[0].y, quad[1].z - quad[0].z);
            Vector3f edgeB = new Vector3f(quad[2].x - quad[0].x, quad[2].y - quad[0].y, quad[2].z - quad[0].z);
            Vector3f normal = edgeA.cross(edgeB);
            if (normal.lengthSquared() > 1.0e-10F) normal.normalize();
            else normal.set(nx, ny, nz);

            int tint = style.colorRgb();
            for (Point point : quad) {
                int red = tint < 0 ? point.red : (tint >> 16) & 255;
                int green = tint < 0 ? point.green : (tint >> 8) & 255;
                int blue = tint < 0 ? point.blue : tint & 255;
                target.vertex(point.x, point.y, point.z).color(red, green, blue, point.alpha)
                        .uv(point.u, point.v).overlayCoords(style.family(), style.payload()).uv2(style.cutoff(), style.optics())
                        .normal(normal.x, normal.y, normal.z).endVertex();
            }
        }

        private SurfaceStyle immediateStyle() {
            if (fluid != null) {
                ResourceLocation sprite = fluidSprite();
                if (sprite == null) return NO_SURFACE;
                MaterialDescription description = fluidMaterial(materials, fluid, sprite, layer);
                return style(null, description, layer, mipCutoff, flow, level, position);
            }
            if (state == null) return NO_SURFACE;
            return style(state, blockMaterial(materials, state, null, layer, -1), layer, mipCutoff, 0, level, position);
        }

        private ResourceLocation fluidSprite() {
            if (sprites == null) return null;
            float centerU = (quad[0].u + quad[1].u + quad[2].u + quad[3].u) * 0.25F;
            float centerV = (quad[0].v + quad[1].v + quad[2].v + quad[3].v) * 0.25F;
            for (TextureAtlasSprite sprite : sprites) {
                if (sprite == null || centerU < sprite.getU0() || centerU > sprite.getU1()
                        || centerV < sprite.getV0() || centerV > sprite.getV1()) continue;
                boolean inside = true;
                for (Point point : quad) {
                    if (point.u < sprite.getU0() - UV_MARGIN || point.u > sprite.getU1() + UV_MARGIN
                            || point.v < sprite.getV0() - UV_MARGIN || point.v > sprite.getV1() + UV_MARGIN) {
                        inside = false;
                        break;
                    }
                }
                if (inside) return sprite.contents().name();
            }
            return null;
        }

        private void clear() {
            count = 0;
            selected = null;
            state = null;
            fluid = null;
            sprites = null;
            materials = null;
            level = null;
            position = null;
        }
    }
}

package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.taste.engine.effects.VegetationMotion;
import dev.taste.engine.environment.EnvironmentSnapshot;
import dev.taste.engine.light.VoxelSectionCache;
import dev.taste.engine.light.VoxelSectionGeometry;
import dev.taste.engine.light.VoxelSectionGeometry.Coordinate;
import dev.taste.engine.light.VoxelSectionGeometry.Surface;
import dev.taste.engine.material.GpuCompanionAtlas;
import dev.taste.engine.material.MaterialDescription;
import dev.taste.engine.material.MaterialQuery;
import dev.taste.engine.material.MaterialResolver;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;

/** Captures independently emitted Forge terrain into four material attachments. */
public final class TerrainMaterialPass implements AutoCloseable {
    public record Geometry(TextureTarget albedoAo, TextureTarget normalSurface, TextureTarget emissionFlags,
            TextureTarget motionLight, TextureTarget auxiliary, boolean motionValid) { }
    private record DrawSection(Coordinate coordinate, float x, float y, float z, double distance) { }
    private record GpuMesh(LevelChunkSection section, VertexBuffer buffer, long bytes, Set<Surface> surfaces) { }
    private static final int SECTION_RADIUS = 5;
    private static final int BUILD_BUDGET = 2;
    private static final int PALETTE_WIDTH = 256;
    private static final int MATERIAL_TEXELS = 6;
    private static final long GPU_MESH_BUDGET = 96L * 1024 * 1024;
    private static final int[] DRAW_ATTACHMENTS = {
            GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1,
            GL30.GL_COLOR_ATTACHMENT2, GL30.GL_COLOR_ATTACHMENT3
    };
    private static final float[] ZERO = {0, 0, 0, 0};
    private final VoxelSectionCache sections;
    private final GpuCompanionAtlas companions = new GpuCompanionAtlas();
    private final Map<Surface, Integer> ids = new LinkedHashMap<>();
    private final Map<Long, GpuMesh> gpuMeshes = new LinkedHashMap<>(32, 0.75F, true);
    private final BufferBuilder buffer = new BufferBuilder(262_144);
    private long world = -1;
    private long meshBytes;
    private ShaderInstance materialShader;
    private ShaderInstance resolveShader;
    private FloatBuffer paletteBuffer;
    private FrameSnapshot previous;
    private double wetTime;
    private float wetAmount;
    private MaterialDescription.TextureFormat companionFormat;
    private boolean companionFormatSet;

    public TerrainMaterialPass(VoxelSectionCache sections) { this.sections = sections; }

    public Geometry capture(TargetPool targets, TextureTarget opaque, FrameSnapshot frame,
            PipelineParameters settings, MaterialResolver resolver, boolean embeddium) throws IOException {
        long externalOther = Math.max(0, targets.externalBytes() - allocatedBytes());
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (opaque == null || level == null || !enabled(settings, "materials.pbr")) {
            releaseTargets(targets);
            if (!gpuMeshes.isEmpty() || companions.allocatedBytes() > 0) clearMeshes();
            return null;
        }
        if (world != frame.worldEpoch()) {
            clearMeshes();
            world = frame.worldEpoch();
        }
        Matrix4f viewProjection = matrix(frame.projection()).mul(matrix(frame.view()));
        List<DrawSection> visible = visible(level, frame, viewProjection);
        List<DrawSection> ready = new ArrayList<>();
        Set<Surface> activeSurfaces = new LinkedHashSet<>();
        int built = 0;
        for (DrawSection draw : visible) {
            Coordinate coordinate = draw.coordinate();
            var chunk = level.getChunkSource().getChunk(coordinate.x(), coordinate.z(), false);
            if (chunk == null) {
                remove(coordinate.key());
                continue;
            }
            LevelChunkSection section = chunk.getSections()[coordinate.y() - level.getMinSection()];
            GpuMesh cached = gpuMeshes.get(coordinate.key());
            if (cached == null || cached.section() != section) {
                if (!section.hasOnlyAir() && built >= BUILD_BUDGET) continue;
                remove(coordinate.key());
                VoxelSectionGeometry.Mesh source = section.hasOnlyAir() ? null : sections.get(level, coordinate, resolver);
                cached = upload(level, coordinate, section, source);
                gpuMeshes.put(coordinate.key(), cached);
                meshBytes += cached.bytes();
                if (!section.hasOnlyAir()) built++;
            }
            if (cached.buffer() != null) {
                ready.add(draw);
                activeSurfaces.addAll(cached.surfaces());
            }
        }
        while (meshBytes > GPU_MESH_BUDGET || gpuMeshes.size() > 768)
            remove(gpuMeshes.keySet().iterator().next());
        if (ready.isEmpty()) {
            releaseTargets(targets);
            previous = null;
            return null;
        }
        if (materialShader == null) materialShader = shader("taste_terrain_material", DefaultVertexFormat.NEW_ENTITY);
        TextureAtlas atlas = minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        MaterialDescription.TextureFormat format = format(settings);
        if (!companionFormatSet || companionFormat != format) {
            companions.close();
            companionFormat = format;
            companionFormatSet = true;
        }
        boolean motionValid = previous != null && previous.worldEpoch() == frame.worldEpoch()
                && previous.frameIndex() + 1 == frame.frameIndex();
        Geometry result = new Geometry(
                acquire(targets, "terrain-albedo", frame, TargetDescriptor.Format.RGBA8, true),
                acquire(targets, "terrain-normal", frame, TargetDescriptor.Format.RGBA16F, false),
                acquire(targets, "terrain-emission", frame, TargetDescriptor.Format.RGBA16F, false),
                acquire(targets, "terrain-motion", frame, TargetDescriptor.Format.RGBA16F, false),
                acquire(targets, "terrain-aux", frame, TargetDescriptor.Format.RGBA8, false), motionValid);
        List<GpuCompanionAtlas.Request> requests = activeSurfaces.stream().filter(surface -> surface.sprite() != null)
                .map(surface -> new GpuCompanionAtlas.Request(atlas.getSprite(surface.sprite()), surface.material(), format, null))
                .toList();
        companions.prepare(minecraft.getResourceManager(), atlas, requests,
                Math.max(0L, targets.limitBytes() - targets.allocatedBytes() - externalOther - meshBytes));
        targets.accountExternal(externalOther + allocatedBytes());
        applyAnisotropy(settings);
        int palette = uploadPalette(targets, settings, atlas, format);
        result.albedoAo().bindWrite(true);
        attach(1, result.normalSurface());
        attach(2, result.emissionFlags());
        attach(3, result.motionLight());
        GL20.glDrawBuffers(DRAW_ATTACHMENTS);
        if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Terrain material framebuffer is incomplete");
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        for (int index = 0; index < 4; index++) GL30.glClearBufferfv(GL11.GL_COLOR, index, ZERO);
        GL30.glClearBufferfv(GL11.GL_DEPTH, 0, new float[]{1});
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        BufferUploader.invalidate();
        materialShader.setSampler("BlockAtlas", atlas.getId());
        materialShader.setSampler("MaterialPalette", palette);
        materialShader.setSampler("NormalAtlas", companions.normalTexture() == 0 ? atlas.getId() : companions.normalTexture());
        materialShader.setSampler("SpecularAtlas", companions.specularTexture() == 0 ? atlas.getId() : companions.specularTexture());
        materialShader.setSampler("HeightAtlas", companions.heightTexture() == 0 ? atlas.getId() : companions.heightTexture());
        materialShader.getUniform("ViewProjection").set(viewProjection);
        Matrix4f motionProjection = matrix(frame.unjitteredProjection()).mul(matrix(frame.view()));
        materialShader.getUniform("MotionProjection").set(motionProjection);
        materialShader.getUniform("PreviousViewProjection").set(motionValid ? previousProjection(frame) : motionProjection);
        set(materialShader, "NormalStrength", value(settings, "materials.normalStrength"));
        set(materialShader, "GeneratedNormals", enabled(settings, "materials.generatedNormals")
                ? value(settings, "materials.generatedNormalStrength") : 0);
        set(materialShader, "ParallaxDepth", enabled(settings, "materials.parallax") ? value(settings, "materials.parallaxDepth") : 0);
        set(materialShader, "ParallaxSteps", value(settings, "materials.parallaxSteps"));
        set(materialShader, "RoofCheckRange", value(settings, "wetness.roofCheckRange"));
        set(materialShader, "NativeMipCutoff", embeddium ? 0.1F : 0.5F);
        set(materialShader, "RoughnessScale", value(settings, "materials.roughnessScale"));
        float[] eye = frame.cameraWorld().relativeTo(frame.origin());
        materialShader.getUniform("Eye").set(eye[0], eye[1], eye[2]);
        set(materialShader, "AuxOnly", 0);
        drawSections(ready);
        result.auxiliary().bindWrite(true);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D, result.albedoAo().getDepthTextureId(), 0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Terrain auxiliary framebuffer is incomplete");
        GL30.glClearBufferfv(GL11.GL_COLOR, 0, ZERO);
        RenderSystem.depthFunc(GL11.GL_EQUAL);
        RenderSystem.depthMask(false);
        set(materialShader, "AuxOnly", 1);
        drawSections(ready);
        previous = frame;
        return result;
    }

    private void drawSections(List<DrawSection> ready) {
        Uniform sectionOrigin = materialShader.getUniform("SectionOrigin");
        materialShader.apply();
        VegetationMotion.bind(materialShader.getId());
        try {
            for (DrawSection draw : ready) {
                GpuMesh mesh = gpuMeshes.get(draw.coordinate().key());
                if (mesh == null || mesh.buffer() == null) continue;
                sectionOrigin.set(draw.x(), draw.y(), draw.z());
                sectionOrigin.upload();
                mesh.buffer().bind();
                mesh.buffer().draw();
            }
        } finally {
            VertexBuffer.unbind();
            materialShader.clear();
        }
    }

    public TextureTarget shade(TargetPool targets, Geometry geometry, TextureTarget scene, TextureTarget opaque, TextureTarget world,
            FrameSnapshot frame, PipelineParameters settings, EnvironmentSnapshot environment,
            Vector3f lightDirection, Vector3f lightColor, Runnable draw) throws IOException {
        if (geometry == null || opaque == null) return scene;
        if (resolveShader == null) resolveShader = shader("taste_terrain_resolve", DefaultVertexFormat.POSITION_TEX);
        TextureTarget result = acquire(targets, "terrain-shaded", frame, TargetDescriptor.Format.RGBA16F, false);
        result.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> resolveShader);
        resolveShader.setSampler("SceneSampler", scene.getColorTextureId());
        resolveShader.setSampler("OpaqueDepth", opaque.getDepthTextureId());
        resolveShader.setSampler("WorldDepth", world.getDepthTextureId());
        resolveShader.setSampler("MaterialDepth", geometry.albedoAo().getDepthTextureId());
        resolveShader.setSampler("AlbedoAo", geometry.albedoAo().getColorTextureId());
        resolveShader.setSampler("NormalSurface", geometry.normalSurface().getColorTextureId());
        resolveShader.setSampler("EmissionFlags", geometry.emissionFlags().getColorTextureId());
        resolveShader.setSampler("MotionLight", geometry.motionLight().getColorTextureId());
        resolveShader.setSampler("MaterialAux", geometry.auxiliary().getColorTextureId());
        resolveShader.getUniform("InverseProjection").set(matrix(frame.inverseProjection()));
        resolveShader.getUniform("InverseView").set(matrix(frame.view()).invert());
        resolveShader.getUniform("LightDirection").set(lightDirection);
        resolveShader.getUniform("LightColor").set(lightColor);
        set(resolveShader, "Subsurface", enabled(settings, "materials.subsurface")
                ? value(settings, "materials.subsurfaceStrength") : 0);
        set(resolveShader, "EmissiveStrength", enabled(settings, "emission.enabled") ? value(settings, "emission.strength") : 0);
        set(resolveShader, "Wetness", wetness(frame, settings, environment.rain()));
        set(resolveShader, "WetDarkening", value(settings, "wetness.darkening"));
        set(resolveShader, "WetSmoothness", value(settings, "wetness.smoothness"));
        set(resolveShader, "PuddleCoverage", enabled(settings, "wetness.puddles") ? value(settings, "wetness.puddleCoverage") : 0);
        set(resolveShader, "PuddleDepth", value(settings, "wetness.puddleDepth"));
        set(resolveShader, "WorldSeed", value(settings, "general.seed"));
        resolveShader.getUniform("PuddleOrigin").set((float) (frame.origin().x() % 8192.0),
                (float) (frame.origin().z() % 8192.0));
        try {
            draw.run();
        } finally {
            resolveShader.clear();
        }
        return result;
    }

    private float wetness(FrameSnapshot frame, PipelineParameters settings, float rain) {
        double delta = wetTime == 0 ? 0 : Math.max(0, Math.min(1, frame.timeSeconds() - wetTime));
        wetTime = frame.timeSeconds();
        float seconds = rain > wetAmount ? value(settings, "wetness.wetSeconds") : value(settings, "wetness.drySeconds");
        wetAmount += (rain - wetAmount) * (float) (1 - Math.exp(-delta / Math.max(seconds, 0.01F)));
        return enabled(settings, "wetness.enabled") ? wetAmount : 0;
    }

    private int uploadPalette(TargetPool targets, PipelineParameters settings, TextureAtlas atlas,
            MaterialDescription.TextureFormat format) {
        int texels = Math.max(MATERIAL_TEXELS, ids.size() * MATERIAL_TEXELS);
        int rows = (texels + PALETTE_WIDTH - 1) / PALETTE_WIDTH;
        int floats = rows * PALETTE_WIDTH * 4;
        if (paletteBuffer == null || paletteBuffer.capacity() < floats) paletteBuffer = BufferUtils.createFloatBuffer(floats);
        paletteBuffer.clear();
        for (Surface surface : ids.keySet()) {
            MaterialDescription material = surface.material();
            MaterialDescription.Metadata metadata = material.metadata();
            MaterialDescription.TextureFormat selected = metadata.effectiveTextureFormat(format, null);
            TextureAtlasSprite sprite = surface.sprite() == null ? null : atlas.getSprite(surface.sprite());
            var available = sprite == null ? null : companions.availability(sprite);
            int emissive = metadata.emissionColorRgb() == null ? material.tintRgb() : metadata.emissionColorRgb();
            MaterialDescription fallback = MaterialDescription.fallback();
            boolean neutral = metadata.family() == MaterialDescription.Family.NEUTRAL
                    && material.roughness() == fallback.roughness() && material.f0() == fallback.f0() && material.metallic() == 0;
            float roughness = (neutral ? value(settings, "materials.defaultRoughness") : material.roughness())
                    * value(settings, "materials.roughnessScale");
            float f0 = neutral ? value(settings, "materials.defaultF0") : material.f0();
            paletteBuffer.put(Math.min(1, roughness)).put(f0).put(material.metallic()).put(material.emission());
            paletteBuffer.put(linear(emissive >> 16)).put(linear(emissive >> 8)).put(linear(emissive)).put(metadata.cutoutThreshold());
            paletteBuffer.put(material.porosity()).put(material.subsurface()).put(metadata.wetness()).put(metadata.metalCode());
            boolean sample = selected != MaterialDescription.TextureFormat.PROCEDURAL;
            paletteBuffer.put(sample && available != null && available.normal() ? 1 : 0)
                    .put(sample && available != null && available.specular() ? 1 : 0)
                    .put(selected == MaterialDescription.TextureFormat.OLDPBR ? 1 : 0)
                    .put(metadata.effectiveNormalY(selected) == MaterialDescription.NormalY.DIRECTX ? -1 : 1);
            if (sprite == null) paletteBuffer.put(0).put(0).put(1).put(1);
            else paletteBuffer.put(sprite.getU0()).put(sprite.getV0()).put(sprite.getU1()).put(sprite.getV1());
            paletteBuffer.put(metadata.heightMap() != null && available != null && available.height() ? 1 : 0)
                    .put(0).put(0).put(0);
        }
        while (paletteBuffer.position() < floats) paletteBuffer.put(0);
        paletteBuffer.flip();
        TextureTarget palette = targets.acquire(new TargetDescriptor("terrain-palette", PALETTE_WIDTH, rows,
                TargetDescriptor.Format.RGBA32F, false, 1, 1, 1));
        RenderSystem.bindTexture(palette.getColorTextureId());
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, PALETTE_WIDTH, rows, GL11.GL_RGBA, GL11.GL_FLOAT, paletteBuffer);
        return palette.getColorTextureId();
    }

    private GpuMesh upload(ClientLevel level, Coordinate coordinate, LevelChunkSection section, VoxelSectionGeometry.Mesh mesh) {
        if (mesh == null || mesh.quads() == 0) return new GpuMesh(section, null, 0, Set.of());
        FloatBuffer vertices = mesh.vertices();
        Set<Surface> used = new LinkedHashSet<>();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        for (int quad = 0; quad < mesh.quads(); quad++) {
            Surface surface = mesh.surface(quad);
            if (!ids.containsKey(surface) && ids.size() >= 65_534) continue;
            int id = ids.computeIfAbsent(surface, ignored -> ids.size());
            used.add(surface);
            int first = quad * VoxelSectionGeometry.VERTEX_FLOATS * 4;
            int step = VoxelSectionGeometry.VERTEX_FLOATS;
            Vector3f a = position(vertices, first);
            Vector3f normal = position(vertices, first + step).sub(a).cross(position(vertices, first + step * 2).sub(a));
            if (normal.lengthSquared() <= 1.0e-10F) normal.set(0, 1, 0);
            else normal.normalize();
            Vector3f center = new Vector3f(a).add(position(vertices, first + step * 2)).mul(0.5F);
            BlockPos exposure = BlockPos.containing(coordinate.blockX() + center.x, coordinate.blockY() + center.y + 0.12,
                    coordinate.blockZ() + center.z);
            int layer = surface.layer() == MaterialQuery.Layer.CUTOUT ? 3
                    : surface.layer() == MaterialQuery.Layer.CUTOUT_MIPPED ? 2 : 1;
            int flags = layer << 9 | (level.dimensionType().hasSkyLight() ? 1 << 8 : 0);
            int roofDistance = Math.max(0, Math.min(255,
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING, exposure.getX(), exposure.getZ()) - exposure.getY()));
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = first + vertex * step;
                int packedColor = mesh.rawColor(quad * 4 + vertex);
                int ao = byteOf(mesh.ambient(quad * 4 + vertex));
                buffer.vertex(vertices.get(offset), vertices.get(offset + 1), vertices.get(offset + 2))
                        .color((packedColor >> 16) & 255, (packedColor >> 8) & 255, packedColor & 255, byteOf(vertices.get(offset + 9)))
                        .uv(vertices.get(offset + 4), vertices.get(offset + 5))
                        .overlayCoords(id, flags | ao | ((mesh.wind(quad * 4 + vertex) & 31) << 11))
                        .uv2(Math.round(vertices.get(offset + 10)), Math.round(vertices.get(offset + 3) * 240) | roofDistance << 8)
                        .normal(normal.x, normal.y, normal.z).endVertex();
            }
        }
        BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
        if (rendered == null) return new GpuMesh(section, null, 0, Set.of());
        long bytes = rendered.vertexBuffer().remaining();
        VertexBuffer gpu = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try {
            gpu.bind();
            gpu.upload(rendered);
            return new GpuMesh(section, gpu, bytes, Set.copyOf(used));
        } catch (RuntimeException failure) {
            gpu.close();
            throw failure;
        } finally {
            VertexBuffer.unbind();
        }
    }

    private static List<DrawSection> visible(ClientLevel level, FrameSnapshot frame, Matrix4f matrix) {
        FrameSnapshot.Vec3d origin = frame.origin();
        FrameSnapshot.Vec3d eye = frame.cameraWorld();
        float[] eyeRelative = eye.relativeTo(origin);
        int x0 = SectionPos.blockToSectionCoord(eye.x());
        int y0 = SectionPos.blockToSectionCoord(eye.y());
        int z0 = SectionPos.blockToSectionCoord(eye.z());
        FrustumIntersection frustum = new FrustumIntersection(matrix);
        List<DrawSection> result = new ArrayList<>();
        for (int z = z0 - SECTION_RADIUS; z <= z0 + SECTION_RADIUS; z++)
            for (int y = Math.max(y0 - SECTION_RADIUS, level.getMinSection()); y <= Math.min(y0 + SECTION_RADIUS, level.getMaxSection() - 1); y++)
                for (int x = x0 - SECTION_RADIUS; x <= x0 + SECTION_RADIUS; x++) {
                    Coordinate coordinate = new Coordinate(x, y, z);
                    float px = (float) (coordinate.blockX() - origin.x());
                    float py = (float) (coordinate.blockY() - origin.y());
                    float pz = (float) (coordinate.blockZ() - origin.z());
                    if (!frustum.testAab(px, py, pz, px + 16, py + 16, pz + 16)) continue;
                    float dx = px + 8 - eyeRelative[0], dy = py + 8 - eyeRelative[1], dz = pz + 8 - eyeRelative[2];
                    result.add(new DrawSection(coordinate, px, py, pz, dx * dx + dy * dy + dz * dz));
                }
        result.sort(Comparator.comparingDouble(DrawSection::distance));
        return result;
    }

    private Matrix4f previousProjection(FrameSnapshot current) {
        FrameSnapshot.Vec3d origin = current.origin(), prior = previous.origin();
        Matrix4f oldView = matrix(previous.view()).translate((float) (origin.x() - prior.x()),
                (float) (origin.y() - prior.y()), (float) (origin.z() - prior.z()));
        return matrix(previous.unjitteredProjection()).mul(oldView);
    }

    public void dirty(BlockPos position) {
        int x = SectionPos.blockToSectionCoord(position.getX());
        int y = SectionPos.blockToSectionCoord(position.getY());
        int z = SectionPos.blockToSectionCoord(position.getZ());
        for (int dz = (position.getZ() & 15) == 0 ? -1 : 0; dz <= ((position.getZ() & 15) == 15 ? 1 : 0); dz++)
            for (int dy = (position.getY() & 15) == 0 ? -1 : 0; dy <= ((position.getY() & 15) == 15 ? 1 : 0); dy++)
                for (int dx = (position.getX() & 15) == 0 ? -1 : 0; dx <= ((position.getX() & 15) == 15 ? 1 : 0); dx++)
                    remove(SectionPos.asLong(x + dx, y + dy, z + dz));
    }

    public long allocatedBytes() { return meshBytes + companions.allocatedBytes(); }
    public void invalidate() { previous = null; }

    private void remove(long key) {
        GpuMesh removed = gpuMeshes.remove(key);
        if (removed == null) return;
        if (removed.buffer() != null) removed.buffer().close();
        meshBytes -= removed.bytes();
    }
    private void clearMeshes() {
        while (!gpuMeshes.isEmpty()) remove(gpuMeshes.keySet().iterator().next());
        ids.clear();
        companions.close();
        companionFormatSet = false;
        previous = null;
        wetTime = 0;
        wetAmount = 0;
    }
    private static void releaseTargets(TargetPool targets) {
        for (String name : List.of("terrain-albedo", "terrain-normal", "terrain-emission", "terrain-motion",
                "terrain-aux", "terrain-palette", "terrain-shaded")) targets.release(name);
    }
    @Override public void close() {
        clearMeshes();
        if (materialShader != null) { materialShader.close(); materialShader = null; }
        if (resolveShader != null) { resolveShader.close(); resolveShader = null; }
        paletteBuffer = null;
    }

    private static void attach(int index, TextureTarget target) {
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index,
                GL11.GL_TEXTURE_2D, target.getColorTextureId(), 0);
    }
    private static TextureTarget acquire(TargetPool pool, String name, FrameSnapshot frame, TargetDescriptor.Format format, boolean depth) {
        return pool.acquire(new TargetDescriptor(name, frame.width(), frame.height(), format, depth, 1, 1, 1));
    }
    private static ShaderInstance shader(String id, VertexFormat format) throws IOException {
        return new ShaderInstance(Minecraft.getInstance().getResourceManager(), new ResourceLocation("taste", id), format);
    }
    private static Vector3f position(FloatBuffer buffer, int index) {
        return new Vector3f(buffer.get(index), buffer.get(index + 1), buffer.get(index + 2));
    }
    private static MaterialDescription.TextureFormat format(PipelineParameters settings) {
        return switch (settings.value("materials.format").getAsString()) {
            case "labpbr" -> MaterialDescription.TextureFormat.LABPBR;
            case "oldpbr" -> MaterialDescription.TextureFormat.OLDPBR;
            case "procedural" -> MaterialDescription.TextureFormat.PROCEDURAL;
            default -> null;
        };
    }

    private void applyAnisotropy(PipelineParameters settings) {
        if (!GL.getCapabilities().GL_EXT_texture_filter_anisotropic) return;
        float requested = Float.parseFloat(settings.value("materials.anisotropy").getAsString());
        float maximum = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        for (int texture : new int[]{companions.normalTexture(), companions.heightTexture()}) {
            if (texture == 0) continue;
            RenderSystem.bindTexture(texture);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                    Math.max(1, Math.min(requested, maximum)));
        }
    }
    private static Matrix4f matrix(FrameSnapshot.Matrix4 value) { return new Matrix4f().set(value.columnMajor()); }
    private static float value(PipelineParameters settings, String key) { return settings.value(key).getAsFloat(); }
    private static boolean enabled(PipelineParameters settings, String key) { return settings.value(key).getAsBoolean(); }
    private static void set(ShaderInstance shader, String key, float value) { shader.getUniform(key).set(value); }
    private static int byteOf(float value) { return Math.round(Math.max(0, Math.min(1, value)) * 255); }
    private static float linear(int channel) {
        float s = (channel & 255) / 255.0F;
        return s <= 0.04045F ? s / 12.92F : (float) Math.pow((s + 0.055F) / 1.055F, 2.4);
    }
}

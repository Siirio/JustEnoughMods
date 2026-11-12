package dev.taste.engine.render.reflection;

import dev.taste.engine.effects.VegetationMotion;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.GlStateScope;
import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import dev.taste.engine.render.backend.TerrainBackend;
import dev.taste.engine.render.embeddium.Embeddium031Backend;
import dev.taste.engine.render.vanilla.VanillaTerrainBackend;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/** Captures resident opaque and cutout terrain through independent reflection views. */
public final class ReflectionCapture implements AutoCloseable {
    public record Result(TextureTarget probeAtlas, int probeFaceSize, Vector3f probeOrigin, float probeRange,
            TextureTarget planar, Matrix4f planarProjection, float planarHeight, boolean nativeSrgb, long generation) { }

    private record ProbeKey(FrameSnapshot.Vec3d origin, int size, float range) { }
    private record Probe(ProbeKey key, TextureTarget texture, long frame) { }
    private record Planar(TextureTarget texture, Matrix4f projection, float height) { }

    private static final String PLANAR_TARGET = "reflection-planar";
    private static final String NATIVE_PROBE = "reflection-native-probe";
    private static final String NATIVE_PLANAR = "reflection-native-planar";
    private static final int PROBE_REFRESH_FRAMES = 16;
    private static final float NEAR = 0.08F;
    private static final int[][] CUBE_FACES = {
        {1, 0, 0, 0, -1, 0}, {-1, 0, 0, 0, -1, 0},
        {0, 1, 0, 0, 0, 1}, {0, -1, 0, 0, 0, -1},
        {0, 0, 1, 0, -1, 0}, {0, 0, -1, 0, -1, 0}
    };

    private final WaterPlane waterPlane = new WaterPlane();
    private ShaderInstance vanillaShader;
    private ShaderInstance linearizeShader;
    private Probe front;
    private ProbeKey staging;
    private int stagingFace;
    private int stagingTarget;
    private boolean dirtyProbe;
    private boolean capturing;
    private boolean planarActive;
    private long worldEpoch = -1L;
    private long generation;
    private String terrainOwner = "";

    /**
     * This method draws mesh buffers directly. It does not enter the level render loop
     * or run simulation and callbacks for a secondary view.
     */
    public Result capture(TargetPool targets, TerrainBackend terrain, ClientLevel level, FrameSnapshot frame,
            PipelineParameters parameters, boolean submerged) throws IOException {
        RenderSystem.assertOnRenderThread();
        if (capturing) return null;
        if (level == null || terrain == null || !enabled(parameters, "reflections.enabled")) {
            releaseTargets(targets);
            return null;
        }
        boolean embeddium = terrain instanceof Embeddium031Backend;
        if (!(terrain instanceof VanillaTerrainBackend) && !embeddium) {
            releaseTargets(targets);
            return null;
        }
        if (worldEpoch != frame.worldEpoch() || !terrain.id().equals(terrainOwner)) {
            releaseTargets(targets);
            worldEpoch = frame.worldEpoch();
            terrainOwner = terrain.id();
        }
        String method = parameters.value("reflections.method").getAsString();
        boolean useProbe = method.equals("hybrid");
        boolean usePlanar = !submerged && enabled(parameters, "reflections.planarWater")
                && enabled(parameters, "water.enabled") && !method.equals("sky");
        if (!useProbe) releaseProbes(targets);
        if (!usePlanar) releasePlanar(targets);
        if (!useProbe && !usePlanar) return null;

        float range = Math.max(16.0F, Math.min(96.0F, parameters.value("reflections.maxDistance").getAsFloat()));
        Planar planar = null;
        capturing = true;
        try (GlStateScope ignored = GlStateScope.capture(); CaptureState state = new CaptureState()) {
            if (useProbe) updateProbe(targets, terrain, level, frame, parameters, range);
            if (usePlanar) {
                double selected = waterPlane.select(level, frame);
                if (Double.isFinite(selected)) {
                    planar = drawPlanar(targets, terrain, frame, parameters, range, selected);
                }
                if (planar == null) releasePlanar(targets);
                else if (!planarActive) {
                    planarActive = true;
                    generation++;
                }
            }
        } finally {
            capturing = false;
        }

        if (front == null && planar == null) return null;
        Vector3f probeOrigin = front == null ? new Vector3f() : new Vector3f(front.key().origin().relativeTo(frame.origin()));
        return new Result(front == null ? null : front.texture(), front == null ? 0 : front.key().size(), probeOrigin,
                front == null ? 0.0F : front.key().range(), planar == null ? null : planar.texture(),
                planar == null ? new Matrix4f() : planar.projection(), planar == null ? 0.0F : planar.height(),
                false, generation);
    }

    private void updateProbe(TargetPool targets, TerrainBackend terrain, ClientLevel level, FrameSnapshot frame,
            PipelineParameters parameters, float range) throws IOException {
        // Avoid committing a cube whose faces straddle a native lightning flash.
        if (level.getSkyFlashTime() > 0) {
            staging = null;
            stagingFace = 0;
            dirtyProbe = true;
            return;
        }
        var eye = frame.cameraWorld();
        int size = Integer.parseInt(parameters.value("reflections.probeResolution").getAsString());
        var origin = probeOrigin(level, eye);
        if (origin == null) return;
        ProbeKey key = new ProbeKey(origin, size, range);
        boolean refresh = dirtyProbe || front == null || !front.key().equals(key)
                || frame.frameIndex() - front.frame() >= PROBE_REFRESH_FRAMES;
        if (staging == null && !refresh) return;
        if (!key.equals(staging) || dirtyProbe) {
            staging = key;
            stagingFace = 0;
            dirtyProbe = false;
        }
        int mipLevels = Integer.numberOfTrailingZeros(size) + 1;
        TextureTarget atlas = targets.acquire(new TargetDescriptor(probeTarget(stagingTarget),
                size * 3, size * 2, TargetDescriptor.Format.RGBA16F, true, mipLevels, 1, 1));
        int faces = parameters.value("reflections.probeFacesPerFrame").getAsInt();
        int end = Math.min(CUBE_FACES.length, stagingFace + faces);
        for (int face = stagingFace; face < end; face++) {
            Matrix4f projection = faceProjection(face, range);
            drawTerrain(targets, atlas, (face % 3) * size, (face / 3) * size, size, size,
                    terrain, projection, staging.origin());
        }
        stagingFace = end;
        if (stagingFace < CUBE_FACES.length) return;
        generateProbeMips(atlas, mipLevels);
        front = new Probe(staging, atlas, frame.frameIndex());
        staging = null;
        stagingTarget ^= 1;
        stagingFace = 0;
        dirtyProbe = false;
        generation++;
    }

    private Planar drawPlanar(TargetPool targets, TerrainBackend terrain, FrameSnapshot frame,
            PipelineParameters parameters, float range, double waterY) throws IOException {
        float plane = (float) (waterY - frame.origin().y());
        Matrix4f inverseView = matrix(frame.view()).invert();
        Vector3f eye = inverseView.getTranslation(new Vector3f());
        if (eye.y <= plane + 0.035F) return null;
        Vector3f forward = inverseView.transformDirection(new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f up = inverseView.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F));
        eye.y = 2.0F * plane - eye.y;
        forward.y = -forward.y;
        up.y = -up.y;
        Matrix4f view = new Matrix4f().lookAt(eye, new Vector3f(eye).add(forward), up);
        Matrix4f projection = matrix(frame.unjitteredProjection());
        projection.m22(-(range + NEAR) / (range - NEAR));
        projection.m32(-2.0F * range * NEAR / (range - NEAR));
        if (!applyObliquePlane(projection, view, plane + 0.015F)) return null;
        projection.mul(view);
        float scale = Float.parseFloat(parameters.value("reflections.resolutionScale").getAsString());
        int width = Math.max(1, Math.round(frame.width() * scale));
        int height = Math.max(1, Math.round(frame.height() * scale));
        TextureTarget target = targets.acquire(new TargetDescriptor(PLANAR_TARGET, width, height,
                TargetDescriptor.Format.RGBA16F, true, 1, 1, 1));
        target.setFilterMode(GL11.GL_LINEAR);
        drawTerrain(targets, target, 0, 0, width, height, terrain, projection, frame.origin());
        return new Planar(target, projection, plane);
    }

    private static boolean applyObliquePlane(Matrix4f projection, Matrix4f view, float height) {
        Vector4f plane = new Vector4f(0.0F, 1.0F, 0.0F, -height);
        new Matrix4f(view).invert().transpose().transform(plane);
        Vector4f corner = new Vector4f(Math.copySign(1.0F, plane.x), Math.copySign(1.0F, plane.y), 1.0F, 1.0F);
        new Matrix4f(projection).invert().transform(corner);
        float divisor = plane.dot(corner);
        if (!Float.isFinite(divisor) || divisor <= 0.00001F) return false;
        plane.mul(2.0F / divisor);
        projection.m02(plane.x - projection.m03()).m12(plane.y - projection.m13())
                .m22(plane.z - projection.m23()).m32(plane.w - projection.m33());
        return true;
    }

    private void drawTerrain(TargetPool targets, TextureTarget target, int x, int y, int width, int height,
            TerrainBackend terrain, Matrix4f projection, FrameSnapshot.Vec3d origin) throws IOException {
        FrameSnapshot.Vec3d previousOrigin = VegetationMotion.drawFrom(origin);
        try {
            if (terrain instanceof Embeddium031Backend embeddium) {
                String name = target.width == width && target.height == height ? NATIVE_PLANAR : NATIVE_PROBE;
                TextureTarget nativeTarget = targets.acquire(new TargetDescriptor(name, width, height,
                        TargetDescriptor.Format.RGBA8, true, 1, 1, 1));
                beginTerrain(nativeTarget, 0, 0, width, height);
                embeddium.drawShadowCasters(projection, origin);
                linearizeCapture(nativeTarget, target, x, y, width, height);
            } else if (terrain instanceof VanillaTerrainBackend vanilla) {
                beginTerrain(target, x, y, width, height);
                drawVanilla(vanilla, projection, origin);
            }
        } finally {
            VegetationMotion.drawFrom(previousOrigin);
        }
    }

    private static void beginTerrain(TextureTarget target, int x, int y, int width, int height) {
        target.bindWrite(false);
        RenderSystem.viewport(x, y, width, height);
        RenderSystem.enableScissor(x, y, width, height);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClearDepth(1.0);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        BufferUploader.reset();
    }

    private void linearizeCapture(TextureTarget source, TextureTarget target, int x, int y, int width, int height)
            throws IOException {
        if (linearizeShader == null) {
            linearizeShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation("taste", "taste_reflection_linearize"), DefaultVertexFormat.POSITION_TEX);
        }
        RenderSystem.disableScissor();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, width, height, x, y, x + width, y + height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        target.bindWrite(false);
        RenderSystem.viewport(x, y, width, height);
        RenderSystem.enableScissor(x, y, width, height);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> linearizeShader);
        linearizeShader.setSampler("NativeColor", source.getColorTextureId());
        linearizeShader.setSampler("NativeDepth", source.getDepthTextureId());
        try {
            BufferBuilder vertices = Tesselator.getInstance().getBuilder();
            vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            vertices.vertex(-1.0, -1.0, 0.0).uv(0.0F, 0.0F).endVertex();
            vertices.vertex(1.0, -1.0, 0.0).uv(1.0F, 0.0F).endVertex();
            vertices.vertex(1.0, 1.0, 0.0).uv(1.0F, 1.0F).endVertex();
            vertices.vertex(-1.0, 1.0, 0.0).uv(0.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(vertices.end());
        } finally {
            linearizeShader.clear();
        }
    }

    private void drawVanilla(VanillaTerrainBackend vanilla, Matrix4f projection, FrameSnapshot.Vec3d origin)
            throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        if (vanillaShader == null) {
            vanillaShader = new ShaderInstance(minecraft.getResourceManager(),
                    new ResourceLocation("taste", "taste_reflection_terrain"), DefaultVertexFormat.BLOCK);
        }
        minecraft.gameRenderer.lightTexture().turnOnLightLayer();
        vanillaShader.setSampler("BlockAtlas", minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId());
        vanillaShader.setSampler("NativeLightmap", RenderSystem.getShaderTexture(2));
        require("CaptureProjection").set(projection);
        Uniform sectionOrigin = require("SectionOrigin");
        Uniform cutoff = require("AlphaCutoff");
        vanillaShader.apply();
        VegetationMotion.bind(vanillaShader.getId());
        try {
            vanilla.drawShadowCasters(projection, origin, (mesh, x, y, z, alpha) -> {
                sectionOrigin.set(x, y, z);
                sectionOrigin.upload();
                cutoff.set(alpha);
                cutoff.upload();
                mesh.bind();
                mesh.draw();
            });
        } finally {
            VertexBuffer.unbind();
            vanillaShader.clear();
        }
    }

    private Uniform require(String name) {
        Uniform result = vanillaShader.getUniform(name);
        if (result == null) throw new IllegalStateException("Missing reflection terrain uniform " + name);
        return result;
    }

    private static Matrix4f faceProjection(int face, float range) {
        int[] axes = CUBE_FACES[face];
        return new Matrix4f().perspective((float) (Math.PI * 0.5), 1.0F, NEAR, range)
                .lookAt(0.0F, 0.0F, 0.0F, axes[0], axes[1], axes[2], axes[3], axes[4], axes[5]);
    }

    private FrameSnapshot.Vec3d probeOrigin(ClientLevel level, FrameSnapshot.Vec3d eye) {
        if (staging != null && nearby(eye, staging.origin(), 9.0) && usable(level, staging.origin())) return staging.origin();
        if (front != null && nearby(eye, front.key().origin(), 9.0) && usable(level, front.key().origin())) return front.key().origin();
        double x = Math.floor(eye.x()) + 0.5;
        double y = Math.floor(eye.y()) + 0.5;
        double z = Math.floor(eye.z()) + 0.5;
        for (int dy : new int[] {0, 1, -1, 2, -2}) {
            var candidate = new FrameSnapshot.Vec3d(x, y + dy, z);
            if (usable(level, candidate)) return candidate;
        }
        return usable(level, eye) ? eye : null;
    }

    private static boolean usable(ClientLevel level, FrameSnapshot.Vec3d origin) {
        BlockPos pos = BlockPos.containing(origin.x(), origin.y(), origin.z());
        if (level.isOutsideBuildHeight(pos) || level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false) == null)
            return false;
        var state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean nearby(FrameSnapshot.Vec3d a, FrameSnapshot.Vec3d b, double range) {
        double x = a.x() - b.x();
        double y = a.y() - b.y();
        double z = a.z() - b.z();
        return x * x + y * y + z * z <= range * range;
    }

    private static void generateProbeMips(TextureTarget atlas, int levels) {
        int prior = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas.getColorTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, levels - 1);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prior);
        }
    }

    public void dirty(BlockPos pos) {
        waterPlane.dirty(pos);
        var point = new FrameSnapshot.Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        ProbeKey current = staging == null ? front == null ? null : front.key() : staging;
        if (current != null && nearby(point, current.origin(), Math.min(24.0F, current.range()))) dirtyProbe = true;
    }

    public void releaseTargets(TargetPool targets) {
        releaseProbes(targets);
        releasePlanar(targets);
        waterPlane.reset();
    }

    private void releaseProbes(TargetPool targets) {
        targets.release(probeTarget(0));
        targets.release(probeTarget(1));
        targets.release(NATIVE_PROBE);
        if (front != null) generation++;
        front = null;
        staging = null;
        stagingFace = 0;
        stagingTarget = 0;
        dirtyProbe = false;
    }

    private void releasePlanar(TargetPool targets) {
        targets.release(PLANAR_TARGET);
        targets.release(NATIVE_PLANAR);
        if (planarActive) generation++;
        planarActive = false;
    }

    private static String probeTarget(int index) { return "reflection-probe-" + index; }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 matrix) { return new Matrix4f().set(matrix.columnMajor()); }

    private static boolean enabled(PipelineParameters parameters, String id) {
        return parameters.value(id).getAsBoolean();
    }

    public static void bind(ShaderInstance consumer, Result result, RenderTarget fallback) {
        TextureTarget probe = result == null ? null : result.probeAtlas();
        TextureTarget planar = result == null ? null : result.planar();
        consumer.setSampler("ProbeAtlas", probe == null ? fallback.getColorTextureId() : probe.getColorTextureId());
        consumer.setSampler("ProbeDepth", probe == null ? fallback.getDepthTextureId() : probe.getDepthTextureId());
        consumer.setSampler("PlanarCapture", planar == null ? fallback.getColorTextureId() : planar.getColorTextureId());
        set(consumer, "ProbeFaceSize", probe == null ? 0.0F : result.probeFaceSize());
        set(consumer, "ProbeRange", probe == null ? 0.0F : result.probeRange());
        set(consumer, "PlanarEnabled", planar == null ? 0.0F : 1.0F);
        set(consumer, "PlanarHeight", planar == null ? 0.0F : result.planarHeight());
        set(consumer, "CaptureNativeSrgb", result != null && result.nativeSrgb() ? 1.0F : 0.0F);
        if (probe != null && consumer.getUniform("ProbeOrigin") != null) consumer.getUniform("ProbeOrigin").set(result.probeOrigin());
        if (planar != null && consumer.getUniform("PlanarProjection") != null)
            consumer.getUniform("PlanarProjection").set(result.planarProjection());
    }

    private static void set(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    @Override public void close() {
        front = null;
        staging = null;
        stagingFace = 0;
        stagingTarget = 0;
        dirtyProbe = false;
        planarActive = false;
        capturing = false;
        worldEpoch = -1L;
        terrainOwner = "";
        waterPlane.reset();
        if (vanillaShader != null) {
            vanillaShader.close();
            vanillaShader = null;
        }
        if (linearizeShader != null) {
            linearizeShader.close();
            linearizeShader = null;
        }
    }

    private static final class CaptureState implements AutoCloseable {
        private final int lightmap = RenderSystem.getShaderTexture(2);
        private final float fogStart = RenderSystem.getShaderFogStart();
        private final float fogEnd = RenderSystem.getShaderFogEnd();
        private final double clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        private final float[] clearColor = new float[4];

        CaptureState() {
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
            RenderSystem.setShaderFogStart(10_000.0F);
            RenderSystem.setShaderFogEnd(20_000.0F);
        }

        @Override public void close() {
            RenderSystem.setShaderTexture(2, lightmap);
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            GL11.glClearDepth(clearDepth);
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
        }
    }
}

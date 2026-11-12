package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import dev.taste.engine.light.LocalLightingPass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

/** Draws only Taste-owned native sprite geometry. Foreign particles keep their native render batches. */
public final class OwnedParticlePass implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COLOR = "owned-particle-color";
    private static final String REVEAL = "owned-particle-reveal";
    private static final String COMPOSITE = "owned-particle-composite";
    private static final String EMISSION = "owned-particle-emission";
    private static final ThreadLocal<OwnedParticlePass> ACTIVE = new ThreadLocal<>();
    private static final Map<Particle, Appearance> OWNED = new IdentityHashMap<>();

    private record Appearance(boolean soft, int emission) { }
    private record Draw(Particle particle, Appearance appearance, boolean terrain, float depth) { }

    private final ArrayList<Draw> visible = new ArrayList<>();
    private ShaderInstance sorted;
    private ShaderInstance weighted;
    private ShaderInstance reveal;
    private ShaderInstance composite;
    private TextureTarget colorTarget;
    private TextureTarget revealTarget;
    private TextureTarget compositeTarget;
    private TextureTarget emissionTarget;
    private TextureTarget emissionTexture;
    private TextureTarget coverageTexture;
    private boolean inverseCoverage;
    private FrameSnapshot frame;
    private PipelineParameters config;
    private Camera camera;
    private float partialTick;
    private Matrix4f particleView;
    private String effectiveMode = "native";
    private String lastFailure;
    private long failedRevision = Long.MIN_VALUE;
    private int failedWidth;
    private int failedHeight;
    private long retryFrame;
    private float fogDensity;
    private final Vector3f fogColor = new Vector3f();
    private boolean weightedDrawFailed;

    public static void track(Particle particle, boolean soft, float emission) {
        int encoded = Math.round(Math.max(0.0F, Math.min(4.0F, emission)) * (127.0F / 4.0F));
        OWNED.put(particle, new Appearance(soft, encoded));
    }

    /** The vanilla particle engine still does simulation, frustum checks and provider-specific culling. */
    public static boolean intercept(Particle particle, Camera camera, float partialTick) {
        OwnedParticlePass active = ACTIVE.get();
        if (active == null) return false;
        Appearance appearance = OWNED.get(particle);
        if (appearance == null) return false;
        ParticleRenderType type = particle.getRenderType();
        boolean terrain = type == ParticleRenderType.TERRAIN_SHEET;
        if (!terrain && type != ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                && type != ParticleRenderType.PARTICLE_SHEET_OPAQUE && type != ParticleRenderType.PARTICLE_SHEET_LIT) return false;
        if (active.camera != null && active.camera != camera) return false;
        if (active.camera == null) {
            active.camera = camera;
            active.partialTick = partialTick;
            active.particleView = new Matrix4f(RenderSystem.getModelViewMatrix());
        }
        Vec3 relative = particle.getBoundingBox().getCenter().subtract(camera.getPosition());
        float z = active.particleView.transformPosition(new Vector3f((float) relative.x, (float) relative.y, (float) relative.z)).z;
        active.visible.add(new Draw(particle, appearance, terrain, -z));
        return true;
    }

    public void begin(FrameSnapshot nextFrame, PipelineParameters parameters, TargetPool targets, boolean weightedSupported) {
        end();
        effectiveMode = "native";
        OWNED.keySet().removeIf(particle -> !particle.isAlive());
        boolean unchangedFailure = failedRevision == parameters.revision()
                && failedWidth == nextFrame.width() && failedHeight == nextFrame.height()
                && nextFrame.frameIndex() < retryFrame;
        if (!parameters.value("particles.enabled").getAsBoolean() || OWNED.isEmpty() || unchangedFailure) {
            release(targets);
            return;
        }
        frame = nextFrame;
        config = parameters;
        try (GlStateScope ignored = GlStateScope.capture()) {
            if (sorted == null) sorted = shader("taste_owned_particle_sorted", DefaultVertexFormat.PARTICLE);
            if (composite == null) composite = shader("taste_owned_particle_composite", DefaultVertexFormat.POSITION_TEX);
            colorTarget = target(targets, COLOR);
            compositeTarget = target(targets, COMPOSITE);
            effectiveMode = "sorted";
            if (weightedSupported && !weightedDrawFailed && config.value("particles.transparency").getAsString().equals("weighted")) {
                try {
                    if (weighted == null) weighted = shader("taste_owned_particle_weighted", DefaultVertexFormat.PARTICLE);
                    if (reveal == null) reveal = shader("taste_owned_particle_reveal", DefaultVertexFormat.PARTICLE);
                    revealTarget = target(targets, REVEAL);
                    revealTarget.setClearColor(1, 1, 1, 1);
                    effectiveMode = "weighted";
                } catch (IOException | RuntimeException unsupported) {
                    targets.release(REVEAL);
                    report(unsupported);
                }
            } else targets.release(REVEAL);
            if (emissionStrength() > 0 && OWNED.values().stream().anyMatch(particle -> particle.emission() > 0)) {
                try {
                    emissionTarget = target(targets, EMISSION);
                } catch (RuntimeException unsupported) {
                    targets.release(EMISSION);
                    report(unsupported);
                }
            } else targets.release(EMISSION);
            ACTIVE.set(this);
        } catch (IOException | RuntimeException failure) {
            report(failure);
            failedRevision = parameters.revision();
            failedWidth = nextFrame.width();
            failedHeight = nextFrame.height();
            retryFrame = failure instanceof IOException ? Long.MAX_VALUE : nextFrame.frameIndex() + 60;
            end();
            release(targets);
            effectiveMode = "native";
        }
    }

    public TextureTarget render(TargetPool targets, TextureTarget source, TextureTarget sceneDepth,
            LocalLightingPass.TileData lights, Runnable fullscreen) {
        if (ACTIVE.get() == this) ACTIVE.remove();
        if (visible.isEmpty()) return source;
        try (GlStateScope ignored = GlStateScope.capture()) {
            boolean oit = effectiveMode.equals("weighted");
            if (oit) {
                try {
                    drawLayer(weighted, colorTarget, sceneDepth, lights, 1, false);
                    drawLayer(reveal, revealTarget, sceneDepth, lights, 2, false);
                } catch (RuntimeException unsupported) {
                    report(unsupported);
                    weightedDrawFailed = true;
                    effectiveMode = "sorted";
                    oit = false;
                }
            }
            if (!oit) {
                visible.sort(Comparator.comparingDouble(Draw::depth).reversed());
                drawLayer(sorted, colorTarget, sceneDepth, lights, 0, true);
            }
            if (emissionTarget != null && visible.stream().anyMatch(entry -> entry.appearance().emission() > 0)) {
                try {
                    if (oit) visible.sort(Comparator.comparingDouble(Draw::depth).reversed());
                    drawLayer(sorted, emissionTarget, sceneDepth, lights, 3, true);
                    emissionTexture = emissionTarget;
                } catch (RuntimeException unavailable) {
                    report(unavailable);
                    targets.release(EMISSION);
                }
            }

            compositeTarget.bindWrite(true);
            TemporalPostPass.setupFullscreen(composite);
            composite.setSampler("SceneColor", source.getColorTextureId());
            composite.setSampler("ParticleColor", colorTarget.getColorTextureId());
            composite.setSampler("ParticleReveal", (oit ? revealTarget : colorTarget).getColorTextureId());
            composite.getUniform("Weighted").set(oit ? 1.0F : 0.0F);
            try {
                fullscreen.run();
                requireGl("owned particle composition");
            } finally {
                composite.clear();
            }
            coverageTexture = oit ? revealTarget : colorTarget;
            inverseCoverage = oit;
            return compositeTarget;
        } catch (RuntimeException failure) {
            report(failure);
            failedRevision = config.revision();
            failedWidth = frame.width();
            failedHeight = frame.height();
            retryFrame = frame.frameIndex() + 60;
            effectiveMode = "native";
            emissionTexture = null;
            return source;
        } finally {
            visible.clear();
        }
    }

    private void drawLayer(ShaderInstance program, TextureTarget output, TextureTarget sceneDepth,
            LocalLightingPass.TileData lights, int mode, boolean depthOrder) {
        output.clear(Minecraft.ON_OSX);
        output.bindWrite(true);
        TemporalPostPass.setupFullscreen(program);
        RenderSystem.enableBlend();
        program.setSampler("SceneDepth", sceneDepth.getDepthTextureId());
        program.setSampler("LocalLights", lights == null ? sceneDepth.getDepthTextureId() : lights.texture());
        program.getUniform("View").set(particleView);
        program.getUniform("Projection").set(TemporalPostPass.matrix(frame.projection()));
        program.getUniform("InverseProjection").set(TemporalPostPass.matrix(frame.inverseProjection()));
        float[] relative = frame.cameraWorld().relativeTo(frame.origin());
        program.getUniform("CameraOrigin").set(relative[0], relative[1], relative[2]);
        program.getUniform("LocalLightCount").set(lights == null ? 0.0F : (float) lights.lights().size());
        program.getUniform("LightTileColumns").set(lights == null ? 1.0F : (float) lights.columns());
        program.getUniform("SoftDistance").set(config.value("particles.softEdges").getAsBoolean() ? value("particles.softDistance") : 0.0F);
        program.getUniform("Lighting").set(config.value("particles.lit").getAsBoolean() ? 1.0F : 0.0F);
        program.getUniform("Ambient").set(value("lighting.ambientFloor"));
        program.getUniform("SkyIntensity").set(value("lighting.skyIntensity"));
        program.getUniform("BlockIntensity").set(value("lighting.blockIntensity"));
        program.getUniform("EmissionStrength").set(emissionStrength());
        program.getUniform("FogDensity").set(fogDensity);
        program.getUniform("FogColor").set(fogColor);
        program.getUniform("FogDistance").set(value("fog.maxDistance"));
        program.getUniform("Mode").set((float) mode);

        try {
            if (depthOrder) {
                int first = 0;
                while (first < visible.size()) {
                    boolean terrain = visible.get(first).terrain();
                    int last = first + 1;
                    while (last < visible.size() && visible.get(last).terrain() == terrain) last++;
                    drawBatch(program, terrain, first, last, false);
                    first = last;
                }
            } else {
                drawBatch(program, false, 0, visible.size(), true);
                drawBatch(program, true, 0, visible.size(), true);
            }
            requireGl("owned particle layer");
        } finally {
            program.clear();
        }
    }

    private void drawBatch(ShaderInstance program, boolean terrain, int first, int last, boolean filterAtlas) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation atlas = terrain ? TextureAtlas.LOCATION_BLOCKS : TextureAtlas.LOCATION_PARTICLES;
        program.setSampler("ParticleAtlas", minecraft.getTextureManager().getTexture(atlas).getId());
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        AppearanceVertex vertices = new AppearanceVertex(buffer);
        try {
            for (int index = first; index < last; index++) {
                Draw entry = visible.get(index);
                if (filterAtlas && entry.terrain() != terrain) continue;
                vertices.appearance = entry.appearance();
                entry.particle().render(vertices, camera, partialTick);
            }
        } catch (RuntimeException | Error failure) {
            buffer.end().release();
            throw failure;
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    private TextureTarget target(TargetPool targets, String name) {
        return targets.acquire(new TargetDescriptor(name, frame.width(), frame.height(),
                TargetDescriptor.Format.RGBA16F, false, 1, 1, 1));
    }

    private static ShaderInstance shader(String name, VertexFormat format) throws IOException {
        return new ShaderInstance(Minecraft.getInstance().getResourceManager(), new ResourceLocation("taste", name), format);
    }

    private static void requireGl(String stage) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) throw new IllegalStateException(stage + " GL error " + error);
    }

    private void report(Exception error) {
        String message = error.getMessage();
        if (!java.util.Objects.equals(lastFailure, message)) LOGGER.warn("Taste particle pass reduced its rendering mode", error);
        lastFailure = message;
    }

    private float value(String id) { return config.value(id).getAsFloat(); }
    private float emissionStrength() {
        if (!config.value("emission.enabled").getAsBoolean()) return 0.0F;
        return value("emission.strength") * value("emission.particleStrength");
    }

    public void atmosphere(float density, Vector3f linearColor) {
        fogDensity = Math.max(0.0F, density);
        fogColor.set(linearColor);
    }

    public TextureTarget emissionTexture() { return emissionTexture; }
    public TextureTarget coverageTexture() { return coverageTexture; }
    public boolean inverseCoverage() { return inverseCoverage; }
    public String effectiveMode() { return effectiveMode; }

    public void end() {
        if (ACTIVE.get() == this) ACTIVE.remove();
        visible.clear();
        frame = null;
        config = null;
        camera = null;
        particleView = null;
        colorTarget = null;
        revealTarget = null;
        compositeTarget = null;
        emissionTarget = null;
        emissionTexture = null;
        coverageTexture = null;
        inverseCoverage = false;
        fogDensity = 0.0F;
        fogColor.zero();
    }

    private static void release(TargetPool targets) {
        targets.release(COLOR);
        targets.release(REVEAL);
        targets.release(COMPOSITE);
        targets.release(EMISSION);
    }

    @Override public void close() {
        end();
        OWNED.clear();
        effectiveMode = "native";
        failedRevision = Long.MIN_VALUE;
        weightedDrawFailed = false;
        if (sorted != null) { sorted.close(); sorted = null; }
        if (weighted != null) { weighted.close(); weighted = null; }
        if (reveal != null) { reveal.close(); reveal = null; }
        if (composite != null) { composite.close(); composite = null; }
    }

    /** Vanilla packed light uses one byte per coordinate. The owned program reads the upper bytes as material flags. */
    private static final class AppearanceVertex implements VertexConsumer {
        private final VertexConsumer output;
        private Appearance appearance;

        private AppearanceVertex(VertexConsumer output) { this.output = output; }

        @Override public VertexConsumer vertex(double x, double y, double z) { output.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { output.color(r, g, b, a); return this; }
        @Override public VertexConsumer uv(float u, float v) { output.uv(u, v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { output.overlayCoords(u, v); return this; }
        @Override public VertexConsumer uv2(int u, int v) {
            output.uv2((u & 255) | (appearance.soft() ? 256 : 0), (v & 255) | (appearance.emission() << 8));
            return this;
        }
        @Override public VertexConsumer normal(float x, float y, float z) { output.normal(x, y, z); return this; }
        @Override public void endVertex() { output.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { output.defaultColor(r, g, b, a); }
        @Override public void unsetDefaultColor() { output.unsetDefaultColor(); }
    }
}

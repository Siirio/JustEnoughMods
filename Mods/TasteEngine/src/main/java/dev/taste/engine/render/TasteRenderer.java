package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import dev.taste.engine.client.ClientState;
import dev.taste.engine.client.CompatibilityReport;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.config.ConsumerRegistry;
import dev.taste.engine.config.EffectiveConfig;
import dev.taste.engine.config.SettingRegistry;
import dev.taste.engine.environment.EnvironmentResolver;
import dev.taste.engine.environment.EnvironmentSnapshot;
import dev.taste.engine.material.MaterialGeneration;
import dev.taste.engine.material.MaterialResolver;
import dev.taste.engine.material.MaterialRuleLoader;
import dev.taste.engine.render.backend.TerrainBackend;
import dev.taste.engine.render.embeddium.Embeddium031Backend;
import dev.taste.engine.render.vanilla.VanillaTerrainBackend;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Owns the world-only render route and the two-stage portable presentation pipeline. */
public final class TasteRenderer implements AutoCloseable, RenderHooks.Listener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TasteRenderer INSTANCE = new TasteRenderer();
    private final EnvironmentResolver environments = new EnvironmentResolver();
    private final FrameCoordinator frames = new FrameCoordinator();
    private final MaterialGeneration materialGeneration = new MaterialGeneration();
    private final MaterialResolver materialResolver = new MaterialResolver();
    private final MaterialRuleLoader materialRules = new MaterialRuleLoader();
    private final AdaptiveQualityController adaptiveQuality = new AdaptiveQualityController();
    private final AllocationLedger allocations = new AllocationLedger();
    private final HandRenderer hand = new HandRenderer();
    private CapabilityProbe.Result probe;
    private SettingRegistry registry;
    private ConsumerRegistry consumers;
    private TargetPool targets;
    private ShaderInstance sceneShader;
    private ShaderInstance finalShader;
    private PassTimerRing timer;
    private TextureTarget worldTarget;
    private TextureTarget sceneTarget;
    private RenderTarget nativeTarget;
    private AutoCloseable targetRoute;
    private WorldViewScope worldScope;
    private FrameCoordinator.Frame activeFrame;
    private TerrainBackend backend;
    private PipelineParameters parameters;
    private EnvironmentSnapshot environment;
    private int width;
    private int height;
    private int nativeWidth;
    private int nativeHeight;
    private long frame;
    private int levelIdentity;
    private long worldEpoch;
    private float partialTick;
    private double lastGpuMilliseconds;
    private String failure = "";
    private boolean loggedFailure;
    private boolean worldOpen;
    private boolean afterLevel;
    private boolean materialRulesLoaded;

    private TasteRenderer() { RenderHooks.setListener(this); }

    public static TasteRenderer instance() { return INSTANCE; }
    public synchronized boolean baselineAvailable() { return probe != null && probe.baseline(); }
    public synchronized Optional<CapabilityProbe.Result> capabilityResult() { return Optional.ofNullable(probe); }
    public synchronized String failure() { return failure; }
    public synchronized long allocatedBytes() { return targets == null ? 0L : targets.allocatedBytes(); }
    public synchronized double lastGpuMilliseconds() { return lastGpuMilliseconds; }

    /** Marks the immutable final-world-depth stage. Composition occurs at the renderLevel return boundary. */
    public synchronized void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && worldOpen) afterLevel = true;
    }

    @Override
    public synchronized void beginWorld(Camera camera, Matrix4f projection, float nextPartialTick) {
        RenderSystem.assertOnRenderThread();
        if (worldOpen) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        ConfigSnapshot requested = ClientState.store().current();
        try {
            if (probe == null) probe = CapabilityProbe.probe();
            EffectiveConfig effective = EffectiveConfig.resolve(requested, ClientState.capabilities());
            if (!requested.requestedEnabled() || !effective.active() || hasBlockingConflict()) {
                releaseGpu();
                return;
            }
            ensureReady(minecraft, requested);
            if (!probe.baseline()) throw new IllegalStateException(probe.reason());
            partialTick = nextPartialTick;
            environment = environments.resolve(minecraft, nextPartialTick);
            parameters = PipelineParameters.resolve(requested, registry, consumers);
            nativeTarget = minecraft.getMainRenderTarget();
            nativeWidth = nativeTarget.width;
            nativeHeight = nativeTarget.height;
            float renderScale = Math.max(0.5F, Math.min(2.0F,
                    value("general.renderScale") * adaptiveQuality.scale()));
            width = Math.max(1, Math.round(nativeWidth * renderScale));
            height = Math.max(1, Math.round(nativeHeight * renderScale));
            int nextIdentity = System.identityHashCode(minecraft.level);
            if (nextIdentity != levelIdentity) {
                levelIdentity = nextIdentity;
                worldEpoch++;
            }
            Optional<FrameSnapshot> nextFrame = snapshot(camera, projection);
            if (nextFrame.isEmpty()) {
                frames.invalidate(TemporalHistory.Reason.PROJECTION_CHANGE);
                failure = "World projection is not ready; native rendering remains active";
                parameters = null;
                environment = null;
                nativeTarget = null;
                return;
            }
            WorldViewScope.ViewKey key = new WorldViewScope.ViewKey(environment.dimension(), 0);
            activeFrame = frames.begin(nextFrame.get(), key, false);
            selectBackend(effective);
            backend.beginFrame(frame);
            worldTarget = targets.acquire(descriptor("world", width, height, true));
            sceneTarget = targets.acquire(descriptor("scene", width, height, true));
            allocations.record("world", new AllocationLedger.Allocation("world", descriptor("world", width, height, true).byteSize(), frame, false));
            allocations.record("scene", new AllocationLedger.Allocation("scene", descriptor("scene", width, height, true).byteSize(), frame, false));
            worldTarget.clear(Minecraft.ON_OSX);
            worldTarget.bindWrite(true);
            targetRoute = WorldTargetRouter.route(worldTarget);
            worldScope = WorldViewScope.enter(key);
            worldOpen = true;
            afterLevel = false;
        } catch (IOException | RuntimeException renderFailure) {
            fail(renderFailure);
            closeWorldScope();
        }
    }

    @Override
    public synchronized void endWorld() {
        if (!worldOpen) return;
        RenderSystem.assertOnRenderThread();
        closeRouteOnly();
        try {
            if (!afterLevel) throw new IllegalStateException("Forge AFTER_LEVEL was not observed before world completion");
            if (timer != null) timer.begin();
            executeGraph();
            if (timer != null) {
                timer.end(frame);
                timer.poll().ifPresent(value -> lastGpuMilliseconds = value);
            }
            frames.complete(activeFrame);
            failure = "";
            loggedFailure = false;
            frame++;
        } catch (Exception renderFailure) {
            restoreCapturedWorld();
            fail(renderFailure);
        } finally {
            closeWorldScope();
        }
    }

    @Override public synchronized void beginLayer(RenderType type) {
        if (worldOpen && backend != null) backend.beginLayer(type.toString());
    }
    @Override public synchronized void endLayer(RenderType type) {
        if (worldOpen && backend != null) backend.endLayer(type.toString());
    }
    @Override public synchronized void beginHand() {
        if (!worldOpen && !hand.capturing()) hand.begin(frame);
    }
    @Override public synchronized void endHand() {
        if (hand.capturing()) hand.end(frame);
    }

    public synchronized void invalidateResources() {
        frames.invalidate(TemporalHistory.Reason.RESOURCE_RELOAD);
        if (RenderSystem.isOnRenderThread()) releaseGpu();
        else RenderSystem.recordRenderCall(this::releaseGpu);
    }

    private void ensureReady(Minecraft minecraft, ConfigSnapshot config) throws IOException {
        if (registry == null) {
            registry = SettingRegistry.bundled();
            consumers = ConsumerRegistry.complete(registry);
            if (!consumers.unassigned(registry).isEmpty())
                throw new IllegalStateException("Unassigned settings: " + consumers.unassigned(registry));
        }
        if (!materialRulesLoaded) {
            materialResolver.clear();
            materialRules.load(minecraft.getResourceManager(), materialResolver);
            materialRulesLoaded = true;
            materialGeneration.advanceRender();
        }
        long budget = Math.max(64L, config.value("performance.memoryBudgetMiB").getAsLong()) * 1024L * 1024L;
        if (targets == null) targets = new TargetPool(new MemoryBudget(budget));
        if (sceneShader == null)
            sceneShader = new ShaderInstance(minecraft.getResourceManager(), new ResourceLocation("taste", "taste_scene"), DefaultVertexFormat.POSITION_TEX);
        if (finalShader == null)
            finalShader = new ShaderInstance(minecraft.getResourceManager(), new ResourceLocation("taste", "taste_final"), DefaultVertexFormat.POSITION_TEX);
        if (timer == null) timer = new PassTimerRing(6);
    }

    private void selectBackend(EffectiveConfig effective) {
        String wanted = effective.backend() == EffectiveConfig.Backend.EMBEDDIUM ? "embeddium-0.3.31" : "vanilla";
        if (backend != null && backend.id().equals(wanted)) return;
        if (backend != null) backend.close();
        if (effective.backend() == EffectiveConfig.Backend.EMBEDDIUM) {
            String version = ModList.get().getModContainerById("embeddium")
                    .map(container -> container.getModInfo().getVersion().toString()).orElse("");
            backend = new Embeddium031Backend(version);
        } else backend = new VanillaTerrainBackend();
        backend.activate(materialGeneration.snapshot());
    }

    private void executeGraph() throws Exception {
        RenderGraph.Resource world = new RenderGraph.Resource("world", "world-target");
        RenderGraph.Resource scene = new RenderGraph.Resource("scene", "scene-target");
        RenderGraph.Resource output = new RenderGraph.Resource("output", "native-target");
        RenderGraph.Compiled graph = RenderGraph.compile(Set.of(world), Set.of(output), java.util.List.of(
                new RenderGraph.Pass("scene-lighting", Set.of(world), Set.of(scene), this::drawScene),
                new RenderGraph.Pass("final-color", Set.of(scene), Set.of(output), this::drawFinal)));
        graph.execute(ignored -> GlStateScope.capture());
    }

    private void drawScene() {
        sceneTarget.bindWrite(true);
        bindCommon(sceneShader, worldTarget);
        set(sceneShader, "SunIntensity", enabled("lighting.sunEnabled") ? value("lighting.sunIntensity") : 0.0F);
        set(sceneShader, "SkyIntensity", value("lighting.skyIntensity"));
        set(sceneShader, "AmbientFloor", value("lighting.ambientFloor"));
        set(sceneShader, "BlockIntensity", value("lighting.blockIntensity"));
        set(sceneShader, "WarmColdBalance", value("lighting.warmColdBalance"));
        set(sceneShader, "ShadowStrength", enabled("shadows.enabled") ? 1.0F : 0.0F);
        set(sceneShader, "ContactShadow", enabled("shadows.contact") ? value("shadows.contactLength") : 0.0F);
        set(sceneShader, "AoStrength", enabled("ao.enabled") ? value("ao.strength") : 0.0F);
        set(sceneShader, "AoRadius", value("ao.radius"));
        set(sceneShader, "GiStrength", value("gi.intensity"));
        set(sceneShader, "EmissionStrength", enabled("emission.enabled") ? value("emission.strength") : 0.0F);
        set(sceneShader, "ReflectionStrength", enabled("reflections.enabled") ? 1.0F : 0.0F);
        set(sceneShader, "CloudCoverage", enabled("clouds.enabled") ? value("clouds.coverage") : 0.0F);
        set(sceneShader, "CloudDensity", value("clouds.density"));
        set(sceneShader, "Wetness", enabled("wetness.enabled") ? environment.rain() * value("wetness.smoothness") : 0.0F);
        set(sceneShader, "WaterStrength", enabled("water.enabled") ? value("water.normalStrength") : 0.0F);
        set(sceneShader, "LavaStrength", enabled("lava.enabled") && enabled("emission.lava")
                ? value("emission.strength") : 0.0F);
        set(sceneShader, "ReducedMotion", enabled("accessibility.reducedMotion") ? 1.0F : 0.0F);
        drawFullscreen();
        sceneShader.clear();
        frames.debug().publish("scene", new DebugAttachments.Attachment(sceneTarget.getColorTextureId(), width, height, "Linear scene composite"));
    }

    private void drawFinal() {
        nativeTarget.bindWrite(true);
        bindCommon(finalShader, sceneTarget);
        set(finalShader, "Exposure", powerOfTwo(value("color.exposureEV")));
        set(finalShader, "Saturation", value("color.saturation"));
        set(finalShader, "Contrast", value("color.contrast"));
        set(finalShader, "Temperature", value("color.temperature"));
        set(finalShader, "Tint", value("color.tint"));
        set(finalShader, "Gamma", value("color.gamma"));
        set(finalShader, "BloomEnabled", enabled("bloom.enabled") ? 1.0F : 0.0F);
        set(finalShader, "BloomStrength", value("bloom.strength"));
        set(finalShader, "BloomThreshold", value("bloom.threshold"));
        set(finalShader, "BloomRadius", value("bloom.radius"));
        set(finalShader, "FogStrength", enabled("fog.enabled") ? value("fog.density") : 0.0F);
        set(finalShader, "FogDistance", value("fog.maxDistance"));
        set(finalShader, "ShaftStrength", enabled("fog.volumetric") ? value("fog.shaftsStrength") : 0.0F);
        set(finalShader, "Vignette", value("camera.vignette"));
        set(finalShader, "Grain", value("camera.grain"));
        set(finalShader, "Sharpen", value("camera.sharpen"));
        set(finalShader, "DofEnabled", enabled("camera.dof") ? 1.0F : 0.0F);
        set(finalShader, "FocusDistance", value("camera.focusDistance"));
        set(finalShader, "Aperture", value("camera.aperture"));
        set(finalShader, "DofRadius", value("camera.dofMaxRadius"));
        set(finalShader, "Outline", enabled("style.outline") ? value("style.outlineStrength") : 0.0F);
        set(finalShader, "OutlineWidth", value("style.outlineWidth"));
        set(finalShader, "ToonBands", parameters.value("style.shading").getAsString().equals("toon") ? value("style.toonBands") : 0.0F);
        set(finalShader, "WetLens", enabled("weather.wetLens") ? 1.0F : 0.0F);
        set(finalShader, "Underwater", environment.underwater() ? 1.0F : 0.0F);
        set(finalShader, "UnderwaterDensity", enabled("underwater.fog") ? value("underwater.fogDensity") : 0.0F);
        set(finalShader, "Thunder", environment.thunder() * value("weather.thunderFlash"));
        set(finalShader, "ReducedMotion", enabled("accessibility.reducedMotion") ? 1.0F : 0.0F);
        set(finalShader, "MinimumContrast", value("accessibility.minimumContrast"));
        set(finalShader, "ToneMapper", parameters.value("color.toneMapper").getAsString().equals("neutral") ? 1.0F : 0.0F);
        drawFullscreen();
        finalShader.clear();
        frames.debug().publish("final", new DebugAttachments.Attachment(nativeTarget.getColorTextureId(), nativeWidth, nativeHeight, "Tone-mapped world before hand and GUI"));
        updateAdaptiveScale();
    }

    private void bindCommon(ShaderInstance shader, TextureTarget input) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);
        shader.setSampler("DiffuseSampler", input.getColorTextureId());
        shader.setSampler("DepthSampler", worldTarget.getDepthTextureId());
        set2(shader, "InSize", input.width, input.height);
        set(shader, "FarPlane", Math.max(64.0F, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0F));
        set(shader, "Time", (float) ((frame + partialTick) / 20.0));
        set(shader, "Rain", environment.rain());
    }

    private void updateAdaptiveScale() {
        float scale = adaptiveQuality.update(lastGpuMilliseconds, value("performance.targetMs"), 1.0F / 60.0F,
                value("performance.minimumScale"), value("performance.recoverySeconds"), enabled("performance.adaptive"));
        if (scale < 0.999F) frames.debug().publish("adaptive-scale",
                new DebugAttachments.Attachment(0, Math.round(width * scale), Math.round(height * scale), "Effective internal scale"));
    }

    private Optional<FrameSnapshot> snapshot(Camera camera, Matrix4f projection) {
        if (!finite(projection)) return Optional.empty();
        float determinant = projection.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1.0E-8F) return Optional.empty();
        Vec3 position = camera.getPosition();
        FrameSnapshot.Vec3d cameraWorld = new FrameSnapshot.Vec3d(position.x, position.y, position.z);
        FrameSnapshot.Vec3d origin = new FrameSnapshot.Vec3d(Math.floor(position.x / 256.0) * 256.0,
                Math.floor(position.y / 256.0) * 256.0, Math.floor(position.z / 256.0) * 256.0);
        Matrix4f inverse = new Matrix4f(projection).invert();
        if (!finite(inverse)) return Optional.empty();
        return Optional.of(new FrameSnapshot(frame, worldEpoch, environment.dimension(), origin, cameraWorld,
                matrix(new Matrix4f()), matrix(projection), matrix(inverse), matrix(projection),
                width, height, (environment.gameTime() + partialTick) / 20.0));
    }

    private static boolean finite(Matrix4f value) {
        float[] elements = new float[16];
        value.get(elements);
        for (float element : elements) if (!Float.isFinite(element)) return false;
        return true;
    }

    private static FrameSnapshot.Matrix4 matrix(Matrix4f value) {
        float[] elements = new float[16];
        value.get(elements);
        return new FrameSnapshot.Matrix4(elements);
    }

    private boolean hasBlockingConflict() {
        return new CompatibilityReport().collect(probe).stream()
                .anyMatch(finding -> finding.severity() == CompatibilityReport.Severity.BLOCKING);
    }

    private void closeRouteOnly() {
        if (targetRoute != null) {
            try { targetRoute.close(); }
            catch (Exception closeFailure) { LOGGER.error("Could not close Taste world target route", closeFailure); }
            targetRoute = null;
        }
        if (nativeTarget != null) nativeTarget.bindWrite(true);
    }

    private void restoreCapturedWorld() {
        if (worldTarget == null || nativeTarget == null) return;
        try (GlStateScope ignored = GlStateScope.capture()) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, worldTarget.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, nativeTarget.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, worldTarget.width, worldTarget.height,
                    0, 0, nativeTarget.width, nativeTarget.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } catch (RuntimeException recoveryFailure) {
            LOGGER.error("Taste could not restore the captured native world after a pipeline failure", recoveryFailure);
        }
    }

    private void closeWorldScope() {
        closeRouteOnly();
        if (worldScope != null) {
            worldScope.close();
            worldScope = null;
        }
        worldOpen = false;
        afterLevel = false;
        activeFrame = null;
        parameters = null;
        environment = null;
        nativeTarget = null;
    }

    private void fail(Exception renderFailure) {
        failure = renderFailure.getMessage() == null ? renderFailure.getClass().getSimpleName() : renderFailure.getMessage();
        frames.invalidate(TemporalHistory.Reason.PIPELINE_CHANGE);
        if (!loggedFailure) {
            LOGGER.error("Taste renderer restored native ownership after a frame failure", renderFailure);
            loggedFailure = true;
        }
    }

    private void releaseGpu() {
        closeWorldScope();
        if (backend != null) { backend.close(); backend = null; }
        if (timer != null) { timer.close(); timer = null; }
        if (targets != null) { targets.close(); targets = null; }
        if (sceneShader != null) { sceneShader.close(); sceneShader = null; }
        if (finalShader != null) { finalShader.close(); finalShader = null; }
        allocations.clear();
        frames.clear();
        materialRulesLoaded = false;
        width = 0;
        height = 0;
        nativeWidth = 0;
        nativeHeight = 0;
    }

    private static TargetDescriptor descriptor(String name, int width, int height, boolean depth) {
        return new TargetDescriptor(name, width, height, TargetDescriptor.Format.RGBA16F, depth, 1, 1, 1);
    }

    private float value(String id) { return parameters.value(id).getAsFloat(); }
    private boolean enabled(String id) { return parameters.value(id).getAsBoolean(); }
    private static float powerOfTwo(float exponent) { return (float) Math.pow(2.0, exponent); }
    private static void set(ShaderInstance shader, String name, float value) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(value);
    }
    private static void set2(ShaderInstance shader, String name, float x, float y) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(x, y);
    }
    private static void drawFullscreen() {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
        builder.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
        builder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
        builder.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    @Override public synchronized void close() { releaseGpu(); }
}

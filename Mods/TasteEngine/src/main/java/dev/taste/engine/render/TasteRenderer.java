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
import dev.taste.engine.effects.BloomPyramid;
import dev.taste.engine.environment.EnvironmentResolver;
import dev.taste.engine.environment.EnvironmentSnapshot;
import dev.taste.engine.light.ShadowRenderer;
import dev.taste.engine.light.LocalLightingPass;
import dev.taste.engine.material.MaterialGeneration;
import dev.taste.engine.material.MaterialResolver;
import dev.taste.engine.material.MaterialRuleLoader;
import dev.taste.engine.render.backend.TerrainBackend;
import dev.taste.engine.render.embeddium.Embeddium031Backend;
import dev.taste.engine.render.vanilla.VanillaTerrainBackend;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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
    private final ShadowRenderer shadows = new ShadowRenderer();
    private final BloomPyramid bloomPyramid = new BloomPyramid();
    private final IndirectLightingPass indirect = new IndirectLightingPass();
    private final LocalLightingPass localLighting = new LocalLightingPass();
    private CapabilityProbe.Result probe;
    private SettingRegistry registry;
    private ConsumerRegistry consumers;
    private TargetPool targets;
    private ShaderInstance sceneShader;
    private ShaderInstance finalShader;
    private ShaderInstance bloomShader;
    private ShaderInstance exposureShader;
    private PassTimerRing timer;
    private TextureTarget worldTarget;
    private TextureTarget opaqueTarget;
    private TextureTarget indirectTarget;
    private boolean opaqueCaptured;
    private boolean screenGi;
    private LocalLightingPass.TileData lightTiles;
    private ShadowRenderer.PointMaps pointMaps;
    private TextureTarget sceneTarget;
    private TextureTarget volumeTarget;
    private TextureTarget bloomTarget;
    private TextureTarget exposureTarget;
    private long exposureWorldEpoch = -1;
    private long exposureTime;
    private int exposureIndex;
    private int allocatedBloomLevels;
    private RenderTarget nativeTarget;
    private AutoCloseable targetRoute;
    private WorldViewScope worldScope;
    private FrameCoordinator.Frame activeFrame;
    private TerrainBackend backend;
    private PipelineParameters parameters;
    private EnvironmentSnapshot environment;
    private List<ShadowRenderer.MapLayer> shadowMaps = List.of();
    private Vector3f towardsSun;
    private boolean daylight;
    private float directionalIntensity;
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
    private boolean materialRulesLoaded;

    private TasteRenderer() { RenderHooks.setListener(this); }

    public static TasteRenderer instance() { return INSTANCE; }
    public synchronized boolean baselineAvailable() { return probe != null && probe.baseline(); }
    public synchronized Optional<CapabilityProbe.Result> capabilityResult() { return Optional.ofNullable(probe); }
    public synchronized String failure() { return failure; }
    public synchronized long allocatedBytes() { return targets == null ? 0L : targets.allocatedBytes(); }
    public synchronized double lastGpuMilliseconds() { return lastGpuMilliseconds; }
    public synchronized void dirtyLocalLight(ClientLevel level, BlockPos position) {
        if (Minecraft.getInstance().level == level) {
            localLighting.dirty(position);
            shadows.invalidatePointMaps();
        }
    }

    @Override
    public synchronized void beginWorld(Camera camera, Matrix4f cameraView, Matrix4f projection, float nextPartialTick) {
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
            screenGi = effective.values().get("gi.mode").getAsString().equals("screen");
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
            Optional<FrameSnapshot> nextFrame = snapshot(camera, cameraView, projection);
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
            if (timer != null) timer.begin();
            executeGraph();
            if (timer != null) timer.poll().ifPresent(value -> lastGpuMilliseconds = value);
            frames.complete(activeFrame);
            failure = "";
            loggedFailure = false;
            frame++;
        } catch (Exception renderFailure) {
            restoreCapturedWorld();
            fail(renderFailure);
        } finally {
            if (timer != null) timer.end(frame);
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

    @Override public synchronized void beforeTranslucentTerrain() {
        if (!worldOpen || opaqueCaptured) return;
        try (GlStateScope ignored = GlStateScope.capture()) {
            clearGlErrors();
            opaqueTarget = targets.acquire(descriptor("opaque", width, height, true));
            RenderSystem.disableScissor();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, worldTarget.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, opaqueTarget.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            requireNoGlError("opaque terrain capture");
            opaqueCaptured = true;
            frames.debug().publish("opaque-color", new DebugAttachments.Attachment(
                    opaqueTarget.getColorTextureId(), width, height, "World color before translucent terrain"));
            frames.debug().publish("opaque-depth", new DebugAttachments.Attachment(
                    opaqueTarget.getDepthTextureId(), width, height, "World depth before translucent terrain"));
        } catch (RuntimeException captureFailure) {
            opaqueTarget = null;
            fail(captureFailure);
        }
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
        RenderGraph.Resource opaque = new RenderGraph.Resource("opaque", "opaque-target");
        RenderGraph.Resource diffuse = new RenderGraph.Resource("indirect", "indirect-history");
        RenderGraph.Resource visibility = new RenderGraph.Resource("sun-visibility", "sun-shadow-maps");
        RenderGraph.Resource local = new RenderGraph.Resource("local-light-tiles", "local-light-data");
        RenderGraph.Resource pointShadows = new RenderGraph.Resource("point-visibility", "point-shadow-atlas");
        RenderGraph.Resource volume = new RenderGraph.Resource("atmosphere", "atmosphere-target");
        RenderGraph.Resource scene = new RenderGraph.Resource("scene", "scene-target");
        RenderGraph.Resource bloom = new RenderGraph.Resource("bloom", "bloom-pyramid");
        RenderGraph.Resource exposure = new RenderGraph.Resource("exposure", "exposure-history");
        RenderGraph.Resource output = new RenderGraph.Resource("output", "native-target");
        RenderGraph.Compiled graph = RenderGraph.compile(Set.of(world, opaque), Set.of(output), List.of(
                new RenderGraph.Pass("sun-visibility", Set.of(), Set.of(visibility), this::drawShadowMaps),
                new RenderGraph.Pass("local-light-tiles", Set.of(), Set.of(local), this::prepareLocalLighting),
                new RenderGraph.Pass("point-visibility", Set.of(local), Set.of(pointShadows), this::drawPointShadows),
                new RenderGraph.Pass("atmosphere", Set.of(world, opaque, visibility, local, pointShadows), Set.of(volume), this::drawVolume),
                new RenderGraph.Pass("indirect", Set.of(opaque), Set.of(diffuse), this::drawIndirect),
                new RenderGraph.Pass("scene-lighting", Set.of(world, opaque, visibility, volume, diffuse, local, pointShadows), Set.of(scene), this::drawScene),
                new RenderGraph.Pass("bloom", Set.of(world, scene), Set.of(bloom), this::drawBloom),
                new RenderGraph.Pass("exposure", Set.of(scene), Set.of(exposure), this::drawExposure),
                new RenderGraph.Pass("final-color", Set.of(world, scene, bloom, exposure), Set.of(output), this::drawFinal)));
        graph.execute(ignored -> GlStateScope.capture());
    }

    private void drawShadowMaps() throws IOException {
        shadowMaps = List.of();
        float angle = environment.sunAngle();
        towardsSun = new Vector3f(-(float) Math.sin(angle), (float) Math.cos(angle), 0);
        daylight = towardsSun.y >= 0;
        if (!daylight) towardsSun.negate();
        directionalIntensity = !environment.hasSky() ? 0
                : daylight ? (enabled("lighting.sunEnabled") ? value("lighting.sunIntensity") : 0)
                            : value("lighting.moonIntensity");
        directionalIntensity *= Math.min(1, towardsSun.y / 0.08F);
        boolean needShafts = volumeEnabled() && value("fog.shaftsStrength") > 0;
        if (!(enabled("shadows.enabled") || needShafts) || directionalIntensity <= 0) {
            shadows.releaseFrom(targets, 0);
            return;
        }
        clearGlErrors();
        int resolution = Integer.parseInt(parameters.value("shadows.resolution").getAsString());
        shadowMaps = shadows.draw(backend, targets, activeFrame.snapshot(), towardsSun, resolution,
                parameters.value("shadows.cascades").getAsInt(), value("shadows.distance"), value("shadows.splitLambda"));
        requireNoGlError("sun-visibility");
        for (int index = 0; index < shadowMaps.size(); index++) {
            ShadowRenderer.MapLayer layer = shadowMaps.get(index);
            frames.debug().publish("sun-shadow-" + index, new DebugAttachments.Attachment(
                    layer.texture(), layer.resolution(), layer.resolution(),
                    "Directional terrain depth, " + layer.submissions() + " caster submissions"));
        }
    }

    private void bindShadowMaps() {
        set(sceneShader, "CascadeCount", shadowMaps.size());
        set(sceneShader, "ShadowBias", value("shadows.bias"));
        set(sceneShader, "ShadowNormalBias", value("shadows.normalBias"));
        set(sceneShader, "ShadowSamples", value("shadows.filterSamples"));
        set(sceneShader, "SunAngularRadius", value("shadows.sunAngularRadius"));
        int shadowFilter = switch (parameters.value("shadows.filter").getAsString()) {
            case "pcss" -> 2;
            case "pcf" -> 1;
            default -> 0;
        };
        set(sceneShader, "ShadowFilter", shadowFilter);
        set(sceneShader, "VolumeSteps", value("fog.steps"));
        set(sceneShader, "VolumeDensity", value("fog.density"));
        set(sceneShader, "VolumeDistance", value("fog.maxDistance"));
        set(sceneShader, "VolumeAnisotropy", value("fog.anisotropy"));
        set(sceneShader, "VolumeStrength", value("fog.shaftsStrength"));
        set(sceneShader, "VolumeHeightBase", value("fog.heightBase") - (float) activeFrame.snapshot().origin().y());
        set(sceneShader, "VolumeHeightFalloff", value("fog.heightFalloff"));
        sceneShader.getUniform("TowardsSun").set(towardsSun);
        sceneShader.getUniform("SunColor").set(linearColor(daylight ? "lighting.sunColor" : "lighting.moonColor"));
        sceneShader.getUniform("FogColor").set(linearColor("fog.color"));
        Matrix4f view = new Matrix4f().set(activeFrame.snapshot().view().columnMajor());
        sceneShader.getUniform("InverseView").set(view.invert());
        sceneShader.getUniform("Projection").set(new Matrix4f().set(activeFrame.snapshot().projection().columnMajor()));
        for (int index = 0; index < 4; index++) {
            ShadowRenderer.MapLayer layer = index < shadowMaps.size() ? shadowMaps.get(index) : null;
            sceneShader.setSampler("ShadowMap" + index, layer == null ? worldTarget.getDepthTextureId() : layer.texture());
            if (layer == null) continue;
            sceneShader.getUniform("LightProjection" + index).set(layer.view().projection());
            set2(sceneShader, "CascadeRange" + index, layer.view().far(), layer.view().depthSpan());
        }
    }

    private Vector3f linearColor(String setting) {
        int color = Integer.parseInt(parameters.value(setting).getAsString().substring(1), 16);
        return new Vector3f(linearChannel(color >> 16), linearChannel(color >> 8), linearChannel(color));
    }

    private static float linearChannel(int channel) {
        float s = (channel & 255) / 255.0F;
        return s <= 0.04045F ? s / 12.92F : (float) Math.pow((s + 0.055F) / 1.055F, 2.4);
    }

    private boolean volumeEnabled() {
        return enabled("fog.enabled") && enabled("fog.volumetric") && value("fog.density") > 0;
    }

    private void prepareLocalLighting() {
        clearGlErrors();
        lightTiles = localLighting.prepare(targets, activeFrame.snapshot(), parameters, materialResolver);
        requireNoGlError("local light upload");
    }

    private void drawPointShadows() throws IOException {
        clearGlErrors();
        int resolution = Integer.parseInt(parameters.value("shadows.localResolution").getAsString());
        int count = enabled("shadows.enabled") && enabled("shadows.localLights")
                ? parameters.value("shadows.maxLocalLights").getAsInt() : 0;
        int cadence = parameters.value("shadows.dynamicEveryFrames").getAsInt();
        int refresh = enabled("shadows.cacheStatic") ? Math.max(cadence, 20) : cadence;
        pointMaps = shadows.drawPointMaps(backend, targets, activeFrame.snapshot(),
                lightTiles == null ? List.of() : lightTiles.lights(), resolution, count, refresh);
        requireNoGlError("point-light visibility");
        if (pointMaps != null) {
            frames.debug().publish("point-shadow-atlas", new DebugAttachments.Attachment(pointMaps.texture(),
                    pointMaps.columns() * resolution, pointMaps.rows() * resolution, "Point light terrain depth"));
        }
    }

    private void bindLocalLighting() {
        sceneShader.setSampler("LocalLights", lightTiles == null ? worldTarget.getColorTextureId() : lightTiles.texture());
        sceneShader.setSampler("PointShadowAtlas", pointMaps == null ? worldTarget.getDepthTextureId() : pointMaps.texture());
        set(sceneShader, "LocalLightCount", lightTiles == null ? 0 : lightTiles.lights().size());
        set(sceneShader, "PointShadowCount", pointMaps == null ? 0 : pointMaps.count());
        set2(sceneShader, "PointShadowGrid", pointMaps == null ? 1 : pointMaps.columns(), pointMaps == null ? 1 : pointMaps.rows());
        set(sceneShader, "LightTileColumns", lightTiles == null ? 1 : lightTiles.columns());
        set2(sceneShader, "LightViewport", width, height);
        set(sceneShader, "LocalVolumes", enabled("fog.localLightVolumes") ? 1 : 0);
        set(sceneShader, "BlockIntensity", value("lighting.blockIntensity"));
    }

    private void drawIndirect() throws IOException {
        clearGlErrors();
        indirectTarget = indirect.render(targets, opaqueTarget, activeFrame.snapshot(), parameters, screenGi,
                TasteRenderer::drawFullscreen);
        requireNoGlError("indirect lighting");
        if (indirectTarget != null) {
            frames.debug().publish("indirect", new DebugAttachments.Attachment(indirectTarget.getColorTextureId(),
                    indirectTarget.width, indirectTarget.height, "Screen diffuse bounce and ambient occlusion"));
        }
    }

    private void drawVolume() {
        if (!volumeEnabled()) {
            targets.release("atmosphere");
            volumeTarget = null;
            return;
        }
        clearGlErrors();
        float scale = Float.parseFloat(parameters.value("fog.resolutionScale").getAsString());
        int volumeWidth = Math.max(1, Math.round(width * scale));
        int volumeHeight = Math.max(1, Math.round(height * scale));
        volumeTarget = targets.acquire(descriptor("atmosphere", volumeWidth, volumeHeight, false));
        volumeTarget.bindWrite(true);
        bindCommon(sceneShader, worldTarget);
        bindShadowMaps();
        sceneShader.setSampler("VolumeSampler", worldTarget.getColorTextureId());
        set(sceneShader, "VolumeOnly", 1);
        set(sceneShader, "VolumeEnabled", 0);
        bindIndirect();
        bindLocalLighting();
        set(sceneShader, "SunIntensity", directionalIntensity);
        try {
            drawFullscreen();
            requireNoGlError("atmosphere");
        } finally {
            sceneShader.clear();
        }
        frames.debug().publish("atmosphere", new DebugAttachments.Attachment(volumeTarget.getColorTextureId(),
                volumeWidth, volumeHeight, "Atmospheric scattering and transmittance"));
    }

    private void drawScene() {
        clearGlErrors();
        sceneTarget.bindWrite(true);
        bindCommon(sceneShader, worldTarget);
        bindShadowMaps();
        sceneShader.setSampler("VolumeSampler", volumeTarget == null ? worldTarget.getColorTextureId() : volumeTarget.getColorTextureId());
        set(sceneShader, "VolumeOnly", 0);
        set(sceneShader, "VolumeEnabled", volumeTarget == null ? 0 : 1);
        bindIndirect();
        bindLocalLighting();
        bindSky();
        set(sceneShader, "SunIntensity", directionalIntensity);
        set(sceneShader, "SkyIntensity", value("lighting.skyIntensity"));
        set(sceneShader, "AmbientFloor", value("lighting.ambientFloor"));
        set(sceneShader, "BlockIntensity", value("lighting.blockIntensity"));
        set(sceneShader, "WarmColdBalance", value("lighting.warmColdBalance"));
        set(sceneShader, "ShadowStrength", enabled("shadows.enabled") ? 1.0F : 0.0F);
        set(sceneShader, "ContactShadow", enabled("shadows.contact") ? value("shadows.contactLength") : 0.0F);
        set(sceneShader, "EmissionStrength", enabled("emission.enabled") ? value("emission.strength") : 0.0F);
        set(sceneShader, "ReflectionStrength", enabled("reflections.enabled") ? 1.0F : 0.0F);
        set(sceneShader, "CloudCoverage", enabled("clouds.enabled") ? value("clouds.coverage") : 0.0F);
        set(sceneShader, "CloudDensity", value("clouds.density"));
        set(sceneShader, "Wetness", enabled("wetness.enabled") ? environment.rain() * value("wetness.smoothness") : 0.0F);
        set(sceneShader, "WaterStrength", enabled("water.enabled") ? value("water.normalStrength") : 0.0F);
        set(sceneShader, "LavaStrength", enabled("lava.enabled") && enabled("emission.lava")
                ? value("emission.strength") : 0.0F);
        set(sceneShader, "ReducedMotion", enabled("accessibility.reducedMotion") ? 1.0F : 0.0F);
        try {
            drawFullscreen();
            requireNoGlError("scene-lighting");
        } finally {
            sceneShader.clear();
        }
        frames.debug().publish("scene", new DebugAttachments.Attachment(sceneTarget.getColorTextureId(), width, height, "Linear scene composite"));
    }

    private void bindIndirect() {
        TextureTarget opaque = opaqueTarget == null ? worldTarget : opaqueTarget;
        TextureTarget lighting = indirectTarget == null ? opaque : indirectTarget;
        sceneShader.setSampler("OpaqueDepth", opaque.getDepthTextureId());
        sceneShader.setSampler("IndirectSampler", lighting.getColorTextureId());
        sceneShader.setSampler("IndirectDepth", lighting.getDepthTextureId());
        set(sceneShader, "IndirectEnabled", indirectTarget == null ? 0 : 1);
    }

    private void bindSky() {
        String mode = parameters.value("sky.mode").getAsString();
        ClientLevel level = Minecraft.getInstance().level;
        int skyMode = level == null || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL
                ? 0 : switch (mode) {
                    case "stylized" -> 1;
                    case "atmospheric" -> 2;
                    default -> 0;
                };
        set(sceneShader, "SkyMode", skyMode);
        set(sceneShader, "SkySunShape", parameters.value("sky.sunShape").getAsString().equals("square") ? 1 : 0);
        set(sceneShader, "SkySunSize", value("sky.sunSize"));
        set(sceneShader, "SkyMoonSize", value("sky.moonSize"));
        set(sceneShader, "SkyMoonPhase", environment.moonPhase());
        set(sceneShader, "SkySaturation", value("sky.sunsetSaturation"));
        set(sceneShader, "SkyStarDensity", enabled("sky.stars") ? value("sky.starDensity") : 0);
        set(sceneShader, "SkyStarBrightness", value("sky.starBrightness"));
        set(sceneShader, "SkyAurora", enabled("sky.aurora") ? value("sky.auroraStrength") : 0);
        set(sceneShader, "SkyMilkyWay", enabled("sky.milkyWay") ? 1 : 0);
        set(sceneShader, "SkyRainbow", enabled("sky.rainbow") ? 1 : 0);
        set(sceneShader, "SkyTurbidity", value("sky.atmosphereTurbidity"));
        float angle = environment.sunAngle();
        sceneShader.getUniform("SkySolarDirection").set(-(float) Math.sin(angle), (float) Math.cos(angle), 0);
        sceneShader.getUniform("SkyZenith").set(linearColor("sky.zenithColor"));
        sceneShader.getUniform("SkyHorizon").set(linearColor("sky.horizonColor"));
        sceneShader.getUniform("SkyMoonColor").set(linearColor("lighting.moonColor"));
        sceneShader.getUniform("SkyDiscColor").set(linearColor("lighting.sunColor"));
    }

    private void drawFinal() {
        clearGlErrors();
        nativeTarget.bindWrite(true);
        bindCommon(finalShader, sceneTarget);
        set(finalShader, "Exposure", powerOfTwo(value("color.exposureEV")));
        set(finalShader, "AutoExposure", exposureTarget == null ? 0 : 1);
        finalShader.setSampler("ExposureSampler", exposureTarget == null ? sceneTarget.getColorTextureId() : exposureTarget.getColorTextureId());
        set(finalShader, "Saturation", value("color.saturation"));
        set(finalShader, "Contrast", value("color.contrast"));
        set(finalShader, "Temperature", value("color.temperature"));
        set(finalShader, "Tint", value("color.tint"));
        set(finalShader, "Gamma", value("color.gamma"));
        finalShader.getUniform("ShadowTint").set(linearColor("color.shadowTint"));
        finalShader.getUniform("HighlightTint").set(linearColor("color.highlightTint"));
        finalShader.setSampler("BloomSampler", bloomTarget == null ? sceneTarget.getColorTextureId() : bloomTarget.getColorTextureId());
        set(finalShader, "BloomStrength", bloomTarget == null ? 0 : value("bloom.strength"));
        set(finalShader, "FogStrength", enabled("fog.enabled") && volumeTarget == null ? value("fog.density") : 0.0F);
        set(finalShader, "FogDistance", value("fog.maxDistance"));
        finalShader.getUniform("FogColor").set(linearColor("fog.color"));
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
        try {
            drawFullscreen();
            requireNoGlError("final-color");
        } finally {
            finalShader.clear();
        }
        frames.debug().publish("final", new DebugAttachments.Attachment(nativeTarget.getColorTextureId(), nativeWidth, nativeHeight, "Tone-mapped world before hand and GUI"));
        updateAdaptiveScale();
    }

    private void drawBloom() throws IOException {
        bloomTarget = null;
        List<BloomPyramid.Level> levels = enabled("bloom.enabled") && value("bloom.strength") > 0
                ? bloomPyramid.layout(width, height, parameters.value("bloom.levels").getAsInt()) : List.of();
        for (int index = levels.size(); index < allocatedBloomLevels; index++) {
            targets.release("bloom-down-" + index);
        }
        for (int index = Math.max(0, levels.size() - 1); index < allocatedBloomLevels - 1; index++) {
            targets.release("bloom-up-" + index);
        }
        allocatedBloomLevels = levels.size();
        if (levels.isEmpty()) return;
        clearGlErrors();
        if (bloomShader == null) {
            bloomShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation("taste", "taste_bloom"), DefaultVertexFormat.POSITION_TEX);
        }
        TextureTarget input = sceneTarget;
        TextureTarget[] down = new TextureTarget[levels.size()];
        for (BloomPyramid.Level level : levels) {
            TextureTarget destination = targets.acquire(descriptor("bloom-down-" + level.index(), level.width(), level.height(), false));
            renderBloom(input, null, destination, level.index() == 0 ? 0 : 1);
            down[level.index()] = destination;
            input = destination;
        }
        for (int index = levels.size() - 2; index >= 0; index--) {
            BloomPyramid.Level level = levels.get(index);
            TextureTarget destination = targets.acquire(descriptor("bloom-up-" + index, level.width(), level.height(), false));
            renderBloom(input, down[index], destination, 2);
            input = destination;
        }
        requireNoGlError("bloom");
        bloomTarget = input;
        frames.debug().publish("bloom", new DebugAttachments.Attachment(input.getColorTextureId(),
                input.width, input.height, "Reconstructed bloom pyramid"));
    }

    private void drawExposure() throws IOException {
        if (!enabled("color.autoExposure")) {
            targets.release("exposure-0");
            targets.release("exposure-1");
            resetExposure();
            return;
        }
        clearGlErrors();
        if (exposureShader == null) {
            exposureShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation("taste", "taste_exposure"), DefaultVertexFormat.POSITION_TEX);
        }
        TextureTarget next = targets.acquire(descriptor("exposure-" + exposureIndex, 1, 1, false));
        next.bindWrite(true);
        bindCommon(exposureShader, sceneTarget);
        boolean history = exposureTarget != null && exposureWorldEpoch == worldEpoch;
        exposureShader.setSampler("PreviousSampler", history ? exposureTarget.getColorTextureId() : sceneTarget.getColorTextureId());
        long now = System.nanoTime();
        float elapsed = Math.min(1, (now - exposureTime) / 1_000_000_000F);
        set(exposureShader, "HasHistory", history ? 1 : 0);
        set(exposureShader, "DeltaSeconds", elapsed);
        set(exposureShader, "MinEV", value("color.minEV"));
        set(exposureShader, "MaxEV", value("color.maxEV"));
        set(exposureShader, "LightSeconds", value("color.adaptLightSeconds"));
        set(exposureShader, "DarkSeconds", value("color.adaptDarkSeconds"));
        try {
            drawFullscreen();
            requireNoGlError("exposure");
        } finally {
            exposureShader.clear();
        }
        exposureTarget = next;
        exposureWorldEpoch = worldEpoch;
        exposureTime = now;
        exposureIndex ^= 1;
    }

    private void resetExposure() {
        exposureTarget = null;
        exposureWorldEpoch = -1;
        exposureTime = 0;
        exposureIndex = 0;
    }

    private void renderBloom(TextureTarget input, TextureTarget fine, TextureTarget destination, int mode) {
        destination.setFilterMode(GL11.GL_LINEAR);
        destination.bindWrite(true);
        bindCommon(bloomShader, input);
        bloomShader.setSampler("FineSampler", fine == null ? input.getColorTextureId() : fine.getColorTextureId());
        set(bloomShader, "Mode", mode);
        set(bloomShader, "Threshold", value("bloom.threshold"));
        set(bloomShader, "Knee", value("bloom.knee"));
        set(bloomShader, "SkyWeight", value("bloom.skyWeight"));
        set(bloomShader, "Radius", value("bloom.radius"));
        set(bloomShader, "Anamorphic", value("bloom.anamorphic"));
        try {
            drawFullscreen();
        } finally {
            bloomShader.clear();
        }
    }

    private void bindCommon(ShaderInstance shader, TextureTarget input) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("DiffuseSampler", input.getColorTextureId());
        shader.setSampler("DepthSampler", worldTarget.getDepthTextureId());
        set2(shader, "InSize", input.width, input.height);
        var inverseProjection = shader.getUniform("InverseProjection");
        if (inverseProjection != null)
            inverseProjection.set(new Matrix4f().set(activeFrame.snapshot().inverseProjection().columnMajor()));
        set(shader, "Time", (float) (((environment.gameTime() % 1_728_000L) + partialTick) / 20.0));
        set(shader, "Rain", environment.rain());
    }

    private void updateAdaptiveScale() {
        float scale = adaptiveQuality.update(lastGpuMilliseconds, value("performance.targetMs"), 1.0F / 60.0F,
                value("performance.minimumScale"), value("performance.recoverySeconds"), enabled("performance.adaptive"));
        if (scale < 0.999F) frames.debug().publish("adaptive-scale",
                new DebugAttachments.Attachment(0, Math.round(width * scale), Math.round(height * scale), "Effective internal scale"));
    }

    private Optional<FrameSnapshot> snapshot(Camera camera, Matrix4f cameraView, Matrix4f projection) {
        if (!finite(cameraView) || !finite(projection)) return Optional.empty();
        float determinant = projection.determinant();
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1.0E-8F) return Optional.empty();
        Vec3 position = camera.getPosition();
        FrameSnapshot.Vec3d cameraWorld = new FrameSnapshot.Vec3d(position.x, position.y, position.z);
        FrameSnapshot.Vec3d origin = new FrameSnapshot.Vec3d(Math.floor(position.x / 256.0) * 256.0,
                Math.floor(position.y / 256.0) * 256.0, Math.floor(position.z / 256.0) * 256.0);
        Matrix4f inverse = new Matrix4f(projection).invert();
        if (!finite(inverse)) return Optional.empty();
        float[] relativeCamera = cameraWorld.relativeTo(origin);
        Matrix4f view = new Matrix4f(cameraView).translate(-relativeCamera[0], -relativeCamera[1], -relativeCamera[2]);
        return Optional.of(new FrameSnapshot(frame, worldEpoch, environment.dimension(), origin, cameraWorld,
                matrix(view), matrix(projection), matrix(inverse), matrix(projection),
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
        activeFrame = null;
        parameters = null;
        environment = null;
        opaqueCaptured = false;
        opaqueTarget = null;
        indirectTarget = null;
        screenGi = false;
        lightTiles = null;
        pointMaps = null;
        volumeTarget = null;
        bloomTarget = null;
        shadowMaps = List.of();
        towardsSun = null;
        nativeTarget = null;
    }

    private void fail(Exception renderFailure) {
        failure = renderFailure.getMessage() == null ? renderFailure.getClass().getSimpleName() : renderFailure.getMessage();
        frames.invalidate(TemporalHistory.Reason.PIPELINE_CHANGE);
        indirect.invalidate();
        shadows.invalidatePointMaps();
        resetExposure();
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
        if (bloomShader != null) { bloomShader.close(); bloomShader = null; }
        if (exposureShader != null) { exposureShader.close(); exposureShader = null; }
        resetExposure();
        allocatedBloomLevels = 0;
        shadows.close();
        indirect.close();
        localLighting.close();
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

    private static void clearGlErrors() {
        for (int count = 0; count < 32 && GL11.glGetError() != GL11.GL_NO_ERROR; count++) {
            // The debug callback has already reported errors from the preceding foreign render stage.
        }
    }

    private static void requireNoGlError(String pass) {
        int error = GL11.glGetError();
        if (error == GL11.GL_NO_ERROR) return;
        for (int count = 0; count < 31 && GL11.glGetError() != GL11.GL_NO_ERROR; count++) {
            // Drain all errors owned by this pass before native rendering resumes.
        }
        throw new IllegalStateException("OpenGL error 0x" + Integer.toHexString(error) + " in Taste pass " + pass);
    }

    @Override public synchronized void close() { releaseGpu(); }
}

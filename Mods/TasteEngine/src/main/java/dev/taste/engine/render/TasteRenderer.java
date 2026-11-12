package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.google.gson.GsonBuilder;
import dev.taste.engine.client.ClientState;
import dev.taste.engine.client.ClientEvents;
import dev.taste.engine.client.CompatibilityReport;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.config.ConsumerRegistry;
import dev.taste.engine.config.EffectiveConfig;
import dev.taste.engine.config.SettingRegistry;
import dev.taste.engine.effects.BloomPyramid;
import dev.taste.engine.effects.PhotoCapture;
import dev.taste.engine.effects.PortalScenePass;
import dev.taste.engine.effects.VegetationMotion;
import dev.taste.engine.environment.CloudRenderer;
import dev.taste.engine.environment.DimensionProfiles;
import dev.taste.engine.environment.EnvironmentResolver;
import dev.taste.engine.environment.EnvironmentSnapshot;
import dev.taste.engine.environment.LensWetness;
import dev.taste.engine.environment.LightningEvents;
import dev.taste.engine.api.LightProvider.Light;
import dev.taste.engine.light.ShadowRenderer;
import dev.taste.engine.light.LocalLightingPass;
import dev.taste.engine.light.VoxelSectionCache;
import dev.taste.engine.light.VoxelGiPass;
import dev.taste.engine.material.MaterialGeneration;
import dev.taste.engine.material.MaterialResolver;
import dev.taste.engine.material.MaterialRuleLoader;
import dev.taste.engine.render.backend.TerrainBackend;
import dev.taste.engine.render.embeddium.Embeddium031Backend;
import dev.taste.engine.render.forward.EntityForwardLighting;
import dev.taste.engine.render.forward.ActorMotionPass;
import dev.taste.engine.render.forward.NativeEmissionCapture;
import dev.taste.engine.render.reflection.ReflectionCapture;
import dev.taste.engine.render.vanilla.VanillaTerrainBackend;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Owns the world-only render route and the two-stage portable presentation pipeline. */
public final class TasteRenderer implements AutoCloseable, RenderHooks.Listener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TasteRenderer INSTANCE = new TasteRenderer();
    private final EnvironmentResolver environments = new EnvironmentResolver();
    private final DimensionProfiles dimensionProfiles = new DimensionProfiles();
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
    private final MaterialSurfacePass materialSurfaces = new MaterialSurfacePass();
    private final MaterialCompositePass materialComposite = new MaterialCompositePass();
    private final VoxelSectionCache meshSections = new VoxelSectionCache(96L * 1024 * 1024);
    private final VoxelGiPass voxelLighting = new VoxelGiPass(meshSections);
    private final TerrainMaterialPass terrainMaterials = new TerrainMaterialPass(meshSections);
    private final CloudRenderer clouds = new CloudRenderer();
    private final TemporalPostPass temporal = new TemporalPostPass();
    private final CameraPostPass cameraPost = new CameraPostPass();
    private final GuiColorPass guiColor = new GuiColorPass();
    private final DebugViewPass debugView = new DebugViewPass();
    private final ReflectionCapture reflectionCapture = new ReflectionCapture();
    private final PhotoCapture photo = PhotoCapture.instance();
    private final PortalScenePass portals = new PortalScenePass();
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
    private boolean voxelGi;
    private LocalLightingPass.TileData lightTiles;
    private ShadowRenderer.PointMaps pointMaps;
    private TextureTarget sceneTarget;
    private TextureTarget surfaceTarget;
    private TerrainMaterialPass.Geometry terrainGeometry;
    private TextureTarget terrainColor;
    private ReflectionCapture.Result offscreenReflections;
    private TextureTarget resolvedColor;
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
    private ConfigSnapshot renderingConfig;
    private EnvironmentSnapshot environment;
    private DimensionProfiles.Atmosphere atmosphere;
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
    private long lastGpuSampleFrame = -1;
    private String debugUnavailable = "";
    private boolean graphCaptured;
    private String failure = "";
    private boolean loggedFailure;
    private boolean worldOpen;
    private boolean materialRulesLoaded;
    private PipelineParameters handParameters;
    private HandRenderer.Lighting handLighting;
    private HandRenderer.Reflection handReflection;
    private boolean cloudPrepared;
    private boolean replaceNativeClouds;
    private boolean graphicsPresented;
    private boolean guiCompositeFrame;
    private final FabulousLayerRouter fabulous = new FabulousLayerRouter();
    private final OwnedParticlePass ownedParticles = new OwnedParticlePass();
    private final LensWetness lensWetness = new LensWetness();
    private float lensFilm;
    private List<LightningEvents.Flash> lightning = List.of();
    private float lightningFlash;
    private ShaderInstance nativeEmissionShader;
    private TextureTarget actorMotion;
    private boolean directionalMapsPrepared;
    private List<TextureTarget> glassTransmission = List.of();
    private ConfigSnapshot guiCandidate;
    private String aaMode = "off";
    private Matrix4f nativeProjectionArgument;
    private Matrix4f projectionBeforeJitter;
    private VertexSorting sortingBeforeJitter;

    private TasteRenderer() { RenderHooks.setListener(this); }

    public static TasteRenderer instance() { return INSTANCE; }
    public synchronized boolean baselineAvailable() { return probe != null && probe.baseline(); }
    public synchronized Optional<CapabilityProbe.Result> capabilityResult() { return Optional.ofNullable(probe); }
    public synchronized String failure() { return failure; }
    public synchronized long allocatedBytes() {
        return (targets == null ? 0L : targets.allocatedBytes()) + materialSurfaces.allocatedBytes()
                + terrainMaterials.allocatedBytes() + voxelLighting.allocatedBytes();
    }
    public synchronized double lastGpuMilliseconds() { return lastGpuMilliseconds; }
    public synchronized boolean graphicsActive() { return graphicsPresented; }
    public synchronized String overlayText() {
        if (!graphicsPresented || handParameters == null || Minecraft.getInstance().level == null) return "";
        if (!debugUnavailable.isBlank()) return "Taste: " + debugUnavailable;
        if (!handParameters.value("performance.timerOverlay").getAsBoolean()) return "";
        return lastGpuSampleFrame < 0 ? "Taste GPU: waiting for a measurement"
                : String.format(Locale.ROOT, "Taste GPU: %.2f ms | %d frames ago", lastGpuMilliseconds,
                        Math.max(0, frame - lastGpuSampleFrame));
    }
    public synchronized void dirtyLocalLight(ClientLevel level, BlockPos position) {
        if (Minecraft.getInstance().level == level) {
            localLighting.dirty(position);
            shadows.invalidatePointMaps();
            materialSurfaces.dirty(position);
            voxelLighting.dirty(position);
            terrainMaterials.dirty(position);
            reflectionCapture.dirty(position);
        }
    }

    @Override
    public synchronized void beginWorld(Camera camera, Matrix4f cameraView, Matrix4f projection, float nextPartialTick) {
        RenderSystem.assertOnRenderThread();
        if (worldOpen) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        ConfigSnapshot requested = ClientState.store().renderSnapshot();
        renderingConfig = requested;
        try {
            if (probe == null) probe = CapabilityProbe.probe();
            EffectiveConfig effective = EffectiveConfig.resolve(requested, ClientState.capabilities());
            if (!requested.requestedEnabled() || !effective.active() || hasBlockingConflict()) {
                releaseGpu();
                if (!requested.requestedEnabled()) ClientState.store().markPreviewVisible(requested);
                else ClientState.store().rejectPreview(requested, effective.reason().isBlank()
                        ? "An incompatible mod owns world rendering" : effective.reason());
                return;
            }
            ensureReady(minecraft, requested);
            renderingConfig = requested;
            if (!probe.baseline()) throw new IllegalStateException(probe.reason());
            partialTick = nextPartialTick;
            parameters = PipelineParameters.resolve(requested, registry, consumers);
            environment = environments.resolve(minecraft, camera, nextPartialTick, value("general.visualTime"), photo);
            atmosphere = dimensionProfiles.resolve(minecraft.level, camera.getPosition(), parameters);
            lensFilm = lensWetness.update(minecraft.level, camera.getPosition(), environment, enabled("weather.wetLens"),
                    photo.freezesDecoration(), value("weather.lensDrySeconds"));
            lightning = LightningEvents.instance().flashes(minecraft.level, nextPartialTick, parameters);
            float nextFlash = Math.min(1F, minecraft.level.getSkyFlashTime() * 0.5F)
                    * LightningEvents.effectiveStrength(parameters);
            if (nextFlash > lightningFlash + 0.08F || lightningFlash <= 0F && nextFlash > 0F) {
                frames.invalidate(TemporalHistory.Reason.LIGHTNING);
                indirect.invalidate();
                voxelLighting.invalidate();
            }
            lightningFlash = nextFlash;
            String giMode = effective.values().get("gi.mode").getAsString();
            screenGi = giMode.equals("screen");
            voxelGi = giMode.equals("voxel");
            aaMode = effective.values().get("antialiasing.mode").getAsString();
            nativeTarget = minecraft.getMainRenderTarget();
            nativeWidth = nativeTarget.width;
            nativeHeight = nativeTarget.height;
            float renderScale = Math.max(0.5F, Math.min(2.0F,
                    value("general.renderScale") * adaptiveQuality.scale()));
            renderScale = photo.scale(renderScale, nativeWidth, nativeHeight);
            width = Math.max(1, Math.round(nativeWidth * renderScale));
            height = Math.max(1, Math.round(nativeHeight * renderScale));
            replaceNativeClouds = atmosphere.allowOwnedSky();
            cloudPrepared = replaceNativeClouds && CloudRenderer.enabled(parameters, environment);
            if (cloudPrepared) clouds.prepare();
            int nextIdentity = System.identityHashCode(minecraft.level);
            if (nextIdentity != levelIdentity) {
                levelIdentity = nextIdentity;
                worldEpoch++;
                hand.invalidateHistory();
            }
            Matrix4f actualProjection = photo.collecting() || aaMode.equals("taa")
                    ? TemporalPostPass.jitteredProjection(projection, width, height,
                            photo.collecting() ? photo.sequenceLength() : parameters.value("antialiasing.jitterLength").getAsInt(),
                            photo.collecting() ? photo.jitterIndex() : frame)
                    : projection;
            Optional<FrameSnapshot> nextFrame = snapshot(camera, cameraView, actualProjection, projection);
            if (nextFrame.isEmpty()) {
                frames.invalidate(TemporalHistory.Reason.PROJECTION_CHANGE);
                failure = "World projection is not ready; native rendering remains active";
                photo.abort();
                ClientState.store().rejectPreview(requested, failure);
                graphicsPresented = false;
                handParameters = null;
                handLighting = null;
                handReflection = null;
                guiCompositeFrame = false;
                guiCandidate = null;
                parameters = null;
                environment = null;
                nativeTarget = null;
                return;
            }
            WorldViewScope.ViewKey key = new WorldViewScope.ViewKey(environment.dimension(), 0);
            activeFrame = frames.begin(nextFrame.get(), key, false);
            prepareDirectionalLight();
            selectBackend(effective);
            backend.beginFrame(frame);
            worldTarget = targets.acquire(descriptor("world", width, height, true));
            sceneTarget = targets.acquire(descriptor("scene", width, height, true));
            fabulous.route(minecraft.levelRenderer, targets, width, height);
            allocations.record("world", new AllocationLedger.Allocation("world", descriptor("world", width, height, true).byteSize(), frame, false));
            allocations.record("scene", new AllocationLedger.Allocation("scene", descriptor("scene", width, height, true).byteSize(), frame, false));
            worldTarget.clear(Minecraft.ON_OSX);
            worldTarget.bindWrite(true);
            targetRoute = WorldTargetRouter.route(worldTarget);
            worldScope = WorldViewScope.enter(key);
            if (actualProjection != projection) {
                nativeProjectionArgument = projection;
                projectionBeforeJitter = new Matrix4f(projection);
                sortingBeforeJitter = RenderSystem.getVertexSorting();
                projection.set(actualProjection);
                RenderSystem.setProjectionMatrix(actualProjection, sortingBeforeJitter);
            }
            worldOpen = true;
            VegetationMotion.beginFrame(activeFrame.snapshot(), parameters, environment);
            NativeEmissionCapture.begin(targets, worldTarget, parameters);
            if (Minecraft.useShaderTransparency()) {
                NativeEmissionCapture.admit(minecraft.levelRenderer.getItemEntityTarget());
                NativeEmissionCapture.admit(minecraft.levelRenderer.getTranslucentTarget());
                NativeEmissionCapture.admit(minecraft.levelRenderer.getWeatherTarget());
            }
            ownedParticles.begin(activeFrame.snapshot(), parameters, targets, probe.flags().contains("rgba16f-mrt"));
            float particleFog = environment.underwater()
                    ? (enabled("underwater.fog") ? value("underwater.fogDensity") : 0)
                    : (enabled("fog.enabled") ? atmosphere.fogDensity() : 0);
            ownedParticles.atmosphere(particleFog, atmosphere.fogColor());
            if (actorMotionRequested()) ActorMotionPass.begin(activeFrame.snapshot(), nextPartialTick);
            EntityForwardLighting.begin(activeFrame.snapshot(), parameters, environment, materialResolver, towardsSun,
                    linearColor(daylight ? "lighting.sunColor" : "lighting.moonColor"), directionalIntensity);
            if (!photo.freezesDecoration()) dimensionProfiles.decorate(minecraft.level, camera.getPosition(), parameters);
        } catch (IOException | RuntimeException renderFailure) {
            fail(renderFailure);
            closeWorldScope();
        }
    }

    @Override
    public synchronized void endWorld() {
        if (!worldOpen) return;
        RenderSystem.assertOnRenderThread();
        EntityForwardLighting.end();
        ActorMotionPass.end();
        NativeEmissionCapture.end();
        closeRouteOnly();
        try {
            VegetationMotion.drawFrom(activeFrame.snapshot().origin());
            if (timer != null) timer.begin();
            executeGraph();
            if (timer != null) timer.pollSample().ifPresent(sample -> {
                lastGpuMilliseconds = sample.milliseconds();
                lastGpuSampleFrame = sample.frame();
            });
            frames.complete(activeFrame);
            graphicsPresented = true;
            handParameters = parameters;
            handLighting = handLighting(activeFrame.snapshot());
            handReflection = handReflection(activeFrame.snapshot());
            if (guiCompositeFrame) guiCandidate = renderingConfig;
            else ClientState.store().markPreviewVisible(renderingConfig);
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

    public synchronized boolean suppressNativeClouds() {
        return worldOpen && replaceNativeClouds;
    }

    public synchronized boolean suppressNativePrecipitation() {
        return worldOpen && atmosphere != null && atmosphere.allowOwnedSky() && ClientEvents.ownsPrecipitation();
    }

    public synchronized boolean composeFabulous(PostChain chain) {
        return worldOpen && fabulous.compose(chain, worldTarget, TasteRenderer::drawFullscreen);
    }

    @Override public synchronized void beforeActors() {
        if (!worldOpen || directionalMapsPrepared || !enabled("entities.enhancedLighting")
                || !enabled("entities.shadowsOnSelf") || !enabled("shadows.enabled")) return;
        try (GlStateScope ignored = GlStateScope.capture()) {
            VegetationMotion.drawFrom(activeFrame.snapshot().origin());
            drawShadowMaps();
            EntityForwardLighting.directionalShadows(shadowMaps, activeFrame.snapshot(), parameters);
        } catch (IOException | RuntimeException renderFailure) {
            directionalMapsPrepared = false;
            shadowMaps = List.of();
            EntityForwardLighting.end();
            LOGGER.debug("Could not prepare shadow maps before native actor rendering", renderFailure);
        } finally {
            VegetationMotion.drawFromCamera();
        }
    }

    public synchronized void applyGuiColor() {
        if (Minecraft.getInstance().level == null) {
            if (targets != null) releaseGpu();
            return;
        }
        if (!graphicsPresented || !guiCompositeFrame || handParameters == null || targets == null) return;
        try (GlStateScope ignored = GlStateScope.capture()) {
            clearGlErrors();
            guiColor.render(Minecraft.getInstance().getMainRenderTarget(), targets, handParameters, TasteRenderer::drawFullscreen);
            requireNoGlError("composed GUI color");
            ClientState.store().markPreviewVisible(guiCandidate);
        } catch (IOException | RuntimeException error) {
            String cause = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            ClientState.store().rejectPreview(guiCandidate, cause);
            LOGGER.error("Could not apply Taste color to the composed GUI", error);
        } finally {
            guiCompositeFrame = false;
            guiCandidate = null;
        }
    }
    @Override public synchronized void endLayer(RenderType type) {
        if (worldOpen && backend != null) backend.endLayer(type.toString());
    }
    @Override public synchronized void beginHand() {
        if (!worldOpen && !hand.capturing() && targets != null && handParameters != null)
            hand.begin(targets, frame, handParameters, RenderSystem.getProjectionMatrix(), handLighting, handReflection);
    }
    @Override public synchronized void endHand() {
        if (hand.capturing()) hand.end(frame, TasteRenderer::drawFullscreen);
    }

    private HandRenderer.Lighting handLighting(FrameSnapshot state) {
        if (directionalIntensity <= 0 || towardsSun == null) return null;
        Matrix4f view = new Matrix4f().set(state.view().columnMajor());
        return new HandRenderer.Lighting(view.transformDirection(new Vector3f(towardsSun)),
                linearColor(daylight ? "lighting.sunColor" : "lighting.moonColor"), directionalIntensity);
    }

    private HandRenderer.Reflection handReflection(FrameSnapshot state) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !environment.hasSky()) return null;
        FrameSnapshot.Vec3d eye = state.cameraWorld();
        Vec3 sky = level.getSkyColor(new Vec3(eye.x(), eye.y(), eye.z()), partialTick);
        Vector3f up = new Matrix4f().set(state.view().columnMajor()).transformDirection(new Vector3f(0, 1, 0));
        float visibility = level.getBrightness(LightLayer.SKY,
                BlockPos.containing(eye.x(), eye.y(), eye.z())) / 15F;
        Vector3f linearSky = new Vector3f(linearChannel(Math.round((float) sky.x * 255)),
                linearChannel(Math.round((float) sky.y * 255)), linearChannel(Math.round((float) sky.z * 255)));
        return new HandRenderer.Reflection(materialResolver, up, linearSky, atmosphere.fogColor(), visibility);
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
        long budget = Math.max(64L, config.value("performance.memoryBudgetMiB").getAsLong()) * 1024L * 1024L;
        if (targets != null && targets.limitBytes() != budget) releaseGpu();
        if (!materialRulesLoaded) {
            materialRules.load(minecraft.getResourceManager(), materialResolver);
            dimensionProfiles.load(minecraft.getResourceManager());
            materialRulesLoaded = true;
            materialGeneration.advanceRender();
        }
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
        RenderGraph.Resource transmission = new RenderGraph.Resource("glass-transmission", "glass-shadow-transmission");
        RenderGraph.Resource local = new RenderGraph.Resource("local-light-tiles", "local-light-data");
        RenderGraph.Resource pointShadows = new RenderGraph.Resource("point-visibility", "point-shadow-atlas");
        RenderGraph.Resource volume = new RenderGraph.Resource("atmosphere", "atmosphere-target");
        RenderGraph.Resource scene = new RenderGraph.Resource("scene", "scene-target");
        RenderGraph.Resource surfaces = new RenderGraph.Resource("material-surfaces", "surface-target");
        RenderGraph.Resource terrainData = new RenderGraph.Resource("terrain-materials", "terrain-gbuffer");
        RenderGraph.Resource terrainShading = new RenderGraph.Resource("terrain-shaded", "terrain-lighting-target");
        RenderGraph.Resource reflections = new RenderGraph.Resource("reflection-captures", "reflection-resident-targets");
        RenderGraph.Resource media = new RenderGraph.Resource("material-composite", "media-target");
        RenderGraph.Resource portal = new RenderGraph.Resource("portal-optics", "portal-target");
        RenderGraph.Resource emissive = new RenderGraph.Resource("native-emission", "emission-scene");
        RenderGraph.Resource cloud = new RenderGraph.Resource("clouds", "cloud-target");
        RenderGraph.Resource particles = new RenderGraph.Resource("owned-particles", "particle-scene");
        RenderGraph.Resource motion = new RenderGraph.Resource("actor-motion", "actor-motion-target");
        RenderGraph.Resource resolved = new RenderGraph.Resource("temporal", "resolved-history");
        RenderGraph.Resource bloom = new RenderGraph.Resource("bloom", "bloom-pyramid");
        RenderGraph.Resource exposure = new RenderGraph.Resource("exposure", "exposure-history");
        RenderGraph.Resource output = new RenderGraph.Resource("output", "native-target");
        RenderGraph.Compiled graph = RenderGraph.compile(Set.of(world, opaque), Set.of(output), List.of(
                new RenderGraph.Pass("sun-visibility", Set.of(), Set.of(visibility), this::drawShadowMaps),
                new RenderGraph.Pass("glass-transmission", Set.of(visibility), Set.of(transmission), this::drawGlassTransmission),
                new RenderGraph.Pass("local-light-tiles", Set.of(), Set.of(local), this::prepareLocalLighting),
                new RenderGraph.Pass("point-visibility", Set.of(local), Set.of(pointShadows), this::drawPointShadows),
                new RenderGraph.Pass("atmosphere", Set.of(world, opaque, visibility, transmission, local, pointShadows), Set.of(volume), this::drawVolume),
                new RenderGraph.Pass("indirect", Set.of(opaque, visibility, local), Set.of(diffuse), this::drawIndirect),
                new RenderGraph.Pass("scene-lighting", Set.of(world, opaque, visibility, transmission, volume, diffuse, local, pointShadows), Set.of(scene), this::drawScene),
                new RenderGraph.Pass("terrain-materials", Set.of(opaque), Set.of(terrainData), this::drawTerrainMaterials),
                new RenderGraph.Pass("terrain-shaded", Set.of(world, scene, opaque, terrainData), Set.of(terrainShading), this::shadeTerrain),
                new RenderGraph.Pass("material-surfaces", Set.of(), Set.of(surfaces), this::drawMaterialSurfaces),
                new RenderGraph.Pass("reflection-captures", Set.of(), Set.of(reflections), this::drawReflectionCaptures),
                new RenderGraph.Pass("material-composite", Set.of(world, terrainShading, surfaces, opaque, reflections), Set.of(media), this::drawMaterialComposite),
                new RenderGraph.Pass("portal-optics", Set.of(media, opaque), Set.of(portal), this::drawPortals),
                new RenderGraph.Pass("native-emission", Set.of(portal, world, volume), Set.of(emissive), this::drawNativeEmission),
                new RenderGraph.Pass("clouds", Set.of(emissive, world), Set.of(cloud), this::drawClouds),
                new RenderGraph.Pass("owned-particles", Set.of(cloud, opaque, world, local), Set.of(particles), this::drawOwnedParticles),
                new RenderGraph.Pass("actor-motion", Set.of(world, opaque), Set.of(motion), this::drawActorMotion),
                new RenderGraph.Pass("temporal", Set.of(particles, world, opaque, terrainData, emissive, motion), Set.of(resolved), this::drawTemporal),
                new RenderGraph.Pass("bloom", Set.of(world, resolved, terrainData, surfaces, emissive, volume, particles), Set.of(bloom), this::drawBloom),
                new RenderGraph.Pass("exposure", Set.of(resolved), Set.of(exposure), this::drawExposure),
                new RenderGraph.Pass("final-color", Set.of(world, opaque, resolved, bloom, exposure, motion), Set.of(output), this::drawFinal)));
        graph.execute(ignored -> {
            targets.accountExternal(materialSurfaces.allocatedBytes() + terrainMaterials.allocatedBytes()
                    + voxelLighting.allocatedBytes());
            return GlStateScope.capture();
        });
        captureFrameGraph(graph);
    }

    private void captureFrameGraph(RenderGraph.Compiled graph) {
        if (!enabled("performance.captureFrameGraph")) {
            graphCaptured = false;
            return;
        }
        if (graphCaptured || renderingConfig != ClientState.store().current()) return;
        graphCaptured = true;
        List<Map<String, Object>> passes = graph.passes().stream().<Map<String, Object>>map(pass -> Map.of(
                "name", pass.name(), "reads", pass.reads().stream().map(RenderGraph.Resource::name).sorted().toList(),
                "writes", pass.writes().stream().map(RenderGraph.Resource::name).sorted().toList())).toList();
        Path file = FMLPaths.GAMEDIR.get().resolve("logs").resolve("taste-frame-graph-" + System.currentTimeMillis() + ".json");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                    "passes", passes, "attachments", frames.debug().snapshot(), "memory", allocatedBytes())));
        } catch (IOException error) {
            LOGGER.warn("Could not write Taste frame graph diagnostics", error);
        }
    }

    private void prepareDirectionalLight() {
        float angle = environment.sunAngle();
        towardsSun = new Vector3f(-(float) Math.sin(angle), (float) Math.cos(angle), 0);
        daylight = towardsSun.y >= 0;
        if (!daylight) towardsSun.negate();
        directionalIntensity = !environment.hasSky() ? 0
                : daylight ? (enabled("lighting.sunEnabled") ? value("lighting.sunIntensity") : 0)
                            : value("lighting.moonIntensity");
        directionalIntensity *= Math.min(1, towardsSun.y / 0.08F);
    }

    private void drawShadowMaps() throws IOException {
        if (directionalMapsPrepared) return;
        shadowMaps = List.of();
        shadows.prepareActors(activeFrame.snapshot(), partialTick,
                enabled("shadows.enabled") && enabled("shadows.entities"),
                enabled("shadows.enabled") && enabled("shadows.blockEntities"));
        boolean needShafts = volumeEnabled() && (environment.underwater()
                ? enabled("underwater.shafts") && value("underwater.shaftStrength") > 0 : value("fog.shaftsStrength") > 0);
        if (!(enabled("shadows.enabled") || needShafts) || directionalIntensity <= 0) {
            shadows.releaseFrom(targets, 0);
            directionalMapsPrepared = true;
            return;
        }
        clearGlErrors();
        int resolution = Integer.parseInt(parameters.value("shadows.resolution").getAsString());
        shadowMaps = shadows.draw(backend, targets, activeFrame.snapshot(), towardsSun, resolution,
                parameters.value("shadows.cascades").getAsInt(), value("shadows.distance"), value("shadows.splitLambda"),
                enabled("shadows.cacheStatic"), parameters.value("shadows.dynamicEveryFrames").getAsInt());
        requireNoGlError("sun-visibility");
        for (int index = 0; index < shadowMaps.size(); index++) {
            ShadowRenderer.MapLayer layer = shadowMaps.get(index);
            frames.debug().publish("sun-shadow-" + index, new DebugAttachments.Attachment(
                    layer.texture(), layer.resolution(), layer.resolution(),
                    "Directional terrain depth, " + layer.submissions() + " caster submissions"));
        }
        directionalMapsPrepared = true;
    }

    private void bindShadowMaps() {
        sceneShader.getUniform("GlassTransmission").set(12);
        sceneShader.getUniform("GlassTransmissionSecond").set(13);
        set(sceneShader, "GlassTransmissionEnabled", glassTransmission.size());
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
        set(sceneShader, "VolumeDensity", environment.underwater() ? value("underwater.fogDensity") : atmosphere.fogDensity());
        set(sceneShader, "CaveDensity", caveDensity());
        set(sceneShader, "VolumeDistance", value("fog.maxDistance"));
        set(sceneShader, "VolumeAnisotropy", value("fog.anisotropy"));
        set(sceneShader, "VolumeStrength", environment.underwater()
                ? (enabled("underwater.shafts") ? value("underwater.shaftStrength") : 0) : value("fog.shaftsStrength"));
        set(sceneShader, "VolumeWater", environment.underwater() ? 1 : 0);
        set(sceneShader, "VolumeHeightBase", value("fog.heightBase") - (float) activeFrame.snapshot().origin().y());
        set(sceneShader, "VolumeHeightFalloff", value("fog.heightFalloff"));
        sceneShader.getUniform("TowardsSun").set(towardsSun);
        sceneShader.getUniform("SunColor").set(linearColor(daylight ? "lighting.sunColor" : "lighting.moonColor"));
        sceneShader.getUniform("FogColor").set(atmosphere.fogColor());
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

    private void drawGlassTransmission() throws IOException {
        if (!enabled("shadows.enabled") || !enabled("shadows.coloredGlass")) {
            targets.release(MaterialSurfacePass.GLASS_SHADOW_TARGET);
            targets.release(MaterialSurfacePass.GLASS_SHADOW_SECOND_TARGET);
            glassTransmission = List.of();
            return;
        }
        clearGlErrors();
        glassTransmission = materialSurfaces.drawGlassTransmission(targets, activeFrame.snapshot(), materialResolver, shadowMaps);
        requireNoGlError("glass transmission");
        for (int index = 0; index < glassTransmission.size(); index++) {
            TextureTarget layer = glassTransmission.get(index);
            frames.debug().publish("glass-transmission-" + index, new DebugAttachments.Attachment(
                    layer.getColorTextureId(), layer.width, layer.height, "Color transmittance and entry depth per cascade"));
        }
    }

    private void drawSceneFullscreen() {
        // The native ShaderInstance cache owns units 0–11.
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] previous = new int[2];
        for (int index = 0; index < previous.length; index++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 12 + index);
            previous[index] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderTarget source = index < glassTransmission.size() ? glassTransmission.get(index) : worldTarget;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, source.getColorTextureId());
        }
        GL13.glActiveTexture(active);
        try {
            drawFullscreen();
        } finally {
            for (int index = 0; index < previous.length; index++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + 12 + index);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous[index]);
            }
            GL13.glActiveTexture(active);
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
        if (environment.underwater())
            return enabled("underwater.shafts") && value("underwater.shaftStrength") > 0 && value("underwater.fogDensity") > 0;
        return enabled("fog.enabled") && enabled("fog.volumetric") && (atmosphere.fogDensity() > 0 || caveDensity() > 0);
    }

    private float caveDensity() {
        ClientLevel level = Minecraft.getInstance().level;
        if (!enabled("fog.caveMist") || environment.underwater() || !environment.hasSky() || level == null) return 0;
        FrameSnapshot.Vec3d eye = activeFrame.snapshot().cameraWorld();
        BlockPos center = BlockPos.containing(eye.x(), eye.y(), eye.z());
        if (!level.hasChunkAt(center)) return 0;
        int sheltered = 0;
        for (int z = -1; z <= 1; z++) for (int x = -1; x <= 1; x++) {
            BlockPos sample = center.offset(x * 3, 0, z * 3);
            if (level.hasChunkAt(sample) && !level.canSeeSky(sample)) sheltered++;
        }
        return value("fog.caveDensity") * sheltered / 9F;
    }

    private void prepareLocalLighting() {
        clearGlErrors();
        FrameSnapshot snapshot = activeFrame.snapshot();
        List<Light> edgeLights = volumeEnabled() ? portals.edgeLights(Minecraft.getInstance().level, snapshot.cameraWorld(), parameters) : List.of();
        List<Light> flashes = lightning.stream().sorted(java.util.Comparator.comparingDouble(LightningEvents.Flash::strength).reversed())
                .limit(4).map(strike -> new Light(strike.x(), strike.y() + 1.6, strike.z(),
                        0.72F, 0.82F, 1F, strike.strength() * 7F, 28F, true)).toList();
        lightTiles = localLighting.prepare(targets, snapshot, parameters, materialResolver, edgeLights, flashes);
        requireNoGlError("local light upload");
    }

    private void drawPointShadows() throws IOException {
        clearGlErrors();
        int resolution = Integer.parseInt(parameters.value("shadows.localResolution").getAsString());
        int count = enabled("shadows.enabled") && enabled("shadows.localLights")
                ? parameters.value("shadows.maxLocalLights").getAsInt() : 0;
        if (lightTiles != null) count = Math.max(count, lightTiles.portalCount());
        int cadence = parameters.value("shadows.dynamicEveryFrames").getAsInt();
        int refresh = enabled("shadows.cacheStatic") && !shadows.hasMovingCasters() ? Math.max(cadence, 20) : cadence;
        if (VegetationMotion.affectsCasters()) refresh = 1;
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
        set(sceneShader, "PortalLightCount", lightTiles == null ? 0 : lightTiles.portalCount());
        set(sceneShader, "EventLightCount", lightTiles == null ? 0 : lightTiles.eventCount());
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
        if (voxelGi) {
            indirectTarget = voxelLighting.render(targets, opaqueTarget, indirectTarget, activeFrame.snapshot(),
                    parameters, Minecraft.getInstance().level, materialResolver, towardsSun,
                    linearColor(daylight ? "lighting.sunColor" : "lighting.moonColor"), directionalIntensity,
                    lightTiles == null ? List.of() : lightTiles.lights());
        } else if (voxelLighting.allocatedBytes() > 0) {
            voxelLighting.releaseTargets(targets);
        }
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
            drawSceneFullscreen();
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
        set(sceneShader, "LightWrap", value("style.lightWrap"));
        set(sceneShader, "ShadingStyle", switch (parameters.value("style.shading").getAsString()) {
            case "soft" -> 1;
            case "toon" -> 2;
            default -> 0;
        });
        set(sceneShader, "ToonBands", value("style.toonBands"));
        set(sceneShader, "ReducedMotion", enabled("accessibility.reducedMotion") ? 1.0F : 0.0F);
        try {
            drawSceneFullscreen();
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
        int skyMode = atmosphere.endStars() ? 3 : !atmosphere.allowOwnedSky() ? 0 : switch (mode) {
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

    private void drawMaterialSurfaces() throws IOException {
        clearGlErrors();
        surfaceTarget = materialSurfaces.draw(targets, activeFrame.snapshot(), parameters, materialResolver);
        requireNoGlError("material surface metadata");
    }

    private void drawTerrainMaterials() throws IOException {
        clearGlErrors();
        terrainGeometry = terrainMaterials.capture(targets, opaqueTarget, activeFrame.snapshot(), parameters,
                materialResolver, backend instanceof Embeddium031Backend);
        requireNoGlError("terrain material capture");
    }

    private void shadeTerrain() throws IOException {
        clearGlErrors();
        terrainColor = terrainMaterials.shade(targets, terrainGeometry, sceneTarget, opaqueTarget, worldTarget, activeFrame.snapshot(),
                parameters, environment, towardsSun, linearColor(daylight ? "lighting.sunColor" : "lighting.moonColor"),
                TasteRenderer::drawFullscreen);
        requireNoGlError("terrain material lighting");
    }

    private void drawMaterialComposite() throws IOException {
        clearGlErrors();
        resolvedColor = materialComposite.render(targets, terrainColor, opaqueTarget, worldTarget, surfaceTarget,
                materialSurfaces.surfaceOptics(), activeFrame.snapshot(), parameters, environment, offscreenReflections,
                TasteRenderer::drawFullscreen);
        requireNoGlError("material composition");
    }

    private void drawReflectionCaptures() throws IOException {
        clearGlErrors();
        offscreenReflections = reflectionCapture.capture(targets, backend, Minecraft.getInstance().level,
                activeFrame.snapshot(), parameters, environment.underwater() || environment.lava());
        requireNoGlError("resident reflection captures");
    }

    private void drawPortals() throws IOException {
        clearGlErrors();
        resolvedColor = portals.draw(targets, activeFrame.snapshot(), parameters, resolvedColor, opaqueTarget);
        requireNoGlError("portal composition");
    }

    private void drawClouds() throws IOException {
        if (!cloudPrepared) {
            clouds.releaseTargets(targets);
            clouds.invalidate();
            return;
        }
        clearGlErrors();
        TextureTarget output = clouds.render(targets, resolvedColor, worldTarget, activeFrame.snapshot(),
                parameters, environment, towardsSun, linearColor(daylight ? "lighting.sunColor" : "lighting.moonColor"),
                directionalIntensity, daylight, TasteRenderer::drawFullscreen);
        requireNoGlError("cloud composition");
        if (output != null) resolvedColor = output;
    }

    private void drawTemporal() throws IOException {
        clearGlErrors();
        resolvedColor = temporal.resolve(resolvedColor, worldTarget, opaqueTarget, targets, activeFrame,
                parameters, photo.collecting() ? "off" : aaMode,
                EntityForwardLighting.hasReactiveOverlay(), terrainGeometry, actorMotion, NativeEmissionCapture.target(),
                ownedParticles.coverageTexture(), ownedParticles.inverseCoverage(), TasteRenderer::drawFullscreen);
        resolvedColor = photo.accumulate(resolvedColor, worldTarget, targets, activeFrame, TasteRenderer::drawFullscreen);
        requireNoGlError("temporal resolve");
    }

    private boolean actorMotionRequested() {
        return !photo.collecting() && (aaMode.equals("taa")
                || enabled("camera.motionBlur") && !enabled("accessibility.reducedMotion")
                || parameters.value("performance.debugView").getAsString().equals("velocity"));
    }

    private void drawActorMotion() throws IOException {
        if (!actorMotionRequested()) {
            ActorMotionPass.releaseTargets(targets);
            return;
        }
        actorMotion = ActorMotionPass.render(targets, worldTarget, opaqueTarget, activeFrame.snapshot());
        if (actorMotion != null) frames.debug().publish("actor-motion", new DebugAttachments.Attachment(
                actorMotion.getColorTextureId(), actorMotion.width, actorMotion.height,
                "Actor motion, previous depth and pose correspondence"));
    }

    private void drawOwnedParticles() {
        resolvedColor = ownedParticles.render(targets, resolvedColor, opaqueTarget == null ? worldTarget : opaqueTarget,
                lightTiles, TasteRenderer::drawFullscreen);
        TextureTarget emission = ownedParticles.emissionTexture();
        if (emission != null) frames.debug().publish("particle-emission", new DebugAttachments.Attachment(
                emission.getColorTextureId(), emission.width, emission.height, "Owned particle emission"));
    }

    private void drawNativeEmission() throws IOException {
        TextureTarget source = NativeEmissionCapture.target();
        if (source == null) {
            targets.release("native-emission-scene");
            return;
        }
        if (nativeEmissionShader == null) nativeEmissionShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                new ResourceLocation("taste", "taste_native_emission_composite"), DefaultVertexFormat.POSITION_TEX);
        TextureTarget output = targets.acquire(descriptor("native-emission-scene", width, height, false));
        clearGlErrors();
        output.bindWrite(true);
        bindCommon(nativeEmissionShader, resolvedColor);
        nativeEmissionShader.setSampler("NativeEmission", source.getColorTextureId());
        nativeEmissionShader.setSampler("Atmosphere", volumeTarget == null ? source.getColorTextureId() : volumeTarget.getColorTextureId());
        set(nativeEmissionShader, "HasAtmosphere", volumeTarget == null ? 0 : 1);
        try {
            drawFullscreen();
            requireNoGlError("native emission");
        } finally {
            nativeEmissionShader.clear();
        }
        frames.debug().publish("native-emission", new DebugAttachments.Attachment(source.getColorTextureId(),
                source.width, source.height, "Native eyes and glint radiance with receiver depth"));
        resolvedColor = output;
    }

    private Vector3f lensLight() {
        FrameSnapshot state = activeFrame.snapshot();
        Matrix4f view = new Matrix4f().set(state.view().columnMajor());
        Vector3f direction = view.transformDirection(new Vector3f(towardsSun));
        Vector4f clip = new Matrix4f().set(state.unjitteredProjection().columnMajor())
                .transform(new Vector4f(direction, 0));
        if (!daylight || clip.w <= 0.0001F || directionalIntensity <= 0) return new Vector3f(-1, -1, 0);
        float u = 0.5F + clip.x / clip.w * 0.5F;
        float v = 0.5F + clip.y / clip.w * 0.5F;
        float visible = Math.max(0, 1 - environment.rain()) * Math.min(1, directionalIntensity);
        return new Vector3f(u, v, visible);
    }

    private void drawFinal() throws IOException {
        clearGlErrors();
        if (!photo.collecting() && drawDebugView()) return;
        debugUnavailable = "";
        Vector3f lens = lensLight();
        cameraPost.prepare(worldTarget, opaqueTarget, cloudPrepared ? clouds.cloudTexture() : null, targets, activeFrame, parameters,
                lens.x, lens.y, lens.z, TasteRenderer::drawFullscreen);
        RenderTarget photoOutput = photo.output(nativeTarget, targets);
        String presentationAa = photo.collecting() ? "off" : aaMode;
        RenderTarget graded = cameraPost.finalTarget(photoOutput, targets, presentationAa);
        graded.bindWrite(true);
        GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
        bindCommon(finalShader, resolvedColor);
        set(finalShader, "Exposure", powerOfTwo(value("color.exposureEV")));
        set(finalShader, "AutoExposure", exposureTarget == null ? 0 : 1);
        finalShader.setSampler("ExposureSampler", exposureTarget == null ? resolvedColor.getColorTextureId() : exposureTarget.getColorTextureId());
        set(finalShader, "Saturation", value("color.saturation"));
        set(finalShader, "Contrast", value("color.contrast"));
        set(finalShader, "Temperature", value("color.temperature"));
        set(finalShader, "Tint", value("color.tint"));
        set(finalShader, "Gamma", value("color.gamma"));
        finalShader.getUniform("ShadowTint").set(linearColor("color.shadowTint"));
        finalShader.getUniform("HighlightTint").set(linearColor("color.highlightTint"));
        finalShader.setSampler("BloomSampler", bloomTarget == null ? resolvedColor.getColorTextureId() : bloomTarget.getColorTextureId());
        set(finalShader, "BloomStrength", bloomTarget == null ? 0 : value("bloom.strength"));
        set(finalShader, "FogStrength", enabled("fog.enabled") && volumeTarget == null ? atmosphere.fogDensity() : 0.0F);
        set(finalShader, "FogDistance", value("fog.maxDistance"));
        finalShader.getUniform("FogColor").set(atmosphere.fogColor());
        set(finalShader, "Vignette", value("camera.vignette"));
        set(finalShader, "Grain", value("camera.grain"));
        set(finalShader, "Sharpen", value("camera.sharpen"));
        set(finalShader, "DofEnabled", enabled("camera.dof") ? 1.0F : 0.0F);
        set(finalShader, "FocusDistance", value("camera.focusDistance"));
        set(finalShader, "Aperture", value("camera.aperture"));
        set(finalShader, "DofRadius", value("camera.dofMaxRadius"));
        set(finalShader, "Outline", enabled("style.outline") ? value("style.outlineStrength") : 0.0F);
        set(finalShader, "OutlineWidth", value("style.outlineWidth"));
        finalShader.getUniform("OutlineColor").set(linearColor("style.outlineColor"));
        set(finalShader, "PaletteStrength", value("style.paletteStrength"));
        set(finalShader, "WetLens", lensFilm);
        set(finalShader, "Underwater", environment.underwater() ? 1.0F : 0.0F);
        set(finalShader, "UnderwaterDensity", enabled("underwater.fog") ? value("underwater.fogDensity") : 0.0F);
        set(finalShader, "Thunder", lightningFlash);
        set(finalShader, "ReducedMotion", enabled("accessibility.reducedMotion") ? 1.0F : 0.0F);
        set(finalShader, "MinimumContrast", value("accessibility.minimumContrast"));
        set(finalShader, "ToneMapper", parameters.value("color.toneMapper").getAsString().equals("neutral") ? 1.0F : 0.0F);
        temporal.bindFinal(finalShader, activeFrame, worldTarget, terrainGeometry, actorMotion);
        cameraPost.bind(finalShader, parameters, resolvedColor, activeFrame, presentationAa);
        guiCompositeFrame = !photo.collecting() && GuiColorPass.enabled(parameters);
        if (guiCompositeFrame) {
            set(finalShader, "ColorVision", 0);
        } else {
            targets.release("composed-gui");
        }
        try {
            drawFullscreen();
            requireNoGlError("final-color");
        } finally {
            finalShader.clear();
        }
        cameraPost.present(graded, photoOutput, TasteRenderer::drawFullscreen);
        photo.present(photoOutput, nativeTarget, worldTarget, cameraPost.focusTexture(),
                activeFrame, parameters, TasteRenderer::drawFullscreen);
        requireNoGlError("camera presentation");
        frames.debug().publish("final", new DebugAttachments.Attachment(nativeTarget.getColorTextureId(), nativeWidth, nativeHeight, "Tone-mapped world before hand and GUI"));
        updateAdaptiveScale();
    }

    private boolean drawDebugView() throws IOException {
        DebugViewPass.View view = DebugViewPass.View.valueOf(
                parameters.value("performance.debugView").getAsString().toUpperCase(Locale.ROOT));
        if (view == DebugViewPass.View.NONE) return false;
        int data = switch (view) {
            case DEPTH -> worldTarget.getDepthTextureId();
            case NORMALS, MATERIALS -> terrainGeometry == null ? 0 : terrainGeometry.normalSurface().getColorTextureId();
            case ALBEDO -> terrainGeometry == null ? 0 : terrainGeometry.albedoAo().getColorTextureId();
            case EMISSION -> terrainGeometry == null ? 0 : terrainGeometry.emissionFlags().getColorTextureId();
            case VELOCITY -> terrainGeometry == null ? 0 : terrainGeometry.motionLight().getColorTextureId();
            case SHADOWS -> shadowMaps.isEmpty() ? 0 : shadowMaps.get(0).texture();
            case REACTIVE -> opaqueTarget == null ? 0 : worldTarget.getColorTextureId();
            case GI -> indirectTarget == null ? 0 : indirectTarget.getColorTextureId();
            case EXPOSURE -> exposureTarget == null ? 0 : exposureTarget.getColorTextureId();
            default -> 0;
        };
        int secondary = view == DebugViewPass.View.REACTIVE && opaqueTarget != null ? opaqueTarget.getColorTextureId() : 0;
        debugUnavailable = data == 0 ? view.name().toLowerCase(Locale.ROOT) + " attachment unavailable" : "";
        guiCompositeFrame = false;
        debugView.render(nativeTarget, view, data, secondary, worldTarget.getDepthTextureId(), activeFrame.snapshot(), TasteRenderer::drawFullscreen);
        requireNoGlError("diagnostic presentation");
        return true;
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
        TextureTarget input = resolvedColor;
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
        if (photo.lockExposure() && exposureTarget != null) return;
        if (lightningFlash > 0F && exposureTarget != null && enabled("color.autoExposure")) return;
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
        bindCommon(exposureShader, resolvedColor);
        boolean history = exposureTarget != null && exposureWorldEpoch == worldEpoch;
        exposureShader.setSampler("PreviousSampler", history ? exposureTarget.getColorTextureId() : resolvedColor.getColorTextureId());
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
        bloomShader.setSampler("TerrainEmission", terrainGeometry == null ? input.getColorTextureId()
                : terrainGeometry.emissionFlags().getColorTextureId());
        bloomShader.setSampler("TerrainDepth", terrainGeometry == null ? worldTarget.getDepthTextureId()
                : terrainGeometry.albedoAo().getDepthTextureId());
        bloomShader.setSampler("SurfaceData", surfaceTarget == null ? input.getColorTextureId() : surfaceTarget.getColorTextureId());
        bloomShader.setSampler("SurfaceDepth", surfaceTarget == null ? worldTarget.getDepthTextureId() : surfaceTarget.getDepthTextureId());
        TextureTarget nativeEmission = NativeEmissionCapture.target();
        TextureTarget particleEmission = ownedParticles.emissionTexture();
        bloomShader.setSampler("NativeEmission", nativeEmission == null ? input.getColorTextureId() : nativeEmission.getColorTextureId());
        bloomShader.setSampler("ParticleEmission", particleEmission == null ? input.getColorTextureId() : particleEmission.getColorTextureId());
        bloomShader.setSampler("Atmosphere", volumeTarget == null ? input.getColorTextureId() : volumeTarget.getColorTextureId());
        set(bloomShader, "NativeEmissionActive", nativeEmission == null ? 0 : 1);
        set(bloomShader, "ParticleEmissionActive", particleEmission == null ? 0 : 1);
        set(bloomShader, "HasAtmosphere", volumeTarget == null ? 0 : 1);
        var jitter = TemporalPostPass.jitterUv(activeFrame.snapshot());
        set2(bloomShader, "SourceJitter", jitter.x, jitter.y);
        set(bloomShader, "TerrainEmissionActive", terrainGeometry == null ? 0 : 1);
        set(bloomShader, "SurfaceEmissionActive", surfaceTarget == null ? 0 : 1);
        set(bloomShader, "EmissionScale", enabled("emission.enabled") ? value("emission.strength") : 0);
        set(bloomShader, "EmissionWeight", value("bloom.emissionWeight"));
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
        set(shader, "Time", (float) (activeFrame.snapshot().timeSeconds() % 86_400.0));
        set(shader, "Rain", environment.rain());
    }

    private void updateAdaptiveScale() {
        float scale = adaptiveQuality.update(lastGpuMilliseconds, value("performance.targetMs"), 1.0F / 60.0F,
                value("performance.minimumScale"), value("performance.recoverySeconds"), enabled("performance.adaptive"));
        if (scale < 0.999F) frames.debug().publish("adaptive-scale",
                new DebugAttachments.Attachment(0, Math.round(width * scale), Math.round(height * scale), "Effective internal scale"));
    }

    private Optional<FrameSnapshot> snapshot(Camera camera, Matrix4f cameraView, Matrix4f projection, Matrix4f unjittered) {
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
                matrix(view), matrix(projection), matrix(inverse), matrix(unjittered),
                width, height, photo.visualTime((environment.gameTime() + partialTick) / 20.0)));
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
        VegetationMotion.endFrame();
        ActorMotionPass.end();
        NativeEmissionCapture.end();
        ownedParticles.end();
        EntityForwardLighting.end();
        fabulous.restore();
        closeRouteOnly();
        if (nativeProjectionArgument != null) {
            nativeProjectionArgument.set(projectionBeforeJitter);
            RenderSystem.setProjectionMatrix(projectionBeforeJitter, sortingBeforeJitter);
            nativeProjectionArgument = null;
            projectionBeforeJitter = null;
            sortingBeforeJitter = null;
        }
        if (worldScope != null) {
            worldScope.close();
            worldScope = null;
        }
        worldOpen = false;
        activeFrame = null;
        parameters = null;
        renderingConfig = null;
        environment = null;
        atmosphere = null;
        opaqueCaptured = false;
        opaqueTarget = null;
        indirectTarget = null;
        screenGi = false;
        voxelGi = false;
        lightTiles = null;
        pointMaps = null;
        surfaceTarget = null;
        terrainGeometry = null;
        terrainColor = null;
        offscreenReflections = null;
        resolvedColor = null;
        cloudPrepared = false;
        replaceNativeClouds = false;
        volumeTarget = null;
        bloomTarget = null;
        shadowMaps = List.of();
        directionalMapsPrepared = false;
        glassTransmission = List.of();
        actorMotion = null;
        towardsSun = null;
        nativeTarget = null;
    }

    private void fail(Exception renderFailure) {
        failure = renderFailure.getMessage() == null ? renderFailure.getClass().getSimpleName() : renderFailure.getMessage();
        ClientState.store().rejectPreview(renderingConfig, failure);
        frames.invalidate(TemporalHistory.Reason.PIPELINE_CHANGE);
        indirect.invalidate();
        voxelLighting.invalidate();
        temporal.invalidate();
        ActorMotionPass.invalidate();
        cameraPost.invalidate();
        clouds.invalidate();
        photo.abort();
        graphicsPresented = false;
        handParameters = null;
        handLighting = null;
        handReflection = null;
        hand.invalidateHistory();
        terrainMaterials.invalidate();
        materialComposite.invalidate();
        guiCompositeFrame = false;
        guiCandidate = null;
        shadows.invalidatePointMaps();
        resetExposure();
        if (!loggedFailure) {
            if (renderingConfig == null || renderingConfig.value("performance.logShaderErrors").getAsBoolean())
                LOGGER.error("Taste renderer restored native ownership after a frame failure", renderFailure);
            else LOGGER.warn("Taste frame restored native ownership: {}", failure);
            loggedFailure = true;
        }
    }

    private void releaseGpu() {
        closeWorldScope();
        NativeEmissionCapture.release();
        ActorMotionPass.release();
        ownedParticles.close();
        lensWetness.clear();
        lightning = List.of();
        lightningFlash = 0F;
        if (nativeEmissionShader != null) { nativeEmissionShader.close(); nativeEmissionShader = null; }
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
        materialSurfaces.close();
        materialComposite.close();
        terrainMaterials.close();
        voxelLighting.close();
        meshSections.clear();
        clouds.close();
        temporal.close();
        cameraPost.close();
        photo.close();
        hand.close();
        portals.close();
        guiColor.close();
        fabulous.close();
        EntityForwardLighting.release();
        debugView.close();
        reflectionCapture.close();
        dimensionProfiles.clear();
        handParameters = null;
        handLighting = null;
        handReflection = null;
        graphicsPresented = false;
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

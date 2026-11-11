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
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.config.EffectiveConfig;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/** Owns the first shipping frame path: native world capture and configurable HDR-style presentation. */
public final class TasteRenderer implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TasteRenderer INSTANCE = new TasteRenderer();
    private CapabilityProbe.Result probe;
    private ShaderInstance shader;
    private TextureTarget source;
    private int width;
    private int height;
    private long frame;
    private String failure = "";
    private boolean loggedFailure;

    private TasteRenderer() { }

    public static TasteRenderer instance() { return INSTANCE; }
    public synchronized boolean baselineAvailable() { return probe != null && probe.baseline(); }
    public synchronized Optional<CapabilityProbe.Result> capabilityResult() { return Optional.ofNullable(probe); }
    public synchronized String failure() { return failure; }

    public synchronized void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        RenderSystem.assertOnRenderThread();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameRenderer == null) {
            releaseGpu();
            return;
        }
        ConfigSnapshot snapshot = ClientState.store().current();
        if (probe == null) probe = CapabilityProbe.probe();
        EffectiveConfig effective = EffectiveConfig.resolve(snapshot, ClientState.capabilities());
        if (!snapshot.requestedEnabled() || !effective.active()) {
            releaseGpu();
            return;
        }
        try {
            ensureReady(minecraft);
            if (!probe.baseline()) throw new IllegalStateException(probe.reason());
            draw(minecraft, snapshot, event.getPartialTick());
            failure = "";
            loggedFailure = false;
            frame++;
        } catch (IOException | RuntimeException renderingFailure) {
            failure = renderingFailure.getMessage() == null ? renderingFailure.getClass().getSimpleName() : renderingFailure.getMessage();
            releaseGpu();
            if (!loggedFailure) {
                LOGGER.error("Taste renderer disabled after a frame failure", renderingFailure);
                loggedFailure = true;
            }
        }
    }

    public synchronized void invalidateResources() {
        if (RenderSystem.isOnRenderThread()) releaseGpu();
        else RenderSystem.recordRenderCall(this::releaseGpu);
    }

    private void ensureReady(Minecraft minecraft) throws IOException {
        RenderTarget main = minecraft.getMainRenderTarget();
        if (probe == null) probe = CapabilityProbe.probe();
        if (!probe.baseline()) return;
        if (shader == null) {
            shader = new ShaderInstance(minecraft.getResourceManager(), new ResourceLocation("taste", "taste_final"), DefaultVertexFormat.POSITION_TEX);
        }
        if (source == null || width != main.width || height != main.height) {
            if (source != null) source.destroyBuffers();
            width = main.width;
            height = main.height;
            source = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            source.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    private void draw(Minecraft minecraft, ConfigSnapshot config, float partialTick) {
        RenderTarget main = minecraft.getMainRenderTarget();
        try (GlStateScope ignored = GlStateScope.capture()) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, source.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

            main.bindWrite(true);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShader(() -> shader);
            shader.setSampler("DiffuseSampler", source.getColorTextureId());
            shader.setSampler("DepthSampler", source.getDepthTextureId());
            set2("InSize", width, height);
            set1("FarPlane", Math.max(64.0F, minecraft.options.getEffectiveRenderDistance() * 16.0F));
            set1("Time", (float) ((frame + partialTick) / 20.0));
            set1("Rain", minecraft.level.getRainLevel(partialTick));
            set1("Exposure", powerOfTwo(config.value("color.exposureEV").getAsFloat()));
            set1("Saturation", config.value("color.saturation").getAsFloat());
            set1("Contrast", config.value("color.contrast").getAsFloat());
            set1("Temperature", config.value("color.temperature").getAsFloat());
            set1("Tint", config.value("color.tint").getAsFloat());
            set1("Gamma", config.value("color.gamma").getAsFloat());
            set1("BloomEnabled", config.value("bloom.enabled").getAsBoolean() ? 1.0F : 0.0F);
            set1("BloomStrength", config.value("bloom.strength").getAsFloat());
            set1("BloomThreshold", config.value("bloom.threshold").getAsFloat());
            set1("BloomRadius", config.value("bloom.radius").getAsFloat());
            set1("FogStrength", config.value("fog.enabled").getAsBoolean() ? config.value("fog.density").getAsFloat() : 0.0F);
            set1("FogDistance", config.value("fog.maxDistance").getAsFloat());
            set1("ShaftStrength", config.value("fog.volumetric").getAsBoolean() ? config.value("fog.shaftsStrength").getAsFloat() : 0.0F);
            set1("Vignette", config.value("camera.vignette").getAsFloat());
            set1("Grain", config.value("camera.grain").getAsFloat());
            set1("Sharpen", config.value("camera.sharpen").getAsFloat());
            set1("DofEnabled", config.value("camera.dof").getAsBoolean() ? 1.0F : 0.0F);
            set1("FocusDistance", config.value("camera.focusDistance").getAsFloat());
            set1("Aperture", config.value("camera.aperture").getAsFloat());
            set1("DofRadius", config.value("camera.dofMaxRadius").getAsFloat());
            set1("Outline", config.value("style.outline").getAsBoolean() ? config.value("style.outlineStrength").getAsFloat() : 0.0F);
            set1("OutlineWidth", config.value("style.outlineWidth").getAsFloat());
            set1("ToonBands", config.value("style.shading").getAsString().equals("toon")
                    ? config.value("style.toonBands").getAsFloat() : 0.0F);
            set1("WetLens", config.value("weather.wetLens").getAsBoolean() ? 1.0F : 0.0F);
            set1("Underwater", minecraft.player != null && minecraft.player.isUnderWater() ? 1.0F : 0.0F);
            set1("UnderwaterDensity", config.value("underwater.fog").getAsBoolean()
                    ? config.value("underwater.fogDensity").getAsFloat() : 0.0F);
            set1("Thunder", minecraft.level.getThunderLevel(partialTick) * config.value("weather.thunderFlash").getAsFloat());
            set1("ReducedMotion", config.value("accessibility.reducedMotion").getAsBoolean() ? 1.0F : 0.0F);
            set1("MinimumContrast", config.value("accessibility.minimumContrast").getAsFloat());
            set1("ToneMapper", config.value("color.toneMapper").getAsString().equals("neutral") ? 1.0F : 0.0F);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.vertex(-1.0D, -1.0D, 0.0D).uv(0.0F, 0.0F).endVertex();
            builder.vertex(1.0D, -1.0D, 0.0D).uv(1.0F, 0.0F).endVertex();
            builder.vertex(1.0D, 1.0D, 0.0D).uv(1.0F, 1.0F).endVertex();
            builder.vertex(-1.0D, 1.0D, 0.0D).uv(0.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(builder.end());
            shader.clear();
        }
    }

    private void set1(String name, float value) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(value);
    }

    private void set2(String name, float x, float y) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(x, y);
    }

    private static float powerOfTwo(float exponent) { return (float) Math.pow(2.0, exponent); }

    private void releaseGpu() {
        if (source != null) {
            source.destroyBuffers();
            source = null;
        }
        if (shader != null) {
            shader.close();
            shader = null;
        }
        width = 0;
        height = 0;
    }

    @Override public synchronized void close() { releaseGpu(); }
}

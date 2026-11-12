package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** World-only camera optics. Native hand and HUD render after this pass. */
public final class CameraPostPass implements AutoCloseable {
    private final DisplayLut lut = new DisplayLut();
    private ShaderInstance opticsShader;
    private ShaderInstance displayShader;
    private TextureTarget optics;
    private TextureTarget raw;
    private TextureTarget opaque;
    private long opticsGeneration = -1;
    private long opticsTime;
    private int opticsIndex;
    private int outputWidth;
    private int outputHeight;
    private float sunU;
    private float sunV;
    private boolean sunInView;

    public void prepare(TextureTarget world, TextureTarget capturedOpaque, TextureTarget clouds,
            TargetPool pool, FrameCoordinator.Frame frame, PipelineParameters parameters,
            float sourceU, float sourceV, float sourceVisibility, Runnable draw) throws IOException {
        raw = world;
        opaque = capturedOpaque;
        sunU = sourceU;
        sunV = sourceV;
        sunInView = sourceU > 0.0F && sourceU < 1.0F && sourceV > 0.0F && sourceV < 1.0F;
        boolean focus = enabled(parameters, "camera.dof") && parameters.value("camera.focusMode").getAsString().equals("crosshair");
        boolean flare = enabled(parameters, "camera.lensFlare") && value(parameters, "camera.flareStrength") > 0.0F;
        if (value(parameters, "color.lutStrength") > 0.0F) lut.load(Minecraft.getInstance().getResourceManager());
        if (!focus && !flare) {
            pool.release("camera-optics-0");
            pool.release("camera-optics-1");
            invalidateOptics();
            return;
        }
        if (opticsShader == null) opticsShader = createShader("taste_optics");
        try (GlStateScope ignored = GlStateScope.capture()) {
            TextureTarget output = pool.acquire(new TargetDescriptor("camera-optics-" + opticsIndex, 1, 1,
                    TargetDescriptor.Format.RGBA16F, false, 1, 1, 1));
            boolean reuse = optics != null && opticsGeneration == frame.ticket().generation()
                    && frame.ticket().reasons().isEmpty();
            output.bindWrite(true);
            TemporalPostPass.setupFullscreen(opticsShader);
            opticsShader.setSampler("DepthSampler", world.getDepthTextureId());
            opticsShader.setSampler("CloudSampler", (clouds == null ? world : clouds).getColorTextureId());
            set(opticsShader, "HasCloudCoverage", clouds == null ? 0.0F : 1.0F);
            opticsShader.setSampler("PreviousOptics", reuse ? optics.getColorTextureId() : world.getColorTextureId());
            opticsShader.getUniform("InverseProjection").set(TemporalPostPass.matrix(frame.snapshot().inverseProjection()));
            Vector2f jitter = TemporalPostPass.jitterUv(frame.snapshot());
            opticsShader.getUniform("WorldJitter").set(jitter.x, jitter.y);
            opticsShader.getUniform("SourceUv").set(sourceU, sourceV);
            set(opticsShader, "SourceVisibility", flare ? sourceVisibility : 0.0F);
            set(opticsShader, "CrosshairFocus", focus ? 1.0F : 0.0F);
            set(opticsShader, "ManualFocus", value(parameters, "camera.focusDistance"));
            long now = System.nanoTime();
            float seconds = Math.min(0.25F, (now - opticsTime) * 1.0e-9F);
            set(opticsShader, "HistoryBlend", reuse ? (float) Math.exp(-seconds * 11.0F) : 0.0F);
            try {
                draw.run();
                requireNoGlError("camera optics");
            } finally {
                opticsShader.clear();
            }
            optics = output;
            opticsGeneration = frame.ticket().generation();
            opticsTime = now;
            opticsIndex ^= 1;
        }
    }

    public RenderTarget finalTarget(RenderTarget nativeTarget, TargetPool pool, String mode) {
        outputWidth = nativeTarget.width;
        outputHeight = nativeTarget.height;
        if (!mode.equals("fxaa")) {
            pool.release("display-grade");
            return nativeTarget;
        }
        return pool.acquire(new TargetDescriptor("display-grade", nativeTarget.width, nativeTarget.height,
                TargetDescriptor.Format.RGBA16F, false, 1, 1, 1));
    }

    public void bind(ShaderInstance shader, PipelineParameters parameters, TextureTarget color,
            FrameCoordinator.Frame frame, String mode) {
        boolean reducedMotion = enabled(parameters, "accessibility.reducedMotion");
        boolean dof = enabled(parameters, "camera.dof");
        boolean autoFocus = dof && parameters.value("camera.focusMode").getAsString().equals("crosshair") && optics != null;
        set(shader, "DofEnabled", dof ? 1.0F : 0.0F);
        set(shader, "AutoFocus", autoFocus ? 1.0F : 0.0F);
        set(shader, "FocusDistance", value(parameters, "camera.focusDistance"));
        set(shader, "Aperture", value(parameters, "camera.aperture"));
        set(shader, "DofRadius", value(parameters, "camera.dofMaxRadius"));
        float focalLength = Math.abs(TemporalPostPass.matrix(frame.snapshot().unjitteredProjection()).m11()) * 0.012F;
        set(shader, "FocalLength", focalLength);
        set(shader, "Shutter", !reducedMotion && enabled(parameters, "camera.motionBlur") ? value(parameters, "camera.shutter") : 0.0F);
        set(shader, "MotionSamples", value(parameters, "camera.motionSamples"));
        set(shader, "ChromaticAberration", reducedMotion ? 0.0F : value(parameters, "camera.chromaticAberration"));
        set(shader, "Vignette", value(parameters, "camera.vignette"));
        set(shader, "Grain", value(parameters, "camera.grain"));
        set(shader, "Sharpen", value(parameters, "camera.sharpen"));
        set(shader, "ReducedMotion", reducedMotion ? 1.0F : 0.0F);
        set(shader, "ProtectUnknown", opaque != null && enabled(parameters, "accessibility.preserveTelegraphs") ? 1.0F : 0.0F);
        set(shader, "FlareStrength", sunInView && optics != null && enabled(parameters, "camera.lensFlare")
                ? value(parameters, "camera.flareStrength") * value(parameters, "accessibility.flashLimit") : 0.0F);
        shader.getUniform("OutputSize").set((float) outputWidth, (float) outputHeight);
        shader.getUniform("SunUv").set(sunU, sunV);
        set(shader, "FlashLimit", value(parameters, "accessibility.flashLimit"));
        set(shader, "MinimumContrast", value(parameters, "accessibility.minimumContrast"));
        set(shader, "ColorVision", switch (parameters.value("accessibility.colorVision").getAsString()) {
            case "protan" -> 1.0F;
            case "deutan" -> 2.0F;
            case "tritan" -> 3.0F;
            default -> 0.0F;
        });
        set(shader, "DeferDisplayEncoding", mode.equals("fxaa") ? 1.0F : 0.0F);
        set(shader, "LutSize", lut.size());
        set(shader, "LutStrength", lut.size() > 0 ? value(parameters, "color.lutStrength") : 0.0F);
        shader.setSampler("OpticsSampler", optics == null ? color.getColorTextureId() : optics.getColorTextureId());
        shader.setSampler("LutSampler", lut.size() == 0 ? color.getColorTextureId() : lut.texture());
        shader.setSampler("RawWorld", raw.getColorTextureId());
        shader.setSampler("OpaqueWorld", (opaque == null ? raw : opaque).getColorTextureId());
    }

    public void present(RenderTarget graded, RenderTarget nativeTarget, Runnable draw) throws IOException {
        if (graded == nativeTarget) return;
        if (displayShader == null) displayShader = createShader("taste_display");
        try (GlStateScope ignored = GlStateScope.capture()) {
            nativeTarget.bindWrite(true);
            TemporalPostPass.setupFullscreen(displayShader);
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            displayShader.setSampler("DisplaySampler", graded.getColorTextureId());
            try {
                draw.run();
                requireNoGlError("FXAA display");
            } finally {
                displayShader.clear();
            }
        }
    }

    public TextureTarget focusTexture() { return optics; }

    public String lutStatus() { return lut.status(); }

    public void invalidate() {
        invalidateOptics();
        raw = null;
        opaque = null;
    }

    private void invalidateOptics() {
        optics = null;
        opticsGeneration = -1;
        opticsTime = 0L;
        opticsIndex = 0;
    }

    private static ShaderInstance createShader(String name) throws IOException {
        return new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                new ResourceLocation("taste", name), DefaultVertexFormat.POSITION_TEX);
    }

    private static boolean enabled(PipelineParameters parameters, String id) { return parameters.value(id).getAsBoolean(); }
    private static float value(PipelineParameters parameters, String id) { return TemporalPostPass.value(parameters, id); }
    private static void set(ShaderInstance shader, String uniform, float value) { shader.getUniform(uniform).set(value); }

    private static void requireNoGlError(String stage) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) throw new IllegalStateException(stage + " failed with GL error " + error);
    }

    @Override public void close() {
        invalidate();
        lut.close();
        if (opticsShader != null) {
            opticsShader.close();
            opticsShader = null;
        }
        if (displayShader != null) {
            displayShader.close();
            displayShader = null;
        }
    }
}

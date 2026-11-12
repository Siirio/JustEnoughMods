package dev.taste.engine.effects;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.taste.engine.config.ConfigSnapshot;
import dev.taste.engine.render.FrameCoordinator;
import dev.taste.engine.render.FrameSnapshot;
import dev.taste.engine.render.GlStateScope;
import dev.taste.engine.render.PipelineParameters;
import dev.taste.engine.render.TargetDescriptor;
import dev.taste.engine.render.TargetPool;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Owns explicit world capture, linear HDR accumulation and the local photo preview. */
public final class PhotoCapture implements AutoCloseable {
    private static final PhotoCapture INSTANCE = new PhotoCapture();
    private static final String CAPTURE = "photo-output";
    private static final String PREVIEW = "photo-preview";
    private static final long MAX_CAPTURE_NANOS = 45_000_000_000L;
    private enum Mode { CLOSED, PREVIEW, COLLECTING, READY }

    private Mode mode = Mode.CLOSED;
    private ClientLevel level;
    private boolean focusGuide;
    private boolean hideHud;
    private boolean hudHeld;
    private boolean previousHud;
    private boolean lockVisualTime;
    private int factor;
    private int requestedFrames;
    private int frames;
    private int index;
    private int width;
    private int height;
    private long memoryBudget;
    private long started;
    private double frozenTime = Double.NaN;
    private float frozenSunAngle = Float.NaN;
    private FrameSnapshot previousFrame;
    private TextureTarget accumulation;
    private TextureTarget finished;
    private ShaderInstance accumulationShader;
    private ShaderInstance guideShader;

    private PhotoCapture() { }

    public static PhotoCapture instance() { return INSTANCE; }

    public void toggle(Minecraft minecraft, ConfigSnapshot config) {
        if (mode != Mode.CLOSED) {
            abort();
            return;
        }
        if (minecraft.level == null || !enabled(config)) {
            notify(minecraft, "taste.photo.disabled");
            return;
        }
        level = minecraft.level;
        hideHud = config.value("photo.hideHud").getAsBoolean();
        focusGuide = config.value("photo.focusPeaking").getAsBoolean();
        lockVisualTime = config.value("photo.lockVisualTime").getAsBoolean();
        factor = Integer.parseInt(config.value("photo.supersample").getAsString());
        requestedFrames = config.value("photo.accumulationFrames").getAsInt();
        memoryBudget = config.value("performance.memoryBudgetMiB").getAsLong() * 1024L * 1024L;
        mode = Mode.PREVIEW;
        notify(minecraft, "taste.photo.preview");
    }

    public void action(Minecraft minecraft) {
        if (mode == Mode.PREVIEW) {
            if (hideHud) {
                previousHud = minecraft.options.hideGui;
                minecraft.options.hideGui = true;
                hudHeld = true;
            }
            mode = Mode.COLLECTING;
            resetAccumulation();
            started = System.nanoTime();
            frozenTime = Double.NaN;
            frozenSunAngle = Float.NaN;
        } else if (mode == Mode.READY && finished != null) {
            TextureTarget photo = finished;
            Runnable save = () -> save(minecraft, photo);
            if (RenderSystem.isOnRenderThread()) save.run();
            else RenderSystem.recordRenderCall(save::run);
        }
    }

    private void save(Minecraft minecraft, TextureTarget photo) {
        if (mode != Mode.READY || finished != photo) return;
        try (GlStateScope ignored = GlStateScope.capture()) {
            Screenshot.grab(minecraft.gameDirectory, photo,
                    message -> minecraft.execute(() -> minecraft.gui.getChat().addMessage(message)));
        } catch (RuntimeException failure) {
            notify(minecraft, "taste.photo.saveFailed");
        } finally {
            finished = null;
            mode = Mode.PREVIEW;
        }
    }

    public void tick(Minecraft minecraft, ConfigSnapshot config) {
        if (mode == Mode.CLOSED) return;
        if (minecraft.level != level || minecraft.screen != null || !enabled(config)) {
            abort();
        } else if (collecting() && System.nanoTime() - started > MAX_CAPTURE_NANOS) {
            abort();
            notify(minecraft, "taste.photo.timeout");
        }
    }

    private static boolean enabled(ConfigSnapshot config) {
        return config.requestedEnabled() && config.value("photo.enabled").getAsBoolean();
    }

    public boolean collecting() { return mode == Mode.COLLECTING; }
    public boolean freezesDecoration() { return collecting() && lockVisualTime; }
    public int sequenceLength() { return Math.max(2, requestedFrames); }
    public long jitterIndex() { return frames; }
    public boolean lockExposure() { return collecting() && lockVisualTime && frames > 1; }

    public double visualTime(double ordinary) {
        if (!collecting() || !lockVisualTime) return ordinary;
        if (Double.isNaN(frozenTime)) frozenTime = ordinary;
        return frozenTime;
    }

    public float sunAngle(float ordinary) {
        if (!collecting() || !lockVisualTime) return ordinary;
        if (Float.isNaN(frozenSunAngle)) frozenSunAngle = ordinary;
        return frozenSunAngle;
    }

    public float scale(float ordinaryScale, int nativeWidth, int nativeHeight) {
        if (!collecting()) return ordinaryScale;
        long captureWidth = (long) nativeWidth * factor;
        long captureHeight = (long) nativeHeight * factor;
        int maximum = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        // Two HDR/depth histories, the saved output and the core world and scene targets.
        long minimumBytes = captureWidth * captureHeight * 52L;
        if (captureWidth > maximum || captureHeight > maximum || minimumBytes > memoryBudget) {
            abort();
            notify(Minecraft.getInstance(), "taste.photo.limit");
            return ordinaryScale;
        }
        return factor;
    }

    public TextureTarget accumulate(TextureTarget hdr, TextureTarget world, TargetPool pool,
            FrameCoordinator.Frame frame, Runnable draw) throws IOException {
        if (!collecting()) {
            pool.release("photo-history-0");
            pool.release("photo-history-1");
            if (mode != Mode.READY) pool.release(CAPTURE);
            return hdr;
        }
        FrameSnapshot current = frame.snapshot();
        if (previousFrame != null && (!frame.ticket().reasons().isEmpty() || cameraChanged(previousFrame, current)))
            resetAccumulation();
        if (accumulationShader == null) accumulationShader = shader("taste_photo_accumulate");

        try (GlStateScope ignored = GlStateScope.capture()) {
            TextureTarget output = pool.acquire(descriptor("photo-history-" + index, hdr.width, hdr.height,
                    TargetDescriptor.Format.RGBA16F, true));
            output.bindWrite(true);
            setup(accumulationShader);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
            RenderSystem.depthMask(true);
            accumulationShader.setSampler("CurrentColor", hdr.getColorTextureId());
            accumulationShader.setSampler("CurrentDepth", world.getDepthTextureId());
            accumulationShader.setSampler("HistoryColor", frames == 0 ? hdr.getColorTextureId() : accumulation.getColorTextureId());
            accumulationShader.setSampler("HistoryDepth", frames == 0 ? world.getDepthTextureId() : accumulation.getDepthTextureId());
            setDepthUniforms(accumulationShader, current);
            accumulationShader.getUniform("HasHistory").set(frames == 0 ? 0.0F : 1.0F);
            draw(accumulationShader, draw);
            index ^= 1;
            frames++;
            width = hdr.width;
            height = hdr.height;
            accumulation = output;
            previousFrame = current;
            return output;
        }
    }

    public RenderTarget output(RenderTarget nativeTarget, TargetPool pool) {
        if (collecting()) return pool.acquire(descriptor(CAPTURE, width, height, TargetDescriptor.Format.RGBA8, false));
        if (mode != Mode.CLOSED && focusGuide) return pool.acquire(descriptor(PREVIEW,
                nativeTarget.width, nativeTarget.height, TargetDescriptor.Format.RGBA8, false));
        pool.release(PREVIEW);
        return nativeTarget;
    }

    public void present(RenderTarget output, RenderTarget nativeTarget, TextureTarget world, TextureTarget focus,
            FrameCoordinator.Frame frame, PipelineParameters parameters, Runnable draw) throws IOException {
        if (output == nativeTarget) return;
        if (focusGuide) {
            if (guideShader == null) guideShader = shader("taste_photo_guide");
            nativeTarget.bindWrite(true);
            setup(guideShader);
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            guideShader.setSampler("PhotoColor", output.getColorTextureId());
            guideShader.setSampler("CurrentDepth", world.getDepthTextureId());
            guideShader.setSampler("FocusSampler", focus == null ? output.getColorTextureId() : focus.getColorTextureId());
            setDepthUniforms(guideShader, frame.snapshot());
            boolean automatic = focus != null && parameters.value("camera.dof").getAsBoolean()
                    && parameters.value("camera.focusMode").getAsString().equals("crosshair");
            guideShader.getUniform("AutoFocus").set(automatic ? 1.0F : 0.0F);
            guideShader.getUniform("FocusDistance").set(parameters.value("camera.focusDistance").getAsFloat());
            guideShader.getUniform("FocalLength").set(Math.abs(matrix(frame.snapshot().unjitteredProjection()).m11()) * 0.012F);
            guideShader.getUniform("Aperture").set(parameters.value("camera.aperture").getAsFloat());
            guideShader.getUniform("OutSize").set((float) nativeTarget.width, (float) nativeTarget.height);
            draw(guideShader, draw);
        } else {
            RenderSystem.disableScissor();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, output.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, nativeTarget.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, output.width, output.height, 0, 0, nativeTarget.width, nativeTarget.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        }
        if (collecting() && frames >= requestedFrames) {
            finished = (TextureTarget) output;
            mode = Mode.READY;
            restoreHud();
            notify(Minecraft.getInstance(), "taste.photo.ready");
        }
    }

    private static boolean cameraChanged(FrameSnapshot old, FrameSnapshot current) {
        if (old.worldEpoch() != current.worldEpoch() || old.width() != current.width() || old.height() != current.height()
                || !old.dimension().equals(current.dimension()) || !old.unjitteredProjection().equals(current.unjitteredProjection())) return true;
        float[] delta = old.cameraWorld().relativeTo(current.cameraWorld());
        if (delta[0] * delta[0] + delta[1] * delta[1] + delta[2] * delta[2] > 0.000004F) return true;
        Matrix4f a = matrix(old.view());
        Matrix4f b = matrix(current.view());
        return Math.abs(a.m00() - b.m00()) + Math.abs(a.m01() - b.m01()) + Math.abs(a.m02() - b.m02())
                + Math.abs(a.m10() - b.m10()) + Math.abs(a.m11() - b.m11()) + Math.abs(a.m12() - b.m12())
                + Math.abs(a.m20() - b.m20()) + Math.abs(a.m21() - b.m21()) + Math.abs(a.m22() - b.m22()) > 0.0002F;
    }

    private static void setDepthUniforms(ShaderInstance shader, FrameSnapshot frame) {
        shader.getUniform("InverseProjection").set(matrix(frame.inverseProjection()));
        Matrix4f difference = matrix(frame.projection()).mul(matrix(frame.unjitteredProjection()).invert());
        shader.getUniform("WorldJitter").set(difference.m30() * 0.5F, difference.m31() * 0.5F);
    }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 matrix) { return new Matrix4f().set(matrix.columnMajor()); }

    private static TargetDescriptor descriptor(String name, int width, int height, TargetDescriptor.Format format, boolean depth) {
        return new TargetDescriptor(name, width, height, format, depth, 1, 1, 1);
    }

    private static ShaderInstance shader(String name) throws IOException {
        return new ShaderInstance(Minecraft.getInstance().getResourceManager(), new ResourceLocation("taste", name),
                DefaultVertexFormat.POSITION_TEX);
    }

    private static void setup(ShaderInstance shader) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> shader);
    }

    private static void draw(ShaderInstance shader, Runnable draw) {
        try {
            draw.run();
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) throw new IllegalStateException("Photo pass failed with GL error " + error);
        } finally {
            shader.clear();
        }
    }

    private void resetAccumulation() {
        previousFrame = null;
        accumulation = null;
        frames = 0;
        index = 0;
    }

    private void restoreHud() {
        if (hudHeld) {
            Minecraft.getInstance().options.hideGui = previousHud;
            hudHeld = false;
        }
    }

    public void abort() {
        restoreHud();
        mode = Mode.CLOSED;
        level = null;
        finished = null;
        resetAccumulation();
    }

    private static void notify(Minecraft minecraft, String key) {
        if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable(key), true);
    }

    @Override public void close() {
        abort();
        if (accumulationShader != null) {
            accumulationShader.close();
            accumulationShader = null;
        }
        if (guideShader != null) {
            guideShader.close();
            guideShader = null;
        }
    }
}

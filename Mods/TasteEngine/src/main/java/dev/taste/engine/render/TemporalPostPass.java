package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

/** Reprojects the retained world before camera filters, grading and native hand rendering. */
public final class TemporalPostPass implements AutoCloseable {
    private ShaderInstance shader;
    private TextureTarget history;
    private FrameSnapshot previous;
    private FrameSnapshot motionPrevious;
    private long previousRevision = -1;
    private int index;

    public TextureTarget resolve(TextureTarget color, TextureTarget world, TextureTarget opaque, TargetPool pool,
            FrameCoordinator.Frame frame, PipelineParameters parameters, String mode, boolean reactiveActorFlash,
            TerrainMaterialPass.Geometry terrain, TextureTarget actorMotion, TextureTarget nativeEmission,
            TextureTarget particleCoverage, boolean inverseCoverage,
            Runnable draw) throws IOException {
        FrameSnapshot current = frame.snapshot();
        boolean consecutive = previous != null && current.frameIndex() == previous.frameIndex() + 1;
        boolean valid = consecutive && previousRevision == parameters.revision()
                && frame.ticket().reasons().isEmpty() && !cameraCut(current, previous);
        motionPrevious = valid ? previous : null;
        previous = current;
        previousRevision = parameters.revision();

        if (!mode.equals("taa")) {
            pool.release("temporal-0");
            pool.release("temporal-1");
            history = null;
            index = 0;
            return color;
        }
        if (shader == null) {
            shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation("taste", "taste_temporal"), DefaultVertexFormat.POSITION_TEX);
        }

        TextureTarget output;
        try (GlStateScope ignored = GlStateScope.capture()) {
            output = pool.acquire(new TargetDescriptor("temporal-" + index, color.width, color.height,
                    TargetDescriptor.Format.RGBA16F, false, 1, 1, 1));
            boolean reuse = valid && !reactiveActorFlash && history != null
                    && history.width == output.width && history.height == output.height;
            output.bindWrite(true);
            setupFullscreen(shader);
            shader.setSampler("CurrentColor", color.getColorTextureId());
            shader.setSampler("CurrentDepth", world.getDepthTextureId());
            shader.setSampler("History", reuse ? history.getColorTextureId() : color.getColorTextureId());
            shader.setSampler("RawWorld", world.getColorTextureId());
            shader.setSampler("OpaqueWorld", (opaque == null ? world : opaque).getColorTextureId());
            shader.setSampler("OpaqueDepth", (opaque == null ? world : opaque).getDepthTextureId());
            shader.setSampler("TerrainDepth", terrain == null ? world.getDepthTextureId() : terrain.albedoAo().getDepthTextureId());
            shader.setSampler("TerrainMotion", terrain == null ? color.getColorTextureId() : terrain.motionLight().getColorTextureId());
            shader.setSampler("ActorMotion", actorMotion == null ? color.getColorTextureId() : actorMotion.getColorTextureId());
            shader.setSampler("NativeEmission", nativeEmission == null ? color.getColorTextureId() : nativeEmission.getColorTextureId());
            shader.setSampler("ParticleCoverage", particleCoverage == null ? color.getColorTextureId() : particleCoverage.getColorTextureId());
            shader.getUniform("HasTerrainMotion").set(terrain != null && terrain.motionValid() ? 1.0F : 0.0F);
            shader.getUniform("HasActorMotion").set(actorMotion == null ? 0.0F : 1.0F);
            shader.getUniform("HasNativeEmission").set(nativeEmission == null ? 0.0F : 1.0F);
            shader.getUniform("ParticleCoverageMode").set(particleCoverage == null ? 0.0F : inverseCoverage ? 2.0F : 1.0F);
            shader.getUniform("CurrentInverseProjection").set(matrix(current.inverseProjection()));
            shader.getUniform("PreviousClipFromView").set(reuse ? previousClipFromView(current, motionPrevious) : new Matrix4f());
            Vector2f jitter = jitterUv(current);
            shader.getUniform("WorldJitter").set(jitter.x, jitter.y);
            shader.getUniform("HistoryWeight").set(reuse ? value(parameters, "antialiasing.historyWeight") : 0.0F);
            shader.getUniform("Disocclusion").set(value(parameters, "antialiasing.disocclusionThreshold"));
            shader.getUniform("VarianceGamma").set(value(parameters, "antialiasing.varianceGamma"));
            shader.getUniform("ReactiveStrength").set(value(parameters, "antialiasing.reactiveStrength"));
            shader.getUniform("ProtectUnknown").set(parameters.value("accessibility.preserveTelegraphs").getAsBoolean() ? 1.0F : 0.0F);
            try {
                draw.run();
                if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new IllegalStateException("Temporal resolve failed");
            } finally {
                shader.clear();
            }
        }

        history = output;
        index ^= 1;
        return output;
    }

    public void bindFinal(ShaderInstance finalShader, FrameCoordinator.Frame frame, TextureTarget world,
            TerrainMaterialPass.Geometry terrain, TextureTarget actors) {
        FrameSnapshot current = frame.snapshot();
        Vector2f jitter = jitterUv(current);
        finalShader.getUniform("WorldJitter").set(jitter.x, jitter.y);
        finalShader.getUniform("HasMotionHistory").set(motionPrevious == null ? 0.0F : 1.0F);
        finalShader.getUniform("PreviousClipFromView").set(motionPrevious == null ? new Matrix4f()
                : previousClipFromView(current, motionPrevious));
        finalShader.setSampler("TerrainDepth", terrain == null ? world.getDepthTextureId() : terrain.albedoAo().getDepthTextureId());
        finalShader.setSampler("TerrainMotion", terrain == null ? world.getColorTextureId() : terrain.motionLight().getColorTextureId());
        finalShader.setSampler("ActorMotion", actors == null ? world.getColorTextureId() : actors.getColorTextureId());
        finalShader.getUniform("HasTerrainMotion").set(terrain != null && terrain.motionValid() ? 1.0F : 0.0F);
        finalShader.getUniform("HasActorMotion").set(actors == null ? 0.0F : 1.0F);
    }

    public static Matrix4f jitteredProjection(Matrix4f projection, int width, int height, int sequenceLength, long frameIndex) {
        int sample = (int) (frameIndex % Math.max(2, sequenceLength)) + 1;
        float dx = 2.0F * (radicalInverse(sample, 2) - 0.5F) / width;
        float dy = 2.0F * (radicalInverse(sample, 3) - 0.5F) / height;
        return new Matrix4f().translation(dx, dy, 0.0F).mul(projection);
    }

    static Vector2f jitterUv(FrameSnapshot frame) {
        if (frame.projection().equals(frame.unjitteredProjection())) return new Vector2f();
        Matrix4f difference = matrix(frame.projection()).mul(matrix(frame.unjitteredProjection()).invert());
        return new Vector2f(difference.m30() * 0.5F, difference.m31() * 0.5F);
    }

    static Matrix4f matrix(FrameSnapshot.Matrix4 source) {
        return new Matrix4f().set(source.columnMajor());
    }

    static void setupFullscreen(ShaderInstance program) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> program);
    }

    static float value(PipelineParameters parameters, String id) {
        return parameters.value(id).getAsFloat();
    }

    private static Matrix4f previousClipFromView(FrameSnapshot current, FrameSnapshot old) {
        float[] rebase = current.origin().relativeTo(old.origin());
        Matrix4f oldView = matrix(old.view()).translate(rebase[0], rebase[1], rebase[2]);
        return matrix(old.unjitteredProjection()).mul(oldView).mul(matrix(current.view()).invert());
    }

    private static boolean cameraCut(FrameSnapshot current, FrameSnapshot old) {
        float[] delta = current.cameraWorld().relativeTo(old.cameraWorld());
        if (delta[0] * delta[0] + delta[1] * delta[1] + delta[2] * delta[2] > 16 * 16) return true;
        Matrix4f currentView = matrix(current.view());
        Matrix4f oldView = matrix(old.view());
        return currentView.m02() * oldView.m02() + currentView.m12() * oldView.m12()
                + currentView.m22() * oldView.m22() < 0.5F;
    }

    private static float radicalInverse(int sample, int base) {
        float result = 0.0F;
        float fraction = 1.0F / base;
        while (sample > 0) {
            result += (sample % base) * fraction;
            sample /= base;
            fraction /= base;
        }
        return result;
    }

    public void invalidate() {
        history = null;
        previous = null;
        motionPrevious = null;
        previousRevision = -1;
        index = 0;
    }

    @Override public void close() {
        invalidate();
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}

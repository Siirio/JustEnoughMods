package dev.taste.engine.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Screen-space occlusion and diffuse bounce from the captured opaque view. */
public final class IndirectLightingPass implements AutoCloseable {
    private ShaderInstance shader;
    private TextureTarget previousTarget;
    private FrameSnapshot previousFrame;
    private int nextIndex;
    private long previousRevision = -1;

    public TextureTarget render(TargetPool targets, TextureTarget opaque, FrameSnapshot frame,
            PipelineParameters parameters, boolean screenGi, Runnable draw) throws IOException {
        boolean ao = parameters.value("ao.enabled").getAsBoolean();
        if (opaque == null || !(ao || screenGi)) {
            targets.release("indirect-0");
            targets.release("indirect-1");
            invalidate();
            return null;
        }
        if (shader == null) {
            shader = new ShaderInstance(Minecraft.getInstance().getResourceManager(),
                    new ResourceLocation("taste", "taste_indirect"), DefaultVertexFormat.POSITION_TEX);
        }
        float scale = ao ? Float.parseFloat(parameters.value("ao.resolutionScale").getAsString()) : 0.5F;
        int width = Math.max(1, Math.round(opaque.width * scale));
        int height = Math.max(1, Math.round(opaque.height * scale));
        boolean useHistory = previousFrame != null && previousFrame.worldEpoch() == frame.worldEpoch()
                && previousTarget.width == width && previousTarget.height == height
                && previousRevision == parameters.revision() && frame.timeSeconds() >= previousFrame.timeSeconds()
                && previousFrame.unjitteredProjection().equals(frame.unjitteredProjection());
        TextureTarget output = targets.acquire(new TargetDescriptor("indirect-" + nextIndex, width, height,
                TargetDescriptor.Format.RGBA16F, true, 1, 1, 1));
        RenderSystem.disableScissor();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, opaque.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, output.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, opaque.width, opaque.height, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        output.bindWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(() -> shader);
        shader.setSampler("OpaqueColor", opaque.getColorTextureId());
        shader.setSampler("OpaqueDepth", opaque.getDepthTextureId());
        shader.setSampler("PreviousIndirect", useHistory ? previousTarget.getColorTextureId() : opaque.getColorTextureId());
        shader.setSampler("PreviousDepth", useHistory ? previousTarget.getDepthTextureId() : opaque.getDepthTextureId());
        Uniform inverse = shader.getUniform("InverseProjection");
        inverse.set(matrix(frame.inverseProjection()));
        shader.getUniform("Projection").set(matrix(frame.projection()));
        set("AoDirections", ao ? value(parameters, "ao.directions") : 0);
        set("AoSteps", value(parameters, "ao.steps"));
        set("AoRadius", value(parameters, "ao.radius"));
        set("AoThickness", value(parameters, "ao.thickness"));
        set("AoStrength", value(parameters, "ao.strength"));
        set("GiSamples", screenGi ? value(parameters, "gi.screenSamples") : 0);
        set("GiDistance", value(parameters, "gi.maxDistance"));
        set("GiIntensity", value(parameters, "gi.intensity"));
        set("ColorBleed", value(parameters, "gi.colorBleed"));
        set("AoTemporal", useHistory ? value(parameters, "ao.temporalWeight") : 0);
        set("GiTemporal", useHistory ? value(parameters, "gi.temporalWeight") : 0);
        if (useHistory) {
            FrameSnapshot.Vec3d current = frame.origin();
            FrameSnapshot.Vec3d previous = previousFrame.origin();
            Matrix4f oldView = matrix(previousFrame.view()).translate((float) (current.x() - previous.x()),
                    (float) (current.y() - previous.y()), (float) (current.z() - previous.z()));
            Matrix4f reprojection = matrix(previousFrame.projection()).mul(oldView).mul(matrix(frame.view()).invert());
            shader.getUniform("Reprojection").set(reprojection);
        }
        try {
            draw.run();
        } finally {
            shader.clear();
        }
        previousTarget = output;
        previousFrame = frame;
        previousRevision = parameters.revision();
        nextIndex ^= 1;
        return output;
    }

    private static Matrix4f matrix(FrameSnapshot.Matrix4 values) { return new Matrix4f().set(values.columnMajor()); }
    private static float value(PipelineParameters parameters, String id) { return parameters.value(id).getAsFloat(); }
    private void set(String name, float value) { shader.getUniform(name).set(value); }

    public void invalidate() {
        previousTarget = null;
        previousFrame = null;
        previousRevision = -1;
        nextIndex = 0;
    }

    @Override public void close() {
        invalidate();
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }
}

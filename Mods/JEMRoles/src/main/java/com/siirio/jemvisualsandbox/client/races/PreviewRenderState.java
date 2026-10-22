package com.siirio.jemvisualsandbox.client.races;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Captures the shared preview state; each invocation owns its own restoration scope. */
final class PreviewRenderState implements AutoCloseable {
    private final EntityRenderDispatcher dispatcher;
    private final PoseStack modelView;
    private final Matrix4f oldProjection;
    private final Matrix3f oldInverseView;
    private final VertexSorting oldSorting;
    private final ShaderInstance oldShader;
    private final float[] oldColor;
    private final float oldFogStart;
    private final float oldFogEnd;
    private final int[] oldTextures;
    private final int[] viewport;
    private final int[] scissor;
    private final boolean oldScissor;
    private final boolean oldDepth;
    private final boolean oldDepthMask;
    private final int oldDepthFunc;
    private final boolean oldBlend;
    private final boolean oldCull;
    private final int srcRgb;
    private final int dstRgb;
    private final int srcAlpha;
    private final int dstAlpha;
    private final Quaternionf oldCamera;

    PreviewRenderState(EntityRenderDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        this.oldInverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
        this.oldSorting = RenderSystem.getVertexSorting();
        this.oldShader = RenderSystem.getShader();
        this.oldColor = RenderSystem.getShaderColor().clone();
        this.oldFogStart = RenderSystem.getShaderFogStart();
        this.oldFogEnd = RenderSystem.getShaderFogEnd();
        this.oldTextures = new int[]{RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2)};
        this.viewport = new int[4];
        this.scissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        this.oldScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        this.oldDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        this.oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        this.oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        this.oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        this.oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        this.srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        this.dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        this.srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        this.dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        this.oldCamera = new Quaternionf(dispatcher.cameraOrientation());
        modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
    }

    boolean scissorEnabled() { return oldScissor; }
    int scissor(int index) { return scissor[index]; }

    @Override
    public void close() {
        dispatcher.overrideCameraOrientation(oldCamera);
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
        RenderSystem.setInverseViewRotationMatrix(oldInverseView);
        RenderSystem.setShader(() -> oldShader);
        RenderSystem.setShaderFogStart(oldFogStart);
        RenderSystem.setShaderFogEnd(oldFogEnd);
        for (int i = 0; i < oldTextures.length; i++) RenderSystem.setShaderTexture(i, oldTextures[i]);
        RenderSystem.setShaderColor(oldColor[0], oldColor[1], oldColor[2], oldColor[3]);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        if (oldScissor) RenderSystem.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
        else RenderSystem.disableScissor();
        RenderSystem.depthMask(oldDepthMask);
        RenderSystem.depthFunc(oldDepthFunc);
        if (oldDepth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        if (oldBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        if (oldCull) RenderSystem.enableCull(); else RenderSystem.disableCull();
    }
}

package com.siirio.jemvisualsandbox.client.races;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** A perspective skin stage. Only its camera moves; the world player stays put. */
final class RaceStageRenderer {
    private RaceStageRenderer() {}

    static void render(GuiGraphics graphics, int screenWidth, int screenHeight, int stageWidth,
                       int accent, float entry, float reveal, float switchProgress, float turnDirection) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || reveal >= 1) return;
        graphics.flush();

        float expand = smooth(entry / .24F);
        float orbit = smooth(entry / .62F);
        float enter = smooth((entry - .62F) / .32F);
        double scaleX = client.getWindow().getWidth() / (double) screenWidth;
        double scaleY = client.getWindow().getHeight() / (double) screenHeight;
        int top = (int) Mth.lerp(expand, 48, 0);
        int width = Math.max(1, (int) Math.round(Mth.lerp(expand, stageWidth, screenWidth) * scaleX));
        int bottom = (int) Mth.lerp(expand, Math.max(72, screenHeight - 87), screenHeight);
        int height = Math.max(1, (int) Math.round((bottom - top) * scaleY));
        int y = (int) Math.round((screenHeight - bottom) * scaleY);

        float yaw = (1 - switchProgress) * turnDirection * .55F + orbit * Mth.PI;
        float distance = Mth.lerp(enter, 5.0F, .008F);
        float targetY = Mth.lerp(enter, 1.8F * .51F, 1.62F);
        float rearHeight=Mth.lerp(orbit,1.8F*.58F,1.62F+.18F);
        float cameraY = Mth.lerp(enter, rearHeight, 1.62F);
        Matrix4f view = new Matrix4f().lookAt(Mth.sin(yaw) * distance, cameraY,
                Mth.cos(yaw) * distance, 0, targetY, 0, 0, 1, 0);
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(34), width / (float) height, .004F, 64);

        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix3f oldInverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
        var oldSorting = RenderSystem.getVertexSorting();
        var oldShader = RenderSystem.getShader();
        float[] oldColor = RenderSystem.getShaderColor().clone();
        float oldFogStart = RenderSystem.getShaderFogStart(), oldFogEnd = RenderSystem.getShaderFogEnd();
        int[] oldTextures = {RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2)};
        int[] viewport = new int[4], scissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        boolean oldScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean oldDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        var dispatcher = client.getEntityRenderDispatcher();
        Quaternionf oldCamera = new Quaternionf(dispatcher.cameraOrientation());
        float body = player.yBodyRot, oldBody = player.yBodyRotO;
        float head = player.yHeadRot, oldHead = player.yHeadRotO;
        float playerYaw = player.getYRot(), oldYaw = player.yRotO;
        float pitch = player.getXRot(), oldPitch = player.xRotO;
        var modelView = RenderSystem.getModelViewStack();
        var buffers = client.renderBuffers().bufferSource();
        modelView.pushPose();
        try {
            RenderSystem.viewport(0, y, width, height);
            RenderSystem.enableScissor(0, y, width, height);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setInverseViewRotationMatrix(new Matrix3f(view).transpose());
            RenderSystem.setShaderFogStart(64);
            RenderSystem.setShaderFogEnd(128);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            dispatcher.overrideCameraOrientation(new Quaternionf().setFromNormalized(new Matrix3f(view)).conjugate());
            player.yBodyRot = player.yBodyRotO = 0;
            player.yHeadRot = player.yHeadRotO = 0;
            player.setYRot(0); player.yRotO = 0;
            player.setXRot(0); player.xRotO = 0;

            PoseStack poses = new PoseStack();
            poses.last().pose().set(view);
            poses.last().normal().set(new Matrix3f(view));
            poses.translate(0, Math.sin(System.nanoTime() / 1_000_000_000.0 * 1.7) * .009 * (1 - enter), 0);
            // Resolve through the dispatcher, then render only the live entity.
            // Skipping dispatcher.render avoids world shadows, fire and hitboxes
            // without changing any of the dispatcher's corresponding flags.
            RenderSystem.runAsFancy(() -> dispatcher.getRenderer(player).render(player, 0, 0, poses, buffers, 15728880));
            buffers.endBatch();
        } finally {
            player.yBodyRot = body; player.yBodyRotO = oldBody;
            player.yHeadRot = head; player.yHeadRotO = oldHead;
            player.setYRot(playerYaw); player.yRotO = oldYaw;
            player.setXRot(pitch); player.xRotO = oldPitch;
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

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}

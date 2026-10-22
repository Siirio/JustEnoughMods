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

/** A perspective skin stage. Only its camera moves; the world player stays put. */
final class RaceStageRenderer {
    private net.minecraft.client.multiplayer.ClientLevel sourceLevel;
    private net.minecraft.client.player.RemotePlayer preview;
    private net.minecraft.client.player.LocalPlayer sourcePlayer;

    void dispose() { sourceLevel = null; sourcePlayer = null; preview = null; }

    private net.minecraft.client.player.RemotePlayer prepare(Minecraft client) {
        if (client.level == null || client.player == null) { dispose(); return null; }
        var source = client.player;
        if (sourceLevel != client.level || sourcePlayer != source || preview == null) {
            sourceLevel = client.level;
            sourcePlayer = source;
            preview = new net.minecraft.client.player.RemotePlayer(sourceLevel, source.getGameProfile()) {
                @Override public net.minecraft.resources.ResourceLocation getSkinTextureLocation() {
                    return source.getSkinTextureLocation();
                }
                @Override public String getModelName() { return source.getModelName(); }
                @Override public boolean isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart part) {
                    return source.isModelPartShown(part);
                }
            };
        }
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
            preview.setItemSlot(slot, source.getItemBySlot(slot).copy());
        return preview;
    }

    void render(GuiGraphics graphics, int screenWidth, int screenHeight, int stageWidth,
                       int accent, float entry, float reveal, float switchProgress, float turnDirection) {
        var client = Minecraft.getInstance();
        var player = prepare(client);
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

        var dispatcher = client.getEntityRenderDispatcher();
        float body = player.yBodyRot, oldBody = player.yBodyRotO;
        float head = player.yHeadRot, oldHead = player.yHeadRotO;
        float playerYaw = player.getYRot(), oldYaw = player.yRotO;
        float pitch = player.getXRot(), oldPitch = player.xRotO;
        var modelView = RenderSystem.getModelViewStack();
        var buffers = client.renderBuffers().bufferSource();
        try (var state = new PreviewRenderState(dispatcher)) {
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
                // Use a detached native player so camera-player animation hooks cannot hide the preview.
                // Skipping dispatcher.render avoids world shadows, fire and hitboxes
                // without changing any of the dispatcher's corresponding flags.
                RenderSystem.runAsFancy(() -> dispatcher.getRenderer(player).render(player, 0, 0, poses, buffers, 15728880));
                buffers.endBatch();
            } finally {
                player.yBodyRot = body; player.yBodyRotO = oldBody;
                player.yHeadRot = head; player.yHeadRotO = oldHead;
                player.setYRot(playerYaw); player.yRotO = oldYaw;
                player.setXRot(pitch); player.xRotO = oldPitch;
            }
        }
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}

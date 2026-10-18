package com.siirio.jemmenus.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Skin-only vanilla geometry. No world entity, animation setup, renderer layers or model reloads. */
public final class VanillaGuiPlayers {
    private static final String[] PARTS = {"head", "hat", "body", "jacket", "right_arm", "left_arm",
            "right_sleeve", "left_sleeve", "right_leg", "left_leg", "right_pants", "left_pants"};
    private static final ModelPart CLASSIC = mesh(false), SLIM = mesh(true);
    private VanillaGuiPlayers() { }
    private static ModelPart mesh(boolean slim) {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
    }
    public static void render(AbstractClientPlayer player, float yaw, PoseStack poses, MultiBufferSource buffers, int light) {
        // InventoryScreen sets the body's GUI orientation; the dispatcher yaw argument is zero there.
        render(player.getSkinTextureLocation(), "slim".equals(player.getModelName()), player.yBodyRot, poses, buffers, light);
    }
    public static void render(ResourceLocation skin, boolean slim, float yaw, PoseStack poses, MultiBufferSource buffers, int light) {
        ModelPart root = slim ? SLIM : CLASSIC;
        root.getAllParts().forEach(ModelPart::resetPose);
        poses.pushPose();
        try {
            poses.mulPose(Axis.YP.rotationDegrees(180 - yaw));
            poses.scale(-1, -1, 1);
            poses.translate(0, -1.501, 0);
            var vertices = buffers.getBuffer(RenderType.entityTranslucent(skin));
            for (String part : PARTS) root.getChild(part).render(poses, vertices, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        } finally { poses.popPose(); }
    }
    public static void renderQuickSkin(GuiGraphics graphics, int x, int y, float scale, float rotation,
                                       ResourceLocation skin, boolean slim) {
        if (skin == null) return;
        graphics.flush();
        var poses = graphics.pose();
        poses.pushPose();
        try {
            Lighting.setupForEntityInInventory();
            poses.translate(x, y, 50);
            poses.scale(scale, scale, -scale);
            poses.mulPose(Axis.ZP.rotationDegrees(180));
            var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            render(skin, slim, 180 + rotation, poses, buffers, 15728880);
            buffers.endBatch();
        } finally { poses.popPose(); Lighting.setupForFlatItems(); }
    }
}

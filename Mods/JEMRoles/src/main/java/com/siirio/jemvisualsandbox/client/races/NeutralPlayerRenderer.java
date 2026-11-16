package com.siirio.jemvisualsandbox.client.races;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;

/**
 * Vanilla player geometry for detached GUI stages.
 *
 * <p>The renderer does not enter the world entity-dispatch path. EMF, Epic Fight and
 * camera-facing render hooks therefore cannot replace, cull or retain state for this model.</p>
 */
final class NeutralPlayerRenderer {
    private static final ModelPart CLASSIC_ROOT = mesh(false);
    private static final ModelPart SLIM_ROOT = mesh(true);
    private static final PlayerModel<AbstractClientPlayer> CLASSIC = new PlayerModel<>(CLASSIC_ROOT, false);
    private static final PlayerModel<AbstractClientPlayer> SLIM = new PlayerModel<>(SLIM_ROOT, true);
    private static final CapeLayer CLASSIC_CAPE = new CapeLayer(parent(CLASSIC));
    private static final CapeLayer SLIM_CAPE = new CapeLayer(parent(SLIM));

    private static net.minecraft.client.resources.model.ModelManager armorManager;
    private static HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, HumanoidModel<AbstractClientPlayer>> classicArmor;
    private static HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, HumanoidModel<AbstractClientPlayer>> slimArmor;

    private NeutralPlayerRenderer() {
    }

    static void render(AbstractClientPlayer player, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        boolean slim = "slim".equals(player.getModelName());
        PlayerModel<AbstractClientPlayer> model = slim ? SLIM : CLASSIC;
        ModelPart root = slim ? SLIM_ROOT : CLASSIC_ROOT;
        root.getAllParts().forEach(ModelPart::resetPose);

        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        float walkSpeed = Mth.clamp(player.walkAnimation.speed(partialTick), -1, 1);
        float walkPosition = player.walkAnimation.position(partialTick);
        float age = player.tickCount + partialTick;

        model.attackTime = player.getAttackAnim(partialTick);
        model.riding = player.isPassenger();
        model.young = player.isBaby();
        model.crouching = player.isCrouching();
        model.swimAmount = player.getSwimAmount(partialTick);
        model.prepareMobModel(player, walkPosition, walkSpeed, partialTick);
        model.setupAnim(player, walkPosition, walkSpeed, age, Mth.wrapDegrees(headYaw - bodyYaw), pitch);

        var manager = Minecraft.getInstance().getModelManager();
        if (armorManager != manager) {
            classicArmor = armor(CLASSIC, manager);
            slimArmor = armor(SLIM, manager);
            armorManager = manager;
        }

        poses.pushPose();
        try {
            poses.mulPose(Axis.YP.rotationDegrees(180 - bodyYaw));
            poses.scale(-1, -1, 1);
            poses.translate(0, -1.501, 0);
            model.renderToBuffer(poses, buffers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation())),
                    light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            (slim ? slimArmor : classicArmor).render(poses, buffers, light, player,
                    walkPosition, walkSpeed, partialTick, age, Mth.wrapDegrees(headYaw - bodyYaw), pitch);
            if (player.isModelPartShown(PlayerModelPart.CAPE)) {
                (slim ? SLIM_CAPE : CLASSIC_CAPE).render(poses, buffers, light, player,
                        walkPosition, walkSpeed, partialTick, age, Mth.wrapDegrees(headYaw - bodyYaw), pitch);
            }
        } finally {
            poses.popPose();
        }
    }

    private static ModelPart mesh(boolean slim) {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
    }

    private static RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent(
            PlayerModel<AbstractClientPlayer> model) {
        return new RenderLayerParent<>() {
            @Override
            public PlayerModel<AbstractClientPlayer> getModel() {
                return model;
            }

            @Override
            public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
                return player.getSkinTextureLocation();
            }
        };
    }

    private static HumanoidArmorLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>, HumanoidModel<AbstractClientPlayer>> armor(
            PlayerModel<AbstractClientPlayer> model,
            net.minecraft.client.resources.model.ModelManager manager) {
        var inner = new HumanoidModel<AbstractClientPlayer>(LayerDefinition.create(
                HumanoidModel.createMesh(new CubeDeformation(.5F), 0), 64, 32).bakeRoot());
        var outer = new HumanoidModel<AbstractClientPlayer>(LayerDefinition.create(
                HumanoidModel.createMesh(new CubeDeformation(1F), 0), 64, 32).bakeRoot());
        return new HumanoidArmorLayer<>(parent(model), inner, outer, manager);
    }
}

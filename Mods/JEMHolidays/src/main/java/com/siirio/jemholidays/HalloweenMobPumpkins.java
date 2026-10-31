package com.siirio.jemholidays;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

import java.time.LocalDate;

final class HalloweenMobPumpkins {
    private static final float MIN_SCALE = 0.22F;
    private static final float HEAD_COVERAGE_SCALE = 25.0F / 24.0F;
    private static final float BABY_HEAD_SCALE = 1.5F;
    private static final float HEIGHT_LIMIT_SCALE = 0.48F;

    private HalloweenMobPumpkins() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.addListener(HalloweenMobPumpkins::onLivingRendered);
    }

    private static void onLivingRendered(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !isDecoratedHumanoid(event, mob)
                || mob.isInvisible()
                || !SeasonCalendar.isHalloween(LocalDate.now())) {
            return;
        }

        float scale = pumpkinScale(mob);
        float headCenterY = Math.min(
                mob.getBbHeight() - scale * 0.36F,
                mob.getEyeHeight() + scale * 0.08F);
        float headYaw = mob.getViewYRot(event.getPartialTick());

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        poseStack.pushPose();
        poseStack.translate(0.0D, headCenterY, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - headYaw));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.JACK_O_LANTERN.defaultBlockState(),
                poseStack,
                buffers,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static boolean isDecoratedHumanoid(RenderLivingEvent.Post<?, ?> event, Mob mob) {
        return event.getRenderer().getModel() instanceof HumanoidModel<?>
                || mob instanceof AbstractVillager
                || mob instanceof AbstractIllager
                || mob instanceof Witch;
    }

    private static float pumpkinScale(Mob mob) {
        float modelHeadScale = mob.isBaby() ? BABY_HEAD_SCALE : 1.0F;
        float widthScale = mob.getBbWidth() * HEAD_COVERAGE_SCALE * modelHeadScale;
        float heightLimit = mob.getBbHeight() * HEIGHT_LIMIT_SCALE;
        return Math.max(MIN_SCALE, Math.min(widthScale, heightLimit));
    }
}

package com.siirio.jembuilding.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.item.BlueprintRollItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT)
public final class BlueprintSelectionPreview {
    private static Selection selection() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !(client.player.getMainHandItem().getItem() instanceof BlueprintRollItem)) return null;
        BlockPos first = BlueprintRollItem.first(client.player.getMainHandItem()).orElse(BlueprintInput.pendingFirst());
        if (first == null || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return new Selection(first, hit.getBlockPos());
    }

    @SubscribeEvent
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Selection value = selection();
        if (value == null) return;
        Minecraft client = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        AABB bounds = value.bounds().move(-camera.x, -camera.y, -camera.z);
        PoseStack poses = event.getPoseStack();
        var buffer = client.renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(poses, buffer.getBuffer(RenderType.lines()), bounds, 0.22F, 0.72F, 1.0F, 0.85F);
        AABB first = new AABB(value.first()).inflate(0.04D).move(-camera.x, -camera.y, -camera.z);
        AABB second = new AABB(value.second()).inflate(0.04D).move(-camera.x, -camera.y, -camera.z);
        LevelRenderer.renderLineBox(poses, buffer.getBuffer(RenderType.lines()), first, 0.3F, 1.0F, 0.55F, 1.0F);
        LevelRenderer.renderLineBox(poses, buffer.getBuffer(RenderType.lines()), second, 1.0F, 0.75F, 0.25F, 1.0F);
        buffer.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        Selection value = selection();
        if (value == null) return;
        Minecraft client = Minecraft.getInstance();
        int width = Math.abs(value.first().getX() - value.second().getX()) + 1;
        int height = Math.abs(value.first().getY() - value.second().getY()) + 1;
        int depth = Math.abs(value.first().getZ() - value.second().getZ()) + 1;
        long total = (long) width * height * depth;
        Component text = Component.translatable("hud.jem_building.blueprint.selection", width, height, depth, total);
        event.getGuiGraphics().drawCenteredString(client.font, text, event.getWindow().getGuiScaledWidth() / 2,
                event.getWindow().getGuiScaledHeight() / 2 + 24, 0x8FD8FF);
    }

    private record Selection(BlockPos first, BlockPos second) {
        AABB bounds() {
            return new AABB(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()),
                    Math.max(first.getX(), second.getX()) + 1D, Math.max(first.getY(), second.getY()) + 1D, Math.max(first.getZ(), second.getZ()) + 1D);
        }
    }

    private BlueprintSelectionPreview() { }
}

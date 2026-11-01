package com.siirio.jembuilding.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.construction.ConstructionPlane;
import com.siirio.jembuilding.item.BuilderHammerItem;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT)
public final class ConstructionPreview {
    private static ConstructionPlane lastPlane = ConstructionPlane.XY;

    public record Selection(BlockPos start, BlockPos end, ConstructionPlane plane) { }

    public static Optional<Selection> current() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !(client.player.getMainHandItem().getItem() instanceof BuilderHammerItem)) return Optional.empty();
        Optional<BlockPos> selected = BuilderHammerItem.start(client.player.getMainHandItem());
        if (selected.isEmpty()) return Optional.empty();
        BlockPos start = selected.get();
        ConstructionPlane plane = choosePlane(start, client.player.getEyePosition(), client.player.getLookAngle());
        BlockPos end = endpoint(start, plane, client.player.getEyePosition(), client.player.getLookAngle());
        return Optional.of(new Selection(start, plane.constrain(start, end), plane));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Optional<Selection> value = current();
        if (value.isEmpty()) return;
        Selection selection = value.get();
        Minecraft client = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        int radius = Math.max(32, client.options.getEffectiveRenderDistance() * 16 + 16);
        int minX = Math.max(Math.min(selection.start().getX(), selection.end().getX()), Mth.floor(camera.x) - radius);
        int maxX = Math.min(Math.max(selection.start().getX(), selection.end().getX()), Mth.floor(camera.x) + radius);
        int minY = Math.max(Math.min(selection.start().getY(), selection.end().getY()), client.level.getMinBuildHeight());
        int maxY = Math.min(Math.max(selection.start().getY(), selection.end().getY()), client.level.getMaxBuildHeight() - 1);
        int minZ = Math.max(Math.min(selection.start().getZ(), selection.end().getZ()), Mth.floor(camera.z) - radius);
        int maxZ = Math.min(Math.max(selection.start().getZ(), selection.end().getZ()), Mth.floor(camera.z) + radius);
        if (minX > maxX || minY > maxY || minZ > maxZ) return;
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        try {
            poses.translate(-camera.x, -camera.y, -camera.z);
            var buffers = client.renderBuffers().bufferSource();
            RenderType type = RenderType.lines();
            VertexConsumer vertices = buffers.getBuffer(type);
            drawGrid(vertices, poses, selection.plane(), minX, minY, minZ, maxX, maxY, maxZ);
            buffers.endBatch(type);
        } finally { poses.popPose(); }
    }

    private static ConstructionPlane choosePlane(BlockPos start, Vec3 eye, Vec3 look) {
        if (Math.abs(look.y) > 0.62D) { lastPlane = ConstructionPlane.XZ; return lastPlane; }
        double x = Math.abs(eye.x - start.getX() - 0.5D);
        double z = Math.abs(eye.z - start.getZ() - 0.5D);
        double hysteresis = 0.75D;
        if (lastPlane == ConstructionPlane.ZY && x + hysteresis >= z) return lastPlane;
        if (lastPlane == ConstructionPlane.XY && z + hysteresis >= x) return lastPlane;
        lastPlane = x > z ? ConstructionPlane.ZY : ConstructionPlane.XY;
        return lastPlane;
    }

    private static BlockPos endpoint(BlockPos start, ConstructionPlane plane, Vec3 eye, Vec3 look) {
        Minecraft client = Minecraft.getInstance();
        double coordinate = switch (plane) { case XY -> start.getZ() + 0.5D; case ZY -> start.getX() + 0.5D; case XZ -> start.getY() + 0.5D; };
        double origin = switch (plane) { case XY -> eye.z; case ZY -> eye.x; case XZ -> eye.y; };
        double direction = switch (plane) { case XY -> look.z; case ZY -> look.x; case XZ -> look.y; };
        double borderDistance = Math.min(60_000_000D, Math.max(256D, client.level.getWorldBorder().getSize()));
        double travel = Math.abs(direction) < 1.0E-5D ? borderDistance : (coordinate - origin) / direction;
        if (!Double.isFinite(travel) || travel <= 0D) travel = borderDistance;
        travel = Math.min(travel, borderDistance);
        Vec3 point = eye.add(look.scale(travel));
        int minX = Mth.floor(client.level.getWorldBorder().getMinX());
        int maxX = Mth.floor(client.level.getWorldBorder().getMaxX() - 1D);
        int minZ = Mth.floor(client.level.getWorldBorder().getMinZ());
        int maxZ = Mth.floor(client.level.getWorldBorder().getMaxZ() - 1D);
        return new BlockPos(Mth.clamp(Mth.floor(point.x), minX, maxX),
                Mth.clamp(Mth.floor(point.y), client.level.getMinBuildHeight(), client.level.getMaxBuildHeight() - 1),
                Mth.clamp(Mth.floor(point.z), minZ, maxZ));
    }

    private static void drawGrid(VertexConsumer out, PoseStack poses, ConstructionPlane plane, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        float r = 0.91F, g = 0.55F, b = 0.22F, a = 0.9F;
        switch (plane) {
            case XY -> {
                double z = minZ + 0.002D;
                for (int x = minX; x <= maxX + 1; x++) line(out, poses, new Vec3(x, minY, z), new Vec3(x, maxY + 1D, z), r, g, b, a);
                for (int y = minY; y <= maxY + 1; y++) line(out, poses, new Vec3(minX, y, z), new Vec3(maxX + 1D, y, z), r, g, b, a);
            }
            case ZY -> {
                double x = minX + 0.002D;
                for (int z = minZ; z <= maxZ + 1; z++) line(out, poses, new Vec3(x, minY, z), new Vec3(x, maxY + 1D, z), r, g, b, a);
                for (int y = minY; y <= maxY + 1; y++) line(out, poses, new Vec3(x, y, minZ), new Vec3(x, y, maxZ + 1D), r, g, b, a);
            }
            case XZ -> {
                double y = minY + 0.002D;
                for (int x = minX; x <= maxX + 1; x++) line(out, poses, new Vec3(x, y, minZ), new Vec3(x, y, maxZ + 1D), r, g, b, a);
                for (int z = minZ; z <= maxZ + 1; z++) line(out, poses, new Vec3(minX, y, z), new Vec3(maxX + 1D, y, z), r, g, b, a);
            }
        }
    }

    private static void line(VertexConsumer out, PoseStack poses, Vec3 from, Vec3 to, float r, float g, float b, float a) {
        Vec3 direction = to.subtract(from).normalize();
        if (direction.lengthSqr() < 0.0001D) return;
        var pose = poses.last();
        out.vertex(pose.pose(), (float) from.x, (float) from.y, (float) from.z).color(r, g, b, a).normal(pose.normal(), (float) direction.x, (float) direction.y, (float) direction.z).endVertex();
        out.vertex(pose.pose(), (float) to.x, (float) to.y, (float) to.z).color(r, g, b, a).normal(pose.normal(), (float) direction.x, (float) direction.y, (float) direction.z).endVertex();
    }

    private ConstructionPreview() { }
}

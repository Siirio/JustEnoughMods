package com.siirio.jembuilding.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.construction.ConstructionGeometry;
import com.siirio.jembuilding.construction.ConstructionPlane;
import com.siirio.jembuilding.construction.ConstructionShape;
import com.siirio.jembuilding.item.BuilderHammerItem;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT)
public final class ConstructionPreview {
    private static final int MAX_SPHERE_PREVIEW_CELLS = 12_000;
    private static BlockPos cachedSphereCenter;
    private static int cachedSphereRadius = -1;
    private static List<BlockPos> cachedSphereCells = List.of();

    public record Selection(BlockPos start, BlockPos end, ConstructionShape shape, ConstructionPlane plane) { }

    public static Optional<Selection> current() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !(client.player.getMainHandItem().getItem() instanceof BuilderHammerItem)) return Optional.empty();
        Optional<BlockPos> selected = BuilderHammerItem.start(client.player.getMainHandItem());
        if (selected.isEmpty()) return Optional.empty();
        BlockPos start = selected.get();
        ConstructionShape shape = BuilderHammerItem.shape(client.player.getMainHandItem()).orElse(ConstructionShape.WALL);
        ConstructionPlane plane = BuilderHammerItem.plane(client.player.getMainHandItem()).orElse(ConstructionPlane.XY);
        BlockPos requested = target(start, shape, plane, client.player.getEyePosition(), client.player.getLookAngle());
        return Optional.of(new Selection(start, ConstructionGeometry.constrain(shape, plane, start, requested), shape, plane));
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
        int sphereRadius = selection.shape() == ConstructionShape.SPHERE
                ? ConstructionGeometry.sphereRadius(selection.start(), selection.end()) : 0;
        int shapeMinX = selection.shape() == ConstructionShape.SPHERE ? selection.start().getX() - sphereRadius : Math.min(selection.start().getX(), selection.end().getX());
        int shapeMaxX = selection.shape() == ConstructionShape.SPHERE ? selection.start().getX() + sphereRadius : Math.max(selection.start().getX(), selection.end().getX());
        int shapeMinY = selection.shape() == ConstructionShape.SPHERE ? selection.start().getY() - sphereRadius : Math.min(selection.start().getY(), selection.end().getY());
        int shapeMaxY = selection.shape() == ConstructionShape.SPHERE ? selection.start().getY() + sphereRadius : Math.max(selection.start().getY(), selection.end().getY());
        int shapeMinZ = selection.shape() == ConstructionShape.SPHERE ? selection.start().getZ() - sphereRadius : Math.min(selection.start().getZ(), selection.end().getZ());
        int shapeMaxZ = selection.shape() == ConstructionShape.SPHERE ? selection.start().getZ() + sphereRadius : Math.max(selection.start().getZ(), selection.end().getZ());
        int minX = Math.max(shapeMinX, Mth.floor(camera.x) - radius);
        int maxX = Math.min(shapeMaxX, Mth.floor(camera.x) + radius);
        int minY = Math.max(shapeMinY, client.level.getMinBuildHeight());
        int maxY = Math.min(shapeMaxY, client.level.getMaxBuildHeight() - 1);
        int minZ = Math.max(shapeMinZ, Mth.floor(camera.z) - radius);
        int maxZ = Math.min(shapeMaxZ, Mth.floor(camera.z) + radius);
        if (minX > maxX || minY > maxY || minZ > maxZ) return;
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        try {
            poses.translate(-camera.x, -camera.y, -camera.z);
            var buffers = client.renderBuffers().bufferSource();
            RenderType type = RenderType.lines();
            VertexConsumer vertices = buffers.getBuffer(type);
            switch (selection.shape()) {
                case WALL, FLOOR -> drawGrid(vertices, poses, selection.plane(), minX, minY, minZ, maxX, maxY, maxZ);
                case COLUMN -> drawBox(vertices, poses, minX, minY, minZ, maxX + 1D, maxY + 1D, maxZ + 1D);
                case SPHERE -> drawSphereBlocks(vertices, poses, selection.start(), sphereRadius,
                        minX, minY, minZ, maxX, maxY, maxZ);
            }
            buffers.endBatch(type);
        } finally { poses.popPose(); }
    }

    private static BlockPos target(BlockPos start, ConstructionShape shape, ConstructionPlane plane, Vec3 eye, Vec3 look) {
        Minecraft client = Minecraft.getInstance();
        if (client.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            return block.getBlockPos().relative(block.getDirection());
        }
        if (shape == ConstructionShape.COLUMN) {
            double horizontal = look.x * look.x + look.z * look.z;
            double travel;
            if (horizontal < 1.0E-5D) {
                travel = ConstructionGeometry.MAX_SPAN - 1D;
            } else {
                double targetX = start.getX() + 0.5D - eye.x;
                double targetZ = start.getZ() + 0.5D - eye.z;
                travel = (targetX * look.x + targetZ * look.z) / horizontal;
                if (!Double.isFinite(travel) || travel <= 0D) {
                    travel = client.hitResult == null ? 0D : eye.distanceTo(client.hitResult.getLocation());
                }
                travel = Math.min(travel, ConstructionGeometry.MAX_SPAN * 2D);
            }
            return new BlockPos(start.getX(), Mth.floor(eye.y + look.y * travel), start.getZ());
        }
        if (shape == ConstructionShape.SPHERE && client.hitResult != null) {
            return BlockPos.containing(client.hitResult.getLocation());
        }
        double coordinate = switch (plane) { case XY -> start.getZ() + 0.5D; case ZY -> start.getX() + 0.5D; case XZ -> start.getY() + 0.5D; };
        double origin = switch (plane) { case XY -> eye.z; case ZY -> eye.x; case XZ -> eye.y; };
        double direction = switch (plane) { case XY -> look.z; case ZY -> look.x; case XZ -> look.y; };
        double travel = Math.abs(direction) < 1.0E-5D ? ConstructionGeometry.MAX_SPAN - 1D : (coordinate - origin) / direction;
        if (!Double.isFinite(travel) || travel <= 0D) travel = ConstructionGeometry.MAX_SPAN - 1D;
        travel = Math.min(travel, ConstructionGeometry.MAX_SPAN - 1D);
        Vec3 point = eye.add(look.scale(travel));
        int minX = Mth.floor(client.level.getWorldBorder().getMinX());
        int maxX = Mth.floor(client.level.getWorldBorder().getMaxX() - 1D);
        int minZ = Mth.floor(client.level.getWorldBorder().getMinZ());
        int maxZ = Mth.floor(client.level.getWorldBorder().getMaxZ() - 1D);
        return new BlockPos(Mth.clamp(Mth.floor(point.x), minX, maxX),
                Mth.clamp(Mth.floor(point.y), client.level.getMinBuildHeight(), client.level.getMaxBuildHeight() - 1),
                Mth.clamp(Mth.floor(point.z), minZ, maxZ));
    }

    private static void drawBox(VertexConsumer out, PoseStack poses, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        float r = 0.91F, g = 0.55F, b = 0.22F, a = 0.9F;
        for (double y : new double[]{minY, maxY}) for (double z : new double[]{minZ, maxZ})
            line(out, poses, new Vec3(minX, y, z), new Vec3(maxX, y, z), r, g, b, a);
        for (double x : new double[]{minX, maxX}) for (double z : new double[]{minZ, maxZ})
            line(out, poses, new Vec3(x, minY, z), new Vec3(x, maxY, z), r, g, b, a);
        for (double x : new double[]{minX, maxX}) for (double y : new double[]{minY, maxY})
            line(out, poses, new Vec3(x, y, minZ), new Vec3(x, y, maxZ), r, g, b, a);
    }

    private static void drawSphereBlocks(VertexConsumer out, PoseStack poses, BlockPos center, int radius,
                                         int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!center.equals(cachedSphereCenter) || radius != cachedSphereRadius) {
            cachedSphereCenter = center.immutable();
            cachedSphereRadius = radius;
            cachedSphereCells = ConstructionGeometry.spherePositions(center, radius);
        }
        int stride = Math.max(1, (cachedSphereCells.size() + MAX_SPHERE_PREVIEW_CELLS - 1) / MAX_SPHERE_PREVIEW_CELLS);
        for (int index = 0; index < cachedSphereCells.size(); index += stride) {
            BlockPos pos = cachedSphereCells.get(index);
            if (pos.getX() < minX || pos.getX() > maxX || pos.getY() < minY || pos.getY() > maxY
                    || pos.getZ() < minZ || pos.getZ() > maxZ) continue;
            drawBox(out, poses, pos.getX() + 0.002D, pos.getY() + 0.002D, pos.getZ() + 0.002D,
                    pos.getX() + 0.998D, pos.getY() + 0.998D, pos.getZ() + 0.998D);
        }
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

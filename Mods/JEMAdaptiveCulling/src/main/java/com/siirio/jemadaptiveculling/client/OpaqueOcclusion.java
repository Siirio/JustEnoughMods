package com.siirio.jemadaptiveculling.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OpaqueOcclusion {
    private OpaqueOcclusion() {
    }

    public static boolean blocked(BlockGetter level, Vec3 camera, Vec3 target) {
        int targetBlockX = Mth.floor(target.x);
        int targetBlockY = Mth.floor(target.y);
        int targetBlockZ = Mth.floor(target.z);
        return BlockGetter.traverseBlocks(
                camera,
                target,
                level,
                (blockGetter, position) -> blocksView(blockGetter, position, camera, target, targetBlockX, targetBlockY, targetBlockZ)
                        ? Boolean.TRUE
                        : null,
                ignored -> Boolean.FALSE
        );
    }

    private static boolean blocksView(
            BlockGetter level,
            BlockPos position,
            Vec3 camera,
            Vec3 target,
            int targetBlockX,
            int targetBlockY,
            int targetBlockZ
    ) {
        if (position.getX() == targetBlockX && position.getY() == targetBlockY && position.getZ() == targetBlockZ) {
            return false;
        }
        BlockState state = level.getBlockState(position);
        if (!state.canOcclude()) {
            return false;
        }
        VoxelShape shape = state.getOcclusionShape(level, position);
        return switch (ShapeClassifier.classify(shape)) {
            case EMPTY -> false;
            case FULL -> true;
            case PARTIAL -> {
                BlockHitResult hit = shape.clip(camera, target, position);
                yield hit != null;
            }
        };
    }
}

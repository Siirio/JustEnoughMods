package com.siirio.jembuilding.construction;

import net.minecraft.core.BlockPos;

public enum ConstructionPlane {
    XY,
    ZY,
    XZ;

    public boolean contains(BlockPos start, BlockPos end) {
        return switch (this) {
            case XY -> start.getZ() == end.getZ();
            case ZY -> start.getX() == end.getX();
            case XZ -> start.getY() == end.getY();
        };
    }

    public BlockPos constrain(BlockPos start, BlockPos end) {
        return switch (this) {
            case XY -> new BlockPos(end.getX(), end.getY(), start.getZ());
            case ZY -> new BlockPos(start.getX(), end.getY(), end.getZ());
            case XZ -> new BlockPos(end.getX(), start.getY(), end.getZ());
        };
    }
}

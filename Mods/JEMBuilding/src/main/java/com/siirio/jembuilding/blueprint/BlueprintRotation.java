package com.siirio.jembuilding.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

public enum BlueprintRotation {
    NONE(Rotation.NONE),
    CLOCKWISE_90(Rotation.CLOCKWISE_90),
    CLOCKWISE_180(Rotation.CLOCKWISE_180),
    COUNTERCLOCKWISE_90(Rotation.COUNTERCLOCKWISE_90);

    private final Rotation vanilla;

    BlueprintRotation(Rotation vanilla) {
        this.vanilla = vanilla;
    }

    public BlueprintRotation next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public BlockPos apply(BlockPos relative, int width, int depth) {
        return switch (this) {
            case NONE -> relative;
            case CLOCKWISE_90 -> new BlockPos(depth - 1 - relative.getZ(), relative.getY(), relative.getX());
            case CLOCKWISE_180 -> new BlockPos(width - 1 - relative.getX(), relative.getY(), depth - 1 - relative.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(relative.getZ(), relative.getY(), width - 1 - relative.getX());
        };
    }

    public Rotation vanilla() {
        return vanilla;
    }
}

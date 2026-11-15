package com.siirio.jemlightingsandbox.terrain;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Native block emission, measured at the modeled light source in world space. */
record BlockLight(double x, double y, double z, float red, float green, float blue, int emission, boolean area) {
    private static final int WARM_WHITE = 0xFFE4B8;

    static BlockLight at(ClientLevel level, BlockPos position, BlockState state, int emission) {
        boolean area = state.getRenderShape() == RenderShape.MODEL
                && Block.isShapeFullBlock(state.getShape(level, position));
        double x = position.getX() + 0.5;
        double y = position.getY() + 0.5;
        double z = position.getZ() + 0.5;
        var block = state.getBlock();
        // Match the native flame's mean position and leave particle jitter out of shadow rays.
        if (block instanceof WallTorchBlock || block instanceof RedstoneWallTorchBlock) {
            Direction towardWall = state.getValue(WallTorchBlock.FACING).getOpposite();
            x += towardWall.getStepX() * 0.27;
            y = position.getY() + 0.92;
            z += towardWall.getStepZ() * 0.27;
        } else if (block instanceof LanternBlock) {
            y = position.getY() + (state.getValue(LanternBlock.HANGING) ? 4.5 : 3.5) / 16.0;
        } else if (block instanceof TorchBlock) {
            y = position.getY() + 0.70;
        }
        int color = emissionColor(state, level, position);
        return new BlockLight(x, y, z, linear(color >>> 16), linear(color >>> 8), linear(color), emission, area);
    }

    private static int emissionColor(BlockState state, ClientLevel level, BlockPos pos) {
        if (state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH) || state.is(Blocks.SOUL_LANTERN)
                || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return 0x62D8FF;
        }
        if (state.is(Blocks.REDSTONE_LAMP)) {
            return 0xFFC88B;
        }
        if (state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)) {
            return 0xFF6450;
        }
        if (state.is(Blocks.SEA_LANTERN)) {
            return 0xB9F4EE;
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN)
                || state.is(Blocks.GLOWSTONE) || state.is(BlockTags.CAMPFIRES)) {
            return WARM_WHITE;
        }
        if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.MAGMA_BLOCK)) {
            return 0xFF9762;
        }
        int color = state.getMapColor(level, pos).col;
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        int brightest = Math.max(red, Math.max(green, blue));
        int darkest = Math.min(red, Math.min(green, blue));
        if (brightest < 64 || brightest - darkest < 40) {
            return WARM_WHITE;
        }
        return red * 255 / brightest << 16 | green * 255 / brightest << 8 | blue * 255 / brightest;
    }

    private static float linear(int component) {
        float srgb = (component & 255) / 255.0F;
        return srgb <= 0.04045F ? srgb / 12.92F : (float) Math.pow((srgb + 0.055F) / 1.055F, 2.4);
    }
}

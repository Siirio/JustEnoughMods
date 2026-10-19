package com.siirio.jemcompat.feature.backpack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Retired registry compatibility only: preserve old block geometry without an upgrade interaction. */
public final class AugmentBayBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 7, 0, 16, 16, 16), Block.box(0, 0, 0, 3, 7, 3), Block.box(13, 0, 0, 16, 7, 3),
            Block.box(0, 0, 13, 3, 7, 16), Block.box(13, 0, 13, 16, 7, 16));

    public AugmentBayBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}

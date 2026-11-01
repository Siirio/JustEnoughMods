package com.siirio.jembuilding.construction;

import com.siirio.jembuilding.item.BuilderHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ConstructionPlaceholderBlock extends Block {
    public ConstructionPlaceholderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (ConstructionRuntime.isNativeFill()) return InteractionResult.PASS;
        if (level.isClientSide) {
            return player.getItemInHand(hand).getItem() instanceof BlockItem
                    || player.getItemInHand(hand).getItem() instanceof BuilderHammerItem
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (player.getItemInHand(hand).getItem() instanceof BuilderHammerItem && player.isShiftKeyDown()) {
            ConstructionRuntime.cancel(player, pos);
            return InteractionResult.CONSUME;
        }
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) {
            ConstructionRuntime.fill(player, hand, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1.0F; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.DESTROY; }
}

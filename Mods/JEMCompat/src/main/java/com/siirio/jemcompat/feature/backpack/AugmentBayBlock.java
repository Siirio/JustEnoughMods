package com.siirio.jemcompat.feature.backpack;

import com.siirio.jemcompat.feature.transport.AircraftPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class AugmentBayBlock extends Block {
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0, 7, 0, 16, 16, 16), Block.box(0, 0, 0, 3, 7, 3), Block.box(13, 0, 0, 16, 7, 3),
            Block.box(0, 0, 13, 3, 7, 16), Block.box(13, 0, 13, 16, 7, 16));
    public AugmentBayBlock(Properties properties) { super(properties); }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) { return SHAPE; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!AircraftPolicy.enabled(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer,
                new SimpleMenuProvider((id, inventory, owner) -> new AugmentBayMenu(id, inventory, pos),
                        Component.translatable("block.jemcompat.augment_bay")), pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

package com.siirio.jemeconomy.shipping;

import com.siirio.jemeconomy.registry.EconomyBlockEntities;
import com.siirio.jemidentity.api.IdentityPolicy;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class ShippingBoxBlock extends BaseEntityBlock {
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(new com.siirio.jemeconomy.full.client.ShippingBoxParticles());
    }
    public ShippingBoxBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.HORIZONTAL_FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return ShippingStationShape.get(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return EconomyBlockEntities.SHIPPING_BOX.get().create(pos, state); }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box ? box.inputComparatorSignal() : 0;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box) {
            IdentityPolicy.resolve(player).ifPresent(principal -> box.bindOwner(principal.accountId(), player.getGameProfile().getName()));
        }
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box)) return InteractionResult.FAIL;
        if (!box.acquire(serverPlayer)) { player.displayClientMessage(Component.translatable("jem.economy.shipping.busy"), true); return InteractionResult.CONSUME; }
        NetworkHooks.openScreen(serverPlayer, box, buffer -> buffer.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }
    @Override public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box && !box.isOwner(serverPlayer)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }
    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
                                                  net.minecraft.world.level.material.FluidState fluid) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box && !box.isOwner(player)) {
            player.displayClientMessage(Component.translatable("jem.economy.shipping.owner_only"), true); return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        return entity instanceof ShippingBoxBlockEntity box ? java.util.List.of(box.preservedStack()) : java.util.List.of();
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) { return net.minecraft.world.level.material.PushReaction.BLOCK; }
}

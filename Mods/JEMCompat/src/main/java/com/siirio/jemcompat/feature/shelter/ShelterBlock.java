package com.siirio.jemcompat.feature.shelter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/** Cell index = x + size*z + size*size*y. Local +Z is the open/front face (SOUTH at rotation 0). */
public final class ShelterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, 26);
    private final int size;
    private boolean dismantling;

    ShelterBlock(int size, Properties properties) {
        super(properties); this.size = size;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(CELL, 0));
    }
    public int size() { return size; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, CELL); }
    public BlockPos cell(BlockPos origin, Direction facing, int index) {
        int x = index % size, z = index / size % size, y = index / (size * size);
        Direction right = facing.getCounterClockWise();
        return origin.relative(right, x).relative(facing, z).above(y);
    }
    public BlockPos origin(BlockPos pos, BlockState state) {
        int index = state.getValue(CELL);
        return pos.subtract(cell(BlockPos.ZERO, state.getValue(FACING), index));
    }
    public List<BlockPos> cells(BlockPos origin, Direction facing) {
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < size * size * size; i++) result.add(cell(origin, facing, i));
        return result;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel(); BlockPos origin = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        int cellIndex = 0;
        for (BlockPos pos : cells(origin, facing)) {
            if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()
                    || level.getBlockEntity(pos) != null || context.getPlayer() == null
                    || !context.getPlayer().mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return null;
            if (!level.isUnobstructed(defaultBlockState().setValue(FACING, facing).setValue(CELL, cellIndex++), pos, CollisionContext.empty())) return null;
            if (pos.getY() == origin.getY() && (!level.hasChunkAt(pos.below())
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))) return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // The enclosing native Forge ItemStack placement transaction captures every cell and posts MultiPlaceEvent.
        for (int i = 1; i < size * size * size; i++) level.setBlock(cell(pos, state.getValue(FACING), i), state.setValue(CELL, i), 3);
        if (level.getBlockEntity(pos) instanceof ShelterController home) home.owner(placer.getUUID());
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(CELL) == 0 ? new ShelterController(pos, state) : null;
    }
    public ShelterController controller(Level level, BlockPos pos, BlockState state) {
        BlockPos anchor = origin(pos, state);
        return level.hasChunkAt(anchor) && level.getBlockEntity(anchor) instanceof ShelterController home ? home : null;
    }
    public boolean intact(ServerLevel level, BlockPos anchor, Direction facing) {
        for (int i = 0; i < size * size * size; i++) {
            BlockPos pos = cell(anchor, facing, i);
            if (!level.hasChunkAt(pos)) return false;
            BlockState state = level.getBlockState(pos);
            if (!state.is(this) || state.getValue(CELL) != i || state.getValue(FACING) != facing) return false;
        }
        return true;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ShelterController home = controller(level, pos, state);
            if (home != null && player.getUUID().equals(home.owner())) {
                ShelterEvents.discover(serverPlayer);
                java.util.UUID session = java.util.UUID.randomUUID();
                NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                        (id, inventory, ignored) -> new ShelterMenu(id, inventory, home, session), getName()), buffer -> ShelterMenu.writeOpening(buffer, home, session));
            } else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("jemcompat.shelter.not_owner"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()) popResource(level, pos, new ItemStack(asItem()));
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity entity, ItemStack tool) {
        // The whole structure produces its single item in playerWillDestroy; no per-cell loot.
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this)); player.causeFoodExhaustion(0.005F);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && !dismantling) {
            dismantling = true;
            try {
                BlockPos anchor = origin(pos, state);
                CompanionData.get((ServerLevel) level).removeHome(level.dimension().location().toString(), anchor);
                for (BlockPos cell : cells(anchor, state.getValue(FACING))) if (!cell.equals(pos) && level.hasChunkAt(cell)
                        && level.getBlockState(cell).is(this) && origin(cell, level.getBlockState(cell)).equals(anchor)) level.removeBlock(cell, false);
            } finally { dismantling = false; }
        }
        super.onRemove(state, level, pos, replacement, moved);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return ShelterGeometry.shape(size, state.getValue(CELL), state.getValue(FACING)); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ShelterGeometry.shape(size, state.getValue(CELL), state.getValue(FACING));
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) { return net.minecraft.world.level.material.PushReaction.BLOCK; }
}

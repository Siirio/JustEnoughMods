package com.siirio.jemserver.smp.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class StructureBarrierBlock extends Block {
    private static final float EXPLOSION_RESISTANCE=3_600_000.0F;

    public StructureBarrierBlock() {
        super(BlockBehaviour.Properties.of().strength(-1.0F,EXPLOSION_RESISTANCE).noLootTable().noOcclusion()
                .isValidSpawn((state,level,pos,type)->false).isRedstoneConductor((state,level,pos)->false)
                .isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos)->false));
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(context instanceof EntityCollisionContext entityContext&&passable(entityContext.getEntity(),pos)) return Shapes.empty();
        return Shapes.block();
    }
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos) { return true; }
    @Override public float getShadeBrightness(BlockState state,BlockGetter level,BlockPos pos) { return 1.0F; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public boolean canBeReplaced(BlockState state,BlockPlaceContext context) {
        if(!(context.getItemInHand().getItem() instanceof BlockItem blockItem)) return false;
        BlockState placed=blockItem.getBlock().defaultBlockState();
        return !placed.getCollisionShape(context.getLevel(),context.getClickedPos()).isEmpty();
    }

    private static boolean passable(Entity entity,BlockPos pos) {
        if(entity instanceof ServerPlayer player) return StructureStaging.canPass(player,pos);
        if(entity!=null&&entity.level().isClientSide()) return Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                ()->()->com.siirio.jemserver.client.smp.EventBoundaryRenderer.canPass(pos)));
        return false;
    }
}

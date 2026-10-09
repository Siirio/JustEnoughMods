package com.siirio.jemcompat.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CampaignBarrierBlock extends Block {
    private static final float EXPLOSION_RESISTANCE=3_600_000.0F;

    public CampaignBarrierBlock() {
        super(BlockBehaviour.Properties.of().strength(-1.0F,EXPLOSION_RESISTANCE).noLootTable().noOcclusion()
                .isValidSpawn((state,level,pos,type)->false).isRedstoneConductor((state,level,pos)->false)
                .isSuffocating((state,level,pos)->false).isViewBlocking((state,level,pos)->false));
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos) { return true; }
    @Override public float getShadeBrightness(BlockState state,BlockGetter level,BlockPos pos) { return 1.0F; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
}

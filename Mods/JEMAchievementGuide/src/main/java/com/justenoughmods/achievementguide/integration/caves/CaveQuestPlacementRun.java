package com.justenoughmods.achievementguide.integration.caves;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
public final class CaveQuestPlacementRun {
    public static final class Frame {public final ServerPlayer actor;public final BlockPos pos;public final Block block;public final int count;public BlockState rust;public Frame(ServerPlayer actor,BlockPos pos,Block block,int count){this.actor=actor;this.pos=pos.immutable();this.block=block;this.count=count;}}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();private CaveQuestPlacementRun(){}
    public static void begin(ServerPlayer actor,BlockPos pos,Block block,int count){CURRENT.set(new Frame(actor,pos,block,count));}
    public static Frame current(){return CURRENT.get();}public static Frame end(){var run=CURRENT.get();CURRENT.remove();return run;}
}

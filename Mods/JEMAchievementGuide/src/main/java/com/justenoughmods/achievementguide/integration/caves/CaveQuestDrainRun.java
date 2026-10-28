package com.justenoughmods.achievementguide.integration.caves;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
public final class CaveQuestDrainRun {
    public static final class Run {public final ServerLevel level;public final BlockPos drain;public final ServerPlayer actor;public Fluid fluid;public boolean removed;public Run(ServerLevel level,BlockPos pos,ServerPlayer actor){this.level=level;this.drain=pos;this.actor=actor;}}
    private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();
    private CaveQuestDrainRun(){}
    public static Run current(){return CURRENT.get();}
    public static void begin(Level world,BlockPos pos){
        CURRENT.remove();if(!(world instanceof ServerLevel level))return;
        var origin=CaveQuestBlockOrigins.get(level,pos,"Placed");
        var actor=origin.hasUUID("actor")?CaveQuestProof.actor(level,origin.getUUID("actor")):null;
        if(actor!=null && origin.getString("block").equals("alexscaves:drain")) CURRENT.set(new Run(level,pos.immutable(),actor));
    }
    public static void end(){CURRENT.remove();}
}

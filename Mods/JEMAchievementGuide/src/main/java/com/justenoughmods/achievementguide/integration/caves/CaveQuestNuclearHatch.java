package com.justenoughmods.achievementguide.integration.caves;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
public final class CaveQuestNuclearHatch {
    public record Origin(UUID explosion,BlockPos egg){}
    private static final ThreadLocal<Origin> CURRENT=new ThreadLocal<>();
    private CaveQuestNuclearHatch(){}
    public static Origin current(){return CURRENT.get();}
    public static void observe(Entity explosion,BlockPos pos,Runnable nativeCall){
        Origin previous=CURRENT.get();CURRENT.set(new Origin(explosion.getUUID(),pos.immutable()));
        try{nativeCall.run();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
}

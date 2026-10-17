package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.server.level.ServerPlayer;

/** Compiled only into SANDBOX. Never accepts dedicated-server or guest authority. */
final class RaceDevelopment {
    private RaceDevelopment() {}
    static boolean integrated(){return true;}
    static boolean enabled(ServerPlayer p){return RaceExperience.allowed(p);}
    static boolean cooldownsDisabled(ServerPlayer p){return enabled(p);}
    static boolean unlimitedSelection(ServerPlayer p){return enabled(p);}
    static int tier(ServerPlayer p,int race,int normal){
        var data=RaceExperience.data(p);String key="DevelopmentTier"+race;
        return enabled(p)&&data.contains(key)?RacePowerStages.clamp(data.getInt(key)):normal;
    }
    static int bloomUpgrade(ServerPlayer p,int normal){
        return enabled(p)&&RaceExperience.data(p).contains("DevelopmentBloomUpgrade")?
            Math.max(0,Math.min(3,RaceExperience.data(p).getInt("DevelopmentBloomUpgrade"))):normal;
    }
    static void storeBloomUpgrade(ServerPlayer p,int next){RaceExperience.data(p).putInt("DevelopmentBloomUpgrade",next);}
    static void request(int action,int value){SandboxNetwork.developmentInput(action,value);}
    static void handle(ServerPlayer p,int action,int value){
        if(!enabled(p)||!RaceExperience.selected(p))return;
        if(action==8&&value>=0&&value<=3){
            RaceExperience.selectionChanged(p);
            RaceExperience.data(p).putInt("DevelopmentTier"+RaceExperience.race(p),value);
            if(RaceExperience.race(p)==2)RaceExperience.data(p).putInt("DevelopmentBloomUpgrade",value);
        }else if(action==9){
            RaceExperience.selectionChanged(p);
            int race=RaceExperience.race(p);
            RaceExperience.data(p).putInt("DevelopmentTier"+race,0);
            if(race==2)RaceExperience.data(p).putInt("DevelopmentBloomUpgrade",0);
        }else return;
        RaceExperience.dirty(p);SandboxRaceTraits.refresh(p);RaceExperience.snapshot(p);
    }
}

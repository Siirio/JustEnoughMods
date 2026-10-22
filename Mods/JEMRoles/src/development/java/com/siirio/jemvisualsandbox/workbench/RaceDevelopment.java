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
        return enabled(p) ? com.siirio.jemroles.race.RaceService.record(p)
                .map(record -> RacePowerStages.clamp(record.developmentTier(race,normal))).orElse(normal) : normal;
    }
    static int bloomUpgrade(ServerPlayer p,int normal){
        return enabled(p) ? com.siirio.jemroles.race.RaceService.record(p)
                .map(record -> RacePowerStages.clamp(record.developmentBloomUpgrade(normal))).orElse(normal) : normal;
    }
    static void storeBloomUpgrade(ServerPlayer p,int next){
        com.siirio.jemroles.race.RaceService.mutate(p, record -> record.setDevelopmentBloomUpgrade(next));
    }
    static void request(int action,int value){SandboxNetwork.developmentInput(action,value);}
    static void handle(ServerPlayer p,int action,int value){
        if(!enabled(p)||!RaceExperience.selected(p))return;
        if(action==8&&value>=0&&value<=3){
            RaceExperience.selectionChanged(p);
            com.siirio.jemroles.race.RaceService.mutate(p, record -> record.setDevelopmentTier(RaceExperience.race(p),value));
            if(RaceExperience.race(p)==2)storeBloomUpgrade(p,value);
        }else if(action==9){
            RaceExperience.selectionChanged(p);
            int race=RaceExperience.race(p);
            com.siirio.jemroles.race.RaceService.mutate(p, record -> record.setDevelopmentTier(race,0));
            if(race==2)storeBloomUpgrade(p,0);
        }else return;
        SandboxRaceTraits.refresh(p);RaceExperience.snapshot(p);
    }
}

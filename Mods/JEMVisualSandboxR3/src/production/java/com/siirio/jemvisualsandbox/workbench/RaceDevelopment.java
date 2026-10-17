package com.siirio.jemvisualsandbox.workbench;

import net.minecraft.server.level.ServerPlayer;

/** Sealed PRODUCTION implementation: no cheat packet action, command or state mutation. */
final class RaceDevelopment {
    private RaceDevelopment() {}
    static boolean integrated(){return false;}
    static boolean enabled(ServerPlayer p){return false;}
    static boolean cooldownsDisabled(ServerPlayer p){return false;}
    static boolean unlimitedSelection(ServerPlayer p){return false;}
    static int tier(ServerPlayer p,int race,int normal){return normal;}
    static int bloomUpgrade(ServerPlayer p,int normal){return normal;}
    static void storeBloomUpgrade(ServerPlayer p,int next){RaceExperience.data(p).putInt("BloomUpgrade",next);}
    static void request(int action,int value){}
    static void handle(ServerPlayer p,int action,int value){}
}

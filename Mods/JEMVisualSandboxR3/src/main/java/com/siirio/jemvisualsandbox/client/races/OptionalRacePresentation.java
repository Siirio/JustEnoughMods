package com.siirio.jemvisualsandbox.client.races;

import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.fml.ModList;

/** Client-only optional type boundary. */
public final class OptionalRacePresentation {
    private static Class<?> bridge;
    private static boolean initialized;
    private static int lastRace=-1,lastTier=-1;
    private OptionalRacePresentation() {}
    private static Class<?> bridge(){
        if(!initialized){
            initialized=true;
            if(com.siirio.jemvisualsandbox.workbench.SandboxOptionalIrons.available())try{bridge=Class.forName("com.siirio.jemvisualsandbox.compat.irons.client.SandboxIronsClient");}
            catch(ClassNotFoundException e){throw new IllegalStateException("Missing optional race presentation",e);}
        }
        return bridge;
    }
    private static Object call(String name,Class<?>[] args,Object... values){
        if(bridge()==null)return null;
        try{return bridge.getMethod(name,args).invoke(null,values);}catch(ReflectiveOperationException e){throw new IllegalStateException("Optional race presentation failed",e);}
    }
    public static void accept(RaceExperience.Status status){
        int race=status.selected()?status.race():-1,tier=status.tiers()[Math.max(0,status.race())];
        call("accept",new Class<?>[]{boolean.class,int.class,int.class},status.selected(),status.race(),tier);
        if(race!=lastRace||tier!=lastTier)call("refresh",new Class<?>[]{boolean.class},race!=lastRace);
        lastRace=race;lastTier=tier;
    }
    public static boolean nativeKey(KeyMapping key){return Boolean.TRUE.equals(call("usesNativeKey",new Class<?>[]{KeyMapping.class},key));}
    public static boolean nativeHud(){
        if(!net.minecraft.client.Minecraft.getInstance().hasSingleplayerServer()&&ModList.get().isLoaded("jem_roles")&&ModList.get().isLoaded("irons_spellbooks"))return true;
        return Boolean.TRUE.equals(call("shouldUseNativeCast",new Class<?>[0]));
    }
}

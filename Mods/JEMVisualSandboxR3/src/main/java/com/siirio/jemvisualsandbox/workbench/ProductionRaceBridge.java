package com.siirio.jemvisualsandbox.workbench;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Dedicated adapter to the existing identity-bound JEMRoles save owner. */
public final class ProductionRaceBridge {
    private ProductionRaceBridge() {}
    private static Object call(String type,Object target,String name,Class<?>[] signature,Object... args){
        try{return Class.forName(type).getMethod(name,signature).invoke(target,args);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Race migration API: "+type+"."+name,failure);}
    }
    public static boolean authenticated(ServerPlayer p){
        return p!=null && p.server.isDedicatedServer() && net.minecraftforge.fml.ModList.get().isLoaded("jem_roles")
            && ((Optional<?>)call("com.siirio.jemidentity.api.IdentityPolicy",null,"resolve",new Class<?>[]{ServerPlayer.class},p)).isPresent();
    }
    /** Boss membership belongs to the optional canonical profile registry. */
    public static boolean registeredBoss(net.minecraft.world.entity.LivingEntity entity){
        if(!net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers"))return false;
        return ((Optional<?>)call("com.siirio.jemworldbosstiers.api.WorldTierApi",null,"profile",
                new Class<?>[]{net.minecraft.world.entity.LivingEntity.class},entity)).isPresent();
    }
    private static Optional<?> record(ServerPlayer p){return (Optional<?>)call("com.siirio.jemroles.race.RaceService",null,"record",new Class<?>[]{ServerPlayer.class},p);}
    public static CompoundTag data(ServerPlayer p){
        Object record=record(p).orElse(null);
        if(record==null)return new CompoundTag();
        String type="com.siirio.jemroles.race.RaceRecord";
        CompoundTag tag=(CompoundTag)call(type,record,"mechanics",new Class<?>[0]);
        int race=((Enum<?>)call(type,record,"race",new Class<?>[0])).ordinal();
        if(!tag.getBoolean("Migrated")){
            tag.putInt("Uses"+race,((Number)call(type,record,"spellProgress",new Class<?>[0])).intValue());
            tag.putLong("Cooldown"+race+"_signature",((Number)call(type,record,"cooldown",new Class<?>[]{String.class},"signature")).longValue());
            for(String key:new String[]{"guardian","gravity","void_projectile","lava_drink","second_blossom"})tag.putLong("Cooldown"+race+"_"+key,((Number)call(type,record,"cooldown",new Class<?>[]{String.class},key)).longValue());
            tag.putInt("BloomUpgrade",race==2?RacePowerStages.stageForUses(tag.getInt("Uses2")):0);
            p.getPersistentData().putInt("JemSandboxResonance",((Number)call(type,record,"resonance",new Class<?>[0])).intValue());
            tag.putBoolean("Migrated",true);dirty(p);
        }
        tag.putBoolean("Selected",true);tag.putInt("Race",race);
        tag.putLong("ActiveTicks",((Number)call(type,record,"activeTicks",new Class<?>[0])).longValue());
        tag.putBoolean("FreeSwitchUsed",(Boolean)call(type,record,"freeSwitchUsed",new Class<?>[0]));
        return tag;
    }
    public static void dirty(ServerPlayer p){
        Object save=call("com.siirio.jemroles.race.RaceSavedData",null,"get",new Class<?>[]{net.minecraft.server.MinecraftServer.class},p.server);
        call("com.siirio.jemroles.race.RaceSavedData",save,"changed",new Class<?>[0]);
    }
    @SuppressWarnings({"unchecked","rawtypes"}) public static void choose(ServerPlayer p,int race){
        try{
            Class type=Class.forName("com.siirio.jemroles.race.RaceId");
            Object next=type.getEnumConstants()[race];
            Object result=call("com.siirio.jemroles.race.RaceService",null,"choose",new Class<?>[]{ServerPlayer.class,type},p,next);
            if(((Enum<?>)result).name().equals("CHANGE_LOCKED"))p.displayClientMessage(net.minecraft.network.chat.Component.translatable("jem.roles.change_locked"),true);
        }catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}
    }
    public static void tick(ServerPlayer p){
        record(p).ifPresent(value->{call("com.siirio.jemroles.race.RaceRecord",value,"addActiveTicks",new Class<?>[]{long.class},20L);dirty(p);});
    }
}

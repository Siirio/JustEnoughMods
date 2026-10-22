package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/** Restores pre-race self effects only while the race still owns the active grant. */
final class RaceOwnedEffects {
    private record Grant(MobEffectInstance instance,CompoundTag applied,long appliedAt,CompoundTag baseline,long captured) {}
    private static final Map<UUID,Map<MobEffect,Grant>> OWNED=new HashMap<>();
    private RaceOwnedEffects() {}
    private static boolean owns(Grant grant,MobEffectInstance current,long now){
        if(grant==null||current==null||current!=grant.instance)return false;
        CompoundTag expected=age(grant.applied.copy(),Math.max(0,now-grant.appliedAt));
        return expected!=null&&expected.equals(current.save(new CompoundTag()));
    }
    static void grant(ServerPlayer player,MobEffectInstance effect){
        long now=player.level().getGameTime();
        var grants=OWNED.computeIfAbsent(player.getUUID(),id->new HashMap<>());
        var previous=player.getEffect(effect.getEffect());
        var owned=grants.get(effect.getEffect());
        boolean retained=owns(owned,previous,now);
        CompoundTag baseline=retained?owned.baseline:previous==null?null:previous.save(new CompoundTag());
        long captured=retained?owned.captured:now;
        CompoundTag before=previous==null?null:previous.save(new CompoundTag());
        player.addEffect(effect);
        var current=player.getEffect(effect.getEffect());
        CompoundTag applied=current==null?null:current.save(new CompoundTag());
        boolean changed=applied!=null&&!applied.equals(before);
        if(current!=null&&(retained||changed))grants.put(effect.getEffect(),
            new Grant(current,applied,now,baseline,captured));
        else grants.remove(effect.getEffect());
    }
    static void clearAll(){OWNED.clear();}
    static void clear(ServerPlayer player){
        var grants=OWNED.remove(player.getUUID());if(grants==null)return;
        long now=player.level().getGameTime();
        grants.forEach((type,grant)->{
            // A newer external amplifier, extension or replacement retains authority.
            if(!owns(grant,player.getEffect(type),now))return;
            player.removeEffect(type);
            if(grant.baseline!=null){
                CompoundTag aged=age(grant.baseline.copy(),Math.max(0,now-grant.captured));
                if(aged!=null){var restored=MobEffectInstance.load(aged);if(restored!=null)player.addEffect(restored);}
            }
        });
    }
    private static CompoundTag age(CompoundTag effect,long elapsed){
        CompoundTag hidden=effect.contains("HiddenEffect",10)?age(effect.getCompound("HiddenEffect"),elapsed):null;
        int duration=effect.getInt("Duration");
        if(duration!=-1&&duration<=elapsed)return hidden;
        if(duration!=-1)effect.putInt("Duration",(int)(duration-elapsed));
        if(hidden==null)effect.remove("HiddenEffect");else effect.put("HiddenEffect",hidden);
        return effect;
    }
}

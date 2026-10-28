package com.justenoughmods.achievementguide.integration.battle;
import java.util.ArrayDeque;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
public final class BattleQuestDamage {
    private record Hit(LivingEntity victim,DamageSource source,float vitality) {}
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Hit> frames=new ArrayDeque<>();int overflow; }
    private BattleQuestDamage() {}
    public static void begin(LivingEntity target,DamageSource source) { Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Hit(target,source,target.getHealth()+target.getAbsorptionAmount())); }
    public static void end(LivingEntity target,boolean accepted) {
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Hit hit=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();
        if(hit!=null&&hit.victim==target&&accepted&&Float.isFinite(hit.vitality)&&Float.isFinite(target.getHealth()+target.getAbsorptionAmount())&&target.getHealth()+target.getAbsorptionAmount()<hit.vitality) BattleQuestNightfall.damage(target,hit.source);
    }
    public static void clear(){CALLS.remove();}
}

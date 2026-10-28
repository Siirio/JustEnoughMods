package com.justenoughmods.achievementguide.integration.battle;
import java.util.ArrayDeque;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
public final class BattleQuestPhoenix {
    private static final String PROOF="jem_quest_phoenix_rebirth";
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Entity> frames=new ArrayDeque<>();int overflow; }
    private BattleQuestPhoenix() {}
    public static void death(LivingEntity victim,DamageSource source,boolean committed){
        if(!committed||!BattleQuestProof.natural(victim)||source.is(DamageTypes.GENERIC_KILL)||!BattleQuestProof.real(source.getEntity()))return;
        String type=BattleQuestProof.type(victim);
        if(type.equals("luminous_beasts:phoenix")||type.equals("luminous_beasts:wind_phoenix")){
            victim.getPersistentData().putUUID(PROOF,victim.getUUID());
        }else if(type.equals("luminous_beasts:baby_phoenix")||type.equals("luminous_beasts:baby_wind_phoenix")){
            if(victim.getPersistentData().hasUUID(PROOF))BattleQuestProof.emit(source.getEntity(),"v3_combat_beast_phoenix");
        }
    }
    public static void begin(Entity adult){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(adult);}
    public static void spawned(Entity child){
        Calls calls=CALLS.get();if(calls.overflow!=0||calls.frames.isEmpty()||child==null)return;Entity adult=calls.frames.peek();
        if(!adult.getPersistentData().hasUUID(PROOF)||!adult.getPersistentData().getUUID(PROOF).equals(adult.getUUID())||!BattleQuestProof.natural(adult)
            ||child.level()!=adult.level()||!(child.level() instanceof net.minecraft.server.level.ServerLevel level)||level.getEntity(child.getUUID())!=child)return;
        String expected=BattleQuestProof.type(adult).equals("luminous_beasts:wind_phoenix")?"luminous_beasts:baby_wind_phoenix":"luminous_beasts:baby_phoenix";
        if(BattleQuestProof.type(child).equals(expected))child.getPersistentData().putUUID(PROOF,adult.getUUID());
    }
    public static void end(){Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}if(!calls.frames.isEmpty())calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();}
    public static void clear(){CALLS.remove();}
}

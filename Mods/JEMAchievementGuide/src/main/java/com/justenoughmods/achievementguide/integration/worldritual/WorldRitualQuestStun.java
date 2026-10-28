package com.justenoughmods.achievementguide.integration.worldritual;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.Event;
import net.imasillylittleguy.cnc.entity.*;
public final class WorldRitualQuestStun {
    public interface GoalAccess {GoalSelector jem$ritualGoals();}
    public interface ReachAccess {double jem$ritualReach(LivingEntity actor);}
    private record Attempt(ServerPlayer actor,Mob target,double reach,Vec3 origin,boolean inside){}
    private record Before(Event event,Entity target,DamageSource source,boolean stunned){}
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls {final ArrayDeque<Before> frames=new ArrayDeque<>();int overflow;}
    private static final Map<Event,Attempt> PENDING=new IdentityHashMap<>();
    private static final Map<UUID,Attempt> ACTIVE=new HashMap<>();
    private WorldRitualQuestStun(){}
    private static EntityDataAccessor<Boolean> key(Entity target){return target instanceof WendigoEntity?WendigoEntity.DATA_stunned:target instanceof WechugeEntity?WechugeEntity.DATA_stunned:target instanceof SkinwalkerEntity?SkinwalkerEntity.DATA_stunned:null;}
    private static boolean stunned(Entity target){EntityDataAccessor<Boolean> key=target==null?null:key(target);return key!=null&&target.getEntityData().get(key);}
    public static void begin(Event event,Entity target,DamageSource source){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Before(event,target,source,stunned(target)));}
    public static void end(){
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Before before=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();
        if(before==null||before.stunned||!(before.event instanceof LivingAttackEvent event)||before.source==null||before.target!=event.getEntity()||before.source!=event.getSource()||!before.source.is(DamageTypes.FIREWORKS)
            ||!(before.source.getDirectEntity() instanceof FireworkRocketEntity rocket)||!WorldRitualQuestProof.real(rocket.getOwner())||before.source.getEntity()!=rocket.getOwner()||!(before.target instanceof Mob target)||!stunned(target)||!WorldRitualQuestProof.natural(target))return;
        ServerPlayer actor=(ServerPlayer)rocket.getOwner();if(actor.level()!=target.level())return;
        double reach=reach(target,actor);if(reach<0)return;
        if(PENDING.size()>=128)PENDING.clear();PENDING.put(event,new Attempt(actor,target,reach,actor.position(),target.distanceToSqr(actor)<=reach));
    }
    private static double reach(Mob target,LivingEntity actor){
        if(!(target instanceof GoalAccess access))return -1;double reach=-1;int count=0;
        for(WrappedGoal wrapped:access.jem$ritualGoals().getAvailableGoals()){if(++count>32)return -1;Goal goal=wrapped.getGoal();if(goal instanceof MeleeAttackGoal&&goal instanceof ReachAccess getter)reach=Math.max(reach,getter.jem$ritualReach(actor));}
        return reach;
    }
    public static void posted(Event event){if(!(event instanceof LivingAttackEvent attack)||!(attack.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isSameThread())return;Attempt attempt=PENDING.remove(event);if(attempt==null||event.isCanceled()||!WorldRitualQuestProof.real(attempt.actor)||!stunned(attempt.target))return;if(ACTIVE.size()>=256)ACTIVE.clear();ACTIVE.put(attempt.actor.getUUID(),attempt);}
    public static void state(Entity entity,EntityDataAccessor<?> field,Object value){if(!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)||!level.getServer().isSameThread())return;if(key(entity)!=field||!Boolean.FALSE.equals(value))return;ACTIVE.values().removeIf(attempt->attempt.target==entity);}
    public static void moved(ServerPlayer actor,Vec3 before){
        Attempt attempt=ACTIVE.get(actor.getUUID());if(attempt==null)return;
        if(attempt.actor!=actor||!WorldRitualQuestProof.real(actor)||!attempt.target.isAlive()||attempt.target.level()!=actor.level()||!stunned(attempt.target)||actor.serverLevel().getEntity(attempt.target.getUUID())!=attempt.target){ACTIVE.remove(actor.getUUID());return;}
        double reach=reach(attempt.target,actor);if(reach<0){ACTIVE.remove(actor.getUUID());return;}
        boolean inside=attempt.inside||attempt.target.position().distanceToSqr(before)<=reach||attempt.target.distanceToSqr(actor)<=reach;
        if(inside&&attempt.target.distanceToSqr(actor)>reach&&actor.position().distanceToSqr(before)>0.0001&&actor.position().distanceToSqr(attempt.origin)>0.0001){ACTIVE.remove(actor.getUUID());WorldRitualQuestProof.emit(actor,"v3_fauna_fireworks_escape");}
        else if(inside!=attempt.inside)ACTIVE.put(actor.getUUID(),new Attempt(actor,attempt.target,reach,attempt.origin,inside));
    }
    public static void forget(UUID actor){ACTIVE.remove(actor);PENDING.values().removeIf(attempt->attempt.actor.getUUID().equals(actor));}
    public static void clear(){CALLS.remove();PENDING.clear();ACTIVE.clear();}
}

package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class SonicWaveAbility {
    public static final double WAVE_RADIUS=8;
    public static final int DARKNESS_TICKS=100;
    public static final int DOMAIN_TICKS=SculkDomain.DURATION;
    private record Wave(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 origin,long start,Set<UUID> hit,int tier) {}
    private record Confusion(UUID owner,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,long until) {}
    private static final Map<UUID,Wave> ACTIVE=new HashMap<>();
    private static final Map<UUID,Confusion> CONFUSED=new HashMap<>();
    private SonicWaveAbility() {}
    public static boolean cast(ServerPlayer p) {
        if(!RaceExperience.canAct(p)||ACTIVE.containsKey(p.getUUID()))return false;
        ACTIVE.put(p.getUUID(),new Wave(p.serverLevel().dimension(),p.position(),p.level().getGameTime(),new HashSet<>(),RaceExperience.tier(p)));
        SandboxNetwork.effect(p,32,3,p.position(),0,p.getId());
        p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,net.minecraft.sounds.SoundSource.PLAYERS,.65F,1.15F);
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!RaceExperience.serverActive(event.getServer()))return;
        CONFUSED.entrySet().removeIf(entry->{
            Confusion confusion=entry.getValue();var level=event.getServer().getLevel(confusion.dimension);
            return level==null||level.getGameTime()>=confusion.until||!(level.getEntity(entry.getKey()) instanceof Mob mob)||!mob.isAlive();
        });
        ACTIVE.entrySet().removeIf(entry->{
            ServerPlayer p=event.getServer().getPlayerList().getPlayer(entry.getKey());Wave wave=entry.getValue();
            if(p==null||!p.isAlive()||RaceExperience.race(p)!=3||p.serverLevel().dimension()!=wave.dimension)return true;
            int age=(int)(p.level().getGameTime()-wave.start);if(age<0||age>18)return true;
            if(age==7&&wave.tier>=3)SculkDomain.create(p,wave.origin);
            if(age>=7){
                double radius=WAVE_RADIUS*Math.min(1,(age-6)/12.0);
                for(LivingEntity target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(wave.origin,wave.origin).inflate(radius,3,radius),entity->entity!=p&&entity.isAlive())){
                    if(target.position().subtract(wave.origin).multiply(1,0,1).lengthSqr()>radius*radius||!wave.hit.add(target.getUUID()))continue;
                    if(target instanceof Mob mob&&(mob instanceof Enemy||mob.getTarget()==p)){
                        if(mob.hurt(p.damageSources().sonicBoom(p),12))SandboxNetwork.effect(p,36,3,mob.position(),0,mob.getId());
                        if(wave.tier>=1){Vec3 away=mob.position().subtract(wave.origin).multiply(1,0,1).normalize();mob.push(away.x*.35,.12,away.z*.35);}
                    }
                    if(wave.tier>=2&&(target instanceof Mob||target instanceof net.minecraft.world.entity.player.Player)){
                        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS,DARKNESS_TICKS,0));
                        if(target instanceof Mob mob){
                            CONFUSED.put(mob.getUUID(),new Confusion(p.getUUID(),wave.dimension,p.level().getGameTime()+DARKNESS_TICKS));
                            forgetTarget(mob);
                        }
                    }
                }
            }
            return false;
        });
    }
    private static boolean confused(Mob mob){
        if(mob.level().isClientSide||mob.getServer()==null||!RaceExperience.serverActive(mob.getServer()))return false;
        Confusion confusion=CONFUSED.get(mob.getUUID());
        return confusion!=null&&confusion.dimension==mob.level().dimension()&&confusion.until>mob.level().getGameTime()
                &&mob.hasEffect(MobEffects.DARKNESS);
    }
    private static void forgetTarget(Mob mob){
        mob.setTarget(null);mob.setLastHurtByMob(null);mob.getNavigation().stop();
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent event){
        if(event.getEntity() instanceof Mob mob&&event.getNewTarget()!=null&&confused(mob))event.setNewTarget(null);
    }
    @SubscribeEvent public static void disorient(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof Mob mob)||!confused(mob))return;
        if(mob.getTarget()!=null||mob.tickCount%10==0)forgetTarget(mob);
        if(mob.tickCount%10==0){
            double angle=mob.getRandom().nextDouble()*Math.PI*2;
            double x=mob.getX()+Math.cos(angle)*3,z=mob.getZ()+Math.sin(angle)*3;
            mob.getLookControl().setLookAt(x,mob.getEyeY(),z,90,90);
            mob.getNavigation().moveTo(x,mob.getY(),z,.8);
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
    @SubscribeEvent public static void missedAttack(LivingAttackEvent event){
        // Only an actively disoriented mob's own immediate hit misses. Other damage remains native.
        if(event.getSource().getDirectEntity() instanceof Mob mob&&confused(mob))event.setCanceled(true);
    }
    static void selectionChanged(ServerPlayer p){
        ACTIVE.remove(p.getUUID());CONFUSED.values().removeIf(value->value.owner.equals(p.getUUID()));SculkDomain.clear(p);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){ACTIVE.clear();CONFUSED.clear();}
}

package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_roles")
public final class LifeChorusAbility {
    private record Chorus(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 origin,long start,Set<UUID> answered,int tier) {}
    private record OutwardWave(UUID owner,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 origin,long start,Set<UUID> repelled) {}
    private static final Map<UUID,Chorus> ACTIVE=new HashMap<>();
    static final int WAVE_TICKS=12;
    static final double WAVE_RADIUS=16;
    private static final java.util.List<OutwardWave> WAVES=new java.util.ArrayList<>();
    private LifeChorusAbility() {}
    public static boolean cast(ServerPlayer p) {
        if(!RaceExperience.canAct(p) || ACTIVE.containsKey(p.getUUID()))return false;
        ACTIVE.put(p.getUUID(),new Chorus(p.serverLevel().dimension(),p.position(),p.level().getGameTime(),new HashSet<>(),RaceExperience.tier(p)));
        startOutwardWave(p,7);
        SandboxNetwork.effect(p,30,0,p.position(),RacePowerStages.chorusRadius(RaceExperience.tier(p)),p.getId());
        return true;
    }
    static void guardianEntry(ServerPlayer p){
        if(!RaceExperience.allowed(p))return;
        startOutwardWave(p,0);
        SandboxNetwork.effect(p,35,0,p.position(),0,p.getId());
    }
    private static void startOutwardWave(ServerPlayer p,int delay){
        WAVES.add(new OutwardWave(p.getUUID(),p.serverLevel().dimension(),p.position(),p.level().getGameTime()+delay,new HashSet<>()));
    }
    private static boolean hostile(ServerPlayer p,Mob m){return m instanceof Enemy || m.getTarget()==p;}
    private static void repel(ServerPlayer p,Vec3 origin,double radius,Set<UUID> hit){
        var area=new net.minecraft.world.phys.AABB(origin,origin).inflate(radius,3,radius);
        for(Mob mob:p.serverLevel().getEntitiesOfClass(Mob.class,area,m->m.isAlive() && hostile(p,m))){
            if(!ProductionRaceBridge.canInteract(p,mob))continue;
            if(mob.position().subtract(origin).multiply(1,0,1).lengthSqr()>radius*radius || !hit.add(mob.getUUID()))continue;
            double dx=origin.x-mob.getX(),dz=origin.z-mob.getZ();if(dx*dx+dz*dz<.0001){dx=1;dz=0;}
            mob.knockback(.9,dx,dz);mob.hurtMarked=true;
        }
    }
    private static boolean friendly(ServerPlayer caster, LivingEntity target) {
        if(!ProductionRaceBridge.canInteract(caster,target))return false;
        if(target==caster)return true;
        if(target instanceof Player player)return !player.isSpectator() && (caster.getTeam()==null && player.getTeam()==null || caster.isAlliedTo(player));
        ServerPlayer owner=ProductionRaceBridge.damageOwner(target);
        if(owner!=null)return owner.isAlive() && !owner.isSpectator() && (owner==caster || caster.getTeam()==null && owner.getTeam()==null || caster.isAlliedTo(owner));
        if(target instanceof Mob mob && mob.getTarget()!=null)return false;
        return target instanceof Animal || target instanceof AbstractVillager || target instanceof IronGolem;
    }
    private static boolean peacefulAnimal(LivingEntity target) {
        return target instanceof Animal && ProductionRaceBridge.damageOwner(target)==null
                && (!(target instanceof Mob mob) || mob.getTarget()==null);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || !RaceExperience.serverActive(e.getServer()))return;
        WAVES.removeIf(w->{
            ServerPlayer p=e.getServer().getPlayerList().getPlayer(w.owner);
            if(p==null || !p.isAlive() || p.isSpectator() || p.serverLevel().dimension()!=w.dimension)return true;
            long age=p.level().getGameTime()-w.start;if(age<0)return false;if(age>=WAVE_TICKS)return true;
            repel(p,w.origin,WAVE_RADIUS*(age+1)/WAVE_TICKS,w.repelled);return false;
        });
        ACTIVE.entrySet().removeIf(entry->{
            ServerPlayer p=e.getServer().getPlayerList().getPlayer(entry.getKey());Chorus c=entry.getValue();
            if(p==null || !p.isAlive() || p.isSpectator() || p.serverLevel().dimension()!=c.dimension)return true;
            int age=(int)(p.level().getGameTime()-c.start);if(age<0 || age>=118)return true;
            if(age==7)p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,.6F,1.15F);
            if(age>=7 && age<=18) {
                double radius=RacePowerStages.chorusRadius(c.tier)*Math.min(1,(age-6)/(double)WAVE_TICKS);
                for(LivingEntity target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().move(c.origin.subtract(p.position())).inflate(radius,16,radius),v->v.isAlive() && friendly(p,v))) {
                    if(target.position().subtract(c.origin).multiply(1,0,1).lengthSqr()>radius*radius || !c.answered.add(target.getUUID()))continue;
                    float missing=Math.max(0,target.getMaxHealth()-target.getHealth());
                    float healing=target instanceof Player?6:Math.max(6,.20F*target.getMaxHealth());
                    if(peacefulAnimal(target))healing=missing;
                    float previousHealth=target.getHealth();
                    target.heal(Math.min(missing,healing));
                    if(target instanceof ServerPlayer healedPlayer)SandboxRaceTraits.syncHealthChange(healedPlayer,previousHealth);
                    if(c.tier>=2)target.getActiveEffects().stream().filter(v->v.getEffect().getCategory()==MobEffectCategory.HARMFUL).map(MobEffectInstance::getEffect).toList().forEach(target::removeEffect);
                    // Retain the preceding canonical Life Chorus full-health ally boon.
                    if(c.tier>=3 && target.getHealth()>=target.getMaxHealth()){
                        var boon=new MobEffectInstance(MobEffects.ABSORPTION,600,1);
                        if(target==p)RaceOwnedEffects.grant(p,boon);else target.addEffect(boon);
                    }
                    SandboxNetwork.effect(p,31,0,target.position(),Math.max(0,13-age),target.getId());
                }
            }
            return false;
        });
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void protect(LivingHurtEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p) || !RaceExperience.serverActive(p.server))return;
        Chorus c=ACTIVE.get(p.getUUID());if(c==null || c.dimension!=p.serverLevel().dimension())return;
        long age=p.level().getGameTime()-c.start;
        if(age>=18 && age<118)e.setAmount(e.getAmount()*.75F);
    }
    static void selectionChanged(ServerPlayer p){ACTIVE.remove(p.getUUID());WAVES.removeIf(w->w.owner.equals(p.getUUID()));}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { ACTIVE.clear();WAVES.clear(); }
}

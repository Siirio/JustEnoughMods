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

@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class LifeChorusAbility {
    private record Chorus(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 origin,long start,Set<UUID> answered) {}
    private record OutwardWave(UUID owner,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 origin,long start,Set<UUID> repelled) {}
    private static final Map<UUID,Chorus> ACTIVE=new HashMap<>();
    static final int WAVE_TICKS=12;
    static final double WAVE_RADIUS=16;
    private static final java.util.List<OutwardWave> WAVES=new java.util.ArrayList<>();
    private LifeChorusAbility() {}
    public static void cast(ServerPlayer p) {
        if(p.server.isDedicatedServer() || !p.server.isSingleplayerOwner(p.getGameProfile()) || !p.isAlive() || p.isSpectator() || ACTIVE.containsKey(p.getUUID()))return;
        ACTIVE.put(p.getUUID(),new Chorus(p.serverLevel().dimension(),p.position(),p.level().getGameTime(),new HashSet<>()));
        startOutwardWave(p,7);
        SandboxNetwork.effect(p,30,0,p.position(),0,p.getId());
    }
    static void guardianEntry(ServerPlayer p){
        if(p.server.isDedicatedServer() || !p.server.isSingleplayerOwner(p.getGameProfile()))return;
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
            if(mob.position().subtract(origin).multiply(1,0,1).lengthSqr()>radius*radius || !hit.add(mob.getUUID()))continue;
            double dx=origin.x-mob.getX(),dz=origin.z-mob.getZ();if(dx*dx+dz*dz<.0001){dx=1;dz=0;}
            mob.knockback(.9,dx,dz);mob.hurtMarked=true;
        }
    }
    private static boolean friendly(LivingEntity e) {
        if(e instanceof Player || e instanceof Animal || e instanceof AbstractVillager || e instanceof IronGolem)return true;
        String namespace=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace();return namespace.equals("recruits") || namespace.equals("workers");
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || e.getServer().isDedicatedServer())return;
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
                double radius=WAVE_RADIUS*Math.min(1,(age-6)/(double)WAVE_TICKS);
                for(LivingEntity target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().move(c.origin.subtract(p.position())).inflate(radius,16,radius),v->v.isAlive() && friendly(v) && !(v instanceof Mob m && hostile(p,m)))) {
                    if(target.position().subtract(c.origin).multiply(1,0,1).lengthSqr()>radius*radius || !c.answered.add(target.getUUID()))continue;
                    if(target instanceof Player)target.heal(6);else target.setHealth(target.getMaxHealth());
                    target.getActiveEffects().stream().filter(v->v.getEffect().getCategory()==MobEffectCategory.HARMFUL).map(MobEffectInstance::getEffect).toList().forEach(target::removeEffect);
                    // Retain the preceding canonical Life Chorus full-health ally boon.
                    if(target.getHealth()>=target.getMaxHealth())target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,600,1));
                    SandboxNetwork.effect(p,31,0,target.position(),Math.max(0,13-age),target.getId());
                }
            }
            return false;
        });
    }
    @SubscribeEvent(priority=EventPriority.LOW) public static void protect(LivingHurtEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p) || p.server.isDedicatedServer())return;
        Chorus c=ACTIVE.get(p.getUUID());if(c==null || c.dimension!=p.serverLevel().dimension())return;
        long age=p.level().getGameTime()-c.start;
        if(age>=18 && age<118)e.setAmount(e.getAmount()*.75F);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { ACTIVE.clear();WAVES.clear(); }
}

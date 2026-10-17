package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class SonicWaveAbility {
    private record Wave(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 origin,long start,Set<UUID> hit,int tier) {}
    private static final Map<UUID,Wave> ACTIVE=new HashMap<>();
    private SonicWaveAbility() {}
    public static boolean cast(ServerPlayer p) {
        if(!RaceExperience.canAct(p) || ACTIVE.containsKey(p.getUUID()))return false;
        ACTIVE.put(p.getUUID(),new Wave(p.serverLevel().dimension(),p.position(),p.level().getGameTime(),new HashSet<>(),RaceExperience.tier(p)));SandboxNetwork.effect(p,32,3,p.position(),0,p.getId());
        p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,net.minecraft.sounds.SoundSource.PLAYERS,.65F,1.15F);
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END || e.getServer().isDedicatedServer())return;
        ACTIVE.entrySet().removeIf(entry->{
            ServerPlayer p=e.getServer().getPlayerList().getPlayer(entry.getKey());Wave w=entry.getValue();
            if(p==null || !p.isAlive() || p.serverLevel().dimension()!=w.dimension)return true;
            int age=(int)(p.level().getGameTime()-w.start);if(age<0 || age>18)return true;
            if(age>=7) {
                double radius=8*Math.min(1,(age-6)/12.0);
                for(Mob mob:p.serverLevel().getEntitiesOfClass(Mob.class,new AABB(w.origin,w.origin).inflate(radius,3,radius),m->m.isAlive() && (m instanceof Enemy || m.getTarget()==p)))
                    if(mob.position().subtract(w.origin).multiply(1,0,1).lengthSqr()<=radius*radius && w.hit.add(mob.getUUID())){
                        if(mob.hurt(p.damageSources().sonicBoom(p),12))SandboxNetwork.effect(p,36,3,mob.position(),0,mob.getId());
                        if(w.tier>=2){Vec3 away=mob.position().subtract(w.origin).normalize();mob.push(away.x*.35,.12,away.z*.35);}
                        if(w.tier>=3)mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,40,0));
                    }
            }
            return false;
        });
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { ACTIVE.clear(); }
}

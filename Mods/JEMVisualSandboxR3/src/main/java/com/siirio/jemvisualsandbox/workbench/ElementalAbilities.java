package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated sandbox choreography. Production race state remains owned by JEMRoles. */
@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class ElementalAbilities {
    private record Rift(UUID target, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long start) {}
    private record Wave(java.util.List<Vec3> spots, Vec3 direction, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long start,int tier) {}
    private static final Map<UUID,Rift> RIFTS = new HashMap<>();
    private static final Map<UUID,Wave> WAVES = new HashMap<>();
    private ElementalAbilities() {}
    private static boolean allowed(ServerPlayer p) {
        return !p.server.isDedicatedServer() && p.server.isSingleplayerOwner(p.getGameProfile()) && p.isAlive() && !p.isSpectator();
    }
    private static Vec3 forward(ServerPlayer p) {
        double angle = Math.toRadians(p.getYRot());
        return new Vec3(-Math.sin(angle),0,Math.cos(angle));
    }
    public static boolean rift(ServerPlayer p) {
        if (!RaceExperience.canAct(p) || RIFTS.containsKey(p.getUUID())) return false;
        LivingEntity target = SandboxSkills.riftTarget(p);
        if (target == null) { p.displayClientMessage(Component.literal("Look at a living target within range"),true); return false; }
        if(target instanceof net.minecraft.world.entity.player.Player&&!p.server.isPvpAllowed()){p.displayClientMessage(Component.translatable("jem.roles.action.pvp_disabled"),true);return false;}
        Vec3 behind=target.position().subtract(target.getLookAngle().normalize().scale(1.8));
        if(SandboxEnvironment.nearestSafe(p,p.serverLevel(),net.minecraft.core.BlockPos.containing(behind),3,true).isEmpty()){
            p.displayClientMessage(Component.translatable("jem.roles.action.no_safe_destination"),true);return false;
        }
        RIFTS.put(p.getUUID(),new Rift(target.getUUID(),p.serverLevel().dimension(),p.level().getGameTime()));
        SandboxNetwork.effect(p,5,1,p.position().add(forward(p).scale(1.2)),p.getYRot(),-1);
        return true;
    }
    public static boolean eruption(ServerPlayer p) {
        if (!RaceExperience.canAct(p) || WAVES.containsKey(p.getUUID())) return false;
        Vec3 direction=forward(p);java.util.List<Vec3> spots=new java.util.ArrayList<>();
        for(int i=0;i<5;i++)spots.add(SurfacePlacement.top(p,p.position().add(direction.scale(1.6+i*2)),10));
        if(spots.stream().allMatch(java.util.Objects::isNull)){p.displayClientMessage(Component.literal("No valid eruption surface"),true);return false;}
        WAVES.put(p.getUUID(),new Wave(spots,direction,p.serverLevel().dimension(),p.level().getGameTime(),RaceExperience.tier(p)));
        java.util.List<Vec3> trail=new java.util.ArrayList<>();
        for(int i=0;i<=20;i++) {
            Vec3 point=SurfacePlacement.top(p,p.position().add(direction.scale(i*.5)),10);
            if(point==null)break;trail.add(point);
        }
        SandboxNetwork.path(p,0,trail);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.isDedicatedServer()) return;
        RIFTS.entrySet().removeIf(entry -> {
            ServerPlayer p=server.getPlayerList().getPlayer(entry.getKey()); Rift r=entry.getValue();
            if(p==null || !allowed(p) || RaceExperience.race(p)!=1 || p.serverLevel().dimension()!=r.dimension) return true;
            long age=p.level().getGameTime()-r.start;
            if(age<4) { p.setDeltaMovement(0,p.getDeltaMovement().y,0);p.connection.send(new ClientboundSetEntityMotionPacket(p)); }
            if(age>=14) {
                if(SandboxSkills.commitRift(p,r.target)) {RaceExperience.finishSignature(p);SandboxNetwork.effect(p,9,1,p.position().add(forward(p).scale(1.2)),p.getYRot(),-1);}
                return true;
            }
            return age<0;
        });
        WAVES.entrySet().removeIf(entry -> {
            ServerPlayer p=server.getPlayerList().getPlayer(entry.getKey()); Wave w=entry.getValue();
            if(p==null || !allowed(p) || RaceExperience.race(p)!=4 || p.serverLevel().dimension()!=w.dimension) return true;
            int age=(int)(p.level().getGameTime()-w.start);
            if(age<0 || age>26) return true;
            if(age<=16 && age%4==0) {
                int index=age/4;Vec3 spot=w.spots.get(index);
                if(spot!=null)SandboxNetwork.effect(p,8,4,spot,(float)Math.toDegrees(Math.atan2(w.direction.x,w.direction.z)),index%3);
            }
            if(age>=5 && (age-5)%4==0) {
                int index=(age-5)/4;
                if(index<5) {
                    Vec3 spot=w.spots.get(index);
                    if(spot!=null)for(LivingEntity target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(spot,spot).inflate(1.8,2.5,1.8),e->e!=p&&e.isAlive())) {
                        int tier=w.tier;target.hurt(p.damageSources().onFire(),4+2*tier);target.setSecondsOnFire(2*tier);target.push(0,.7,0);
                    }
                }
            }
            return false;
        });
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { RIFTS.clear();WAVES.clear(); }
}

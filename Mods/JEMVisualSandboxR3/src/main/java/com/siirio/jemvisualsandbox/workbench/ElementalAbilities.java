package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated sandbox choreography. Production race state remains owned by JEMRoles. */
@Mod.EventBusSubscriber(modid="jem_visual_sandbox")
public final class ElementalAbilities {
    private record Rift(UUID target, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long start) {}
    private record Wave(Vec3 origin, Vec3 direction, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long start) {}
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
    public static void rift(ServerPlayer p) {
        if (!allowed(p) || RIFTS.containsKey(p.getUUID())) return;
        LivingEntity target = SandboxSkills.riftTarget(p);
        if (target == null) { p.displayClientMessage(Component.literal("Look at a living target within 32 blocks"),true); return; }
        RIFTS.put(p.getUUID(),new Rift(target.getUUID(),p.serverLevel().dimension(),p.level().getGameTime()));
        SandboxNetwork.effect(p,5,1,p.position().add(forward(p).scale(1.2)),p.getYRot(),-1);
    }
    public static void eruption(ServerPlayer p) {
        if (!allowed(p) || WAVES.containsKey(p.getUUID())) return;
        WAVES.put(p.getUUID(),new Wave(p.position(),forward(p),p.serverLevel().dimension(),p.level().getGameTime()));
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.isDedicatedServer()) return;
        RIFTS.entrySet().removeIf(entry -> {
            ServerPlayer p=server.getPlayerList().getPlayer(entry.getKey()); Rift r=entry.getValue();
            if(p==null || !allowed(p) || p.serverLevel().dimension()!=r.dimension) return true;
            long age=p.level().getGameTime()-r.start;
            if(age<4) { p.setDeltaMovement(0,p.getDeltaMovement().y,0);p.connection.send(new ClientboundSetEntityMotionPacket(p)); }
            if(age>=14) {
                if(SandboxSkills.commitRift(p,r.target)) SandboxNetwork.effect(p,9,1,p.position().add(forward(p).scale(1.2)),p.getYRot(),-1);
                return true;
            }
            return age<0;
        });
        WAVES.entrySet().removeIf(entry -> {
            ServerPlayer p=server.getPlayerList().getPlayer(entry.getKey()); Wave w=entry.getValue();
            if(p==null || !allowed(p) || p.serverLevel().dimension()!=w.dimension) return true;
            int age=(int)(p.level().getGameTime()-w.start);
            if(age<0 || age>26) return true;
            if(age<=16 && age%4==0) {
                int index=age/4;Vec3 spot=ground(p,w,index);
                if(spot==null)return true;
                SandboxNetwork.effect(p,8,4,spot,(float)Math.toDegrees(Math.atan2(w.direction.x,w.direction.z)),index%3);
            }
            if(age>=5 && (age-5)%4==0) {
                int index=(age-5)/4;
                if(index<5) {
                    Vec3 spot=ground(p,w,index);
                    if(spot!=null)for(LivingEntity target:p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(spot,spot).inflate(1.8,2.5,1.8),e->e!=p&&e.isAlive())) {
                        target.hurt(p.damageSources().onFire(),10);target.setSecondsOnFire(6);target.push(0,.7,0);
                    }
                }
            }
            return false;
        });
    }
    private static Vec3 ground(ServerPlayer p,Wave w,int index) {
        Vec3 next=w.origin.add(w.direction.scale(1.6+index*2));
        var level=p.serverLevel();BlockPos at=BlockPos.containing(next);
        if(!level.hasChunkAt(at))return null;
        Vec3 point=null;
        for(int dy=2;dy>=-4;dy--) {
            BlockPos floor=at.offset(0,dy-1,0);
            var shape=level.getBlockState(floor).getCollisionShape(level,floor);
            if(!shape.isEmpty() && level.getBlockState(floor.above()).getCollisionShape(level,floor.above()).isEmpty()) {
                point=new Vec3(next.x,floor.getY()+shape.max(net.minecraft.core.Direction.Axis.Y)+.025,next.z);break;
            }
        }
        if(point==null || !level.getWorldBorder().isWithinBounds(BlockPos.containing(point)))return null;
        var hit=level.clip(new ClipContext(w.origin.add(0,.4,0),point.add(0,.4,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return hit.getType()==HitResult.Type.MISS || hit.getLocation().distanceToSqr(point.add(0,.4,0))<.3 ? point:null;
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { RIFTS.clear();WAVES.clear(); }
}

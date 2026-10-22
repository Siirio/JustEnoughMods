package com.siirio.jemvisualsandbox.workbench;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Transient selection protection, scoped to one logical server lifetime. */
@Mod.EventBusSubscriber(modid="jem_roles")
public final class RaceSelectionScene {
    private RaceSelectionScene() {}
    private record Scene(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,Vec3 anchor,long expires,boolean completing) {}
    private static final Map<MinecraftServer,Map<UUID,Scene>> SERVERS=new java.util.IdentityHashMap<>();
    private static Map<UUID,Scene> scenes(ServerPlayer player) {
        return SERVERS.computeIfAbsent(player.server, ignored -> new HashMap<>());
    }
    static void complete(ServerPlayer p){
        Scene scene=scenes(p).get(p.getUUID());
        boolean confirmed=RaceExperience.selected(p);
        // Selection acknowledgements may repeat; they must never extend protection.
        if(scene!=null&&confirmed&&!scene.completing)scenes(p).put(p.getUUID(),new Scene(scene.dimension,scene.anchor,p.server.overworld().getGameTime()+100,true));
    }
    public static void setEnabled(ServerPlayer p,boolean enabled){
        if(!RaceExperience.allowed(p))return;
        if(enabled){
            // Reopening the progression menu must not grant gameplay immunity.
            if(RaceExperience.selected(p))return;
            scenes(p).putIfAbsent(p.getUUID(),new Scene(p.serverLevel().dimension(),p.position(),p.server.overworld().getGameTime()+600,false));
        }
        else scenes(p).remove(p.getUUID());
    }
    public static boolean locked(ServerPlayer p){
        Map<UUID,Scene> scenes=SERVERS.get(p.server);
        Scene scene=scenes==null?null:scenes.get(p.getUUID());
        return scene!=null&&(scene.expires>p.server.overworld().getGameTime()||selectionPending(p));
    }
    private static boolean selectionPending(ServerPlayer p){
        if(RaceExperience.allowed(p))return !RaceExperience.selected(p);
        return false;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||!RaceExperience.allowed(p))return;
        Scene scene=scenes(p).get(p.getUUID());
        if(scene!=null){
            if(scene.expires<=p.server.overworld().getGameTime()&&!selectionPending(p)||scene.dimension!=p.serverLevel().dimension())scenes(p).remove(p.getUUID());
            else {p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;if(p.position().distanceToSqr(scene.anchor)>.0025)p.connection.teleport(scene.anchor.x,scene.anchor.y,scene.anchor.z,p.getYRot(),p.getXRot());}
        }
    }
    @SubscribeEvent public static void protect(LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p&&locked(p))e.setCanceled(true);}
    @SubscribeEvent public static void interact(PlayerInteractEvent e){if(e.getEntity() instanceof ServerPlayer p&&locked(p)&&e.isCancelable())e.setCanceled(true);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&locked(p))e.setCanceled(true);}
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event); }
    private static void clear(PlayerEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) scenes(player).remove(player.getUUID());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { SERVERS.remove(event.getServer()); }
}

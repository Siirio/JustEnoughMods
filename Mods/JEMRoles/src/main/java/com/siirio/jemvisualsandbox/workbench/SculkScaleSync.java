package com.siirio.jemvisualsandbox.workbench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends the server-owned Sculk size to the owner and only to players tracking that owner. */
@Mod.EventBusSubscriber(modid="jem_roles")
final class SculkScaleSync {
    private static final Map<UUID,Set<UUID>> WATCHERS=new HashMap<>();
    private static final Map<UUID,Set<UUID>> PENDING=new HashMap<>();
    private static final Set<UUID> AUTHENTICATED=new HashSet<>();

    private SculkScaleSync() {}

    static void changed(ServerPlayer target,boolean enlarged){
        if(RaceExperience.allowed(target))SandboxNetwork.sculkScale(target,target,enlarged);
        for(ServerPlayer viewer:trackingViewers(target))
            SandboxNetwork.sculkScale(viewer,target,enlarged);
    }

    static List<ServerPlayer> trackingViewers(ServerPlayer target){
        List<ServerPlayer> result=new ArrayList<>();
        for(UUID viewerId:WATCHERS.getOrDefault(target.getUUID(),Set.of())){
            ServerPlayer viewer=target.server.getPlayerList().getPlayer(viewerId);
            if(viewer!=null&&viewer.serverLevel()==target.serverLevel()&&RaceExperience.allowed(viewer)
                    &&SandboxNetwork.supported(viewer.connection.connection))result.add(viewer);
        }
        return List.copyOf(result);
    }

    @SubscribeEvent public static void startTracking(PlayerEvent.StartTracking event){
        if(event.getEntity() instanceof ServerPlayer viewer&&event.getTarget() instanceof ServerPlayer target){
            WATCHERS.computeIfAbsent(target.getUUID(),ignored->new HashSet<>()).add(viewer.getUUID());
            queue(viewer.getUUID(),target.getUUID());
        }
    }

    @SubscribeEvent public static void stopTracking(PlayerEvent.StopTracking event){
        if(event.getEntity() instanceof ServerPlayer viewer&&event.getTarget() instanceof ServerPlayer target){
            UUID targetId=target.getUUID(),viewerId=viewer.getUUID();
            Set<UUID> watchers=WATCHERS.get(targetId);
            if(watchers!=null){
                watchers.remove(viewerId);
                if(watchers.isEmpty())WATCHERS.remove(targetId);
            }
            Set<UUID> pending=PENDING.get(viewerId);
            if(pending!=null){
                pending.remove(targetId);
                if(pending.isEmpty())PENDING.remove(viewerId);
            }
        }
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player)authenticated(player);
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer player)SculkDomain.synchronize(player);
    }

    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getEntity() instanceof ServerPlayer player)SculkDomain.synchronize(player);
    }

    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event){
        if(event.phase==TickEvent.Phase.END&&event.player instanceof ServerPlayer player)authenticated(player);
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        PENDING.entrySet().removeIf(entry->{
            ServerPlayer viewer=event.getServer().getPlayerList().getPlayer(entry.getKey());
            if(viewer==null)return true;
            if(!RaceExperience.allowed(viewer))return false;
            entry.getValue().removeIf(targetId->{
                ServerPlayer target=event.getServer().getPlayerList().getPlayer(targetId);
                if(target==null||target.serverLevel()!=viewer.serverLevel()
                        ||!WATCHERS.getOrDefault(targetId,Set.of()).contains(viewer.getUUID()))return true;
                if(!RaceExperience.allowed(target))return false;
                SandboxNetwork.sculkScale(viewer,target,SculkDomain.enlarged(target));
                return true;
            });
            return entry.getValue().isEmpty();
        });
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        UUID id=player.getUUID();
        AUTHENTICATED.remove(id);
        WATCHERS.remove(id);
        WATCHERS.values().forEach(viewers->viewers.remove(id));
        WATCHERS.values().removeIf(Set::isEmpty);
        PENDING.remove(id);
        PENDING.values().forEach(targets->targets.remove(id));
        PENDING.values().removeIf(Set::isEmpty);
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        WATCHERS.clear();
        PENDING.clear();
        AUTHENTICATED.clear();
    }

    private static void authenticated(ServerPlayer player){
        if(!RaceExperience.allowed(player)||!AUTHENTICATED.add(player.getUUID()))return;
        SculkDomain.synchronize(player);
        SandboxNetwork.quickSkinAuthReady(player);
        for(var entry:WATCHERS.entrySet())
            if(entry.getValue().contains(player.getUUID()))queue(player.getUUID(),entry.getKey());
    }

    private static void queue(UUID viewer,UUID target){
        PENDING.computeIfAbsent(viewer,ignored->new HashSet<>()).add(target);
    }
}

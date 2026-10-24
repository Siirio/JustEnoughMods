package com.siirio.jemserver.smp;

import com.siirio.jemserver.smp.events.*;
import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import com.siirio.jemworldbosstiers.encounter.HostedEncounters;
import com.siirio.jemworldbosstiers.encounter.PendingRewardContainer;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

@Mod.EventBusSubscriber(modid="jem_server",value=Dist.DEDICATED_SERVER)
public final class CombatNetwork {
    private static final int TICKS_PER_SECOND=20;
    private static final String VERSION="4";
    private static final SimpleChannel CHANNEL=NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jem_server","combat_v2"))
            .networkProtocolVersion(()->VERSION).clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).simpleChannel();
    public record Update(CompoundTag data,boolean result) {}
    public record Claim(UUID result) {}
    public record Vote(UUID event,boolean continueBattle) {}

    public static void register() {
        CHANNEL.messageBuilder(Update.class,0,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet,buffer)->{buffer.writeNbt(packet.data());buffer.writeBoolean(packet.result());})
                .decoder(buffer->new Update(buffer.readNbt(new NbtAccounter(SmpProtocol.MAX_VIEW_BYTES)),buffer.readBoolean()))
                .consumerMainThread((packet,context)->{
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.CombatHud.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Claim.class,1,NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet,buffer)->buffer.writeUUID(packet.result())).decoder(buffer->new Claim(buffer.readUUID()))
                .consumerMainThread((packet,context)->{
                    // Reserved legacy discriminator: rewards are claimed only by jem_rewards.
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Vote.class,2,NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet,buffer)->{buffer.writeUUID(packet.event());buffer.writeBoolean(packet.continueBattle());})
                .decoder(buffer->new Vote(buffer.readUUID(),buffer.readBoolean()))
                .consumerMainThread((packet,context)->{
                    var player=context.get().getSender();
                    if(player!=null&&SmpEnvironment.active(player)&&SmpRequests.allow(player)) BloodMoonVoting.vote(player,packet.event(),packet.continueBattle());
                    context.get().setPacketHandled(true);
                }).add();
    }
    public static void vote(UUID event,boolean continueBattle) { CHANNEL.sendToServer(new Vote(event,continueBattle)); }
    public static void tick(MinecraftServer server) {
        if(server.getTickCount()%TICKS_PER_SECOND!=0) return;
        var visible=new HashSet<UUID>();
        for(var party:SmpData.get(server).parties().snapshots()) {
            if(!party.state().equals("ACTIVE") || !party.hasBossEntity()) continue;
            var dimension=ResourceLocation.tryParse(party.dimension());
            var level=dimension==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
            if(level==null || !(level.getEntity(party.bossEntity()) instanceof LivingEntity boss)) continue;
            var participants=HostedEncounterApi.status(boss);
            var snapshot=base(party.id(),party.dimension(),party.ends(),boss.getDisplayName().getString(),false);
            snapshot.putString("titleKey",boss.getType().getDescriptionId());
            if(party.hasEventId()) {
                var event=SmpData.get(server).events().snapshot(party.eventId());
                if(event!=null) snapshot.putLong("ends",event.ends());
            }
            var rows=new ListTag();
            var included=new HashSet<UUID>();
            for(var participant:participants) {
                included.add(participant.playerId());
                var player=server.getPlayerList().getPlayer(participant.playerId());
                var member=party.member(participant.playerId());
                var row=playerRow(participant.playerId(),member,player,participant.state());
                if(!row.contains("remaining")) row.putLong("remaining",participant.remainingTicks()/TICKS_PER_SECOND);
                rows.add(row);
            }
            for(UUID id:party.members().keySet()) {
                var member=party.member(id);
                if(!included.contains(id) && member.simulated()) rows.add(playerRow(id,member,null,"ALIVE"));
            }
            snapshot.put("rows",rows);
            for(var participant:participants) {
                var player=server.getPlayerList().getPlayer(participant.playerId());
                if(player!=null && player.level()==level && !player.isSpectator()) publish(player,snapshot,visible);
            }
        }
        for(var event:SmpData.get(server).events().snapshots()) {
            if(!event.state().equals("ACTIVE") || !event.activity().equals("BLOOD_MOON") || !event.combatStarted()) continue;
            var snapshot=bloodSnapshot(server,event);
            for(UUID id:event.members().keySet()) {
                var player=server.getPlayerList().getPlayer(id);
                if(player!=null && new EventSession(event).accepted(id) && player.level().dimension().location().toString().equals(event.dimension())) publish(player,snapshot,visible);
            }
        }
        for(UUID id:SmpServerSession.get(server).combatPlayers()) if(!visible.contains(id)) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) send(player,new Update(new CompoundTag(),false));
            SmpServerSession.get(server).player(id).combat = null;
        }
    }

    public static void refreshBloodMoon(MinecraftServer server,EventState event) {
        if(!event.state().equals("ACTIVE")) return;
        CompoundTag snapshot=bloodSnapshot(server,event);
        for(UUID id:event.members().keySet()) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null && new EventSession(event).accepted(id)) {
                send(player,new Update(snapshot,false));
                SmpServerSession.get(server).player(id).combat = snapshot.copy();
            }
        }
    }

    private static CompoundTag bloodSnapshot(MinecraftServer server,EventState event) {
        var snapshot=base(event.id(),event.dimension(),event.ends(),"",true);
        snapshot.putInt("wave",event.wave());
        snapshot.putInt("waves",BloodMoonWaves.WAVES);
        snapshot.putInt("remainingMobs",Math.max(0,event.waveTotal()-event.waveKilled()));
        var eventLevel=EventRegions.level(server,event);
        if(eventLevel!=null) for(UUID mobId:event.mobs()) {
            var mob=eventLevel.getEntity(mobId);
            if(mob!=null && mob.getPersistentData().getString("jem:blood_category").equals("MINIBOSS")) {
                snapshot.putString("miniboss",mob.getDisplayName().getString());
                snapshot.putString("minibossKey",mob.getType().getDescriptionId());
                break;
            }
        }
        var rows=new ListTag();
        for(UUID id:event.members().keySet()) {
            var member=event.member(id);
            var player=server.getPlayerList().getPlayer(id);
            rows.add(playerRow(id,member,player,member.accepted()?"ALIVE":"ELIMINATED"));
        }
        snapshot.put("rows",rows);
        BloodMoonVoting.snapshot(server,event,snapshot);
        return snapshot;
    }
    private static CompoundTag base(UUID id,String dimension,long ends,String title,boolean blood) {
        var result=new CompoundTag();
        result.putUUID("id",id);
        result.putString("dimension",dimension);
        result.putString("title",title);
        result.putBoolean("blood",blood);
        result.putLong("ends",ends);
        return result;
    }
    private static CompoundTag playerRow(UUID id,PartyMember member,ServerPlayer player,String state) {
        return playerRow(id,member==null?"":member.name(),member!=null&&member.simulated(),
                member==null?0F:member.hp(),member==null?0F:member.maxHp(),member==null?0:member.ping(),player,state);
    }
    private static CompoundTag playerRow(UUID id,EventMember member,ServerPlayer player,String state) {
        return playerRow(id,member.name(),false,0F,0F,0,player,state);
    }
    private static CompoundTag playerRow(UUID id,String name,boolean simulated,float hp,float maxHp,int ping,ServerPlayer player,String state) {
        var row=new CompoundTag();
        row.putUUID("id",id);
        row.putString("name",player==null?name:player.getGameProfile().getName());
        if(player==null && !simulated) state="DISCONNECTED";
        else if(player!=null && ReviveSupport.downed(player)) {state="DOWNED";row.putLong("remaining",ReviveSupport.remainingTicks(player)/TICKS_PER_SECOND);row.putBoolean("timerPaused",ReviveSupport.timerPaused(player));}
        else if(player!=null && (!player.isAlive() || player.isSpectator())) state="ELIMINATED";
        row.putString("state",state);
        row.putFloat("hp",simulated?hp:player==null?0:player.getHealth());
        row.putFloat("maxHp",simulated?maxHp:player==null?0:player.getMaxHealth());
        if(simulated) row.putInt("ping",ping);
        return row;
    }
    private static void publish(ServerPlayer player,CompoundTag snapshot,Set<UUID> visible) {
        if(!visible.add(player.getUUID())) return;
        if(!snapshot.equals(SmpServerSession.get(player.server).player(player.getUUID()).combat)) {
            send(player,new Update(snapshot,false));
            SmpServerSession.get(player.server).player(player.getUUID()).combat = snapshot.copy();
        }
    }
    @SubscribeEvent public static void closed(HostedEncounterClosedEvent event) {
        if (!SmpEnvironment.active(event.server())) return;
        var result = new CompoundTag();
        result.putUUID("id", event.bossId());
        result.putString("source", event.bossId().toString());
        var boss = event.boss();
        result.putString("title", boss == null ? "" : boss.getDisplayName().getString());
        result.putString("titleKey", boss == null ? "" : boss.getType().getDescriptionId());
        result.putBoolean("success", event.success());
        var rows = new ListTag();
        var players = event.frozenState().getCompound("Players");
        for (String id : players.getAllKeys()) {
            var entry = players.getCompound(id);
            var row = new CompoundTag();
            row.putUUID("id", UUID.fromString(id)); row.putString("name", entry.getString("Name"));
            row.putDouble("damage", entry.getDouble("BossDamage"));
            row.putInt("revives", entry.getInt("Revives")); row.putInt("downs", entry.getInt("Downs"));
            rows.add(row);
        }
        result.put("rows", rows);
        for (String id : players.getAllKeys()) {
            var player = event.server().getPlayerList().getPlayer(UUID.fromString(id));
            if (player != null) complete(player, result);
        }
    }

    public static void completedBloodMoon(MinecraftServer server,EventState event) {
        var result=base(event.id(),event.dimension(),event.ends(),"",true);
        result.putBoolean("success",true);
        result.putString("source",event.id().toString());
        var rows=new ListTag();
        event.members().keySet().stream().sorted(Comparator
                .comparingInt((UUID id)->event.member(id).kills()).reversed()
                .thenComparing(Comparator.comparingDouble((UUID id)->event.member(id).damage()).reversed())
                .thenComparing(UUID::toString)).forEach(id->{
            var row=resultRow(event.member(id));
            row.putUUID("id",id);
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) row.putString("name",player.getGameProfile().getName());
            rows.add(row);
        });
        result.put("rows",rows);
        for(UUID id:event.members().keySet()) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) complete(player,result);
        }
    }
    public static void failedBloodMoon(MinecraftServer server,EventState event) {
        var result=base(event.id(),event.dimension(),event.ends(),"",true);
        result.putBoolean("success",false);
        result.putString("source",event.id().toString());
        var rows=new ListTag();
        for(UUID id:event.members().keySet()) {
            var row=resultRow(event.member(id));
            row.putUUID("id",id);
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) row.putString("name",player.getGameProfile().getName());
            rows.add(row);
        }
        result.put("rows",rows);
        for(UUID id:event.members().keySet()) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) complete(player,result);
        }
    }
    /** Explicit result-screen projection; persistent member metadata never enters the packet. */
    private static CompoundTag resultRow(EventMember member) {
        var row=new CompoundTag();
        row.putString("name",member.name());
        row.putInt("kills",member.kills());
        row.putDouble("damage",member.damage());
        row.putInt("revives",member.revives());
        row.putInt("waves",member.waves());
        return row;
    }
    private static void complete(ServerPlayer player,CompoundTag result) {
        SmpServerSession.get(player.server).player(player.getUUID()).combat = null;
        send(player,new Update(new CompoundTag(),false));
        sendResult(player,result);
    }
    private static ListTag bundles(ServerPlayer player,String source) {
        return PendingRewardContainer.bundles(player, source);
    }
    private static void sendResult(ServerPlayer player,CompoundTag result) {
        var snapshot=result.copy();
        snapshot.put("bundles",bundles(player,result.getString("source")));
        send(player,new Update(snapshot,true));
    }
    private static void send(ServerPlayer player,Update packet) {
        if(CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),packet);
    }
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SmpEnvironment.active(player))
            SmpServerSession.get(player.server).player(player.getUUID()).combat = null;
    }
    private CombatNetwork() {}
}

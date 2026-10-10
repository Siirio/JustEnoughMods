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
    private static final Map<UUID,CompoundTag> SENT=new HashMap<>();
    private static final Map<UUID,CompoundTag> RESULTS=new HashMap<>();
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
                    var player=context.get().getSender();
                    if(player!=null&&SmpEnvironment.active(player)&&SmpRequests.allow(player)) claim(player,packet.result());
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
    public static void claim(UUID result) { CHANNEL.sendToServer(new Claim(result)); }
    public static void vote(UUID event,boolean continueBattle) { CHANNEL.sendToServer(new Vote(event,continueBattle)); }
    private static void claim(ServerPlayer player,UUID resultId) {
        var result=RESULTS.get(player.getUUID());
        if(result==null || !result.getUUID("id").equals(resultId)) return;
        var ids=new ArrayList<UUID>();
        for(var tag:bundles(player,result.getString("source"))) ids.add(((CompoundTag)tag).getUUID("id"));
        if(!ids.isEmpty() && !PendingRewardContainer.claimBundles(player,ids))
            result.putString("error","jem.smp.result.no_space");
        result.putBoolean("refresh",true);
        sendResult(player,result);
        result.remove("error");
        result.remove("refresh");
    }
    public static void tick(MinecraftServer server) {
        if(server.getTickCount()%TICKS_PER_SECOND!=0) return;
        var visible=new HashSet<UUID>();
        for(var party:SmpData.get(server).all("parties")) {
            if(!party.getString("state").equals("ACTIVE") || !party.hasUUID("bossEntity")) continue;
            var dimension=ResourceLocation.tryParse(party.getString("dimension"));
            var level=dimension==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,dimension));
            if(level==null || !(level.getEntity(party.getUUID("bossEntity")) instanceof LivingEntity boss)) continue;
            var participants=HostedEncounterApi.status(boss);
            var snapshot=base(party,boss.getDisplayName().getString(),false);
            snapshot.putString("titleKey",boss.getType().getDescriptionId());
            if(party.hasUUID("eventId")) {
                var event=SmpData.get(server).find("events",party.getUUID("eventId"));
                if(event!=null) snapshot.putLong("ends",event.getLong("ends"));
            }
            var rows=new ListTag();
            var included=new HashSet<UUID>();
            for(var participant:participants) {
                included.add(participant.playerId());
                var player=server.getPlayerList().getPlayer(participant.playerId());
                var member=SmpRecords.members(party).getCompound(participant.playerId().toString());
                var row=playerRow(participant.playerId(),member,player,participant.state());
                if(!row.contains("remaining")) row.putLong("remaining",participant.remainingTicks()/TICKS_PER_SECOND);
                rows.add(row);
            }
            for(UUID id:SmpRecords.memberIds(party)) {
                var member=SmpRecords.members(party).getCompound(id.toString());
                if(!included.contains(id) && member.getBoolean("simulated")) rows.add(playerRow(id,member,null,"ALIVE"));
            }
            snapshot.put("rows",rows);
            for(var participant:participants) {
                var player=server.getPlayerList().getPlayer(participant.playerId());
                if(player!=null && player.level()==level && !player.isSpectator()) publish(player,snapshot,visible);
            }
        }
        for(var event:SmpData.get(server).all("events")) {
            if(!event.getString("state").equals("ACTIVE") || !event.getString("activity").equals("BLOOD_MOON") || !event.getBoolean("combatStarted")) continue;
            var snapshot=bloodSnapshot(server,event);
            for(UUID id:SmpRecords.memberIds(event)) {
                var player=server.getPlayerList().getPlayer(id);
                if(player!=null && new EventSession(event).accepted(id) && player.level().dimension().location().toString().equals(event.getString("dimension"))) publish(player,snapshot,visible);
            }
        }
        for(UUID id:List.copyOf(SENT.keySet())) if(!visible.contains(id)) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) send(player,new Update(new CompoundTag(),false));
            SENT.remove(id);
        }
    }

    public static void refreshBloodMoon(MinecraftServer server,CompoundTag event) {
        if(!event.getString("state").equals("ACTIVE")) return;
        CompoundTag snapshot=bloodSnapshot(server,event);
        for(UUID id:SmpRecords.memberIds(event)) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null && new EventSession(event).accepted(id)) {
                send(player,new Update(snapshot,false));
                SENT.put(id,snapshot.copy());
            }
        }
    }

    private static CompoundTag bloodSnapshot(MinecraftServer server,CompoundTag event) {
        var snapshot=base(event,"",true);
        snapshot.putInt("wave",event.getInt("wave"));
        snapshot.putInt("waves",BloodMoonWaves.WAVES);
        snapshot.putInt("remainingMobs",Math.max(0,event.getInt("waveTotal")-event.getInt("waveKilled")));
        var eventLevel=EventRegions.level(server,event);
        if(eventLevel!=null) for(var mobTag:event.getList("mobs",Tag.TAG_STRING)) {
            var mob=eventLevel.getEntity(UUID.fromString(mobTag.getAsString()));
            if(mob!=null && mob.getPersistentData().getString("jem:blood_category").equals("MINIBOSS")) {
                snapshot.putString("miniboss",mob.getDisplayName().getString());
                snapshot.putString("minibossKey",mob.getType().getDescriptionId());
                break;
            }
        }
        var rows=new ListTag();
        for(UUID id:SmpRecords.memberIds(event)) {
            var member=SmpRecords.members(event).getCompound(id.toString());
            var player=server.getPlayerList().getPlayer(id);
            rows.add(playerRow(id,member,player,member.getBoolean("accepted")?"ALIVE":"ELIMINATED"));
        }
        snapshot.put("rows",rows);
        BloodMoonVoting.snapshot(server,event,snapshot);
        return snapshot;
    }
    private static CompoundTag base(CompoundTag source,String title,boolean blood) {
        var result=new CompoundTag();
        result.putUUID("id",source.getUUID("id"));
        result.putString("dimension",source.getString("dimension"));
        result.putString("title",title);
        result.putBoolean("blood",blood);
        result.putLong("ends",source.getLong("ends"));
        return result;
    }
    private static CompoundTag playerRow(UUID id,CompoundTag member,ServerPlayer player,String state) {
        var row=new CompoundTag();
        row.putUUID("id",id);
        row.putString("name",player==null?member.getString("name"):player.getGameProfile().getName());
        boolean simulated=member.getBoolean("simulated");
        if(player==null && !simulated) state="DISCONNECTED";
        else if(player!=null && ReviveSupport.downed(player)) {state="DOWNED";row.putLong("remaining",ReviveSupport.remainingTicks(player)/TICKS_PER_SECOND);row.putBoolean("timerPaused",ReviveSupport.timerPaused(player));}
        else if(player!=null && (!player.isAlive() || player.isSpectator())) state="ELIMINATED";
        row.putString("state",state);
        row.putFloat("hp",simulated?member.getFloat("hp"):player==null?0:player.getHealth());
        row.putFloat("maxHp",simulated?member.getFloat("maxHp"):player==null?0:player.getMaxHealth());
        if(simulated) row.putInt("ping",member.getInt("ping"));
        return row;
    }
    private static void publish(ServerPlayer player,CompoundTag snapshot,Set<UUID> visible) {
        if(!visible.add(player.getUUID())) return;
        if(!snapshot.equals(SENT.get(player.getUUID()))) {
            send(player,new Update(snapshot,false));
            SENT.put(player.getUUID(),snapshot.copy());
        }
    }
    @SubscribeEvent public static void closed(HostedEncounterClosedEvent event) {
        var boss=event.boss();
        if(!SmpEnvironment.active(boss.getServer())) return;
        var result=new CompoundTag();
        result.putUUID("id",boss.getUUID());
        result.putString("source",boss.getStringUUID());
        result.putString("title",boss.getDisplayName().getString());
        result.putString("titleKey",boss.getType().getDescriptionId());
        result.putBoolean("success",event.success());
        result.put("rows",HostedEncounters.results(boss));
        for(var participant:HostedEncounterApi.status(boss)) {
            var player=boss.getServer().getPlayerList().getPlayer(participant.playerId());
            if(player!=null) complete(player,result);
        }
    }
    public static void completedBloodMoon(MinecraftServer server,CompoundTag event) {
        var result=base(event,"",true);
        result.putBoolean("success",true);
        result.putString("source",event.getUUID("id").toString());
        var rows=new ListTag();
        SmpRecords.memberIds(event).stream().sorted(Comparator
                .comparingInt((UUID id)->SmpRecords.members(event).getCompound(id.toString()).getInt("kills")).reversed()
                .thenComparing(Comparator.comparingDouble((UUID id)->SmpRecords.members(event).getCompound(id.toString()).getDouble("damage")).reversed())
                .thenComparing(UUID::toString)).forEach(id->{
            var row=SmpRecords.members(event).getCompound(id.toString()).copy();
            row.putUUID("id",id);
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) row.putString("name",player.getGameProfile().getName());
            rows.add(row);
        });
        result.put("rows",rows);
        for(UUID id:SmpRecords.memberIds(event)) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) complete(player,result);
        }
    }
    public static void failedBloodMoon(MinecraftServer server,CompoundTag event) {
        var result=base(event,"",true);
        result.putBoolean("success",false);
        result.putString("source",event.getUUID("id").toString());
        var rows=new ListTag();
        for(UUID id:SmpRecords.memberIds(event)) {
            var row=SmpRecords.members(event).getCompound(id.toString()).copy();
            row.putUUID("id",id);
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) row.putString("name",player.getGameProfile().getName());
            rows.add(row);
        }
        result.put("rows",rows);
        for(UUID id:SmpRecords.memberIds(event)) {
            var player=server.getPlayerList().getPlayer(id);
            if(player!=null) complete(player,result);
        }
    }
    private static void complete(ServerPlayer player,CompoundTag result) {
        RESULTS.put(player.getUUID(),result.copy());
        SENT.remove(player.getUUID());
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
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) { SENT.remove(event.getEntity().getUUID()); }
    public static void logout(ServerPlayer player) { SENT.remove(player.getUUID());RESULTS.remove(player.getUUID()); }
    public static void clear() { SENT.clear();RESULTS.clear(); }
    private CombatNetwork() {}
}

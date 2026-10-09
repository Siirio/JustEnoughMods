package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

public final class EventNetwork {
    private static final String PROTOCOL_VERSION="9";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("jem_server","events"),()->PROTOCOL_VERSION,PROTOCOL_VERSION::equals,PROTOCOL_VERSION::equals);
    public record Prompt(UUID id,String type,String title,String entityType,boolean raidAvailable,String state,List<String> occupants) {}
    public record Choice(UUID id,String type,String mode,boolean solo) {}
    public record Boundary(UUID id,String dimension,String type,int minX,int minY,int minZ,int maxX,int maxY,int maxZ,int color,boolean active,boolean passable) {}
    public record EventMob(int entityId,boolean bloodMoon) {}
    public record Beam(ResourceLocation dimension,net.minecraft.core.BlockPos position,int duration,float radius) {}
    public record Telegraph(UUID eventId,ResourceLocation dimension,AttackGeometry geometry,int duration,int color) {}
    public record AttackVfx(UUID eventId,ResourceLocation dimension,AttackGeometry geometry,int duration,int color) {}
    public record Zones(List<Boundary> bounds) {}
    public static void register() {
        CHANNEL.messageBuilder(Telegraph.class,5,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet,buffer)->writeAttack(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color(),buffer))
                .decoder(buffer->{AttackPacket packet=readAttack(buffer);return new Telegraph(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color());})
                .consumerMainThread((p,c)->{if(p.duration()>0&&p.duration()<=200) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventAttackRenderer.accept(p));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(AttackVfx.class,6,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet,buffer)->writeAttack(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color(),buffer))
                .decoder(buffer->{AttackPacket packet=readAttack(buffer);return new AttackVfx(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color());})
                .consumerMainThread((p,c)->{if(p.duration()>0&&p.duration()<=80) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventAttackRenderer.accept(p));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Beam.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeResourceLocation(p.dimension());b.writeBlockPos(p.position());b.writeVarInt(p.duration());b.writeFloat(p.radius());}).decoder(b->new Beam(b.readResourceLocation(),b.readBlockPos(),b.readVarInt(),b.readFloat())).consumerMainThread((p,c)->{if(p.duration()>0&&p.duration()<=40&&Float.isFinite(p.radius())&&p.radius()>=.1F&&p.radius()<=8F) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventMobRenderer.beam(p.dimension(),p.position(),p.duration(),p.radius()));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(EventMob.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeVarInt(p.entityId());b.writeBoolean(p.bloodMoon());}).decoder(b->new EventMob(b.readVarInt(),b.readBoolean())).consumerMainThread((p,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventMobRenderer.accept(p.entityId(),p.bloodMoon()));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Prompt.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeUUID(p.id());b.writeUtf(p.type(),32);b.writeUtf(p.title(),256);b.writeUtf(p.entityType(),128);b.writeBoolean(p.raidAvailable());b.writeUtf(p.state(),32);b.writeCollection(p.occupants(),(out,name)->out.writeUtf(name,64));}).decoder(b->new Prompt(b.readUUID(),b.readUtf(32),b.readUtf(256),b.readUtf(128),b.readBoolean(),b.readUtf(32),b.readList(in->in.readUtf(64)))).consumerMainThread((p,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventEntryScreen.open(p));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Choice.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->{b.writeUUID(p.id());b.writeUtf(p.type(),32);b.writeUtf(p.mode(),32);b.writeBoolean(p.solo());}).decoder(b->new Choice(b.readUUID(),b.readUtf(32),b.readUtf(32),b.readBoolean())).consumerMainThread((p,c)->{var player=c.get().getSender();if(player!=null&&SmpEnvironment.active(player)) choose(player,p);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Zones.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeCollection(p.bounds(),(out,z)->{out.writeUUID(z.id());out.writeUtf(z.dimension(),128);out.writeUtf(z.type(),32);out.writeInt(z.minX());out.writeInt(z.minY());out.writeInt(z.minZ());out.writeInt(z.maxX());out.writeInt(z.maxY());out.writeInt(z.maxZ());out.writeInt(z.color());out.writeBoolean(z.active());out.writeBoolean(z.passable());})).decoder(b->new Zones(b.readCollection(FriendlyByteBuf.limitValue(ArrayList::new,32),in->new Boundary(in.readUUID(),in.readUtf(128),in.readUtf(32),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readBoolean(),in.readBoolean())))).consumerMainThread((p,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventBoundaryRenderer.accept(p));c.get().setPacketHandled(true);}).add();
    }
    public static void eventMob(ServerPlayer player,net.minecraft.world.entity.Entity entity) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new EventMob(entity.getId(),entity.getPersistentData().hasUUID(BloodMoon.EVENT_ID))); }
    public static void beam(ServerPlayer player,net.minecraft.core.BlockPos position,int duration,float entityWidth) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Beam(player.level().dimension().location(),position,duration,Math.max(.25F,Math.min(8F,entityWidth*.4F)))); }
    public static void telegraph(ServerPlayer player,UUID eventId,AttackGeometry geometry,int duration,int color) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Telegraph(eventId,player.level().dimension().location(),geometry,duration,color)); }
    public static void attackVfx(ServerPlayer player,UUID eventId,AttackGeometry geometry,int duration,int color) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new AttackVfx(eventId,player.level().dimension().location(),geometry,duration,color)); }
    public static void prompt(ServerPlayer player,UUID id,String type,String title,String entityType) {
        boolean structure=type.equals("BOSS_STRUCTURE");
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Prompt(id,type,title,entityType,
                structure && StructureStaging.raidAvailable(player,id),structure?StructureStaging.arenaState(player,id):"FREE",
                structure?StructureStaging.occupants(player,id):List.of()));
    }
    public static void zones(ServerPlayer player,List<Boundary> bounds) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Zones(bounds)); }
    public static void select(Prompt prompt,String mode,boolean solo) { CHANNEL.sendToServer(new Choice(prompt.id(),prompt.type(),mode,solo)); }
    private static void choose(ServerPlayer player,Choice choice) {
        try {
            if(choice.type().equals("BOSS_STRUCTURE")) { StructureStaging.choose(player,choice.id(),choice.mode(),choice.solo());return; }
            if(choice.type().equals("BOSS_FIGHT")) {
                var entity=player.serverLevel().getEntity(choice.id());
                SmpRecords.require(entity instanceof net.minecraft.world.entity.LivingEntity,"unavailable");
                var boss=(net.minecraft.world.entity.LivingEntity)entity;
                SmpRecords.require(BossHostingPrompt.canOffer(player,boss),"unavailable");
                SmpRuntime.host(player,boss);
                if(choice.solo()) {
                    var party=SmpData.get(player.server).all("parties").stream().filter(p->p.hasUUID("bossEntity") && p.getUUID("bossEntity").equals(choice.id()) && p.getUUID("owner").equals(player.getUUID())).findFirst().orElseThrow();
                    party.putBoolean("solo",true);party.remove("invitations");SmpRecords.members(party).getCompound(player.getStringUUID()).putBoolean("ready",true);HostedParties.start(player,party);SmpData.get(player.server).changed(party);
                }
                return;
            }
            var row=SmpData.get(player.server).find("events",choice.id());
            SmpRecords.require(row!=null && row.getString("activity").equals(choice.type()),"unavailable");
            if(choice.type().equals("BLOOD_MOON")) EventParty.join(player,row,choice.solo());
            else if(choice.type().equals("BOSS_RAID")) EventParty.join(player,row,choice.solo());
        } catch(IllegalArgumentException failure) { player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.smp.error."+failure.getMessage())); }
    }

    private static void writeAttack(UUID eventId,ResourceLocation dimension,AttackGeometry geometry,int duration,int color,FriendlyByteBuf buffer) {
        buffer.writeUUID(eventId);buffer.writeResourceLocation(dimension);buffer.writeEnum(geometry.type());
        buffer.writeInt(geometry.minX());buffer.writeInt(geometry.maxX());buffer.writeInt(geometry.minZ());buffer.writeInt(geometry.maxZ());
        buffer.writeDouble(geometry.originX());buffer.writeDouble(geometry.originZ());buffer.writeDouble(geometry.secondX());buffer.writeDouble(geometry.secondZ());
        buffer.writeDouble(geometry.targetX());buffer.writeDouble(geometry.targetZ());buffer.writeDouble(geometry.directionX());buffer.writeDouble(geometry.directionZ());
        buffer.writeDouble(geometry.radius());buffer.writeDouble(geometry.width());buffer.writeDouble(geometry.length());buffer.writeDouble(geometry.startAngle());buffer.writeDouble(geometry.endAngle());
        buffer.writeLong(geometry.startTick());buffer.writeVarLong(geometry.windupDuration());buffer.writeVarLong(geometry.activeDuration());
        buffer.writeDouble(geometry.progress());buffer.writeVarInt(geometry.variant());buffer.writeBoolean(geometry.reverse());
        buffer.writeVarInt(duration);buffer.writeInt(color);
    }

    private static AttackPacket readAttack(FriendlyByteBuf buffer) {
        UUID eventId=buffer.readUUID();ResourceLocation dimension=buffer.readResourceLocation();AttackGeometry.Type type=buffer.readEnum(AttackGeometry.Type.class);
        int minX=buffer.readInt(),maxX=buffer.readInt(),minZ=buffer.readInt(),maxZ=buffer.readInt();
        double originX=buffer.readDouble(),originZ=buffer.readDouble(),secondX=buffer.readDouble(),secondZ=buffer.readDouble(),targetX=buffer.readDouble(),targetZ=buffer.readDouble();
        double directionX=buffer.readDouble(),directionZ=buffer.readDouble(),radius=buffer.readDouble(),width=buffer.readDouble(),length=buffer.readDouble(),startAngle=buffer.readDouble(),endAngle=buffer.readDouble();
        long startTick=buffer.readLong(),windup=buffer.readVarLong(),active=buffer.readVarLong();double progress=buffer.readDouble();
        int variant=buffer.readVarInt();boolean reverse=buffer.readBoolean();
        AttackGeometry geometry=new AttackGeometry(type,minX,maxX,minZ,maxZ,originX,originZ,secondX,secondZ,targetX,targetZ,
                directionX,directionZ,radius,width,length,startAngle,endAngle,startTick,windup,active,progress,variant,reverse);
        return new AttackPacket(eventId,dimension,geometry,buffer.readVarInt(),buffer.readInt());
    }

    private record AttackPacket(UUID eventId,ResourceLocation dimension,AttackGeometry geometry,int duration,int color) {}
    private EventNetwork() {}
}

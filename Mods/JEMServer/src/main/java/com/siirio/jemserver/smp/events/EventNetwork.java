package com.siirio.jemserver.smp.events;

import java.util.function.BiConsumer;
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
    private static final int MAX_PROMPT_OCCUPANTS = 256;
    private static final int MAX_ATTACK_SPAN = 512;
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("jem_server","events"),()->PROTOCOL_VERSION,PROTOCOL_VERSION::equals,PROTOCOL_VERSION::equals);

    public static void register(BiConsumer<ServerPlayer, EventChoice> actions) {
        CHANNEL.messageBuilder(EventTelegraph.class,5,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet,buffer)->writeAttack(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color(),buffer))
                .decoder(buffer->{EventAttackPacket packet=readAttack(buffer);return new EventTelegraph(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color());})
                .consumerMainThread((p,c)->{if(p.duration()>0&&p.duration()<=200) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventAttackRenderer.accept(p));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(EventAttackVfx.class,6,NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet,buffer)->writeAttack(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color(),buffer))
                .decoder(buffer->{EventAttackPacket packet=readAttack(buffer);return new EventAttackVfx(packet.eventId(),packet.dimension(),packet.geometry(),packet.duration(),packet.color());})
                .consumerMainThread((p,c)->{if(p.duration()>0&&p.duration()<=80) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventAttackRenderer.accept(p));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(EventBeam.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeResourceLocation(p.dimension());b.writeBlockPos(p.position());b.writeVarInt(p.duration());b.writeFloat(p.radius());}).decoder(b->new EventBeam(b.readResourceLocation(),b.readBlockPos(),b.readVarInt(),b.readFloat())).consumerMainThread((p,c)->{if(p.duration()>0&&p.duration()<=40&&Float.isFinite(p.radius())&&p.radius()>=.1F&&p.radius()<=8F) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventMobRenderer.beam(p.dimension(),p.position(),p.duration(),p.radius()));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(EventMobPacket.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeVarInt(p.entityId());b.writeBoolean(p.bloodMoon());}).decoder(b->new EventMobPacket(b.readVarInt(),b.readBoolean())).consumerMainThread((p,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventMobRenderer.accept(p.entityId(),p.bloodMoon()));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(EventPrompt.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeUUID(p.id());b.writeUtf(p.type(),32);b.writeUtf(p.title(),256);b.writeUtf(p.entityType(),128);b.writeBoolean(p.raidAvailable());b.writeUtf(p.state(),32);b.writeCollection(p.occupants(),(out,name)->out.writeUtf(name,64));}).decoder(b->new EventPrompt(b.readUUID(),b.readUtf(32),b.readUtf(256),b.readUtf(128),b.readBoolean(),b.readUtf(32),b.readCollection(FriendlyByteBuf.limitValue(ArrayList::new,MAX_PROMPT_OCCUPANTS),in->in.readUtf(64)))).consumerMainThread((p,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventEntryScreen.open(p));c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(EventChoice.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->{b.writeUUID(p.id());b.writeUtf(p.type().name(),32);b.writeUtf(p.mode().name(),32);b.writeBoolean(p.solo());}).decoder(b->new EventChoice(b.readUUID(),EventEntryKind.valueOf(b.readUtf(32)),EventEntryKind.valueOf(b.readUtf(32)),b.readBoolean())).consumerMainThread((p,c)->{var player=c.get().getSender();if(player!=null&&player.server.isDedicatedServer()) actions.accept(player,p);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(EventZones.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->b.writeCollection(p.bounds(),(out,z)->{out.writeUUID(z.id());out.writeUtf(z.dimension(),128);out.writeUtf(z.type(),32);out.writeInt(z.minX());out.writeInt(z.minY());out.writeInt(z.minZ());out.writeInt(z.maxX());out.writeInt(z.maxY());out.writeInt(z.maxZ());out.writeInt(z.color());out.writeBoolean(z.active());out.writeBoolean(z.passable());})).decoder(b->new EventZones(b.readCollection(FriendlyByteBuf.limitValue(ArrayList::new,32),in->new EventBoundary(in.readUUID(),in.readUtf(128),in.readUtf(32),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readInt(),in.readBoolean(),in.readBoolean())))).consumerMainThread((p,c)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.siirio.jemserver.client.smp.EventBoundaryRenderer.accept(p));c.get().setPacketHandled(true);}).add();
    }
    public static void eventMob(ServerPlayer player,net.minecraft.world.entity.Entity entity) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new EventMobPacket(entity.getId(),entity.getPersistentData().hasUUID(BloodMoon.EVENT_ID))); }
    public static void beam(ServerPlayer player,net.minecraft.core.BlockPos position,int duration,float entityWidth) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new EventBeam(player.level().dimension().location(),position,duration,Math.max(.25F,Math.min(8F,entityWidth*.4F)))); }
    public static void telegraph(ServerPlayer player,UUID eventId,AttackGeometry geometry,int duration,int color) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new EventTelegraph(eventId,player.level().dimension().location(),geometry,duration,color)); }
    public static void attackVfx(ServerPlayer player,UUID eventId,AttackGeometry geometry,int duration,int color) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new EventAttackVfx(eventId,player.level().dimension().location(),geometry,duration,color)); }
    public static void prompt(ServerPlayer player,UUID id,String type,String title,String entityType) {
        boolean structure=type.equals("BOSS_STRUCTURE");
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new EventPrompt(id,type,title,entityType,
                structure && StructureStaging.raidAvailable(player,id),structure?StructureStaging.arenaState(player,id):"FREE",
                structure?StructureStaging.occupants(player,id).stream().limit(MAX_PROMPT_OCCUPANTS).toList():List.of()));
    }
    public static void zones(ServerPlayer player,List<EventBoundary> bounds) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new EventZones(bounds)); }
    public static void select(EventPrompt prompt,String mode,boolean solo) { CHANNEL.sendToServer(new EventChoice(prompt.id(),EventEntryKind.valueOf(prompt.type()),EventEntryKind.valueOf(mode),solo)); }
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

    private static EventAttackPacket readAttack(FriendlyByteBuf buffer) {
        UUID eventId=buffer.readUUID();ResourceLocation dimension=buffer.readResourceLocation();AttackGeometry.Type type=buffer.readEnum(AttackGeometry.Type.class);
        int minX=buffer.readInt(),maxX=buffer.readInt(),minZ=buffer.readInt(),maxZ=buffer.readInt();
        double originX=buffer.readDouble(),originZ=buffer.readDouble(),secondX=buffer.readDouble(),secondZ=buffer.readDouble(),targetX=buffer.readDouble(),targetZ=buffer.readDouble();
        double directionX=buffer.readDouble(),directionZ=buffer.readDouble(),radius=buffer.readDouble(),width=buffer.readDouble(),length=buffer.readDouble(),startAngle=buffer.readDouble(),endAngle=buffer.readDouble();
        long startTick=buffer.readLong(),windup=buffer.readVarLong(),active=buffer.readVarLong();double progress=buffer.readDouble();
        int variant=buffer.readVarInt();boolean reverse=buffer.readBoolean();
        long spanX = (long) maxX - minX + 1, spanZ = (long) maxZ - minZ + 1;
        if (spanX <= 0 || spanZ <= 0 || spanX > MAX_ATTACK_SPAN || spanZ > MAX_ATTACK_SPAN
                || maxX == Integer.MAX_VALUE || maxZ == Integer.MAX_VALUE || windup < 0 || active < 0)
            throw new io.netty.handler.codec.DecoderException("Invalid event attack bounds");
        for (double value : new double[] {originX, originZ, secondX, secondZ, targetX, targetZ,
                directionX, directionZ, radius, width, length, startAngle, endAngle, progress})
            if (!Double.isFinite(value)) throw new io.netty.handler.codec.DecoderException("Invalid event attack geometry");
        AttackGeometry geometry=new AttackGeometry(type,minX,maxX,minZ,maxZ,originX,originZ,secondX,secondZ,targetX,targetZ,
                directionX,directionZ,radius,width,length,startAngle,endAngle,startTick,windup,active,progress,variant,reverse);
        return new EventAttackPacket(eventId,dimension,geometry,buffer.readVarInt(),buffer.readInt());
    }

    private EventNetwork() {}
}

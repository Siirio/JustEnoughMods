package com.siirio.jemsettlements.network;

import net.minecraft.network.FriendlyByteBuf;

/** Quote and bounded server-observed stats; client attributes may omit non-syncable modifiers. */
public record HireQuotePacket(int entityId,long cost,long wage,boolean coins,boolean affordable,String reason,Stats stats) {
    public record Stats(float health,float maxHealth,int level,double attack,int kills,double speed,int xp,int armor,float morale) { }
    static void encode(HireQuotePacket p,FriendlyByteBuf b) {
        b.writeVarInt(p.entityId);b.writeLong(p.cost);b.writeLong(p.wage);b.writeBoolean(p.coins);b.writeBoolean(p.affordable);b.writeUtf(p.reason,96);
        Stats s=p.stats;b.writeFloat(s.health);b.writeFloat(s.maxHealth);b.writeVarInt(s.level);b.writeDouble(s.attack);b.writeVarInt(s.kills);b.writeDouble(s.speed);b.writeVarInt(s.xp);b.writeVarInt(s.armor);b.writeFloat(s.morale);
    }
    static HireQuotePacket decode(FriendlyByteBuf b) {
        int id=b.readVarInt();long cost=b.readLong(),wage=b.readLong();boolean coins=b.readBoolean(),affordable=b.readBoolean();String reason=b.readUtf(96);
        return new HireQuotePacket(id,cost,wage,coins,affordable,reason,new Stats(b.readFloat(),b.readFloat(),b.readVarInt(),b.readDouble(),b.readVarInt(),b.readDouble(),b.readVarInt(),b.readVarInt(),b.readFloat()));
    }
}

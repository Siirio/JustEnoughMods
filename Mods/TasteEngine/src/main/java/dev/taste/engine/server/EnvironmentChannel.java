package dev.taste.engine.server;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public final class EnvironmentChannel {
    public static final int PROTOCOL=1;
    public void encode(EnvironmentMessages.Snapshot message,FriendlyByteBuf buffer){buffer.writeVarLong(message.revision());buffer.writeVarInt(message.zones().size());for(var zone:message.zones()){buffer.writeUtf(zone.id(),128);buffer.writeDouble(zone.x());buffer.writeDouble(zone.y());buffer.writeDouble(zone.z());buffer.writeDouble(zone.radius());buffer.writeVarInt(zone.priority());buffer.writeFloat(zone.fog());buffer.writeFloat(zone.red());buffer.writeFloat(zone.green());buffer.writeFloat(zone.blue());}}
    public EnvironmentMessages.Snapshot decode(FriendlyByteBuf buffer,int maximumZones){long revision=buffer.readVarLong();int count=buffer.readVarInt();if(count<0||count>maximumZones)throw new IllegalArgumentException("Environment zone count exceeds limit");List<EnvironmentMessages.Zone> zones=new ArrayList<>();for(int i=0;i<count;i++)zones.add(new EnvironmentMessages.Zone(buffer.readUtf(128),buffer.readDouble(),buffer.readDouble(),buffer.readDouble(),buffer.readDouble(),buffer.readVarInt(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat()));return new EnvironmentMessages.Snapshot(revision,zones);}
}

package dev.taste.engine.server;

import dev.taste.engine.server.EnvironmentMessages.Delta;
import dev.taste.engine.server.EnvironmentMessages.Effect;
import dev.taste.engine.server.EnvironmentMessages.Hello;
import dev.taste.engine.server.EnvironmentMessages.Preference;
import dev.taste.engine.server.EnvironmentMessages.Profile;
import dev.taste.engine.server.EnvironmentMessages.Pulse;
import dev.taste.engine.server.EnvironmentMessages.Snapshot;
import dev.taste.engine.server.EnvironmentMessages.Zone;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Play-only optional channel. No packet is sent to an endpoint that lacks this channel. */
public final class EnvironmentChannel {
    public static final int PROTOCOL = 1;
    private static final String VERSION = Integer.toString(PROTOCOL);
    private static final int FEATURES = EnvironmentMessages.ZONES | EnvironmentMessages.PULSES;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("taste", "environment"),
            () -> VERSION, NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));

    private EnvironmentChannel() { }

    static void register() {
        CHANNEL.registerMessage(0, Hello.class, EnvironmentChannel::writeHello,
                bounded(EnvironmentChannel::readHello), EnvironmentChannel::hello, Optional.empty());
        CHANNEL.registerMessage(1, Snapshot.class, EnvironmentChannel::writeSnapshot,
                bounded(EnvironmentChannel::readSnapshot), EnvironmentChannel::snapshot, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, Delta.class, EnvironmentChannel::writeDelta,
                bounded(EnvironmentChannel::readDelta), EnvironmentChannel::delta, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, Pulse.class, EnvironmentChannel::writePulse,
                bounded(EnvironmentChannel::readPulse), EnvironmentChannel::pulse, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, Preference.class, (message, buffer) -> buffer.writeBoolean(message.acceptEnvironment()),
                bounded(buffer -> new Preference(buffer.readBoolean())), EnvironmentChannel::preference, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void announce(Connection connection, boolean accept) {
        if (!CHANNEL.isRemotePresent(connection)) return;
        CHANNEL.sendTo(new Hello(PROTOCOL, FEATURES), connection, NetworkDirection.PLAY_TO_SERVER);
        preference(connection, accept);
    }

    public static void preference(Connection connection, boolean accept) {
        if (CHANNEL.isRemotePresent(connection))
            CHANNEL.sendTo(new Preference(accept), connection, NetworkDirection.PLAY_TO_SERVER);
    }

    static void announce(ServerPlayer player) { send(player, new Hello(PROTOCOL, FEATURES)); }

    static void send(ServerPlayer player, Object message) {
        Connection connection = player.connection.connection;
        if (CHANNEL.isRemotePresent(connection)) CHANNEL.sendTo(message, connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void hello(Hello message, Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context context = source.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ServerPlayer sender = context.getSender();
            if (sender != null && message.protocol() == PROTOCOL && (message.features() & EnvironmentMessages.ZONES) != 0)
                context.enqueueWork(() -> EnvironmentServerEvents.hello(sender, message.features()));
        } else if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> ClientEnvironmentState.hello(context.getNetworkManager(), message));
        }
        context.setPacketHandled(true);
    }

    private static void preference(Preference message, Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context context = source.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) context.enqueueWork(() -> EnvironmentServerEvents.preference(sender, message.acceptEnvironment()));
        context.setPacketHandled(true);
    }

    private static void snapshot(Snapshot message, Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context context = source.get();
        context.enqueueWork(() -> ClientEnvironmentState.snapshot(context.getNetworkManager(), message));
        context.setPacketHandled(true);
    }

    private static void delta(Delta message, Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context context = source.get();
        context.enqueueWork(() -> ClientEnvironmentState.delta(context.getNetworkManager(), message));
        context.setPacketHandled(true);
    }

    private static void pulse(Pulse message, Supplier<NetworkEvent.Context> source) {
        NetworkEvent.Context context = source.get();
        context.enqueueWork(() -> ClientEnvironmentState.pulse(context.getNetworkManager(), message));
        context.setPacketHandled(true);
    }

    private static <T> Function<FriendlyByteBuf, T> bounded(Function<FriendlyByteBuf, T> read) {
        return buffer -> {
            if (buffer.readableBytes() > EnvironmentMessages.MAX_PAYLOAD) return null;
            try {
                T result = read.apply(buffer);
                return buffer.isReadable() ? null : result;
            } catch (RuntimeException malformed) {
                // Forge's indexed codec discards a null decode before calling the packet handler.
                return null;
            }
        };
    }

    private static void writeHello(Hello message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.protocol());
        buffer.writeVarInt(message.features());
    }

    private static Hello readHello(FriendlyByteBuf buffer) { return new Hello(buffer.readVarInt(), buffer.readVarInt()); }

    private static void writeSnapshot(Snapshot message, FriendlyByteBuf buffer) {
        header(buffer, message.dimension(), message.generation(), message.worldTime());
        writeZones(buffer, message.zones());
    }

    private static Snapshot readSnapshot(FriendlyByteBuf buffer) {
        return new Snapshot(dimension(buffer), buffer.readVarLong(), buffer.readVarLong(),
                readZones(buffer, EnvironmentMessages.MAX_ZONES));
    }

    private static void writeDelta(Delta message, FriendlyByteBuf buffer) {
        header(buffer, message.dimension(), message.generation(), message.worldTime());
        buffer.writeVarLong(message.previousGeneration());
        buffer.writeVarInt(message.removed().size());
        message.removed().forEach(id -> buffer.writeUtf(id, EnvironmentMessages.MAX_ZONE_ID));
        writeZones(buffer, message.zones());
    }

    private static Delta readDelta(FriendlyByteBuf buffer) {
        ResourceLocation dimension = dimension(buffer);
        long generation = buffer.readVarLong();
        long time = buffer.readVarLong();
        long previous = buffer.readVarLong();
        int count = count(buffer, EnvironmentMessages.MAX_CHANGES);
        List<String> removed = new ArrayList<>(count);
        for (int index = 0; index < count; index++) removed.add(buffer.readUtf(EnvironmentMessages.MAX_ZONE_ID));
        return new Delta(dimension, generation, time, previous, removed, readZones(buffer, EnvironmentMessages.MAX_CHANGES - count));
    }

    private static void writePulse(Pulse message, FriendlyByteBuf buffer) {
        writeDimension(buffer, message.dimension());
        buffer.writeByte(message.effect().ordinal());
        buffer.writeInt(message.x());
        buffer.writeInt(message.y());
        buffer.writeInt(message.z());
        buffer.writeVarInt(message.duration());
        buffer.writeFloat(message.intensity());
        buffer.writeLong(message.seed());
    }

    private static Pulse readPulse(FriendlyByteBuf buffer) {
        return new Pulse(dimension(buffer), value(buffer, Effect.values()), buffer.readInt(), buffer.readInt(),
                buffer.readInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readLong());
    }

    private static void header(FriendlyByteBuf buffer, ResourceLocation dimension, long generation, long worldTime) {
        writeDimension(buffer, dimension);
        buffer.writeVarLong(generation);
        buffer.writeVarLong(worldTime);
    }

    private static void writeDimension(FriendlyByteBuf buffer, ResourceLocation dimension) {
        buffer.writeUtf(dimension.toString(), EnvironmentMessages.MAX_DIMENSION_ID);
    }

    private static ResourceLocation dimension(FriendlyByteBuf buffer) {
        return new ResourceLocation(buffer.readUtf(EnvironmentMessages.MAX_DIMENSION_ID));
    }

    private static void writeZones(FriendlyByteBuf buffer, List<Zone> zones) {
        buffer.writeVarInt(zones.size());
        for (Zone zone : zones) {
            buffer.writeUtf(zone.id(), EnvironmentMessages.MAX_ZONE_ID);
            buffer.writeByte(zone.profile().ordinal());
            buffer.writeInt(zone.minX());
            buffer.writeInt(zone.minY());
            buffer.writeInt(zone.minZ());
            buffer.writeInt(zone.maxX());
            buffer.writeInt(zone.maxY());
            buffer.writeInt(zone.maxZ());
            buffer.writeByte(zone.priority());
            buffer.writeFloat(zone.fade());
            buffer.writeFloat(zone.fog());
            buffer.writeFloat(zone.red());
            buffer.writeFloat(zone.green());
            buffer.writeFloat(zone.blue());
            buffer.writeVarLong(zone.expiresAt());
        }
    }

    private static List<Zone> readZones(FriendlyByteBuf buffer, int maximum) {
        int count = count(buffer, maximum);
        List<Zone> zones = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            zones.add(new Zone(buffer.readUtf(EnvironmentMessages.MAX_ZONE_ID), value(buffer, Profile.values()),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                    buffer.readUnsignedByte(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readVarLong()));
        }
        return zones;
    }

    private static int count(FriendlyByteBuf buffer, int maximum) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) throw new IllegalArgumentException("Environment entry count exceeds its limit");
        return count;
    }

    private static <T> T value(FriendlyByteBuf buffer, T[] entries) {
        int index = buffer.readUnsignedByte();
        if (index >= entries.length) throw new IllegalArgumentException("Unknown environment enum value");
        return entries[index];
    }
}

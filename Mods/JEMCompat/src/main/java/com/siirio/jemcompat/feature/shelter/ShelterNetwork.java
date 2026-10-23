package com.siirio.jemcompat.feature.shelter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ShelterNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("jemcompat", "shelters"), () -> "1", "1"::equals, "1"::equals);
    public record Action(int menu, UUID nonce, int revision, UUID pet, boolean bind) { }
    public record Snapshot(int menu, UUID nonce, int revision, boolean occupied, List<ShelterMenu.Row> rows) { }
    private ShelterNetwork() { }
    public static void register() {
        CHANNEL.messageBuilder(Action.class, 0, NetworkDirection.PLAY_TO_SERVER).encoder((message, buffer) -> {
            buffer.writeVarInt(message.menu); buffer.writeUUID(message.nonce); buffer.writeVarInt(message.revision); buffer.writeUUID(message.pet); buffer.writeBoolean(message.bind);
        }).decoder(buffer -> new Action(buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean()))
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof ShelterMenu menu && menu.containerId == message.menu) menu.action(message);
                }).add();
        CHANNEL.messageBuilder(Snapshot.class, 1, NetworkDirection.PLAY_TO_CLIENT).encoder(ShelterNetwork::encode).decoder(ShelterNetwork::decode)
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.siirio.jemcompat.client.shelter.ShelterScreen.receive(message))).add();
    }
    public static void action(Action message) { CHANNEL.sendToServer(message); }
    public static void snapshot(ServerPlayer player, Snapshot message) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message); }
    private static void encode(Snapshot message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.menu); buffer.writeUUID(message.nonce); buffer.writeVarInt(message.revision); buffer.writeBoolean(message.occupied);
        buffer.writeVarInt(message.rows.size());
        for (ShelterMenu.Row row : message.rows) {
            buffer.writeUUID(row.id()); buffer.writeUtf(row.species(), 128); buffer.writeUtf(row.name(), 256); buffer.writeVarInt(row.flags()); buffer.writeUtf(row.blocked(), 64);
        }
    }
    private static Snapshot decode(FriendlyByteBuf buffer) {
        int menu = buffer.readVarInt(); UUID nonce = buffer.readUUID(); int revision = buffer.readVarInt(); boolean occupied = buffer.readBoolean();
        int count = buffer.readVarInt(); if (count < 0 || count > 16384) throw new IllegalArgumentException("Invalid companion list size");
        List<ShelterMenu.Row> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) rows.add(new ShelterMenu.Row(buffer.readUUID(), buffer.readUtf(128), buffer.readUtf(256), buffer.readVarInt(), buffer.readUtf(64)));
        return new Snapshot(menu, nonce, revision, occupied, List.copyOf(rows));
    }
}

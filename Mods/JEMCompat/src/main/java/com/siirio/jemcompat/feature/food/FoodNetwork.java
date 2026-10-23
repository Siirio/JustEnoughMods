package com.siirio.jemcompat.feature.food;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Server-to-client read-only dietary progress; no client food mutation packet exists. */
public final class FoodNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jemcompat", "food"))
            .networkProtocolVersion(() -> "1").clientAcceptedVersions("1"::equals).serverAcceptedVersions("1"::equals).simpleChannel();
    private record Progress(int count) {}
    private FoodNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(Progress.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.count()))
                .decoder(buffer -> new Progress(Math.max(0, Math.min(4096, buffer.readVarInt()))))
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.siirio.jemcompat.client.food.FoodTooltips.progress(packet.count())))
                .add();
    }
    static void send(ServerPlayer player, int count) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Progress(count));
    }
}

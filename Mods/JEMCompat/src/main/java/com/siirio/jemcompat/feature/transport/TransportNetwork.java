package com.siirio.jemcompat.feature.transport;

import com.siirio.jemcompat.JEMCompat;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TransportNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JEMCompat.MOD_ID, "transport"), () -> VERSION, VERSION::equals, VERSION::equals);

    private TransportNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(TransportNetwork::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CHANNEL.messageBuilder(BurdenStatus.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BurdenStatus::encode).decoder(BurdenStatus::decode)
                .consumerMainThread(BurdenStatus::handle).add());
    }

    static void sendBurden(ServerPlayer player, int occupiedSlots, int totalSlots, double reduction) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new BurdenStatus(occupiedSlots, totalSlots, (int) Math.round(reduction * 1000.0)));
    }

    public record BurdenStatus(int occupiedSlots, int totalSlots, int reductionTenths) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(occupiedSlots);
            buffer.writeVarInt(totalSlots);
            buffer.writeVarInt(reductionTenths);
        }

        private static BurdenStatus decode(FriendlyByteBuf buffer) {
            return new BurdenStatus(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }

        private void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> com.siirio.jemcompat.client.TransportBurdenClient.accept(this)));
            context.setPacketHandled(true);
        }
    }
}

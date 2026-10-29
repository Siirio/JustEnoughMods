package com.siirio.jemcompat.feature.kaleidoscope;

import com.siirio.jemcompat.JEMCompat;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
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

public final class VoidWalkerNetwork {
    private static final String VERSION="1";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JEMCompat.MOD_ID,"void_walker"),()->VERSION,
            VoidWalkerNetwork::acceptVersion,VoidWalkerNetwork::acceptVersion);

    private VoidWalkerNetwork() { }
    private static boolean acceptVersion(String version) {
        return VERSION.equals(version)||NetworkRegistry.ABSENT.equals(version)||NetworkRegistry.ACCEPTVANILLA.equals(version);
    }
    public static void register(IEventBus modBus) { modBus.addListener(VoidWalkerNetwork::setup); }
    private static void setup(FMLCommonSetupEvent event) { event.enqueueWork(()-> {
        CHANNEL.messageBuilder(Toggle.class,0,NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet,buffer)->{}).decoder(buffer->new Toggle()).consumerMainThread(Toggle::handle).add();
        CHANNEL.messageBuilder(Status.class,1,NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Status::encode).decoder(Status::decode).consumerMainThread(Status::handle).add();
    }); }
    public static boolean supported(Connection connection) { return connection!=null&&CHANNEL.isRemotePresent(connection); }
    public static void toggle() { CHANNEL.sendToServer(new Toggle()); }
    static void status(ServerPlayer player,boolean enabled,boolean equipped,boolean announce) {
        if(supported(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Status(enabled,equipped,announce));
    }
    private record Toggle() {
        private void handle(Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();ServerPlayer player=context.getSender();
            if(player!=null) {
                var result=VoidWalkerEvents.toggle(player);
                status(player,result.enabled(),result.equipped(),true);
            }
            context.setPacketHandled(true);
        }
    }
    public record Status(boolean enabled,boolean equipped,boolean announce) {
        private static Status decode(FriendlyByteBuf buffer) { return new Status(buffer.readBoolean(),buffer.readBoolean(),buffer.readBoolean()); }
        private void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(enabled);buffer.writeBoolean(equipped);buffer.writeBoolean(announce); }
        private void handle(Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();
            context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    ()->()->com.siirio.jemcompat.client.VoidWalkerClient.accept(this)));
            context.setPacketHandled(true);
        }
    }
}

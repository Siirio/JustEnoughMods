package com.siirio.jemserver.smp.fishing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class FishingOddsNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jem_server", "fishing_odds"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).simpleChannel();
    public record ProfileSync(String json) {}
    public static void register() {
        CHANNEL.messageBuilder(ProfileSync.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder((packet, buffer) -> buffer.writeUtf(packet.json(), 131072))
                .decoder(buffer -> new ProfileSync(buffer.readUtf(131072))).consumerMainThread((packet, context) -> FishingProfiles.accept(packet.json())).add();
    }
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ProfileSync(FishingProfiles.encode()));
    }
    private FishingOddsNetwork() {}
}

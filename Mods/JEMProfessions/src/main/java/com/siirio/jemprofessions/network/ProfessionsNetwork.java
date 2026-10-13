package com.siirio.jemprofessions.network;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.JemProfessions;
import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Set;

public final class ProfessionsNetwork {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JemProfessions.MOD_ID, "main"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).simpleChannel();
    private ProfessionsNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ProfessionTogglePacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfessionTogglePacket::encode).decoder(ProfessionTogglePacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) { ProfessionApi.toggle(player, packet.profession(), packet.nodeId()); sendStatus(player, true); }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ProfessionStatusPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProfessionStatusPacket::encode).decoder(ProfessionStatusPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemprofessions.client.ProfessionClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ArchitectHotbarSlotPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ArchitectHotbarSlotPacket::encode).decoder(ArchitectHotbarSlotPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    int slot = packet.logicalSlot();
                    if (player != null && slot >= 9 && slot <= 12 && ProfessionApi.level(player, ProfessionId.ARCHITECT) >= 20
                            && net.minecraftforge.fml.ModList.get().isLoaded("tinyinv")) {
                        player.getInventory().selected = slot + 18;
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void toggle(ProfessionId profession, String nodeId) { CHANNEL.sendToServer(new ProfessionTogglePacket(profession, nodeId)); }
    public static void selectArchitectSlot(int logicalSlot) { CHANNEL.sendToServer(new ArchitectHotbarSlotPacket(logicalSlot)); }

    public static void sendStatus(ServerPlayer player, boolean open) {
        var principal = IdentityPolicy.resolve(player);
        long[] xp = new long[ProfessionId.values().length];
        java.util.Set<String> enabled = new java.util.LinkedHashSet<>();
        principal.ifPresent(value -> {
            var record = ProfessionSavedData.get(player.getServer()).findOrEmpty(value.accountId());
            for (ProfessionId id : ProfessionId.values()) xp[id.ordinal()] = record.xp(id);
            enabled.addAll(record.enabledKeys());
        });
        if (CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ProfessionStatusPacket(open, principal.isPresent(), xp, enabled));
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) sendStatus(player, false); }
}

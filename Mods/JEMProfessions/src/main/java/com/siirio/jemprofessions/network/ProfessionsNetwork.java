package com.siirio.jemprofessions.network;

import com.siirio.jemprofessions.JemProfessions;
import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ProfessionsNetwork {
    private static final String VERSION = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JemProfessions.MOD_ID, "main"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(VERSION::equals).serverAcceptedVersions(VERSION::equals).simpleChannel();
    private ProfessionsNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ArchitectHotbarSlotPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ArchitectHotbarSlotPacket::encode).decoder(ArchitectHotbarSlotPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    int slot = packet.logicalSlot();
                    if (player != null && slot >= 9 && slot <= 12 && ProfessionApi.enabled(player, ProfessionId.ARCHITECT, "architect_hotbar")
                            && net.minecraftforge.fml.ModList.get().isLoaded("tinyinv")) {
                        player.getInventory().selected = slot + 18;
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void selectArchitectSlot(int logicalSlot) { CHANNEL.sendToServer(new ArchitectHotbarSlotPacket(logicalSlot)); }
}

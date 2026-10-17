package com.siirio.jemprofessions.network;

import com.siirio.jemprofessions.JemProfessions;
import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.LinkedHashSet;
import net.puffish.skillsmod.api.Skill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

public final class ProfessionsNetwork {
    private static final String VERSION = "4";
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
        CHANNEL.messageBuilder(ProfessionStatusPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProfessionStatusPacket::encode).decoder(ProfessionStatusPacket::decode).consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.siirio.jemprofessions.client.ProfessionClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ProfessionUnlockPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfessionUnlockPacket::encode).decoder(ProfessionUnlockPacket::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && IdentityPolicy.resolve(player).isPresent()) {
                        var node = ProfessionCatalog.node(packet.profession(), packet.node());
                        if (node != null && ProfessionApi.level(player, packet.profession()) >= node.level()) {
                            boolean parent = node.parentId().isBlank() || ProfessionApi.enabled(player, packet.profession(), node.parentId());
                            if (parent) ProfessionUnlockAuthority.unlock(player, packet.profession(), node.id());
                        }
                        sendStatus(player, true);
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void selectArchitectSlot(int logicalSlot) { CHANNEL.sendToServer(new ArchitectHotbarSlotPacket(logicalSlot)); }
    public static void unlock(ProfessionId profession, String node) { CHANNEL.sendToServer(new ProfessionUnlockPacket(profession, node)); }

    public static void sendStatus(ServerPlayer player, boolean open) {
        boolean authenticated = IdentityPolicy.resolve(player).isPresent();
        int size = ProfessionId.values().length; long[] xp = new long[size]; int[] levels = new int[size], points = new int[size];
        LinkedHashSet<String> unlocked = new LinkedHashSet<>();
        if (authenticated) for (ProfessionId id : ProfessionId.values()) {
            int ordinal = id.ordinal(); xp[ordinal] = ProfessionApi.xp(player, id); levels[ordinal] = ProfessionApi.level(player, id);
            ProfessionApi.category(id).ifPresent(category -> {
                points[ordinal] = category.getPointsLeft(player);
                category.streamSkills().filter(skill -> skill.getState(player) == Skill.State.UNLOCKED)
                        .forEach(skill -> unlocked.add(id.name() + ":" + skill.getId()));
            });
        }
        if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ProfessionStatusPacket(open, authenticated, xp, levels, points, unlocked));
        }
    }
}

package com.siirio.jemvillagertalking.speech;

import com.siirio.jemvillagertalking.JEMVillagerTalking;
import com.siirio.jemvillagertalking.client.speech.ClientVillagerSpeech;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class VillagerSpeechNetwork {
    private static final String PROTOCOL = "5";
    private static final int MAX_LINE_ID_LENGTH = 64;
    private static final int MAX_SPEAKER_ROLE_LENGTH = 32;
    private static final int MAX_SPEAKER_NAME_LENGTH = 64;
    private static final int MAX_AUTHOR_ORDINAL = 4;
    private static final int MAX_SEQUENCE_GAP_TICKS = 10;
    private static final int MAX_DELAY_TICKS = MAX_AUTHOR_ORDINAL
            * (VillagerSpeechCatalog.DISPLAY_TICKS + MAX_SEQUENCE_GAP_TICKS);
    private static final double AUDIBLE_RADIUS = 32.0D;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(JEMVillagerTalking.MOD_ID, "villager_speech"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private VillagerSpeechNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(SpeechPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpeechPacket::encode)
                .decoder(SpeechPacket::decode)
                .consumerMainThread(SpeechPacket::handle)
                .add();
    }

    public static void send(
            Villager villager,
            VillagerSpeechCatalog.Line line,
            int delayTicks,
            boolean replaceSubtitle,
            boolean showAuthor,
            String speakerRole,
            String speakerName,
            int authorOrdinal
    ) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                villager.getX(), villager.getY(), villager.getZ(), AUDIBLE_RADIUS, level.dimension()
        )), new SpeechPacket(
                line.id(),
                delayTicks,
                replaceSubtitle,
                showAuthor,
                bounded(speakerRole, MAX_SPEAKER_ROLE_LENGTH),
                bounded(speakerName, MAX_SPEAKER_NAME_LENGTH),
                authorOrdinal
        ));
    }

    private static String bounded(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record SpeechPacket(
            String lineId,
            int delayTicks,
            boolean replaceSubtitle,
            boolean showAuthor,
            String speakerRole,
            String speakerName,
            int authorOrdinal
    ) {
        private static void encode(SpeechPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.lineId, MAX_LINE_ID_LENGTH);
            buffer.writeVarInt(packet.delayTicks);
            buffer.writeBoolean(packet.replaceSubtitle);
            buffer.writeBoolean(packet.showAuthor);
            buffer.writeUtf(packet.speakerRole, MAX_SPEAKER_ROLE_LENGTH);
            buffer.writeUtf(packet.speakerName, MAX_SPEAKER_NAME_LENGTH);
            buffer.writeVarInt(packet.authorOrdinal);
        }

        private static SpeechPacket decode(FriendlyByteBuf buffer) {
            return new SpeechPacket(
                    buffer.readUtf(MAX_LINE_ID_LENGTH),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readUtf(MAX_SPEAKER_ROLE_LENGTH),
                    buffer.readUtf(MAX_SPEAKER_NAME_LENGTH),
                    buffer.readVarInt()
            );
        }

        private static void handle(SpeechPacket packet, Supplier<NetworkEvent.Context> context) {
            if (packet.delayTicks >= 0 && packet.delayTicks <= MAX_DELAY_TICKS
                    && packet.authorOrdinal >= 1 && packet.authorOrdinal <= MAX_AUTHOR_ORDINAL
                    && VillagerSpeechCatalog.line(packet.lineId) != null) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientVillagerSpeech.accept(
                        packet.lineId,
                        packet.delayTicks,
                        packet.replaceSubtitle,
                        packet.showAuthor,
                        packet.speakerRole,
                        packet.speakerName,
                        packet.authorOrdinal
                ));
            }
            context.get().setPacketHandled(true);
        }
    }
}

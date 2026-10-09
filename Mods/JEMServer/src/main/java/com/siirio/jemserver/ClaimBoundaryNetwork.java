package com.siirio.jemserver;

import com.siirio.jemserver.claims.Claims;
import com.siirio.jemserver.client.ClaimBoundaryClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = "jem_server", value = Dist.DEDICATED_SERVER)
public final class ClaimBoundaryNetwork {
    public static final int MAX_BOUNDS = 256;
    private static final String VERSION = "1";
    private static final Map<UUID, Snapshot> SENT = new HashMap<>();
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation("jem_server", "claim_boundaries"))
            .networkProtocolVersion(() -> VERSION).clientAcceptedVersions(ClaimBoundaryNetwork::compatible)
            .serverAcceptedVersions(ClaimBoundaryNetwork::compatible).simpleChannel();
    public record Bounds(int minX, int minZ, int maxX, int maxZ, int style, UUID owner, String name) {}
    public record Snapshot(ResourceLocation dimension, String toolName, int radius, int remaining, List<Bounds> bounds, BlockPos first, BlockPos second) {}

    private ClaimBoundaryNetwork() {}
    private static boolean compatible(String value) {
        return VERSION.equals(value) || NetworkRegistry.ABSENT.equals(value) || NetworkRegistry.ACCEPTVANILLA.equals(value);
    }

    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(ClaimBoundaryNetwork::encode).decoder(ClaimBoundaryNetwork::decode)
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClaimBoundaryClient.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !Claims.enabled()
                || player.tickCount % Claims.boundaryTicks() != 0 || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        Claims.BoundaryData data = Claims.boundary(player, MAX_BOUNDS);
        if (data == null) return;
        var stack = player.getMainHandItem();
        boolean holding = stack.is(Items.STICK) && stack.hasCustomHoverName() && stack.getHoverName().getString().equals(data.toolName());
        if (!holding && !SENT.containsKey(player.getUUID())) return;
        List<Bounds> bounds = holding ? data.bounds().stream().map(claim -> new Bounds(claim.minX(), claim.minZ(), claim.maxX(), claim.maxZ(),
                claim.style(), claim.owner(), claim.name())).toList() : List.of();
        var selection = holding ? data.selection() : null;
        Snapshot snapshot = new Snapshot(player.level().dimension().location(), data.toolName(), data.radius(), data.remaining(), bounds,
                selection == null ? null : selection.first(), selection == null ? null : selection.second());
        if (!snapshot.equals(SENT.get(player.getUUID()))) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
        if (holding) SENT.put(player.getUUID(), snapshot); else SENT.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SENT.remove(event.getEntity().getUUID()); }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { SENT.clear(); }

    private static void encode(Snapshot packet, FriendlyByteBuf out) {
        out.writeResourceLocation(packet.dimension()); out.writeUtf(packet.toolName(), 256); out.writeVarInt(packet.radius()); out.writeVarInt(packet.remaining());
        out.writeCollection(packet.bounds(), (buffer, bounds) -> {
            buffer.writeInt(bounds.minX()); buffer.writeInt(bounds.minZ()); buffer.writeInt(bounds.maxX()); buffer.writeInt(bounds.maxZ());
            buffer.writeByte(bounds.style()); buffer.writeUUID(bounds.owner()); buffer.writeUtf(bounds.name(), 256);
        });
        out.writeNullable(packet.first(), FriendlyByteBuf::writeBlockPos);
        out.writeNullable(packet.second(), FriendlyByteBuf::writeBlockPos);
    }

    private static Snapshot decode(FriendlyByteBuf in) {
        return new Snapshot(in.readResourceLocation(), in.readUtf(256), in.readVarInt(), in.readVarInt(),
                in.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, MAX_BOUNDS), buffer -> new Bounds(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readUnsignedByte(), buffer.readUUID(), buffer.readUtf(256))),
                in.readNullable(FriendlyByteBuf::readBlockPos), in.readNullable(FriendlyByteBuf::readBlockPos));
    }
}

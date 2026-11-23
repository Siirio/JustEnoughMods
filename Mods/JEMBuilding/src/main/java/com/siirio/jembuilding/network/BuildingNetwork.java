package com.siirio.jembuilding.network;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.client.ConstructionExpiryClient;
import com.siirio.jembuilding.client.ConstructionInput;
import com.siirio.jembuilding.client.ConstructionPreview;
import com.siirio.jembuilding.construction.ConstructionPlane;
import com.siirio.jembuilding.construction.ConstructionShape;
import com.siirio.jembuilding.construction.ConstructionRuntime;
import com.siirio.jembuilding.item.BuilderHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BuildingNetwork {
    private static final String VERSION = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JEMBuilding.MOD_ID, "construction"), () -> VERSION, VERSION::equals, VERSION::equals);

    public record SelectMode(InteractionHand hand, BlockPos clicked, Direction face, ConstructionShape shape, ConstructionPlane plane) { }
    public record Confirm(InteractionHand hand, BlockPos end, long revision) { }
    public record Cancel(InteractionHand hand) { }
    public record ExpiryRequest(BlockPos pos) { }
    public record ExpiryResponse(BlockPos pos, long remainingTicks) { }
    public record SelectionResponse(InteractionHand hand, boolean accepted, BlockPos start, ConstructionShape shape,
                                    ConstructionPlane plane, long revision, SelectionFailure failure) { }

    public enum SelectionFailure {
        NONE,
        INVALID_HAMMER,
        TARGET_UNLOADED,
        TARGET_TOO_FAR,
        AIR_TARGET,
        OUTSIDE_WORLD,
        NO_SELECTION,
        STALE_SELECTION,
        INVALID_GEOMETRY;

        public String translationKey() {
            return "message.jem_building.selection_rejected." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static void register() {
        CHANNEL.messageBuilder(SelectMode.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeEnum(packet.hand());
                    buffer.writeBlockPos(packet.clicked());
                    buffer.writeEnum(packet.face());
                    buffer.writeEnum(packet.shape());
                    buffer.writeEnum(packet.plane());
                })
                .decoder(buffer -> new SelectMode(buffer.readEnum(InteractionHand.class), buffer.readBlockPos(),
                        buffer.readEnum(Direction.class), buffer.readEnum(ConstructionShape.class), buffer.readEnum(ConstructionPlane.class)))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) ConstructionRuntime.selectMode(player, packet.hand(), packet.clicked(), packet.face(), packet.shape(), packet.plane());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Confirm.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeEnum(packet.hand());
                    buffer.writeBlockPos(packet.end());
                    buffer.writeLong(packet.revision());
                })
                .decoder(buffer -> new Confirm(buffer.readEnum(InteractionHand.class), buffer.readBlockPos(), buffer.readLong()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) ConstructionRuntime.confirm(player, packet.hand(), packet.end(), packet.revision());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Cancel.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeEnum(packet.hand()))
                .decoder(buffer -> new Cancel(buffer.readEnum(InteractionHand.class)))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.getItemInHand(packet.hand()).getItem() instanceof BuilderHammerItem) {
                        BuilderHammerItem.clear(player.getItemInHand(packet.hand()));
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ExpiryRequest.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeBlockPos(packet.pos()))
                .decoder(buffer -> new ExpiryRequest(buffer.readBlockPos()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new ExpiryResponse(packet.pos(), ConstructionRuntime.remainingLifetime(player, packet.pos())));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ExpiryResponse.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {
                    buffer.writeBlockPos(packet.pos());
                    buffer.writeLong(packet.remainingTicks());
                })
                .decoder(buffer -> new ExpiryResponse(buffer.readBlockPos(), buffer.readLong()))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ConstructionExpiryClient.receive(packet.pos(), packet.remainingTicks()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SelectionResponse.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {
                    buffer.writeEnum(packet.hand());
                    buffer.writeBoolean(packet.accepted());
                    buffer.writeBlockPos(packet.start());
                    buffer.writeEnum(packet.shape());
                    buffer.writeEnum(packet.plane());
                    buffer.writeLong(packet.revision());
                    buffer.writeEnum(packet.failure());
                })
                .decoder(buffer -> new SelectionResponse(buffer.readEnum(InteractionHand.class), buffer.readBoolean(),
                        buffer.readBlockPos(), buffer.readEnum(ConstructionShape.class), buffer.readEnum(ConstructionPlane.class),
                        buffer.readLong(), buffer.readEnum(SelectionFailure.class)))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConstructionInput.receiveSelection(packet));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void selectMode(InteractionHand hand, BlockPos clicked, Direction face, ConstructionShape shape, ConstructionPlane plane) {
        CHANNEL.sendToServer(new SelectMode(hand, clicked, face, shape, plane));
    }

    public static void confirmClientSelection(InteractionHand hand, BlockPos explicitEnd) {
        if (explicitEnd != null) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConstructionInput.sendConfirmation(hand, explicitEnd));
            return;
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConstructionPreview.current().ifPresent(
                selection -> ConstructionInput.sendConfirmation(hand, selection.end())));
    }

    public static void sendConfirmation(InteractionHand hand, BlockPos end, long revision) {
        CHANNEL.sendToServer(new Confirm(hand, end, revision));
    }

    public static void cancelSelection() {
        CHANNEL.sendToServer(new Cancel(InteractionHand.MAIN_HAND));
    }

    public static void requestExpiry(BlockPos pos) {
        CHANNEL.sendToServer(new ExpiryRequest(pos));
    }

    public static void acceptSelection(net.minecraft.server.level.ServerPlayer player, InteractionHand hand,
                                       BlockPos start, ConstructionShape shape, ConstructionPlane plane, long revision) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SelectionResponse(hand, true, start, shape, plane, revision, SelectionFailure.NONE));
    }

    public static void rejectSelection(net.minecraft.server.level.ServerPlayer player, InteractionHand hand,
                                       SelectionFailure failure) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SelectionResponse(hand, false, BlockPos.ZERO,
                ConstructionShape.WALL, ConstructionPlane.XY, 0L, failure));
    }

    private BuildingNetwork() {
    }
}

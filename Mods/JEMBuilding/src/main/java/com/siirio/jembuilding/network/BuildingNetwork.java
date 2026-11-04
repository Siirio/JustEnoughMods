package com.siirio.jembuilding.network;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.client.ConstructionExpiryClient;
import com.siirio.jembuilding.client.ConstructionPreview;
import com.siirio.jembuilding.client.BlueprintClientState;
import com.siirio.jembuilding.blueprint.BlueprintMode;
import com.siirio.jembuilding.blueprint.BlueprintRotation;
import com.siirio.jembuilding.blueprint.BlueprintRuntime;
import com.siirio.jembuilding.blueprint.BlueprintSavedData;
import com.siirio.jembuilding.blueprint.BlueprintStatus;
import com.siirio.jembuilding.construction.ConstructionPlane;
import com.siirio.jembuilding.construction.ConstructionShape;
import com.siirio.jembuilding.construction.ConstructionRuntime;
import com.siirio.jembuilding.item.BuilderHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    public record Confirm(InteractionHand hand, BlockPos end) { }
    public record Cancel(InteractionHand hand) { }
    public record ExpiryRequest(BlockPos pos) { }
    public record ExpiryResponse(BlockPos pos, long remainingTicks) { }
    public record SelectBlueprintFirst(InteractionHand hand, BlockPos pos) { }
    public record CaptureBlueprint(InteractionHand hand, BlockPos second, BlueprintMode mode) { }
    public record BlueprintPreviewRequest(InteractionHand hand) { }
    public record BlueprintPreviewResponse(UUID id, int width, int height, int depth, List<CompoundTag> palette,
                                           List<BlueprintSavedData.Entry> entries, BlueprintStatus status) { }
    public record PlaceBlueprint(InteractionHand hand, BlockPos destination, BlueprintRotation rotation) { }
    public record CancelBlueprint(InteractionHand hand) { }

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
                })
                .decoder(buffer -> new Confirm(buffer.readEnum(InteractionHand.class), buffer.readBlockPos()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) ConstructionRuntime.confirm(player, packet.hand(), packet.end());
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
        CHANNEL.messageBuilder(SelectBlueprintFirst.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> { buffer.writeEnum(packet.hand()); buffer.writeBlockPos(packet.pos()); })
                .decoder(buffer -> new SelectBlueprintFirst(buffer.readEnum(InteractionHand.class), buffer.readBlockPos()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) BlueprintRuntime.selectFirst(player, packet.hand(), packet.pos());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(CaptureBlueprint.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> { buffer.writeEnum(packet.hand()); buffer.writeBlockPos(packet.second()); buffer.writeEnum(packet.mode()); })
                .decoder(buffer -> new CaptureBlueprint(buffer.readEnum(InteractionHand.class), buffer.readBlockPos(), buffer.readEnum(BlueprintMode.class)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) BlueprintRuntime.capture(player, packet.hand(), packet.second(), packet.mode());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BlueprintPreviewRequest.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeEnum(packet.hand()))
                .decoder(buffer -> new BlueprintPreviewRequest(buffer.readEnum(InteractionHand.class)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) BlueprintRuntime.requestPreview(player, packet.hand());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BlueprintPreviewResponse.class, 8, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {
                    buffer.writeUUID(packet.id()); buffer.writeVarInt(packet.width()); buffer.writeVarInt(packet.height()); buffer.writeVarInt(packet.depth());
                    buffer.writeVarInt(packet.palette().size()); for (CompoundTag state : packet.palette()) buffer.writeNbt(state);
                    buffer.writeVarInt(packet.entries().size());
                    for (BlueprintSavedData.Entry entry : packet.entries()) {
                        buffer.writeVarInt(entry.x()); buffer.writeVarInt(entry.y()); buffer.writeVarInt(entry.z()); buffer.writeVarInt(entry.paletteIndex());
                    }
                    buffer.writeEnum(packet.status());
                })
                .decoder(buffer -> {
                    UUID id = buffer.readUUID(); int width = buffer.readVarInt(); int height = buffer.readVarInt(); int depth = buffer.readVarInt();
                    int paletteSize = buffer.readVarInt(); List<CompoundTag> palette = new ArrayList<>(paletteSize);
                    for (int index = 0; index < paletteSize; index++) palette.add(buffer.readNbt());
                    int entrySize = buffer.readVarInt(); List<BlueprintSavedData.Entry> entries = new ArrayList<>(entrySize);
                    for (int index = 0; index < entrySize; index++) entries.add(new BlueprintSavedData.Entry(
                            buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
                    return new BlueprintPreviewResponse(id, width, height, depth, palette, entries, buffer.readEnum(BlueprintStatus.class));
                })
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BlueprintClientState.receive(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(PlaceBlueprint.class, 9, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> { buffer.writeEnum(packet.hand()); buffer.writeBlockPos(packet.destination()); buffer.writeEnum(packet.rotation()); })
                .decoder(buffer -> new PlaceBlueprint(buffer.readEnum(InteractionHand.class), buffer.readBlockPos(), buffer.readEnum(BlueprintRotation.class)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) BlueprintRuntime.place(player, packet.hand(), packet.destination(), packet.rotation());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(CancelBlueprint.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeEnum(packet.hand()))
                .decoder(buffer -> new CancelBlueprint(buffer.readEnum(InteractionHand.class)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) BlueprintRuntime.cancel(player, packet.hand());
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void selectMode(InteractionHand hand, BlockPos clicked, Direction face, ConstructionShape shape, ConstructionPlane plane) {
        CHANNEL.sendToServer(new SelectMode(hand, clicked, face, shape, plane));
    }

    public static void confirmClientSelection(InteractionHand hand, BlockPos explicitEnd) {
        if (explicitEnd != null) {
            CHANNEL.sendToServer(new Confirm(hand, explicitEnd));
            return;
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConstructionPreview.current().ifPresent(
                selection -> CHANNEL.sendToServer(new Confirm(hand, selection.end()))));
    }

    public static void cancelSelection() {
        CHANNEL.sendToServer(new Cancel(InteractionHand.MAIN_HAND));
    }

    public static void requestExpiry(BlockPos pos) {
        CHANNEL.sendToServer(new ExpiryRequest(pos));
    }

    public static void selectBlueprintFirst(InteractionHand hand, BlockPos pos) { CHANNEL.sendToServer(new SelectBlueprintFirst(hand, pos)); }
    public static void captureBlueprint(InteractionHand hand, BlockPos second, BlueprintMode mode) { CHANNEL.sendToServer(new CaptureBlueprint(hand, second, mode)); }
    public static void requestBlueprint(InteractionHand hand) { CHANNEL.sendToServer(new BlueprintPreviewRequest(hand)); }
    public static void placeBlueprint(InteractionHand hand, BlockPos destination, BlueprintRotation rotation) {
        CHANNEL.sendToServer(new PlaceBlueprint(hand, destination, rotation));
    }
    public static void cancelBlueprint(InteractionHand hand) { CHANNEL.sendToServer(new CancelBlueprint(hand)); }
    public static void clearBlueprintSelection(InteractionHand hand) { CHANNEL.sendToServer(new CancelBlueprint(hand)); }

    public static void sendBlueprint(ServerPlayer player, BlueprintSavedData.Blueprint blueprint) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlueprintPreviewResponse(blueprint.id(), blueprint.width(),
                blueprint.height(), blueprint.depth(), blueprint.palette(), blueprint.entries(), blueprint.status()));
    }

    public static void confirmBlueprint(InteractionHand hand) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BlueprintClientState.confirm(hand));
    }

    private BuildingNetwork() {
    }
}

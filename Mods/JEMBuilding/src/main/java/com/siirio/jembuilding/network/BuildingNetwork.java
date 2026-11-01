package com.siirio.jembuilding.network;

import com.siirio.jembuilding.JEMBuilding;
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
import net.minecraftforge.network.simple.SimpleChannel;

public final class BuildingNetwork {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JEMBuilding.MOD_ID, "construction"), () -> VERSION, VERSION::equals, VERSION::equals);

    public record SelectMode(InteractionHand hand, BlockPos clicked, Direction face, ConstructionShape shape, ConstructionPlane plane) { }
    public record Confirm(InteractionHand hand, BlockPos end) { }
    public record Cancel(InteractionHand hand) { }

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

    private BuildingNetwork() {
    }
}

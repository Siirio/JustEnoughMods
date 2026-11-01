package com.siirio.jembuilding.network;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.client.ConstructionPreview;
import com.siirio.jembuilding.construction.ConstructionPlane;
import com.siirio.jembuilding.construction.ConstructionRuntime;
import com.siirio.jembuilding.item.BuilderHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BuildingNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JEMBuilding.MOD_ID, "construction"), () -> VERSION, VERSION::equals, VERSION::equals);

    public record Confirm(InteractionHand hand, ConstructionPlane plane, BlockPos end) { }
    public record Cancel(InteractionHand hand) { }

    public static void register() {
        CHANNEL.messageBuilder(Confirm.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeEnum(packet.hand());
                    buffer.writeEnum(packet.plane());
                    buffer.writeBlockPos(packet.end());
                })
                .decoder(buffer -> new Confirm(buffer.readEnum(InteractionHand.class), buffer.readEnum(ConstructionPlane.class), buffer.readBlockPos()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) ConstructionRuntime.confirm(player, packet.hand(), packet.plane(), packet.end());
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Cancel.class, 1, NetworkDirection.PLAY_TO_SERVER)
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

    public static void confirmClientSelection(InteractionHand hand) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConstructionPreview.current().ifPresent(
                selection -> CHANNEL.sendToServer(new Confirm(hand, selection.plane(), selection.end()))));
    }

    public static void cancelSelection() {
        CHANNEL.sendToServer(new Cancel(InteractionHand.MAIN_HAND));
    }

    private BuildingNetwork() {
    }
}

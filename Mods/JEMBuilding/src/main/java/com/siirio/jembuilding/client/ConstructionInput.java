package com.siirio.jembuilding.client;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.item.BuilderHammerItem;
import com.siirio.jembuilding.network.BuildingNetwork;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT)
public final class ConstructionInput {
    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof BuilderHammerItem)) return;
        event.setCanceled(true);
        Minecraft client = Minecraft.getInstance();
        if (client.player != event.getEntity() || client.screen != null) return;
        client.setScreen(new ConstructionModeScreen(event.getPos(), event.getFace(), InteractionHand.MAIN_HAND,
                event.getEntity().getLookAngle()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void confirmActiveSelection(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null || client.getCameraEntity() != client.player
                || client.player.isShiftKeyDown()) return;
        ItemStack hammer = client.player.getMainHandItem();
        if (!(hammer.getItem() instanceof BuilderHammerItem) || !BuilderHammerItem.hasConfirmedSelection(hammer)) return;
        ConstructionPreview.current().ifPresent(selection -> {
            sendConfirmation(InteractionHand.MAIN_HAND, selection.end());
            event.setSwingHand(false);
            event.setCanceled(true);
        });
    }

    public static void sendConfirmation(InteractionHand hand, BlockPos end) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        OptionalLong revision = BuilderHammerItem.revision(client.player.getItemInHand(hand));
        if (revision.isPresent()) BuildingNetwork.sendConfirmation(hand, end, revision.getAsLong());
    }

    public static void receiveSelection(BuildingNetwork.SelectionResponse response) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        ItemStack stack = client.player.getItemInHand(response.hand());
        if (!response.accepted()) {
            if (stack.getItem() instanceof BuilderHammerItem) BuilderHammerItem.clear(stack);
            client.player.displayClientMessage(Component.translatable(response.failure().translationKey()), true);
            return;
        }
        if (!(stack.getItem() instanceof BuilderHammerItem)) {
            client.player.displayClientMessage(Component.translatable(
                    BuildingNetwork.SelectionFailure.INVALID_HAMMER.translationKey()), true);
            return;
        }
        BuilderHammerItem.setStart(stack, response.start(), response.shape(), response.plane(), response.revision());
    }

    private ConstructionInput() { }
}

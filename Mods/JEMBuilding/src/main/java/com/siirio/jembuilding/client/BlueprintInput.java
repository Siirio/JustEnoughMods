package com.siirio.jembuilding.client;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.item.BlueprintRollItem;
import com.siirio.jembuilding.network.BuildingNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT)
public final class BlueprintInput {
    private static BlockPos pendingFirst;

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof BlueprintRollItem)) return;
        event.setCanceled(true);
        Minecraft client = Minecraft.getInstance();
        if (client.player != event.getEntity() || client.screen != null) return;
        if (BlueprintRollItem.blueprintId(event.getEntity().getMainHandItem()).isPresent()) return;
        BlockPos first = BlueprintRollItem.first(event.getEntity().getMainHandItem()).orElse(pendingFirst);
        if (first == null) {
            pendingFirst = event.getPos().immutable();
            BuildingNetwork.selectBlueprintFirst(InteractionHand.MAIN_HAND, event.getPos());
            BlueprintClientAnimation.playOpen(InteractionHand.MAIN_HAND);
            return;
        }
        event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.jem_building.blueprint.use_right_click"), true);
    }

    static void clearPendingFirst() { pendingFirst = null; }
    static BlockPos pendingFirst() { return pendingFirst; }

    private BlueprintInput() { }
}

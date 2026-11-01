package com.siirio.jembuilding.client;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.item.BuilderHammerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

    private ConstructionInput() { }
}

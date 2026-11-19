package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.JemSettlements;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=JemSettlements.MOD_ID,value=Dist.CLIENT)
public final class FamilyScreenTabs {
    private FamilyScreenTabs() { }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!FamilyClientState.active()) return; String name = event.getScreen().getClass().getName();
        if (!(event.getScreen() instanceof MerchantScreen) && !name.startsWith("com.talhanation.recruits.client.gui.")) return;
        int width = 66, x = event.getScreen().width/2 - 88, y = Math.max(4, event.getScreen().height/2 - 92);
        event.addListener(Button.builder(Component.translatable("jem.settlements.family.home_tab"), button -> FamilyClientState.open()).bounds(x,y,width,20).build());
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { FamilyClientState.clear(); }
}

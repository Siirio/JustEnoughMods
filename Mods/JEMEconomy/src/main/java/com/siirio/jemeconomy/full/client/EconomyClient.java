package com.siirio.jemeconomy.full.client;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.registry.EconomyMenus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EconomyClient {
    private EconomyClient() { }
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.siirio.jemeconomy.registry.EconomyBlockEntities.SHIPPING_BOX.get(), ShippingBoxRenderer::new);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> {
        MenuScreens.register(EconomyMenus.SHIPPING_BOX.get(), ShippingBoxScreen::new);
        MenuScreens.register(EconomyMenus.COIN_MERCHANT.get(), net.minecraft.client.gui.screens.inventory.MerchantScreen::new);
    }); }
}

package com.siirio.jemtwelveeyes;

import com.siirio.jemcompat.discovery.CampaignDiscoveryEvents;
import com.siirio.jemcompat.discovery.CampaignItems;
import com.siirio.jemcompat.gate.CampaignCoordinator;
import com.siirio.jemcompat.gate.CampaignGateEvents;
import com.siirio.jemcompat.gate.CampaignBlocks;
import com.siirio.jemcompat.site.CampaignSiteService;
import com.siirio.jemtwelveeyes.network.CampaignNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JEMTwelveEyes.MOD_ID)
public final class JEMTwelveEyes {
    public static final String MOD_ID = "jem_twelve_eyes";
    public static final String CONTENT_NAMESPACE = "jemcompat";

    public JEMTwelveEyes() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CampaignNetwork.register();
        CampaignBlocks.register(modBus);
        CampaignItems.register(modBus);
        modBus.addListener(this::addCreativeItems);
        MinecraftForge.EVENT_BUS.register(new CampaignCoordinator());
        MinecraftForge.EVENT_BUS.register(CampaignGateEvents.class);
        MinecraftForge.EVENT_BUS.register(CampaignDiscoveryEvents.class);
        MinecraftForge.EVENT_BUS.register(CampaignSiteService.class);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            CampaignItems.locators().forEach(item -> event.accept(item.get()));
        }
    }
}

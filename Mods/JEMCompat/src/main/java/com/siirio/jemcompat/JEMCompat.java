package com.siirio.jemcompat;

import com.siirio.jemcompat.compat.jade.TamingDataLoader;
import com.siirio.jemcompat.config.JEMClientConfig;
import com.siirio.jemcompat.feature.worldtier.EnhancedAiTierController;
import com.siirio.jemcompat.feature.leash.UniversalLeashEvents;
import com.siirio.jemcompat.feature.elements.ElementalImpactEvents;
import com.siirio.jemcompat.feature.kaleidoscope.VoidWalkerEvents;
import com.siirio.jemcompat.feature.transport.TransportContent;
import com.siirio.jemcompat.feature.transport.TransportEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(JEMCompat.MOD_ID)
public final class JEMCompat {
    public static final String MOD_ID = "jemcompat";

    public JEMCompat() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        TransportContent.register(modBus);
        modBus.addListener(JEMCompat::creativeTabs);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, JEMClientConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        if (ModList.get().isLoaded("enhancedai") && ModList.get().isLoaded("jem_world_boss_tiers")) {
            MinecraftForge.EVENT_BUS.register(new EnhancedAiTierController());
        }
        MinecraftForge.EVENT_BUS.register(UniversalLeashEvents.class);
        MinecraftForge.EVENT_BUS.register(ElementalImpactEvents.class);
        MinecraftForge.EVENT_BUS.register(TransportEvents.class);
        if (ModList.get().isLoaded("kaleidoscope_end")) MinecraftForge.EVENT_BUS.register(VoidWalkerEvents.class);
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS || event.getTabKey() == CreativeModeTabs.SEARCH) {
            event.accept(TransportContent.AIRCRAFT_FUEL);
        }
        if (event.getTabKey() == CreativeModeTabs.SEARCH) ForgeRegistries.ITEMS.getValues().stream()
                .filter(SpawnEggItem.class::isInstance).forEach(item -> {
                    var stack = item.getDefaultInstance();
                    if (!event.getEntries().contains(stack)) event.accept(() -> item);
                });
    }

    @SubscribeEvent
    public void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(TamingDataLoader.instance());
    }
}

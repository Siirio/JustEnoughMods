package com.siirio.mobhome;

import com.siirio.mobhome.feature.shelter.ShelterContent;
import com.siirio.mobhome.feature.shelter.ShelterNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Standalone common entry point, shared by integrated and dedicated logical servers. */
@Mod(MobHome.MOD_ID)
public final class MobHome {
    public static final String MOD_ID = "mobhome";

    public MobHome() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ShelterContent.register(bus);
        ShelterNetwork.register();
        bus.addListener(MobHome::creativeTabs);
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ShelterContent.SMALL_ITEM);
            event.accept(ShelterContent.LARGE_ITEM);
        }
    }
}

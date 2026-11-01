package com.siirio.jembuilding;

import com.siirio.jembuilding.construction.ConstructionRuntime;
import com.siirio.jembuilding.network.BuildingNetwork;
import com.siirio.jembuilding.registry.BuildingContent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JEMBuilding.MOD_ID)
public final class JEMBuilding {
    public static final String MOD_ID = "jem_building";

    public JEMBuilding() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BuildingContent.register(modBus);
        BuildingNetwork.register();
        MinecraftForge.EVENT_BUS.register(ConstructionRuntime.class);
    }
}

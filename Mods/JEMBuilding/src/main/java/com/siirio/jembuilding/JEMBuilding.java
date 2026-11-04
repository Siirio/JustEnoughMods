package com.siirio.jembuilding;

import com.siirio.jembuilding.construction.ConstructionRuntime;
import com.siirio.jembuilding.blueprint.BlueprintConfig;
import com.siirio.jembuilding.blueprint.BlueprintRuntime;
import com.siirio.jembuilding.network.BuildingNetwork;
import com.siirio.jembuilding.registry.BuildingContent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JEMBuilding.MOD_ID)
public final class JEMBuilding {
    public static final String MOD_ID = "jem_building";

    public JEMBuilding() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BuildingContent.register(modBus);
        BuildingNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BlueprintConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(ConstructionRuntime.class);
        MinecraftForge.EVENT_BUS.register(BlueprintRuntime.class);
    }
}

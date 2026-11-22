package com.siirio.jemcraftablespawneggs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JEMCraftableSpawnEggs.MOD_ID)
public final class JEMCraftableSpawnEggs {
    public static final String MOD_ID = "jem_craftable_spawn_eggs";

    public JEMCraftableSpawnEggs() {
        GeneratedSpawnEggContent.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(SpawnEggPolicy.class);
        if (net.minecraftforge.fml.ModList.get().isLoaded("paraglider"))
            MinecraftForge.EVENT_BUS.register(SpawnerSpiritOrbPolicy.class);
    }
}

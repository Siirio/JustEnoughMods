package com.siirio.jemexcavation;

import com.siirio.jemexcavation.feature.CoreEnchantments;
import com.siirio.jemexcavation.feature.ExcavationEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JEMExcavation.MOD_ID)
public final class JEMExcavation {
    public static final String MOD_ID = "jem_excavation";

    public JEMExcavation() {
        CoreEnchantments.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(ExcavationEvents.class);
    }
}

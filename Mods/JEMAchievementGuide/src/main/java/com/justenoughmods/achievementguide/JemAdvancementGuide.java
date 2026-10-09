package com.justenoughmods.achievementguide;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(JemAdvancementGuide.MOD_ID)
public final class JemAdvancementGuide {
    public static final String MOD_ID = "jem_advancements";
    public static final String GUIDE_NAMESPACE = MOD_ID;

    public JemAdvancementGuide() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(JemCriteria::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(JemCriteria.class);
    }
}

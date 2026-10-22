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
        com.justenoughmods.achievementguide.handbook.manuals.CommonManuals.register();
        modBus.addListener(JemCriteria::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(JemCriteria.class);
        modBus.addListener(JemAdvancementGuide::registerPackCriteria);
        MinecraftForge.EVENT_BUS.register(com.justenoughmods.achievementguide.integration.ForgeGameplayEvents.class);
        if (net.minecraftforge.fml.ModList.get().isLoaded("immersive_weathering")) {
            MinecraftForge.EVENT_BUS.register(com.justenoughmods.achievementguide.integration.ModCompatibilityEvents.class);
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_drill_lifecycle")) {
            MinecraftForge.EVENT_BUS.register(com.justenoughmods.achievementguide.integration.DrillAdvancements.class);
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("treechop")) {
            com.justenoughmods.achievementguide.integration.treechop.TreeChopIntegration.register();
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("ftbquests")) {
            com.justenoughmods.achievementguide.compat.ftb.FtbQuestBridge.register();
        }
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> com.justenoughmods.achievementguide.client.ClientBootstrap::register);
    }

    private static void registerPackCriteria(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try (var stream = JemAdvancementGuide.class.getResourceAsStream("/data/jem_advancements/node_criteria.json")) {
                if (stream == null) throw new IllegalStateException("Missing guide criteria catalog");
                try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                    var ids = new java.util.ArrayList<String>();
                    com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("criteria")
                            .forEach(id -> ids.add(id.getAsString()));
                    JemCriteria.registerNodes(ids);
                }
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Cannot load guide criteria catalog", exception);
            }
        });
    }
}

package com.siirio.jempackcore;

import com.google.gson.JsonParser;
import com.siirio.jempackcore.postend.PostEndEvents;
import com.siirio.jempackcore.postend.PostEndItems;
import com.siirio.jempackcore.postend.PostEndSounds;
import com.siirio.jempackcore.enchantment.CoreEnchantments;
import com.siirio.jempackcore.enchantment.ExcavationEvents;
import com.justenoughmods.achievementguide.client.ClientBootstrap;
import com.justenoughmods.achievementguide.criterion.JemCriteria;
import com.justenoughmods.achievementguide.integration.ForgeGameplayEvents;
import com.justenoughmods.achievementguide.integration.ModCompatibilityEvents;
import com.justenoughmods.achievementguide.integration.treechop.TreeChopIntegration;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JEMPackCore.MOD_ID)
public final class JEMPackCore {
    public static final String MOD_ID = "jem_pack_core";

    public JEMPackCore() {
        CoreEnchantments.register(FMLJavaModLoadingContext.get().getModEventBus());
        PostEndItems.register(FMLJavaModLoadingContext.get().getModEventBus());
        PostEndSounds.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.register(PostEndEvents.class);
        MinecraftForge.EVENT_BUS.register(ExcavationEvents.class);
        CreatureSpawnProbability.SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(ForgeGameplayEvents.class);
        if (ModList.get().isLoaded("immersive_weathering")) {
            MinecraftForge.EVENT_BUS.register(ModCompatibilityEvents.class);
        }
        if (ModList.get().isLoaded("treechop")) {
            TreeChopIntegration.register();
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::register);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try (var stream = JEMPackCore.class.getResourceAsStream("/data/jem_advancements/node_criteria.json")) {
                if (stream == null) {
                    throw new IllegalStateException("Missing pack criteria catalog");
                }
                try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    var ids = new ArrayList<String>();
                    JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("criteria")
                            .forEach(id -> ids.add(id.getAsString()));
                    JemCriteria.registerNodes(ids);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot load pack criteria catalog", exception);
            }
        });
    }
}

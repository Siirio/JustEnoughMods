package com.justenoughmods.achievementguide.client;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class ClientBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientBootstrap() {
    }

    public static void register() {
        boolean betterAdvancements = ModList.get().isLoaded("betteradvancements");
        boolean patchouli = ModList.get().isLoaded("patchouli");
        LOGGER.info("[JEM Advancements] Patchouli click integration bootstrap: betteradvancements={}, patchouli={}", betterAdvancements, patchouli);
        if (betterAdvancements && patchouli) {
            MinecraftForge.EVENT_BUS.register(AdvancementScreenEvents.class);
            LOGGER.info("[JEM Advancements] Registered Patchouli advancement click integration");
        }
    }
}

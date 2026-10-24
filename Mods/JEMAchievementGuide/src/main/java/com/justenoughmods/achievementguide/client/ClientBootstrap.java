package com.justenoughmods.achievementguide.client;

import net.minecraftforge.fml.ModList;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void register() {
        boolean patchouli = ModList.get().isLoaded("patchouli");
        if (patchouli) {
            com.justenoughmods.achievementguide.client.handbook.HandbookNavigation.register();
        }
    }
}

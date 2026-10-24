package com.justenoughmods.achievementguide.client;

import betteradvancements.common.gui.BetterAdvancementsScreen;

/** Binary entry point retained for installed Better Advancements through the Compat shim. */
public final class AdvancementScreenEvents {
    private AdvancementScreenEvents() {}

    public static void directClick(BetterAdvancementsScreen screen, double mouseX, double mouseY, int button) {
        // Advancement notes and their book click routing were retired by the user.
    }
}

package com.siirio.jemcompat.client;

import betteradvancements.common.gui.BetterAdvancementsScreen;

public final class AdvancementScreenEvents {
    private AdvancementScreenEvents() {
    }

    public static void directClick(BetterAdvancementsScreen screen, double mouseX, double mouseY, int button) {
        com.justenoughmods.achievementguide.client.AdvancementScreenEvents.directClick(screen, mouseX, mouseY, button);
    }
}

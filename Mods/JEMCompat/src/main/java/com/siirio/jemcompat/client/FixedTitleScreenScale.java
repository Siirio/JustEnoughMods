package com.siirio.jemcompat.client;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the main menu at its authored scale without changing the player's in-game GUI setting. */
@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT)
public final class FixedTitleScreenScale {
    private static final int TITLE_SCREEN_SCALE = 4;

    private FixedTitleScreenScale() { }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        int requestedScale = event.getNewScreen() instanceof TitleScreen
                ? TITLE_SCREEN_SCALE
                : minecraft.options.guiScale().get();
        apply(minecraft, requestedScale);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TitleScreen) apply(minecraft, TITLE_SCREEN_SCALE);
    }

    private static void apply(Minecraft minecraft, int requestedScale) {
        int effectiveScale = minecraft.getWindow().calculateScale(
                requestedScale, minecraft.options.forceUnicodeFont().get());
        if (Double.compare(minecraft.getWindow().getGuiScale(), effectiveScale) != 0) {
            minecraft.getWindow().setGuiScale(effectiveScale);
        }
    }
}

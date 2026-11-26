package com.siirio.jemtwelveeyes.client;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import com.siirio.jemtwelveeyes.CampaignEndingEvent;
import com.siirio.jemtwelveeyes.network.CampaignNetwork;
import com.siirio.jemtwelveeyes.postend.PostEndSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = JEMTwelveEyes.MOD_ID, value = Dist.CLIENT)
public final class SecretEndingClient {
    private static final long VANILLA_NANOS = 12_000_000_000L;
    private static final long GLITCH_NANOS = 3_140_000_000L;
    private static long startedAt;
    private static WinScreen vanillaScreen;
    private static GlitchScreen glitchScreen;
    private static SimpleSoundInstance glitchSound;

    private SecretEndingClient() { }

    @SubscribeEvent
    public static void start(CampaignEndingEvent event) {
        reset();
        startedAt = System.nanoTime();
        vanillaScreen = new LockedWinScreen();
        glitchScreen = new GlitchScreen();
        Minecraft.getInstance().setScreen(vanillaScreen);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || startedAt == 0L) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) {
            reset();
            return;
        }
        long elapsed = System.nanoTime() - startedAt;
        if (elapsed < VANILLA_NANOS) {
            if (minecraft.screen != vanillaScreen) minecraft.setScreen(vanillaScreen);
            return;
        }
        if (elapsed < VANILLA_NANOS + GLITCH_NANOS) {
            if (glitchSound == null) {
                glitchSound = SimpleSoundInstance.forUI(PostEndSounds.SECRET_GLITCH.get(), 1.0F);
                minecraft.getSoundManager().play(glitchSound);
            }
            if (minecraft.screen != glitchScreen) minecraft.setScreen(glitchScreen);
            return;
        }
        CampaignNetwork.completeSecretEnding();
        minecraft.setScreen(null);
        reset();
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        if (glitchSound != null) Minecraft.getInstance().getSoundManager().stop(glitchSound);
        if (glitchScreen != null) glitchScreen.closeClip();
        glitchSound = null;
        vanillaScreen = null;
        glitchScreen = null;
        startedAt = 0L;
    }

    private static final class LockedWinScreen extends WinScreen {
        private LockedWinScreen() { super(true, () -> { }); }
        @Override public boolean shouldCloseOnEsc() { return false; }
        @Override public void onClose() { }
        @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return true; }
        @Override public boolean isPauseScreen() { return false; }
    }

    private static final class GlitchScreen extends Screen {
        private final SecretEndingClip clip = new SecretEndingClip(Minecraft.getInstance().getResourceManager());

        private GlitchScreen() { super(Component.empty()); }
        @Override public boolean shouldCloseOnEsc() { return false; }
        @Override public void onClose() { }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            long elapsed = Math.max(0L, System.nanoTime() - startedAt - VANILLA_NANOS);
            clip.render(graphics, width, height, elapsed);
        }
        private void closeClip() { clip.close(); }
    }
}

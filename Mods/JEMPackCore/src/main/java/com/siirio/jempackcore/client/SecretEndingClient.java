package com.siirio.jempackcore.client;

import com.siirio.jempackcore.JEMPackCore;
import com.siirio.jemtwelveeyes.CampaignEndingEvent;
import com.siirio.jempackcore.postend.PostEndSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = JEMPackCore.MOD_ID, value = Dist.CLIENT)
public final class SecretEndingClient {
    private static final long VANILLA_NANOS = 12_000_000_000L;
    private static final long GLITCH_NANOS = 9_000_000_000L;
    private static long startedAt;
    private static WinScreen vanillaScreen;
    private static GlitchScreen glitchScreen;
    private static SimpleSoundInstance glitchSound;
    private static Runnable finishCredits;
    private static boolean awaitingCredits;

    private SecretEndingClient() { }

    @SubscribeEvent
    public static void start(CampaignEndingEvent event) {
        awaitingCredits = true;
    }

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        if (!awaitingCredits || !(event.getNewScreen() instanceof WinScreen original)) return;
        awaitingCredits = false;
        startedAt = System.nanoTime();
        finishCredits = original::onClose;
        vanillaScreen = new LockedWinScreen();
        glitchScreen = new GlitchScreen();
        event.setNewScreen(vanillaScreen);
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
        Runnable finished = finishCredits;
        reset();
        finished.run();
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        if (glitchSound != null) Minecraft.getInstance().getSoundManager().stop(glitchSound);
        glitchSound = null;
        awaitingCredits = false;
        finishCredits = null;
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
        private final Random random = new Random();
        private GlitchScreen() { super(Component.empty()); }
        @Override public boolean shouldCloseOnEsc() { return false; }
        @Override public void onClose() { }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            for (int i = 0; i < 24; i++) {
                int y = random.nextInt(Math.max(1, height));
                int h = 1 + random.nextInt(8);
                int color = random.nextBoolean() ? 0xAAFFFFFF : random.nextBoolean() ? 0xAAFF0044 : 0xAA00E5FF;
                graphics.fill(random.nextInt(Math.max(1, width / 3)), y, width - random.nextInt(Math.max(1, width / 3)), Math.min(height, y + h), color);
            }
            double glitchElapsed = (System.nanoTime() - startedAt - VANILLA_NANOS) / 1_000_000_000.0D;
            String text = glitchElapsed < 2.2D ? "IT IS" : "NOT THE END";
            float pulse = 1.8F + 0.25F * Mth.sin((float) glitchElapsed * 13.0F);
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
            graphics.pose().scale(pulse, pulse, 1.0F);
            graphics.drawCenteredString(font, text, random.nextInt(5) - 2, random.nextInt(5) - 2, 0xFFFFFFFF);
            graphics.pose().popPose();
        }
    }
}

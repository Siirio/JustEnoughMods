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
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = JEMTwelveEyes.MOD_ID, value = Dist.CLIENT)
public final class SecretEndingClient {
    private static final long VANILLA_NANOS = 12_000_000_000L;
    private static final long GLITCH_NANOS = 9_000_000_000L;
    private static long startedAt;
    private static WinScreen vanillaScreen;
    private static GlitchScreen glitchScreen;
    private static SimpleSoundInstance glitchSound;

    private SecretEndingClient() { }

    @SubscribeEvent
    public static void start(CampaignEndingEvent event) {
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
        private GlitchScreen() { super(Component.empty()); }
        @Override public boolean shouldCloseOnEsc() { return false; }
        @Override public void onClose() { }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            double glitchElapsed = (System.nanoTime() - startedAt - VANILLA_NANOS) / 1_000_000_000.0D;
            long frame = (long) (glitchElapsed * 24.0D);
            Random random = new Random(0x5EEDC0DEL ^ frame * 0x9E3779B97F4A7C15L);
            renderField(graphics, random, glitchElapsed);
            String text = glitchElapsed < 2.2D ? "IT IS" : "NOT THE END";
            float pulse = 2.05F + 0.08F * Mth.sin((float) glitchElapsed * 5.0F);
            int jitterX = random.nextInt(3) - 1;
            int jitterY = random.nextInt(3) - 1;
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2.0F + jitterX, height / 2.0F + jitterY, 0.0F);
            graphics.pose().scale(pulse, pulse, 1.0F);
            graphics.drawCenteredString(font, text, -2, 0, 0xBFFF20E8);
            graphics.drawCenteredString(font, text, 2, 0, 0xBF20E8FF);
            graphics.drawCenteredString(font, text, 0, 0, 0xFFF5EEFF);
            graphics.pose().popPose();
            renderTextSlices(graphics, random, text, pulse);
        }

        private void renderField(GuiGraphics graphics, Random random, double elapsed) {
            graphics.fill(0, 0, width, height, 0xFF080018);
            int bands = Math.max(18, height / 18);
            for (int band = 0; band < bands; band++) {
                int y0 = band * height / bands;
                int y1 = (band + 1) * height / bands;
                int wave = (int) (22.0D * Math.sin(elapsed * 1.7D + band * 0.48D));
                int red = 14 + band % 4 * 5;
                int blue = 38 + band % 5 * 7;
                graphics.fill(Math.max(0, wave), y0, width, y1, 0xFF000000 | red << 16 | 4 << 8 | blue);
            }
            for (int y = 0; y < height; y += 4) graphics.fill(0, y, width, y + 1, 0x36000000);
            int majorTears = Math.max(7, height / 95);
            for (int i = 0; i < majorTears; i++) {
                int y = random.nextInt(Math.max(1, height));
                int h = 3 + random.nextInt(Math.max(4, height / 75));
                int w = width / 5 + random.nextInt(Math.max(2, width / 2));
                int x = random.nextInt(Math.max(1, width + w / 3)) - w / 3;
                int leftOffset = 7 + random.nextInt(Math.max(8, width / 70));
                int rightOffset = 8 + random.nextInt(Math.max(9, width / 60));
                int magentaEnd = Math.min(width, x + w - random.nextInt(Math.max(2, w / 5)));
                int cyanEnd = Math.min(width, x + w + random.nextInt(Math.max(2, w / 6)));
                graphics.fill(x - leftOffset, Math.max(0, y - 2), magentaEnd - leftOffset,
                        Math.min(height, y + h), 0xA7F01F63);
                graphics.fill(x + rightOffset, Math.min(height, y + 2), cyanEnd + rightOffset,
                        Math.min(height, y + h + 3), 0xA71FE7F0);
                graphics.fill(x, y, Math.min(width, x + w), Math.min(height, y + h),
                        random.nextBoolean() ? 0xC07D1EDB : 0xC05222B8);
                if (w > 30) {
                    int cut = x + w / 3 + random.nextInt(Math.max(2, w / 3));
                    graphics.fill(cut, y, Math.min(width, cut + 3 + random.nextInt(Math.max(4, w / 12))),
                            Math.min(height, y + h), 0xC0080018);
                }
            }
            int hazeLayers = Math.max(14, height / 42);
            for (int i = 0; i < hazeLayers; i++) {
                int y = random.nextInt(Math.max(1, height));
                int h = 8 + random.nextInt(Math.max(9, height / 18));
                int w = width / 8 + random.nextInt(Math.max(2, width / 2));
                int x = random.nextInt(Math.max(1, width + w / 2)) - w / 2;
                int color = random.nextBoolean() ? 0x183C0874 : 0x182D166A;
                graphics.fill(x - 12, Math.max(0, y - 3), Math.min(width, x + w + 12), Math.min(height, y + h + 3), color);
                graphics.fill(x, y, Math.min(width, x + w), Math.min(height, y + h), color | 0x08000000);
            }
            int fragments = Math.max(52, width / 28);
            for (int i = 0; i < fragments; i++) {
                int y = random.nextInt(Math.max(1, height));
                int h = 1 + random.nextInt(Math.max(2, height / 130));
                int w = width / 40 + random.nextInt(Math.max(2, width / 7));
                int x = random.nextInt(Math.max(1, width + w / 2)) - w / 2;
                int palette = random.nextInt(6);
                int color = switch (palette) {
                    case 0 -> 0x9ACB16FF;
                    case 1 -> 0x826827FF;
                    case 2 -> 0x824C00B8;
                    case 3 -> 0x70380088;
                    case 4 -> 0x824F16C7;
                    default -> 0x708D38FF;
                };
                renderFragment(graphics, random, x, y, w, h, color);
            }
            int tearY = random.nextInt(Math.max(1, height));
            int tearH = 2 + random.nextInt(5);
            graphics.fill(0, tearY, width, Math.min(height, tearY + tearH), 0x8A9D24FF);
            graphics.fill(width / 10, Math.min(height, tearY + tearH), width * 9 / 10,
                    Math.min(height, tearY + tearH + 2), 0x8A25DFFF);
        }

        private void renderFragment(GuiGraphics graphics, Random random, int x, int y, int width, int height, int color) {
            int segments = 1 + random.nextInt(4);
            int cursor = x;
            int remaining = width;
            for (int segment = 0; segment < segments && remaining > 2; segment++) {
                int gap = segment == 0 ? 0 : 3 + random.nextInt(Math.max(4, width / 10));
                cursor += gap;
                remaining -= gap;
                if (remaining <= 2) break;
                int segmentWidth = segment == segments - 1 ? remaining
                        : Math.max(2, remaining / (segments - segment) - random.nextInt(Math.max(2, remaining / 7)));
                int end = Math.min(this.width, cursor + segmentWidth);
                int glow = (color & 0x00FFFFFF) | 0x26000000;
                graphics.fill(cursor - 5, Math.max(0, y - 2), Math.min(this.width, end + 8), Math.min(this.height, y + height + 3), glow);
                graphics.fill(cursor - 3, Math.max(0, y - 1), Math.max(cursor - 2, end - 3), Math.min(this.height, y + height), 0x5CE927FF);
                graphics.fill(cursor + 3, Math.min(this.height, y + 1), Math.min(this.width, end + 4), Math.min(this.height, y + height + 1), 0x5C25DCFF);
                graphics.fill(cursor, y, end, Math.min(this.height, y + height), color);
                if (height > 2 && random.nextBoolean()) {
                    int highlight = random.nextBoolean() ? 0x7037DFFF : 0x70E22CFF;
                    graphics.fill(cursor + 2, y, end, Math.min(this.height, y + 1), highlight);
                }
                if (segmentWidth > 12 && random.nextBoolean()) {
                    int splice = cursor + segmentWidth / 3 + random.nextInt(Math.max(2, segmentWidth / 3));
                    graphics.fill(splice, y, Math.min(this.width, end + 2), Math.min(this.height, y + Math.max(1, height / 2)),
                            random.nextBoolean() ? 0xAEE927FF : 0xAE25DCFF);
                }
                cursor = end;
                remaining -= segmentWidth;
            }
        }

        private void renderTextSlices(GuiGraphics graphics, Random random, String text, float scale) {
            int centerY = height / 2;
            for (int slice = 0; slice < 7; slice++) {
                int sliceY = centerY - 13 + random.nextInt(26);
                int sliceHeight = 1 + random.nextInt(4);
                int offset = (random.nextBoolean() ? 1 : -1) * (3 + random.nextInt(8));
                int color = random.nextBoolean() ? 0xE8FF2BE9 : 0xE82BE4FF;
                graphics.enableScissor(0, Math.max(0, sliceY), width, Math.min(height, sliceY + sliceHeight));
                graphics.pose().pushPose();
                graphics.pose().translate(width / 2.0F + offset, height / 2.0F, 0.0F);
                graphics.pose().scale(scale, scale, 1.0F);
                graphics.drawCenteredString(font, text, 0, 0, color);
                graphics.pose().popPose();
                graphics.disableScissor();
            }
        }
    }
}

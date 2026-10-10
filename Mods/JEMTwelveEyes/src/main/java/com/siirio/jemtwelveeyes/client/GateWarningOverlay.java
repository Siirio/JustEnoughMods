package com.siirio.jemtwelveeyes.client;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = JEMTwelveEyes.MOD_ID, value = Dist.CLIENT)
public final class GateWarningOverlay {
    private static final int TICKS_PER_SECOND = 20;
    private static final int HORIZONTAL_MARGIN = 24;
    private static final int VERTICAL_MARGIN = 16;
    private static final int PANEL_PADDING = 8;
    private static final int LINE_GAP = 2;
    private static final int SECTION_GAP = 7;
    private static final int TITLE_COLOR = 0xFFFF5555;
    private static final int DETAILS_COLOR = 0xFFFFFFFF;
    private static final int PANEL_COLOR = 0xFF000000;
    private static final int PANEL_ALPHA = 176;
    private static final float MIN_SCALE = 0.6F;
    private static final double FADE_SECONDS = 0.5D;
    private static Component title = Component.empty();
    private static Component details = Component.empty();
    private static long shownAt;
    private static long expiresAt;
    private static int cachedWidth = -1;
    private static int cachedHeight = -1;
    private static GateWarningLayout cachedLayout;

    private GateWarningOverlay() {
    }

    public static void show(Component newTitle, Component newDetails, int durationTicks) {
        title = newTitle;
        details = newDetails;
        shownAt = System.nanoTime();
        expiresAt = shownAt + secondsToNanos((double) durationTicks / TICKS_PER_SECOND);
        cachedLayout = null;
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void unloaded(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clear();
    }

    private static void clear() {
        title = Component.empty();
        details = Component.empty();
        shownAt = 0L;
        expiresAt = 0L;
        cachedWidth = -1;
        cachedHeight = -1;
        cachedLayout = null;
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        long now = System.nanoTime();
        Minecraft minecraft = Minecraft.getInstance();
        if (now >= expiresAt || minecraft.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int availableWidth = Math.max(font.width("...") + PANEL_PADDING * 2, screenWidth - HORIZONTAL_MARGIN * 2);
        int availableHeight = Math.max(font.lineHeight, screenHeight - VERTICAL_MARGIN * 2);
        GateWarningLayout layout = layout(font, availableWidth, availableHeight);
        int alpha = Math.round(255.0F * fade(now));
        int panelColor = alpha(PANEL_COLOR, Math.round(PANEL_ALPHA * alpha / 255.0F));
        int left = (screenWidth - layout.panelWidth()) / 2;
        int top = (screenHeight - layout.panelHeight()) / 2;
        graphics.fill(left, top, left + layout.panelWidth(), top + layout.panelHeight(), panelColor);
        graphics.pose().pushPose();
        graphics.pose().translate(screenWidth / 2.0F, top + PANEL_PADDING, 0.0F);
        graphics.pose().scale(layout.scale(), layout.scale(), 1.0F);
        int y = 0;
        for (FormattedCharSequence line : layout.titleLines()) {
            graphics.drawCenteredString(font, line, 0, y, alpha(TITLE_COLOR, alpha));
            y += font.lineHeight + LINE_GAP;
        }
        y += SECTION_GAP;
        for (FormattedCharSequence line : layout.detailLines()) {
            graphics.drawCenteredString(font, line, 0, y, alpha(DETAILS_COLOR, alpha));
            y += font.lineHeight + LINE_GAP;
        }
        graphics.pose().popPose();
    }

    private static GateWarningLayout layout(Font font, int availableWidth, int availableHeight) {
        if (cachedLayout != null && cachedWidth == availableWidth && cachedHeight == availableHeight) {
            return cachedLayout;
        }
        float scale = 1.0F;
        GateWarningLayout layout;
        do {
            int contentWidth = Math.max(1, Math.round((availableWidth - PANEL_PADDING * 2) / scale));
            List<FormattedCharSequence> titleLines = new ArrayList<>(font.split(title, contentWidth));
            List<FormattedCharSequence> detailLines = new ArrayList<>(font.split(details, contentWidth));
            int contentHeight = lineHeight(font, titleLines.size() + detailLines.size()) + SECTION_GAP;
            int panelHeight = Math.round(contentHeight * scale) + PANEL_PADDING * 2;
            int panelWidth = Math.min(availableWidth, widest(font, titleLines, detailLines, scale) + PANEL_PADDING * 2);
            layout = new GateWarningLayout(titleLines, detailLines, scale, panelWidth, panelHeight);
            scale -= 0.05F;
        } while (layout.panelHeight() > availableHeight && scale >= MIN_SCALE);
        cachedWidth = availableWidth;
        cachedHeight = availableHeight;
        cachedLayout = layout;
        return layout;
    }

    private static int lineHeight(Font font, int count) {
        return count == 0 ? 0 : count * font.lineHeight + (count - 1) * LINE_GAP;
    }

    private static int widest(Font font, List<FormattedCharSequence> titleLines,
                              List<FormattedCharSequence> detailLines, float scale) {
        int widest = 0;
        for (FormattedCharSequence line : titleLines) {
            widest = Math.max(widest, Math.round(font.width(line) * scale));
        }
        for (FormattedCharSequence line : detailLines) {
            widest = Math.max(widest, Math.round(font.width(line) * scale));
        }
        return widest;
    }

    private static float fade(long now) {
        double elapsed = nanosToSeconds(now - shownAt);
        double remaining = nanosToSeconds(expiresAt - now);
        return (float) Mth.clamp(Math.min(elapsed / FADE_SECONDS, remaining / FADE_SECONDS), 0.0D, 1.0D);
    }

    private static int alpha(int color, int alpha) {
        return color & 0x00FFFFFF | alpha << 24;
    }

    private static long secondsToNanos(double seconds) {
        return (long) (seconds * 1_000_000_000L);
    }

    private static double nanosToSeconds(long nanos) {
        return nanos / 1_000_000_000.0D;
    }

}

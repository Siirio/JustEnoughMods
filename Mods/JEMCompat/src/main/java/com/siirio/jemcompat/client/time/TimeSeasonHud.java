package com.siirio.jemcompat.client.time;

import com.siirio.jemcompat.JEMCompat;
import com.siirio.jemcompat.compat.seasons.SeasonHudAdapter;
import com.siirio.jemcompat.config.JEMClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Locale;

/** Player-inventory presentation of native synchronized values, with no separate calendar. */
@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID, value = Dist.CLIENT)
public final class TimeSeasonHud {
    private static final ResourceLocation DAY = new ResourceLocation(JEMCompat.MOD_ID, "textures/gui/time/day_cycle.png");
    private static final ResourceLocation SEASON = new ResourceLocation(JEMCompat.MOD_ID, "textures/gui/time/season_cycle.png");
    private static final int SIZE = 24;

    private TimeSeasonHud() {}

    @SubscribeEvent
    public static void renderScreen(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen) {
            draw(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    private static void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        boolean day = JEMClientConfig.SHOW_TIME_HUD.get();
        boolean season = JEMClientConfig.SHOW_SEASON_HUD.get() && ModList.get().isLoaded("sereneseasons");
        if (!day && !season) return;
        int width = day && season ? SIZE * 2 + 4 : SIZE;
        int x = Math.max(0, Math.min(graphics.guiWidth() - width,
                (graphics.guiWidth() - width) / 2 + JEMClientConfig.TIME_HUD_OFFSET_X.get()));
        int y = Math.max(0, Math.min(JEMClientConfig.TIME_HUD_OFFSET_Y.get(), graphics.guiHeight() - SIZE));
        List<Component> tooltip = List.of();
        if (day) {
            graphics.blit(DAY, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
            // Native celestial angle respects the dimension's fixed time and current synchronized world time.
            double angle = mc.level.getTimeOfDay(partialTick);
            long clock = Math.floorMod(mc.level.dimensionType().fixedTime().orElse(mc.level.getDayTime()), 24000L);
            marker(graphics, x, y, angle, clock < 13000 ? 0xFFFFD56B : 0xFFE0EBFF);
            if (hovered(x, y, mouseX, mouseY)) {
                String phase = clock < 1000 || clock >= 23000 ? "dawn" : clock < 12000 ? "day" : clock < 14000 ? "dusk" : "night";
                int minutes = (int) ((clock + 6000) % 24000 * 1440 / 24000);
                String time = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
                tooltip = List.of(Component.translatable("hud.jemcompat.phase." + phase),
                        Component.translatable("hud.jemcompat.world_clock", time));
            }
            x += SIZE + 4;
        }
        if (season) {
            var state = SeasonHudAdapter.read(mc.level, mc.player.blockPosition());
            if (!state.active()) graphics.setColor(0.45F, 0.45F, 0.45F, 0.8F);
            graphics.blit(SEASON, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
            graphics.setColor(1, 1, 1, 1);
            // Each season owns a quarter of the wheel: Spring starts at the upper-left boundary.
            if (state.active()) marker(graphics, x, y, state.cycleFraction() - 0.125, 0xFFFFF5D6);
            if (hovered(x, y, mouseX, mouseY)) tooltip = state.tooltip();
        }
        if (!tooltip.isEmpty()) graphics.renderComponentTooltip(mc.font, tooltip, mouseX, mouseY);
    }

    private static boolean hovered(int x, int y, int mouseX, int mouseY) {
        int dx = mouseX - (x + 12), dy = mouseY - (y + 12);
        return mouseX >= 0 && dx * dx + dy * dy <= 144;
    }

    private static void marker(GuiGraphics graphics, int x, int y, double fraction, int color) {
        double angle = fraction * Math.PI * 2 - Math.PI / 2;
        int px = x + 12 + (int) Math.round(Math.cos(angle) * 9);
        int py = y + 12 + (int) Math.round(Math.sin(angle) * 9);
        graphics.fill(px - 2, py - 2, px + 2, py + 2, 0xFF30291F);
        graphics.fill(px - 1, py - 1, px + 1, py + 1, color);
    }
}

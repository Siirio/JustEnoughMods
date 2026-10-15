package com.siirio.jemroles.client;

import com.siirio.jemroles.race.RaceId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Small readable trait sheet opened from a compact race-card icon. */
public final class RaceTraitDetailsScreen extends Screen {
    private final Screen parent;
    private final RaceId race;
    private final boolean positive;
    private final int index;

    public RaceTraitDetailsScreen(Screen parent, RaceId race, boolean positive, int index) {
        super(Component.translatable(key(race, positive, index)));
        this.parent = parent;
        this.race = race;
        this.positive = positive;
        this.index = index;
    }

    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> minecraft.setScreen(parent))
                .bounds(width / 2 - 45, height / 2 + 48, 90, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(360, width - 32), panelHeight = 138;
        int x = (width - panelWidth) / 2, y = (height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xF21B1614);
        frame(graphics, x, y, panelWidth, panelHeight, positive ? 0xFF73B879 : 0xFFE06D57);
        graphics.renderItem(new ItemStack(RaceSelectionScreen.traitIcon(race, index, positive)), x + 18, y + 18);
        graphics.drawString(font, title, x + 44, y + 22, positive ? 0xFFB7DB88 : 0xFFFF9B83, false);
        int lineY = y + 50;
        for (var line : font.split(Component.translatable(detailKey()), panelWidth - 36)) {
            graphics.drawString(font, line, x + 18, lineY, 0xFFE2D5C5, false);
            lineY += 11;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    private String detailKey() { return base(race) + (positive ? ".buff.detail." : ".debuff.detail.") + index; }
    private static String key(RaceId race, boolean positive, int index) {
        return base(race) + (positive ? ".buff." : ".debuff.") + index;
    }
    private static String base(RaceId race) { return "jem.roles.race." + race.key(); }
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 2, c); g.fill(x, y + h - 2, x + w, y + h, c);
        g.fill(x, y, x + 2, y + h, c); g.fill(x + w - 2, y, x + w, y + h, c);
    }
}

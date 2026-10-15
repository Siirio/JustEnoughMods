package com.siirio.jemroles.client;

import com.siirio.jemroles.network.RaceStatusPacket;
import com.siirio.jemroles.race.RaceId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Compact modal with the exact race mechanics; keeps the carousel readable. */
public final class RacePowerDetailsScreen extends Screen {
    private final Screen parent;
    private final RaceStatusPacket status;
    private final RaceId race;
    public RacePowerDetailsScreen(Screen parent, RaceStatusPacket status, RaceId race) {
        super(Component.translatable("jem.roles.power_details")); this.parent = parent; this.status = status; this.race = race;
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> minecraft.setScreen(parent))
                .bounds(width / 2 - 45, height / 2 + 102, 90, 20).build());
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g); int w = Math.min(390, width - 28), h = 230, x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xF21B1614); frame(g, x, y, w, h, 0xFFE0A552);
        g.renderItem(new ItemStack(RaceSelectionScreen.powerIcon(race)), x + 18, y + 16);
        g.drawString(font, Component.translatable(base() + ".spell"), x + 42, y + 20, 0xFFFFD58E, false);
        int lineY = y + 45;
        lineY = paragraph(g, Component.translatable(base() + ".spell.detail"), x + 18, lineY, w - 36, 0xFFE2D5C5);
        lineY += 6;
        for (int tier = 1; tier <= 3; tier++) {
            int color = status.selected() && status.race() == race && tier <= status.spellTier() ? 0xFFB7DB88 : 0xFF9A8B7D;
            lineY = paragraph(g, Component.literal("◆ ").append(Component.translatable(base() + ".tier." + tier)), x + 18, lineY, w - 36, color) + 3;
        }
        lineY += 3;
        paragraph(g, Component.translatable(base() + ".mastery.detail"), x + 18, lineY, w - 36, 0xFFBAA78F);
        super.render(g, mouseX, mouseY, partialTick);
    }
    private int paragraph(GuiGraphics g, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, width)) { g.drawString(font, line, x, y, color, false); y += 10; }
        return y;
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    private String base() { return "jem.roles.race." + race.key(); }
    private static void frame(GuiGraphics g,int x,int y,int w,int h,int c){g.fill(x,y,x+w,y+2,c);g.fill(x,y+h-2,x+w,y+h,c);g.fill(x,y,x+2,y+h,c);g.fill(x+w-2,y,x+w,y+h,c);}
}

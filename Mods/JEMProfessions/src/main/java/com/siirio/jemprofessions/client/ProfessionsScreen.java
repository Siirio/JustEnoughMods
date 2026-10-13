package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.ProfessionStatusPacket;
import com.siirio.jemprofessions.progress.ProfessionFormula;
import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ProfessionsScreen extends Screen {
    private final ProfessionStatusPacket status;
    public ProfessionsScreen(ProfessionStatusPacket status) { super(Component.translatable("jem.professions.screen.title")); this.status = status; }

    @Override protected void init() {
        int cardW = 190, cardH = 43, gap = 6;
        int startX = (width - cardW * 2 - gap) / 2;
        int startY = Math.max(38, (height - (cardH + gap) * 5) / 2);
        ProfessionId[] ids = ProfessionId.values();
        for (int i = 0; i < ids.length; i++) {
            ProfessionId id = ids[i];
            int x = startX + (i % 2) * (cardW + gap), y = startY + (i / 2) * (cardH + gap);
            addRenderableWidget(Button.builder(Component.translatable("jem.professions.view_tree"), button ->
                    minecraft.setScreen(new ProfessionTreeScreen(status, id))).bounds(x + cardW - 70, y + 12, 62, 20).build());
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cardW = 190, cardH = 43, gap = 6;
        int startX = (width - cardW * 2 - gap) / 2;
        int startY = Math.max(38, (height - (cardH + gap) * 5) / 2);
        graphics.drawCenteredString(font, title, width / 2, startY - 22, 0xFFFFE5C0);
        long[] xp = status.xp();
        ProfessionId[] ids = ProfessionId.values();
        for (int i = 0; i < ids.length; i++) {
            ProfessionId id = ids[i]; int x = startX + (i % 2) * (cardW + gap), y = startY + (i / 2) * (cardH + gap);
            graphics.fill(x, y, x + cardW, y + cardH, 0xE02C211B);
            graphics.fill(x, y, x + 3, y + cardH, 0xFFB66C35);
            int level = ProfessionFormula.levelFor(xp[id.ordinal()]);
            graphics.drawString(font, Component.translatable(nameKey(id)), x + 9, y + 7, 0xFFFFE5C0, false);
            graphics.drawString(font, Component.literal("L" + level + "/50  XP " + xp[id.ordinal()]), x + 9, y + 24, 0xFFCDB99C, false);
        }
        if (!status.authenticated()) graphics.drawCenteredString(font, Component.translatable("jem.professions.identity_required"), width / 2, height - 20, 0xFFFF8888);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    static String nameKey(ProfessionId id) { return "jem.professions.name." + id.name().toLowerCase(); }
}

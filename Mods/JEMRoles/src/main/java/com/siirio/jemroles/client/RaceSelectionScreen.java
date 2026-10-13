package com.siirio.jemroles.client;

import com.siirio.jemroles.network.RaceStatusPacket;
import com.siirio.jemroles.network.RolesNetwork;
import com.siirio.jemroles.race.RaceId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RaceSelectionScreen extends Screen {
    private static final int PANEL = 112;
    private final RaceStatusPacket status;

    public RaceSelectionScreen(RaceStatusPacket status) {
        super(Component.translatable("jem.roles.screen.title"));
        this.status = status;
    }

    @Override
    protected void init() {
        int total = PANEL * 3 + 16;
        int start = (width - total) / 2;
        int y = Math.max(78, height / 2 + 46);
        for (int i = 0; i < RaceId.values().length; i++) {
            RaceId race = RaceId.values()[i];
            Button button = Button.builder(Component.translatable("jem.roles.select"), value -> confirm(race))
                    .bounds(start + i * (PANEL + 8) + 8, y, PANEL - 16, 20).build();
            button.active = status.authenticated() && (!status.selected() || status.freeSwitchAvailable() || status.race() == race);
            addRenderableWidget(button);
        }
    }

    private void confirm(RaceId race) {
        if (status.selected() && status.race() == race) return;
        Minecraft.getInstance().setScreen(new ConfirmScreen(accepted -> {
            if (accepted) RolesNetwork.choose(race, status.nonce());
            Minecraft.getInstance().setScreen(accepted ? null : this);
        }, Component.translatable("jem.roles.confirm.title"),
                Component.translatable("jem.roles.confirm.body", Component.translatable(key(race)))));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int total = PANEL * 3 + 16;
        int start = (width - total) / 2;
        int top = Math.max(34, height / 2 - 82);
        graphics.drawCenteredString(font, title, width / 2, top - 22, 0xFFF4DFC1);
        for (int i = 0; i < RaceId.values().length; i++) {
            RaceId race = RaceId.values()[i];
            int x = start + i * (PANEL + 8);
            int border = status.selected() && status.race() == race ? 0xFFCE8A42 : 0xFF6D4B32;
            graphics.fill(x, top, x + PANEL, top + 112, 0xE02C211B);
            graphics.fill(x, top, x + PANEL, top + 2, border);
            graphics.fill(x, top + 110, x + PANEL, top + 112, border);
            graphics.drawCenteredString(font, Component.translatable(key(race)), x + PANEL / 2, top + 10, 0xFFFFE7C7);
            for (int line = 1; line <= 3; line++) {
                Component text = Component.translatable("jem.roles.trait." + race.name().toLowerCase() + "." + line);
                graphics.drawWordWrap(font, text, x + 8, top + 30 + (line - 1) * 24, PANEL - 16, 0xFFD4C2A9);
            }
        }
        Component footer = !status.authenticated() ? Component.translatable("jem.roles.identity_required")
                : status.selected() && !status.freeSwitchAvailable() ? Component.translatable("jem.roles.change_locked")
                : status.selected() ? Component.translatable("jem.roles.free_change", 36000L - status.activeTicks())
                : Component.translatable("jem.roles.provisional");
        graphics.drawCenteredString(font, footer, width / 2, top + 126, 0xFFE1BE83);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String key(RaceId race) { return "jem.roles.race." + race.name().toLowerCase(); }
}

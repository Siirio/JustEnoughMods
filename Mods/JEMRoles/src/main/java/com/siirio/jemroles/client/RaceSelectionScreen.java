package com.siirio.jemroles.client;

import com.siirio.jemroles.network.RaceStatusPacket;
import com.siirio.jemroles.network.RolesNetwork;
import com.siirio.jemroles.race.RaceId;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RaceSelectionScreen extends Screen {
    private static final int VISIBLE = 3;
    private static final int GAP = 8;
    private final RaceStatusPacket status;
    private int first;

    public RaceSelectionScreen(RaceStatusPacket status) {
        super(Component.translatable("jem.roles.screen.title"));
        this.status = status;
        int selected = status.selected() ? status.race().ordinal() : 0;
        this.first = Math.floorMod(selected - 1, RaceId.values().length);
    }

    @Override protected void init() {
        clearWidgets();
        Layout layout = layout();
        for (int slot = 0; slot < VISIBLE; slot++) {
            RaceId race = raceAt(slot);
            int x = layout.left + slot * (layout.cardWidth + GAP);
            Button select = Button.builder(Component.translatable("jem.roles.select"), value -> confirm(race))
                    .bounds(x + 8, layout.bottom - 28, layout.cardWidth - 16, 20).build();
            select.active = status.authenticated() && (!status.selected() || status.freeSwitchAvailable() || status.race() == race);
            addRenderableWidget(select);
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> page(-1)).bounds(layout.left - 40, height / 2 - 10, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page(1)).bounds(layout.right + 20, height / 2 - 10, 20, 20).build());
    }

    private void page(int direction) {
        first = Math.floorMod(first + direction, RaceId.values().length);
        rebuildWidgets();
    }

    private void confirm(RaceId race) {
        if (status.selected() && status.race() == race) return;
        Minecraft.getInstance().setScreen(new ConfirmScreen(accepted -> {
            if (accepted) RolesNetwork.choose(race, status.nonce());
            Minecraft.getInstance().setScreen(accepted ? null : this);
        }, Component.translatable("jem.roles.confirm.title"),
                Component.translatable("jem.roles.confirm.body", Component.translatable(name(race)))));
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) { page(delta < 0 ? 1 : -1); return true; }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        Layout layout = layout();
        g.drawCenteredString(font, title, width / 2, 18, 0xFFFFE7C7);
        g.drawCenteredString(font, Component.translatable("jem.roles.screen.subtitle"), width / 2, 31, 0xFFD4C2A9);
        drawPeek(g, raceAt(-1), layout.left - 18, layout);
        drawPeek(g, raceAt(VISIBLE), layout.right - 26, layout);
        for (int slot = 0; slot < VISIBLE; slot++) drawCard(g, raceAt(slot), layout.left + slot * (layout.cardWidth + GAP), layout);
        Component footer = !status.authenticated() ? Component.translatable("jem.roles.identity_required")
                : status.selected() && !status.freeSwitchAvailable() ? Component.translatable("jem.roles.change_locked")
                : Component.translatable("jem.roles.free_first_choice");
        g.drawCenteredString(font, footer, width / 2, height - 15, 0xFFE1BE83);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPeek(GuiGraphics g, RaceId race, int x, Layout layout) {
        int width = 44;
        g.fill(x, layout.top + 12, x + width, layout.bottom - 12, 0xB51A1512);
        g.fill(x, layout.top + 12, x + width, layout.top + 14, 0x886D4B32);
        g.fill(x, layout.bottom - 14, x + width, layout.bottom - 12, 0x886D4B32);
        g.renderItem(new ItemStack(icon(race)), x + 14, layout.top + 24);
        g.fill(x, layout.top + 12, x + width, layout.bottom - 12, 0x66201916);
    }

    private RaceId raceAt(int offset) {
        return RaceId.values()[Math.floorMod(first + offset, RaceId.values().length)];
    }

    private void drawCard(GuiGraphics g, RaceId race, int x, Layout layout) {
        boolean selected = status.selected() && status.race() == race;
        int border = selected ? 0xFFD99B4E : 0xFF6D4B32;
        g.fill(x, layout.top, x + layout.cardWidth, layout.bottom, 0xE82C211B);
        g.fill(x, layout.top, x + layout.cardWidth, layout.top + 2, border);
        g.fill(x, layout.bottom - 2, x + layout.cardWidth, layout.bottom, border);
        g.fill(x, layout.top, x + 2, layout.bottom, border);
        g.fill(x + layout.cardWidth - 2, layout.top, x + layout.cardWidth, layout.bottom, border);
        ItemStack icon = new ItemStack(icon(race));
        g.renderItem(icon, x + layout.cardWidth / 2 - 8, layout.top + 8);
        g.drawCenteredString(font, Component.translatable(name(race)), x + layout.cardWidth / 2, layout.top + 29, 0xFFFFE7C7);
        g.drawCenteredString(font, Component.translatable(base(race) + ".lore"), x + layout.cardWidth / 2, layout.top + 41, 0xFFD4C2A9);
        g.fill(x + 8, layout.top + 54, x + layout.cardWidth - 8, layout.top + 55, 0xFF8A5D36);
        int y = layout.top + 59;
        for (int i = 1; i <= 5; i++) y = line(g, x, y, layout.cardWidth, 0xFFB7DB88, "+ ", base(race) + ".buff." + i);
        y += 2;
        for (int i = 1; i <= 2; i++) y = line(g, x, y, layout.cardWidth, 0xFFE0876A, "- ", base(race) + ".debuff." + i);
        y += 3;
        g.drawString(font, Component.translatable(base(race) + ".spell"), x + 9, y, 0xFFE1BE83, false);
        y += 11;
        int tier = selected ? status.spellTier() : 1;
        for (int i = 1; i <= 3; i++) {
            int color = i <= tier ? 0xFFB7DB88 : 0xFF7F7265;
            g.drawString(font, Component.literal((i <= tier ? "◆ " : "◇ ")).append(Component.translatable(base(race) + ".tier." + i)), x + 9, y, color, false);
            y += 9;
        }
        if (selected) g.drawString(font, Component.translatable("jem.roles.progress", status.spellProgress(), 30), x + 9, y + 2, 0xFFD4C2A9, false);
    }

    private int line(GuiGraphics g, int x, int y, int width, int color, String prefix, String key) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(prefix).append(Component.translatable(key)), width - 18);
        for (var line : lines) { g.drawString(font, line, x + 9, y, color, false); y += 9; }
        return y + 1;
    }

    private Layout layout() {
        int cardWidth = Math.max(146, Math.min(218, (width - 72) / VISIBLE));
        int total = cardWidth * VISIBLE + GAP * (VISIBLE - 1);
        int left = (width - total) / 2;
        return new Layout(left, left + total, 46, height - 34, cardWidth);
    }

    private static net.minecraft.world.item.Item icon(RaceId race) {
        return switch (race) {
            case ANCESTORS -> Items.TOTEM_OF_UNDYING;
            case VOID_WALKERS -> Items.ENDER_PEARL;
            case BORN_IN_BLOOM -> Items.SPORE_BLOSSOM;
            case INFECTED_BY_SCULK -> Items.SCULK_CATALYST;
            case NETHER_HEARTEDS -> Items.MAGMA_CREAM;
        };
    }

    private static String name(RaceId race) { return base(race) + ".name"; }
    private static String base(RaceId race) { return "jem.roles.race." + race.key(); }
    private record Layout(int left, int right, int top, int bottom, int cardWidth) {}
}

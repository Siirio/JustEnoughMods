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
            addRenderableWidget(Button.builder(Component.translatable("jem.roles.details"), value ->
                    Minecraft.getInstance().setScreen(new RacePowerDetailsScreen(this, status, race)))
                    .bounds(x + 8, layout.bottom - 52, layout.cardWidth - 16, 18).build());
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
        drawPeek(g, raceAt(-1), layout.left - 18, layout);
        drawPeek(g, raceAt(VISIBLE), layout.right - 26, layout);
        for (int slot = 0; slot < VISIBLE; slot++) drawCard(g, raceAt(slot), layout.left + slot * (layout.cardWidth + GAP), layout, mouseX, mouseY);
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

    private void drawCard(GuiGraphics g, RaceId race, int x, Layout layout, int mouseX, int mouseY) {
        boolean selected = status.selected() && status.race() == race;
        int border = selected ? 0xFFFFC45A : accent(race);
        g.fill(x, layout.top, x + layout.cardWidth, layout.bottom, background(race));
        g.fill(x, layout.top, x + layout.cardWidth, layout.top + 2, border);
        g.fill(x, layout.bottom - 2, x + layout.cardWidth, layout.bottom, border);
        g.fill(x, layout.top, x + 2, layout.bottom, border);
        g.fill(x + layout.cardWidth - 2, layout.top, x + layout.cardWidth, layout.bottom, border);
        ItemStack icon = new ItemStack(icon(race));
        g.renderItem(icon, x + layout.cardWidth / 2 - 8, layout.top + 8);
        g.drawCenteredString(font, Component.translatable(name(race)), x + layout.cardWidth / 2, layout.top + 29, 0xFFFFE7C7);
        g.drawCenteredString(font, Component.translatable(base(race) + ".lore"), x + layout.cardWidth / 2, layout.top + 41, 0xFFE0D5C8);
        g.fill(x + 8, layout.top + 54, x + layout.cardWidth - 8, layout.top + 55, 0xFF8A5D36);
        int y = layout.top + 62;
        g.drawString(font, Component.translatable("jem.roles.strengths"), x + 9, y, 0xFFB7DB88, false);
        for (int i = 1; i <= 5; i++) g.renderItem(new ItemStack(traitIcon(race, i, true)), x + 9 + (i - 1) * 22, y + 11);
        y += 31;
        g.drawString(font, Component.translatable("jem.roles.weaknesses"), x + 9, y, 0xFFE0876A, false);
        for (int i = 1; i <= 2; i++) g.renderItem(new ItemStack(traitIcon(race, i, false)), x + 9 + (i - 1) * 22, y + 11);
        y += 34;
        g.fill(x + 8, y - 3, x + layout.cardWidth - 8, y - 2, 0x887B5A35);
        g.renderItem(new ItemStack(powerIcon(race)), x + 9, y);
        g.drawString(font, Component.translatable(base(race) + ".spell.short"), x + 30, y + 4, 0xFFFFD58E, false);
        y += 24;
        int tier = selected ? status.spellTier() : 1;
        for (int i = 1; i <= 3; i++) {
            int color = i <= tier ? 0xFFB7DB88 : 0xFF7F7265;
            g.drawString(font, Component.literal((i <= tier ? "◆ " : "◇ ")).append(Component.translatable(base(race) + ".tier.short." + i)), x + 9, y, color, false);
            y += 9;
        }
        if (selected) g.drawString(font, Component.translatable("jem.roles.progress", status.spellProgress(), 30), x + 9, y + 1, 0xFFD4C2A9, false);

        // Every compact icon explains itself on hover; full mechanics live in the clickable details panel.
        for (int i = 1; i <= 5; i++) if (inside(mouseX, mouseY, x + 9 + (i - 1) * 22, layout.top + 73, 16, 16))
            g.renderTooltip(font, Component.translatable(base(race) + ".buff." + i), mouseX, mouseY);
        for (int i = 1; i <= 2; i++) if (inside(mouseX, mouseY, x + 9 + (i - 1) * 22, layout.top + 104, 16, 16))
            g.renderTooltip(font, Component.translatable(base(race) + ".debuff." + i), mouseX, mouseY);
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

    private static int background(RaceId race) { return switch (race) {
        case ANCESTORS -> 0xEB30251A; case VOID_WALKERS -> 0xEB211A38; case BORN_IN_BLOOM -> 0xEB1C3422;
        case INFECTED_BY_SCULK -> 0xEB102F32; case NETHER_HEARTEDS -> 0xEB3B1A14;
    }; }
    private static int accent(RaceId race) { return switch (race) {
        case ANCESTORS -> 0xFFCC9A52; case VOID_WALKERS -> 0xFF9B6FE8; case BORN_IN_BLOOM -> 0xFF70B877;
        case INFECTED_BY_SCULK -> 0xFF38A7AA; case NETHER_HEARTEDS -> 0xFFE36B35;
    }; }
    static net.minecraft.world.item.Item powerIcon(RaceId race) { return switch (race) {
        case ANCESTORS -> Items.SOUL_LANTERN; case VOID_WALKERS -> Items.ENDER_EYE; case BORN_IN_BLOOM -> Items.FLOWERING_AZALEA;
        case INFECTED_BY_SCULK -> Items.ECHO_SHARD; case NETHER_HEARTEDS -> Items.FIRE_CHARGE;
    }; }
    private static net.minecraft.world.item.Item traitIcon(RaceId race, int index, boolean positive) {
        if (!positive) return switch (race) {
            case ANCESTORS -> index == 1 ? Items.ELYTRA : Items.CROSSBOW;
            case VOID_WALKERS -> index == 1 ? Items.WATER_BUCKET : Items.ROTTEN_FLESH;
            case BORN_IN_BLOOM -> index == 1 ? Items.FLINT_AND_STEEL : Items.DEAD_BUSH;
            case INFECTED_BY_SCULK -> index == 1 ? Items.SUNFLOWER : Items.GOAT_HORN;
            case NETHER_HEARTEDS -> index == 1 ? Items.HEART_OF_THE_SEA : Items.POWDER_SNOW_BUCKET;
        };
        net.minecraft.world.item.Item[] icons = switch (race) {
            case ANCESTORS -> new net.minecraft.world.item.Item[]{Items.TOTEM_OF_UNDYING, Items.WHEAT, Items.EMERALD, Items.GOLD_INGOT, Items.SOUL_LANTERN};
            case VOID_WALKERS -> new net.minecraft.world.item.Item[]{Items.CHORUS_FRUIT, Items.END_STONE, Items.ENDER_PEARL, Items.ENDER_EYE, Items.FEATHER};
            case BORN_IN_BLOOM -> new net.minecraft.world.item.Item[]{Items.FLOWERING_AZALEA, Items.MOSS_BLOCK, Items.OAK_SAPLING, Items.SUNFLOWER, Items.SPORE_BLOSSOM};
            case INFECTED_BY_SCULK -> new net.minecraft.world.item.Item[]{Items.SCULK_SENSOR, Items.SCULK_VEIN, Items.ECHO_SHARD, Items.SCULK_CATALYST, Items.EXPERIENCE_BOTTLE};
            case NETHER_HEARTEDS -> new net.minecraft.world.item.Item[]{Items.LAVA_BUCKET, Items.BLAZE_POWDER, Items.MAGMA_CREAM, Items.FIRE_CHARGE, Items.GOLDEN_SWORD};
        };
        return icons[Math.max(0, Math.min(icons.length - 1, index - 1))];
    }
    private static boolean inside(double px, double py, int x, int y, int w, int h) { return px >= x && px < x + w && py >= y && py < y + h; }

    @Override public boolean shouldCloseOnEsc() { return status.selected(); }
    @Override public void onClose() { if (status.selected()) super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }

    private static String name(RaceId race) { return base(race) + ".name"; }
    private static String base(RaceId race) { return "jem.roles.race." + race.key(); }
    private record Layout(int left, int right, int top, int bottom, int cardWidth) {}
}

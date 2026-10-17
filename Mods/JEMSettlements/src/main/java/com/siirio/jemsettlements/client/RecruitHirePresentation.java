package com.siirio.jemsettlements.client;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitHireMenu;
import com.talhanation.recruits.world.RecruitsGroup;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Pixel-sized native assets; no inventory, currency or recruit state is reconstructed here. */
public final class RecruitHirePresentation {
    private static final ResourceLocation OAK = new ResourceLocation("minecraft", "textures/block/dark_oak_planks.png");
    private static final ResourceLocation PARCHMENT = new ResourceLocation("jem_settlements", "textures/gui/recruit/parchment.png");
    private static final ResourceLocation HUD = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final int INK = 0xff352718;
    private static final String[] STATS = {"health", "max_health", "level", "attack", "kills", "speed", "xp", "armor", "morale"};
    private static final Item[] ICONS = {Items.APPLE, Items.GOLDEN_APPLE, Items.IRON_SWORD, Items.DIAMOND_SWORD,
            Items.SKELETON_SKULL, Items.LEATHER_BOOTS, Items.EXPERIENCE_BOTTLE, Items.SHIELD, Items.HONEY_BOTTLE};
    private RecruitHirePresentation() { }
    public static Component tr(String key, Object... args) { return Component.translatable("jem.settlements.recruit_gui." + key, args); }
    public static boolean requiresQuote() {
        return !Minecraft.getInstance().hasSingleplayerServer() && com.siirio.jemsettlements.network.SettlementNetwork.supported();
    }

    public static String groupName(RecruitsGroup group) {
        return Minecraft.getInstance().font.plainSubstrByWidth(groupFullName(group), 62);
    }
    private static String groupFullName(RecruitsGroup group) {
        // Server-created user names are data. Only the exact native default names are translated.
        return switch (group.getName()) {
            case "Infantry" -> tr("group.infantry").getString();
            case "Ranged" -> tr("group.ranged").getString();
            case "Cavalry" -> tr("group.cavalry").getString();
            case "Ranged Cavalry" -> tr("group.ranged_cavalry").getString();
            default -> group.getName();
        };
    }

    public static void wood(GuiGraphics g, int x, int y, int w, int h, boolean focused) {
        g.fill(x, y, x + w, y + h, 0xff21180f);
        for (int dy = 2; dy < h - 2; dy += 16) for (int dx = 2; dx < w - 2; dx += 16)
            g.blit(OAK, x + dx, y + dy, 0, 0, Math.min(16, w - 2 - dx), Math.min(16, h - 2 - dy), 16, 16);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, focused ? 0xffe6cf83 : 0xffa78248);
        g.fill(x + 1, y + 2, x + 2, y + h - 1, 0xffa78248);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xff392719);
        for (int dx : new int[]{2, w - 5}) for (int dy : new int[]{2, h - 5}) {
            g.fill(x + dx, y + dy, x + dx + 3, y + dy + 3, 0xff6a6556);
            g.fill(x + dx, y + dy, x + dx + 2, y + dy + 1, 0xffc4b994);
        }
    }

    public static void background(GuiGraphics g, Font font, AbstractRecruitEntity recruit, RecruitHireMenu menu,
                                  int x, int y, float rotation) {
        wood(g, x, y, 300, 223, false);
        g.blitNineSliced(PARCHMENT, x + 5, y + 19, 290, 198, 3, 16, 16, 0, 0);
        g.fill(x + 8, y + 23, x + 77, y + 97, 0xff413d35);
        g.fill(x + 10, y + 25, x + 75, y + 95, 0xff211b17);
        g.fill(x + 11, y + 26, x + 74, y + 27, 0xff72634a);
        int scale = Math.max(1, (int) Math.min(44F / Math.max(.1F, recruit.getBbHeight()), 46F / Math.max(.1F, recruit.getBbWidth())));
        g.enableScissor(x + 11, y + 36, x + 74, y + 85);
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x + 42, y + 83, scale, rotation / 20F, 0, recruit);
        g.disableScissor();
        // Draw exactly the native 36 player slots, using the native menu coordinates.
        for (var slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xff716653);
            g.fill(sx, sy, sx + 17, sy + 17, 0xff30261f);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff211b17);
            g.fill(sx + 1, sy + 17, sx + 18, sy + 18, 0xffd0bb8b);
        }
        g.fill(x + 179, y + 125, x + 180, y + 212, 0xffa49168);
    }

    public static void labels(GuiGraphics g, Font font, AbstractRecruitEntity recruit, HireQuoteClient.Quote quote) {
        g.drawString(font, tr("title"), 9, 6, 0xffffe3a3, true);
        g.drawString(font, font.plainSubstrByWidth(recruit.getName().getString(), 60), 12, 27, 0xfff8e9c5, false);
        g.drawCenteredString(font, tr(recruit.getIsOwned() ? "employed" : "available"), 42, 86, 0xffdbc99b);
        String[] values = values(recruit,quote);
        for (int i = 0; i < STATS.length; i++) {
            int sx = 85 + (i % 2) * 104, sy = 24 + (i / 2) * 15;
            if (i < 2) g.blit(HUD, sx + 3, sy + 3, 52, 0, 9, 9);
            else g.renderFakeItem(new ItemStack(ICONS[i]), sx, sy);
            g.drawString(font, values[i], sx + 20, sy + 4, INK, false);
            if (i == 1) g.drawString(font, tr("max"), sx + 54, sy + 4, 0xff77613b, false);
        }
        g.drawString(font, tr("player_inventory"), 8, 128, INK, false);
        // Fee and weekly wages are shown once each. The primary action contains no currency icon.
        int row = wrapped(g, font, tr("fee"), 186, 128, 101, INK);
        if (quote != null) {
            SettlementPriceRenderer.draw(g, font, quote.cost(), 186, row + 1, 102, INK, quote.coins());
            int wages = wrapped(g, font, tr("weekly_wage"), 186, row + 23, 102, INK);
            SettlementPriceRenderer.draw(g, font, quote.wage(), 186, wages + 1, 102, INK, quote.coins());
        } else if (requiresQuote()) {
            wrapped(g, font, tr("pending"), 186, row + 3, 102, INK);
        } else if (ClientManager.currency != null) {
            g.renderFakeItem(ClientManager.currency, 186, row + 1);
            g.drawString(font, Integer.toString(recruit.getCost()), 205, row + 5, INK, false);
        }
    }

    private static String[] values(AbstractRecruitEntity recruit,HireQuoteClient.Quote quote) {
        if(quote!=null){var s=quote.stats();return new String[]{decimal(s.health()),decimal(s.maxHealth()),Integer.toString(s.level()),decimal(s.attack()),Integer.toString(s.kills()),decimal(s.speed()),Integer.toString(s.xp()),Integer.toString(s.armor()),decimal(s.morale())};}
        if(requiresQuote())return new String[]{"вЂ”","вЂ”","вЂ”","вЂ”","вЂ”","вЂ”","вЂ”","вЂ”","вЂ”"};
        return new String[]{decimal(recruit.getHealth()), decimal(recruit.getMaxHealth()), Integer.toString(recruit.getXpLevel()),
                decimal(recruit.getAttributeValue(Attributes.ATTACK_DAMAGE)), Integer.toString(recruit.getKills()), decimal(recruit.getAttributeValue(Attributes.MOVEMENT_SPEED)),
                Integer.toString(recruit.getXp()), Integer.toString(recruit.getArmorValue()), decimal(recruit.getMorale())};
    }
    private static String decimal(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private static int wrapped(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, width)) { g.drawString(font, line, x, y, color, false); y += 10; }
        return y;
    }
    public static void tooltips(GuiGraphics g, Font font, AbstractRecruitEntity recruit, int x, int y, int mx, int my) {
        int rx = mx - x, ry = my - y;
        for (int i = 0; i < STATS.length; i++) {
            int sx = 85 + (i % 2) * 104, sy = 24 + (i / 2) * 15;
            if (rx >= sx && rx < sx + 101 && ry >= sy && ry < sy + 15) {
                g.renderComponentTooltip(font, List.of(tr("stat." + STATS[i], values(recruit,HireQuoteClient.quote(recruit.getId()))[i])), mx, my); return;
            }
        }
        if (rx >= 9 && rx < 76 && ry >= 25 && ry < 97)
            g.renderComponentTooltip(font, List.of(recruit.getName(), tr("rotate")), mx, my);
        else if (rx >= 180 && rx < 292 && ry >= 128 && ry < 212)
            g.renderComponentTooltip(font, List.of(tr("payroll_tooltip")), mx, my);
        else if (rx >= 94 && rx < 174 && ry >= 101 && ry < 121) {
            var selected = ClientManager.getSelectedGroup();
            g.renderComponentTooltip(font, selected == null ? List.of(tr("group_tooltip"))
                    : List.of(Component.literal(groupFullName(selected)), tr("group_tooltip")), mx, my);
        }
    }
}

package com.siirio.jemsettlements.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The host chooses currency. Optional Coin presentation is reached only for Coin snapshots. */
public final class SettlementPriceRenderer {
    public record Unit(String id, long value) { }
    private SettlementPriceRenderer() { }
    public static List<Unit> units() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_economy")) return List.of(new Unit("emerald", 1L));
        return com.siirio.jemsettlements.compat.economy.CoinPresentationAccess.units();
    }
    public static Component format(long amount, boolean coins) {
        return coins ? com.siirio.jemsettlements.compat.economy.CoinPricePresentation.format(amount, true)
                : Component.translatable("jem.settlements.currency.emerald_amount", amount);
    }
    public static Component formatAll(long amount, boolean coins) {
        return coins ? com.siirio.jemsettlements.compat.economy.CoinPresentationAccess.formatAll(amount) : format(amount, false);
    }
    public static void draw(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int color, boolean coins) {
        if (coins) { com.siirio.jemsettlements.compat.economy.CoinPricePresentation.draw(graphics, font, amount, x, y, width, color, true); return; }
        graphics.renderFakeItem(new ItemStack(Items.EMERALD), x, y);
        String count = Long.toString(amount);
        if (width > 30) graphics.drawString(font, count, x + 18, y + 4, color, false);
        else {
            graphics.pose().pushPose(); graphics.pose().translate(x + 15 - font.width(count) * .5F, y + 10, 200);
            graphics.pose().scale(.5F, .5F, 1F); graphics.drawString(font, count, 0, 0, color, true); graphics.pose().popPose();
        }
    }
    public static int fullHeight(Font font, long amount, int width, boolean coins) {
        return coins ? com.siirio.jemsettlements.compat.economy.CoinPricePresentation.fullHeight(font, amount, width, true) : 18;
    }
    public static void drawFull(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int color, boolean coins) {
        if (coins) com.siirio.jemsettlements.compat.economy.CoinPricePresentation.drawFull(graphics, font, amount, x, y, width, color, true);
        else draw(graphics, font, amount, x, y, width, color, false);
    }
    public static void tooltip(GuiGraphics graphics, Font font, long amount, int x, int y, int width, int height, boolean coins) {
        if (coins) com.siirio.jemsettlements.compat.economy.CoinPricePresentation.tooltip(graphics, font, amount, x, y, width, height, true);
        else graphics.renderTooltip(font, format(amount, false), x, y);
    }
}

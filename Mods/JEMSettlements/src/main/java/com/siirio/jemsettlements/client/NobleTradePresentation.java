package com.siirio.jemsettlements.client;

import com.talhanation.recruits.client.gui.component.RecruitsMultiLineEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ExtendedButton;

/** Material adapter for the native noble offer screen; never owns trade or group state. */
public final class NobleTradePresentation {
    private NobleTradePresentation() { }

    public static void background(GuiGraphics g, int x, int y) {
        RecruitHirePresentation.wood(g, x, y, 256, 200, false);
        g.fill(x + 5, y + 18, x + 91, y + 189, 0xb51b1814);
        g.fill(x + 95, y + 28, x + 250, y + 49, 0xb51b1814);
        g.fill(x + 95, y + 76, x + 250, y + 196, 0xb51b1814);
        SettlementMaterials.foliage(g, x + 235, y + 2);
        SettlementMaterials.foliage(g, x + 1, y + 181);
    }

    public static ExtendedButton button(Font font, Button nativeButton) {
        return new ExtendedButton(nativeButton.getX(), nativeButton.getY(), nativeButton.getWidth(), nativeButton.getHeight(),
                nativeButton.getMessage(), ignored -> nativeButton.onPress()) {
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                RecruitHirePresentation.wood(g, getX(), getY(), width, height, active && isHoveredOrFocused());
                g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 6,
                        active ? 0xfff9e5a6 : 0xffb1a18c);
            }
        };
    }

    public static RecruitsMultiLineEditBox description(Font font, RecruitsMultiLineEditBox nativeBox) {
        var result = new RecruitsMultiLineEditBox(font, nativeBox.getX(), nativeBox.getY(), nativeBox.getWidth(), 60,
                Component.empty(), nativeBox.getMessage()) {
            @Override protected void renderBackground(GuiGraphics g) {
                g.fill(getX(), getY(), getX() + width, getY() + height, 0xff72634a);
                g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xff211b17);
            }
        };
        result.setValue(nativeBox.getValue());
        result.setEnableEditing(false);
        return result;
    }

    public static void payroll(GuiGraphics g, Font font, NobleQuoteClient.Quote quote, int x, int y) {
        if (quote == null) {
            if (RecruitHirePresentation.requiresQuote()) {
                int row = y + 158;
                for (var line : font.split(RecruitHirePresentation.tr("pending"), 145)) {
                    g.drawString(font, line, x + 99, row, 0xffdbc99b, false);
                    row += 10;
                }
            }
            return;
        }
        for (int column = 0; column < 2; column++) {
            int left = x + 99 + column * 76, row = y + 156;
            for (var line : font.split(RecruitHirePresentation.tr(column == 0 ? "fee" : "weekly_wage"), 70)) {
                g.drawString(font, line, left, row, 0xffffffff, false);
                row += 10;
            }
            SettlementPriceRenderer.draw(g, font, column == 0 ? quote.cost() : quote.wage(),
                    left, y + 178, 70, 0xffffffff, quote.coins());
        }
    }
}

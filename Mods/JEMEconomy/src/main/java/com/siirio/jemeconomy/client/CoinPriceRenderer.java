package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemeconomy.registry.EconomyItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Native item assets with exact monetary counts; no physical stack is created. */
public final class CoinPriceRenderer {
    private record Part(CoinDenomination denomination, long count) {}
    private CoinPriceRenderer() {}
    public static void draw(GuiGraphics graphics, long copper, int x, int y, int width) {
        if (copper <= 0L) return;
        var font = Minecraft.getInstance().font;
        List<Part> parts = new ArrayList<>(); long remainder = copper;
        for (var denomination : CoinDenomination.descending()) {
            long count = remainder / denomination.value(); remainder %= denomination.value();
            if (count > 0L) parts.add(new Part(denomination, count));
        }
        int wanted = parts.stream().mapToInt(part -> 10 + font.width(Long.toString(part.count()))).sum();
        // A native payout cell is narrow: use an exact single-denomination amount there,
        // while the selected-offer strip and tooltip always expose the full composition.
        if (wanted > width / 0.75F) {
            parts = List.of(new Part(CoinDenomination.COPPER, copper));
            wanted = 10 + font.width(Long.toString(copper));
        }
        float scale = Math.min(1.0F, (float) width / wanted);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 120);
        graphics.pose().scale(scale, scale, 1);
        int offset = 0;
        for (var part : parts) {
            graphics.pose().pushPose();
            graphics.pose().translate(offset, 0, 0); graphics.pose().scale(0.5F, 0.5F, 1);
            graphics.renderFakeItem(new ItemStack(EconomyItems.item(part.denomination())), 0, 0);
            graphics.pose().popPose();
            graphics.drawString(font, Long.toString(part.count()), offset + 9, 0, 0xFFFFFF, true);
            offset += 10 + font.width(Long.toString(part.count()));
        }
        graphics.pose().popPose();
    }
}

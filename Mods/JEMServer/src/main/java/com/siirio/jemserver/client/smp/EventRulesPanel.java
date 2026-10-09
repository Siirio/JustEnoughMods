package com.siirio.jemserver.client.smp;

import static com.siirio.jemserver.client.ui.JemPalette.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

final class EventRulesPanel {
    static int render(GuiGraphics g, ListTag rules, int x, int y, int width) {
        var font = Minecraft.getInstance().font;
        int start = y;
        g.drawString(font, Component.translatable("jem.smp.rules"), x, y, CREAM, false);
        y += 18;
        for (int index = 0; index < rules.size(); index++) {
            var rule = rules.getCompound(index);
            var args = rule.getList("args", Tag.TAG_STRING);
            Object[] values = new Object[args.size()];
            for (int i = 0; i < args.size(); i++) values[i] = args.getString(i);
            var label = Component.literal("• ").append(Component.translatable(rule.getString("key"), values));
            for (var line : font.split(label, Math.max(30, width))) {
                g.drawString(font, line, x, y, MUTED, false);
                y += font.lineHeight + 3;
            }
            y += 3;
        }
        return y - start;
    }
    private EventRulesPanel() {}
}

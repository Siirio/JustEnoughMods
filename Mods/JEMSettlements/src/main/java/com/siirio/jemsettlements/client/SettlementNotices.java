package com.siirio.jemsettlements.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small, connection-scoped notices. Persistent causes stay visible in their management section. */
public final class SettlementNotices {
    private static final Map<String, Boolean> SEEN = new LinkedHashMap<>();
    private SettlementNotices() { }
    public static void clear() { SEEN.clear(); }
    public static void accept(CompoundTag view) {
        if (!view.contains("Notice")) return;
        String key = view.getString("NoticeId");
        if (SEEN.putIfAbsent(key, true) != null) return;
        while (SEEN.size() > 256) SEEN.remove(SEEN.keySet().iterator().next());
        String kind = view.getString("Notice");
        ItemStack icon = new ItemStack(switch (kind) { case "storage" -> Items.CHEST; case "rebellion", "payroll" -> Items.SHIELD; default -> Items.BELL; });
        Minecraft.getInstance().getToasts().addToast(new NoticeToast(icon,
                Component.translatable("jem.settlements.notice." + kind), Component.literal(view.getString("Name"))));
    }
    private record NoticeToast(ItemStack icon, Component title, Component subtitle) implements Toast {
        @Override public int height() { return 48; }
        @Override public Visibility render(GuiGraphics graphics, ToastComponent component, long elapsed) {
            var font = Minecraft.getInstance().font;
            graphics.fill(0, 0, width(), height(), 0xff55534c); graphics.fill(2, 2, width()-2, height()-2, 0xff292824);
            graphics.renderItem(icon, 7, 14);
            graphics.drawWordWrap(font, title, 29, 5, width()-34, 0xffe6d7ae);
            graphics.drawString(font, font.plainSubstrByWidth(subtitle.getString(), width()-34), 29, 33, 0xffb6b09d, false);
            return elapsed < 5000L ? Visibility.SHOW : Visibility.HIDE;
        }
    }
}

package com.siirio.jemserver.client.smp;

import static com.siirio.jemserver.client.ui.JemPalette.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class SmpSidebar {
    private static final List<String> SMP_TABS = List.of("parties", "events", "market", "settlements", "roles", "professions");
    private int x, y, width, rowHeight, availableHeight;
    private List<String> tabs = SMP_TABS;
    private net.minecraft.nbt.CompoundTag features = new net.minecraft.nbt.CompoundTag();
    public void features(net.minecraft.nbt.CompoundTag value) {
        features = value.copy();
        if (availableHeight > 0) updateRows();
    }
    private void updateRows() {
        tabs = SMP_TABS.stream().filter(this::available).toList();
        rowHeight = Math.max(18, Math.min(38, availableHeight / tabs.size()));
    }
    private boolean available(String tab) {
        String feature = tab.equals("market") || tab.equals("shops") ? "economy" : tab;
        return !java.util.Set.of("economy", "roles", "professions", "settlements", "recruits", "events", "prizes").contains(feature)
                || features.getBoolean(feature);
    }
    public void layout(int x, int y, int width, int availableHeight) {
        this.x = x; this.y = y; this.width = width;
        this.availableHeight = availableHeight;
        updateRows();
    }
    public void render(GuiGraphics g, String active, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < tabs.size(); i++) {
            String tab = tabs.get(i);
            int top = y + i * rowHeight;
            boolean selected = active.equals(tab), hovered = tab.equals(at(mouseX, mouseY));
            if (selected) g.fill(x, top, x + width, top + rowHeight - 4, ROW);
            else if (hovered) g.fill(x, top, x + width, top + rowHeight - 4, HOVER);
            g.fill(x, top + rowHeight - 5, x + width, top + rowHeight - 4, INSET);
            if (selected) {
                g.fill(x, top + 2, x + 3, top + rowHeight - 6, COPPER);
                g.fill(x + 3, top + 2, x + 4, top + rowHeight - 6, PEACH);
            }
            int iconSize = Math.min(24, rowHeight - 8);
            SmpIcons.draw(g, tab, x + 7, top + (rowHeight - 4 - iconSize) / 2, iconSize);
            String label = Component.translatable("jem.smp.sidebar." + tab).getString();
            g.drawString(font, font.plainSubstrByWidth(label, Math.max(12, width - 38)), x + 34, top + (rowHeight - 13) / 2, selected ? PEACH : CREAM, false);
        }
    }
    public String at(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y) return null;
        int row = (int)(mouseY - y) / rowHeight;
        return row < tabs.size() && (mouseY - y) % rowHeight < rowHeight - 4 ? tabs.get(row) : null;
    }

    public static boolean openExternal(String route) {
        if (route.equals("professions") && !net.minecraftforge.fml.ModList.get().isLoaded("jem_professions")) return true;
        if (route.equals("roles") && !net.minecraftforge.fml.ModList.get().isLoaded("jem_roles")) return true;
        var module = switch (route) {
            case "roles" -> com.siirio.jemserver.smp.SmpModuleOpen.Module.ROLES;
            case "professions" -> com.siirio.jemserver.smp.SmpModuleOpen.Module.PROFESSIONS;
            default -> null;
        };
        if (module == null || Minecraft.getInstance().getConnection() == null) return false;
        com.siirio.jemserver.smp.SmpNetwork.openModule(module);
        return true;
    }
}

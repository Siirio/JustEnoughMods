package com.siirio.jemserver.client.smp;

import static com.siirio.jemserver.client.ui.JemPalette.*;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class SmpSidebar {
    private static final List<String> SMP_TABS = List.of("home", "profile", "players", "parties", "events", "shops", "market", "prizes");
    private int x, y, width, rowHeight;
    private List<String> tabs = SMP_TABS;
    public void layout(int x, int y, int width, int availableHeight) {
        this.x = x; this.y = y; this.width = width;
        var routes = new java.util.ArrayList<>(SMP_TABS);
        var mods = net.minecraftforge.fml.ModList.get();
        if (mods.isLoaded("jem_settlements")) routes.add(routes.indexOf("prizes"), "settlements");
        if (mods.isLoaded("recruits")) routes.add(routes.indexOf("prizes"), "recruits");
        if (mods.isLoaded("jem_roles")) routes.add("roles");
        if (mods.isLoaded("jem_professions")) routes.add("professions");
        tabs = List.copyOf(routes);
        rowHeight = Math.max(18, Math.min(38, availableHeight / tabs.size()));
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

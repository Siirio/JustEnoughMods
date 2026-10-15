package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.ProfessionStatusPacket;
import com.siirio.jemprofessions.network.ProfessionsNetwork;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import com.siirio.jemprofessions.tree.ProfessionNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One-screen profession atlas: player in the center, ten roots around it, selected branches outside. */
public final class ProfessionRadialScreen extends Screen {
    private final ProfessionStatusPacket status;
    private ProfessionId selected = ProfessionId.ARCHITECT;
    private final Map<ProfessionId, Box> roots = new LinkedHashMap<>();
    private final Map<ProfessionNode, Box> nodes = new LinkedHashMap<>();

    public ProfessionRadialScreen(ProfessionStatusPacket status) {
        super(Component.translatable("jem.professions.screen.title"));
        this.status = status;
    }

    @Override protected void init() { rebuildLayout(); }

    private void rebuildLayout() {
        roots.clear(); nodes.clear();
        int cx = width / 2, cy = Math.max(112, height / 2 - 12);
        double rx = Math.min(190, width * 0.27), ry = Math.min(92, height * 0.25);
        ProfessionId[] ids = ProfessionId.values();
        for (int i = 0; i < ids.length; i++) {
            double angle = -Math.PI / 2 + i * Math.PI * 2 / ids.length;
            roots.put(ids[i], new Box((int)(cx + Math.cos(angle) * rx) - 13, (int)(cy + Math.sin(angle) * ry) - 13, 26, 26));
        }
        List<String> branches = ProfessionCatalog.branches(selected);
        int branchGap = Math.max(78, Math.min(118, (width - 64) / Math.max(1, branches.size())));
        int total = branchGap * Math.max(0, branches.size() - 1);
        int startX = width / 2 - total / 2;
        int baseY = Math.min(height - 72, cy + 122);
        for (int b = 0; b < branches.size(); b++) {
            String branch = branches.get(b);
            List<ProfessionNode> branchNodes = ProfessionCatalog.nodes(selected).stream().filter(n -> n.branch().equals(branch)).toList();
            for (ProfessionNode node : branchNodes) {
                int y = baseY + (node.rank() - 1) * 42;
                if (y > height - 34) y = baseY - node.rank() * 42;
                nodes.put(node, new Box(startX + b * branchGap - 12, y - 12, 24, 24));
            }
        }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int cx = width / 2, cy = Math.max(112, height / 2 - 12);
        g.fill(12, 12, width - 12, height - 12, 0xE6151114);
        g.fill(14, 14, width - 14, 16, 0xFFB1763E);
        g.drawCenteredString(font, title, cx, 22, 0xFFFFE6BE);
        g.drawCenteredString(font, Component.translatable("jem.professions.one_screen_hint"), cx, 35, 0xFFBCA98D);

        // Links are drawn before nodes so the hierarchy stays legible.
        for (var entry : roots.entrySet()) line(g, cx, cy, entry.getValue().cx(), entry.getValue().cy(), entry.getKey() == selected ? 0xFFD7A24A : 0xFF675646);
        Box selectedRoot = roots.get(selected);
        for (ProfessionNode node : ProfessionCatalog.nodes(selected)) {
            Box to = nodes.get(node);
            Box from = node.parentId().isBlank() ? selectedRoot : nodes.get(ProfessionCatalog.node(selected, node.parentId()));
            if (from != null && to != null) line(g, from.cx(), from.cy(), to.cx(), to.cy(), unlocked(node) ? 0xFFE6B64E : 0xFF605346);
        }

        // Center identity.
        g.fill(cx - 22, cy - 22, cx + 22, cy + 22, 0xFF35271E);
        frame(g, cx - 22, cy - 22, 44, 44, 0xFFE6B64E);
        g.renderItem(new ItemStack(Items.PLAYER_HEAD), cx - 8, cy - 8);
        g.drawCenteredString(font, minecraft.player == null ? Component.literal("Player") : minecraft.player.getDisplayName(), cx, cy + 28, 0xFFFFE6BE);

        ProfessionId hoveredRoot = null;
        for (var entry : roots.entrySet()) {
            ProfessionId id = entry.getKey(); Box box = entry.getValue(); boolean active = id == selected;
            g.fill(box.x, box.y, box.x + box.w, box.y + box.h, active ? 0xFF5A3820 : 0xEE2B211B);
            frame(g, box.x, box.y, box.w, box.h, active ? 0xFFFFC45A : 0xFF805A3C);
            g.renderItem(new ItemStack(icon(id)), box.x + 5, box.y + 5);
            if (box.contains(mouseX, mouseY)) hoveredRoot = id;
        }

        ProfessionNode hoveredNode = null;
        for (var entry : nodes.entrySet()) {
            ProfessionNode node = entry.getKey(); Box box = entry.getValue();
            boolean unlocked = unlocked(node), ready = status.levels()[selected.ordinal()] >= node.level() && parentUnlocked(node);
            g.fill(box.x, box.y, box.x + box.w, box.y + box.h, unlocked ? 0xFF5B431E : ready ? 0xFF3C3024 : 0xFF211D1A);
            frame(g, box.x, box.y, box.w, box.h, unlocked ? 0xFFFFD15C : ready ? 0xFFA47A45 : 0xFF554B42);
            g.renderItem(new ItemStack(item(node.icon())), box.x + 4, box.y + 4);
            if (box.contains(mouseX, mouseY)) hoveredNode = node;
        }

        long xp = status.xp()[selected.ordinal()]; int level = status.levels()[selected.ordinal()], points = status.points()[selected.ordinal()];
        g.drawCenteredString(font, Component.translatable(name(selected)).append("  §6L" + level + "  §7XP " + xp + "  §e◆ " + points), cx, 51, 0xFFFFE6BE);
        super.render(g, mouseX, mouseY, partialTick);
        if (hoveredRoot != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(name(hoveredRoot)).withStyle(net.minecraft.ChatFormatting.GOLD));
            tip.add(Component.translatable("jem.professions.xp." + hoveredRoot.name().toLowerCase(java.util.Locale.ROOT)));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        } else if (hoveredNode != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("jem.professions.node." + hoveredNode.id()).withStyle(net.minecraft.ChatFormatting.GOLD));
            tip.add(Component.translatable("jem.professions.branch." + hoveredNode.branch()));
            tip.add(Component.translatable("jem.professions.unlock_level", hoveredNode.level()));
            if (unlocked(hoveredNode)) tip.add(Component.translatable("jem.professions.unlocked").withStyle(net.minecraft.ChatFormatting.GREEN));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (var entry : roots.entrySet()) if (entry.getValue().contains(mouseX, mouseY)) {
            selected = entry.getKey(); rebuildLayout(); return true;
        }
        for (var entry : nodes.entrySet()) if (entry.getValue().contains(mouseX, mouseY)) {
            ProfessionNode node = entry.getKey();
            if (!unlocked(node) && status.levels()[selected.ordinal()] >= node.level() && parentUnlocked(node)) ProfessionsNetwork.unlock(selected, node.id());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean unlocked(ProfessionNode node) { return status.unlocked().contains(selected.name() + ":" + node.id()); }
    private boolean parentUnlocked(ProfessionNode node) {
        return node.parentId().isBlank() || status.unlocked().contains(selected.name() + ":" + node.parentId());
    }
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color); g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color); g.fill(x + w - 1, y, x + w, y + h, color);
    }
    private static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) { int x = x1 + (x2 - x1) * i / Math.max(1, steps), y = y1 + (y2 - y1) * i / Math.max(1, steps); g.fill(x, y, x + 2, y + 2, color); }
    }
    private static Item item(String id) { return BuiltInRegistries.ITEM.getOptional(new ResourceLocation(id)).orElse(Items.BOOK); }
    private static Item icon(ProfessionId id) { return switch (id) {
        case ARCHITECT -> Items.BRICKS; case FARMER -> Items.WHEAT; case FISHER -> Items.FISHING_ROD; case MINER -> Items.IRON_PICKAXE;
        case EXPLORER -> Items.COMPASS; case ENGINEER -> item("create:wrench"); case WARRIOR -> Items.IRON_SWORD; case MERCHANT -> Items.EMERALD;
        case MAYOR -> Items.BELL; case MAGE -> item("irons_spellbooks:iron_spell_book");
    }; }
    private static String name(ProfessionId id) { return "jem.professions.name." + id.name().toLowerCase(java.util.Locale.ROOT); }
    private record Box(int x, int y, int w, int h) { int cx(){return x+w/2;} int cy(){return y+h/2;} boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;} }
}

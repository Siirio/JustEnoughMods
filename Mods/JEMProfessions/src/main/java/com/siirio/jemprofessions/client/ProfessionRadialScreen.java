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

/** One radial profession snowflake. Pufferfish remains the XP, point and unlock owner. */
public final class ProfessionRadialScreen extends Screen {
    private static final double ROOT_RADIUS = 78.0D;
    private static final double TIER_ONE_RADIUS = 132.0D;
    private static final double TIER_STEP = 58.0D;
    private final ProfessionStatusPacket status;
    private final Map<ProfessionId, Point> rootPoints = new LinkedHashMap<>();
    private final Map<NodeRef, Point> nodePoints = new LinkedHashMap<>();
    private ProfessionId selected = ProfessionId.ARCHITECT;
    private double zoom;
    private double panX;
    private double panY;
    private boolean dragging;

    public ProfessionRadialScreen(ProfessionStatusPacket status) {
        super(Component.translatable("jem.professions.screen.title"));
        this.status = status;
    }

    @Override protected void init() {
        buildGraph();
        if (zoom == 0.0D) {
            double fitX = (width - 48.0D) / 540.0D;
            double fitY = (height - 104.0D) / 540.0D;
            zoom = Math.max(0.56D, Math.min(1.0D, Math.min(fitX, fitY)));
        }
    }

    private void buildGraph() {
        rootPoints.clear();
        nodePoints.clear();
        ProfessionId[] ids = ProfessionId.values();
        for (int i = 0; i < ids.length; i++) {
            ProfessionId profession = ids[i];
            double axis = -Math.PI / 2.0D + i * Math.PI * 2.0D / ids.length;
            rootPoints.put(profession, polar(axis, ROOT_RADIUS));
            List<String> branches = ProfessionCatalog.branches(profession);
            for (int branchIndex = 0; branchIndex < branches.size(); branchIndex++) {
                String branch = branches.get(branchIndex);
                double spread = (branchIndex - (branches.size() - 1) / 2.0D) * 0.12D;
                double branchAngle = axis + spread;
                for (ProfessionNode node : ProfessionCatalog.nodes(profession)) {
                    if (!node.branch().equals(branch)) continue;
                    double radius = TIER_ONE_RADIUS + (node.rank() - 1) * TIER_STEP;
                    nodePoints.put(new NodeRef(profession, node), polar(branchAngle, radius));
                }
            }
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int centerX = width / 2 + (int)Math.round(panX);
        int centerY = (height + 58) / 2 + (int)Math.round(panY);
        graphics.fill(8, 8, width - 8, height - 8, 0xE9151211);

        Map<ProfessionId, Box> roots = projectRoots(centerX, centerY);
        Map<NodeRef, Box> nodes = projectNodes(centerX, centerY);
        for (var entry : roots.entrySet()) line(graphics, centerX, centerY, entry.getValue().cx(), entry.getValue().cy(),
                entry.getKey() == selected ? 0xFFE9B44E : 0xFF655746);
        for (var entry : nodes.entrySet()) {
            NodeRef ref = entry.getKey();
            Box from = ref.node.parentId().isBlank() ? roots.get(ref.profession)
                    : nodes.get(new NodeRef(ref.profession, ProfessionCatalog.node(ref.profession, ref.node.parentId())));
            if (from != null) line(graphics, from.cx(), from.cy(), entry.getValue().cx(), entry.getValue().cy(),
                    unlocked(ref) ? 0xFFE6B64E : ref.profession == selected ? 0xFF8A704F : 0xFF51483F);
        }

        graphics.fill(centerX - 21, centerY - 21, centerX + 21, centerY + 21, 0xFF35271E);
        frame(graphics, centerX - 21, centerY - 21, 42, 42, 0xFFE6B64E);
        graphics.renderItem(new ItemStack(Items.PLAYER_HEAD), centerX - 8, centerY - 8);

        ProfessionId hoveredRoot = null;
        for (var entry : roots.entrySet()) {
            ProfessionId profession = entry.getKey(); Box box = entry.getValue(); boolean active = profession == selected;
            graphics.fill(box.x, box.y, box.x + box.w, box.y + box.h, active ? 0xFF624021 : 0xF02A211C);
            frame(graphics, box.x, box.y, box.w, box.h, active ? 0xFFFFC45A : 0xFF805A3C);
            graphics.renderItem(new ItemStack(icon(profession)), box.x + 4, box.y + 4);
            if (box.contains(mouseX, mouseY)) hoveredRoot = profession;
        }

        NodeRef hoveredNode = null;
        for (var entry : nodes.entrySet()) {
            NodeRef ref = entry.getKey(); Box box = entry.getValue();
            boolean unlocked = unlocked(ref);
            boolean ready = status.levels()[ref.profession.ordinal()] >= ref.node.level() && parentUnlocked(ref);
            int fill = unlocked ? 0xFF5B431E : ready ? 0xFF3C3024 : 0xFF211D1A;
            int border = unlocked ? 0xFFFFD15C : ready ? 0xFFA47A45 : 0xFF554B42;
            if (ref.profession != selected) { fill = dim(fill); border = dim(border); }
            graphics.fill(box.x, box.y, box.x + box.w, box.y + box.h, fill);
            frame(graphics, box.x, box.y, box.w, box.h, border);
            graphics.renderItem(new ItemStack(item(ref.node.icon())), box.x + 3, box.y + 3);
            if (box.contains(mouseX, mouseY)) hoveredNode = ref;
        }

        graphics.fill(8, 8, width - 8, 59, 0xF11F1815);
        graphics.fill(10, 57, width - 10, 59, 0xFFB1763E);
        graphics.drawCenteredString(font, title, width / 2, 17, 0xFFFFE6BE);
        long xp = status.xp()[selected.ordinal()];
        int level = status.levels()[selected.ordinal()], points = status.points()[selected.ordinal()];
        graphics.drawCenteredString(font, Component.translatable(name(selected)).append("  §6L" + level + "  §7XP " + xp + "  §e◆ " + points), width / 2, 32, 0xFFFFE6BE);
        graphics.drawCenteredString(font, Component.translatable("jem.professions.radial_controls"), width / 2, 45, 0xFFAD9B83);

        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredRoot != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(name(hoveredRoot)).withStyle(net.minecraft.ChatFormatting.GOLD));
            tip.add(Component.translatable("jem.professions.xp." + hoveredRoot.name().toLowerCase(java.util.Locale.ROOT)));
            graphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        } else if (hoveredNode != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("jem.professions.node." + hoveredNode.node.id()).withStyle(net.minecraft.ChatFormatting.GOLD));
            tip.add(Component.translatable(name(hoveredNode.profession)));
            tip.add(Component.translatable("jem.professions.branch." + hoveredNode.node.branch()));
            tip.add(Component.translatable("jem.professions.unlock_level", hoveredNode.node.level()));
            if (unlocked(hoveredNode)) tip.add(Component.translatable("jem.professions.unlocked").withStyle(net.minecraft.ChatFormatting.GREEN));
            graphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2 + (int)Math.round(panX), centerY = (height + 58) / 2 + (int)Math.round(panY);
        for (var entry : projectRoots(centerX, centerY).entrySet()) if (entry.getValue().contains(mouseX, mouseY)) {
            selected = entry.getKey(); return true;
        }
        for (var entry : projectNodes(centerX, centerY).entrySet()) if (entry.getValue().contains(mouseX, mouseY)) {
            NodeRef ref = entry.getKey(); selected = ref.profession;
            if (!unlocked(ref) && status.levels()[ref.profession.ordinal()] >= ref.node.level() && parentUnlocked(ref))
                ProfessionsNetwork.unlock(ref.profession, ref.node.id());
            return true;
        }
        if (button == 0 || button == 2) { dragging = true; return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && (button == 0 || button == 2)) { panX += dragX; panY += dragY; return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0.0D) return false;
        double old = zoom;
        zoom = Math.max(0.50D, Math.min(1.45D, zoom * (delta > 0 ? 1.12D : 0.89D)));
        double fixedX = width / 2.0D + panX, fixedY = (height + 58) / 2.0D + panY;
        panX += (mouseX - fixedX) * (1.0D - zoom / old);
        panY += (mouseY - fixedY) * (1.0D - zoom / old);
        return true;
    }

    private Map<ProfessionId, Box> projectRoots(int centerX, int centerY) {
        Map<ProfessionId, Box> result = new LinkedHashMap<>();
        rootPoints.forEach((profession, point) -> result.put(profession, box(point, centerX, centerY, 24)));
        return result;
    }
    private Map<NodeRef, Box> projectNodes(int centerX, int centerY) {
        Map<NodeRef, Box> result = new LinkedHashMap<>();
        nodePoints.forEach((ref, point) -> result.put(ref, box(point, centerX, centerY, 22)));
        return result;
    }
    private Box box(Point point, int centerX, int centerY, int size) {
        return new Box((int)Math.round(centerX + point.x * zoom) - size / 2, (int)Math.round(centerY + point.y * zoom) - size / 2, size, size);
    }
    private boolean unlocked(NodeRef ref) { return status.unlocked().contains(ref.profession.name() + ":" + ref.node.id()); }
    private boolean parentUnlocked(NodeRef ref) { return ref.node.parentId().isBlank() || status.unlocked().contains(ref.profession.name() + ":" + ref.node.parentId()); }
    private static Point polar(double angle, double radius) { return new Point(Math.cos(angle) * radius, Math.sin(angle) * radius); }
    private static int dim(int color) {
        int a = color >>> 24, r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        return a << 24 | r * 2 / 3 << 16 | g * 2 / 3 << 8 | b * 2 / 3;
    }
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color); g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color); g.fill(x + w - 1, y, x + w, y + h, color);
    }
    private static void line(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps), y = y1 + (y2 - y1) * i / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }
    private static Item item(String id) { return BuiltInRegistries.ITEM.getOptional(new ResourceLocation(id)).orElse(Items.BOOK); }
    private static Item icon(ProfessionId id) { return switch (id) {
        case ARCHITECT -> Items.BRICKS; case FARMER -> Items.WHEAT; case FISHER -> Items.FISHING_ROD; case MINER -> Items.IRON_PICKAXE;
        case EXPLORER -> Items.COMPASS; case ENGINEER -> item("create:wrench"); case WARRIOR -> Items.IRON_SWORD; case MERCHANT -> Items.EMERALD;
        case MAYOR -> Items.BELL; case MAGE -> item("irons_spellbooks:iron_spell_book");
    }; }
    private static String name(ProfessionId id) { return "jem.professions.name." + id.name().toLowerCase(java.util.Locale.ROOT); }
    private record Point(double x, double y) {}
    private record NodeRef(ProfessionId profession, ProfessionNode node) {}
    private record Box(int x, int y, int w, int h) {
        int cx(){return x+w/2;} int cy(){return y+h/2;} boolean contains(double px,double py){return px>=x&&px<x+w&&py>=y&&py<y+h;}
    }
}

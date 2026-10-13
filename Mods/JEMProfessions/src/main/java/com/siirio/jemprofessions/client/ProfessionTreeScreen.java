package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.ProfessionStatusPacket;
import com.siirio.jemprofessions.network.ProfessionsNetwork;
import com.siirio.jemprofessions.progress.ProfessionFormula;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import com.siirio.jemprofessions.tree.ProfessionNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

public final class ProfessionTreeScreen extends Screen {
    private static final int CARD_W = 116, CARD_H = 58, GAP_X = 10, GAP_Y = 16;
    private final ProfessionStatusPacket status;
    private final ProfessionId profession;
    private final Map<ProfessionNode, Button> toggles = new LinkedHashMap<>();
    private final Map<ProfessionNode, Point> positions = new LinkedHashMap<>();
    public ProfessionTreeScreen(ProfessionStatusPacket status, ProfessionId profession) { super(Component.translatable(ProfessionsScreen.nameKey(profession))); this.status = status; this.profession = profession; }
    public ProfessionId profession() { return profession; }

    @Override protected void init() {
        List<String> branches = ProfessionCatalog.nodes(profession).stream().map(ProfessionNode::branch).distinct().toList();
        int total = branches.size() * CARD_W + Math.max(0, branches.size() - 1) * GAP_X;
        int startX = (width - total) / 2, startY = 58;
        int level = ProfessionFormula.levelFor(status.xp()[profession.ordinal()]);
        for (ProfessionNode node : ProfessionCatalog.nodes(profession)) {
            int x = startX + branches.indexOf(node.branch()) * (CARD_W + GAP_X), y = startY + (node.rank() - 1) * (CARD_H + GAP_Y);
            positions.put(node, new Point(x, y));
            if (!node.toggleable() || !node.available()) continue;
            boolean enabled = status.enabled().contains(profession.name() + ":" + node.id());
            Button button = Button.builder(Component.translatable(enabled ? "jem.professions.enabled" : "jem.professions.disabled"), value -> ProfessionsNetwork.toggle(profession, node.id()))
                    .bounds(x + CARD_W - 52, y + CARD_H - 18, 48, 15).build();
            button.active = status.authenticated() && level >= node.level() && ProfessionCatalog.parentSatisfied(profession, node, level) && activeParents(node) && dependencyPresent(node);
            addRenderableWidget(button); toggles.put(node, button);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), value -> minecraft.setScreen(new ProfessionsScreen(status))).bounds(width / 2 - 40, height - 25, 80, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); int level = ProfessionFormula.levelFor(status.xp()[profession.ordinal()]);
        graphics.drawCenteredString(font, title.copy().append("  L" + level + "/50"), width / 2, 18, 0xFFFFE5C0);
        graphics.drawCenteredString(font, Component.translatable("jem.professions.tree.earned_progress"), width / 2, 34, 0xFFBAA78F);
        for (ProfessionNode node : ProfessionCatalog.nodes(profession)) if (!node.parentId().isBlank()) {
            ProfessionNode parent = ProfessionCatalog.node(profession, node.parentId()); Point from = positions.get(parent), to = positions.get(node);
            if (from != null && to != null) graphics.fill(from.x + CARD_W / 2 - 1, from.y + CARD_H, to.x + CARD_W / 2 + 1, to.y, 0xFFC17B3F);
        }
        ProfessionNode hovered = null;
        for (ProfessionNode node : ProfessionCatalog.nodes(profession)) {
            Point point = positions.get(node); boolean dependency = dependencyPresent(node); boolean parent = ProfessionCatalog.parentSatisfied(profession, node, level) && activeParents(node); boolean unlocked = level >= node.level() && parent && dependency; boolean available = node.available();
            int background = available ? unlocked ? 0xE0352A21 : 0xE0201C19 : 0xE0252422;
            graphics.fill(point.x, point.y, point.x + CARD_W, point.y + CARD_H, background);
            graphics.fill(point.x, point.y, point.x + 3, point.y + CARD_H, available && unlocked ? 0xFFC17B3F : 0xFF66584B);
            graphics.drawString(font, Component.translatable("jem.professions.branch." + node.branch()), point.x + 7, point.y + 5, 0xFFBAA78F, false);
            graphics.drawString(font, Component.translatable("jem.professions.node." + node.id()), point.x + 7, point.y + 17, available ? unlocked ? 0xFFFFE2BD : 0xFF8F8375 : 0xFFAA7D73, false);
            graphics.drawString(font, Component.translatable("jem.professions.rank_level", node.rank(), node.level()), point.x + 7, point.y + 30, 0xFFC5B399, false);
            Component state = !available ? Component.translatable("jem.professions.unavailable." + node.availability().name().toLowerCase(java.util.Locale.ROOT))
                    : !dependency ? Component.translatable("jem.professions.requires", node.dependency())
                    : !parent ? Component.translatable("jem.professions.requires_parent", Component.translatable("jem.professions.node." + node.parentId()))
                    : !unlocked ? Component.translatable("jem.professions.unlocks", node.level())
                    : node.toggleable() ? Component.translatable("jem.professions.activation.toggle") : Component.translatable("jem.professions.always_on");
            graphics.drawString(font, state, point.x + 7, point.y + 43, 0xFFBAA78F, false);
            if (mouseX >= point.x && mouseX < point.x + CARD_W && mouseY >= point.y && mouseY < point.y + CARD_H) hovered = node;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) graphics.renderTooltip(font, Component.translatable(hovered.effectKey()), mouseX, mouseY);
    }
    private static boolean dependencyPresent(ProfessionNode node) { return node.dependency().isBlank() || ModList.get().isLoaded(node.dependency()); }
    private boolean activeParents(ProfessionNode node) { if (node.parentId().isBlank()) return true; ProfessionNode parent = ProfessionCatalog.node(profession, node.parentId()); return parent != null && (!parent.available() || !parent.toggleable() || status.enabled().contains(profession.name() + ":" + parent.id())) && activeParents(parent); }
    private record Point(int x, int y) { }
}

package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.network.ProfessionStatusPacket;
import com.siirio.jemprofessions.network.ProfessionsNetwork;
import com.siirio.jemprofessions.progress.ProfessionFormula;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import com.siirio.jemprofessions.tree.ProfessionNode;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

public final class ProfessionTreeScreen extends Screen {
    private final ProfessionStatusPacket status;
    private final ProfessionId profession;
    private final Map<ProfessionNode, Button> toggles = new LinkedHashMap<>();
    public ProfessionTreeScreen(ProfessionStatusPacket status, ProfessionId profession) {
        super(Component.translatable(ProfessionsScreen.nameKey(profession))); this.status = status; this.profession = profession;
    }
    public ProfessionId profession() { return profession; }

    @Override protected void init() {
        int x = width / 2 - 190, y = 52;
        int row = 0;
        int level = ProfessionFormula.levelFor(status.xp()[profession.ordinal()]);
        for (ProfessionNode node : ProfessionCatalog.nodes(profession)) {
            if (!node.toggleable()) { row++; continue; }
            boolean enabled = status.enabled().contains(profession.name() + ":" + node.id());
            Button button = Button.builder(Component.translatable(enabled ? "jem.professions.enabled" : "jem.professions.disabled"), value ->
                    ProfessionsNetwork.toggle(profession, node.id())).bounds(x + 292, y + row * 30 + 4, 78, 20).build();
            button.active = status.authenticated() && level >= node.level() && dependencyPresent(node);
            addRenderableWidget(button); toggles.put(node, button); row++;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), value -> minecraft.setScreen(new ProfessionsScreen(status)))
                .bounds(width / 2 - 40, Math.min(height - 26, y + row * 30 + 8), 80, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 190, y = 52;
        int level = ProfessionFormula.levelFor(status.xp()[profession.ordinal()]);
        graphics.drawCenteredString(font, title.copy().append("  L" + level + "/50"), width / 2, 25, 0xFFFFE5C0);
        int row = 0;
        for (ProfessionNode node : ProfessionCatalog.nodes(profession)) {
            int rowY = y + row * 30;
            boolean unlocked = level >= node.level(); boolean dependency = dependencyPresent(node);
            int color = unlocked && dependency ? 0xE0352A21 : 0xE0201C19;
            graphics.fill(x, rowY, x + 380, rowY + 26, color);
            graphics.fill(x, rowY, x + 3, rowY + 26, unlocked && dependency ? 0xFFC17B3F : 0xFF66584B);
            graphics.drawString(font, Component.translatable("jem.professions.node." + node.id()), x + 9, rowY + 4, unlocked ? 0xFFFFE2BD : 0xFF8F8375, false);
            Component state = !dependency ? Component.translatable("jem.professions.requires", node.dependency())
                    : !unlocked ? Component.translatable("jem.professions.unlocks", node.level())
                    : node.toggleable() ? Component.translatable("jem.professions.activation.toggle") : Component.translatable("jem.professions.always_on");
            graphics.drawString(font, state, x + 9, rowY + 15, 0xFFBAA78F, false);
            row++;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    private static boolean dependencyPresent(ProfessionNode node) { return node.dependency().isBlank() || ModList.get().isLoaded(node.dependency()); }
}

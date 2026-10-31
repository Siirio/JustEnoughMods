package com.justenoughmods.achievementguide.compat.ftb;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import java.util.Comparator;
import java.util.List;
import net.mehvahdjukaar.advframes.blocks.AdvancementFrameBlockTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FtbQuestFrameScreen extends Screen {
    private final AdvancementFrameBlockTile tile;
    private List<Quest> quests = List.of();
    private int page;

    public FtbQuestFrameScreen(AdvancementFrameBlockTile tile) {
        super(Component.translatable("jem.quest_frame.title"));
        this.tile = tile;
    }

    @Override protected void init() {
        clearWidgets();
        var file = ClientQuestFile.INSTANCE;
        if (file != null && file.selfTeamData != null) {
            quests = file.collect(Quest.class).stream().filter(file.selfTeamData::isCompleted)
                    .sorted(Comparator.comparing(quest -> quest.getTitle().getString(), String.CASE_INSENSITIVE_ORDER)).toList();
        }
        int start = page * 10;
        for (int index = start; index < Math.min(start + 10, quests.size()); index++) {
            Quest quest = quests.get(index);
            int y = 36 + (index - start) * 20;
            addRenderableWidget(Button.builder(quest.getTitle(), button -> {
                FtbQuestFrameNetwork.select(tile.getBlockPos(), quest.getId());
                Minecraft.getInstance().setScreen(null);
            }).bounds(width / 2 - 150, y, 300, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button -> { if (page > 0) { page--; rebuildWidgets(); } })
                .bounds(width / 2 - 70, height - 28, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> { if ((page + 1) * 10 < quests.size()) { page++; rebuildWidgets(); } })
                .bounds(width / 2 + 30, height - 28, 40, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        if (quests.isEmpty()) graphics.drawCenteredString(font, Component.translatable("jem.quest_frame.none"), width / 2, 55, 0xAAAAAA);
    }
}

package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.questbook.QuestbookConnections;
import com.justenoughmods.achievementguide.client.questbook.QuestbookLayout;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FTB draws image widgets before this phase and native quest widgets after it. */
@Mixin(value = QuestPanel.class, remap = false)
public abstract class QuestbookConnectionsMixin {
    @Shadow @Final private QuestScreen questScreen;
    @Shadow private double questMinX;
    @Shadow private double questMinY;

    @Inject(method = "drawOffsetBackground", at = @At("TAIL"), remap = false)
    private void jem$cosmeticConnections(GuiGraphics graphics, Theme theme, int x, int y, int width, int height,
                                        CallbackInfo callback) {
        Chapter chapter = ((QuestScreenChapterAccess)questScreen).jem$selectedChapter();
        if (chapter == null || !chapter.hasTag("jem_visual_v4")) return;
        var board = QuestbookLayout.board(chapter.id);
        if (board == null) return;
        QuestbookConnections.draw(graphics, board, x, y, questMinX, questMinY,
                questScreen.getQuestButtonSize() + questScreen.getQuestButtonSpacing());
    }
}

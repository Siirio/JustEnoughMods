package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = QuestScreen.class, remap = false)
public interface QuestScreenChapterAccess {
    @Accessor("selectedChapter") Chapter jem$selectedChapter();
}

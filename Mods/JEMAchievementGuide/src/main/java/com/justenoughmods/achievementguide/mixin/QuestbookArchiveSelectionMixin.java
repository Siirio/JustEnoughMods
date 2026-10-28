package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.questbook.QuestbookArchives;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestbookArchiveSelectionMixin {
    @Shadow @Final private List<Movable> selectedObjects;
    @ModifyVariable(method = "selectChapter", at = @At("HEAD"), argsOnly = true, remap = false)
    private Chapter jem$restoreActiveChapter(Chapter chapter) {
        return QuestbookArchives.archived(chapter) ? QuestbookArchives.firstActive(chapter.file) : chapter;
    }
    @Inject(method = "viewQuest", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$noArchivedQuestView(Quest quest, CallbackInfo callback) {
        if (QuestbookArchives.archived(quest)) callback.cancel();
    }
    @Inject(method = "open", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$noArchivedDeepLink(QuestObject object, boolean focus, CallbackInfo callback) {
        if (QuestbookArchives.archived(object)) callback.cancel();
    }
    @Inject(method = "restorePersistedScreenData", at = @At("TAIL"), remap = false)
    private void jem$noArchivedPersistedSelection(BaseQuestFile file, QuestScreen.PersistedData data, CallbackInfo callback) {
        if (QuestbookArchives.visualV4(file)) selectedObjects.removeIf(object -> object instanceof QuestObjectBase quest && QuestbookArchives.archived(quest));
    }
}

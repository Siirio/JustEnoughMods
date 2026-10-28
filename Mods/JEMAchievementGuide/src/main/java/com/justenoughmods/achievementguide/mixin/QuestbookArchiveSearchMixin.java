package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.questbook.QuestbookArchives;
import dev.ftb.mods.ftbquests.client.gui.SelectQuestObjectScreen;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SelectQuestObjectScreen.class, remap = false)
public abstract class QuestbookArchiveSearchMixin {
    // In2001.4.22 edit mode bypasses isSearchable after this first predicate call.
    @Redirect(method = "addButtons", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z", ordinal = 0), remap = false)
    private boolean jem$excludeArchivedBeforeIcon(Predicate<QuestObjectBase> predicate, Object candidate) {
        QuestObjectBase object = (QuestObjectBase)candidate;
        return !QuestbookArchives.archived(object) && predicate.test(object);
    }
}

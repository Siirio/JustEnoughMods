package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.TeamData;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Set only the initial client presentation, leaving native user toggles and progress intact. */
@Mixin(value = ChapterGroup.class, remap = false)
public abstract class FtbChapterGroupDefaultsMixin {
    @Shadow private boolean guiCollapsed;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void jem$collapsedByDefault(long id, BaseQuestFile file, CallbackInfo callback) {
        if (file instanceof ClientQuestFile && !((ChapterGroup) (Object) this).isDefaultGroup()) guiCollapsed = true;
    }

    @Inject(method = "getVisibleChapters", at = @At("RETURN"), cancellable = true, remap = false)
    private void jem$sevenChapterSidebar(TeamData team, CallbackInfoReturnable<List<Chapter>> callback) {
        var chapters = callback.getReturnValue();
        // Native edit mode deliberately exposes always_invisible archives. The
        // marked book keeps its seven-entry navigation even for its author.
        if (chapters.stream().anyMatch(chapter -> chapter.file instanceof ClientQuestFile && chapter.hasTag("jem_rescue_v3"))) {
            callback.setReturnValue(chapters.stream().filter(chapter -> chapter.hasTag("jem_rescue_v3")).toList());
        }
    }

    @Inject(method = "getFirstVisibleChapter", at = @At("RETURN"), cancellable = true, remap = false)
    private void jem$usefulFirstChapter(TeamData team, CallbackInfoReturnable<Chapter> callback) {
        var chapters = ((ChapterGroup)(Object)this).getChapters();
        chapters.stream().filter(chapter -> chapter.file instanceof ClientQuestFile && chapter.hasTag("jem_rescue_v3"))
                .findFirst().ifPresent(callback::setReturnValue);
    }
}

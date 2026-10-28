package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.TeamData;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Set only the initial client presentation, leaving native user toggles and progress intact. */
@Mixin(value = ChapterGroup.class, remap = false)
public abstract class FtbChapterGroupDefaultsMixin {
    @Shadow private boolean guiCollapsed;

    @Unique private static boolean jem$activeChapter(Chapter chapter) {
        String filename = chapter.getFilename();
        return !filename.startsWith("_archive_") && !filename.equals("_jem_frozen_claims") && !filename.equals("_jem_unavailable")
                && !chapter.hasTag("jem_archive") && !chapter.hasTag("jem_claims_archive")
                && (chapter.hasTag("jem_rescue_v3") || chapter.hasTag("jem_visual_v4"));
    }

    @Unique private boolean jem$markedBook() {
        BaseQuestFile file = ((ChapterGroup)(Object)this).getFile();
        return file instanceof ClientQuestFile && file.getAllChapters().stream().anyMatch(FtbChapterGroupDefaultsMixin::jem$activeChapter);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void jem$collapsedByDefault(long id, BaseQuestFile file, CallbackInfo callback) {
        if (file instanceof ClientQuestFile && !((ChapterGroup) (Object) this).isDefaultGroup()) guiCollapsed = true;
    }

    @Inject(method = "getVisibleChapters", at = @At("RETURN"), cancellable = true, remap = false)
    private void jem$sevenChapterSidebar(TeamData team, CallbackInfoReturnable<List<Chapter>> callback) {
        var chapters = callback.getReturnValue();
        // Native edit mode deliberately exposes always_invisible archives. The
        // marked book keeps its seven-entry navigation even for its author.
        if (jem$markedBook()) callback.setReturnValue(chapters.stream().filter(FtbChapterGroupDefaultsMixin::jem$activeChapter).toList());
    }

    @Inject(method = "getFirstVisibleChapter", at = @At("RETURN"), cancellable = true, remap = false)
    private void jem$usefulFirstChapter(TeamData team, CallbackInfoReturnable<Chapter> callback) {
        if (jem$markedBook()) callback.setReturnValue(((ChapterGroup)(Object)this).getVisibleChapters(team).stream().findFirst().orElse(null));
    }

    @Inject(method = "isVisible", at = @At("RETURN"), cancellable = true, remap = false)
    private void jem$hideArchiveOnlyGroup(TeamData team, CallbackInfoReturnable<Boolean> callback) {
        if (jem$markedBook() && ((ChapterGroup)(Object)this).getVisibleChapters(team).isEmpty()) callback.setReturnValue(false);
    }
}

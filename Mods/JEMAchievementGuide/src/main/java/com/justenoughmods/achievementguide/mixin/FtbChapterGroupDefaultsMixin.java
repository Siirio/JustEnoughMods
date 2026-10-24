package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.ChapterGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Set only the initial client presentation, leaving native user toggles and progress intact. */
@Mixin(value = ChapterGroup.class, remap = false)
public abstract class FtbChapterGroupDefaultsMixin {
    @Shadow private boolean guiCollapsed;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void jem$collapsedByDefault(long id, BaseQuestFile file, CallbackInfo callback) {
        if (file instanceof ClientQuestFile && !((ChapterGroup) (Object) this).isDefaultGroup()) guiCollapsed = true;
    }
}

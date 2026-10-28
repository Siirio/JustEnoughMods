package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.client.gui.quests.ChapterImageButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPositionableButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.ChapterImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Image dimensions are authored in the same grid units as native quest centers. */
@Mixin(value = ChapterImageButton.class, remap = false)
public abstract class QuestbookImageGeometryMixin {
    @Shadow @Final private QuestScreen questScreen;
    @Shadow @Final private ChapterImage chapterImage;

    @Inject(method = "getPosition", at = @At("RETURN"), cancellable = true, remap = false)
    private void jem$gridImageExtents(CallbackInfoReturnable<QuestPositionableButton.Position> callback) {
        if (!chapterImage.getChapter().hasTag("jem_visual_v4")) return;
        double size = questScreen.getQuestButtonSize();
        double ratio = (size + questScreen.getQuestButtonSpacing()) / size;
        var position = callback.getReturnValue();
        callback.setReturnValue(new QuestPositionableButton.Position(position.x(), position.y(),
                position.w() * ratio, position.h() * ratio));
    }
}

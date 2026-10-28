package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The same native camera coordinates read by QuestScreen.PersistedData. */
@Mixin(value = QuestPanel.class, remap = false)
public interface QuestPanelCameraAccess {
    @Accessor("questMinX") double jem$getMinX();
    @Accessor("questMinY") double jem$getMinY();
    @Accessor("centerQuestX") double jem$getCenterX();
    @Accessor("centerQuestY") double jem$getCenterY();
    @Accessor("centerQuestX") void jem$setCenterX(double value);
    @Accessor("centerQuestY") void jem$setCenterY(double value);
}

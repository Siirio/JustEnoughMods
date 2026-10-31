package com.justenoughmods.achievementguide.compat.ftb;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.level.ServerPlayer;

public interface FtbQuestFrameAccess {
    void jem$setFtbQuest(long questId, DisplayInfo display, ServerPlayer owner);
    long jem$getFtbQuestId();
}

package com.justenoughmods.achievementguide.integration;

import net.minecraft.server.level.ServerPlayer;

public interface JukeboxQuestReceipt {
    void jem$beginPlayback(ServerPlayer actor);
    void jem$finishPlayback();
}

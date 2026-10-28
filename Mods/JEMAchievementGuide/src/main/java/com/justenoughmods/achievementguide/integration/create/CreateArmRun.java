package com.justenoughmods.achievementguide.integration.create;

import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

public interface CreateArmRun {
    void jem$configured(ServerPlayer actor, ListTag committedPoints);
}

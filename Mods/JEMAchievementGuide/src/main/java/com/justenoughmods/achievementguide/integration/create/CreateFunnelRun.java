package com.justenoughmods.achievementguide.integration.create;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public interface CreateFunnelRun {
    void jem$filterConfiguration(ServerPlayer actor, CompoundTag configuration);
    void jem$beltConfiguration(ServerPlayer actor);
}

package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Ephemeral receipt belongs to the native jug between its effect and clear calls. */
public interface JugFoodRun {
    void jem$applyForQuest(LivingEntity actor, int duration);
    ServerPlayer jem$consumeDrinker();
}

package com.justenoughmods.achievementguide.integration.wildlife;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;

public record WildlifeQuestBreed(ServerPlayer actor, AgeableMob child) {}

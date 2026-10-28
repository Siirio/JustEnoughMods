package com.justenoughmods.achievementguide.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/** Transient evidence for the same native death call, never a gameplay authority. */
public interface CanonicalBossDeathProof {
    void jem$approvedNativeVictory(LivingDeathEvent event, ServerPlayer killer);
}

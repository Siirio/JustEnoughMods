package com.justenoughmods.achievementguide.integration.create;

import java.util.UUID;

/** Synchronous evidence from BasinRecipe's real acceptOutputs commit. */
public interface CreateMixingRun {
    void jem$beginMixing(UUID actor);
    void jem$committedBrass();
    void jem$finishMixing(boolean success);
    void jem$beginCompacting(UUID actor);
}

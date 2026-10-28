package com.justenoughmods.achievementguide.integration;

import java.util.UUID;

/** Quest provenance lives only as long as the actual native transient orbit object */
public interface CosmicQuestOrigin {
    UUID jem$questCosmicActor();
    void jem$questCosmicActor(UUID actor);
}

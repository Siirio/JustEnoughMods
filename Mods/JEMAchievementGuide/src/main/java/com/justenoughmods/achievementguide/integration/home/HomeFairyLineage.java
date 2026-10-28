package com.justenoughmods.achievementguide.integration.home;

import java.util.UUID;

/** Technical creator lineage only, not completion progress or native ItemStack data. */
public interface HomeFairyLineage {
    UUID jem$homeCreator();
    void jem$homeCreator(UUID player);
}

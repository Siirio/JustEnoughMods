package com.justenoughmods.achievementguide.integration.worldmisc;

import java.util.Set;
import java.util.UUID;

/** Actual contributions last only for the native CombatTracker encounter. */
public interface WorldEagleContributors {
    Set<UUID> jem$eagleContributors();
}

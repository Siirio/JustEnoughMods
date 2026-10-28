package com.justenoughmods.achievementguide.integration.home;

/** Read-only view of the installed chimney's own state queries. */
public interface HomeChimneyState {
    boolean isDirty();
    boolean isClean();
}

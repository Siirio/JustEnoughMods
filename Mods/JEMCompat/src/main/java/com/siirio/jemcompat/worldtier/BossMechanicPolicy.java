package com.siirio.jemcompat.worldtier;

public final class BossMechanicPolicy {
    private BossMechanicPolicy() {
    }

    public static float scaledAbsoluteThreshold(float nativeThreshold, float nativeMaximumHealth, float currentMaximumHealth) {
        if (nativeMaximumHealth <= 0.0f || currentMaximumHealth <= 0.0f) {
            return nativeThreshold;
        }
        return nativeThreshold * currentMaximumHealth / nativeMaximumHealth;
    }
}

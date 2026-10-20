package com.siirio.jemworldbosstiers.compat.irons;

/** The native lifesteal share applies to the accepted net target resource loss. */
public final class SiphonHealing {
    private SiphonHealing() { }

    public static float acceptedHealing(boolean accepted, float before, float after, float nativeShare) {
        return accepted ? Math.max(0.0F, before - after) * nativeShare : 0.0F;
    }
}

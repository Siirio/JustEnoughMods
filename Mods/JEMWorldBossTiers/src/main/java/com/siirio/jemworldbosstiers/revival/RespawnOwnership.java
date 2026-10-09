package com.siirio.jemworldbosstiers.revival;

public final class RespawnOwnership {
    private static final String ARENA_OFFERING = "arena_offering";
    private static final String NONE = "none";

    private RespawnOwnership() {
    }

    public static boolean usesJemScheduler(String revivalStrategy) {
        return ARENA_OFFERING.equals(revivalStrategy) || NONE.equals(revivalStrategy);
    }
}

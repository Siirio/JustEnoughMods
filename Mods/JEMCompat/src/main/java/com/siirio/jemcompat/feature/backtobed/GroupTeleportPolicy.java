package com.siirio.jemcompat.feature.backtobed;

public final class GroupTeleportPolicy {
    public static final double HORIZONTAL_RADIUS = 1.75;
    public static final double VERTICAL_TOLERANCE = 1.0;
    private static final long TICKS_PER_DAY = 24_000L;

    private GroupTeleportPolicy() {
    }

    public static long day(long overworldDayTime) {
        return Math.floorDiv(overworldDayTime, TICKS_PER_DAY);
    }

    public static boolean canActivate(long currentDay, boolean hasLastUse, long lastUseDay) {
        return !hasLastUse || currentDay != lastUseDay;
    }

    public static boolean isNearby(double hostX, double hostY, double hostZ,
                                   double playerX, double playerY, double playerZ) {
        return Math.abs(playerX - hostX) <= HORIZONTAL_RADIUS
                && Math.abs(playerY - hostY) <= VERTICAL_TOLERANCE
                && Math.abs(playerZ - hostZ) <= HORIZONTAL_RADIUS;
    }
}

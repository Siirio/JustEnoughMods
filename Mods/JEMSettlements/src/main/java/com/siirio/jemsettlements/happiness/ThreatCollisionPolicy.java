package com.siirio.jemsettlements.happiness;

/** Collision shapes may protrude beyond their owning block (notably fences and walls). */
public final class ThreatCollisionPolicy {
    public static int firstOwner(double bodyMinimum) { return (int) Math.floor(bodyMinimum) - 1; }
    public static int lastOwner(double bodyMaximum) { return (int) Math.floor(bodyMaximum) + 1; }
    public static boolean overlaps(double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ,
                                   double bMinX, double bMinY, double bMinZ, double bMaxX, double bMaxY, double bMaxZ) {
        return aMinX < bMaxX && aMaxX > bMinX && aMinY < bMaxY && aMaxY > bMinY && aMinZ < bMaxZ && aMaxZ > bMinZ;
    }
    public static boolean supported(double surface, double feet) { return Math.abs(surface - feet) <= .001; }
    public static boolean canStep(double from, double to, double nativeMaximumStep) { return to - from <= Math.max(0, nativeMaximumStep) + .001; }
    private ThreatCollisionPolicy() { }
}

package com.siirio.jemsettlements.happiness;

/** The caller supplies the canonical registered census, including residents whose native entity is unloaded. */
public final class CrowdingPolicy {
    public static int penalty(int area, int registeredResidents, int floorPerResident, int maximum, boolean known, int prior) {
        if (!known) return prior;
        double ratio = Math.min(1D, Math.max(0, area) / (Math.max(1, registeredResidents) * (double) Math.max(1, floorPerResident)));
        return (int) Math.round(Math.max(0, maximum) * (1D - ratio));
    }
    private CrowdingPolicy() { }
}

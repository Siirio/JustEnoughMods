package com.siirio.jemsettlements.town;

/** Exact inclusive block-area quote shared by all expansion interfaces. */
public record TerritoryQuote(int minX, int minZ, int maxX, int maxZ, long addedCopper) {
    public static TerritoryQuote union(int oldMinX, int oldMinZ, int oldMaxX, int oldMaxZ,
                                       int proposedMinX, int proposedMinZ, int proposedMaxX, int proposedMaxZ) {
        int minX = Math.min(oldMinX, proposedMinX), minZ = Math.min(oldMinZ, proposedMinZ);
        int maxX = Math.max(oldMaxX, proposedMaxX), maxZ = Math.max(oldMaxZ, proposedMaxZ);
        return new TerritoryQuote(minX, minZ, maxX, maxZ,
                Math.subtractExact(area(minX, minZ, maxX, maxZ), area(oldMinX, oldMinZ, oldMaxX, oldMaxZ)));
    }
    private static long area(int minX, int minZ, int maxX, int maxZ) {
        return Math.multiplyExact((long) maxX - minX + 1L, (long) maxZ - minZ + 1L);
    }
}

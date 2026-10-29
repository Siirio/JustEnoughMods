package com.siirio.jemsettlements.town;

/** Exact inclusive block-area quote shared by all expansion interfaces. */
public record TerritoryQuote(int minX, int minZ, int maxX, int maxZ, long addedCopper) {
    public static TerritoryQuote union(int oldMinX, int oldMinZ, int oldMaxX, int oldMaxZ,
                                       int proposedMinX, int proposedMinZ, int proposedMaxX, int proposedMaxZ) {
        return union(oldMinX, oldMinZ, oldMaxX, oldMaxZ, proposedMinX, proposedMinZ, proposedMaxX, proposedMaxZ,
                com.siirio.jemsettlements.payroll.SettlementEconomyConfig.expansionPerBlock(),
                com.siirio.jemsettlements.payroll.SettlementEconomyConfig.expansionMinimum());
    }
    public static TerritoryQuote union(int oldMinX, int oldMinZ, int oldMaxX, int oldMaxZ,
                                       int proposedMinX, int proposedMinZ, int proposedMaxX, int proposedMaxZ,
                                       long perBlock, long minimum) {
        if (perBlock <= 0L || minimum <= 0L) throw new IllegalArgumentException("Expansion rates must be positive");
        int minX = Math.min(oldMinX, proposedMinX), minZ = Math.min(oldMinZ, proposedMinZ);
        int maxX = Math.max(oldMaxX, proposedMaxX), maxZ = Math.max(oldMaxZ, proposedMaxZ);
        long expanded=area(minX,minZ,maxX,maxZ),existing=area(oldMinX,oldMinZ,oldMaxX,oldMaxZ);
        long added=expanded==Long.MAX_VALUE?Long.MAX_VALUE:Math.max(0L,expanded-existing);
        long price=added>Long.MAX_VALUE/perBlock?Long.MAX_VALUE:perBlock*added;
        return new TerritoryQuote(minX,minZ,maxX,maxZ,added==0L?0L:Math.max(minimum,price));
    }
    private static long area(int minX, int minZ, int maxX, int maxZ) {
        long width=(long)maxX-minX+1L,height=(long)maxZ-minZ+1L;
        return width>Long.MAX_VALUE/height?Long.MAX_VALUE:width*height;
    }
}

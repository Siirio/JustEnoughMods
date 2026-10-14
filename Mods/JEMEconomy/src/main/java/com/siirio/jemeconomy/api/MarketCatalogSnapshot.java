package com.siirio.jemeconomy.api;

import java.util.List;

public record MarketCatalogSnapshot(long revision, int page, int pages, String search, String category,
                                    long walletBalance, int[] physicalCounts, long ticksUntilUpdate, List<MarketItemSnapshot> items,
                                    String errorKey) {
    public MarketCatalogSnapshot { physicalCounts = physicalCounts.clone(); items = List.copyOf(items); }
    @Override public int[] physicalCounts() { return physicalCounts.clone(); }
}

package com.siirio.jemeconomy.policy;

import net.minecraft.resources.ResourceLocation;

public record ItemPolicy(int schema, ResourceLocation item, Outcome outcome, Long sellCopper, Long buyCopper,
                         int dailyPlayerUnits, int dailyGlobalUnits, String demandGroup, String notes) {
    public enum Outcome { BUY_AND_SELL, BUY_ONLY, NONE }
    public enum MarketCategory { FOOD, RESOURCES, BLOCKS, DROPS }
    public boolean sellable() { return outcome == Outcome.BUY_AND_SELL && sellCopper != null && sellCopper > 0 && buyCopper != null && buyCopper > 0; }
    public boolean buyable() { return sellable() || outcome == Outcome.BUY_ONLY && buyCopper != null && buyCopper > 0; }
    public MarketCategory marketCategory() {
        String path = item.getPath();
        if (outcome == Outcome.BUY_ONLY) return MarketCategory.BLOCKS;
        if (demandGroup.equals("drops") || path.equals("bone") || path.equals("string") || path.equals("gunpowder")) return MarketCategory.DROPS;
        if (demandGroup.equals("mining")) return MarketCategory.RESOURCES;
        if (demandGroup.equals("forestry") && (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_sapling"))) return MarketCategory.BLOCKS;
        if (path.endsWith("_wool")) return MarketCategory.BLOCKS;
        if (java.util.Set.of("leather", "rabbit_hide", "feather", "honeycomb", "buckskin", "hide").contains(path)) return MarketCategory.RESOURCES;
        if (demandGroup.equals("handcrafts")) return MarketCategory.RESOURCES;
        return MarketCategory.FOOD;
    }
}

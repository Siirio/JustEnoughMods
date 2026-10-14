package com.siirio.jemeconomy.policy;

import net.minecraft.resources.ResourceLocation;

public record ItemPolicy(int schema, ResourceLocation item, Outcome outcome, Long sellCopper, Long buyCopper,
                         int dailyPlayerUnits, int dailyGlobalUnits, String demandGroup, String notes) {
    public enum Outcome { BUY_AND_SELL, NONE }
    public enum MarketCategory { FOOD, RESOURCES, BLOCKS, DROPS }
    public boolean sellable() { return outcome == Outcome.BUY_AND_SELL && sellCopper != null && sellCopper > 0 && buyCopper != null && buyCopper > 0; }
    public boolean buyable() { return sellable(); }
    public MarketCategory marketCategory() {
        String path = item.getPath();
        if (path.equals("bone") || path.equals("string") || path.equals("gunpowder")) return MarketCategory.DROPS;
        if (demandGroup.equals("mining")) return MarketCategory.RESOURCES;
        return MarketCategory.FOOD;
    }
}

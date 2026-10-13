package com.siirio.jemeconomy.policy;

import net.minecraft.resources.ResourceLocation;

public record ItemPolicy(int schema, ResourceLocation item, Outcome outcome, Long sellCopper, Long buyCopper,
                         int dailyPlayerUnits, int dailyGlobalUnits, String demandGroup, String notes) {
    public enum Outcome { BUY_AND_SELL, NONE }
    public boolean sellable() { return outcome == Outcome.BUY_AND_SELL && sellCopper != null && sellCopper > 0 && buyCopper != null && buyCopper > 0; }
    public boolean buyable() { return sellable(); }
}

package com.siirio.jemeconomy.policy;

import net.minecraft.resources.ResourceLocation;

public record ItemPolicy(int schema, ResourceLocation item, Outcome outcome, Long sellCopper, Long buyCopper,
                         int dailyPlayerUnits, int dailyGlobalUnits, String demandGroup, String notes,
                         long sellDenominator, long buyDenominator, ResourceLocation quotaFamily, int quotaUnits,
                         boolean fixedPrice, Long bulk1024) {
    public ItemPolicy(int schema, ResourceLocation item, Outcome outcome, Long sellCopper, Long buyCopper,
                      int dailyPlayerUnits, int dailyGlobalUnits, String demandGroup, String notes) {
        this(schema,item,outcome,sellCopper,buyCopper,dailyPlayerUnits,dailyGlobalUnits,demandGroup,notes,1,1,item,1,false,null);
    }
    public RationalPrice salePrice(int quantity, int multiplier) {
        if (!sellable() || quantity < 0) return RationalPrice.ZERO;
        return new RationalPrice(sellCopper, sellDenominator).multiply(quantity)
                .multiply(fixedPrice ? 100 : multiplier).divide(100);
    }
    public long saleTotal(int quantity, int multiplier) { return salePrice(quantity, multiplier).floor(); }
    public long purchaseTotal(int quantity) {
        if (!buyable() || quantity <= 0) return 0;
        if (bulk1024 != null && quantity >= 256) return new RationalPrice(bulk1024,1024).multiply(quantity).ceil();
        return new RationalPrice(buyCopper,buyDenominator).multiply(quantity).ceil();
    }
    public boolean disc() { return fixedPrice && demandGroup.equals("music_discs"); }
    public enum Outcome { BUY_AND_SELL, BUY_ONLY, SELL_ONLY, NONE }
    public enum MarketCategory { FOOD, RESOURCES, BLOCKS, DROPS }
    public boolean sellable() { return (outcome == Outcome.BUY_AND_SELL || outcome == Outcome.SELL_ONLY) && sellCopper != null && sellCopper > 0; }
    public boolean buyable() { return (outcome == Outcome.BUY_AND_SELL || outcome == Outcome.BUY_ONLY) && buyCopper != null && buyCopper > 0; }
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

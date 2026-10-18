package com.siirio.jemsettlements.payroll;

import java.util.EnumMap;
import net.minecraftforge.common.ForgeConfigSpec;

/** Editable balance; pricing remains server authoritative. Expansion rates and minimums are positive server-owned quotes. */
public final class SettlementEconomyConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.LongValue FOUNDING;
    private static final ForgeConfigSpec.LongValue EMERALD_FOUNDING;
    private static final ForgeConfigSpec.LongValue EXPANSION, EXPANSION_MINIMUM, EMERALD_EXPANSION, EMERALD_EXPANSION_MINIMUM;
    private static final EnumMap<RecruitRank, ForgeConfigSpec.LongValue> EMERALD_HIRE = new EnumMap<>(RecruitRank.class);
    private static final EnumMap<RecruitRank, ForgeConfigSpec.LongValue> EMERALD_WAGE = new EnumMap<>(RecruitRank.class);
    private static final EnumMap<RecruitRank, ForgeConfigSpec.LongValue> HIRE = new EnumMap<>(RecruitRank.class);
    private static final EnumMap<RecruitRank, ForgeConfigSpec.LongValue> WAGE = new EnumMap<>(RecruitRank.class);
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        FOUNDING = builder.comment("Found a 97 by 97 settlement; expansion has separate configurable rates.")
                .defineInRange("foundingCopper", 300L, 1L, 1_000_000L);
        EMERALD_FOUNDING = builder.comment("Standalone emerald count; independent of Coins and never converted from a Coin balance.").defineInRange("foundingEmeralds", 300L, 1L, 1_000_000L);
        EXPANSION = builder.comment("Copper units per newly added inclusive block; 1 is one Copper Coin, not one Gold.")
                .defineInRange("expansionCopperPerBlock", 1L, 1L, 1_000_000L);
        EXPANSION_MINIMUM = builder.defineInRange("expansionMinimumCopper", 10L, 1L, 1_000_000L);
        EMERALD_EXPANSION = builder.comment("Standalone physical emeralds per new block; not a currency conversion.")
                .defineInRange("expansionEmeraldsPerBlock", 1L, 1L, 1_000_000L);
        EMERALD_EXPANSION_MINIMUM = builder.defineInRange("expansionMinimumEmeralds", 1L, 1L, 1_000_000L);
        for (RecruitRank rank : RecruitRank.values()) if (rank != RecruitRank.UNSUPPORTED) {
            builder.push(rank.name().toLowerCase(java.util.Locale.ROOT));
            HIRE.put(rank, builder.defineInRange("hireCopper", rank.defaultHire(), 1L, 1_000_000L));
            WAGE.put(rank, builder.defineInRange("weeklyWageCopper", rank.defaultWage(), 1L, 1_000_000L));
            EMERALD_HIRE.put(rank, builder.defineInRange("hireEmeralds", rank.defaultHire(), 1L, 1_000_000L));
            EMERALD_WAGE.put(rank, builder.defineInRange("weeklyWageEmeralds", rank.defaultWage(), 1L, 1_000_000L));
            builder.pop();
        }
        SPEC = builder.build();
    }
    private SettlementEconomyConfig() { }
    public static long expansionPerBlock() { return (com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? EXPANSION : EMERALD_EXPANSION).get(); }
    public static long expansionMinimum() { return (com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? EXPANSION_MINIMUM : EMERALD_EXPANSION_MINIMUM).get(); }
    public static long founding() { return com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? FOUNDING.get() : EMERALD_FOUNDING.get(); }
    public static long hire(RecruitRank rank) { return rank == RecruitRank.UNSUPPORTED ? 0L : (com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? HIRE : EMERALD_HIRE).get(rank).get(); }
    public static long wage(RecruitRank rank) { return rank == RecruitRank.UNSUPPORTED ? 0L : (com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? WAGE : EMERALD_WAGE).get(rank).get(); }
}

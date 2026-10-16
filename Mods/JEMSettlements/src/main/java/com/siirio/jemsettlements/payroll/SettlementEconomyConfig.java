package com.siirio.jemsettlements.payroll;

import java.util.EnumMap;
import net.minecraftforge.common.ForgeConfigSpec;

/** Editable balance; pricing remains server authoritative. Area price is a protected contract. */
public final class SettlementEconomyConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.LongValue FOUNDING;
    private static final EnumMap<RecruitRank, ForgeConfigSpec.LongValue> HIRE = new EnumMap<>(RecruitRank.class);
    private static final EnumMap<RecruitRank, ForgeConfigSpec.LongValue> WAGE = new EnumMap<>(RecruitRank.class);
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        FOUNDING = builder.comment("Found a 97 by 97 settlement; later expansion is 1 copper per newly added block.")
                .defineInRange("foundingCopper", 300L, 1L, 1_000_000L);
        for (RecruitRank rank : RecruitRank.values()) if (rank != RecruitRank.UNSUPPORTED) {
            builder.push(rank.name().toLowerCase(java.util.Locale.ROOT));
            HIRE.put(rank, builder.defineInRange("hireCopper", rank.defaultHire(), 1L, 1_000_000L));
            WAGE.put(rank, builder.defineInRange("weeklyWageCopper", rank.defaultWage(), 1L, 1_000_000L));
            builder.pop();
        }
        SPEC = builder.build();
    }
    private SettlementEconomyConfig() { }
    public static long founding() { return FOUNDING.get(); }
    public static long hire(RecruitRank rank) { return rank == RecruitRank.UNSUPPORTED ? 0L : HIRE.get(rank).get(); }
    public static long wage(RecruitRank rank) { return rank == RecruitRank.UNSUPPORTED ? 0L : WAGE.get(rank).get(); }
}

package com.siirio.jemsettlements.payroll;

import net.minecraft.resources.ResourceLocation;

public enum RecruitRank {
    RECRUIT(120, 20), BOWMAN(200, 40), HORSEMAN(400, 70), CAPTAIN(700, 120), UNSUPPORTED(0, 0);
    private final long hire;
    private final long wage;
    RecruitRank(long hire, long wage) { this.hire = hire; this.wage = wage; }
    public long defaultHire() { return hire; }
    public long defaultWage() { return wage; }
    public long hiringCost() { return SettlementEconomyConfig.hire(this); }
    public long wage() { return SettlementEconomyConfig.wage(this); }
    public static RecruitRank of(ResourceLocation id) {
        if (!id.getNamespace().equals("recruits")) return UNSUPPORTED;
        return switch (id.getPath()) {
            case "recruit", "scout", "messenger" -> RECRUIT;
            case "bowman", "crossbowman", "recruit_shieldman", "nomad" -> BOWMAN;
            case "horseman", "patrol_leader" -> HORSEMAN;
            case "captain", "villager_noble", "siege_engineer" -> CAPTAIN;
            default -> UNSUPPORTED;
        };
    }
}

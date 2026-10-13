package com.siirio.jemsettlements.payroll;

import net.minecraft.resources.ResourceLocation;

public enum RecruitRank {
    RECRUIT(20), BOWMAN(40), HORSEMAN(70), CAPTAIN(120), UNSUPPORTED(0);
    private final long wage;
    RecruitRank(long wage) { this.wage = wage; }
    public long wage() { return wage; }
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

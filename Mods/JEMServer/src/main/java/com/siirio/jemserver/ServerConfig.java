package com.siirio.jemserver;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue HISTORY_SIZE;
    public static final ForgeConfigSpec.IntValue SAFE_RADIUS;
    public static final ForgeConfigSpec.IntValue KILL_CREDIT_TICKS;
    public static final ForgeConfigSpec.BooleanValue SLEEP_ENABLED;
    public static final ForgeConfigSpec.IntValue SLEEP_VOTE_SECONDS;
    public static final ForgeConfigSpec.BooleanValue LANDMARKS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue LEADERBOARD_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_PUBLIC_PER_PLAYER;
    public static final ForgeConfigSpec.IntValue MAX_LANDMARKS;
    public static final ForgeConfigSpec.IntValue NAME_LENGTH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("death");
        HISTORY_SIZE = builder.defineInRange("historySize", 3, 1, 3);
        SAFE_RADIUS = builder.defineInRange("safeSearchRadius", 8, 1, 16);
        builder.pop().push("sleep");
        SLEEP_ENABLED = builder.define("enabled", true);
        SLEEP_VOTE_SECONDS = builder.defineInRange("voteSeconds", 9, 1, 60);
        builder.pop().push("landmarks");
        LANDMARKS_ENABLED = builder.define("enabled", true);
        MAX_PUBLIC_PER_PLAYER = builder.defineInRange("maxPublicPerPlayer", 32, 1, 4096);
        MAX_LANDMARKS = builder.defineInRange("maxLandmarks", 512, 1, 4096);
        NAME_LENGTH = builder.defineInRange("maxNameLength", 48, 1, 80);
        builder.pop().push("leaderboard");
        LEADERBOARD_ENABLED = builder.define("enabled", true);
        KILL_CREDIT_TICKS = builder.defineInRange("environmentalKillCreditTicks", 100, 1, 1200);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {}
}

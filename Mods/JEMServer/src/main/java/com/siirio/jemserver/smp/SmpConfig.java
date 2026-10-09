package com.siirio.jemserver.smp;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SmpConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SHOP_BATCH,
            SHOP_LIMIT,
            SHOP_FAVORITES_LIMIT,
            WAYPOINT_MINUTES,
            RUSH_TRACKED_BLOCKS,
            CONTRACT_DELIVERIES,
            DELIVERY_BATCH,
            EVENT_REGION_RETRY_SECONDS,
            SHOP_STACK_NBT_BYTES,
            DISCOVERY_INTERVAL;
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE,
            HISTORY,
            POST_COOLDOWN,
            REFRESH_TICKS,
            PAGE_SIZE,
            MAX_TEXT,
            PARTY_WAIT_MINUTES,
            ENCOUNTER_RADIUS,
            EVENT_HOURS,
            EVENT_GAP_HOURS,
            EVENT_REGION_CHUNKS,
            EVENT_CLAIM_BUFFER,
            EVENT_DISTANCE,
            WAVE_PAUSE;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SCHEDULE;

    public static final ForgeConfigSpec.IntValue COOKING_DISH_COUNT, COOKING_DISH_QUANTITY, COOKING_MINUTES;

    static {
        var b = new ForgeConfigSpec.Builder();
        DISCOVERY_INTERVAL = b.defineInRange("discoveryIntervalTicks", 200, 20, 1200);
        EVENT_REGION_RETRY_SECONDS = b.defineInRange("eventRegionRetrySeconds", 60, 1, 3600);
        SHOP_STACK_NBT_BYTES = b.defineInRange("shopStackNbtBytes", 8192, 256, 65536);
        CONTRACT_DELIVERIES = b.defineInRange("pendingServiceRewardStacks", 256, 9, 4096);
        DELIVERY_BATCH = b.defineInRange("deliveryStacksPerRequest", 36, 1, 256);
        SHOP_LIMIT = b.defineInRange("shopIndexLimit", 65536, 256, 262144);
        SHOP_FAVORITES_LIMIT = b.defineInRange("shopFavoritesLimit", 256, 8, 1024);
        WAYPOINT_MINUTES = b.defineInRange("temporaryWaypointMinutes", 60, 5, 1440);
        RUSH_TRACKED_BLOCKS = b.defineInRange("rushTrackedBlocks", 262144, 1024, 1048576);
        SHOP_BATCH = b.defineInRange("shopIndexUpdatesPerTick", 64, 1, 1024);
        MAX_ACTIVE = b.defineInRange("activeRecordsPerPlayer", 8, 1, 64);
        HISTORY = b.defineInRange("retainedHistory", 512, 32, 8192);
        POST_COOLDOWN = b.defineInRange("publishCooldownSeconds", 60, 1, 3600);
        REFRESH_TICKS = b.defineInRange("viewRefreshTicks", 20, 10, 200);
        PAGE_SIZE = b.defineInRange("pageSize", 20, 5, 40);
        MAX_TEXT = b.defineInRange("descriptionLength", 512, 32, 1024);
        PARTY_WAIT_MINUTES = b.defineInRange("partyWaitMinutes", 5, 1, 60);
        ENCOUNTER_RADIUS = b.defineInRange("registrationRadius", 64, 8, 256);
        EVENT_HOURS = b.defineInRange("eventDurationHours", 3, 1, 12);
        EVENT_GAP_HOURS = b.defineInRange("catchupGapHours", 12, 1, 48);
        EVENT_REGION_CHUNKS = b.defineInRange("eventRegionChunks", 5, 1, 31);
        EVENT_CLAIM_BUFFER = b.defineInRange("eventClaimBufferBlocks", 64, 16, 512);
        EVENT_DISTANCE = b.defineInRange("eventMinimumSpawnDistance", 4096, 1024, 30000000);
        WAVE_PAUSE = b.defineInRange("bloodMoonRecoverySeconds", 15, 5, 120);
        SCHEDULE =
                b.defineList(
                        "weeklySlotsAstana",
                        java.util.List.of(
                                "COOKING_SHOW:MONDAY:19:00",
                                "RESOURCE_RUSH:TUESDAY:19:00",
                                "FISHING:WEDNESDAY:19:00",
                                "COOKING_SHOW:THURSDAY:19:00",
                                "BLOOD_MOON:FRIDAY:19:00",
                                "RESOURCE_RUSH:SATURDAY:13:00",
                                "FISHING:SATURDAY:16:00",
                                "FISHING:SUNDAY:13:00",
                                "BOSS_RAID:SUNDAY:19:00"),
                        value ->
                                value instanceof String text
                                        && text.matches(
                                                "(RESOURCE_RUSH|FISHING|BOSS_RAID|BLOOD_MOON|COOKING_SHOW):[A-Z]+:[0-2][0-9]:[0-5][0-9]"));
        COOKING_DISH_COUNT = b.defineInRange("cookingDishCount", 3, 1, 8);
        COOKING_DISH_QUANTITY = b.defineInRange("cookingDishQuantity", 4, 1, 64);
        COOKING_MINUTES = b.defineInRange("cookingMinutes", 60, 20, 120);
        SPEC = b.build();
    }

    private SmpConfig() {}
}

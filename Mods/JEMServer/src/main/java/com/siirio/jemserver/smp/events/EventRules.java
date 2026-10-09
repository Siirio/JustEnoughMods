package com.siirio.jemserver.smp.events;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EventRules {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SPAWN_BATCH, SPAWN_ATTEMPTS, BLOOD_SPAWN_MIN_DISTANCE, BLOOD_SPAWN_MAX_DISTANCE, SAFE_DEATHS, FAILURE_DEATHS, DOWNED_SECONDS, ENTRY_PROMPT_DISTANCE, STAGING_RADIUS, RAID_SEARCH_RADIUS, BOUNDARY_DISTANCE, MINIMUM_LAND_PERCENT, BLOOD_COLOR, RUSH_COLOR, RAID_COLOR;
    public static final ForgeConfigSpec.DoubleValue RESPAWN_HEALTH, CHAMPION_SCALE, FINAL_SCALE, PROJECTILE_RESTITUTION;
    static {
        var b=new ForgeConfigSpec.Builder();
        SPAWN_BATCH=b.defineInRange("spawnBatchPerSecond",4,1,16);
        SPAWN_ATTEMPTS=b.defineInRange("spawnAttemptsPerMob",8,1,32);
        BLOOD_SPAWN_MIN_DISTANCE=b.defineInRange("bloodMoonSpawnMinDistance",12,8,24);
        BLOOD_SPAWN_MAX_DISTANCE=b.defineInRange("bloodMoonSpawnMaxDistance",28,16,48);
        SAFE_DEATHS=b.defineInRange("bloodMoonSafeDeaths",2,0,10);
        FAILURE_DEATHS=b.defineInRange("bloodMoonFailureDeaths",3,1,10);
        DOWNED_SECONDS=b.defineInRange("combatDownedSeconds",10,3,60);
        ENTRY_PROMPT_DISTANCE=b.defineInRange("combatEntryPromptDistance",6,2,24);
        PROJECTILE_RESTITUTION=b.defineInRange("combatBarrierProjectileRestitution",.9,.1,1);
        RESPAWN_HEALTH=b.defineInRange("safeRespawnHealth",.8,.1,1);
        CHAMPION_SCALE=b.defineInRange("waveThreeMinibossMultiplier",1.15,1,3);
        FINAL_SCALE=b.defineInRange("waveFiveMinibossMultiplier",1.2,1,3);
        STAGING_RADIUS=b.defineInRange("entityBossStagingRadius",20,8,64);
        RAID_SEARCH_RADIUS=b.defineInRange("raidBossSearchRadius",256,16,2048);
        BOUNDARY_DISTANCE=b.defineInRange("boundaryViewDistance",32,8,128);
        MINIMUM_LAND_PERCENT=b.defineInRange("eventMinimumLandPercent",90,50,100);
        BLOOD_COLOR=b.defineInRange("bloodMoonColor",0xBB172D,0,0xFFFFFF);
        RUSH_COLOR=b.defineInRange("resourceRushColor",0xBAC74B,0,0xFFFFFF);
        RAID_COLOR=b.defineInRange("raidColor",0xE8BB58,0,0xFFFFFF);
        SPEC=b.build();
    }
    private EventRules() {}
}

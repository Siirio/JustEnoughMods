package com.siirio.jemworldbosstiers.encounter;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HostedConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue RAID_HEALTH;
    public static final ForgeConfigSpec.DoubleValue RAID_DAMAGE;
    public static final ForgeConfigSpec.IntValue BLEEDOUT;
    public static final ForgeConfigSpec.IntValue REVIVE;
    public static final ForgeConfigSpec.IntValue GRACE;
    public static final ForgeConfigSpec.IntValue IMMUNITY;
    public static final ForgeConfigSpec.DoubleValue REVIVE_HEALTH;
    public static final ForgeConfigSpec.DoubleValue REVIVE_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue MINIMUM_CONTRIBUTION;
    public static final ForgeConfigSpec.IntValue RESOURCE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue DELIVERY_INTERVAL;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> PROFILE_OVERRIDES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        RAID_HEALTH = builder.defineInRange("raidHealthMultiplier", 2.5, 1, 10);
        RAID_DAMAGE = builder.defineInRange("raidDamageMultiplier", 2.5, 1, 10);
        BLEEDOUT = builder.defineInRange("bleedoutTicks", 600, 20, 72000);
        REVIVE = builder.defineInRange("reviveHoldTicks", 100, 1, 1200);
        GRACE = builder.defineInRange("disconnectGraceTicks", 1800, 0, 72000);
        IMMUNITY = builder.defineInRange("reviveImmunityTicks", 60, 0, 1200);
        REVIVE_HEALTH = builder.defineInRange("reviveHealthFraction", 0.4, 0.01, 1);
        REVIVE_DISTANCE = builder.defineInRange("reviveDistance", 3.0, 1, 8);
        MINIMUM_CONTRIBUTION = builder.defineInRange("minimumDamageFraction", 0.005, 0.0001, 1);
        RESOURCE_MULTIPLIER = builder.defineInRange("raidResourceMultiplier", 3, 1, 64);
        DELIVERY_INTERVAL = builder.defineInRange("pendingDeliveryRetryTicks", 100, 20, 12000);
        PROFILE_OVERRIDES = builder.defineListAllowEmpty("profileOverrides", java.util.List.of(), HostedConfig::validProfile);
        SPEC = builder.build();
    }

    private HostedConfig() {
    }

    private static boolean validProfile(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        String[] fields = text.split(";");
        if (fields.length != 5 || net.minecraft.resources.ResourceLocation.tryParse(fields[0]) == null) {
            return false;
        }
        try {
            for (int i = 1; i < fields.length; i++) {
                double number = Double.parseDouble(fields[i]);
                if (!Double.isFinite(number) || number < 0 || number > 10) {
                    return false;
                }
            }
            return Double.parseDouble(fields[3]) > 0 && Double.parseDouble(fields[4]) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static HostedScaling scale(String profile, int participants, boolean raid) {
        double raidHealth = RAID_HEALTH.get();
        double raidDamage = RAID_DAMAGE.get();
        for (String override : PROFILE_OVERRIDES.get()) {
            String[] fields = override.split(";");
            if (fields[0].equals(profile)) {
                raidHealth = Double.parseDouble(fields[3]);
                raidDamage = Double.parseDouble(fields[4]);
                break;
            }
        }
        return HostedScaling.calculate(participants, raid, raidHealth, raidDamage);
    }
}

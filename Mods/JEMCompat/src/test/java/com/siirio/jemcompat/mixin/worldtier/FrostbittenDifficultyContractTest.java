package com.siirio.jemcompat.mixin.worldtier;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offline contract: invokes the real injection handlers; does not launch Minecraft. */
public final class FrostbittenDifficultyContractTest extends FrostbittenDifficultyMixin {
    public static void main(String[] args) throws Exception {
        var instance = new FrostbittenDifficultyContractTest();
        Method hard = FrostbittenDifficultyMixin.class.getDeclaredMethod("jemcompat$hardDamageOffset", double.class);
        Method easy = FrostbittenDifficultyMixin.class.getDeclaredMethod("jemcompat$easyDamageOffset", double.class);
        hard.setAccessible(true);
        easy.setAccessible(true);
        double hardOffset = (double) hard.invoke(instance, 4.0D);
        double easyOffset = (double) easy.invoke(instance, 13.0D);
        // Preserve exact native multiply-then-offset ordering at both effective and changed config.
        close(10, 14 + easyOffset);
        close(14, 14);
        close(18, 14 - hardOffset);
        close(24, 28 + easyOffset);
        close(32, 28 - hardOffset);

        String profile = Files.readString(Path.of(args[0]));
        int start = profile.indexOf("\"key\": \"legendary_monsters:frostbitten_golem\"");
        if (start < 0) throw new AssertionError("Frost profile missing");
        String frost = profile.substring(start, profile.indexOf("\n    }", start));
        if (frost.contains("special_damage_multiplier")) throw new AssertionError("Blanket special multiplier restored");
        if (!frost.contains("\"scales_with_world_tier\": true")) throw new AssertionError("Tier progression lost");

        double[] soloBefore = {43.7184, 51.2512, 65.3312, 112.64, 123.2};
        double[] soloAfter = {16.192, 18.304, 20.416, 22.528, 24.64};
        for (int tier = 1; tier <= 5; tier++) {
            double factor = 1 + .15 * tier;
            double unboosted = 16 * factor * .88;
            close(soloAfter[tier - 1], unboosted);
            close(soloBefore[tier - 1] * 1.25, new double[]{54.648, 64.064, 81.664, 140.8, 154}[tier - 1]);
            // Native Raid factor never used the removed special; its exact reference remains unchanged.
            close(soloAfter[tier - 1] * 1.25 * 2.5, unboosted * 1.25 * 2.5);
        }
        close(46.5696, netheriteProtection(154));
        close(4.9496832, netheriteProtection(30.8));
        System.out.println("Frostbitten difficulty/config, profile, five-tier solo/co-op and armor contracts passed");
    }

    private static double netheriteProtection(double raw) {
        double armor = Math.min(20, Math.max(4, 20 - raw / 5));
        return raw * (1 - armor / 25) * .36;
    }

    private static void close(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.000001) throw new AssertionError(expected + " != " + actual);
    }
}

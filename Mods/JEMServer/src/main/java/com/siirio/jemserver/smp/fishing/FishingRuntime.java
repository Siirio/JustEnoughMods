package com.siirio.jemserver.smp.fishing;

import com.li64.tide.data.TideData;
import com.li64.tide.data.fishing.FishData;
import com.li64.tide.data.fishing.FishingContext;
import com.li64.tide.data.fishing.mediums.FishingMedium;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import com.siirio.jemserver.smp.Profiles;
import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.events.EventScheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;

public final class FishingRuntime {
    public static boolean active(FishingContext context) {
        return context != null && context.level() != null && context.level().getServer() != null
                && EventScheduler.active(context.level().getServer(), "FISHING") != null;
    }
    public static boolean allowed(FishingContext context) {
        var profile = FishingProfiles.get(context.rod());
        if (context.hook() != null && context.medium().equals("void") && context.hook().canFishInVoid()) return true;
        if (context.dimension().equals(Level.NETHER)) return profile.nether() || context.hook() != null && context.hook().canFishInLava();
        if (context.dimension().equals(Level.END)) return profile.end();
        return true;
    }
    public static double weight(FishData fish, FishingContext context, double nativeWeight) {
        int rarity = fish.profile().rarity().ordinal();
        double nativeTotal = TideData.FISH.get().values().stream()
                .filter(candidate -> candidate.profile().rarity().ordinal() == rarity && candidate.shouldKeep(context))
                .mapToDouble(candidate -> Math.max(0, candidate.modifyWeight(candidate.weight(), candidate.quality(), candidate.modifiers(), context)))
                .sum();
        return nativeTotal <= 0 ? 0 : nativeWeight * FishingProfiles.chances(context.rod())[rarity] / nativeTotal;
    }
    public static void caught(TideFishingHook hook) {
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player) || player instanceof FakePlayer || player.isCreative()) return;
        if (hook.getCatchType() != TideFishingHook.CatchType.FISH) return;
        var row = EventScheduler.active(player.server, "FISHING");
        if (row == null || hook.getPersistentData().getBoolean("jem_fishing_rewarded")) return;
        int xp = 0, catches = 0;
        var profile = FishingProfiles.get(hook.rod());
        int minimum = FishingConfig.OVERWORLD_MIN.get(), maximum = FishingConfig.OVERWORLD_MAX.get();
        if (player.level().dimension().equals(Level.NETHER)) { minimum = FishingConfig.NETHER_MIN.get(); maximum = FishingConfig.NETHER_MAX.get(); }
        if (player.level().dimension().equals(Level.END)) { minimum = FishingConfig.END_MIN.get(); maximum = FishingConfig.END_MAX.get(); }
        for (var stack : hook.getHookedItems()) {
            var fish = FishData.get(stack).orElse(null);
            if (fish == null || stack.isEmpty()) continue;
            double movement = switch (fish.behavior()) { case JITTER, DARTS, LINEAR_WRAP -> 1; case PLATEAU -> .6; default -> .3; };
            double difficulty = .5 * fish.strength() + .35 * Math.min(1, fish.speed() / 2) + .15 * movement;
            xp += FishingXp.calculate(minimum, maximum, fish.profile().rarity().ordinal(), difficulty, profile.quality(),
                    Math.min(1, fish.conditions().size() / 10.0 + hook.getLuck() / 20.0),
                    hook.getCurrentMedium() == FishingMedium.VOID ? FishingConfig.VOID_MULTIPLIER.get() : 1, FishingConfig.CAP_VOID.get());
            catches++;
        }
        if (catches == 0) return;
        hook.getPersistentData().putBoolean("jem_fishing_rewarded", true);
        com.siirio.jemworldbosstiers.api.ExperienceRewardApi.give(player, xp);
        var scores = row.getCompound("scores");
        var score = scores.getCompound(player.getUUID().toString());
        if (score.getInt("catches") == 0) {
            Profiles.count(player.server, player.getUUID(), "fishingEvents");
            Profiles.countEvent(player.server, player.getUUID(), "FISHING");
        }
        score.putInt("catches", score.getInt("catches") + catches);
        score.putInt("points", score.getInt("points") + xp);
        score.putInt("fishingXp", score.getInt("fishingXp") + xp);
        scores.put(player.getUUID().toString(), score);
        row.put("scores", scores);
        SmpData.get(player.server).changed(row);
    }
    private FishingRuntime() {}
}

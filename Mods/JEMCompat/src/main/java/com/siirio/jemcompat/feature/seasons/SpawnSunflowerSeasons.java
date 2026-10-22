package com.siirio.jemcompat.feature.seasons;

import com.ninni.spawn.registry.SpawnBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sereneseasons.season.SeasonalCropGrowthHandler;

/** Native fertility policy for Spawn's mature sunflower seed refill. */
public final class SpawnSunflowerSeasons {
    private SpawnSunflowerSeasons() { }

    public static boolean allowSeedGrowth(ServerLevel level, BlockPos pos) {
        // Only the juvenile is tagged: tagging the mature flower would also
        // suppress sun tracking and apply the random-tick penalty a second time.
        // Use its fertility metadata at the actual mature flower's position so
        // Serene Seasons still owns seasons, biomes, greenhouse and config rules.
        CallbackInfo growth = new CallbackInfo("jemcompat$spawnSunflowerSeedGrowth", true);
        SeasonalCropGrowthHandler.onCropGrowth(level, pos,
                SpawnBlocks.SUNFLOWER_PLANT.get().defaultBlockState(), growth);
        return !growth.isCancelled();
    }
}

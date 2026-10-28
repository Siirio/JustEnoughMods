package com.justenoughmods.achievementguide.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exact Serene Seasons 9.1.0.3 greenhouse-glass predicate, including its native height bound. */
@Mixin(targets = "sereneseasons.season.SeasonalCropGrowthHandler", remap = false)
@Pseudo
public interface GreenhouseQuestAccess {
    @Invoker(value = "isGlassAboveBlock", remap = false)
    static boolean jem$covered(Level level, BlockPos pos) { throw new AssertionError("Native greenhouse invoker was not transformed"); }
}

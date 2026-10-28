package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeHarvestProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class RemainingFoodNativeDropMixin {
    @Inject(method = "popResource", at = @At("HEAD"))
    private static void jem$actualSource(Level level, BlockPos pos, ItemStack source, CallbackInfo callback) { NativeHarvestProof.expect(level, pos, source); }
    @Inject(method = "popResource", at = @At("RETURN"))
    private static void jem$sourceEnd(Level level, BlockPos pos, ItemStack source, CallbackInfo callback) { NativeHarvestProof.expectedEnd(source); }
}

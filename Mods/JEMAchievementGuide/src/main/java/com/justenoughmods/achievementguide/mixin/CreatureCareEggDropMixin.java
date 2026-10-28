package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareEggs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class CreatureCareEggDropMixin {
    @Inject(method = "popResource", at = @At("HEAD"))
    private static void jem$before(Level level, BlockPos pos, ItemStack stack, CallbackInfo callback) { CreatureCareEggs.expected(level, pos, stack); }
    @Inject(method = "popResource", at = @At("RETURN"))
    private static void jem$after(Level level, BlockPos pos, ItemStack stack, CallbackInfo callback) { CreatureCareEggs.expectedEnd(stack); }
}

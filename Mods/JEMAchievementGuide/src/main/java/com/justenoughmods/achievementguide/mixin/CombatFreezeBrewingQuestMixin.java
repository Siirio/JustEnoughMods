package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatBrewingProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BrewingStandBlockEntity.class)
public abstract class CombatFreezeBrewingQuestMixin {
    @Inject(method = "doBrew", at = @At("HEAD"))
    private static void jem$begin(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo callback) { CombatBrewingProof.begin(level, pos); }
    @Inject(method = "doBrew", at = @At("RETURN"))
    private static void jem$end(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo callback) { CombatBrewingProof.end(); }
}

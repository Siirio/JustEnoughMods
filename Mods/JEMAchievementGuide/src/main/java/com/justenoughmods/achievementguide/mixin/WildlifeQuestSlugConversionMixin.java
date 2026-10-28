package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.block.BlockBananaSlugSlime", remap = false)
public abstract class WildlifeQuestSlugConversionMixin {
    @Inject(method = "removeWaterBreadthFirstSearch", at = @At("HEAD"))
    private void jem$before(Level level, BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callback) { WildlifeQuestSlime.begin(pos); }
    @Inject(method = "removeWaterBreadthFirstSearch", at = @At("RETURN"))
    private void jem$after(Level level, BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callback) { WildlifeQuestSlime.end(); }
    @Redirect(method = "removeWaterBreadthFirstSearch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;m_142598_(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private net.minecraft.world.item.ItemStack jem$drained(net.minecraft.world.level.block.BucketPickup block, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        boolean water = level.getFluidState(pos).is(FluidTags.WATER); var output = block.pickupBlock(level, pos, state);
        if (water && !output.isEmpty() && level.getBlockState(pos).isAir()) WildlifeQuestSlime.drained(pos); return output;
    }
    @Redirect(method = "removeWaterBreadthFirstSearch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$converted(Level level, BlockPos pos, BlockState state, Level original, BlockPos root) {
        boolean water = level.getFluidState(pos).is(FluidTags.WATER) || WildlifeQuestSlime.water(pos); boolean accepted = level.setBlockAndUpdate(pos, state);
        if (accepted && water && level.getBlockState(pos).equals(state) && WildlifeQuestSetups.block(state).equals("alexsmobs:crystalized_banana_slug_mucus")) WildlifeQuestWorldChanges.converted(level, root, pos);
        return accepted;
    }
}

package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareEggs;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.block.EagleEgg", remap = false)
public abstract class CreatureCareEagleEggMixin {
    @Unique private final ThreadLocal<CreatureCareEggs.Harvest> jem$harvest = new ThreadLocal<>();
    @Unique private final ThreadLocal<CreatureCareEggs.Hatch> jem$hatch = new ThreadLocal<>();
    @Inject(method = "m_6240_", at = @At("HEAD"))
    private void jem$harvestBefore(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo callback) {
        jem$harvest.set(CreatureCareEggs.harvest(CreatureCareProof.real(player), pos));
    }
    @Inject(method = "m_6240_", at = @At("RETURN"))
    private void jem$harvestAfter(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo callback) {
        var run = jem$harvest.get(); jem$harvest.remove(); CreatureCareEggs.harvested(run);
    }
    @Inject(method = "m_213898_", at = @At("HEAD"))
    private void jem$hatchBefore(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        jem$hatch.set(CreatureCareEggs.hatch(level, pos, state));
    }
    @Inject(method = "m_213898_", at = @At("RETURN"))
    private void jem$hatchAfter(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        var run = jem$hatch.get(); jem$hatch.remove(); CreatureCareEggs.hatched(run);
    }
}

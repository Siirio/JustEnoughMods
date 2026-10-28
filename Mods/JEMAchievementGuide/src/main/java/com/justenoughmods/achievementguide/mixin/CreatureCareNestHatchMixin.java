package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareEggs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.primal.block_entity.NestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.block.NestBlock", remap = false)
public abstract class CreatureCareNestHatchMixin {
    @Unique private static final ThreadLocal<CreatureCareEggs.Hatch> jem$hatch = new ThreadLocal<>();
    @Inject(method = "randomEggTick", at = @At("HEAD"))
    private static void jem$before(BlockState nestState, BlockState eggState, Level level, BlockPos pos, RandomSource random, IntegerProperty property, CallbackInfo callback) {
        jem$hatch.remove();
        if (level instanceof ServerLevel server) jem$hatch.set(CreatureCareEggs.hatch(server, pos, eggState));
    }
    @Inject(method = "randomEggTick", at = @At("RETURN"))
    private static void jem$after(BlockState nestState, BlockState eggState, Level level, BlockPos pos, RandomSource random, IntegerProperty property, CallbackInfo callback) {
        var run = jem$hatch.get(); jem$hatch.remove();
        CreatureCareEggs.finishHatch(run, level.getBlockEntity(pos) instanceof NestBlockEntity nest && nest.getEgg().isEmpty());
    }
}

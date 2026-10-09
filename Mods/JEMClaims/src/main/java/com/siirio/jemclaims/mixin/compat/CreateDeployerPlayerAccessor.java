package com.siirio.jemclaims.mixin.compat;

import net.minecraft.core.BlockPos;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer", remap = false)
public interface CreateDeployerPlayerAccessor {
    @Accessor("blockBreakingProgress")
    Pair<BlockPos, Float> jemClaims$blockBreakingProgress();
}

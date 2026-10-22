package com.siirio.jemvisualsandbox.mixin;

import com.siirio.jemvisualsandbox.workbench.SculkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Temporary terrain must not become a native catalyst propagation surface. */
@Mixin(SculkSpreader.ChargeCursor.class)
public abstract class TemporarySculkGrowthMixin {
    @Shadow private BlockPos pos;
    @Shadow int charge;
    @Inject(method="update",at=@At("HEAD"),cancellable=true)
    private void sandbox$stopTemporaryGrowth(LevelAccessor level,BlockPos origin,RandomSource random,
                                             SculkSpreader spreader,boolean spread,CallbackInfo ci){
        if(SculkDomain.touchesTemporary(level,pos)){
            charge=0;ci.cancel();
        }
    }
}

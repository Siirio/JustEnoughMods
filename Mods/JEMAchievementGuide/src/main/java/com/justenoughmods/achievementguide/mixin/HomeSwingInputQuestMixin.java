package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeSwingProof;
import euphy.upo.casualswing.content.entity.SwingSeatEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SwingSeatEntity.class, remap = false)
public abstract class HomeSwingInputQuestMixin {
    @Shadow private BlockPos originPos;
    @Shadow private boolean isSwing;
    @Inject(method = "updateSwingState", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) { HomeSwingProof.before((SwingSeatEntity)(Object)this, originPos); }
    @Inject(method = "updateSwingState", at = @At("RETURN"))
    private void jem$after(CallbackInfo callback) { HomeSwingProof.after((SwingSeatEntity)(Object)this, originPos, isSwing); }
}

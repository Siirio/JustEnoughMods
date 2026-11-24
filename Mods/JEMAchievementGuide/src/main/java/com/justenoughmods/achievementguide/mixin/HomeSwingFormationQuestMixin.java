package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeSwingProof;
import euphy.upo.casualswing.content.rope.BaseSwingRopeBlockEntity;
import euphy.upo.casualswing.util.SwingStructureManager;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SwingStructureManager.class, remap = false)
public abstract class HomeSwingFormationQuestMixin {
    @Inject(method = "tryFormStructure", at = @At("RETURN"))
    private static void jem$formed(BaseSwingRopeBlockEntity master, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) HomeSwingProof.assembled(master, List.copyOf(master.getSwingParts()));
    }
}

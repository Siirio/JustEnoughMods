package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatEffectProof;
import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AreaEffectCloud.class)
public abstract class CombatLingeringApplicationQuestMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { CombatEffectProof.applyBegin((AreaEffectCloud)(Object)this); }
    @Inject(method = "tick", at = @At("RETURN"))
    private void jem$end(CallbackInfo callback) { CombatEffectProof.applyEnd(); }
}

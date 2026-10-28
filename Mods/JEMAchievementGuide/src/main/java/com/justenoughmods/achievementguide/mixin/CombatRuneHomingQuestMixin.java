package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatRuneProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.entity.BeeArrowEntity", remap = false)
public abstract class CombatRuneHomingQuestMixin {
    @Unique private Vec3 jem$velocity;
    @Inject(method = "updatePainHoming", at = @At("HEAD"))
    private void jem$begin(CallbackInfo callback) { jem$velocity = ((Entity)(Object)this).getDeltaMovement(); }
    @Inject(method = "updatePainHoming", at = @At("RETURN"))
    private void jem$end(CallbackInfo callback) { Entity arrow = (Entity)(Object)this; if (jem$velocity != null && arrow.getDeltaMovement().distanceToSqr(jem$velocity) > 0.000001) CombatRuneProof.markChanged(arrow, "pain"); jem$velocity = null; }
}

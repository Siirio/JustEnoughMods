package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class WorldBattleExtraPullQuestMixin {
    @Unique private Vec3 jem$beforeArtifactPull;
    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void jem$before(Vec3 motion, CallbackInfo ci) { jem$beforeArtifactPull = ((Entity)(Object)this).getDeltaMovement(); }
    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void jem$after(Vec3 motion, CallbackInfo ci) {
        if (jem$beforeArtifactPull != null) WorldBattleExtraProof.velocity((Entity)(Object)this, jem$beforeArtifactPull);
        jem$beforeArtifactPull = null;
    }
}

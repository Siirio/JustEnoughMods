package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.CombatCastCancelProof;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Snapshot before the installed EFIS priority-1000 HEAD cancellation handler. */
@Pseudo
@Mixin(targets = "yesman.epicfight.skill.SkillContainer", remap = false, priority = 1100)
public abstract class CombatCancelQuestMixin {
    @Unique private CombatCastCancelProof.Run jem$cancelRun;
    @Inject(method = "requestCasting", at = @At("HEAD"))
    private void jem$beforeCombatRequest(ServerPlayerPatch executor, FriendlyByteBuf buffer, CallbackInfoReturnable<Boolean> callback) {
        jem$cancelRun = CombatCastCancelProof.begin((SkillContainer) (Object) this, executor);
    }
    @Inject(method = "requestCasting", at = @At("RETURN"))
    private void jem$afterCombatRequest(ServerPlayerPatch executor, FriendlyByteBuf buffer, CallbackInfoReturnable<Boolean> callback) {
        var run = jem$cancelRun;
        jem$cancelRun = null;
        if (run != null) CombatCastCancelProof.finish(run, callback.getReturnValueZ());
    }
}

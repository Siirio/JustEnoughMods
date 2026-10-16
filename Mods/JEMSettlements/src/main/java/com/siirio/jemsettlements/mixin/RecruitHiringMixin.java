package com.siirio.jemsettlements.mixin;

import com.siirio.jemidentity.api.DedicatedSmp;
import com.siirio.jemsettlements.compat.recruits.RecruitHiringService;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractRecruitEntity.class, remap = false)
public abstract class RecruitHiringMixin {
    @Inject(method = "hire", at = @At("HEAD"), cancellable = true)
    private void jem$validate(Player player, RecruitsGroup group, boolean dialogue, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer && DedicatedSmp.active(serverPlayer)
                && !RecruitHiringService.mayHire(serverPlayer, (AbstractRecruitEntity) (Object) this)) cir.setReturnValue(false);
    }
    @Inject(method = "hire", at = @At(value = "INVOKE", target = "Lcom/talhanation/recruits/entities/AbstractRecruitEntity;makeHireSound()V"), cancellable = true)
    private void jem$charge(Player player, RecruitsGroup group, boolean dialogue, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer && DedicatedSmp.active(serverPlayer)
                && !RecruitHiringService.charge(serverPlayer, (AbstractRecruitEntity) (Object) this)) cir.setReturnValue(false);
    }
    @Inject(method = "hire", at = @At("RETURN"))
    private void jem$enroll(Player player, RecruitsGroup group, boolean dialogue, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && player instanceof ServerPlayer serverPlayer && DedicatedSmp.active(serverPlayer))
            RecruitHiringService.hired(serverPlayer, (AbstractRecruitEntity) (Object) this);
    }
    @Inject(method = "checkPayment", at = @At("HEAD"), cancellable = true)
    private void jem$onePayrollOwner(Container inventory, CallbackInfo ci) {
        // This mixin is physically absent on clients/integrated servers.
        ci.cancel();
    }
}

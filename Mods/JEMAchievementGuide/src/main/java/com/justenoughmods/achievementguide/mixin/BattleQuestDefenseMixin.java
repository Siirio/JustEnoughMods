package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.damagesource.DamageSource; import net.minecraft.world.phys.Vec3; import yesman.epicfight.api.utils.AttackResult; import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
@Mixin(value = yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch.class, remap = false)
public abstract class BattleQuestDefenseMixin {
    @Inject(method="onDodgeSuccess",at=@At("RETURN")) private void jem$dodge(DamageSource source, Vec3 point, CallbackInfo ci) { BattleQuestEpic.dodge((ServerPlayerPatch)(Object)this,source); }
    @Inject(method="tryHurt",at=@At("RETURN")) private void jem$guard(DamageSource source,float amount,CallbackInfoReturnable<AttackResult> ci) { BattleQuestEpic.guard((ServerPlayerPatch)(Object)this,source,ci.getReturnValue()); }
}

package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.living.VesperEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.VesperAttackGoal.class,remap=false)
public abstract class CaveQuestVesperMixin {
    @Shadow private VesperEntity entity;
    @Unique private int jem$caveGroundedBefore;
    @Unique private ServerPlayer jem$caveBlockedBy;
    @Inject(method="m_8037_",at=@At("HEAD"))private void jem$before(CallbackInfo ci){jem$caveGroundedBefore=entity.groundedFor;jem$caveBlockedBy=null;}
    @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$actualShieldBlock(LivingEntity target,DamageSource source,float amount){
        var outcome=CaveQuestShieldRun.hurt(target,source,()->target.hurt(source,amount));
        if(outcome.blocked() && source.getEntity()==entity)jem$caveBlockedBy=CaveQuestProof.actor(target);return outcome.result();
    }
    @Inject(method="m_8037_",at=@At("RETURN"))private void jem$nativeGrounded(CallbackInfo ci){
        if(jem$caveBlockedBy!=null && jem$caveGroundedBefore<=0 && entity.groundedFor>0 && !entity.isFlying())CaveQuestProof.emit(jem$caveBlockedBy,"v3_cave_cave_counterplay_vesper");jem$caveBlockedBy=null;
    }
}

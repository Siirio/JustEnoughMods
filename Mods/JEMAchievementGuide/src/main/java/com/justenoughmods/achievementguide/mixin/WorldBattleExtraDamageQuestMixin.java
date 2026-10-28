package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraProof;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class WorldBattleExtraDamageQuestMixin {
    @Unique private final java.util.ArrayDeque<Float> jem$nativeDamage=new java.util.ArrayDeque<>();
    @Unique private int jem$nativeDamageOverflow;
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void jem$begin(DamageSource source, float amount, CallbackInfo ci) { if(jem$nativeDamageOverflow>0||jem$nativeDamage.size()>=32){jem$nativeDamageOverflow++;return;}jem$nativeDamage.push(((LivingEntity)(Object)this).getHealth()); }
    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void jem$committed(DamageSource source, float amount, CallbackInfo ci) { if(jem$nativeDamageOverflow>0){jem$nativeDamageOverflow--;return;}var before=jem$nativeDamage.poll();if(before!=null)WorldBattleExtraProof.damage((LivingEntity)(Object)this,source,before); }
}

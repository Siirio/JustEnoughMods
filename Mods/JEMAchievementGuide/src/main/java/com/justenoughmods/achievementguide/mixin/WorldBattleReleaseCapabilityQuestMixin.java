package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraMovement;import net.minecraft.world.entity.LivingEntity;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets={"com.github.L_Ender.cataclysm.capabilities.ChargeCapability$ChargeCapabilityImp","com.github.L_Ender.cataclysm.capabilities.RenderRushCapability$RenderRushCapabilityImp"},remap=false) public abstract class WorldBattleReleaseCapabilityQuestMixin {
 @Inject(method="tick",at=@At("RETURN"),remap=false) private void jem$nativeEnd(LivingEntity actor,CallbackInfo ci){WorldBattleExtraMovement.nativeTick(actor,this);}
}

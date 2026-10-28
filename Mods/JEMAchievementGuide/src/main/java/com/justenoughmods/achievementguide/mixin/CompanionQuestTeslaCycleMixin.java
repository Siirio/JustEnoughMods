package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.CompanionQuestElectricity;
import dev.xylonity.companions.common.blockentity.AbstractTeslaBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AbstractTeslaBlockEntity.class,remap=false)
public abstract class CompanionQuestTeslaCycleMixin {
 @Inject(method="startCycle",at=@At("HEAD"))private void jem$newNativePulse(CallbackInfo ci){((AbstractTeslaBlockEntity)(Object)this).getPersistentData().remove(CompanionQuestElectricity.PULSE);}
}

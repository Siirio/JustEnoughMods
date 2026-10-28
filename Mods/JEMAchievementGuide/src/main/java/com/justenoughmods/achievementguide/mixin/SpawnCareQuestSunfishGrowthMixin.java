package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.mob.Sunfish;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.mob.Sunfish",remap=false)
public abstract class SpawnCareQuestSunfishGrowthMixin { 
 @Shadow private int lastAgeCategory;@Unique private int jem$category;
 @Inject(method="growUpIfNeeded",at=@At("HEAD"),remap=false) private void jem$before(CallbackInfo ci){jem$category=lastAgeCategory;}
 @Inject(method="growUpIfNeeded",at=@At("RETURN"),remap=false) private void jem$after(CallbackInfo ci){var self=(Sunfish)(Object)this;var proof=SpawnCareQuestProof.data(self);if(jem$category==-2&&lastAgeCategory==-1&&self.getSunfishAge()==-1&&self.isAlive()&&proof.hasUUID("wave_actor")&&proof.hasUUID("wave_parent"))SpawnCareQuestProof.mark(self,"wave_ready",proof.getUUID("wave_actor"));}
 }

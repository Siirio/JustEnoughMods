package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.mob.*;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.entity.Entity;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.mob.TunaEgg",remap=false)
public abstract class SpawnCareQuestTunaHatchMixin { @Unique private Tuna jem$young;
 @Inject(method="hatch",at=@At("HEAD"),remap=false) private void jem$before(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){jem$young=null;} 
 @Redirect(method="hatch",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z",remap=false),remap=false) private boolean jem$hatch(ServerLevel level,Entity child){boolean accepted=level.addFreshEntity(child);var self=(TunaEgg)(Object)this;var proof=SpawnCareQuestProof.data(self);if(accepted&&child instanceof Tuna tuna&&tuna.isBaby()&&tuna.isAlive()&&proof.hasUUID("wave_actor")&&proof.hasUUID("wave_parent")){jem$young=tuna;}return accepted;}
 @Inject(method="hatch",at=@At("RETURN"),remap=false) private void jem$after(ServerLevel level,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){var self=(TunaEgg)(Object)this;var proof=SpawnCareQuestProof.data(self);if(self.isRemoved()&&jem$young!=null&&jem$young.isAlive()&&level.getEntity(jem$young.getUUID())==jem$young){SpawnCareQuestProof.mark(jem$young,"wave_ready",proof.getUUID("wave_actor"));SpawnCareQuestProof.data(jem$young).putUUID("wave_egg",self.getUUID());}jem$young=null;}
 }

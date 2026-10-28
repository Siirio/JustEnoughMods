package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.mob.Tuna;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.item.ItemStack;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.mob.Tuna",remap=false)
public abstract class SpawnCareQuestTunaBreedMixin { 
 @Unique private SpawnCareQuestRun jem$run;
 @Inject(method="m_27563_",at=@At("HEAD"),remap=false) private void jem$before(ServerLevel level,net.minecraft.world.entity.animal.Animal partner,CallbackInfo ci){var self=(Tuna)(Object)this;var actor=SpawnCareQuestProof.real(self.getLoveCause()!=null?self.getLoveCause():partner.getLoveCause());jem$run=SpawnCareQuestRun.begin(self,actor,ItemStack.EMPTY);}
 @Inject(method="m_27563_",at=@At("RETURN"),remap=false) private void jem$after(ServerLevel level,net.minecraft.world.entity.animal.Animal partner,CallbackInfo ci){var run=jem$run;jem$run=null;SpawnCareQuestRun.end(run);if(run!=null)for(var child:run.created)if(SpawnCareQuestProof.type(child).equals("spawn:tuna_egg")&&child.isAlive()&&level.getEntity(child.getUUID())==child){SpawnCareQuestProof.mark(child,"wave_actor",run.actor.getUUID());SpawnCareQuestProof.data(child).putUUID("wave_parent",run.source.getUUID());}}
  }

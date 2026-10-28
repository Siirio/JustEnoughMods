package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.mob.Sunfish;import net.minecraft.server.level.ServerLevel;import net.minecraft.world.item.ItemStack;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.mob.Sunfish",remap=false)
public abstract class SpawnCareQuestSunfishBreedMixin { 
 @Unique private SpawnCareQuestRun jem$run;
 @Inject(method="spawnChildFromBreeding",at=@At("HEAD"),remap=false) private void jem$before(ServerLevel level,com.ninni.spawn.server.entity.mob.Sunfish partner,CallbackInfo ci){var self=(Sunfish)(Object)this;var actor=SpawnCareQuestProof.real(self.getLoveCause()!=null?self.getLoveCause():partner.getLoveCause());jem$run=SpawnCareQuestRun.begin(self,actor,ItemStack.EMPTY);}
 @Inject(method="spawnChildFromBreeding",at=@At("RETURN"),remap=false) private void jem$after(ServerLevel level,com.ninni.spawn.server.entity.mob.Sunfish partner,CallbackInfo ci){var run=jem$run;jem$run=null;SpawnCareQuestRun.end(run);if(run!=null)for(var child:run.created)if(SpawnCareQuestProof.type(child).equals("spawn:sunfish")&&child.isAlive()&&level.getEntity(child.getUUID())==child){SpawnCareQuestProof.mark(child,"wave_actor",run.actor.getUUID());SpawnCareQuestProof.data(child).putUUID("wave_parent",run.source.getUUID());}}
  }

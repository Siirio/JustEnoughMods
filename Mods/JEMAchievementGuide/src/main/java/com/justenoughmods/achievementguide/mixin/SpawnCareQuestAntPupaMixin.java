package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.item.context.UseOnContext;import net.minecraft.world.InteractionResult;import com.ninni.spawn.server.entity.mob.Ant;
@Pseudo @Mixin(targets="com.ninni.spawn.server.item.AntPupaItem",remap=false)
public abstract class SpawnCareQuestAntPupaMixin { 
 @Unique private SpawnCareQuestRun jem$run;
 @Inject(method="m_6225_",at=@At("HEAD"),remap=false) private void jem$before(UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){var actor=SpawnCareQuestProof.real(context.getPlayer());jem$run=actor==null?null:SpawnCareQuestRun.begin(actor,actor,context.getItemInHand());}
 @Inject(method="m_6225_",at=@At("RETURN"),remap=false) private void jem$after(UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){var run=jem$run;jem$run=null;SpawnCareQuestRun.end(run);if(run!=null&&ci.getReturnValue().consumesAction()&&SpawnCareQuestProof.debited(run.input,context.getItemInHand(),1))for(var child:run.created)if(child instanceof Ant ant&&ant.isAlive()&&ant.isBaby()&&ant.isOwnedBy(run.actor)&&run.actor.serverLevel().getEntity(ant.getUUID())==ant){SpawnCareQuestProof.mark(ant,"pupa_actor",run.actor.getUUID());SpawnCareQuestProof.emit(run.actor,"v3_pet_colony");}}
 }

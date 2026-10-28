package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import slexom.animal_feeding_trough.platform.common.block.entity.FeedingTroughBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="slexom.animal_feeding_trough.platform.common.goal.entity.ai.SelfFeedGoal",remap=false)
public abstract class SpawnCareQuestTroughFeedMixin {
 @Shadow protected Animal mob; @Shadow private FeedingTroughBlockEntity feeder;
 @Unique private FeedingTroughBlockEntity jem$feeder; @Unique private ItemStack jem$food; @Unique private boolean jem$love;
 @Inject(method="m_8037_",at=@At("HEAD"),remap=false) private void jem$before(CallbackInfo ci){jem$feeder=feeder;jem$food=feeder==null?ItemStack.EMPTY:feeder.getItems().get(0).copy();jem$love=mob.isInLove();}
 @Inject(method="m_8037_",at=@At("RETURN"),remap=false) private void jem$after(CallbackInfo ci){if(jem$feeder==null || !(mob.level() instanceof net.minecraft.server.level.ServerLevel level)||jem$love||!mob.isInLove()||!SpawnCareQuestProof.debited(jem$food,jem$feeder.getItems().get(0),1))return;var setup=SpawnCareQuestSetups.get(level).at(level,jem$feeder.getBlockPos());if(!setup.hasUUID("actor"))return;var actor=SpawnCareQuestProof.real(level.getServer().getPlayerList().getPlayer(setup.getUUID("actor")));if(actor!=null&&NativeFoodSources.get(level).consume(actor.getUUID(),NativeFoodSources.spent(jem$food,1),"spawn_care_trough",1))SpawnCareQuestProof.emit(actor,"v3_pet_open_table");}
}

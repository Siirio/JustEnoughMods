package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.base.AbstractSeaCow;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.Items;import net.minecraft.world.*;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.base.AbstractSeaCow",remap=false) public abstract class SpawnCareQuestSeaCowMilkMixin { 
 @Unique private SpawnCareQuestProof.Interaction jem$run;
 @Inject(method="m_6071_",at=@At("HEAD"),remap=false) private void jem$before(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$run=SpawnCareQuestProof.begin((AbstractSeaCow)(Object)this,player,hand);}
 @Inject(method="m_6071_",at=@At("RETURN"),remap=false) private void jem$after(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var run=jem$run;jem$run=null;var self=(AbstractSeaCow)(Object)this;if(run!=null&&!self.isBaby()&&self.isUnderWater()&&ci.getReturnValue().consumesAction()&&run.input().is(Items.BUCKET)&&SpawnCareQuestProof.countItem(run.inventory(),Items.BUCKET)-SpawnCareQuestProof.countItem(SpawnCareQuestProof.held(run.actor()),Items.BUCKET)==1&&SpawnCareQuestProof.countItem(SpawnCareQuestProof.held(run.actor()),Items.MILK_BUCKET)-SpawnCareQuestProof.countItem(run.inventory(),Items.MILK_BUCKET)==1)SpawnCareQuestProof.emit(run.actor(),"v3_pet_seagrass_pasture");}
 }

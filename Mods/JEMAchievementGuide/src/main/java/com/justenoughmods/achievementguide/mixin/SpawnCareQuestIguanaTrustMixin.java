package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.base.AbstractIguana;import net.minecraft.world.entity.player.Player;import net.minecraft.world.*;import net.minecraft.world.item.Items;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.base.AbstractIguana",remap=false)
public abstract class SpawnCareQuestIguanaTrustMixin { 
 @Unique private SpawnCareQuestProof.Interaction jem$run; @Unique private boolean jem$shed;
 @Inject(method="m_6071_",at=@At("HEAD"),remap=false) private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var self=(AbstractIguana)(Object)this;jem$run=SpawnCareQuestProof.begin(self,p,hand);jem$shed=self.isBaby()&&self.hasToShed()&&p.getItemInHand(hand).is(Items.BRUSH);}
 @Inject(method="m_6071_",at=@At("RETURN"),remap=false) private void jem$after(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var self=(AbstractIguana)(Object)this;var run=jem$run;jem$run=null;if(run!=null&&jem$shed&&ci.getReturnValue().consumesAction()&&!self.hasToShed()&&run.actor().getUUID().equals(self.getTrustedPlayerUUID())&&(p.getItemInHand(hand).isEmpty()||p.getItemInHand(hand).getDamageValue()>run.input().getDamageValue()))SpawnCareQuestProof.mark(self,"solar_actor",run.actor().getUUID());}
 }

package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value=dev.xylonity.companions.common.entity.hostile.BrokenDinamoEntity.class,remap=false)
public abstract class CompanionQuestRepairMixin {
 @Unique private int jem$state;@Unique private ItemStack jem$repairInput;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$state=((dev.xylonity.companions.common.entity.hostile.BrokenDinamoEntity)(Object)this).getState();jem$repairInput=user.getItemInHand(hand).copy();}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$paidStage(Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var broken=(dev.xylonity.companions.common.entity.hostile.BrokenDinamoEntity)(Object)this;var p=CompanionQuestProof.actor(user);var before=jem$repairInput;jem$repairInput=null;if(before==null || broken.getState()!=jem$state+1)return;var proof=broken.getPersistentData().getCompound("jemCompanionRepairV4");var now=user.getItemInHand(hand);
  boolean paid=(jem$state==2 || jem$state==6)?before.is(dev.xylonity.companions.registry.CompanionsItems.WRENCH.get()) && (now.getItem()==before.getItem() && now.getDamageValue()==before.getDamageValue()+1 || now.isEmpty() && before.getMaxDamage()-before.getDamageValue()==1):before.getCount()-(ItemStack.isSameItemSameTags(before,now)?now.getCount():0)>0;
  if(p==null || !paid || broken.getOwnerUUID()!=null && !p.getUUID().equals(broken.getOwnerUUID())){broken.getPersistentData().remove("jemCompanionRepairV4");return;}
  if(jem$state==0){proof=new CompoundTag();proof.putUUID("actor",p.getUUID());proof.putInt("mask",1);}else if(proof.hasUUID("actor") && proof.getUUID("actor").equals(p.getUUID()))proof.putInt("mask",proof.getInt("mask") | 1<<jem$state);else return;
  broken.getPersistentData().put("jemCompanionRepairV4",proof);
 }
 @Redirect(method="tameDinamo",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private boolean jem$nativeCompleted(Level level,Entity entity,Entity user){boolean result=level.addFreshEntity(entity);var broken=(dev.xylonity.companions.common.entity.hostile.BrokenDinamoEntity)(Object)this;var proof=broken.getPersistentData().getCompound("jemCompanionRepairV4");var p=CompanionQuestProof.actor(user);
  if(result && p!=null && proof.hasUUID("actor") && proof.getUUID("actor").equals(p.getUUID()) && proof.getInt("mask")==119 && broken.getState()==7 && entity instanceof dev.xylonity.companions.common.entity.companion.DinamoEntity d && CompanionQuestProof.owned(d,p)){d.getPersistentData().putUUID("jemCompanionRepairedV4",p.getUUID());CompanionQuestProof.emit(p,"v3_pet_current_flows");}return result;
 }
}

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

@Mixin(value=dev.xylonity.companions.common.entity.CompanionEntity.class,remap=false)
public abstract class CompanionQuestEssenceMixin {
 @Unique private ItemStack jem$essence;
 @Unique private float jem$health;
 @Inject(method="handleDefaultMainActionAndHeal",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<Boolean> ci){var pet=(dev.xylonity.companions.common.entity.CompanionEntity)(Object)this;jem$essence=p.getItemInHand(hand).copy();jem$health=pet.getHealth();}
 @Inject(method="handleDefaultMainActionAndHeal",at=@At("RETURN"))private void jem$after(Player user,InteractionHand hand,CallbackInfoReturnable<Boolean> ci){
  var pet=(dev.xylonity.companions.common.entity.CompanionEntity)(Object)this;var p=CompanionQuestProof.actor(user);var before=jem$essence;jem$essence=null;
  if(p!=null && before!=null && CompanionQuestProof.owned(pet,p) && jem$health<pet.getMaxHealth() && Float.isFinite(jem$health) && Float.isFinite(pet.getHealth()) && pet.getHealth()>jem$health && pet.getMainAction()==1
   && (before.is(dev.xylonity.knightlib.registry.KnightLibItems.SMALL_ESSENCE.get()) || before.is(dev.xylonity.knightlib.registry.KnightLibItems.GREAT_ESSENCE.get()))
   && before.getCount()-(ItemStack.isSameItemSameTags(before,p.getItemInHand(hand))?p.getItemInHand(hand).getCount():0)==1)CompanionQuestProof.emit(p,"v3_pet_essence_and_orders");
 }
}

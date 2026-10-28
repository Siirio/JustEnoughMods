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

@Mixin(value=dev.xylonity.companions.common.entity.companion.AntlionEntity.class,remap=false)
public abstract class CompanionQuestAntlionMixin {
 @Unique private int jem$variant;@Unique private ItemStack jem$hourglass;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$variant=((dev.xylonity.companions.common.entity.companion.AntlionEntity)(Object)this).getVariant();jem$hourglass=p.getItemInHand(hand).copy();}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$after(Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var pet=(dev.xylonity.companions.common.entity.companion.AntlionEntity)(Object)this;var p=CompanionQuestProof.actor(user);var before=jem$hourglass;jem$hourglass=null;var now=user.getItemInHand(hand);
  if(p!=null && before!=null && CompanionQuestProof.owned(pet,p) && before.is(dev.xylonity.companions.registry.CompanionsItems.HOURGLASS.get()) && before.getDamageValue()==0 && pet.getVariant()!=jem$variant
   && ((now.getItem()==before.getItem() && now.getDamageValue()>before.getDamageValue()) || now.isEmpty()))CompanionQuestProof.emit(p,"v3_pet_phases_and_faces");
 }
}

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

@Mixin(value=dev.xylonity.companions.common.entity.companion.MinionEntity.class,remap=false)
public abstract class CompanionQuestMinionLockMixin {
 @Unique private boolean jem$locked;@Unique private ItemStack jem$chains;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$locked=((dev.xylonity.companions.common.entity.companion.MinionEntity)(Object)this).isPhaseLocked();jem$chains=p.getItemInHand(hand).copy();}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$after(Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var pet=(dev.xylonity.companions.common.entity.companion.MinionEntity)(Object)this;var p=CompanionQuestProof.actor(user);var before=jem$chains;jem$chains=null;
  if(p!=null && before!=null && !jem$locked && pet.isPhaseLocked() && CompanionQuestProof.owned(pet,p) && before.is(dev.xylonity.companions.registry.CompanionsItems.NETHERITE_CHAINS.get()) && before.getCount()-user.getItemInHand(hand).getCount()==1)CompanionQuestProof.emit(p,"v3_pet_phases_and_faces_lock");
 }
}

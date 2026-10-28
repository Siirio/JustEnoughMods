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

@Mixin(value=dev.xylonity.companions.common.entity.companion.CroissantDragonEntity.class,remap=false)
public abstract class CompanionQuestDragonMixin {
 @Unique private ItemStack jem$dragonInput;@Unique private int jem$milk;@Unique private boolean jem$wasTame;@Unique private String jem$armor;
 @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player p,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var d=(dev.xylonity.companions.common.entity.companion.CroissantDragonEntity)(Object)this;jem$dragonInput=p.getItemInHand(hand).copy();jem$milk=d.getMilkAmount();jem$wasTame=d.isTame();jem$armor=d.getArmorName();}
 @Inject(method="m_6071_",at=@At("RETURN"))private void jem$after(Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var d=(dev.xylonity.companions.common.entity.companion.CroissantDragonEntity)(Object)this;var p=CompanionQuestProof.actor(user);var input=jem$dragonInput;jem$dragonInput=null;var proof=d.getPersistentData();
  if(p==null || input==null || !CompanionQuestProof.owned(d,p) || !proof.hasUUID("jemCompanionHatchActorV4") || !proof.getUUID("jemCompanionHatchActorV4").equals(p.getUUID()) || !proof.hasUUID("jemCompanionHatchUUIDV4") || !proof.getUUID("jemCompanionHatchUUIDV4").equals(d.getUUID()))return;
  int remaining=ItemStack.isSameItemSameTags(input,user.getItemInHand(hand))?user.getItemInHand(hand).getCount():0;
  if(!jem$wasTame && input.is(Items.MILK_BUCKET) && input.getCount()-remaining==1 && d.getMilkAmount()>jem$milk)proof.putUUID("jemCompanionMilkActorV4",p.getUUID());
  if(input.getItem() instanceof dev.xylonity.companions.common.item.CroissantDragonArmor && input.getCount()-remaining==1 && !d.getArmorName().equals(jem$armor))proof.putUUID("jemCompanionArmorActorV4",p.getUUID());
 }
 @Inject(method="m_8119_",at=@At("RETURN"))private void jem$mature(CallbackInfo ci){var d=(dev.xylonity.companions.common.entity.companion.CroissantDragonEntity)(Object)this;var p=CompanionQuestProof.actor(d.getOwner());var proof=d.getPersistentData();if(p!=null && d.getMilkAmount()>=3 && !d.getArmorName().equals("default") && proof.hasUUID("jemCompanionMilkActorV4") && proof.getUUID("jemCompanionMilkActorV4").equals(p.getUUID()) && proof.hasUUID("jemCompanionArmorActorV4") && proof.getUUID("jemCompanionArmorActorV4").equals(p.getUUID())){CompanionQuestProof.emit(p,"v3_pet_bread_hatch");proof.remove("jemCompanionArmorActorV4");}}
}

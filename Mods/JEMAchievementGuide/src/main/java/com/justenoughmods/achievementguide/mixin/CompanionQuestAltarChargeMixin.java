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

@Mixin(value=dev.xylonity.companions.common.item.CrystallizedBloodItem.class,remap=false)
public abstract class CompanionQuestAltarChargeMixin {
 @Unique private static final ThreadLocal<CompoundTag> jem$charge=new ThreadLocal<>();
 @Inject(method="m_6225_",at=@At("HEAD"))private void jem$before(net.minecraft.world.item.context.UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){jem$charge.remove();if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof dev.xylonity.companions.common.blockentity.AbstractShadeAltarBlockEntity altar){var frame=altar.getPersistentData().getCompound(CompanionQuestShade.CHARGE).copy();frame.putInt("before",altar.getCharges());frame.putInt("input",context.getItemInHand().getCount());jem$charge.set(frame);}}
 @Inject(method="m_6225_",at=@At("RETURN"))private void jem$after(net.minecraft.world.item.context.UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){var frame=jem$charge.get();jem$charge.remove();if(frame==null || !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof dev.xylonity.companions.common.blockentity.AbstractShadeAltarBlockEntity altar) || altar.getCharges()!=frame.getInt("before")+1)return;
  altar.getPersistentData().remove(CompanionQuestShade.CHARGE);var p=CompanionQuestProof.actor(context.getPlayer());if(p==null || frame.getInt("input")-context.getItemInHand().getCount()!=1)return;
  CompoundTag proof=null;if(frame.getInt("before")==0){proof=new CompoundTag();proof.putUUID("actor",p.getUUID());proof.putUUID("generation",java.util.UUID.randomUUID());}
  else if(frame.hasUUID("actor") && frame.getUUID("actor").equals(p.getUUID()) && frame.hasUUID("generation") && frame.getInt("charges")==frame.getInt("before"))proof=frame;
  if(proof!=null){proof.remove("before");proof.remove("input");proof.putInt("charges",altar.getCharges());altar.getPersistentData().put(CompanionQuestShade.CHARGE,proof);altar.setChanged();}
 }
}

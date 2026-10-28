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

@Mixin(value=dev.xylonity.companions.common.item.ShadowBellItem.class,remap=false)
public abstract class CompanionQuestBellMixin {
 @Unique private static final ThreadLocal<CompoundTag> jem$bellCharge=new ThreadLocal<>();
 @Unique private static final ThreadLocal<dev.xylonity.companions.common.blockentity.AbstractShadeAltarBlockEntity> jem$bellAltar=new ThreadLocal<>();
 @Inject(method="m_6225_",at=@At("RETURN"))private void jem$linked(net.minecraft.world.item.context.UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){var p=CompanionQuestProof.actor(context.getPlayer());if(p==null)return;var altar=CompanionQuestShade.resolve(p,context.getItemInHand());if(altar!=null && altar.getBlockPos().equals(context.getClickedPos()) && CompanionQuestShade.charge(altar,p,altar.getPersistentData().getCompound(CompanionQuestShade.CHARGE))){var charge=altar.getPersistentData().getCompound(CompanionQuestShade.CHARGE);var payload=new CompoundTag();payload.putLong("altar",altar.getBlockPos().asLong());payload.putString("dimension",p.level().dimension().location().toString());CompanionQuestStack.put(context.getItemInHand(),p.getUUID(),charge.getUUID("generation"),"bell",payload);}}
 @Inject(method="m_7203_",at=@At("HEAD"))private void jem$before(Level world,Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){jem$bellCharge.remove();jem$bellAltar.remove();var p=CompanionQuestProof.actor(user);if(p==null)return;var stack=p.getItemInHand(hand);var proof=CompanionQuestStack.get(stack);var altar=CompanionQuestShade.resolve(p,stack);
  if(altar!=null && !proof.isEmpty() && proof.getString("kind").equals("bell") && proof.getUUID("actor").equals(p.getUUID()) && proof.getCompound("payload").getString("dimension").equals(p.level().dimension().location().toString()) && proof.getCompound("payload").getLong("altar")==altar.getBlockPos().asLong()){
   var charge=altar.getPersistentData().getCompound(CompanionQuestShade.CHARGE).copy();if(CompanionQuestShade.charge(altar,p,charge) && proof.getUUID("source").equals(charge.getUUID("generation"))){charge.putInt("before",altar.getCharges());if(altar.activeShadeUUID!=null)charge.putUUID("old",altar.activeShadeUUID);jem$bellCharge.set(charge);jem$bellAltar.set(altar);}}
 }
 @Inject(method="m_7203_",at=@At("RETURN"))private void jem$after(Level world,Player user,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci){var charge=jem$bellCharge.get();var altar=jem$bellAltar.get();jem$bellCharge.remove();jem$bellAltar.remove();var p=CompanionQuestProof.actor(user);if(p==null || altar==null || charge==null || altar.getCharges()!=charge.getInt("before")-1 || altar.activeShadeUUID==null || charge.hasUUID("old") && charge.getUUID("old").equals(altar.activeShadeUUID))return;
  var bell=p.getItemInHand(hand);if(bell.hasTag() && bell.getTag().hasUUID("cached_shade") && bell.getTag().getUUID("cached_shade").equals(altar.activeShadeUUID) && p.serverLevel().getEntity(altar.activeShadeUUID) instanceof dev.xylonity.companions.common.entity.ShadeEntity shade && shade.isAlive() && CompanionQuestProof.owned(shade,p) && CompanionQuestShade.matching(altar,shade)){
   CompanionQuestProof.emit(p,"v3_pet_shade_call");if(altar.getCharges()>0){charge.remove("before");charge.remove("old");charge.putInt("charges",altar.getCharges());altar.getPersistentData().put(CompanionQuestShade.CHARGE,charge);altar.setChanged();}}
 }
}

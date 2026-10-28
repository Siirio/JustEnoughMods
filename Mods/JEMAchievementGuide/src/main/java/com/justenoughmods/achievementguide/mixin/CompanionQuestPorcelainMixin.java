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

@Mixin(value=dev.xylonity.companions.common.item.HolyPorcelainPottery.class,remap=false)
public abstract class CompanionQuestPorcelainMixin {
 @Unique private java.util.UUID jem$capturedUUID;@Unique private boolean jem$eligibleCapture;
 @Unique private CompoundTag jem$releaseProof,jem$releaseNBT;@Unique private Entity jem$released;
 @Inject(method="m_6880_",at=@At("HEAD"))private void jem$beforeCapture(ItemStack stack,Player user,LivingEntity target,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var p=CompanionQuestProof.actor(user);jem$capturedUUID=target.getUUID();jem$eligibleCapture=p!=null && target.isAlive() && !(target instanceof Player) && !(target instanceof net.minecraft.world.entity.monster.Enemy) && target.getType().getCategory()!=MobCategory.MONSTER && (!(target instanceof OwnableEntity owned) || owned.getOwnerUUID()==null || owned.getOwnerUUID().equals(p.getUUID())) && !dev.xylonity.companions.common.item.HolyPorcelainPottery.hasStoredEntity(stack);}
 @Inject(method="m_6880_",at=@At("RETURN"))private void jem$captured(ItemStack stack,Player user,LivingEntity target,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){var p=CompanionQuestProof.actor(user);var stored=user.getItemInHand(hand);var tag=stored.hasTag()?stored.getTag().getCompound("stored_entity"):new CompoundTag();
  if(p!=null && jem$eligibleCapture && target.isRemoved() && ci.getReturnValue().consumesAction() && tag.hasUUID("UUID") && tag.getUUID("UUID").equals(jem$capturedUUID))CompanionQuestStack.put(stored,p.getUUID(),jem$capturedUUID,"porcelain",tag);jem$capturedUUID=null;jem$eligibleCapture=false;
 }
 @Inject(method="m_6225_",at=@At("HEAD"))private void jem$beforeRelease(net.minecraft.world.item.context.UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){jem$releaseProof=CompanionQuestStack.get(context.getItemInHand());jem$releaseNBT=context.getItemInHand().hasTag()?context.getItemInHand().getTag().getCompound("stored_entity").copy():new CompoundTag();jem$released=null;}
 @Redirect(method="m_6225_",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private boolean jem$nativeRelease(ServerLevel level,Entity entity){boolean result=level.addFreshEntity(entity);if(result)jem$released=entity;return result;}
 @Inject(method="m_6225_",at=@At("RETURN"))private void jem$released(net.minecraft.world.item.context.UseOnContext context,CallbackInfoReturnable<InteractionResult> ci){
  var p=CompanionQuestProof.actor(context.getPlayer());var proof=jem$releaseProof;var entity=jem$released;
  if(p!=null && proof!=null && !proof.isEmpty() && proof.getString("kind").equals("porcelain") && proof.getUUID("actor").equals(p.getUUID()) && entity!=null && entity.getUUID().equals(proof.getUUID("source")) && proof.getCompound("payload").equals(jem$releaseNBT)
   && p.serverLevel().getEntity(entity.getUUID())==entity && entity.isAlive() && p.serverLevel().noCollision(entity) && !dev.xylonity.companions.common.item.HolyPorcelainPottery.hasStoredEntity(context.getItemInHand()))CompanionQuestProof.emit(p,"v3_pet_porcelain_trip");
  jem$releaseProof=null;jem$releaseNBT=null;jem$released=null;
 }
}

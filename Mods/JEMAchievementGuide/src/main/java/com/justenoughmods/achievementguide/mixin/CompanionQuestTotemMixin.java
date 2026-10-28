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

@Mixin(value=dev.xylonity.companions.common.block.RespawnTotemBlock.class,remap=false)
public abstract class CompanionQuestTotemMixin {
 @Unique private static final ThreadLocal<ItemStack> jem$totemMaterial=new ThreadLocal<>();
 @Inject(method="m_6227_",at=@At("HEAD"))private void jem$before(BlockState state,Level level,BlockPos pos,Player user,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){jem$totemMaterial.set(user.getItemInHand(hand).copy());}
 @Redirect(method="m_6227_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private boolean jem$nativeSummon(Level level,Entity entity,BlockState state,Level original,BlockPos pos,Player user,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
  boolean result=level.addFreshEntity(entity);var p=CompanionQuestProof.actor(user);var material=jem$totemMaterial.get();
  if(result && p!=null && material!=null && material.getCount()-user.getItemInHand(hand).getCount()==1 && entity instanceof dev.xylonity.companions.common.entity.CompanionEntity pet && CompanionQuestProof.owned(pet,p)){
   if(material.is(dev.xylonity.companions.registry.CompanionsItems.OLD_CLOTH.get()) && entity instanceof dev.xylonity.companions.common.entity.companion.CloakEntity)CompanionQuestProof.emit(p,"v3_pet_totem_shapes");
   if(material.is(dev.xylonity.companions.registry.CompanionsItems.RELIC_GOLD.get()) && entity instanceof dev.xylonity.companions.common.entity.companion.MankhEntity)CompanionQuestProof.emit(p,"v3_pet_totem_shapes_mankh");
  }return result;
 }
 @Inject(method="m_6227_",at=@At("RETURN"))private void jem$after(BlockState state,Level level,BlockPos pos,Player user,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){jem$totemMaterial.remove();}
}

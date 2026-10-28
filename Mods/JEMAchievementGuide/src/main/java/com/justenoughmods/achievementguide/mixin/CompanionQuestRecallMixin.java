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

@Mixin(value=dev.xylonity.companions.common.blockentity.RecallPlatformBlockEntity.class,remap=false)
public abstract class CompanionQuestRecallMixin {
 @Redirect(method="onStepped",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;m_6021_(DDD)V"))private void jem$nativeTransport(ServerPlayer user,double x,double y,double z){
  var source=(dev.xylonity.companions.common.blockentity.RecallPlatformBlockEntity)(Object)this;var p=CompanionQuestProof.actor(user);var from=user.position();var target=BlockPos.containing(x,y-1,z);
  boolean valid=p!=null && source.getLevel()==p.level() && source.isActive() && CompanionQuestElectricity.powered(source) && !target.equals(source.getBlockPos()) && CompanionQuestProof.loaded(p.serverLevel(),target) && p.level().getBlockEntity(target) instanceof dev.xylonity.companions.common.blockentity.RecallPlatformBlockEntity other && other.getDistance()!=Integer.MAX_VALUE;
  user.teleportTo(x,y,z);
  if(valid && user.position().distanceToSqr(from)>1.0e-8 && user.position().distanceToSqr(new net.minecraft.world.phys.Vec3(x,y,z))<1.0e-6)CompanionQuestProof.emit(p,"v3_pet_recall_trip");
 }
}

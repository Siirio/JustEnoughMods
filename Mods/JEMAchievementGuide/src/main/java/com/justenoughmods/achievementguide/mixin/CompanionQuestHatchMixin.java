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

@Mixin(value=dev.xylonity.companions.common.blockentity.CroissantEggBlockEntity.class,remap=false)
public abstract class CompanionQuestHatchMixin {
 @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private static boolean jem$nativeHatch(Level level,Entity entity,Level original,BlockPos pos,BlockState state,net.minecraft.world.level.block.entity.BlockEntity owner){
  boolean result=level.addFreshEntity(entity);var proof=owner.getPersistentData();if(result && level instanceof ServerLevel world && entity instanceof dev.xylonity.companions.common.entity.companion.CroissantDragonEntity dragon && !world.getBlockState(pos).is(dev.xylonity.companions.registry.CompanionsBlocks.CROISSANT_EGG.get()) && proof.hasUUID("jemCompanionEggActorV4")){dragon.getPersistentData().putUUID("jemCompanionHatchActorV4",proof.getUUID("jemCompanionEggActorV4"));dragon.getPersistentData().putUUID("jemCompanionHatchUUIDV4",dragon.getUUID());}return result;
 }
}

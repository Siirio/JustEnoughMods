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

@Mixin(value=dev.xylonity.companions.common.entity.projectile.BlackHoleProjectile.class,remap=false)
public abstract class CompanionQuestBlackHoleMixin {
 @Redirect(method="attractNearbyEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"))private void jem$nativePull(LivingEntity target,net.minecraft.world.phys.Vec3 delta){var before=target.getDeltaMovement();target.setDeltaMovement(delta);if(Double.isFinite(delta.lengthSqr()) && target.getDeltaMovement().distanceToSqr(before)>1.0e-8)CompanionQuestSpell.combat((Entity)(Object)this,target);}
}

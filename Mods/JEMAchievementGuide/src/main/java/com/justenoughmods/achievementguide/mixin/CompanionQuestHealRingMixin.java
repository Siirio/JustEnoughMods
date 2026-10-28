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

@Mixin(value=dev.xylonity.companions.common.entity.projectile.HealRingProjectile.class,remap=false)
public abstract class CompanionQuestHealRingMixin {
 @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_5634_(F)V"))private void jem$actualHealing(LivingEntity target,float amount){float before=target.getHealth();target.heal(amount);if(Float.isFinite(amount) && amount>0 && Float.isFinite(before) && Float.isFinite(target.getHealth()) && target.getHealth()>before)CompanionQuestSpell.support((Entity)(Object)this,target);}
}

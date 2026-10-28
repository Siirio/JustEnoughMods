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

@Mixin(value=dev.xylonity.companions.common.entity.ai.puppet.AbstractPuppetRightAttackGoal.class,remap=false)
public abstract class CompanionQuestPuppetRightMixin {
 @Shadow @Final protected dev.xylonity.companions.common.entity.companion.PuppetEntity puppet;
 @Shadow protected abstract void performAttack(LivingEntity target);
 @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Ldev/xylonity/companions/common/entity/ai/puppet/AbstractPuppetRightAttackGoal;performAttack(Lnet/minecraft/world/entity/LivingEntity;)V"))private void jem$nativeArm(dev.xylonity.companions.common.entity.ai.puppet.AbstractPuppetRightAttackGoal goal,LivingEntity target){CompanionQuestPuppetRun.observe(puppet,target,0,()->performAttack(target));}
}

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

@Mixin(value=dev.xylonity.companions.common.entity.projectile.FireMarkRingProjectile.class,remap=false)
public abstract class CompanionQuestFireMarkMixin {
 @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))private boolean jem$nativeMark(LivingEntity target,net.minecraft.world.effect.MobEffectInstance effect){var before=target.getEffect(effect.getEffect());boolean result=target.addEffect(effect);if(result && before==null && target.hasEffect(effect.getEffect()))CompanionQuestSpell.combat((Entity)(Object)this,target);return result;}
}

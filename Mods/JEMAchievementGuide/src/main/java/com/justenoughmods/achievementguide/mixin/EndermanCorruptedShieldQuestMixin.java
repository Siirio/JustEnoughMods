package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.EndermanPearlProof;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Applied after the upstream priority1000 TAIL observer, so completion follows its actual native teleport. */
@Mixin(value = LivingEntity.class, priority = 900)
public abstract class EndermanCorruptedShieldQuestMixin {
    @Inject(method = "blockUsingShield", at = @At("HEAD"))
    private void jem$begin(LivingEntity attacker, CallbackInfo callback) { EndermanPearlProof.beginShield((LivingEntity)(Object)this, attacker); }
    @Inject(method = "blockUsingShield", at = @At("RETURN"))
    private void jem$end(LivingEntity attacker, CallbackInfo callback) { EndermanPearlProof.end(true); }
}

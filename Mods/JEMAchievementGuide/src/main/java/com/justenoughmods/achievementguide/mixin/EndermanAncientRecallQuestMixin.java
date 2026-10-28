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
@Pseudo
@Mixin(targets = "tech.alexnijjar.endermanoverhaul.common.entities.pets.BasePetEnderman", remap = false)
public abstract class EndermanAncientRecallQuestMixin {
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$begin(Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EndermanPearlProof.beginRecall((Entity)(Object)this, actor);
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$end(Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EndermanPearlProof.end(callback.getReturnValue().consumesAction());
    }
}

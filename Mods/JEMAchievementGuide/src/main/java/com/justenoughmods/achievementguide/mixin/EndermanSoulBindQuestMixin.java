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
@Mixin(targets = "tech.alexnijjar.endermanoverhaul.common.items.pearls.SoulPearlItem", remap = false)
public abstract class EndermanSoulBindQuestMixin {
    @Inject(method = "m_6880_", at = @At("HEAD"))
    private void jem$begin(ItemStack stack, Player actor, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EndermanPearlProof.begin("bind", actor, target, stack);
    }
    @Redirect(method = "m_6880_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_21008_(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void jem$hand(Player actor, InteractionHand hand, ItemStack result) {
        actor.setItemInHand(hand, result);
        EndermanPearlProof.delivered(actor.getItemInHand(hand), true);
    }
    @Redirect(method = "m_6880_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;m_36054_(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$inventory(Inventory inventory, ItemStack result) {
        ItemStack before = result.copy();
        boolean accepted = inventory.add(result);
        EndermanPearlProof.delivered(before, accepted && result.getCount() < before.getCount());
        return accepted;
    }
    @Redirect(method = "m_6880_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_36176_(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$drop(Player actor, ItemStack result, boolean random) {
        ItemEntity dropped = actor.drop(result, random);
        EndermanPearlProof.delivered(result, dropped != null && !dropped.isRemoved() && actor.level().getEntity(dropped.getId()) == dropped);
        return dropped;
    }
    @Inject(method = "m_6880_", at = @At("RETURN"))
    private void jem$end(ItemStack stack, Player actor, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        EndermanPearlProof.end(callback.getReturnValue().consumesAction());
    }
}

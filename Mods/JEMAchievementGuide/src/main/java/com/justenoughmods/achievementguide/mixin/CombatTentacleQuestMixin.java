package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.combattools.CombatTentacleProof;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.unusual.block_factorys_bosses.item.UndyingTentacleItem", remap = false)
public abstract class CombatTentacleQuestMixin {
    @Inject(method = "m_7203_", at = @At("HEAD"))
    private void jem$begin(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        CombatTentacleProof.begin(actor, actor.getItemInHand(hand));
    }
    @Redirect(method = "m_7203_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41622_(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
    private void jem$cost(ItemStack tool, int amount, LivingEntity user, Consumer<LivingEntity> broken) {
        tool.hurtAndBreak(amount, user, broken); CombatTentacleProof.paid(tool);
    }
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void jem$pull(Entity target, Vec3 velocity) {
        Vec3 before = target.getDeltaMovement(); target.setDeltaMovement(velocity); CombatTentacleProof.force(target, before, velocity);
    }
    @Inject(method = "m_7203_", at = @At("RETURN"))
    private void jem$end(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        CombatTentacleProof.end(callback.getReturnValue().getResult().consumesAction());
    }
}

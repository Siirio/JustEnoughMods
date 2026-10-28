package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.primal.server_data.ConchShellsData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.item.ConchShellItem", remap = false)
public abstract class CreatureCareConchBindingMixin {
    @Unique private final ThreadLocal<Boolean> jem$unbound = new ThreadLocal<>();
    @Inject(method = "m_6880_", at = @At("HEAD"))
    private void jem$before(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$unbound.set(stack.getTag() == null || !stack.getTag().contains("ConchShell"));
    }
    @Inject(method = "m_6880_", at = @At("RETURN"))
    private void jem$bound(ItemStack stack, Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        boolean unbound = Boolean.TRUE.equals(jem$unbound.get()); jem$unbound.remove(); var actor = CreatureCareProof.real(player);
        if (!unbound || actor == null || !callback.getReturnValue().consumesAction() || stack.getTag() == null) return;
        var binding = stack.getTag().getCompound("ConchShell");
        if (target instanceof OwnableEntity pet && actor.getUUID().equals(pet.getOwnerUUID()) && pet.getOwner() == actor
                && binding.hasUUID("pet") && binding.getUUID("pet").equals(target.getUUID())
                && actor.serverLevel().getEntity(target.getUUID()) == target) {
            var data = ConchShellsData.get(actor.serverLevel());
            if (data.hasPet(target) && data.getDimension(target.getUUID()) != null && data.getDimension(target.getUUID()).equals(target.level().dimension())
                    && data.getPos(target.getUUID()) != null) CreatureCareProof.emit(actor, "v3_fauna_walrus_signal_bound");
        }
    }
}

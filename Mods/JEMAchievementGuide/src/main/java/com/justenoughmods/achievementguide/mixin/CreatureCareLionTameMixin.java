package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.primal.entity.animal.LionEntity", remap = false)
public abstract class CreatureCareLionTameMixin {
    @Unique private ItemStack jem$food = ItemStack.EMPTY;
    @Unique private boolean jem$wild;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        TamableAnimal self = (TamableAnimal)(Object)this;
        jem$food = player.getItemInHand(hand).copy(); jem$wild = !self.isTame();
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$accepted(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        TamableAnimal self = (TamableAnimal)(Object)this;
        ServerPlayer actor = CreatureCareProof.real(player);
        if (jem$wild && actor != null && !jem$food.isEmpty() && org.primal.entity.animal.LionEntity.isTameFood(jem$food)
                && CreatureCareProof.owned(self, actor) && callback.getReturnValue().consumesAction()
                && CreatureCareProof.debited(jem$food, player.getItemInHand(hand))) {
            CreatureCareProof.mark(self, "lion_meat_owner", actor.getUUID());
        }
        jem$food = ItemStack.EMPTY;
    }
}

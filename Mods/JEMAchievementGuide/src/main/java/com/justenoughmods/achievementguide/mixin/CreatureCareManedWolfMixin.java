package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityManedWolf", remap = false)
public abstract class CreatureCareManedWolfMixin {
    @Shadow public abstract int getShakingTime();
    @Unique private ItemStack jem$apple = ItemStack.EMPTY;
    @Unique private boolean jem$quiet;
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$apple = player.getItemInHand(hand).copy(); jem$quiet = getShakingTime() == 0;
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$scent(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var actor = CreatureCareProof.real(player); Mob self = (Mob)(Object)this;
        if (actor != null && jem$quiet && jem$apple.is(Items.APPLE) && getShakingTime() > 0
                && callback.getReturnValue().consumesAction() && CreatureCareProof.debited(jem$apple, player.getItemInHand(hand))) {
            CreatureCareProof.mark(self, "wolf_scent_actor", actor.getUUID());
            CreatureCareProof.data(self).putLong("wolf_scent_until", self.level().getGameTime() + getShakingTime());
        }
        jem$apple = ItemStack.EMPTY;
    }
    @Redirect(method = "attractAnimals", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;m_26519_(DDDD)Z"))
    private boolean jem$attracted(PathNavigation navigation, double x, double y, double z, double speed) {
        BlockPos before = navigation.getTargetPos(); var oldPath = navigation.getPath();
        boolean accepted = navigation.moveTo(x, y, z, speed);
        Mob self = (Mob)(Object)this; var actor = CreatureCareProof.actor(self, "wolf_scent_actor");
        Mob animal = ((CreatureCareNavigationAccess)navigation).jem$careNavigatingMob();
        if (accepted && actor != null && getShakingTime() > 0 && animal instanceof Animal
                && CreatureCareProof.data(self).getLong("wolf_scent_until") >= self.level().getGameTime()
                && navigation.getPath() != null && (navigation.getPath() != oldPath || !navigation.getTargetPos().equals(before))
                && self.distanceToSqr(x, y, z) < self.distanceToSqr(animal)) {
            CreatureCareProof.emit(actor, "v3_fauna_woodland_care");
        }
        return accepted;
    }
}

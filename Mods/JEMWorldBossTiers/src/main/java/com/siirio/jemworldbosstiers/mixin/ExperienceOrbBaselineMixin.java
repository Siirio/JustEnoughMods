package com.siirio.jemworldbosstiers.mixin;

import com.siirio.jemworldbosstiers.api.ExperienceRewardApi;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbBaselineMixin {
    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/world/entity/player/Player;I)I"))
    private int jem$baselineBeforeMending(ExperienceOrb orb, Player player, int experience) {
        return ExperienceRewardApi.repair(player, experience);
    }

    @Redirect(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void jem$transferRemainder(Player player, int experience) {
        ExperienceRewardApi.transfer(player, experience);
    }
}

package com.siirio.jemsettlements.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathNavigation.class)
public abstract class ResidentNavigationMixin {
    @Shadow @Final protected Mob mob;
    @Inject(method = "moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z", at = @At("HEAD"), cancellable = true)
    private void jem$territory(Path path, double speed, CallbackInfoReturnable<Boolean> callback) {
        if (mob instanceof Villager villager && !com.siirio.jemsettlements.runtime.ResidentTerritoryTravel.permits(villager, path))
            callback.setReturnValue(false);
    }
}

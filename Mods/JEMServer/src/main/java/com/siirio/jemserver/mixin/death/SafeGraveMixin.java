package com.siirio.jemserver.mixin.death;

import com.siirio.jemserver.smp.events.BloodMoonDeaths;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "de.maxhenkel.gravestone.events.DeathEvents", remap = false)
public abstract class SafeGraveMixin {
    @Inject(method = "keepInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private static void jem$safeDeath(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (BloodMoonDeaths.keeps(player)) cir.setReturnValue(true);
        else if (BloodMoonDeaths.ordinaryDeath(player)) cir.setReturnValue(false);
    }
}

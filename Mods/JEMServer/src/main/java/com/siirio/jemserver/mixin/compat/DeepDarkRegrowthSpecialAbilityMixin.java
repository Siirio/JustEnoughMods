package com.siirio.jemserver.mixin.compat;

import com.siirio.jemserver.smp.compat.PhantomArmorAbility;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.deepdarkregrowth.network.SpecialAbilityMessage", remap = false)
public abstract class DeepDarkRegrowthSpecialAbilityMixin {
    @Inject(method = "pressAction", at = @At("HEAD"), cancellable = true)
    private static void jemPhantomArmor(Player player, int type, int pressedMs, CallbackInfo callback) {
        if (PhantomArmorAbility.handle(player, type)) callback.cancel();
    }
}

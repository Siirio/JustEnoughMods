package com.siirio.jemcompat.mixin.client.barnyard;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Barnyard 0.6..4 must not send its Clydesdale sprint packet before login. */
@Pseudo
@Mixin(targets = "net.mcreator.barnyardbuddies.init.BarnyardbuddiesModKeyMappings$2", remap = false)
public abstract class BarnyardClydesdaleSprintKeyMixin {
    @Inject(method = "m_7249_(Z)V", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void jem$ignoreBeforeClientConnection(boolean down, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) {
            callback.cancel();
        }
    }
}

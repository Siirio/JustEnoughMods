package com.siirio.jemfreecam.mixin;

import com.siirio.jemfreecam.client.FreeCameraController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Epic Fight polls combat and skill keys outside Forge's interaction event. */
@Pseudo
@Mixin(targets = "yesman.epicfight.client.events.engine.ControlEngine", remap = false)
public abstract class FreeCameraEpicFightInputMixin {
    @Inject(method = "handleEpicFightKeyMappings", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$noBodySkillKeys(CallbackInfo ci) {
        if (!FreeCameraController.isActive()) return;
        // Presses collected while viewing must not be replayed onto the body on exit.
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (!mapping.getName().startsWith("key.epicfight.")) continue;
            while (mapping.consumeClick()) { }
        }
        ci.cancel();
    }

    @Inject(method = "inputTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void jemcompat$noBodyMovementSkill(Input input, CallbackInfo ci) {
        if (FreeCameraController.isActive()) ci.cancel();
    }
}

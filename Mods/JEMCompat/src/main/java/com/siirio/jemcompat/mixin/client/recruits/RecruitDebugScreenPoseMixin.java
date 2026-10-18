package com.siirio.jemcompat.mixin.client.recruits;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recruits 1.15.2 labels must leave the container renderer's pose frame intact. */
@Pseudo
@Mixin(targets = "com.talhanation.recruits.client.gui.DebugInvScreen", remap = false)
public abstract class RecruitDebugScreenPoseMixin {
    @Inject(method = "m_280003_(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;m_85841_(FFF)V"),
            require = 1)
    private void jem$beginNativeLabelPose(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo callback) {
        // The native 0.7 label scale and trailing pop belong to this missing local frame.
        graphics.pose().pushPose();
    }
}

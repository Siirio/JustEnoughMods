package com.siirio.jemsecurity.mixin;

import com.siirio.jemsecurity.client.ResourcePackGuard;
import com.siirio.jemsecurity.network.SecurityClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {
    @Shadow @Final private PackSelectionModel model;
    @Shadow private Button doneButton;
    @Unique private String jemSecurity$blockedPack;
    @Unique private String jemSecurity$reportedPack;

    protected PackSelectionScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "reload", at = @At("HEAD"))
    private void jemSecurity$clearInspectionCache(CallbackInfo callback) {
        ResourcePackGuard.clearCache();
    }

    @Inject(method = "populateLists", at = @At("TAIL"))
    private void jemSecurity$validateSelection(CallbackInfo callback) {
        jemSecurity$blockedPack = ResourcePackGuard.findForbidden(model.getSelected());
        doneButton.active = jemSecurity$blockedPack == null;
        if (jemSecurity$blockedPack == null) {
            jemSecurity$reportedPack = null;
        } else if (!jemSecurity$blockedPack.equals(jemSecurity$reportedPack)) {
            jemSecurity$reportedPack = jemSecurity$blockedPack;
            SecurityClient.reportBlockedPackAttempt(jemSecurity$blockedPack);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void jemSecurity$preventForbiddenCommit(CallbackInfo callback) {
        jemSecurity$blockedPack = ResourcePackGuard.findForbidden(model.getSelected());
        if (jemSecurity$blockedPack != null) callback.cancel();
    }

    @Inject(method = "onClose", at = @At("TAIL"))
    private void jemSecurity$refreshAttestation(CallbackInfo callback) {
        SecurityClient.resourcePacksChanged();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void jemSecurity$renderWarning(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (jemSecurity$blockedPack == null) return;
        graphics.drawCenteredString(font, Component.translatable("jem_security.resource_pack.gotcha")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), width / 2, height - 70, 0xFFFFFF);
    }
}

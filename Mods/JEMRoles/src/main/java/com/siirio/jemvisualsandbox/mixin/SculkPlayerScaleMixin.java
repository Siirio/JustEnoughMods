package com.siirio.jemvisualsandbox.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jemvisualsandbox.workbench.SculkDomain;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class SculkPlayerScaleMixin {
    @Inject(method="scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",at=@At("TAIL"))
    private void sandbox$domainScale(AbstractClientPlayer player,PoseStack pose,float partialTick,CallbackInfo ci){
        if(player.getPersistentData().getBoolean(SculkDomain.SCALE_KEY))pose.scale(1.15F,1.15F,1.15F);
    }
}

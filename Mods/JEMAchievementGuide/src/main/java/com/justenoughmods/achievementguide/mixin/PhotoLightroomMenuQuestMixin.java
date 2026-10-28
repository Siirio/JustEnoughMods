package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.photo.PhotoPrintingProof;
import io.github.mortuusars.exposure.world.inventory.LightroomMenu;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LightroomMenu.class, remap = false)
public abstract class PhotoLightroomMenuQuestMixin {
    @Inject(method = "m_6366_", at = @At("HEAD"))
    private void jem$before(Player player, int button, CallbackInfoReturnable<Boolean> callback) {
        PhotoPrintingProof.menuStart((LightroomMenu)(Object)this, player, button);
    }
    @Inject(method = "m_6366_", at = @At("RETURN"))
    private void jem$after(Player player, int button, CallbackInfoReturnable<Boolean> callback) { PhotoPrintingProof.menuEnd(); }
}

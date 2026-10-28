package com.siirio.jemcinematicinteractions.mixin;

import com.siirio.jemcinematicinteractions.CinematicCraftingTransferMenu;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.client.TableStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "mezz.jei.gui.recipes.RecipesGui", remap = false)
public abstract class RecipesGuiMixin {
    @Shadow
    private Screen parentScreen;

    @Redirect(
            method = "open",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;m_91152_(Lnet/minecraft/client/gui/screens/Screen;)V"
            )
    )
    private void jemCinematicInteractions$preserveScene(Minecraft minecraft, Screen nextScreen) {
        if (minecraft.screen instanceof SceneScreen) {
            SceneAccess.jemCinematicInteractions$consult(() -> minecraft.setScreen(nextScreen));
            return;
        }
        minecraft.setScreen(nextScreen);
    }

    @Inject(method = "getParentContainerMenu", at = @At("RETURN"), cancellable = true)
    private void jemCinematicInteractions$exposeSceneMenu(
            CallbackInfoReturnable<AbstractContainerMenu> callback
    ) {
        if (callback.getReturnValue() != null || !(parentScreen instanceof SceneScreen)) {
            return;
        }

        if (Scene.station instanceof TableStation) {
            callback.setReturnValue(CinematicCraftingTransferMenu.INSTANCE);
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            callback.setReturnValue(minecraft.player.containerMenu);
        }
    }
}

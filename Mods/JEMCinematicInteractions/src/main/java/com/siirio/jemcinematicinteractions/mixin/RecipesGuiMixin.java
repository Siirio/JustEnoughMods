package com.siirio.jemcinematicinteractions.mixin;

import dev.dyslexic.cinematic.client.SceneScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "mezz.jei.gui.recipes.RecipesGui", remap = false)
public abstract class RecipesGuiMixin {
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
}

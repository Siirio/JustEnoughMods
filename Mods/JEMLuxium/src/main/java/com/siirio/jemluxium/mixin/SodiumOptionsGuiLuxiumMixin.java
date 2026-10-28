package com.siirio.jemluxium.mixin;

import com.siirio.jemluxium.JEMLuxium;
import com.siirio.jemluxium.client.LuxiumYouTubeButton;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SodiumOptionsGUI.class, remap = false)
public abstract class SodiumOptionsGuiLuxiumMixin extends Screen {
    @Shadow
    private OptionPage currentPage;

    protected SodiumOptionsGuiLuxiumMixin(Component title) {
        super(title);
    }

    @Inject(method = "rebuildGUI", at = @At("TAIL"))
    private void jemLuxium$addYouTubeButton(CallbackInfo ci) {
        if (currentPage != null
                && JEMLuxium.MOD_ID.equals(currentPage.getId().getModId())
                && "luxium".equals(currentPage.getId().getPath())) {
            addRenderableWidget(new LuxiumYouTubeButton(6, 70));
        }
    }
}

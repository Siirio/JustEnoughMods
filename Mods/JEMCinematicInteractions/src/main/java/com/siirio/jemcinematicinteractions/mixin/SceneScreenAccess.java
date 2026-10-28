package com.siirio.jemcinematicinteractions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "dev.dyslexic.cinematic.client.SceneScreen", remap = false)
public interface SceneScreenAccess {
    @Invoker("overPanel")
    boolean jemCinematicInteractions$overPanel(double mouseX, double mouseY);

    @Invoker("slotAt")
    int jemCinematicInteractions$slotAt(double mouseX, double mouseY);

    @Invoker("panelClick")
    void jemCinematicInteractions$panelClick(int slot, int button, boolean quickMove);
}

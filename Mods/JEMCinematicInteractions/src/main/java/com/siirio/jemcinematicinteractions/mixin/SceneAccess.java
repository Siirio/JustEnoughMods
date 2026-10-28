package com.siirio.jemcinematicinteractions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "dev.dyslexic.cinematic.client.Scene", remap = false)
public interface SceneAccess {
    @Invoker("consult")
    static void jemCinematicInteractions$consult(Runnable action) {
        throw new AssertionError();
    }
}

package com.siirio.jemcompat.mixin.client;

import mezz.jei.common.config.IClientToggleState;
import mezz.jei.gui.overlay.IngredientListOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = IngredientListOverlay.class, remap = false)
public interface IngredientListOverlayAccessor {
    @Accessor("toggleState")
    IClientToggleState jemcompat$getToggleState();
}

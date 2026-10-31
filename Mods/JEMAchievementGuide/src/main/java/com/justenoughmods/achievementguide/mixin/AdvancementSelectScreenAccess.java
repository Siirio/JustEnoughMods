package com.justenoughmods.achievementguide.mixin;

import net.mehvahdjukaar.advframes.blocks.AdvancementFrameBlockTile;
import net.mehvahdjukaar.advframes.client.AdvancementSelectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AdvancementSelectScreen.class)
public interface AdvancementSelectScreenAccess {
    @Accessor(value = "tile", remap = false)
    AdvancementFrameBlockTile jem$getTile();
}

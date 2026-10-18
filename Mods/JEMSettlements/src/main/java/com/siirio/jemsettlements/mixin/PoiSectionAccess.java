package com.siirio.jemsettlements.mixin;

import net.minecraft.world.entity.ai.village.poi.PoiSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PoiSection.class)
public interface PoiSectionAccess {
    @Accessor("isValid") void jem$valid(boolean value);
}

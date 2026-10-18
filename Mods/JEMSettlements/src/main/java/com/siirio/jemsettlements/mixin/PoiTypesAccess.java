package com.siirio.jemsettlements.mixin;

import java.util.Map;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Forge's POI reverse index is the native discovery seam; no duplicate HOME registration. */
@Mixin(PoiTypes.class)
public interface PoiTypesAccess {
    @Accessor("TYPE_BY_STATE")
    static Map<BlockState, PoiType> jem$states() { throw new AssertionError(); }
}

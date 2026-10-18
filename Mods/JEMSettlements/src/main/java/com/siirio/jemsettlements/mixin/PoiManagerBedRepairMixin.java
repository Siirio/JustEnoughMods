package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.compat.NativeBedCompatibility;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Existing chunks can have valid-but-incomplete POI data. Reuse native refresh and its existing claim records. */
@Mixin(PoiManager.class)
public abstract class PoiManagerBedRepairMixin {
    @Inject(method="checkConsistencyWithBlocks",at=@At("HEAD"))
    private void jem$refreshOldBedSection(SectionPos pos,LevelChunkSection blocks,CallbackInfo ci) {
        if(blocks.hasOnlyAir()||!blocks.maybeHas(NativeBedCompatibility::supportedHead))return;
        var section=((PoiSectionStorageAccess)(Object)this).jem$section(pos.asLong());
        if(section.isPresent()&&section.get() instanceof PoiSection poi)((PoiSectionAccess)poi).jem$valid(false);
        // Original checkConsistencyWithBlocks now performs its normal refresh. That path
        // reuses old PoiRecords at existing positions, including their occupied tickets.
    }
}

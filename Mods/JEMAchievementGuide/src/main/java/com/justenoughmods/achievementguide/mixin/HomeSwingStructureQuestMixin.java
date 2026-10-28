package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeSwingLineage;
import com.justenoughmods.achievementguide.integration.home.HomeSwingProof;
import euphy.upo.casualswing.content.rope.BaseSwingRopeBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseSwingRopeBlockEntity.class, remap = false)
public abstract class HomeSwingStructureQuestMixin implements HomeSwingLineage {
    @Unique private UUID jem$homeBuilder;
    @Override public UUID jem$homeBuilder() { return jem$homeBuilder; }
    @Override public void jem$homeBuilder(UUID builder) { jem$homeBuilder = builder; }
    @Inject(method = "finalizeStructure", at = @At("RETURN"))
    private void jem$formed(BaseSwingRopeBlockEntity master, List<BlockPos> parts, BlockPos pos, CallbackInfo callback) {
        HomeSwingProof.assembled(master, parts);
    }
    @Inject(method = "disbandStructure", at = @At("RETURN"))
    private void jem$disbanded(CallbackInfo callback) { jem$homeBuilder = null; }
    @Inject(method = "m_183515_", at = @At("RETURN"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) {
        if (jem$homeBuilder != null) tag.putUUID("jem_home_builder", jem$homeBuilder);
    }
    @Inject(method = "m_142466_", at = @At("RETURN"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) {
        jem$homeBuilder = tag.hasUUID("jem_home_builder") ? tag.getUUID("jem_home_builder") : null;
    }
}

package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateWhistleProofs;
import com.justenoughmods.achievementguide.integration.create.CreateWhistleRun;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.station.GlobalStation;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.trains.entity.Train", remap = false)
public abstract class CreateWhistleTrainQuestMixin implements CreateWhistleRun {
    @Unique private CompoundTag jem$whistle = new CompoundTag();
    @Unique private UUID jem$arriving;
    @Unique private ServerLevel jem$world;
    @Override public CompoundTag jem$whistleProof() { return jem$whistle.copy(); }
    @Override public void jem$whistleProof(CompoundTag proof) { jem$whistle = proof.copy(); }
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void jem$nativeWorld(Level world, CallbackInfo callback) { if (world instanceof ServerLevel level) jem$world = level; }
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$actualMovement(Level world, CallbackInfo callback) {
        if (world instanceof ServerLevel && !jem$whistle.isEmpty()) CreateWhistleProofs.ticked((Train)(Object)this, jem$whistleProof());
        jem$world = null;
    }
    @Inject(method = "arriveAt", at = @At("HEAD"), remap = false)
    private void jem$arriving(GlobalStation station, CallbackInfo callback) {
        jem$arriving = jem$whistle.isEmpty() ? null : CreateWhistleProofs.arriving((Train)(Object)this, station, jem$whistleProof());
    }
    @Inject(method = "arriveAt", at = @At("RETURN"), remap = false)
    private void jem$arrived(GlobalStation station, CallbackInfo callback) {
        if (jem$arriving != null && jem$world != null && station.id.equals(((Train)(Object)this).currentStation)) {
            CreateRunProofs.emit(jem$world, jem$arriving, "v3_eng_conductor"); jem$whistle = new CompoundTag();
        }
        jem$arriving = null;
    }
    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void jem$save(DimensionPalette palette, CallbackInfoReturnable<CompoundTag> callback) {
        if (!jem$whistle.isEmpty()) callback.getReturnValue().put("jemQuestConductorSummonV1", jem$whistle.copy());
    }
    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private static void jem$load(CompoundTag tag, Map<UUID, TrackGraph> graphs, DimensionPalette palette, CallbackInfoReturnable<Train> callback) {
        ((CreateWhistleRun)callback.getReturnValue()).jem$whistleProof(tag.getCompound("jemQuestConductorSummonV1"));
    }
}

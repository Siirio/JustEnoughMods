package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateMovingHarvest;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity", remap = false)
public abstract class CreateBearingHarvestQuestMixin {
    @Shadow protected ControlledContraptionEntity movedContraption;
    @Shadow protected boolean running;
    @Inject(method = "assemble", at = @At("RETURN"), remap = false)
    private void jem$nativeAssembler(CallbackInfo callback) {
        BlockEntity bearing = (BlockEntity)(Object)this;
        CompoundTag command = bearing.getPersistentData().getCompound("jemQuestBearingCommandV1");
        if (bearing.getLevel() instanceof ServerLevel && running && movedContraption != null
                && movedContraption.getContraption() != null && command.hasUUID("actor")) {
            ((Entity)(Object)movedContraption).getPersistentData().putUUID(CreateMovingHarvest.ASSEMBLER, command.getUUID("actor"));
        }
    }
}

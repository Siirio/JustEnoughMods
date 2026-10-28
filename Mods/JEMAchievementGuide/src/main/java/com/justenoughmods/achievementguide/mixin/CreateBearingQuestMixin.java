package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateBearingRun;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity", remap = false)
public abstract class CreateBearingQuestMixin implements CreateBearingRun {
    @Unique private static final String JEM_RUN = "jemQuestBearingCommandV1";
    @Shadow protected ControlledContraptionEntity movedContraption;
    @Shadow protected float angle;
    @Shadow protected boolean running;
    @Shadow protected boolean assembleNextTick;

    @Override public void jem$assemblyCommand(ServerPlayer actor) {
        BlockEntity owner = (BlockEntity)(Object)this;
        owner.getPersistentData().remove(JEM_RUN);
        if (actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || running || !assembleNextTick) return;
        CompoundTag run = new CompoundTag();
        run.putUUID("actor", actor.getUUID());
        run.putFloat("angle", angle);
        owner.getPersistentData().put(JEM_RUN, run);
        owner.setChanged();
    }

    @Inject(method = "disassemble", at = @At("HEAD"), remap = false)
    private void jem$cancel(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        owner.getPersistentData().remove(JEM_RUN);
        owner.setChanged();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$rotationCommitted(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        CompoundTag run = owner.getPersistentData().getCompound(JEM_RUN);
        if (!run.hasUUID("actor")) return;
        if (!running && !assembleNextTick) {
            owner.getPersistentData().remove(JEM_RUN);
            owner.setChanged();
            return;
        }
        if (!running || movedContraption == null || ((Entity)(Object)movedContraption).isRemoved()
                || movedContraption.getContraption() == null || movedContraption.getContraption().getBlocks().isEmpty()
                || Math.abs(angle - run.getFloat("angle")) < .001F) return;
        String action = "create_contraption_motion";
        if ((Object)this instanceof WindmillBearingBlockEntity windmill) {
            if (windmill.getGeneratedSpeed() == 0 || windmill.getSpeed() == 0) return;
            action = "create_windmill_power";
        }
        CreateRunProofs.emit(level, run.getUUID("actor"), action);
        owner.getPersistentData().remove(JEM_RUN);
        owner.setChanged();
    }
}

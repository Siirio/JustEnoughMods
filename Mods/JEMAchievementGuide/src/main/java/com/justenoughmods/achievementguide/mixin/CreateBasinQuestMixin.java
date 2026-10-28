package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateMixingRun;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinBlockEntity", remap = false)
public abstract class CreateBasinQuestMixin implements CreateMixingRun {
    @Unique private UUID jem$mixingActor;
    @Unique private UUID jem$compactingActor;
    @Unique private boolean jem$anyOutputCommitted;
    @Override public void jem$beginCompacting(UUID actor) { jem$compactingActor = actor; jem$anyOutputCommitted = false; }
    @Unique private boolean jem$brassCandidate;
    @Unique private boolean jem$brassCommitted;

    @Override public void jem$beginMixing(UUID actor) {
        jem$mixingActor = actor;
        jem$brassCommitted = false;
    }

    @Override public void jem$committedBrass() { jem$brassCommitted = true; }

    @Override public void jem$finishMixing(boolean success) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (success) {
            CreateRunProofs.invalidate(owner);
            if (jem$anyOutputCommitted && owner.getLevel() instanceof ServerLevel level) CreateRunProofs.emit(level, jem$compactingActor, "v3_eng_compact");
            if (jem$brassCommitted && owner.getLevel() instanceof ServerLevel level) {
                CreateRunProofs.emit(level, jem$mixingActor, "create_mix_brass");
            }
        }
        jem$mixingActor = null;
        jem$compactingActor = null;
        jem$anyOutputCommitted = false;
        jem$brassCommitted = false;
    }

    @Inject(method = "acceptOutputs", at = @At("HEAD"), remap = false)
    private void jem$actualOutputs(List<ItemStack> items, List<FluidStack> fluids, boolean simulate,
                                   CallbackInfoReturnable<Boolean> callback) {
        jem$brassCandidate = !simulate && items.stream().anyMatch(stack -> CreateRunProofs.item(stack, "create:brass_ingot"));
    }

    @Inject(method = "acceptOutputs", at = @At("RETURN"), remap = false)
    private void jem$outputCommitted(List<ItemStack> items, List<FluidStack> fluids, boolean simulate,
                                     CallbackInfoReturnable<Boolean> callback) {
        if (!simulate && callback.getReturnValueZ()) {
            if (jem$brassCandidate) jem$committedBrass();
            if (items.stream().anyMatch(stack -> !stack.isEmpty()) || fluids.stream().anyMatch(stack -> !stack.isEmpty())) jem$anyOutputCommitted = true;
        }
        jem$brassCandidate = false;
    }
}

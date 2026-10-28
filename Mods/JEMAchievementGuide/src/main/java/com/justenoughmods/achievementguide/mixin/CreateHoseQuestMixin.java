package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateFluidOperator;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler", remap = false)
public abstract class CreateHoseQuestMixin {
    @Shadow private FluidFillingBehaviour filler;
    @Shadow private FluidDrainingBehaviour drainer;
    @Unique private boolean jem$deposited;
    @Unique private boolean jem$pulled;
    @Inject(method = "fill", at = @At("HEAD"), remap = false)
    private void jem$beginFill(FluidStack fluid, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> callback) { jem$deposited = false; }
    @Redirect(method = "fill", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidFillingBehaviour;tryDeposit(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Z)Z"), remap = false)
    private boolean jem$actualDeposit(FluidFillingBehaviour owner, Fluid fluid, BlockPos pos, boolean simulate) {
        boolean result = owner.tryDeposit(fluid, pos, simulate); jem$deposited = !simulate && result; return result;
    }
    @Inject(method = "fill", at = @At("RETURN"), remap = false)
    private void jem$filled(FluidStack fluid, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> callback) {
        if (action.execute() && jem$deposited && callback.getReturnValue() > 0 && filler.getWorld() instanceof ServerLevel level)
            CreateRunProofs.emit(level, CreateFluidOperator.actor(), "v3_eng_hose");
        jem$deposited = false;
    }
    @Inject(method = "drainInternal", at = @At("HEAD"), remap = false)
    private void jem$beginDrain(int max, FluidStack fluid, IFluidHandler.FluidAction action, CallbackInfoReturnable<FluidStack> callback) { jem$pulled = false; }
    @Redirect(method = "drainInternal", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Z)Z"), remap = false)
    private boolean jem$actualPull(FluidDrainingBehaviour owner, BlockPos pos, boolean simulate) {
        boolean result = owner.pullNext(pos, simulate); jem$pulled = !simulate && result; return result;
    }
    @Inject(method = "drainInternal", at = @At("RETURN"), remap = false)
    private void jem$drained(int max, FluidStack fluid, IFluidHandler.FluidAction action, CallbackInfoReturnable<FluidStack> callback) {
        if (action.execute() && jem$pulled && !callback.getReturnValue().isEmpty() && drainer.getWorld() instanceof ServerLevel level)
            CreateRunProofs.emit(level, CreateFluidOperator.actor(), "v3_eng_hose");
        jem$pulled = false;
    }
}

package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastCauldronProof;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile$AlchemistCauldronFluidHandler", remap = false)
public abstract class MagicLastCauldronFluidQuestMixin {
    @Inject(method = "fill", at = @At("RETURN"))
    private void jem$fill(FluidStack fluid, FluidAction action, CallbackInfoReturnable<Integer> callback) { MagicLastCauldronProof.changed(this, action.execute() && callback.getReturnValue() > 0); }
    @Inject(method = "drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;", at = @At("RETURN"))
    private void jem$drain(FluidStack fluid, FluidAction action, CallbackInfoReturnable<FluidStack> callback) { MagicLastCauldronProof.changed(this, action.execute() && !callback.getReturnValue().isEmpty()); }
    @Inject(method = "drain(ILnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;", at = @At("RETURN"))
    private void jem$drainAmount(int amount, FluidAction action, CallbackInfoReturnable<FluidStack> callback) { MagicLastCauldronProof.changed(this, action.execute() && !callback.getReturnValue().isEmpty()); }
}

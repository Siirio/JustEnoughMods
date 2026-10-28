package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateSpoutProofs;
import com.justenoughmods.achievementguide.integration.create.CreateFluidOperator;
import com.justenoughmods.achievementguide.integration.create.CreateSpoutRun;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.fluids.FluidNetwork", remap = false)
public abstract class CreateSpoutNetworkQuestMixin {
    @Shadow Level world;
    @Shadow List<Pair<BlockFace, LazyOptional<IFluidHandler>>> targets;
    @Shadow Map<BlockPos, WeakReference<FluidTransportBehaviour>> cache;
    @Shadow Set<BlockPos> visited;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/IFluidHandler;fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I"), remap = false)
    private int jem$actualPumpedFluid(IFluidHandler handler, FluidStack supplied, IFluidHandler.FluidAction action) {
        CreateSpoutRun spout = null;
        if (world instanceof ServerLevel && action.execute() && targets.size() <= 4096) {
            for (var target : targets) {
                BlockPos pos = target.getFirst().getConnectedPos();
                if (target.getSecond().orElse(null) == handler && world.isLoaded(pos)
                        && world.getBlockEntity(pos) instanceof CreateSpoutRun found) { spout = found; break; }
            }
        }
        CompoundTag prior = spout == null ? new CompoundTag() : spout.jem$fluidProof();
        FluidStack before = spout == null ? FluidStack.EMPTY : handler.getFluidInTank(0).copy();
        int accepted = CreateFluidOperator.transfer(CreateSpoutProofs.operator(cache, visited), () -> handler.fill(supplied, action));
        if (spout != null && accepted > 0) spout.jem$pumped(CreateSpoutProofs.operator(cache, visited), before, supplied, accepted, prior);
        return accepted;
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/IFluidHandler;drain(ILnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;"), remap = false)
    private FluidStack jem$nativePoolDrain(IFluidHandler handler, int amount, IFluidHandler.FluidAction action) {
        return CreateFluidOperator.transfer(CreateSpoutProofs.operator(cache, visited), () -> handler.drain(amount, action));
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/IFluidHandler;drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;"), remap = false)
    private FluidStack jem$nativeTypedPoolDrain(IFluidHandler handler, FluidStack fluid, IFluidHandler.FluidAction action) {
        return CreateFluidOperator.transfer(CreateSpoutProofs.operator(cache, visited), () -> handler.drain(fluid, action));
    }
}


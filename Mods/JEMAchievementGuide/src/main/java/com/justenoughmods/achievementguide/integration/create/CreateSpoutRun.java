package com.justenoughmods.achievementguide.integration.create;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;

public interface CreateSpoutRun {
    CompoundTag jem$fluidProof();
    void jem$pumped(UUID actor, FluidStack before, FluidStack supplied, int accepted, CompoundTag prior);
}

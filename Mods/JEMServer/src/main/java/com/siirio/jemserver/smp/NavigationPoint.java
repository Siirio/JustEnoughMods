package com.siirio.jemserver.smp;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record NavigationPoint(
        String subsystem,
        UUID id,
        String label,
        ResourceLocation dimension,
        BlockPos position,
        long expires) {}

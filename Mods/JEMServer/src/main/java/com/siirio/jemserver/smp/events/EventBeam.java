package com.siirio.jemserver.smp.events;

import net.minecraft.resources.ResourceLocation;

public record EventBeam(ResourceLocation dimension,net.minecraft.core.BlockPos position,int duration,float radius) {}

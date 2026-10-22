package com.siirio.jemserver.smp;

import net.minecraft.resources.ResourceLocation;

public record WorldPosition(ResourceLocation dimension, double x, double y, double z, float yaw, float pitch) {}

package com.siirio.jemtwelveeyes.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record BossLocation(String key, ResourceLocation entity, ResourceLocation dimension, BlockPos position) {
}

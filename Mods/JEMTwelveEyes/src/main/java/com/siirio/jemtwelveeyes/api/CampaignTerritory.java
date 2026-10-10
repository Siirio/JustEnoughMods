package com.siirio.jemtwelveeyes.api;

import net.minecraft.resources.ResourceLocation;

public record CampaignTerritory(ResourceLocation dimension, ResourceLocation entity, int minX, int minZ, int maxX, int maxZ) {
}

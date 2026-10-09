package com.siirio.jemworldbosstiers.balance;

import net.minecraft.resources.ResourceLocation;

public record RewardProfile(
        ResourceLocation itemId,
        ResourceLocation sourceBoss,
        String category
) {}

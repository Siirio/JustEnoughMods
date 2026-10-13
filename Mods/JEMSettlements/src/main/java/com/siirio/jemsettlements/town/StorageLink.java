package com.siirio.jemsettlements.town;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record StorageLink(ResourceLocation dimension, BlockPos position, boolean feedResidents,
                          boolean productionInput, boolean productionOutput) {
    public StorageLink(ResourceLocation dimension, BlockPos position) { this(dimension, position, false, true, true); }
    public StorageLink withFeedResidents(boolean enabled) { return new StorageLink(dimension, position, enabled, productionInput, productionOutput); }
    public StorageLink withProduction(boolean input, boolean output) { return new StorageLink(dimension, position, feedResidents, input, output); }
    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Position", position.asLong());
        tag.putBoolean("FeedResidents", feedResidents);
        tag.putBoolean("ProductionInput", productionInput);
        tag.putBoolean("ProductionOutput", productionOutput);
        return tag;
    }

    static StorageLink load(CompoundTag tag) {
        boolean legacy = !tag.contains("ProductionInput") && !tag.contains("ProductionOutput");
        return new StorageLink(new ResourceLocation(tag.getString("Dimension")), BlockPos.of(tag.getLong("Position")),
                tag.getBoolean("FeedResidents"), legacy || tag.getBoolean("ProductionInput"), legacy || tag.getBoolean("ProductionOutput"));
    }
}

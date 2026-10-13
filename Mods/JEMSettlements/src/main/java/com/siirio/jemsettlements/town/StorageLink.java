package com.siirio.jemsettlements.town;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record StorageLink(ResourceLocation dimension, BlockPos position, boolean feedResidents) {
    public StorageLink(ResourceLocation dimension, BlockPos position) { this(dimension, position, false); }
    public StorageLink withFeedResidents(boolean enabled) { return new StorageLink(dimension, position, enabled); }
    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Position", position.asLong());
        tag.putBoolean("FeedResidents", feedResidents);
        return tag;
    }

    static StorageLink load(CompoundTag tag) {
        return new StorageLink(new ResourceLocation(tag.getString("Dimension")), BlockPos.of(tag.getLong("Position")), tag.getBoolean("FeedResidents"));
    }
}

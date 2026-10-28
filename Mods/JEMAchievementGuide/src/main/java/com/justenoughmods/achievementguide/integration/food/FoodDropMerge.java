package com.justenoughmods.achievementguide.integration.food;

import net.minecraft.world.entity.item.ItemEntity;

public interface FoodDropMerge {
    void jem$beforeMerge(ItemEntity source);
    void jem$afterMerge(ItemEntity source);
}

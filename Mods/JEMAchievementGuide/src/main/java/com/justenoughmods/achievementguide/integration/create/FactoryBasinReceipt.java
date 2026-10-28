package com.justenoughmods.achievementguide.integration.create;

import net.minecraft.world.item.crafting.Recipe;

public interface FactoryBasinReceipt {
    void jem$factoryBegin(Recipe<?> recipe);
    void jem$factoryFinish(boolean success);
}

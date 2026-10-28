package com.justenoughmods.achievementguide.integration.food;

/** A native static recipe method passes its actual station explicitly through one synchronous run. */
public interface FoodRecipeRun {
    void jem$beginOutput(int slot);
    void jem$finishOutput(int slot, String recipe, String action);
}

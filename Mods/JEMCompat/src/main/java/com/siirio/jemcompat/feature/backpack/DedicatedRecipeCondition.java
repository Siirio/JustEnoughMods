package com.siirio.jemcompat.feature.backpack;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.fml.loading.FMLLoader;

/** Recipe loading runs on the authoritative host; remote clients receive its recipe snapshot. */
public final class DedicatedRecipeCondition implements ICondition {
    public static final ResourceLocation ID = new ResourceLocation("jemcompat", "dedicated_server");
    private static final DedicatedRecipeCondition INSTANCE = new DedicatedRecipeCondition();

    @Override public ResourceLocation getID() { return ID; }
    @Override public boolean test(IContext context) { return FMLLoader.getDist().isDedicatedServer(); }

    public static final class Serializer implements IConditionSerializer<DedicatedRecipeCondition> {
        @Override public void write(JsonObject json, DedicatedRecipeCondition condition) { }
        @Override public DedicatedRecipeCondition read(JsonObject json) { return INSTANCE; }
        @Override public ResourceLocation getID() { return ID; }
    }
}

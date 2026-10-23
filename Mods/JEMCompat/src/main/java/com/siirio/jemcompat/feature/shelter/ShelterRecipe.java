package com.siirio.jemcompat.feature.shelter;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** One vanilla-tag recipe per shape, with coherent matching wood enforced by actual registry identity. */
public final class ShelterRecipe extends ShapedRecipe {
    ShelterRecipe(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
    }
    @Override public RecipeSerializer<?> getSerializer() { return ShelterContent.RECIPE.get(); }
    @Override public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) return false;
        String family = null;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot); if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String current = family(id); if (current == null || (family != null && !family.equals(current))) return false;
            family = current;
        }
        return family != null;
    }
    private static String family(ResourceLocation id) {
        if (!id.getNamespace().equals("minecraft")) return null;
        String path = id.getPath().replaceFirst("^stripped_", "");
        for (String family : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "crimson", "warped"}) {
            if (path.startsWith(family + "_")) return family;
        }
        return null;
    }
    public static final class Serializer implements RecipeSerializer<ShelterRecipe> {
        private final ShapedRecipe.Serializer nativeSerializer = new ShapedRecipe.Serializer();
        @Override public ShelterRecipe fromJson(ResourceLocation id, JsonObject json) { return new ShelterRecipe(nativeSerializer.fromJson(id, json)); }
        @Override public ShelterRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) { return new ShelterRecipe(nativeSerializer.fromNetwork(id, buffer)); }
        @Override public void toNetwork(FriendlyByteBuf buffer, ShelterRecipe recipe) { nativeSerializer.toNetwork(buffer, recipe); }
    }
}

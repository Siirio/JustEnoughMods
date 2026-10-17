package com.siirio.jemeconomy.full.audit;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in loaded-registry evidence. No entities are spawned or player/save data changed. */
public final class EconomyRuntimeExport {
    private EconomyRuntimeExport() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (event.getServer().isDedicatedServer() && Boolean.getBoolean("jem.economy.export")) export(event.getServer());
    }

    public static void export(MinecraftServer server) {
        JsonObject document = new JsonObject();
        document.addProperty("scope", "Loaded dedicated-server item registry and recipes; not player acceptance");
        document.addProperty("capturedUtc", java.time.Instant.now().toString());
        JsonArray items = new JsonArray();
        for (var id : ForgeRegistries.ITEMS.getKeys().stream().sorted().toList()) {
            var item = ForgeRegistries.ITEMS.getValue(id);
            ItemStack stack = new ItemStack(item);
            JsonObject row = new JsonObject(); row.addProperty("id", id.toString());
            row.addProperty("class", item.getClass().getName()); row.addProperty("name", stack.getHoverName().getString());
            row.addProperty("stackSize", stack.getMaxStackSize()); row.addProperty("durability", stack.getMaxDamage());
            row.addProperty("rarity", stack.getRarity().name());
            JsonArray tags = new JsonArray(); stack.getTags().map(Object::toString).sorted().forEach(tags::add); row.add("tags", tags);
            var food = item.getFoodProperties(stack, null);
            if (food != null) {
                row.addProperty("nutrition", food.getNutrition()); row.addProperty("saturation", food.getSaturationModifier());
                row.addProperty("meat", food.isMeat()); row.addProperty("foodEffects", food.getEffects().size());
            }
            items.add(row);
        }
        document.add("items", items);
        JsonArray recipes = new JsonArray();
        for (var recipe : server.getRecipeManager().getRecipes().stream().sorted(java.util.Comparator.comparing(r -> r.getId().toString())).toList()) {
            JsonObject row = new JsonObject(); row.addProperty("id", recipe.getId().toString());
            row.addProperty("type", String.valueOf(ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer())));
            ItemStack output = recipe.getResultItem(server.registryAccess());
            row.addProperty("output", String.valueOf(ForgeRegistries.ITEMS.getKey(output.getItem()))); row.addProperty("count", output.getCount());
            JsonArray inputs = new JsonArray();
            for (var ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                JsonArray choices = new JsonArray();
                for (ItemStack choice : ingredient.getItems()) choices.add(String.valueOf(ForgeRegistries.ITEMS.getKey(choice.getItem())));
                inputs.add(choices);
            }
            row.add("ingredients", inputs); recipes.add(row);
        }
        document.add("recipes", recipes);
        JsonArray policies = new JsonArray();
        for (var policy : com.siirio.jemeconomy.policy.ItemPolicyRegistry.reviewed()) {
            if (!policy.buyable()) continue;
            JsonObject row = new JsonObject(); row.addProperty("item", policy.item().toString());
            row.addProperty("registered", ForgeRegistries.ITEMS.containsKey(policy.item()));
            row.addProperty("buy", policy.buyCopper()); row.addProperty("sell", policy.sellCopper()); policies.add(row);
        }
        document.add("marketPolicies", policies);
        document.add("nativeTrades", com.siirio.jemeconomy.merchant.NativeTradeCatalog.snapshot(server.overworld()));
        document.add("nativeMerchantContracts", NativeMerchantContracts.snapshot(server.overworld()));
        try {
            Path directory = server.getServerDirectory().toPath().resolve("local/economy-audit"); Files.createDirectories(directory);
            Path pending = directory.resolve("loaded-registry.json.tmp"), target = directory.resolve("loaded-registry.json");
            Files.writeString(pending, new GsonBuilder().setPrettyPrinting().create().toJson(document));
            Files.move(pending, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            com.mojang.logging.LogUtils.getLogger().info("JEM economy evidence: {} loaded items, {} recipes exported to {}", items.size(), recipes.size(), target);
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException("Cannot save requested economy evidence", failure); }
    }
}

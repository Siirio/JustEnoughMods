package com.siirio.jemeconomy.policy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.siirio.jemeconomy.JemEconomy;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemPolicyRegistry {
    private static final ResourceLocation RESOURCE = new ResourceLocation(JemEconomy.MOD_ID, "economy/item_policies.json");
    private static volatile Map<ResourceLocation, ItemPolicy> policies = loadBuiltIn();
    private ItemPolicyRegistry() { }

    public static Optional<ItemPolicy> policy(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        ItemPolicy policy = id == null ? null : policies.get(id);
        if (policy == null || !canonical(stack)) return Optional.empty();
        return Optional.of(policy);
    }

    public static ItemPolicy policy(ResourceLocation id) {
        return policies.getOrDefault(id, new ItemPolicy(1, id, ItemPolicy.Outcome.NONE, null, null, 0, 0, "", "Unclassified; fail closed"));
    }

    public static boolean acceptsShipping(ItemStack stack) { return !TradeRestrictions.marketProhibited(stack) && policy(stack).map(ItemPolicy::sellable).orElse(false); }
    public static Collection<ItemPolicy> reviewed() { return java.util.List.copyOf(policies.values()); }

    public static boolean canonical(ItemStack stack) {
        if (stack.isEmpty() || stack.isDamaged() || stack.isEnchanted()) return false;
        CompoundTag tag = stack.getTag();
        return tag == null || tag.isEmpty();
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Map<ResourceLocation, ItemPolicy>>() {
            @Override protected Map<ResourceLocation, ItemPolicy> prepare(net.minecraft.server.packs.resources.ResourceManager manager,
                    net.minecraft.util.profiling.ProfilerFiller profiler) {
                return manager.getResource(RESOURCE).map(resource -> {
                    try (var reader = resource.openAsReader()) { return parse(com.google.gson.JsonParser.parseReader(reader).getAsJsonArray()); }
                    catch (Exception failure) { throw new IllegalStateException("Invalid economy policy resource " + RESOURCE, failure); }
                }).orElseGet(ItemPolicyRegistry::loadBuiltIn);
            }
            @Override protected void apply(Map<ResourceLocation, ItemPolicy> next, net.minecraft.server.packs.resources.ResourceManager manager,
                    net.minecraft.util.profiling.ProfilerFiller profiler) { policies = Map.copyOf(next); MarketSupply.invalidate(); }
        });
    }

    private static Map<ResourceLocation, ItemPolicy> loadBuiltIn() {
        try (var stream = ItemPolicyRegistry.class.getResourceAsStream("/data/jem_economy/economy/item_policies.json")) {
            if (stream == null) throw new IllegalStateException("Missing built-in economy policy resource");
            return parse(com.google.gson.JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray());
        } catch (Exception failure) { throw new IllegalStateException("Cannot load built-in economy policies", failure); }
    }

    private static Map<ResourceLocation, ItemPolicy> parse(JsonArray array) {
        Map<ResourceLocation, ItemPolicy> result = new LinkedHashMap<>();
        for (JsonElement element : array) {
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(row.get("item").getAsString());
            if (id == null) throw new IllegalArgumentException("Invalid policy item ID: " + row);
            String outcomeText = row.has("outcome") ? row.get("outcome").getAsString() : "NONE";
            ItemPolicy.Outcome outcome;
            try { outcome = ItemPolicy.Outcome.valueOf(outcomeText); } catch (IllegalArgumentException ignored) { outcome = ItemPolicy.Outcome.NONE; }
            Long sell = nullableLong(row, "sell_copper"), buy = nullableLong(row, "buy_copper");
            if (outcome == ItemPolicy.Outcome.BUY_AND_SELL && (sell == null || buy == null || sell <= 0L || buy <= 0L
                    || buy > 1_000_000_000L || sell > 1_000_000_000L || buy <= Math.floorDiv(Math.multiplyExact(sell, 150L), 100L)
                    || integer(row, "daily_player_units") <= 0 || integer(row, "daily_global_units") <= 0))
                throw new IllegalArgumentException("Unsafe two-way economy policy: " + id);
            if (result.containsKey(id)) throw new IllegalArgumentException("Duplicate economy policy: " + id);
            result.put(id, new ItemPolicy(row.has("schema") ? row.get("schema").getAsInt() : 1, id, outcome, sell, buy,
                    integer(row, "daily_player_units"), integer(row, "daily_global_units"), string(row, "demand_group"), string(row, "notes")));
        }
        return result;
    }

    private static Long nullableLong(JsonObject row, String key) { return !row.has(key) || row.get(key).isJsonNull() ? null : row.get(key).getAsLong(); }
    private static int integer(JsonObject row, String key) { return row.has(key) ? Math.max(0, row.get(key).getAsInt()) : 0; }
    private static String string(JsonObject row, String key) { return row.has(key) ? row.get(key).getAsString() : ""; }
}

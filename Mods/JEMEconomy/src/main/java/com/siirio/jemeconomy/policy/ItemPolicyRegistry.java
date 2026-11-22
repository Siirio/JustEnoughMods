package com.siirio.jemeconomy.policy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.siirio.jemcurrency.JemCurrencyCore;
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
    private static final ResourceLocation RESOURCE = new ResourceLocation(JemCurrencyCore.LEGACY_NAMESPACE, "economy/item_policies.json");
    private static final ResourceLocation TIDE_RESOURCE = new ResourceLocation(JemCurrencyCore.LEGACY_NAMESPACE, "economy/tide_fish_values.json");
    private static final ResourceLocation TIDE_TAG_RESOURCE = new ResourceLocation("jem_economy", "tags/items/tide_sale_fish.json");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> TIDE_FISH = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("jem_economy","tide_sale_fish"));
    private static final RegistryData BUILT_IN = loadBuiltIn();
    private static volatile Map<ResourceLocation, ItemPolicy> policies = BUILT_IN.policies();
    private static volatile Map<ResourceLocation, TideFishValue> tideFishValues = BUILT_IN.tideFishValues();
    private ItemPolicyRegistry() { }

    private enum TideTier {
        ORDINARY(10L), RARE(100L), MYTHIC(500L), LEGENDARY(10_000L);
        private final long baseCopper;
        TideTier(long baseCopper) { this.baseCopper = baseCopper; }
    }
    private record TideFishValue(ResourceLocation item, TideTier tier, long baseCopper,
                                 double typicalLowCm, double typicalHighCm, double recordHighCm) {
        private long unitValue(ItemStack stack) {
            double average = (typicalLowCm + typicalHighCm) / 2.0D;
            CompoundTag tag = stack.getTag();
            double length = tag != null && tag.contains("FishLength", net.minecraft.nbt.Tag.TAG_DOUBLE)
                    && Double.isFinite(tag.getDouble("FishLength")) && tag.getDouble("FishLength") > 0.0D
                    ? tag.getDouble("FishLength") : average;
            double progress = Math.max(0.0D, Math.min(1.0D, (length - average) / (recordHighCm - average)));
            long maxBonus = Math.max(1L, Math.floorDiv(baseCopper, 100L));
            return Math.addExact(baseCopper, (long) Math.floor(maxBonus * progress));
        }
    }
    private record RegistryData(Map<ResourceLocation, ItemPolicy> policies,
                                Map<ResourceLocation, TideFishValue> tideFishValues) { }

    public static Optional<ItemPolicy> policy(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        ItemPolicy policy = id == null ? null : policy(id);
        if (policy == null || policy.outcome() == ItemPolicy.Outcome.NONE || !canonical(stack)) return Optional.empty();
        return Optional.of(policy);
    }

    public static ItemPolicy policy(ResourceLocation id) {
        var policy = policies.getOrDefault(id, new ItemPolicy(1, id, ItemPolicy.Outcome.NONE, null, null, 0, 0, "", "Unclassified; fail closed"));
        if (ForgeRegistries.ITEMS.getValue(id) instanceof net.minecraft.world.item.RecordItem || policy.demandGroup().equals("music_discs"))
            return new ItemPolicy(2,id,ItemPolicy.Outcome.BUY_ONLY,null,com.siirio.jemeconomy.physical.CoinDenomination.ARCANE.value(),
                    Math.max(1,policy.dailyPlayerUnits()),Math.max(1,policy.dailyGlobalUnits()),"music_discs","User: one Arcane to buy; never buy back",1,1,id,1,true,null);
        return policy;
    }

    public static boolean acceptsShipping(ItemStack stack) { return !TradeRestrictions.saleProhibited(stack) && policy(stack).map(ItemPolicy::sellable).orElse(false); }
    public static Collection<ItemPolicy> reviewed() { return java.util.List.copyOf(policies.values()); }

    public static boolean canonical(ItemStack stack) {
        if (stack.isEmpty() || stack.isDamaged() || stack.isEnchanted()) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.isEmpty()) return true;
        if (nativeTideFish(stack)) {
            for (String key : tag.getAllKeys()) {
                if (key.equals("FishLength")) {
                    double length=tag.getDouble(key);
                    if (!tag.contains(key, net.minecraft.nbt.Tag.TAG_DOUBLE) || !Double.isFinite(length) || length<=0) return false;
                } else if (!key.equals("IsBucketable") || !tag.contains(key,net.minecraft.nbt.Tag.TAG_BYTE) || tag.getByte(key)<0 || tag.getByte(key)>1) return false;
            }
            return true;
        }
        // Exact native Vinery adapter accepts only bounded native aging fields, without stripping NBT.
        return stack.getItem().isEdible() && com.siirio.jemeconomy.merchant.NativeItemPricing.usesWineAging(stack)
                && com.siirio.jemeconomy.merchant.NativeItemPricing.matchesGoods(new ItemStack(stack.getItem()),stack);
    }

    public static boolean nativeTideFish(ItemStack stack) {
        return stack.is(TIDE_FISH);
    }

    public static boolean matchesSaleGoods(ItemStack expected,ItemStack actual) {
        return ItemStack.isSameItemSameTags(expected,actual);
    }

    public static RationalPrice salePrice(ItemStack stack, int quantity, int multiplier) {
        ItemPolicy policy = policy(stack).filter(ItemPolicy::sellable).orElseThrow(
                () -> new ArithmeticException("Unpriced sale stack"));
        if (quantity < 0 || multiplier <= 0) throw new ArithmeticException("Invalid sale quantity or multiplier");
        TideFishValue fish = tideFishValues.get(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        if (fish == null) return policy.salePrice(quantity, multiplier);
        return new RationalPrice(fish.unitValue(stack), 1).multiply(quantity)
                .multiply(policy.fixedPrice() ? 100L : multiplier).divide(100L);
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<RegistryData>() {
            @Override protected RegistryData prepare(net.minecraft.server.packs.resources.ResourceManager manager,
                    net.minecraft.util.profiling.ProfilerFiller profiler) {
                try {
                    JsonArray policyRows = manager.getResource(RESOURCE).map(resource -> {
                        try (var reader = resource.openAsReader()) {
                            return com.google.gson.JsonParser.parseReader(reader).getAsJsonArray();
                        } catch (Exception failure) {
                            throw new IllegalStateException("Invalid economy policy resource " + RESOURCE, failure);
                        }
                    }).orElseThrow(() -> new IllegalStateException("Missing economy policy resource " + RESOURCE));
                    JsonObject fishRows = manager.getResource(TIDE_RESOURCE).map(resource -> {
                        try (var reader = resource.openAsReader()) {
                            return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                        } catch (Exception failure) {
                            throw new IllegalStateException("Invalid Tide fish resource " + TIDE_RESOURCE, failure);
                        }
                    }).orElseThrow(() -> new IllegalStateException("Missing Tide fish resource " + TIDE_RESOURCE));
                    JsonObject fishTag = manager.getResource(TIDE_TAG_RESOURCE).map(resource -> {
                        try (var reader = resource.openAsReader()) {
                            return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                        } catch (Exception failure) {
                            throw new IllegalStateException("Invalid Tide fish tag resource " + TIDE_TAG_RESOURCE, failure);
                        }
                    }).orElseThrow(() -> new IllegalStateException("Missing Tide fish tag resource " + TIDE_TAG_RESOURCE));
                    return new RegistryData(parse(policyRows), parseTide(fishRows, fishTag, true));
                } catch (RuntimeException failure) {
                    throw failure;
                }
            }
            @Override protected void apply(RegistryData next, net.minecraft.server.packs.resources.ResourceManager manager,
                    net.minecraft.util.profiling.ProfilerFiller profiler) {
                policies = Map.copyOf(next.policies());
                tideFishValues = Map.copyOf(next.tideFishValues());
                MarketSupply.invalidate();
            }
        });
    }

    private static RegistryData loadBuiltIn() {
        try (var policyStream = ItemPolicyRegistry.class.getResourceAsStream("/data/jem_economy/economy/item_policies.json");
             var fishStream = ItemPolicyRegistry.class.getResourceAsStream("/data/jem_economy/economy/tide_fish_values.json");
             var fishTagStream = ItemPolicyRegistry.class.getResourceAsStream("/data/jem_economy/tags/items/tide_sale_fish.json")) {
            if (policyStream == null || fishStream == null || fishTagStream == null)
                throw new IllegalStateException("Missing built-in economy resources");
            return new RegistryData(
                    parse(com.google.gson.JsonParser.parseReader(new InputStreamReader(policyStream, StandardCharsets.UTF_8)).getAsJsonArray()),
                    parseTide(
                            com.google.gson.JsonParser.parseReader(new InputStreamReader(fishStream, StandardCharsets.UTF_8)).getAsJsonObject(),
                            com.google.gson.JsonParser.parseReader(new InputStreamReader(fishTagStream, StandardCharsets.UTF_8)).getAsJsonObject(),
                            false));
        } catch (Exception failure) { throw new IllegalStateException("Cannot load built-in economy policies", failure); }
    }

    private static Map<ResourceLocation, TideFishValue> parseTide(JsonObject root, JsonObject tagRoot, boolean validateRegistry) {
        if (!root.has("schema") || root.get("schema").getAsInt() != 1 || !root.has("fish") || !root.get("fish").isJsonArray())
            throw new IllegalArgumentException("Invalid Tide fish manifest root");
        Map<ResourceLocation, TideFishValue> result = new LinkedHashMap<>();
        java.util.EnumMap<TideTier, Integer> counts = new java.util.EnumMap<>(TideTier.class);
        for (JsonElement element : root.getAsJsonArray("fish")) {
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(row.get("item").getAsString());
            if (id == null || !id.getNamespace().equals("tide") || result.containsKey(id))
                throw new IllegalArgumentException("Invalid or duplicate Tide fish ID: " + row);
            TideTier tier;
            try { tier = TideTier.valueOf(row.get("tier").getAsString()); }
            catch (RuntimeException invalid) { throw new IllegalArgumentException("Invalid Tide tier for " + id, invalid); }
            long base = row.get("base_copper").getAsLong();
            double low = row.get("typical_low_cm").getAsDouble();
            double high = row.get("typical_high_cm").getAsDouble();
            double record = row.get("record_high_cm").getAsDouble();
            double average = (low + high) / 2.0D;
            if (base != tier.baseCopper || !Double.isFinite(low) || !Double.isFinite(high) || !Double.isFinite(record)
                    || low <= 0.0D || high < low || record <= average)
                throw new IllegalArgumentException("Invalid Tide fish values for " + id);
            if (validateRegistry && !ForgeRegistries.ITEMS.containsKey(id))
                throw new IllegalArgumentException("Unregistered Tide fish item " + id);
            result.put(id, new TideFishValue(id, tier, base, low, high, record));
            counts.merge(tier, 1, Integer::sum);
        }
        if (result.size() != 101 || counts.getOrDefault(TideTier.ORDINARY, 0) != 39
                || counts.getOrDefault(TideTier.RARE, 0) != 25 || counts.getOrDefault(TideTier.MYTHIC, 0) != 20
                || counts.getOrDefault(TideTier.LEGENDARY, 0) != 17)
            throw new IllegalArgumentException("Tide fish manifest must contain the exact 101 tier assignment");
        java.util.Set<ResourceLocation> tagged = parseTideTag(tagRoot);
        if (!tagged.equals(result.keySet())) throw new IllegalArgumentException("Tide sale tag and value manifest differ");
        return Map.copyOf(result);
    }

    private static java.util.Set<ResourceLocation> parseTideTag(JsonObject root) {
        if (!root.has("values") || !root.get("values").isJsonArray())
            throw new IllegalArgumentException("Invalid Tide fish tag root");
        java.util.Set<ResourceLocation> result = new java.util.LinkedHashSet<>();
        for (JsonElement element : root.getAsJsonArray("values")) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new IllegalArgumentException("Invalid Tide fish tag entry " + element);
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            if (id == null || !id.getNamespace().equals("tide") || !result.add(id))
                throw new IllegalArgumentException("Invalid or duplicate Tide fish tag entry " + element);
        }
        return java.util.Set.copyOf(result);
    }

    private static Map<ResourceLocation, ItemPolicy> parse(JsonArray array) {
        Map<ResourceLocation, ItemPolicy> result = new LinkedHashMap<>();
        for (JsonElement element : array) {
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(row.get("item").getAsString());
            if (id == null) throw new IllegalArgumentException("Invalid policy item ID: " + row);
            String outcomeText = row.has("outcome") ? row.get("outcome").getAsString() : "NONE";
            ItemPolicy.Outcome outcome;
            try { outcome = ItemPolicy.Outcome.valueOf(outcomeText); } catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Invalid policy outcome for " + id,invalid); }
            Long sell = nullableLong(row, "sell_copper"), buy = nullableLong(row, "buy_copper");
            Long maxFinalSell = nullableLong(row, "max_final_sell_copper");
            if (maxFinalSell != null && (maxFinalSell <= 0L || maxFinalSell > 1_000_000_000L || sell == null || sell <= 0L))
                throw new IllegalArgumentException("Invalid final sell cap for " + id);
            if (outcome == ItemPolicy.Outcome.BUY_AND_SELL && (sell == null || buy == null || sell <= 0L || buy <= 0L
                    || buy > 1_000_000_000L || sell > 1_000_000_000_000_000L || new RationalPrice(sell,positive(row,"sell_denominator")).ceil() > 1_000_000_000L || unsafeSpread(sell,positive(row,"sell_denominator"),buy,positive(row,"buy_denominator"))
                    || integer(row, "daily_player_units") <= 0 || integer(row, "daily_global_units") <= 0))
                throw new IllegalArgumentException("Unsafe two-way economy policy: " + id);
            if (outcome == ItemPolicy.Outcome.SELL_ONLY && (sell == null || sell <= 0 || sell > 1_000_000_000_000_000L || new RationalPrice(sell,positive(row,"sell_denominator")).ceil() > 1_000_000_000L
                    || integer(row,"daily_player_units") <= 0 || integer(row,"daily_global_units") <= 0))
                throw new IllegalArgumentException("Unsafe sell-only policy: " + id);
            if (outcome == ItemPolicy.Outcome.BUY_ONLY && (buy == null || buy <= 0 || buy > 1_000_000_000L))
                throw new IllegalArgumentException("Unsafe buy-only policy: " + id);
            if (result.containsKey(id)) throw new IllegalArgumentException("Duplicate economy policy: " + id);
            result.put(id, new ItemPolicy(row.has("schema") ? row.get("schema").getAsInt() : 1, id, outcome, sell, buy,
                    integer(row, "daily_player_units"), integer(row, "daily_global_units"), string(row, "demand_group"), string(row, "notes"),
                    positive(row,"sell_denominator"), positive(row,"buy_denominator"),
                    row.has("quota_family") ? new ResourceLocation(row.get("quota_family").getAsString()) : id,
                    (int) positive(row,"quota_units"), row.has("fixed_price") && row.get("fixed_price").getAsBoolean(), nullableLong(row,"bulk_1024"), maxFinalSell));
        }
        return result;
    }

    private static boolean unsafeSpread(long sell,long sellDenominator,long buy,long buyDenominator) {
        return java.math.BigInteger.valueOf(buy).multiply(java.math.BigInteger.valueOf(sellDenominator)).multiply(java.math.BigInteger.valueOf(100))
                .compareTo(java.math.BigInteger.valueOf(sell).multiply(java.math.BigInteger.valueOf(buyDenominator)).multiply(java.math.BigInteger.valueOf(150)))<=0;
    }
    private static long positive(JsonObject row,String key) {
        long value = row.has(key) ? row.get(key).getAsLong() : 1;
        if (value <= 0 || value > 1_000_000_000L) throw new IllegalArgumentException("Invalid " + key);
        return value;
    }
    private static Long nullableLong(JsonObject row, String key) { return !row.has(key) || row.get(key).isJsonNull() ? null : row.get(key).getAsLong(); }
    private static int integer(JsonObject row, String key) { return row.has(key) ? Math.max(0, row.get(key).getAsInt()) : 0; }
    private static String string(JsonObject row, String key) { return row.has(key) ? row.get(key).getAsString() : ""; }
}

package com.siirio.jemeconomy.policy;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/** Public purchase admission uses the shared JEM catalog owner; never a second buyback policy. */
public final class MarketSupply {
    public record Listing(ResourceLocation id, ItemStack stack, ItemPolicy policy) { }
    private static final Map<ResourceLocation, Long> PRICES = loadPrices();
    private static final String PAINTING_PREFIX = "painting/";
    private static final Map<MinecraftServer, List<Listing>> CACHE = new java.util.WeakHashMap<>();
    private static final Map<MinecraftServer,Map<String,List<Listing>>> CATEGORY_CACHE=new java.util.WeakHashMap<>();
    private MarketSupply() { }

    /** Presentation filter over admitted stock; never admits goods or changes buyback policy. */
    public static boolean matchesCategory(Listing listing, String category) {
        if (category.startsWith("building")) return listing.policy().bulk1024() != null;
        if (!java.util.Set.of("blocks","food","plants","resources","drops").contains(category)) return false;
        if (!category.equals("plants")) return listing.policy().marketCategory().name().equalsIgnoreCase(category);
        return com.siirio.jemeconomy.compat.catalog.OptionalCatalog.isNature(listing.stack());
    }

    public static synchronized void invalidate() { CACHE.clear(); CATEGORY_CACHE.clear(); }

    public static synchronized List<Listing> listings(MinecraftServer server,String category) {
        return CATEGORY_CACHE.computeIfAbsent(server,ignored->new java.util.HashMap<>()).computeIfAbsent(category,
                key->listings(server).stream().filter(row->matchesCategory(row,key)).toList());
    }

    public static synchronized List<Listing> listings(MinecraftServer server) {
        var cached = CACHE.get(server);
        if (cached != null) return cached;
        List<Listing> result = new ArrayList<>();
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var id = ForgeRegistries.ITEMS.getKey(item);
            if (item == Items.AIR || item == Items.PAINTING) continue;
            ItemStack stack = new ItemStack(item);
            var policy = policy(stack);
            if (policy != null && (policy.buyable() || policy.sellable())) result.add(new Listing(id, stack, policy));
        }
        var paintingPolicy = policy(new ItemStack(Items.PAINTING));
        if (paintingPolicy != null) {
            var registry = server.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT);
            registry.keySet().stream().sorted().forEach(variant -> {
                ResourceLocation offer = new ResourceLocation("jem_economy", PAINTING_PREFIX + variant.getNamespace() + "/" + variant.getPath());
                result.add(new Listing(offer, painting(variant), paintingPolicy));
            });
        }
        result.sort(java.util.Comparator.comparing(row->row.id().toString()));
        var immutable = List.copyOf(result);
        CACHE.put(server, immutable);
        return immutable;
    }

    /** Client supplies only an offer ID. The server reconstructs and validates every delivered stack. */
    public static Listing resolve(MinecraftServer server, ResourceLocation offer) {
        if (offer == null) return null;
        if (offer.getNamespace().equals("jem_economy") && offer.getPath().startsWith(PAINTING_PREFIX)) {
            String[] parts = offer.getPath().substring(PAINTING_PREFIX.length()).split("/", 2);
            if (parts.length != 2) return null;
            ResourceLocation variant = ResourceLocation.tryParse(parts[0] + ":" + parts[1]);
            if (variant == null || !server.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).containsKey(variant)) return null;
            var policy = policy(new ItemStack(Items.PAINTING));
            return policy == null ? null : new Listing(offer, painting(variant), policy);
        }
        var item = ForgeRegistries.ITEMS.getValue(offer);
        if (item == null || item == Items.AIR) return null;
        ItemStack stack = new ItemStack(item);
        var policy = policy(stack);
        return policy == null ? null : new Listing(offer, stack, policy);
    }

    private static ItemStack painting(ResourceLocation variant) {
        ItemStack stack = new ItemStack(Items.PAINTING);
        stack.getOrCreateTagElement("EntityTag").putString("variant", variant.toString());
        return stack;
    }

    private static ItemPolicy policy(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        var existing = ItemPolicyRegistry.policy(id);
        if (existing.sellable() && !TradeRestrictions.saleProhibited(stack)) return existing;
        if (TradeRestrictions.marketProhibited(stack)) return null;
        if (existing.buyable()) return existing;
        Long price = PRICES.get(id);
        if (price == null || !building(stack)) return null;
        return new ItemPolicy(1, id, ItemPolicy.Outcome.BUY_ONLY, null, price, 256, 4096,
                "building_supply", "User-authorized building purchase; no NPC buyback. Exact acquisition budget from building_purchase_prices.json");
    }

    private static boolean building(ItemStack stack) {
        if (stack.is(Items.PAINTING) || stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS)
                || stack.is(ItemTags.WOOL) || stack.is(ItemTags.SAPLINGS)
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "glass")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "glass_panes")))) return true;
        return com.siirio.jemeconomy.compat.catalog.OptionalCatalog.isBuilding(stack);
    }
    private static Map<ResourceLocation, Long> loadPrices() {
        Map<ResourceLocation, Long> result = new LinkedHashMap<>();
        try (var stream = MarketSupply.class.getResourceAsStream("/data/jem_economy/economy/building_purchase_prices.json")) {
            if (stream == null) throw new IllegalStateException("Missing building acquisition prices");
            var rows = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("items");
            for (var element : rows) {
                var row = element.getAsJsonObject();
                var id = new ResourceLocation(row.get("item").getAsString());
                long copper = row.get("buy_copper").getAsLong();
                if (copper <= 0 || copper > 1_000_000_000L || result.putIfAbsent(id, copper) != null)
                    throw new IllegalStateException("Invalid building acquisition price " + id);
            }
            return Map.copyOf(result);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot load building acquisition prices", failure); }
    }
}
